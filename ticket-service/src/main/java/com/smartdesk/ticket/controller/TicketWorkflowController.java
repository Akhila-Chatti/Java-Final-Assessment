package com.smartdesk.ticket.controller;

import com.smartdesk.ticket.dto.*;
import com.smartdesk.ticket.mapper.TicketMapper;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.service.TicketWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketWorkflowController {

    private final TicketWorkflowService workflow;
    private final TicketMapper mapper;

    @PostMapping("/{id}/claim")
    public TicketResponse claim(Caller c, @PathVariable Long id) {
        return mapper.toResponse(workflow.claim(c, id));
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(Caller c, @PathVariable Long id, @Valid @RequestBody AssignTicketRequest req) {
        return mapper.toResponse(workflow.assign(c, id, req));
    }

    /** Agent's "used X of Y today". */
    @GetMapping("/claim-limit")
    public ClaimLimitResponse claimLimit(Caller c) {
        return workflow.myLimit(c);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(Caller c, @PathVariable Long id, @Valid @RequestBody StatusChangeRequest req) {
        return mapper.toResponse(workflow.changeStatus(c, id, req.status()));
    }

    @PostMapping("/{id}/verify")
    public TicketResponse verify(Caller c, @PathVariable Long id, @Valid @RequestBody VerificationRequest req) {
        return mapper.toResponse(workflow.verify(c, id, req.accepted()));
    }

    @PatchMapping("/{id}/triage")
    public TicketResponse overrideTriage(Caller c, @PathVariable Long id, @Valid @RequestBody TriageOverrideRequest req) {
        return mapper.toResponse(workflow.overrideTriage(c, id, req));
    }
}
