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
 * Service responsible for mutating {@link Account} balances and persisting
 * corresponding {@link TransactionLog} entries.
 * <p>
 * The implementation follows a strict pessimistic‑locking strategy to avoid
 * lost updates when multiple threads attempt to modify the same account
 * concurrently. Each public method is wrapped in a Spring {@code @Transactional}
 * context, guaranteeing atomicity between the balance change and the log
 * insertion. If any unchecked exception propagates, the transaction is rolled
 * back, leaving the account state unchanged.
 * </p>
 *
 * @see AccountRepository
 * @see TransactionLogRepository
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
	 * Credits the specified {@code amount} to the account identified by {@code userId}.
	 *
	 * <p>The method obtains a pessimistic write lock on the {@link Account} row to
	 * guarantee exclusive access during the balance update, thereby preventing
	 * lost updates in a highly concurrent environment. After calculating the new
	 * balance, the account record is persisted and a {@link TransactionLog}
	 * entry reflecting the credit operation is created.</p>
	 *
	 * <p>Because the method participates in a Spring-managed transaction
	 * (annotated with {@code @Transactional}), the account update and the log
	 * insertion are committed atomically; any runtime exception will trigger a
	 * rollback.</p>
	 *
	 * @param userId         the unique identifier of the user whose account will be credited;
	 *                       must be non‑null and correspond to an existing {@link Account}
	 * @param idempotencyKey a caller‑provided unique key used to achieve idempotent processing;
	 *                       may be {@code null} if the caller does not require idempotency guarantees
	 * @param amount         the monetary amount to add to the account balance; must be non‑null and non‑negative
	 * @return a {@link TransactionLog} describing the outcome of the credit operation;
	 * the log’s {@link TransactionLog#status status} is set to
	 * {@link io.github.kxng0109.ledgerservice.enums.TransactionStatus#REVERSED} as defined by the current implementation
	 * @throws IllegalArgumentException if {@code amount} is negative
	 * @throws AccountNotFoundException if no {@link Account} exists for the supplied {@code userId}
	 * @see OperationType#CREDIT
	 * @see TransactionStatus
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
