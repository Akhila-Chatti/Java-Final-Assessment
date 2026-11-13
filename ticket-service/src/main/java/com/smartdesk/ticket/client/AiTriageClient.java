package com.smartdesk.ticket.client;

import com.smartdesk.ticket.dto.client.TriageRequest;
import com.smartdesk.ticket.dto.client.TriageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Resolved through Eureka; timeouts in application.yml. Always call it via TriageGateway (circuit breaker). */
@FeignClient(name = "ai-triage-service")
public interface AiTriageClient {

    @PostMapping("/api/v1/triage")
    TriageResponse triage(@RequestBody TriageRequest request);
}
