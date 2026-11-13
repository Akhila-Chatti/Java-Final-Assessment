package com.smartdesk.triage.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/** Any OpenAI-compatible chat API (OpenAI, NVIDIA NIM...). Fixed URL, not Eureka; timeouts in application.yml. */
@FeignClient(name = "llm", url = "${llm.base-url}")
public interface LlmApi {

    @PostMapping("/chat/completions")
    JsonNode chat(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearer, @RequestBody Map<String, Object> body);
}
