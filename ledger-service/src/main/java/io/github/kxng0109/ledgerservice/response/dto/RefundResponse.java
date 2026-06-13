package io.github.kxng0109.ledgerservice.response.dto;

import lombok.Builder;

/**
 * Response DTO returned by the ledger service after processing a refund operation.
 *
 * <p>This immutable record conveys the essential outcome of a refund request.
 * It is typically created by the {@code LedgerFacadeService} after a refund has
 * been recorded in the system and can be safely transmitted to API clients or
 * other downstream services.</p>
 *
 * @param status      the final status of the refund operation (e.g., {@code "COMPLETED"},
 *                    {@code "PENDING"}, {@code "FAILED"}); never {@code null}
 * @param referenceId the external reference or idempotency key associated with the
 *                    refund; never {@code null}
 * @param userId      the identifier of the user who initiated the refund; never {@code null}
 */
@Builder
public record RefundResponse(
		String status,
		String referenceId,
		String userId
) {
}
