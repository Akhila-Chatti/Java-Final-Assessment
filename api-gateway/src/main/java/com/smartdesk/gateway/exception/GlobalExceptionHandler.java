package com.smartdesk.gateway.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/** Error JSON carries status, error, message and timestamp (INTEGRATION_SPEC), on top of the standard ProblemDetail fields. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // before Spring's built-in ProblemDetail handler for validation errors
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException e) {
        return problem(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = new TreeMap<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) details.putIfAbsent(f.getField(), f.getDefaultMessage());
        ProblemDetail p = problem(HttpStatus.BAD_REQUEST, "Validation failed");
        p.setProperty("details", details);
        return p;
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleNotificationDown() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Could not deliver the OTP, please retry");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicate() {
        return problem(HttpStatus.BAD_REQUEST, "An active account already exists with this email address. Please sign in instead.");
    }

    private static ProblemDetail problem(HttpStatus status, String message) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, message);
        p.setProperty("error", status.getReasonPhrase());
        p.setProperty("message", message);
        p.setProperty("timestamp", LocalDateTime.now());
        return p;
    }
}
