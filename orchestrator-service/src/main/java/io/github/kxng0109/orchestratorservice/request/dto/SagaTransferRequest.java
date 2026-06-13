package io.github.kxng0109.orchestratorservice.request.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

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
