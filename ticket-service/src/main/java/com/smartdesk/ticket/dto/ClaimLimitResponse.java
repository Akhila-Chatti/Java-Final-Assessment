package com.smartdesk.ticket.dto;

/** What the agent UI shows: "3 of 7 tickets taken today". */
public record ClaimLimitResponse(long used, int limit, long remaining, long departmentInflowToday, long activeAgents) {}
