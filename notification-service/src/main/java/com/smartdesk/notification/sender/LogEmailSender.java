package com.smartdesk.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                =================== OUTGOING EMAIL SIMULATION ===================
                TO: {}
                SUBJECT: {}
                BODY: {}
                TIMESTAMP: {}
                ================================================================""", to, subject, body, Instant.now());
    }
}
