package com.smartdesk.ticket.dto;

import java.util.Map;

public record ManagerSummaryResponse(Map<String, Long> byStatus, Map<String, Long> byCategory, Double avgResolutionHours) {}
