package com.smartdesk.ticket.dto.client;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Priority;
import com.smartdesk.ticket.entity.TriageSource;

/** AI result, or the fallback used when the AI is unavailable. */
public record TriageOutcome(Department category, Priority priority, String summary, String issueKey, TriageSource source) {

    public static TriageOutcome fallback(String title) {
        return new TriageOutcome(Department.UNCLASSIFIED, Priority.MEDIUM, title, null, TriageSource.FALLBACK);
    }

    public boolean failed() {
        return source == TriageSource.FALLBACK;
    }
}
