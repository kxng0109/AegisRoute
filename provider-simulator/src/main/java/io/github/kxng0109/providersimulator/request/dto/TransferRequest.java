package io.github.kxng0109.providersimulator.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Represents a request to transfer funds from a specific account.
 *
 * <p>The request contains the account number and bank code identifying the source
 * account, together with the amount to be transferred. Validation constraints are
 * applied to ensure that all required fields are present and that the amount is
 * a positive decimal value with up to two fractional digits.</p>
 *
 * <ul>
 *   <li>The {@code accountNumber} must not be {@code null}.</li>
 *   <li>The {@code bankCode} must not be {@code null}.</li>
 *   <li>The {@code amount} must not be {@code null}, must be at least 0.01,
 *       and may contain at most 17 integer digits and 2 fractional digits.</li>
 * </ul>
 *
 * <p>This record is used as the request body of the {@code /api/v1/provider/transfer}
 * endpoint. It is processed by Spring MVC with {@code @Valid} to enforce the
 * defined constraints before the transfer operation is attempted.</p>
 */
@Builder
public record TransferRequest(
		@NotNull(message = "Account Number is required!")
		String accountNumber,

		@NotNull(message = "Bank Code is required!")
		String bankCode,

		@NotNull(message = "Amount must not be blank")
		@DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
		@Digits(integer = 17, fraction = 2, message = "Amount must be in the format '999999.99'")
		BigDecimal amount
) {
}
