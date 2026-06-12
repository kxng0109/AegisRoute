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
 * Service responsible for core ledger operations such as processing debit transactions.
 *
 * <ul>
 *   <li>Retrieves an account using a pessimistic write lock to guarantee exclusive
 *       access during balance updates.</li>
 *   <li>Creates {@link TransactionLog} entries that capture the outcome of each operation,
 *       enabling auditability and idempotency.</li>
 *   <li>Ensures that insufficient funds do not result in a negative balance; in such cases
 *       the transaction is recorded as {@link TransactionStatus#FAILED}.</li>
 *   <li>Relies on {@link AccountRepository} and {@link TransactionLogRepository} for
 *       persistence.</li>
 * </ul>
 * <p>
 * The service is designed to be used within a Spring transactional context; each public
 * method is annotated with {@code @Transactional} to guarantee atomicity of the read‑modify‑write
 * sequence.
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
}
