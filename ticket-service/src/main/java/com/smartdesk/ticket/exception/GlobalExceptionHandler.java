package com.smartdesk.ticket.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Validation errors (400) are rendered by Spring Boot's built-in ProblemDetail handler. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        e.enrich(problem);
        return problem;
    }

    /** A unique constraint (e.g. ticket_assignments.ticket_id: two agents claimed at once): someone else changed it first. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConcurrentChange() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The ticket was changed by someone else (e.g. already claimed). Refresh and retry.");
    }
}
