package io.github.kxng0109.orchestratorservice.enums;

public enum TransactionStatus {
	INITIATED,
	LEDGER_DEBITED,
	PROVIDER_COMPLETED,
	PROVIDER_FAILED,
	REFUND_PENDING,
	REFUND_COMPLETED,
	REFUND_FAILED
}
