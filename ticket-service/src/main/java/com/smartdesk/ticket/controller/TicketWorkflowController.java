package com.smartdesk.ticket.controller;

import com.smartdesk.ticket.dto.*;
import com.smartdesk.ticket.mapper.TicketMapper;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.service.TicketWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketWorkflowController {

    private final TicketWorkflowService workflow;

    @PostMapping("/{id}/claim")
    public TicketResponse claim(Caller c, @PathVariable UUID id) {
        return TicketMapper.toResponse(workflow.claim(c, id));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(Caller c, @PathVariable UUID id, @Valid @RequestBody AssignTicketRequest req) {
        return TicketMapper.toResponse(workflow.assign(c, id, req));
    }

    /** Agent's "used X of Y today". */
    @GetMapping("/claim-limit")
    public ClaimLimitResponse claimLimit(Caller c) {
        return workflow.myLimit(c);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(Caller c, @PathVariable UUID id, @Valid @RequestBody StatusChangeRequest req) {
        return TicketMapper.toResponse(workflow.changeStatus(c, id, req.status()));
    }

    @PostMapping("/{id}/verify")
    public TicketResponse verify(Caller c, @PathVariable UUID id, @Valid @RequestBody VerificationRequest req) {
        return TicketMapper.toResponse(workflow.verify(c, id, req.accepted()));
    }

    @PatchMapping("/{id}/triage")
    public TicketResponse overrideTriage(Caller c, @PathVariable UUID id, @Valid @RequestBody TriageOverrideRequest req) {
        return TicketMapper.toResponse(workflow.overrideTriage(c, id, req));
    }
}
