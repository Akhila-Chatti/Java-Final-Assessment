package com.smartdesk.triage.controller;

import com.smartdesk.triage.dto.TriageRequest;
import com.smartdesk.triage.dto.TriageResult;
import com.smartdesk.triage.service.TriageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal: only the Ticket Service calls this (the gateway does not route it). */
@RestController
@RequestMapping("/api/v1/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triage;

    @PostMapping
    public TriageResult triage(@Valid @RequestBody TriageRequest req) {
        return triage.triage(req);
    }
}
