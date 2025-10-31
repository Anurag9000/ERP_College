package main.java.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures security-sensitive events for auditing.
 */
public final class AuditLogService {
    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        ACCOUNT_LOCKED,
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        MAINTENANCE_TOGGLE,
        ENROLLMENT_CHANGE,
        GRADE_EDIT
    }

    public static final class AuditEvent {
        private final EventType type;
        private final String actor;
        private final String details;
        private final LocalDateTime timestamp;

        public AuditEvent(EventType type, String actor, String details) {
            this.type = type;
            this.actor = actor;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }

        public EventType getType() {
            return type;
        }

        public String getActor() {
            return actor;
        }

        public String getDetails() {
            return details;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("AUDIT");
    private static final List<AuditEvent> EVENTS = Collections.synchronizedList(new ArrayList<>());

    private AuditLogService() {
    }

    public static void log(EventType type, String actor, String details) {
        AuditEvent event = new AuditEvent(type, actor, details);
        EVENTS.add(event);
        LOGGER.info("[{}] {} - {}", type, actor, details);
    }

    public static List<AuditEvent> recentEvents() {
        return new ArrayList<>(EVENTS);
    }

    public static void clear() {
        EVENTS.clear();
    }
}
