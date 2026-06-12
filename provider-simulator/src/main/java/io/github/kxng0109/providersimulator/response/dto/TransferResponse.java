package io.github.kxng0109.providersimulator.response.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Response DTO representing the outcome of a transfer operation.
 *
 * <p>This record is returned by the provider's {@code /api/v1/provider/transfer}
 * endpoint when a transfer request is processed successfully. It contains a
 * unique identifier for the transaction and a status string describing the
 * result of the operation.</p>
 *
 * <ul>
 *   <li>{@code transactionId} – a {@link UUID} generated for the
 *   completed transaction.</li>
 *   <li>{@code status} – a textual indicator of the transaction state, for
 *   example {@code "COMPLETED"}.</li>
 * </ul>
 * <p>
 * The class is immutable and is constructed via the generated Lombok
 * {@code builder()} method.
 */
@Builder
public record TransferResponse(
		UUID transactionId,
		String status
) {
}
