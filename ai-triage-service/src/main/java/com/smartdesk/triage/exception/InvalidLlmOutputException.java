package com.smartdesk.triage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 502: the LLM answered, but not in the agreed JSON shape. The Ticket Service then uses its fallback. */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class InvalidLlmOutputException extends RuntimeException {
    public InvalidLlmOutputException(String message) {
        super(message);
    }
}
