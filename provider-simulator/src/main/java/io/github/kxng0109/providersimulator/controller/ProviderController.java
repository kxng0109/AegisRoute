package io.github.kxng0109.providersimulator.controller;

import io.github.kxng0109.providersimulator.request.dto.TransferRequest;
import io.github.kxng0109.providersimulator.response.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REST controller that simulates a financial provider's transfer service.
 *
 * <p>All endpoints are prefixed with {@code /api/v1/provider}. The controller
 * currently exposes a single operation that accepts a {@link TransferRequest}
 * and returns a {@link TransferResponse} wrapped in a {@link ResponseEntity}.
 * The implementation intentionally introduces random outcomes to emulate
 * varying service conditions such as successful processing, temporary
 * unavailability, and gateway timeouts.</p>
 *
 * <p>Clients should be prepared to handle the following HTTP responses:</p>
 * <ul>
 *   <li><strong>200 OK</strong> – the transfer was processed successfully and the
 *   response body contains a {@link TransferResponse} with a generated
 *   transaction identifier and a status of {@code "COMPLETED"}.</li>
 *   <li><strong>503 SERVICE_UNAVAILABLE</strong> – the simulated provider is
 *   currently unavailable. A {@link ResponseStatusException} is thrown with
 *   this status.</li>
 *   <li><strong>504 GATEWAY_TIMEOUT</strong> – the simulated provider did not
 *   respond within the expected time frame. A {@link ResponseStatusException}
 *   is thrown with this status.</li>
 * </ul>
 *
 * <p>The randomness and artificial delay are for demonstration purposes only
 * and should be replaced with real integration logic in production.</p>
 */
@RestController
@RequestMapping("/api/v1/provider")
public class ProviderController {

	/**
	 * Handles a transfer request submitted to the {@code /api/v1/provider/transfer} endpoint.
	 *
	 * <p>The method validates the incoming {@link TransferRequest} payload, then simulates
	 * processing with a random outcome:</p>
	 * <ul>
	 *   <li>60% chance – the request is processed successfully after a short delay; a
	 *   {@link TransferResponse} containing a generated transaction ID and a status of
	 *   {@code "COMPLETED"} is returned with HTTP 200.</li>
	 *   <li>20% chance – a {@link ResponseStatusException} with status {@code 503
	 *   SERVICE_UNAVAILABLE} is thrown, indicating the service is temporarily unavailable.</li>
	 *   <li>20% chance – a {@link ResponseStatusException} with status {@code 504
	 *   GATEWAY_TIMEOUT} is thrown, indicating a timeout condition.</li>
	 * </ul>
	 *
	 * @param transferRequest the validated transfer request payload containing account
	 *                        details and amount to be transferred
	 * @return a {@link ResponseEntity} wrapping a {@link TransferResponse} when the
	 * simulated processing succeeds
	 * @throws ResponseStatusException if the simulated outcome results in service
	 *                                 unavailability (HTTP 503) or a gateway timeout (HTTP 504)
	 * @throws RuntimeException        if the thread sleep is unexpectedly interrupted
	 */
	@PostMapping("/transfer")
	public ResponseEntity<TransferResponse> handleTransferRequest(
			@Valid @RequestBody TransferRequest transferRequest
	) {
		int chance = ThreadLocalRandom.current().nextInt(1, 101);
		if (chance <= 60) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			TransferResponse response = TransferResponse.builder()
			                                            .transactionId(UUID.randomUUID())
			                                            .status("COMPLETED")
			                                            .build();
			return ResponseEntity.ok(response);
		} else if (chance <= 80) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable. Try again later.");
		} else {
			throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout. Try again later.");
		}
	}
}
