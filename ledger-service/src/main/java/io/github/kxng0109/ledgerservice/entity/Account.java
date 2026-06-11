package io.github.kxng0109.ledgerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a financial account within the ledger service.
 *
 * <p>This entity is mapped to the {@code accounts} table and stores the core
 * attributes required to identify and manage a user's monetary balance:
 *
 * <ul>
 *   <li>A system‑generated unique identifier ({@code id}) used as the primary key.</li>
 *   <li>A unique, non‑null user identifier ({@code userId}) that links the account to a
 *       specific client or customer.</li>
 *   <li>The current monetary balance ({@code balance}) expressed as a {@link BigDecimal}
 *       with two decimal places of precision.</li>
 *   <li>The currency code ({@code currency}) indicating the denomination of the balance.</li>
 *   <li>Audit timestamps ({@code createdAt} and {@code updatedAt}) automatically populated
 *       by Hibernate to record when the account was created and last modified.</li>
 * </ul>
 *
 * <p>Instances of this class are typically managed by a JPA repository. The {@code
 * balance} field should be modified only through controlled business operations that also
 * create corresponding {@link TransactionLog}
 * entries, ensuring a reliable audit trail of all financial activity.
 */
@Entity
@Table(name = "accounts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", unique = true, nullable = false)
	private String userId;

	@Column(name = "balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal balance;

	@Column(name = "currency", nullable = false)
	private String currency;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;
}
