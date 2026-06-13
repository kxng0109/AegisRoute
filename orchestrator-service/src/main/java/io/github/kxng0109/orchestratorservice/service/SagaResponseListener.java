package io.github.kxng0109.orchestratorservice.service;

import io.github.kxng0109.orchestratorservice.entity.TransferSagaStates;
import io.github.kxng0109.orchestratorservice.enums.TransactionStatus;
import io.github.kxng0109.orchestratorservice.repository.TransferSagaStatesRepository;
import io.github.kxng0109.orchestratorservice.response.dto.RefundResponse;
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
	 * Processes a refund response received from the message queue and updates the corresponding
	 * saga state. The method extracts the original transfer reference ID from the refund reference,
	 * looks up the {@link TransferSagaStates} entity using the reference ID and user ID, and then
	 * updates the transaction status based on the refund status:
	 * <ul>
	 *   <li>If the status is {@code "REVERSED"}, the saga state is marked as
	 *   {@link TransactionStatus#REFUND_COMPLETED}.</li>
	 *   <li>If the status is {@code "FAILED"}, the saga state is marked as
	 *   {@link TransactionStatus#REFUND_FAILED}.</li>
	 *   <li>Any other status is considered unknown; a warning is logged and no state is updated.</li>
	 * </ul>
	 * If no saga state is found for the provided reference ID and user ID, an error is logged and
	 * the response is discarded. After a successful status update, the saga state is persisted via
	 * {@link TransferSagaStatesRepository#save(Object)}.
	 *
	 * @param response the refund response payload containing the refund status, reference ID, and user ID
	 */
	@RabbitListener(queues = "refund.response.queue")
	public void handleRefundResponse(RefundResponse response) {
		String referenceId = response.referenceId().replace("refund-", "transfer-");

		Optional<TransferSagaStates> transferStateOpt = sagaStatesRepository.findByLedgerReferenceIdAndUserId(
				referenceId,
				response.userId()
		);

		if (transferStateOpt.isEmpty()) {
			log.error("CRITICAL: Ledger Reference ID {} not found for User ID {}. Refund response discarded.",
			          referenceId, response.userId()
			);
			return;
		}

		TransferSagaStates transferState = transferStateOpt.get();

		switch (response.status()) {
			case "REVERSED" -> transferState.setStatus(TransactionStatus.REFUND_COMPLETED);
			case "FAILED" -> transferState.setStatus(TransactionStatus.REFUND_FAILED);
			default -> {
				log.warn("Unknown refund status received: {}. No state updated.", response.status());
				return;
			}
		}

		sagaStatesRepository.save(transferState);
	}
}
