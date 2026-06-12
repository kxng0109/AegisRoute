package io.github.kxng0109.ledgerservice.service;

import io.github.kxng0109.ledgerservice.entity.TransactionLog;
import io.github.kxng0109.ledgerservice.exception.DuplicateTransactionException;
import io.github.kxng0109.ledgerservice.repository.TransactionLogRepository;
import io.github.kxng0109.ledgerservice.response.dto.DebitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Facade service that orchestrates debit operations ensuring idempotency and
 * concurrency safety.
 *
 * <p>This service delegates the core debit processing to {@link CoreLedgerService}
 * while managing idempotency keys and a Redis‑based lock. The workflow is:</p>
 *
 * <ul>
 *   <li>Check the {@link TransactionLogRepository} for an existing transaction
 *       with the supplied idempotency key.</li>
 *   <li>If found, return a {@link DebitResponse} built from the existing log.</li>
 *   <li>If not found, attempt to acquire a Redis lock uniquely identified by
 *       {@code "idempotency_lock:" + idempotencyKey}.</li>
 *   <li>If the lock cannot be obtained, throw {@link DuplicateTransactionException}
 *       indicating another request with the same key is in progress.</li>
 *   <li>When the lock is held, invoke {@link CoreLedgerService#processDebit(String, String, BigDecimal)}
 *       to create a new transaction.</li>
 *   <li>Transform the resulting {@link TransactionLog} into a {@link DebitResponse}
 *       and release the Redis lock in a {@code finally} block.</li>
 * </ul>
 *
 * <p>All interactions are performed within the same Spring transaction context,
 * and the Redis lock is automatically cleaned up to prevent stale locks.</p>
 */
@Service
@RequiredArgsConstructor
public class LedgerFacadeService {

	private final StringRedisTemplate stringRedisTemplate;
	private final TransactionLogRepository transactionLogRepository;
	private final CoreLedgerService coreLedgerService;

	/**
	 * Processes a debit request for a specific user while ensuring idempotency.
	 *
	 * <p>The method first checks if a transaction with the provided {@code idempotencyKey}
	 * already exists in the database. If such a transaction is found, its corresponding
	 * {@link DebitResponse} is returned immediately.</p>
	 *
	 * <p>If no existing transaction is found, the method attempts to acquire a Redis lock
	 * identified by {@code "idempotency_lock:" + idempotencyKey}. Acquiring this lock
	 * guarantees that concurrent requests with the same idempotency key are serialized.
	 * If the lock cannot be obtained, a {@link DuplicateTransactionException} is thrown,
	 * indicating that another request with the same key is currently being processed.</p>
	 *
	 * <p>Once the lock is obtained, the method delegates the actual debit operation to
	 * {@link CoreLedgerService#processDebit(String, String, BigDecimal)}. The
	 * resulting {@link TransactionLog} is transformed into a {@link DebitResponse}
	 * using {@link #constructDebitResponse(TransactionLog)}. The Redis lock is released
	 * in a {@code finally} block to ensure it is removed regardless of success or failure.</p>
	 *
	 * @param userId         the identifier of the user whose account will be debited; must not be {@code null}
	 * @param idempotencyKey a unique key used to guarantee that the same debit request is not processed more than once; must not be {@code null}
	 * @param amount         the monetary amount to debit; must be a positive {@link BigDecimal}; must not be {@code null}
	 * @return a {@link DebitResponse} containing details of the processed debit transaction,
	 * either retrieved from an existing log or created by the core ledger service
	 * @throws DuplicateTransactionException if a transaction with the same {@code idempotencyKey}
	 *                                       is already being processed
	 */
	public DebitResponse handleDebit(
			String userId,
			String idempotencyKey,
			BigDecimal amount
	) {
		// check the db if the transaction log exists
		Optional<TransactionLog> transactionLog = transactionLogRepository.findByReferenceId(idempotencyKey);
		// If it exists, then return it
		if (transactionLog.isPresent()) {
			return constructDebitResponse(transactionLog.get());
		}

		String redisLockKey = "idempotency_lock:" + idempotencyKey;

		// Try and acquire the redis lock
		Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(
				redisLockKey,
				"LOCKED",
				Duration.ofMinutes(1)
		);

		// IF we can't then that means the transaction is currently ongoing
		if (Boolean.FALSE.equals(lockAcquired)) throw new DuplicateTransactionException(
				"Transaction is currently processing. Please try again.",
				idempotencyKey
		);

		// We have obtained the redis lock
		// wrapped in a try...finally block so that no matter what happens in the coreLedgerService,
		// the redis lock gets deleted
		try {
			// let's call the coreLedgerService to handle the actual transaction
			return constructDebitResponse(
					coreLedgerService.processDebit(
							userId,
							idempotencyKey,
							amount
					)
			);
		} finally {
			// Redis lock gets deleted no matter what,
			// but only after the coreLedgerService is done
			stringRedisTemplate.delete(redisLockKey);
		}
	}

	/**
	 * Creates a {@link DebitResponse} DTO from the given {@link TransactionLog}.
	 *
	 * @param transactionLog the transaction log containing the details of the debit operation;
	 *                       must not be {@code null}
	 * @return a {@code DebitResponse} populated with the transaction identifier, reference identifier,
	 * amount, status, and creation timestamp extracted from {@code transactionLog}
	 */
	private DebitResponse constructDebitResponse(TransactionLog transactionLog) {
		return DebitResponse.builder()
		                    .transactionId(transactionLog.getId())
		                    .referenceId(transactionLog.getReferenceId())
		                    .amount(transactionLog.getAmount())
		                    .status(transactionLog.getStatus())
		                    .timestamp(transactionLog.getCreatedAt())
		                    .build();
	}
}
