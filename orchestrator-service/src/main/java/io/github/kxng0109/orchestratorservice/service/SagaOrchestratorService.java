package io.github.kxng0109.orchestratorservice.service;

import io.github.kxng0109.orchestratorservice.entity.TransferSagaStates;
import io.github.kxng0109.orchestratorservice.enums.TransactionStatus;
import io.github.kxng0109.orchestratorservice.repository.TransferSagaStatesRepository;
import io.github.kxng0109.orchestratorservice.request.dto.SagaTransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class SagaOrchestratorService {

	private final TransferSagaStatesRepository statesRepository;
	private final RestClient ledgerClient;
	private final RestClient providerClient;

	public SagaOrchestratorService(
			TransferSagaStatesRepository statesRepository,
			@Qualifier("ledgerClient") RestClient ledgerClient,
			@Qualifier("providerClient") RestClient providerClient
	) {
		this.statesRepository = statesRepository;
		this.ledgerClient = ledgerClient;
		this.providerClient = providerClient;
	}

	public UUID processTransfer(SagaTransferRequest request) {
		UUID idempotencyKey = UUID.randomUUID();

		TransferSagaStates transferState = TransferSagaStates.builder()
		                                                     .userId(request.userId())
		                                                     .amount(request.amount())
		                                                     .destinationAccount(request.destinationAccount())
		                                                     .destinationBankCode(request.destinationBankCode())
		                                                     .status(TransactionStatus.INITIATED)
		                                                     .ledgerReferenceId(idempotencyKey)
		                                                     .build();
		statesRepository.save(transferState);

		try {
			ledgerClient.post()
			            .uri("/wallets/{userId}/debit", request.userId())
			            .header("X-Idempotency-Key", String.valueOf(idempotencyKey))
			            .body(new DebitRequest(request.amount()))
			            .retrieve()
			            .toBodilessEntity();

			transferState.setStatus(TransactionStatus.LEDGER_DEBITED);
			statesRepository.save(transferState);

		} catch (Exception e) {
			transferState.setStatus(TransactionStatus.LEDGER_FAILED);
			log.warn("Ledger failed to process debit request: {}", e.getMessage(), e);
			return statesRepository.save(transferState).getId();
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
			statesRepository.save(transferState);

		} catch (RestClientResponseException e) {
			HttpStatusCode errorStatusCode = e.getStatusCode();

			switch (errorStatusCode) {
				case HttpStatus.SERVICE_UNAVAILABLE -> {
					transferState.setStatus(TransactionStatus.PROVIDER_UNAVAILABLE);
					log.warn("Provider Unavailable: {}", e.getMessage(), e);
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

		return statesRepository.save(transferState).getId();
	}

	private record DebitRequest(BigDecimal amount) {
	}

	private record TransferRequest(
			String accountNumber,
			String bankCode,
			BigDecimal amount
	) {
	}
}
