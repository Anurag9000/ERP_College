package main.java.data.dao;

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
import java.util.Locale;

/**
 * DAO for persisting system notifications.
 */
public class NotificationDao extends BaseDao {
    private static final String INSERT_SQL = "INSERT INTO notifications (audience, target_id, message, category, is_read, read_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_ALL_SQL = "SELECT id, audience, target_id, message, category, created_at, is_read, read_at FROM notifications ORDER BY created_at DESC";
    private static final String SELECT_VISIBLE_SQL = "SELECT id, audience, target_id, message, category, created_at, is_read, read_at "
            +
            "FROM notifications " +
            "WHERE audience = 'ALL' OR audience = ? OR (audience = 'USER' AND target_id = ?) " +
            "ORDER BY created_at DESC";
    private static final String SELECT_BY_ID_SQL = "SELECT id, audience, target_id, message, category, created_at, is_read, read_at FROM notifications WHERE id = ?";
    private static final String UPDATE_READ_SQL = "UPDATE notifications SET is_read = ?, read_at = ? WHERE id = ?";

    public NotificationDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Inserts a new notification.
     * 
     * @param notification the notification to insert (must not be null with valid
     *                     data)
     * @return the inserted notification with generated ID
     * @throws IllegalArgumentException if notification is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public NotificationMessage insert(NotificationMessage notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }
        if (notification.getAudience() == null) {
            throw new IllegalArgumentException("Notification audience cannot be null");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message cannot be null or empty");
        }
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

    /**
     * Finds visible notifications for a specific audience and target.
     * 
     * @param audience the audience type (must not be null)
     * @param targetId the target ID (can be null)
     * @return list of visible notifications, never null
     * @throws IllegalArgumentException if audience is null
     */
    public List<NotificationMessage> findVisible(NotificationMessage.Audience audience, String targetId) {
        if (audience == null) {
            throw new IllegalArgumentException("Audience cannot be null");
        }
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

    /**
     * Marks a notification as read or unread.
     * 
     * @param id   the notification ID (must be greater than 0)
     * @param read the read status
     * @throws IllegalArgumentException if id is invalid
     * @throws IllegalStateException    if database operation fails
     */
    public void markRead(long id, boolean read) {
        if (id <= 0) {
            throw new IllegalArgumentException("Notification ID must be greater than 0");
        }
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

    public List<NotificationMessage> findAdminHistory(NotificationMessage.Audience audience,
            LocalDateTime from,
            LocalDateTime to,
            String category) {
        List<NotificationMessage> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, audience, target_id, message, category, created_at, is_read, read_at FROM notifications WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (audience != null) {
            sql.append(" AND audience = ?");
            params.add(audience.name());
        }
        if (from != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.valueOf(to));
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(category) LIKE ?");
            params.add("%" + category.trim().toLowerCase(Locale.ENGLISH) + "%");
        }
        sql.append(" ORDER BY created_at DESC");
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Timestamp ts) {
                    ps.setTimestamp(i + 1, ts);
                } else {
                    ps.setObject(i + 1, param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapNotification(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading notification history: {}", ex.getMessage(), ex);
        }
        return list;
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
                readTs != null ? readTs.toLocalDateTime() : null);
        return notification;
    }
}
