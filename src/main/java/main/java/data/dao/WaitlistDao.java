package main.java.data.dao;

import main.java.config.DataSourceRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class WaitlistDao extends BaseDao {
    private static final String SELECT_BY_SECTION = "SELECT student_code FROM section_waitlist WHERE section_code = ? ORDER BY position";
    private static final String INSERT = "INSERT INTO section_waitlist (section_code, student_code, position) VALUES (?, ?, ?)";
    private static final String DELETE = "DELETE FROM section_waitlist WHERE section_code = ? AND student_code = ?";
    private static final String DELETE_SECTION = "DELETE FROM section_waitlist WHERE section_code = ?";

    public WaitlistDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<String> findWaitlist(String sectionCode) {
        List<String> list = new LinkedList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_SECTION)) {
            ps.setString(1, sectionCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading waitlist for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
        return list;
    }

    public void insert(String sectionCode, String studentCode, int position) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, sectionCode);
            ps.setString(2, studentCode);
            ps.setInt(3, position);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting waitlist entry {}:{} - {}", sectionCode, studentCode, ex.getMessage(), ex);
        }
    }

    public void delete(String sectionCode, String studentCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, sectionCode);
            ps.setString(2, studentCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting waitlist entry {}:{} - {}", sectionCode, studentCode, ex.getMessage(), ex);
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
}
