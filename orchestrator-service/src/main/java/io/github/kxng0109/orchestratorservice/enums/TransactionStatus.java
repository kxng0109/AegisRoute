package io.github.kxng0109.orchestratorservice.enums;

/**
 * Represents the discrete lifecycle states of a cross‑system money transfer as it
 * progresses through the orchestrator's saga workflow.
 *
 * <p>The values are persisted in {@link io.github.kxng0109.orchestratorservice.entity.TransferSagaStates}
 * and drive decision‑making in the saga coordination components (e.g.
 * {@link io.github.kxng0109.orchestratorservice.service.SagaResponseListener}). Each
 * constant describes a mutually exclusive condition of the overall transaction,
 * ranging from initial request acceptance to final settlement or failure.</p>
 *
 * <p>All states are stored as {@link jakarta.persistence.EnumType#STRING} in the
 * database, which guarantees stable textual representation across deployments.
 * No two {@link TransactionStatus} values are considered equal unless they are the
 * same enum constant.</p>
 *
 * @see io.github.kxng0109.orchestratorservice.entity.TransferSagaStates
 * @see io.github.kxng0109.orchestratorservice.service.SagaResponseListener
 */
public enum TransactionStatus {
	INITIATED,
	LEDGER_DEBITED,
	LEDGER_FAILED,
	LEDGER_FAILED_CLEAN,
	LEDGER_TIMEOUT,
	LEDGER_TIMEOUT_REFUNDED,
	LEDGER_TIMEOUT_RESOLVED,
	PROVIDER_COMPLETED,
	PROVIDER_UNAVAILABLE,
	PROVIDER_TIMEOUT,
	PROVIDER_FAILED,
	REFUND_PENDING,
	REFUND_COMPLETED,
	REFUND_FAILED
}
