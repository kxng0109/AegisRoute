package io.github.kxng0109.ledgerservice.enums;

/**
 * Represents the lifecycle state of a {@link io.github.kxng0109.ledgerservice.entity.TransactionLog}
 * entry within the ledger service.
 *
 * <p>The status is persisted as a {@code STRING} value in the {@code transaction_logs}
 * table and drives the business logic of the Saga orchestrator, compensating actions,
 * and client notifications.  It is deliberately exhaustive to capture every outcome
 * that can arise from the interaction between the internal ledger and external payment
 * providers.</p>
 *
 * <ul>
 *   <li>{@link #PENDING} – The transaction is locked and awaiting confirmation from an
 *       external system; no final decision has been recorded.</li>
 *   <li>{@link #SUCCESS} – The external provider affirmed the operation and the ledger
 *       entry has been applied (funds added or deducted).</li>
 *   <li>{@link #FAILED} – The internal processing (e.g., balance check, persistence)
 *       could not be completed; the transaction will not affect account balances.</li>
 *   <li>{@link #REVERSED} – A compensating saga step was executed because a downstream
 *       failure occurred after the initial operation succeeded.</li>
 *   <li>{@link #NOT_FOUND} – No corresponding {@code TransactionLog} exists; this is used
 *       by the {@code SagaOrchestratorService} to detect zombie debits.</li>
 * </ul>
 *
 * <p>Consumers should treat the enum as immutable and rely on its values for branching
 * logic rather than comparing string representations.  The enum itself is thread‑safe
 * because it is immutable.</p>
 *
 * @see io.github.kxng0109.ledgerservice.entity.TransactionLog
 * @see OperationType
 */
public enum TransactionStatus {
	PENDING,    // The lock is active, but the final outcome is not yet known.
	SUCCESS,    // The external provider confirmed the transaction, or funds were successfully added.
	FAILED,     // The internal ledger operation failed (e.g., insufficient funds).
	REVERSED,   // The Saga Orchestrator triggered a compensating transaction due to an external failure.
	NOT_FOUND   // Transaction Log was not found (used to confirm Zombie Debits in SagaOrchestratorService)
}
