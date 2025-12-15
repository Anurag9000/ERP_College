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

    public static void sendEmailStub(String recipient, String subject, String body) {
        LOGGER.info("[EMAIL-STUB] to={} subject={} body={}", recipient, subject, truncate(body));
    }

    public static void sendSmsStub(String recipient, String body) {
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
