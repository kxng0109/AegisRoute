package io.github.kxng0109.ledgerservice.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Represents a request to debit a specific amount from a user's wallet.
 *
 * <p>This record is used as the payload of the {@code POST /api/v1/wallets/{userId}/debit}
 * endpoint. The single component, {@code amount}, is subject to several validation
 * constraints to ensure that the transaction is well‑formed before it is processed
 * by the {@link io.github.kxng0109.ledgerservice.service.LedgerFacadeService}.
 *
 * <ul>
 *   <li>{@code @NotNull} – the amount must be provided; a {@code null} value will cause
 *       a validation error.</li>
 *   <li>{@code @DecimalMin("0.01")} – the amount must be greater than or equal to
 *       {@code 0.01}, preventing zero or negative debits.</li>
 *   <li>{@code @Digits(integer = 17, fraction = 2)} – the amount may contain up to
 *       17 digits to the left of the decimal point and up to 2 digits to the right,
 *       matching the expected monetary format {@code 999999.99}.</li>
 * </ul>
 *
 * <p>When a valid {@code DebitRequest} is received, the controller extracts the
 * {@code amount()} value and forwards it to the service layer, where idempotency
 * handling and the actual debit operation are performed.
 */
public record DebitRequest(
		@NotNull(message = "Amount must not be blank")
		@DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
		@Digits(integer = 17, fraction = 2, message = "Amount must be in the format '999999.99'")
		BigDecimal amount
) {
}
