package main.java.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class WaitlistDao extends BaseDao {
    private static final String SELECT_BY_SECTION = "SELECT student_code, position, advisor_approved FROM section_waitlist WHERE section_code = ? ORDER BY position";
    private static final String INSERT = "INSERT INTO section_waitlist (section_code, student_code, position, advisor_approved) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_APPROVAL = "UPDATE section_waitlist SET advisor_approved = ? WHERE section_code = ? AND student_code = ?";
    private static final String DELETE = "DELETE FROM section_waitlist WHERE section_code = ? AND student_code = ?";
    private static final String DELETE_SECTION = "DELETE FROM section_waitlist WHERE section_code = ?";

    public WaitlistDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<String> findWaitlist(String sectionCode) {
        List<String> list = new LinkedList<>();
        for (WaitlistEntry entry : findEntries(sectionCode)) {
            list.add(entry.studentCode());
        }
        return list;
    }

    public List<WaitlistEntry> findEntries(String sectionCode) {
        List<WaitlistEntry> list = new LinkedList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_SECTION)) {
            ps.setString(1, sectionCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WaitlistEntry(
                            rs.getString("student_code"),
                            rs.getInt("position"),
                            rs.getBoolean("advisor_approved")));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading waitlist for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
        return list;
    }

    public void insert(String sectionCode, String studentCode, int position, boolean advisorApproved) {
        try (Connection conn = getConnection()) {
            insert(conn, sectionCode, studentCode, position, advisorApproved);
        } catch (SQLException ex) {
            logger.error("Error inserting waitlist entry: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert waitlist entry", ex);
        }
    }

    public void insert(Connection conn, String sectionCode, String studentCode, int position, boolean advisorApproved)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, sectionCode);
            ps.setString(2, studentCode);
            ps.setInt(3, position);
            ps.setBoolean(4, advisorApproved);
            ps.executeUpdate();
        }
    }

    public void updateApproval(String sectionCode, String studentCode, boolean approved) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(UPDATE_APPROVAL)) {
            ps.setBoolean(1, approved);
            ps.setString(2, sectionCode);
            ps.setString(3, studentCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating waitlist approval {}:{} - {}", sectionCode, studentCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update waitlist approval", ex);
        }
    }

    public void delete(String sectionCode, String studentCode) {
        try (Connection conn = getConnection()) {
            delete(conn, sectionCode, studentCode);
        } catch (SQLException ex) {
            logger.error("Error deleting waitlist entry: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete waitlist entry", ex);
        }
    }

    public void delete(Connection conn, String sectionCode, String studentCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, sectionCode);
            ps.setString(2, studentCode);
            ps.executeUpdate();
        }
    }

    public void deleteAll(String sectionCode) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE_SECTION)) {
            ps.setString(1, sectionCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error clearing waitlist for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
    }

    public record WaitlistEntry(String studentCode, int position, boolean advisorApproved) {
    }
}
