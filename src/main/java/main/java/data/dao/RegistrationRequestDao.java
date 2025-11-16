package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegistrationRequestDao extends BaseDao {
    private static final String INSERT = "INSERT INTO registration_requests (student_code, section_code, status, requested_by) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_STATUS = "UPDATE registration_requests SET status = ?, decided_by = ?, decided_at = CURRENT_TIMESTAMP, notes = ? WHERE id = ?";
    private static final String SELECT_PENDING = "SELECT * FROM registration_requests WHERE status = 'PENDING' ORDER BY created_at";
    private static final String SELECT_BY_STUDENT_SECTION = "SELECT * FROM registration_requests WHERE student_code = ? AND section_code = ?";
    private static final String SELECT_BY_ID = "SELECT * FROM registration_requests WHERE id = ?";

    public RegistrationRequestDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insert(String studentCode, String sectionCode, String requestedBy) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, studentCode);
            ps.setString(2, sectionCode);
            ps.setString(3, "PENDING");
            ps.setString(4, requestedBy);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting registration request {}:{} - {}", studentCode, sectionCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to create registration request", ex);
        }
    }

    public void updateStatus(long id, String status, String decidedBy, String notes) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setString(2, decidedBy);
            ps.setString(3, notes);
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating registration request {}: {}", id, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update registration request", ex);
        }
    }

    public List<RequestRecord> findPending() {
        List<RequestRecord> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PENDING);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException ex) {
            logger.error("Error loading pending registration requests: {}", ex.getMessage(), ex);
        }
        return list;
    }

    public Optional<RequestRecord> findByStudentSection(String studentCode, String sectionCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_STUDENT_SECTION)) {
            ps.setString(1, studentCode);
            ps.setString(2, sectionCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading registration request {}:{} - {}", studentCode, sectionCode, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public Optional<RequestRecord> findById(long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading registration request {}: {}", id, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    private RequestRecord map(ResultSet rs) throws SQLException {
        return new RequestRecord(
                rs.getLong("id"),
                rs.getString("student_code"),
                rs.getString("section_code"),
                rs.getString("status"),
                rs.getString("requested_by"),
                rs.getString("decided_by"),
                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toInstant());
    }

    public record RequestRecord(long id,
                                String studentCode,
                                String sectionCode,
                                String status,
                                String requestedBy,
                                String decidedBy,
                                java.time.Instant decidedAt,
                                String notes,
                                java.time.Instant createdAt) {
    }
}
