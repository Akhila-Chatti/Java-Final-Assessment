package com.smartdesk.triage.util;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/** Removes personal data before ticket text leaves the company for a third-party LLM. */
public final class PiiMasker {

    private record Rule(Pattern pattern, String replacement) {}

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), "[EMAIL]"),
            new Rule(Pattern.compile("\\b(?i:emp|empid|id|acc)[-:# ]?\\d{3,}\\b"), "[ID]"),
            new Rule(Pattern.compile("\\+?\\d[\\d -]{6,}\\d"), "[NUMBER]"), // phone, account, Aadhaar
            new Rule(Pattern.compile("((?i:my name is|regards,?|thanks,?|thank you,?))\\s+\\p{Lu}\\p{Ll}+(\\s+\\p{Lu}\\p{Ll}+)?"), "$1 [NAME]"));

    private PiiMasker() {}

    public static String mask(String text, Collection<String> knownNames) {
        String out = text;
        for (Rule rule : RULES) out = rule.pattern().matcher(out).replaceAll(rule.replacement());
        for (String name : knownNames)
            for (String part : name.split("\\s+"))
                if (part.length() > 2)
                    out = Pattern.compile("\\b" + Pattern.quote(part) + "\\b", Pattern.CASE_INSENSITIVE).matcher(out).replaceAll("[NAME]");
        return out;
    }
}
