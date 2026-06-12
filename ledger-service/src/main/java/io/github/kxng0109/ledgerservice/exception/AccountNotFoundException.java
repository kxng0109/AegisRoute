package io.github.kxng0109.ledgerservice.exception;

/**
 * Thrown when an account cannot be found for the given user identifier.
 */
public class AccountNotFoundException extends LedgerServiceException {

	private static final String ERROR_CODE = "ACCOUNT_NOT_FOUND";

	/**
	 * Constructs a new {@code AccountNotFoundException} with a message that includes the
	 * identifier of the user whose account could not be located. The exception is
	 * associated with the {@value #ERROR_CODE} error code.
	 *
	 * @param userId the identifier of the user for whom the account lookup failed;
	 *               must not be {@code null}
	 */
	public AccountNotFoundException(String userId) {
		super(
				String.format("Account not found for user: %s", userId),
				ERROR_CODE
		);
	}
}
