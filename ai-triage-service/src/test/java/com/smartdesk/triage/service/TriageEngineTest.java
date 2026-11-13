package com.smartdesk.triage.service;

import com.smartdesk.triage.dto.TriageResult.Category;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Demo-mode routing follows the same category guide as the LLM prompt. Text arrives lower-cased. */
class TriageEngineTest {

    @ParameterizedTest
    @CsvSource({
            "my attendance is not marked for monday, HR",
            "need to apply for 3 leaves next week, HR",
            "payslip for october is missing, HR",
            "requesting wfh for two days, HR",
            "i want to opt for the meal plan, HR",
            "laptop is very slow, IT",
            "wifi keeps dropping on floor 3, IT",
            "need a certificate issued for the vpn client, IT",
            "software install fails with an error, IT",
            "ac is not cooling in meeting room 2, OPERATIONS",
            "power issue in the east wing, OPERATIONS",
            "sanitation issue in the 2nd floor washroom, OPERATIONS"
    })
    void routesByCategoryGuide(String text, Category expected) {
        assertEquals(expected, TriageEngine.keywordTriage(text).category());
    }
}
