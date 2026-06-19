package io.github.kxng0109.orchestratorservice.service;

import io.github.kxng0109.orchestratorservice.config.RabbitConfig;
import io.github.kxng0109.orchestratorservice.entity.TransferSagaStates;
import io.github.kxng0109.orchestratorservice.enums.TransactionStatus;
import io.github.kxng0109.orchestratorservice.repository.TransferSagaStatesRepository;
import io.github.kxng0109.orchestratorservice.request.dto.SagaTransferRequest;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orchestrates a distributed transfer saga that coordinates debit operations on a ledger service,
 * forwards funds to an external provider, and records each step for possible compensation.
 *
 * <p>This service is a central component in a micro‑service architecture where financial transfers
 * must be reliable across multiple bounded contexts. It follows the Saga pattern: each step is
 * persisted in {@link TransferSagaStates} and, on failure, subsequent compensating actions can be
 * triggered based on the recorded state.</p>
 *
 * <p>Instances are thread‑safe insofar as the injected dependencies are thread‑safe. The service
 * itself does not perform any internal synchronization; callers should avoid invoking
 * {@link #processTransfer(SagaTransferRequest)} concurrently with the same {@code request}
 * instance.</p>
 *
 * @see TransferSagaStates
 * @see TransactionStatus
 * @see SagaTransferRequest
 * @see RestClient
 * @see RabbitTemplate
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
	 * Initiates a transfer saga that debits the caller's wallet, forwards the funds to an external provider,
	 * and records each step in {@link TransferSagaStates} for later compensation.
	 *
	 * <p>The method performs the following sequence:</p>
	 * <ul>
	 *   <li>Generates a unique {@code UUID} that doubles as an idempotency key for downstream services.</li>
	 *   <li>Persists an initial {@link TransferSagaStates} entry with status {@link TransactionStatus#INITIATED}.</li>
	 *   <li>Attempts to debit the user's ledger account via {@code ledgerClient}. On success the saga state
	 *       progresses to {@link TransactionStatus#LEDGER_DEBITED}; otherwise it transitions to one of the
	 *       failure states ({@link TransactionStatus#LEDGER_FAILED_CLEAN},
	 *       {@link TransactionStatus#LEDGER_TIMEOUT}, {@link TransactionStatus#LEDGER_FAILED}) and the saga
	 *       terminates early.</li>
	 *   <li>If a timeout occurs while contacting the ledger, a pre‑emptive refund request is queued on RabbitMQ
	 *       to mitigate a potential “zombie” debit.</li>
	 *   <li>When the ledger debit succeeds, the method calls the external provider via {@code providerClient}.
	 *       Provider responses are translated into saga state transitions ranging from
	 *       {@link TransactionStatus#PROVIDER_COMPLETED} to various failure or compensation states
	 *       ({@link TransactionStatus#PROVIDER_UNAVAILABLE}, {@link TransactionStatus#PROVIDER_TIMEOUT},
	 *       {@link TransactionStatus#PROVIDER_FAILED}, {@link TransactionStatus#REFUND_PENDING},
	 *       {@link TransactionStatus#REFUND_FAILED}).</li>
	 *   <li>Finally, the updated saga state is persisted and the identifier of the saga record is returned.</li>
	 * </ul>
	 *
	 * <p>All persistence operations are performed through {@code sagaStatesRepository}, which is expected to be
	 * thread‑safe. The method itself does not enforce any synchronization; callers must ensure that a single
	 * {@code processTransfer} invocation per {@code request} is not executed concurrently.</p>
	 *
	 * @param request the transfer request containing {@code userId}, {@code amount},
	 *                {@code destinationAccount} and {@code destinationBankCode}; must be non‑null and
	 *                must satisfy the domain validation rules of {@link SagaTransferRequest}
	 * @return the {@link UUID} that uniquely identifies the persisted saga state record; this identifier can be
	 * used for monitoring, compensation or audit purposes
	 * @throws RestClientResponseException if the ledger or provider services return an HTTP error response
	 *                                     (e.g., 4xx or 5xx). The saga state will be updated to the
	 *                                     corresponding failure status before the exception propagates.
	 * @throws ResourceAccessException     if a low‑level I/O error or timeout occurs while communicating with the
	 *                                     ledger service. The saga state is set to {@link TransactionStatus#LEDGER_TIMEOUT}
	 *                                     and a refund message is enqueued; the exception does not propagate further.
	 * @throws RuntimeException            for any other unchecked exception that escapes the internal try/catch blocks.
	 *                                     Such exceptions indicate unexpected failures (e.g., serialization errors,
	 *                                     RabbitMQ connectivity loss) and will leave the saga in the last successfully
	 *                                     recorded state.
	 * @see TransferSagaStates
	 * @see TransactionStatus
	 * @see SagaTransferRequest
	 * @see RestClient
	 * @see RabbitTemplate
	 */
	@Observed(name = "orchestrator.saga.processTransfer")
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

		} catch (RestClientResponseException e) {
			// If the ledger service returns an http status code before our restClient times-out
			// Then no money was debited, we can be mostly sure about that
			transferState.setStatus(TransactionStatus.LEDGER_FAILED_CLEAN);
			log.warn("Ledger Failed Clean with exception: {}", e.getMessage(), e);
			return sagaStatesRepository.save(transferState).getId();
		} catch (ResourceAccessException e) {
			// Our RestClient times-out or any I/O exception occured
			transferState.setStatus(TransactionStatus.LEDGER_TIMEOUT);
			sagaStatesRepository.save(transferState);

			log.warn("Ledger connection failed or timed out. State Unknown. Firing preemptive refund. Error: {}",
			         e.getMessage(),
			         e
			);

			// Try and issue a refund based on this
			String refundIdempotencyKey = "refund-ledger-timeout-" + uuid;
			try {
				rabbitTemplate.convertAndSend(
						rabbitConfig.TIMEOUT_REFUND_RESPONSE_EXCHANGE_NAME,
						rabbitConfig.TIMEOUT_REFUND_RESPONSE_ROUTING_KEY,
						new CreditRequest(
								request.userId(),
								refundIdempotencyKey,
								request.amount()
						)
				);
			} catch (Exception ex) {
				log.error("CRITICAL: Failed to queue preemptive refund for potential Zombie Ledger request: {}",
				          refundIdempotencyKey,
				          ex
				);
				return sagaStatesRepository.save(transferState).getId();
			}
			return sagaStatesRepository.save(transferState).getId();
		} catch (Exception e) {
			transferState.setStatus(TransactionStatus.LEDGER_FAILED);
			log.warn("Ledger Failed with exception: {}", e.getMessage(), e);
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
		} catch (Exception e) {
			transferState.setStatus(TransactionStatus.PROVIDER_FAILED);
			log.warn("Provider Failed: {}", e.getMessage(), e);
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
