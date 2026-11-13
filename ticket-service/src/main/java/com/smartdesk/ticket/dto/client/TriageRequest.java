package com.smartdesk.ticket.dto.client;

import java.util.List;

/** knownNames = the requester's name, masked by the AI service before the text leaves for the LLM. */
public record TriageRequest(String title, String description, List<String> knownNames) {}
