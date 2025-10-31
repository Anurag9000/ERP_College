package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.NotificationMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for persisting system notifications.
 */
public class NotificationDao extends BaseDao {
    private static final String INSERT_SQL = "INSERT INTO notifications (audience, target_id, message, category, is_read, read_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_ALL_SQL =
            "SELECT id, audience, target_id, message, category, created_at, is_read, read_at FROM notifications ORDER BY created_at DESC";
    private static final String SELECT_VISIBLE_SQL =
            "SELECT id, audience, target_id, message, category, created_at, is_read, read_at " +
            "FROM notifications " +
            "WHERE audience = 'ALL' OR audience = ? OR (audience = 'USER' AND target_id = ?) " +
            "ORDER BY created_at DESC";
    private static final String SELECT_BY_ID_SQL =
            "SELECT id, audience, target_id, message, category, created_at, is_read, read_at FROM notifications WHERE id = ?";
    private static final String UPDATE_READ_SQL =
            "UPDATE notifications SET is_read = ?, read_at = ? WHERE id = ?";

    public NotificationDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public NotificationMessage insert(NotificationMessage notification) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, notification.getAudience().name());
            if (notification.getTargetId() != null) {
                ps.setString(2, notification.getTargetId());
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getCategory());
            ps.setBoolean(5, notification.isRead());
            if (notification.getReadAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(notification.getReadAt()));
            } else {
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            }
            ps.executeUpdate();

            Long generatedId = null;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    generatedId = keys.getLong(1);
                }
            }
            if (generatedId != null) {
                NotificationMessage persisted = fetchById(conn, generatedId);
                if (persisted != null) {
                    notification.setId(persisted.getId());
                    notification.setCreatedAt(persisted.getCreatedAt());
                    notification.setAudience(persisted.getAudience());
                    notification.setTargetId(persisted.getTargetId());
                    notification.setMessage(persisted.getMessage());
                    notification.setCategory(persisted.getCategory());
                    notification.setRead(persisted.isRead());
                    notification.setReadAt(persisted.getReadAt());
                } else if (notification.getCreatedAt() == null) {
                    notification.setCreatedAt(LocalDateTime.now());
                }
            } else if (notification.getCreatedAt() == null) {
                notification.setCreatedAt(LocalDateTime.now());
            }
        } catch (SQLException ex) {
            logger.error("Error inserting notification [{}]: {}", notification.getMessage(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to persist notification", ex);
        }
        return notification;
    }

    public List<NotificationMessage> findAll() {
        List<NotificationMessage> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapNotification(rs));
            }
        } catch (SQLException ex) {
            logger.error("Error loading notifications: {}", ex.getMessage(), ex);
        }
        return list;
    }

    public List<NotificationMessage> findVisible(NotificationMessage.Audience audience, String targetId) {
        List<NotificationMessage> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_VISIBLE_SQL)) {
            ps.setString(1, audience.name());
            if (targetId != null) {
                ps.setString(2, targetId);
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapNotification(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading notifications for {}:{} - {}", audience, targetId, ex.getMessage(), ex);
        }
        return list;
    }

    public void markRead(long id, boolean read) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_READ_SQL)) {
            ps.setBoolean(1, read);
            if (read) {
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating read state for notification {}: {}", id, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update notification state", ex);
        }
    }

    private NotificationMessage fetchById(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapNotification(rs);
                }
            }
        }
        return null;
    }

    private NotificationMessage mapNotification(ResultSet rs) throws SQLException {
        NotificationMessage.Audience audience = NotificationMessage.Audience.valueOf(rs.getString("audience"));
        String targetId = rs.getString("target_id");
        String message = rs.getString("message");
        String category = rs.getString("category");
        Timestamp createdTs = rs.getTimestamp("created_at");
        boolean isRead = rs.getBoolean("is_read");
        Timestamp readTs = rs.getTimestamp("read_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();
        NotificationMessage notification = new NotificationMessage(
                rs.getLong("id"),
                audience,
                targetId,
                message,
                category,
                createdAt,
                isRead,
                readTs != null ? readTs.toLocalDateTime() : null
        );
        return notification;
    }
}
