package io.github.kxng0109.ledgerservice.exception;

import lombok.Getter;

/**
 * Thrown when a duplicate transaction request is detected
 * (same referenceId is already being processed).
 */
public class DuplicateTransactionException extends LedgerServiceException {

	private static final String ERROR_CODE = "DUPLICATE_TRANSACTION";

	@Getter
	private final String referenceId;

	/**
	 * Constructs a new {@code DuplicateTransactionException} with the specified detail
	 * message and the reference identifier of the duplicate transaction.
	 *
	 * @param message     a description of the error condition; typically explains why the
	 *                    transaction is considered a duplicate
	 * @param referenceId the identifier of the transaction that has already been processed;
	 *                    must not be {@code null}
	 */
	public DuplicateTransactionException(String message, String referenceId) {
		super(message, ERROR_CODE);
		this.referenceId = referenceId;
	}
}
