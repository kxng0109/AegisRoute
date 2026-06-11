package io.github.kxng0109.ledgerservice.entity;

import io.github.kxng0109.ledgerservice.enums.OperationType;
import io.github.kxng0109.ledgerservice.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an immutable record of a financial operation performed on an {@link Account}.
 * <p>
 * Each instance corresponds to a row in the {@code transaction_logs} table and captures the
 * essential details required for audit trails, reconciliation, and reporting:
 * <ul>
 *   <li>The unique identifier of the transaction.</li>
 *   <li>The account to which the transaction applies.</li>
 *   <li>The type of operation (e.g., deposit, withdrawal, transfer).</li>
 *   <li>The monetary amount involved, stored with two decimal places of precision.</li>
 *   <li>A globally unique reference identifier that can be used to correlate this log with
 *       external systems or client requests.</li>
 *   <li>The current processing status of the transaction (e.g., pending, completed, failed).</li>
 *   <li>The timestamp when the log entry was created.</li>
 * </ul>
 *
 * <p>The entity is designed to be read‑only after creation; updates to an account's balance
 * should result in a new {@code TransactionLog} rather than modifying an existing one.
 *
 * <p>Typical usage involves persisting a new instance via a JPA repository whenever a
 * transaction is processed, thereby providing a reliable historical record for compliance
 * and debugging purposes.
 */
@Entity
@Table(name = "transaction_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "account_id", nullable = false)
	private Account accountId;

	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false)
	private OperationType operationType;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "reference_id", nullable = false, unique = true)
	private String referenceId;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TransactionStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
