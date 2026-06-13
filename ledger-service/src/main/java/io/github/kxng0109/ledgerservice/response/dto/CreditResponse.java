package io.github.kxng0109.ledgerservice.response.dto;

import io.github.kxng0109.ledgerservice.enums.TransactionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO returned by the ledger service after processing a credit operation.
 *
 * <p>This immutable record contains the essential details of a credit transaction
 * that can be safely sent to API clients or other downstream systems. It is
 * typically constructed by {@link io.github.kxng0109.ledgerservice.service.LedgerFacadeService}
 * from a {@code TransactionLog} entity.</p>
 *
 * @param transactionId the unique identifier of the transaction in the ledger; never {@code null}
 * @param referenceId   the idempotency key or external reference associated with the transaction; never {@code null}
 * @param amount        the monetary amount that was credited; never {@code null}
 * @param status        the current lifecycle state of the transaction, represented by {@link TransactionStatus}; never {@code null}
 * @param timestamp     the instant when the transaction was created in the system; never {@code null}
 */
@Builder
public record CreditResponse(
		UUID transactionId,
		String referenceId,
		BigDecimal amount,
		TransactionStatus status,
		Instant timestamp
) {
}
