package com.smartdesk.ticket.dto.client;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Priority;

public record TriageResponse(Department category, Priority priority, String summary, String issueKey) {}
