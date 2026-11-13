package com.smartdesk.ticket.client;

import com.smartdesk.ticket.dto.client.TriageOutcome;
import com.smartdesk.ticket.dto.client.TriageRequest;
import com.smartdesk.ticket.dto.client.TriageResponse;
import com.smartdesk.ticket.entity.TriageSource;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI call behind a Resilience4j circuit breaker. Timeout, 5xx, invalid LLM output (502) or an open breaker
 * all land in the fallback, so creating a ticket never fails because of the AI.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TriageGateway {

    private final AiTriageClient client;

    @CircuitBreaker(name = "aiTriage", fallbackMethod = "unclassified")
    public TriageOutcome triage(String title, String description, String requesterName) {
        TriageResponse r = client.triage(new TriageRequest(title, description, requesterName == null ? List.of() : List.of(requesterName)));
        return new TriageOutcome(r.category(), r.priority(), r.summary(), r.issueKey(), TriageSource.AI);
    }

    private TriageOutcome unclassified(String title, String description, String requesterName, Throwable cause) {
        log.warn("AI triage unavailable, saving ticket as UNCLASSIFIED: {}", cause.toString());
        return TriageOutcome.fallback(title);
    }
}
