package io.github.kxng0109.orchestratorservice.entity;

import io.github.kxng0109.orchestratorservice.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_saga_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferSagaStates {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Column(name = "amount", precision = 19, scale = 2, nullable = false)
	private BigDecimal amount;

	@Column(name = "destination_account", nullable = false)
	private String destinationAccount;

	@Column(name = "destination_bank_code", nullable = false)
	private String destinationBankCode;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private TransactionStatus status = TransactionStatus.INITIATED;

	@Column(name = "ledger_reference_id", nullable = false, unique = true)
	private String ledgerReferenceId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
