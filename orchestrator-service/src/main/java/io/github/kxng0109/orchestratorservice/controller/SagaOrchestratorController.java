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

	@PostMapping("/transfer")
	public ResponseEntity<UUID> handleTransfer(
			@Valid @RequestBody SagaTransferRequest request
	){
		return new ResponseEntity<>(
				orchestratorService.processTransfer(request),
				HttpStatus.CREATED
		);
	}
}
