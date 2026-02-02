package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.NotificationPreference;
import main.java.models.NotificationPreference.DigestFrequency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * DAO for notification preference storage.
 */
public class NotificationPreferenceDao extends BaseDao {
    private static final String SELECT_SQL = "SELECT user_id, digest_frequency, digest_hour, email_enabled, sms_enabled, updated_at FROM notification_preferences WHERE user_id = ?";
    private static final String UPSERT_SQL = "INSERT INTO notification_preferences (user_id, digest_frequency, digest_hour, email_enabled, sms_enabled) VALUES (?, ?, ?, ?, ?) "
            +
            "ON DUPLICATE KEY UPDATE digest_frequency = VALUES(digest_frequency), digest_hour = VALUES(digest_hour), email_enabled = VALUES(email_enabled), sms_enabled = VALUES(sms_enabled), updated_at = CURRENT_TIMESTAMP";

    public NotificationPreferenceDao() {
        super(DataSourceRegistry.erpDataSource().orElse(null));
    }

    public Optional<NotificationPreference> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading notification preference for {}: {}", userId, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /**
     * Inserts or updates a notification preference.
     * 
     * @param preference the preference to save (must not be null with valid data)
     * @return the saved preference
     * @throws IllegalArgumentException if preference is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public NotificationPreference upsert(NotificationPreference preference) {
        if (preference == null) {
            throw new IllegalArgumentException("Notification preference cannot be null");
        }
        if (preference.getUserId() == null || preference.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (preference.getDigestFrequency() == null) {
            throw new IllegalArgumentException("Digest frequency cannot be null");
        }
        if (preference.getDigestHour() < 0 || preference.getDigestHour() > 23) {
            throw new IllegalArgumentException("Digest hour must be between 0 and 23");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, preference.getUserId());
            ps.setString(2, preference.getDigestFrequency().name());
            ps.setInt(3, preference.getDigestHour());
            ps.setBoolean(4, preference.isEmailEnabled());
            ps.setBoolean(5, preference.isSmsEnabled());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error saving notification preference for {}: {}", preference.getUserId(), ex.getMessage(),
                    ex);
            throw new IllegalStateException("Unable to save notification preference", ex);
        }
        return findByUserId(preference.getUserId()).orElse(preference);
    }

    private NotificationPreference map(ResultSet rs) throws SQLException {
        return new NotificationPreference(
                rs.getString("user_id"),
                DigestFrequency.valueOf(rs.getString("digest_frequency")),
                rs.getInt("digest_hour"),
                rs.getBoolean("email_enabled"),
                rs.getBoolean("sms_enabled"),
                Optional.ofNullable(rs.getTimestamp("updated_at"))
                        .map(Timestamp::toLocalDateTime)
                        .orElse(LocalDateTime.now()));
    }
}
