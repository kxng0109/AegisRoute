package io.github.kxng0109.orchestratorservice.service;

import io.github.kxng0109.orchestratorservice.config.RabbitConfig;
import io.github.kxng0109.orchestratorservice.entity.TransferSagaStates;
import io.github.kxng0109.orchestratorservice.enums.TransactionStatus;
import io.github.kxng0109.orchestratorservice.repository.TransferSagaStatesRepository;
import io.github.kxng0109.orchestratorservice.request.dto.SagaTransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service that coordinates the execution of a money‑transfer saga across the ledger and an
 * external provider. It persists the saga state, invokes the required remote services and,
 * in case of failures, updates the state accordingly and optionally queues a refund
 * operation via RabbitMQ. The saga follows an explicit state machine represented by
 * {@link TransactionStatus} values stored in {@link TransferSagaStates}.
 *
 * <p>Typical flow:</p>
 * <ol>
 *   <li>Create a saga state with status {@code INITIATED} and persist it.</li>
 *   <li>Debit the user's wallet using the ledger service. On success, update the state to
 *   {@code LEDGER_DEBITED}; on failure, set {@code LEDGER_FAILED} and finish.</li>
 *   <li>Invoke the external provider to complete the transfer. Depending on the response,
 *   update the state to {@code PROVIDER_COMPLETED}, {@code PROVIDER_UNAVAILABLE},
 *   {@code PROVIDER_TIMEOUT} or {@code PROVIDER_FAILED}.</li>
 *   <li>If the provider is unavailable, a refund request is sent to a RabbitMQ exchange.
 *   The state is set to {@code REFUND_PENDING} while the message is queued, or to
 *   {@code REFUND_FAILED} if queuing fails.</li>
 *   <li>Finally, persist and return the identifier of the saga state.</li>
 * </ol>
 * <p>
 * All interactions with external services are performed using {@link RestClient},
 * and messaging uses {@link RabbitTemplate} together with {@link RabbitConfig}.
 */
@Service
@Slf4j
public class SagaOrchestratorService {

	private final TransferSagaStatesRepository sagaStatesRepository;
	private final RestClient ledgerClient;
	private final RestClient providerClient;
	private final RabbitTemplate rabbitTemplate;
	private final RabbitConfig rabbitConfig;

	public SagaOrchestratorService(
			TransferSagaStatesRepository sagaStatesRepository,
			@Qualifier("ledgerClient") RestClient ledgerClient,
			@Qualifier("providerClient") RestClient providerClient,
			RabbitTemplate rabbitTemplate,
			RabbitConfig rabbitConfig
	) {
		this.sagaStatesRepository = sagaStatesRepository;
		this.ledgerClient = ledgerClient;
		this.providerClient = providerClient;
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitConfig = rabbitConfig;
	}

	/**
	 * Initiates a money transfer saga for the given {@code request}. The method creates a new saga
	 * state entry, performs a debit operation against the ledger service, and then attempts to
	 * complete the transfer with an external provider. Depending on the outcome of each step,
	 * the saga state is updated with the appropriate {@link TransactionStatus}. If the provider
	 * is unavailable, a refund request is queued for asynchronous processing. The method
	 * persists the final saga state and returns the identifier of that persisted state.
	 *
	 * @param request a {@link SagaTransferRequest} containing the user identifier, transfer amount,
	 *                destination account, and destination bank code
	 * @return the {@link UUID} identifier of the persisted {@link TransferSagaStates} record that
	 * represents the final state of the transfer saga
	 */
	public UUID processTransfer(SagaTransferRequest request) {
		UUID uuid = UUID.randomUUID();
		String idempotencyKey = "transfer-" + uuid;

		TransferSagaStates transferState = TransferSagaStates.builder()
		                                                     .userId(request.userId())
		                                                     .amount(request.amount())
		                                                     .destinationAccount(request.destinationAccount())
		                                                     .destinationBankCode(request.destinationBankCode())
		                                                     .status(TransactionStatus.INITIATED)
		                                                     .ledgerReferenceId(idempotencyKey)
		                                                     .build();
		sagaStatesRepository.save(transferState);

		try {
			ledgerClient.post()
			            .uri("/wallets/{userId}/debit", request.userId())
			            .header("X-Idempotency-Key", idempotencyKey)
			            .body(new DebitRequest(request.amount()))
			            .retrieve()
			            .toBodilessEntity();

			transferState.setStatus(TransactionStatus.LEDGER_DEBITED);
			sagaStatesRepository.save(transferState);

		} catch (Exception e) {
			transferState.setStatus(TransactionStatus.LEDGER_FAILED);
			log.warn("Ledger failed to process debit request: {}", e.getMessage(), e);
			return sagaStatesRepository.save(transferState).getId();
		}

		try {
			providerClient.post()
			              .uri("/provider/transfer")
			              .body(
					              new TransferRequest(
							              request.destinationAccount(),
							              request.destinationBankCode(),
							              request.amount()
					              )
			              )
			              .retrieve()
			              .toBodilessEntity();

			transferState.setStatus(TransactionStatus.PROVIDER_COMPLETED);
			sagaStatesRepository.save(transferState);

		} catch (RestClientResponseException e) {
			HttpStatusCode errorStatusCode = e.getStatusCode();

			switch (errorStatusCode) {
				case HttpStatus.SERVICE_UNAVAILABLE -> {
					transferState.setStatus(TransactionStatus.PROVIDER_UNAVAILABLE);
					sagaStatesRepository.save(transferState);
					log.warn("Provider Unavailable: {}", e.getMessage(), e);

					String refundIdempotencyKey = "refund-" + uuid;
					try {
						rabbitTemplate.convertAndSend(
								rabbitConfig.CREDIT_EXCHANGE_NAME,
								rabbitConfig.CREDIT_ROUTING_KEY,
								new CreditRequest(
										request.userId(),
										refundIdempotencyKey,
										request.amount()
								)
						);

						transferState.setStatus(TransactionStatus.REFUND_PENDING);
						sagaStatesRepository.save(transferState);
					} catch (Exception ex) {
						transferState.setStatus(TransactionStatus.REFUND_FAILED);
						sagaStatesRepository.save(transferState);

						log.error(
								"Error queuing refund for request with reference ID: {}. {}",
								refundIdempotencyKey,
								ex.getMessage(),
								ex
						);
					}
				}
				case HttpStatus.GATEWAY_TIMEOUT -> {
					transferState.setStatus(TransactionStatus.PROVIDER_TIMEOUT);
					log.warn("Provider Gateway Timeout: {}", e.getMessage(), e);
				}
				default -> {
					transferState.setStatus(TransactionStatus.PROVIDER_FAILED);
					log.warn("Provider Failed: {}", e.getMessage(), e);
				}
			}
		}

		return sagaStatesRepository.save(transferState).getId();
	}

	private record DebitRequest(BigDecimal amount) {
	}

	private record TransferRequest(
			String accountNumber,
			String bankCode,
			BigDecimal amount
	) {
	}

	private record CreditRequest(
			String userId,
			String idempotencyKey,
			BigDecimal amount
	) {
	}
}
