package com.smartdesk.ticket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class DailyLimitExceededException extends ApiException {

    private final long used;
    private final int limit;

    public DailyLimitExceededException(long used, int limit) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Daily ticket limit reached (%d/%d). It rises automatically as more tickets arrive today."
                .formatted(used, limit));
        this.used = used;
        this.limit = limit;
    }

    @Override
    public void enrich(ProblemDetail problem) {
        problem.setProperty("used", used);
        problem.setProperty("limit", limit);
    }
}
