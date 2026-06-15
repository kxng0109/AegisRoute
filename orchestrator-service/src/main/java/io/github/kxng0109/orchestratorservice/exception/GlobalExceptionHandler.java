package io.github.kxng0109.orchestratorservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles 503 Service Unavailable errors from ledger-service or provider.
	 * This occurs when the downstream service is temporarily unavailable or overloaded.
	 */
	@ExceptionHandler(HttpServerErrorException.ServiceUnavailable.class)
	public ResponseEntity<ProblemDetail> handleServiceUnavailable(
			HttpServerErrorException.ServiceUnavailable ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.SERVICE_UNAVAILABLE,
				"A downstream service is temporarily unavailable. Please retry."
		);
		problem.setTitle("Service Temporarily Unavailable");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("errorCode", "SERVICE_UNAVAILABLE");
		problem.setProperty("timestamp", Instant.now());

		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.header("Retry-After", "5")  // Suggest retry after 5 seconds
				.body(problem);
	}

	/**
	 * Handles 504 Gateway Timeout errors from provider.
	 * This occurs when the provider takes too long to respond.
	 */
	@ExceptionHandler(HttpServerErrorException.GatewayTimeout.class)
	public ProblemDetail handleGatewayTimeout(
			HttpServerErrorException.GatewayTimeout ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.GATEWAY_TIMEOUT,
				"The request timed out. Please retry."
		);
		problem.setTitle("Gateway Timeout");
		problem.setProperty("errorCode", "GATEWAY_TIMEOUT");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

	/**
	 * Handles all other 5xx server errors from RestClient.
	 */
	@ExceptionHandler(HttpServerErrorException.class)
	public ProblemDetail handleServerError(
			HttpServerErrorException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_GATEWAY,
				"An upstream service returned an error. Please retry."
		);
		problem.setTitle("Bad Gateway");
		problem.setProperty("errorCode", "BAD_GATEWAY");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("statusCode", ex.getStatusCode().value());
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}
}
