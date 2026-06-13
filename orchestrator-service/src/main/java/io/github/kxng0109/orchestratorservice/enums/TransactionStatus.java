package io.github.kxng0109.orchestratorservice.enums;

/**
 * Represents the possible states of a transaction as it progresses through the saga
 * workflow. The values are stored in the {@code status} column of the
 * {@code transfer_saga_states} table and are updated by {@link io.github.kxng0109.orchestratorservice.service.SagaOrchestratorService}
 * as each step of the transfer is attempted.
 *
 * <ul>
 *   <li>{@link #INITIATED} – The transaction has been created but no actions have been taken.</li>
 *   <li>{@link #LEDGER_DEBITED} – The user's ledger has been successfully debited.</li>
 *   <li>{@link #LEDGER_FAILED} – The attempt to debit the ledger failed.</li>
 *   <li>{@link #PROVIDER_COMPLETED} – The external provider completed the transfer.</li>
 *   <li>{@link #PROVIDER_UNAVAILABLE} – The provider service was unavailable (e.g., HTTP 503).</li>
 *   <li>{@link #PROVIDER_TIMEOUT} – The provider request timed out (e.g., HTTP 504).</li>
 *   <li>{@link #PROVIDER_FAILED} – The provider returned an error other than unavailable or timeout.</li>
 *   <li>{@link #REFUND_PENDING} – A refund has been requested but not yet processed.</li>
 *   <li>{@link #REFUND_COMPLETED} – The refund was successfully processed.</li>
 *   <li>{@link #REFUND_FAILED} – The refund attempt failed.</li>
 * </ul>
 */
public enum TransactionStatus {
	INITIATED,
	LEDGER_DEBITED,
	LEDGER_FAILED,
	PROVIDER_COMPLETED,
	PROVIDER_UNAVAILABLE,
	PROVIDER_TIMEOUT,
	PROVIDER_FAILED,
	REFUND_PENDING,
	REFUND_COMPLETED,
	REFUND_FAILED
}
