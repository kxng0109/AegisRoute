package io.github.kxng0109.ledgerservice.service;

import io.github.kxng0109.ledgerservice.entity.Account;
import io.github.kxng0109.ledgerservice.entity.TransactionLog;
import io.github.kxng0109.ledgerservice.enums.OperationType;
import io.github.kxng0109.ledgerservice.enums.TransactionStatus;
import io.github.kxng0109.ledgerservice.exception.AccountNotFoundException;
import io.github.kxng0109.ledgerservice.repository.AccountRepository;
import io.github.kxng0109.ledgerservice.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service component that encapsulates the core ledger operations for user accounts.
 * <p>
 * The service provides transactional methods to debit and credit accounts while
 * maintaining a durable audit trail through {@link TransactionLog} entries.
 * Pessimistic locking is employed when retrieving accounts to guarantee
 * consistency under concurrent access. Each operation records its outcome with
 * an appropriate {@link TransactionStatus} and {@link OperationType}.
 * </p>
 *
 * <p>
 * Dependencies:
 * <ul>
 *   <li>{@link TransactionLogRepository} – persists transaction logs.</li>
 *   <li>{@link AccountRepository} – accesses and updates {@link Account} entities
 *       with write locks.</li>
 * </ul>
 * </p>
 *
 * <p>
 * All public methods are annotated with {@code @Transactional} to ensure that
 * account updates and log persistence occur within a single atomic database
 * transaction. Idempotency is supported via a caller‑supplied {@code idempotencyKey},
 * allowing repeated requests to be safely ignored or reconciled.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CoreLedgerService {

	private final TransactionLogRepository transactionLogRepository;
	private final AccountRepository accountRepository;

	/**
	 * Processes a debit transaction for the specified user.
	 *
	 * <p>This method retrieves the user's account with a pessimistic lock, calculates the new
	 * balance after subtracting the requested {@code amount}, and records a {@link TransactionLog}
	 * indicating whether the debit succeeded or failed. If the resulting balance would be
	 * negative, the transaction is marked as {@link TransactionStatus#FAILED} and the account
	 * balance remains unchanged. Otherwise, the account balance is updated, and a successful
	 * transaction log is persisted.</p>
	 *
	 * @param userId         the unique identifier of the user whose account will be debited
	 * @param idempotencyKey a unique key used to ensure idempotent processing of the transaction
	 * @param amount         the monetary amount to debit from the user's account; must be non‑negative
	 * @return a {@link TransactionLog} representing the outcome of the debit operation,
	 * with status {@link TransactionStatus#SUCCESS} if the debit was applied,
	 * or {@link TransactionStatus#FAILED} if insufficient funds were available
	 */
	@Transactional
	public TransactionLog processDebit(
			String userId,
			String idempotencyKey,
			BigDecimal amount
	) {
		// Acquire the database pessimistic lock
		Account account = accountRepository.findByUserIdForUpdate(userId)
		                                   .orElseThrow(
				                                   () -> new AccountNotFoundException(
						                                   "Account not found for ID: " + userId
				                                   )
		                                   );

		BigDecimal newBalance = account.getBalance().subtract(amount);

		// If the user doesn't have sufficient balance to complete the transaction
		// create a failed log, save it to the database and return it
		if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
			TransactionLog failedLog = TransactionLog.builder()
			                                         .account(account)
			                                         .operationType(OperationType.DEBIT)
			                                         .amount(amount)
			                                         .referenceId(idempotencyKey)
			                                         .status(TransactionStatus.FAILED)
			                                         .build();
			return transactionLogRepository.save(failedLog);
		}

		account.setBalance(newBalance);
		accountRepository.save(account);

		TransactionLog successLog = TransactionLog.builder()
		                                          .account(account)
		                                          .operationType(OperationType.DEBIT)
		                                          .amount(amount)
		                                          .referenceId(idempotencyKey)
		                                          .status(TransactionStatus.SUCCESS)
		                                          .build();

		return transactionLogRepository.save(successLog);
	}

	/**
	 * Credits the specified amount to a user's account.
	 *
	 * <p>This method retrieves the account identified by {@code userId} using a pessimistic
	 * write lock to ensure exclusive access, adds the {@code amount} to the current balance,
	 * persists the updated account, and creates a {@link TransactionLog} recording the credit
	 * operation. The transaction log is saved and returned to the caller.</p>
	 *
	 * @param userId         the unique identifier of the user whose account will be credited
	 * @param idempotencyKey a unique key used to guarantee idempotent processing of the credit
	 * @param amount         the monetary amount to add to the user's balance; must be non‑negative
	 * @return a {@link TransactionLog} representing the credit operation. The log records the
	 * {@link OperationType#CREDIT} and has a status of {@link TransactionStatus#REVERSED}
	 * as defined in the current implementation.
	 */
	@Transactional
	public TransactionLog processCredit(
			String userId,
			String idempotencyKey,
			BigDecimal amount
	) {
		// Acquire the database pessimistic lock
		Account account = accountRepository.findByUserIdForUpdate(userId)
		                                   .orElseThrow(
				                                   () -> new AccountNotFoundException(
						                                   "Account not found for ID: " + userId
				                                   )
		                                   );

		BigDecimal newBalance = account.getBalance().add(amount);

		account.setBalance(newBalance);
		accountRepository.save(account);

		TransactionLog successLog = TransactionLog.builder()
		                                          .account(account)
		                                          .operationType(OperationType.CREDIT)
		                                          .amount(amount)
		                                          .referenceId(idempotencyKey)
		                                          .status(TransactionStatus.REVERSED)
		                                          .build();

		return transactionLogRepository.save(successLog);
	}
}
