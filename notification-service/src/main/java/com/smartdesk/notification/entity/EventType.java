package com.smartdesk.notification.entity;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** All message templates in one place; {placeholders} come from the event's params. */
public enum EventType {
    OTP("Your SmartDesk login code",
            "Hello {name}, your one-time login code is {otp}. It expires in 5 minutes.", true),
    TICKET_CREATED("Ticket received: {code}",
            "Hello {name}, your ticket \"{title}\" was created. Category: {category}, priority: {priority}.{note}", false),
    TICKET_ASSIGNED("Ticket assigned: {code}",
            "Hello {name}, ticket \"{title}\" (priority {priority}) is now in your queue.", false),
    STATUS_CHANGED("Ticket status update: {code}",
            "Hello {name}, ticket \"{title}\" changed to {status} by {actor}.", false),
    VERIFICATION_REQUESTED("Please confirm the fix: {code}",
            "Hello {name}, {actor} marked \"{title}\" as fixed. Confirm it in SmartDesk, or reject it if the problem is still there.", false);

    private static final Pattern VAR = Pattern.compile("\\{(\\w+)}");

    private final String subject;
    private final String body;
    private final boolean sensitive; // OTP: emailed, but never stored in the audit table

    EventType(String subject, String body, boolean sensitive) {
        this.subject = subject;
        this.body = body;
        this.sensitive = sensitive;
    }

    public String subject(Map<String, String> params) {
        return fill(subject, params);
    }

    public String body(Map<String, String> params) {
        return fill(body, params);
    }

    public boolean isSensitive() {
        return sensitive;
    }

    private static String fill(String template, Map<String, String> params) {
        return VAR.matcher(template).replaceAll(m -> Matcher.quoteReplacement(params.getOrDefault(m.group(1), "")));
    }
}
