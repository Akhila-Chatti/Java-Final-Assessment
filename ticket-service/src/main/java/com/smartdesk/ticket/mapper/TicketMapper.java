package com.smartdesk.ticket.mapper;

import com.smartdesk.ticket.dto.TicketResponse;
import com.smartdesk.ticket.entity.AiAnalysis;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketAssignment;

public final class TicketMapper {

    private TicketMapper() {}

    public static TicketResponse toResponse(Ticket t) {
        AiAnalysis a = t.getAnalysis();          // always present: created with the ticket
        TicketAssignment as = t.getAssignment(); // null until claimed/assigned
        return new TicketResponse(t.getTicketId(), t.getCode(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getDepartmentName(), a.getCategory(), a.getPriority(), a.getAiSummary(), a.getSource(),
                t.getCreatedBy(), t.getRequesterName(),
                as == null ? null : as.getUserId(), as == null ? null : as.getAgentName(),
                t.getParentTicketId(), t.getReportCount(), t.getCreatedAt(), t.getResolvedAt());
    }
}
