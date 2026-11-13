package com.smartdesk.ticket.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {
    public InvalidStatusTransitionException(Enum<?> from, Enum<?> to) {
        super(HttpStatus.CONFLICT, "Illegal status change " + from + " -> " + to);
    }
}
