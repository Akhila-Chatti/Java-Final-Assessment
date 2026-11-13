package com.smartdesk.ticket.controller;

import com.smartdesk.ticket.dto.ManagerSummaryResponse;
import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaries;

    @GetMapping("/api/v1/tickets/manager-summary")
    public ManagerSummaryResponse summary(Caller c, @RequestParam(required = false) Department department) {
        return summaries.summary(c, department);
    }
}
