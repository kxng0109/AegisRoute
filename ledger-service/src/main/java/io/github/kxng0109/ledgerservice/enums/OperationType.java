package io.github.kxng0109.ledgerservice.enums;

/**
 * Enumerates the possible directions of a financial transaction.
 *
 * <ul>
 *   <li>{@link #CREDIT} indicates that funds are added to an {@code Account},
 *       increasing its balance.</li>
 *   <li>{@link #DEBIT} indicates that funds are withdrawn from an {@code Account},
 *       decreasing its balance.</li>
 * </ul>
 *
 * The {@code OperationType} is stored in the {@code transaction_logs} table as a
 * {@code STRING} value and is used together with {@link TransactionStatus}
 * to represent the complete state of a {@link io.github.kxng0109.ledgerservice.entity.TransactionLog}
 * entry.
 */
public enum OperationType {
	CREDIT,
	DEBIT
}
