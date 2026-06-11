package io.github.kxng0109.ledgerservice.enums;

/**
 * Represents the lifecycle state of a financial transaction recorded in
 * {@link io.github.kxng0109.ledgerservice.entity.TransactionLog}.
 *
 * <p>The status is persisted as a {@code STRING} value in the {@code transaction_logs}
 * table and is used together with {@link OperationType}
 * to convey the complete processing outcome of a transaction.
 *
 * <ul>
 *   <li>{@link #PENDING} – The transaction has been locked for processing but the final
 *       result is not yet known.</li>
 *   <li>{@link #SUCCESS} – The external provider confirmed the transaction or the funds
 *       were successfully added to the account.</li>
 *   <li>{@link #FAILED} – The internal ledger operation could not be completed
 *       (for example, due to insufficient funds).</li>
 *   <li>{@link #REVERSED} – A compensating transaction was executed by the Saga
 *       orchestrator because an external failure occurred after an initial success.</li>
 * </ul>
 *
 * These values guide business logic for retry, compensation, and reporting
 * mechanisms within the ledger service.
 */
public enum TransactionStatus {
	PENDING,    // The lock is active, but the final outcome is not yet known.
	SUCCESS,    // The external provider confirmed the transaction, or funds were successfully added.
	FAILED,     // The internal ledger operation failed (e.g., insufficient funds).
	REVERSED    // The Saga Orchestrator triggered a compensating transaction due to an external failure.
}
