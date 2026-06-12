package io.github.kxng0109.ledgerservice.exception;

import lombok.Getter;

/**
 * Base exception for all ledger service business exceptions.
 * Provides a common type for global exception handling.
 */
@Getter
public abstract class LedgerServiceException extends RuntimeException {

	private final String errorCode;

	/**
	 * Constructs a new {@code LedgerServiceException} with the specified detail message
	 * and error code.
	 *
	 * @param message   the detail message that explains the reason for the exception
	 * @param errorCode a machine‑readable identifier representing the specific error condition
	 */
	protected LedgerServiceException(String message, String errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * Constructs a new {@code LedgerServiceException} with a detail message, an
	 * error code, and a cause.
	 *
	 * @param message   the detail message that explains the reason for the exception
	 * @param errorCode a machine‑readable identifier that categorises the error
	 * @param cause     the underlying {@link Throwable} that caused this exception,
	 *                  may be {@code null} if the cause is not known
	 */
	protected LedgerServiceException(String message, String errorCode, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
}
