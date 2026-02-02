package main.java.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stubbed delivery service that logs email/SMS notifications.
 */
public final class NotificationDeliveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private NotificationDeliveryService() {
    }

    /**
     * Sends an email stub (logs instead of actually sending).
     * 
     * @param recipient the recipient email (must not be null or empty)
     * @param subject   the subject (must not be null)
     * @param body      the body (can be null)
     * @throws IllegalArgumentException if recipient or subject is null or empty
     */
    public static void sendEmailStub(String recipient, String subject, String body) {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient cannot be null or empty");
        }
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        LOGGER.info("[EMAIL-STUB] to={} subject={} body={}", recipient, subject, truncate(body));
    }

    /**
     * Sends an SMS stub (logs instead of actually sending).
     * 
     * @param recipient the recipient phone (must not be null or empty)
     * @param body      the body (can be null)
     * @throws IllegalArgumentException if recipient is null or empty
     */
    public static void sendSmsStub(String recipient, String body) {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient cannot be null or empty");
        }
        LOGGER.info("[SMS-STUB] to={} body={}", recipient, truncate(body));
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 200) {
            return value;
        }
        return value.substring(0, 200) + "...";
    }
}
