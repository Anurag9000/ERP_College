package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.utils.AuditLogService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for persisting and querying audit events.
 */
public class AuditLogDao extends BaseDao {
    private static final String INSERT_SQL =
            "INSERT INTO audit_events (event_type, actor, details, created_at) VALUES (?, ?, ?, ?)";
    private static final String SELECT_RECENT_SQL =
            "SELECT id, event_type, actor, details, created_at FROM audit_events ORDER BY created_at DESC LIMIT ?";
    private static final String SELECT_RANGE_SQL =
            "SELECT id, event_type, actor, details, created_at " +
            "FROM audit_events " +
            "WHERE (? IS NULL OR created_at >= ?) AND (? IS NULL OR created_at <= ?) " +
            "ORDER BY created_at DESC";

    public AuditLogDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insert(AuditLogService.AuditEvent event) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, event.getType().name());
            ps.setString(2, event.getActor());
            ps.setString(3, event.getDetails());
            ps.setTimestamp(4, Timestamp.valueOf(event.getTimestamp()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Unable to insert audit event {} - {}", event.getType(), ex.getMessage(), ex);
        }
    }

    public List<AuditLogService.AuditEvent> findRecent(int limit) {
        List<AuditLogService.AuditEvent> events = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RECENT_SQL)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading recent audit events: {}", ex.getMessage(), ex);
        }
        return events;
    }

    public List<AuditLogService.AuditEvent> findRange(LocalDateTime from, LocalDateTime to) {
        List<AuditLogService.AuditEvent> events = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RANGE_SQL)) {
            if (from != null) {
                ps.setTimestamp(1, Timestamp.valueOf(from));
                ps.setTimestamp(2, Timestamp.valueOf(from));
            } else {
                ps.setNull(1, java.sql.Types.TIMESTAMP);
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            if (to != null) {
                ps.setTimestamp(3, Timestamp.valueOf(to));
                ps.setTimestamp(4, Timestamp.valueOf(to));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error querying audit events: {}", ex.getMessage(), ex);
        }
        return events;
    }

    private AuditLogService.AuditEvent map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        AuditLogService.EventType type = AuditLogService.EventType.valueOf(rs.getString("event_type"));
        String actor = rs.getString("actor");
        String details = rs.getString("details");
        Timestamp created = rs.getTimestamp("created_at");
        LocalDateTime timestamp = created != null ? created.toLocalDateTime() : LocalDateTime.now();
        return new AuditLogService.AuditEvent(id, type, actor, details, timestamp);
    }
}
