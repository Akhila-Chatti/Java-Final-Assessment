package com.smartdesk.triage.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PiiMaskerTest {

    @Test
    void masksPersonalData() {
        String out = PiiMasker.mask("Ravi Kumar here, mail ravi.k@corp.com or call +91 98480 22338, EMP-10423. Regards, Ravi Kumar",
                List.of("Ravi Kumar"));
        assertFalse(out.contains("ravi.k@corp.com"));
        assertFalse(out.contains("98480"));
        assertFalse(out.contains("10423"));
        assertFalse(out.toLowerCase().contains("ravi"));
    }

    @Test
    void keepsTheProblem() {
        assertEquals("wifi down on floor 3", PiiMasker.mask("wifi down on floor 3", List.of()));
    }
}
