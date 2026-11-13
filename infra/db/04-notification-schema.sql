\connect smartdesk_notification_db

-- =====================================================================
-- NOTIFICATION DATABASE  (owner: notification-service)
-- =====================================================================

-- Audit log of every (simulated) email
CREATE TABLE notifications (
    notification_id    UUID         PRIMARY KEY,
    event_id           UUID         NOT NULL,                     -- idempotency key from the sender
    recipient_user_id  UUID         NOT NULL,
    recipient_email    VARCHAR(255) NOT NULL,
    ticket_id          BIGINT,                                    -- tickets.ticket_id (ticket DB, logical link)
    event_type         VARCHAR(40)  NOT NULL,
    subject            VARCHAR(255) NOT NULL,
    message            TEXT         NOT NULL,                     -- OTP bodies stored as '[redacted]'
    delivery_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ,
    CONSTRAINT uk_notifications_event  UNIQUE (event_id),         -- a retried event is sent once
    CONSTRAINT ck_notifications_type   CHECK (event_type IN ('OTP', 'TICKET_CREATED', 'TICKET_ASSIGNED', 'STATUS_CHANGED', 'VERIFICATION_REQUESTED')),
    CONSTRAINT ck_notifications_status CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'))
);
CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_user_id, created_at);   -- "my notifications"
