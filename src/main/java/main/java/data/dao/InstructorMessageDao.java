package main.java.data.dao;

import main.java.config.DataSourceRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for instructor messaging history.
 */
public class InstructorMessageDao extends BaseDao {
    private static final String INSERT_SQL = """
            INSERT INTO instructor_messages (instructor_username, section_id, subject, body, recipient_ids)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String SELECT_BY_INSTRUCTOR = """
            SELECT id, instructor_username, section_id, subject, body, recipient_ids, created_at
            FROM instructor_messages
            WHERE instructor_username = ?
            ORDER BY created_at DESC
            LIMIT 200
            """;

    public InstructorMessageDao() {
        super(DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Inserts a new instructor message.
     * 
     * @param username     the instructor username (must not be null or empty)
     * @param sectionId    the section ID (must not be null or empty)
     * @param subject      the message subject (must not be null or empty)
     * @param body         the message body (must not be null or empty)
     * @param recipientIds the recipient IDs (must not be null)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException    if database operation fails
     */
    public void insert(String username, String sectionId, String subject, String body, String recipientIds) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject cannot be null or empty");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Body cannot be null or empty");
        }
        if (recipientIds == null) {
            throw new IllegalArgumentException("Recipient IDs cannot be null");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, username);
            ps.setString(2, sectionId);
            ps.setString(3, subject);
            ps.setString(4, body);
            ps.setString(5, recipientIds);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to store instructor message {}:{} - {}", username, sectionId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to save instructor message", ex);
        }
    }

    /**
     * Finds all messages sent by an instructor.
     * 
     * @param username the instructor username (must not be null or empty)
     * @return list of message logs, never null
     * @throws IllegalArgumentException if username is null or empty
     */
    public List<MessageLog> findByInstructor(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        List<MessageLog> results = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_INSTRUCTOR)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Failed to load instructor messages for {}: {}", username, ex.getMessage(), ex);
        }
        return results;
    }

    private MessageLog map(ResultSet rs) throws SQLException {
        return new MessageLog(
                rs.getLong("id"),
                rs.getString("instructor_username"),
                rs.getString("section_id"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getString("recipient_ids"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    public record MessageLog(long id,
            String instructorUsername,
            String sectionId,
            String subject,
            String body,
            String recipientIds,
            LocalDateTime createdAt) {
    }
}
