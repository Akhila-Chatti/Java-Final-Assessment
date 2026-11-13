package com.smartdesk.triage.service;

import com.smartdesk.triage.client.LlmClient;
import com.smartdesk.triage.dto.TriageResult;
import com.smartdesk.triage.dto.TriageResult.Category;
import com.smartdesk.triage.dto.TriageResult.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Separate bean from TriageService so @Cacheable goes through the Spring proxy. */
@Service
@RequiredArgsConstructor
public class TriageEngine {

    private final LlmClient llm;

    /**
     * Redis cache. Key = MD5 of the masked, normalised text, so identical "wifi not working" tickets cost
     * one LLM call. TTL 24h (application.yml). Exceptions are never cached.
     */
    @Cacheable(cacheNames = "triage", key = "T(org.springframework.util.DigestUtils).md5DigestAsHex(#text.getBytes())")
    public TriageResult classify(String text) {
        return llm.enabled() ? llm.classify(text) : keywordTriage(text);
    }

    // ---- demo mode (no LLM_API_KEY): simple keyword rules, same output contract ----

    private static final Map<String, Category> KEYWORDS = new LinkedHashMap<>();
    static {
        // Same guide as the LLM prompt. First keyword (in this order) found in the text wins.
        for (String k : new String[]{"wifi", "wi-fi", "vpn", "network", "internet", "laptop", "certificate", "software",
                "password", "email", "access"})
            KEYWORDS.put(k, Category.IT);
        // HR & Finance
        for (String k : new String[]{"attendance", "leave", "leaves", "payslip", "payslips", "payroll", "salary", "wfh",
                "work from home", "meal plan", "meal", "reimbursement", "benefits", "onboarding", "insurance"})
            KEYWORDS.put(k, Category.HR);
        for (String k : new String[]{"ac", "air conditioning", "air conditioner", "power", "electricity", "sanitation",
                "cleaning", "washroom", "toilet", "printer", "desk", "chair", "parking", "light", "water"})
            KEYWORDS.put(k, Category.OPERATIONS);
    }
    private static final Pattern URGENT = Pattern.compile("\\b(down|outage|urgent|not working|cannot|can't|blocked)\\b");

    static TriageResult keywordTriage(String text) {
        String hit = KEYWORDS.keySet().stream()
                .filter(k -> Pattern.compile("\\b" + k + "\\b").matcher(text).find())
                .findFirst().orElse(null);
        Category category = hit == null ? Category.IT : KEYWORDS.get(hit);
        Priority priority = URGENT.matcher(text).find() ? Priority.HIGH : Priority.MEDIUM;
        String issueKey = hit == null ? "general-request" : hit.replace(' ', '-') + "-issue";
        String summary = text.length() > 200 ? text.substring(0, 200) : text;
        return new TriageResult(category, priority, summary, issueKey);
    }
}
