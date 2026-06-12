package io.github.kxng0109.ledgerservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(
            AccountNotFoundException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setTitle("Account Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("https://api.aegisroute.io/errors/account-not-found"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(
			InsufficientFundsException ex,
			HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_CONTENT,
            ex.getMessage()
        );
        problem.setTitle("Insufficient Funds");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("https://api.aegisroute.io/errors/insufficient-funds"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("currentBalance", ex.getCurrentBalance());
        problem.setProperty("requestedAmount", ex.getRequestedAmount());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ProblemDetail handleDuplicateTransaction(
			DuplicateTransactionException ex,
			HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setTitle("Duplicate Transaction");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("https://api.aegisroute.io/errors/duplicate-transaction"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("referenceId", ex.getReferenceId());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(LockAcquisitionException.class)
    public ProblemDetail handleLockAcquisition(
			LockAcquisitionException ex,
			HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Unable to process request due to high concurrency. Please retry."
        );
        problem.setTitle("Service Temporarily Unavailable");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setType(URI.create("https://api.aegisroute.io/errors/lock-acquisition"));
        problem.setProperty("errorCode", "LOCK_ACQUISITION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
