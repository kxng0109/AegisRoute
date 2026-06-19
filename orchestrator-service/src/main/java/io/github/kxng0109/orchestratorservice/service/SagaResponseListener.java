package io.github.kxng0109.orchestratorservice.service;

import io.github.kxng0109.orchestratorservice.entity.TransferSagaStates;
import io.github.kxng0109.orchestratorservice.enums.TransactionStatus;
import io.github.kxng0109.orchestratorservice.repository.TransferSagaStatesRepository;
import io.github.kxng0109.orchestratorservice.response.dto.RefundResponse;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Listens for refund responses coming from the messaging infrastructure and
 * reconciles them with the corresponding transfer saga state.
 *
 * <p>This component is a Spring {@code @Service} that is instantiated with a
 * {@link TransferSagaStatesRepository} used to locate and persist saga entities.
 * When a {@link RefundResponse} is received on the {@code refund.response.queue}
 * the listener extracts the original transfer reference, validates the existence
 * of a matching {@link TransferSagaStates} record for the same user, and updates
 * the saga status according to the refund outcome.</p>
 *
 * <p>If no matching saga state is found, an error is logged and the message is
 * ignored. For recognized refund statuses the saga entity is updated and saved;
 * unrecognised statuses generate a warning and leave the saga unchanged.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaResponseListener {

	private final TransferSagaStatesRepository sagaStatesRepository;

	/**
	 * Processes a {@link RefundResponse} received from the {@code refund.response.queue} and synchronises the
	 * corresponding {@link TransferSagaStates} entity with the refund outcome.
	 *
	 * <p>The method extracts the original transfer reference from {@code response.referenceId()} by
	 * stripping the {@code "refund-"} or {@code "refund-ledger-timeout-"} prefix and replacing it with
	 * {@code "transfer-"}. It then looks up a {@link TransferSagaStates} record that matches both the
	 * derived ledger reference ID and the {@code response.userId()}.</p>
	 *
	 * <p>If no matching saga state exists, a critical log entry is emitted and the message is ignored.
	 * For recognised refund statuses the saga state is updated as follows:</p>
	 * <ul>
	 *   <li>{@code "NOT_FOUND"} –&gt; {@link TransactionStatus#LEDGER_TIMEOUT_RESOLVED}</li>
	 *   <li>{@code "REVERSED"} –&gt;
	 *     <ul>
	 *       <li>when the response originated from a ledger timeout (prefix {@code "refund-ledger-timeout-"}),
	 *           the saga status becomes {@link TransactionStatus#LEDGER_TIMEOUT_REFUNDED};</li>
	 *       <li>otherwise it becomes {@link TransactionStatus#REFUND_COMPLETED}.</li>
	 *     </ul>
	 *   </li>
	 *   <li>{@code "FAILED"} –&gt; {@link TransactionStatus#REFUND_FAILED}</li>
	 * </ul>
	 *
	 * <p>Any unrecognised {@code response.status()} results in a warning log and leaves the saga state
	 * unchanged. After updating the status, the saga entity is persisted via {@link TransferSagaStatesRepository#save(Object)}.</p>
	 *
	 * <p>This listener is thread‑safe because it does not retain mutable state between invocations;
	 * each call works on a locally scoped {@link TransferSagaStates} instance retrieved from the
	 * repository, which itself is expected to handle concurrent access according to Spring Data JPA
	 * semantics.</p>
	 *
	 * @param response the refund response payload; must be non‑null and contain a non‑blank
	 *                 {@code referenceId()}, a valid {@code userId()}, and a status string
	 *                 matching one of the recognised values ("NOT_FOUND", "REVERSED", "FAILED")
	 */
	@RabbitListener(queues = "refund.response.queue")
	@Observed(name = "saga.response.listener.handleRefundRespond")
	public void handleRefundResponse(RefundResponse response) {
		String referenceId = "";
		// If it contains "refund-ledger-timeout-" that means it came from a ledger timeout issue
		boolean isFromLedgerTimeout = response.referenceId().startsWith("refund-ledger-timeout-");
		if (isFromLedgerTimeout) {
			referenceId = response.referenceId().replace("refund-ledger-timeout-", "transfer-");
		} else {
			referenceId = response.referenceId().replace("refund-", "transfer-");
		}

		// Find the existing saga transfer state in the database
		Optional<TransferSagaStates> transferStateOpt = sagaStatesRepository.findByLedgerReferenceIdAndUserId(
				referenceId,
				response.userId()
		);

		// It doesn't exist? That's weird. Where did this one come from?
		if (transferStateOpt.isEmpty()) {
			log.error("CRITICAL: Ledger Reference ID {} not found for User ID {}. Refund response discarded.",
			          referenceId, response.userId()
			);
			return;
		}

		TransferSagaStates transferState = transferStateOpt.get();

		switch (response.status()) {
			case "NOT_FOUND" -> transferState.setStatus(TransactionStatus.LEDGER_TIMEOUT_RESOLVED);
			case "REVERSED" -> {
				// We have two cases of a "REVERSED":
				// 1. It came as a result of a ledger restClient timeout
				// 2. A refund was issued due to the provider being unavailable
				if (isFromLedgerTimeout) {
					transferState.setStatus(TransactionStatus.LEDGER_TIMEOUT_REFUNDED);
				} else {
					transferState.setStatus(TransactionStatus.REFUND_COMPLETED);
				}
			}
			case "FAILED" -> transferState.setStatus(TransactionStatus.REFUND_FAILED);
			default -> {
				log.warn("Unknown refund status received: {}. No state updated.", response.status());
				return;
			}
		}

		sagaStatesRepository.save(transferState);
	}
}
