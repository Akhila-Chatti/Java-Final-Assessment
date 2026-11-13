package com.smartdesk.triage.dto;

import com.smartdesk.triage.exception.InvalidLlmOutputException;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * The only shape the LLM may produce. Validated in the constructor, so an invalid result
 * cannot exist: it is never cached and never returned.
 */
public record TriageResult(Category category, Priority priority, String summary, String issueKey) implements Serializable {

    public enum Category { IT, HR, OPERATIONS }

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    private static final Pattern ISSUE_KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+){0,3}");

    public TriageResult {
        if (category == null || priority == null) throw new InvalidLlmOutputException("category and priority are required");
        if (summary == null || summary.isBlank() || summary.length() > 300) throw new InvalidLlmOutputException("summary must be 1-300 characters");
        if (issueKey == null || !ISSUE_KEY.matcher(issueKey).matches()) throw new InvalidLlmOutputException("issueKey must look like 'wifi-outage'");
    }
}
