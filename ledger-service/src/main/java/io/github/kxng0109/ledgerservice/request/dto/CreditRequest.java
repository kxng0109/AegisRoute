package io.github.kxng0109.ledgerservice.request.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Represents a request to credit a specific amount to a user's wallet.
 * <p>
 * This record is used as the payload of the {@code POST /api/v1/wallets/{userId}/credit}
 * endpoint. It contains the identifier of the user whose wallet will be credited,
 * an idempotency key that enables safe retries without creating duplicate transactions,
 * and the monetary amount to be added.
 *
 * @param userId         the unique identifier of the user whose wallet will receive the credit
 * @param idempotencyKey a client‑generated key that guarantees the request is processed
 *                       only once; subsequent requests with the same key must produce the same result
 * @param amount         the amount to credit; must be a positive monetary value expressed with
 *                       up to two decimal places
 */
@Builder
public record CreditRequest(
		String userId,
		String idempotencyKey,
		BigDecimal amount
) {
}
