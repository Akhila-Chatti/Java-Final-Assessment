package com.smartdesk.triage.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** knownNames = the requester's name; masked before the text goes to the LLM. */
public record TriageRequest(@NotBlank String title, @NotBlank String description, List<String> knownNames) {}
