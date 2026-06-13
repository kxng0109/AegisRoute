package io.github.kxng0109.orchestratorservice.controller;

import io.github.kxng0109.orchestratorservice.request.dto.SagaTransferRequest;
import io.github.kxng0109.orchestratorservice.service.SagaOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class SagaOrchestratorController {

	private final SagaOrchestratorService orchestratorService;

	/**
	 * Initiates a money transfer saga based on the supplied {@link SagaTransferRequest}.
	 * <p>
	 * The request is validated and then handed over to the {@code SagaOrchestratorService}
	 * which creates a new saga state, attempts to debit the user's ledger account, and
	 * forwards the transfer to an external provider.  The method returns the unique
	 * identifier of the saga instance that was created or updated during processing.
	 *
	 * @param request a validated {@code SagaTransferRequest} containing the user ID,
	 *                destination account details, and transfer amount
	 * @return a {@link ResponseEntity} wrapping the {@link UUID} that uniquely
	 * identifies the initiated transfer saga; the response status is {@code 201 CREATED}
	 */
	@PostMapping("/transfer")
	public ResponseEntity<UUID> handleTransfer(
			@Valid @RequestBody SagaTransferRequest request
	) {
		return new ResponseEntity<>(
				orchestratorService.processTransfer(request),
				HttpStatus.CREATED
		);
	}
}
