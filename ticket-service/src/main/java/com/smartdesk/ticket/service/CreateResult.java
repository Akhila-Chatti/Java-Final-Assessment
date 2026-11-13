package com.smartdesk.ticket.service;

import com.smartdesk.ticket.entity.Ticket;

/** created=false means the Idempotency-Key was already used and this is the original ticket. */
public record CreateResult(Ticket ticket, boolean created) {}
