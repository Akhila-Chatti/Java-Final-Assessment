package com.smartdesk.notification.sender;

/** Swap LogEmailSender for a JavaMailSender-backed bean once SMTP credentials exist; nothing else changes. */
public interface EmailSender {
    void send(String to, String subject, String body);
}
