package io.github.kxng0109.ledgerservice.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Thrown when an account has insufficient balance for a debit operation.
 */
public class InsufficientFundsException extends LedgerServiceException {

	private static final String ERROR_CODE = "INSUFFICIENT_FUNDS";

	@Getter
	private final BigDecimal currentBalance;
	@Getter
	private final BigDecimal requestedAmount;

	/**
	 * Creates a new {@code InsufficientFundsException} indicating that a debit operation
	 * cannot be performed because the account balance is lower than the amount requested.
	 *
	 * @param currentBalance  the current balance of the account; must not be {@code null}
	 * @param requestedAmount the amount that was attempted to be debited; must not be {@code null}
	 */
	public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requestedAmount) {
		super(
				String.format(
						"Insufficient funds. Current balance: %s, Requested: %s",
						currentBalance.toPlainString(),
						requestedAmount.toPlainString()
				),
				ERROR_CODE
		);
		this.currentBalance = currentBalance;
		this.requestedAmount = requestedAmount;
	}
}
