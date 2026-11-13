package com.smartdesk.triage.service;

import com.smartdesk.triage.dto.TriageRequest;
import com.smartdesk.triage.dto.TriageResult;
import com.smartdesk.triage.util.PiiMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TriageService {

    private final TriageEngine engine;

    /** mask PII -> normalise -> cache or LLM. Normalising raises cache hits and keeps PII out of Redis. */
    public TriageResult triage(TriageRequest req) {
        String masked = PiiMasker.mask(req.title() + "\n" + req.description(), req.knownNames() == null ? List.of() : req.knownNames());
        return engine.classify(masked.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip());
    }
}
