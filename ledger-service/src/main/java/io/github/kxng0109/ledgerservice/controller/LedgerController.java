package io.github.kxng0109.ledgerservice.controller;

import io.github.kxng0109.ledgerservice.request.dto.DebitRequest;
import io.github.kxng0109.ledgerservice.response.dto.DebitResponse;
import io.github.kxng0109.ledgerservice.service.LedgerFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class LedgerController {

	private final LedgerFacadeService ledgerFacadeService;

	/**
	 * Handles a debit request for the specified user.
	 *
	 * <p>This endpoint debits the amount provided in the {@link DebitRequest}
	 * from the user's wallet. The request must include an {@code X-Idempotency-Key}
	 * header to ensure that duplicate requests are not processed multiple times.
	 * The method delegates the actual processing to {@link LedgerFacadeService#handleDebit}
	 * and returns the resulting {@link DebitResponse} wrapped in an HTTP 200 response.</p>
	 *
	 * @param userId         the identifier of the user whose wallet will be debited; must not be {@code null}
	 * @param idempotencyKey a unique key supplied in the {@code X-Idempotency-Key} header to guarantee idempotent processing; must not be {@code null}
	 * @param request        the validated debit request payload containing the amount to be debited; must not be {@code null}
	 * @return a {@link ResponseEntity} containing a {@link DebitResponse} with details of the processed transaction and an HTTP status of {@code 200 OK}
	 */
	@PostMapping("/{userId}/debit")
	public ResponseEntity<DebitResponse> debit(
			@PathVariable String userId,
			@RequestHeader(value = "X-Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody DebitRequest request
	) {
		DebitResponse response = ledgerFacadeService.handleDebit(
				userId,
				idempotencyKey,
				request.amount()
		);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
