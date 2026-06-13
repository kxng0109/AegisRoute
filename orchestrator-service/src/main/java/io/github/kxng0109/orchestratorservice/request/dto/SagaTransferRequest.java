package io.github.kxng0109.orchestratorservice.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO that represents a request to initiate a money‑transfer saga.
 * <p>
 * The record is validated by Bean Validation annotations before it is processed
 * by the {@code SagaOrchestratorService}. All fields are required and must satisfy
 * the following constraints:
 * <ul>
 *   <li>{@code userId} – must not be blank.</li>
 *   <li>{@code destinationAccount} – must not be blank.</li>
 *   <li>{@code destinationBankCode} – must not be blank.</li>
 *   <li>{@code amount} – must not be {@code null}, must be at least {@code 0.01},
 *       and may contain up to 17 integer digits and 2 fractional digits.</li>
 * </ul>
 *
 * @param userId              the identifier of the user who owns the source ledger
 * @param destinationAccount  the account number of the transfer recipient
 * @param destinationBankCode the bank code of the recipient's bank
 * @param amount              the monetary amount to transfer
 */
@Builder
public record SagaTransferRequest(
		@NotBlank(message = "User ID is required!")
		String userId,

		@NotBlank(message = "Destination Account is required!")
		String destinationAccount,

		@NotBlank(message = "Destination Bank Code is required!")
		String destinationBankCode,

		@NotNull(message = "Amount must not be blank")
		@DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
		@Digits(integer = 17, fraction = 2, message = "Amount must be in the format '999999.99'")
		BigDecimal amount
) {
}
