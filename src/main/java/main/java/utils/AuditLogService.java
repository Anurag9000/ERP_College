package main.java.utils;

import main.java.data.dao.AuditLogDao;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        private final long id;
        private final EventType type;
        private final String actor;
        private final String details;
        private final LocalDateTime timestamp;

        public AuditEvent(EventType type, String actor, String details) {
            this(0L, type, actor, details, LocalDateTime.now());
        }

        public AuditEvent(long id, EventType type, String actor, String details, LocalDateTime timestamp) {
            this.id = id;
            this.type = Objects.requireNonNull(type, "type");
            this.actor = actor;
            this.details = details;
            this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        }

        public long getId() {
            return id;
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
    private static final AuditLogDao AUDIT_LOG_DAO = new AuditLogDao();
    private static final int DEFAULT_RECENT_LIMIT = 250;

    private AuditLogService() {
    }

    public static void log(EventType type, String actor, String details) {
        AuditEvent event = new AuditEvent(type, actor, details);
        LOGGER.info("[{}] {} - {}", type, actor, details);
        AUDIT_LOG_DAO.insert(event);
    }

    public static List<AuditEvent> recentEvents() {
        return AUDIT_LOG_DAO.findRecent(DEFAULT_RECENT_LIMIT);
    }

    public static List<AuditEvent> findBetween(LocalDateTime from, LocalDateTime to) {
        return AUDIT_LOG_DAO.findRange(from, to);
    }

    public static void exportToCsv(Path path, List<AuditEvent> events) throws IOException {
        List<AuditEvent> source = events != null ? events : Collections.emptyList();
        try (Writer writer = Files.newBufferedWriter(path);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.Builder.create(CSVFormat.DEFAULT)
                             .setHeader("Timestamp", "Type", "Actor", "Details")
                             .build())) {
            for (AuditEvent event : source) {
                printer.printRecord(
                        event.getTimestamp(),
                        event.getType().name(),
                        event.getActor(),
                        event.getDetails()
                );
            }
        }
    }

    public static String toDisplayString(List<AuditEvent> events) {
        return events.stream()
                .map(event -> String.format("[%s] %-18s %-12s %s",
                        event.getTimestamp(),
                        event.getType(),
                        event.getActor() == null ? "n/a" : event.getActor(),
                        event.getDetails()))
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
