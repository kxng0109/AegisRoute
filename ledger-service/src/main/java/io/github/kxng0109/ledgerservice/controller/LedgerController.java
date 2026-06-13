package io.github.kxng0109.ledgerservice.controller;

import io.github.kxng0109.ledgerservice.enums.TransactionStatus;
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
	 * Processes a debit transaction for the specified user.
	 *
	 * <p>This endpoint validates the request payload, applies idempotency handling using the
	 * {@code X-Idempotency-Key} header, and delegates the actual debit operation to the
	 * {@link LedgerFacadeService}. The response contains details of the processed transaction
	 * and an appropriate HTTP status code.</p>
	 *
	 * @param userId         the identifier of the user whose wallet will be debited; must correspond to an existing wallet
	 * @param idempotencyKey a unique key provided by the client to guarantee that duplicate requests
	 *                       are processed only once; required for idempotent operation handling
	 * @param request        the debit request payload containing the amount to be debited; validated
	 * @return a {@link ResponseEntity} containing a {@link DebitResponse} with transaction details.
	 * Returns HTTP 200 (OK) when the debit succeeds, or HTTP 422 (UNPROCESSABLE_CONTENT)
	 * when the transaction fails (e.g., insufficient funds or other business rule violations).
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

		if (response.status().equals(TransactionStatus.FAILED))
			return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_CONTENT);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
