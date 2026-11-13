package com.smartdesk.ticket.service;

import com.smartdesk.ticket.client.TriageGateway;
import com.smartdesk.ticket.dto.client.TriageOutcome;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Tickets saved as UNCLASSIFIED while the AI was down are classified once it is back. */
@Component
@RequiredArgsConstructor
public class RetriageJob {

    private final TicketRepository tickets;
    private final TicketAccess access;
    private final TriageGateway triage;
    private final TransactionTemplate tx;

    @Scheduled(fixedDelayString = "${smartdesk.retriage-interval}")
    public void run() {
        for (Ticket pending : tickets.findTop20ByNeedsRetriageTrueAndStatus(TicketStatus.OPEN)) {
            TriageOutcome r = triage.triage(pending.getTitle(), pending.getDescription(), pending.getRequesterName());
            if (r.failed()) return; // AI still down: next cycle
            tx.executeWithoutResult(s -> {
                Ticket t = access.load(pending.getTicketId());
                if (t.getStatus() == TicketStatus.OPEN && t.isNeedsRetriage())
                    t.applyTriage(r.category(), r.priority(), r.summary(), r.issueKey(), r.source());
            });
        }
    }
}
