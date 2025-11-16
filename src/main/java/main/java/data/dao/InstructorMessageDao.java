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
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insert(String username, String sectionId, String subject, String body, String recipientIds) {
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

    public List<MessageLog> findByInstructor(String username) {
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
                rs.getTimestamp("created_at").toLocalDateTime()
        );
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
