-- SmartDesk: one database per microservice (database-per-service pattern).
-- Services never read each other's tables; cross-service ids are plain UUIDs without foreign keys.
CREATE DATABASE smartdesk_auth_db;
CREATE DATABASE smartdesk_ticket_db;
CREATE DATABASE smartdesk_notification_db;
