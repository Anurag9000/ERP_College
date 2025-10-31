package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.AttendanceRecord;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao extends BaseDao {
    private static final String SELECT_BY_SECTION = "SELECT section_code, attendance_date, student_code, present FROM attendance_records WHERE section_code = ?";
    private static final String INSERT = "INSERT INTO attendance_records (section_code, attendance_date, student_code, present) VALUES (?, ?, ?, ?)";
    private static final String DELETE_SECTION = "DELETE FROM attendance_records WHERE section_code = ?";
    private static final String DELETE_SECTION_DATE = "DELETE FROM attendance_records WHERE section_code = ? AND attendance_date = ?";

    public AttendanceDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<AttendanceRecord> findBySection(String sectionCode) {
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_SECTION)) {
            ps.setString(1, sectionCode);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.LinkedHashMap<LocalDate, AttendanceRecord> map = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    LocalDate date = rs.getDate("attendance_date").toLocalDate();
                    AttendanceRecord record = map.computeIfAbsent(date, d -> new AttendanceRecord(sectionCode, d));
                    record.getAttendanceByStudent().put(rs.getString("student_code"), rs.getBoolean("present"));
                }
                list.addAll(map.values());
            }
        } catch (SQLException ex) {
            logger.error("Error loading attendance for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
        return list;
    }

    public void insert(AttendanceRecord record) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            for (var entry : record.getAttendanceByStudent().entrySet()) {
                ps.setString(1, record.getSectionId());
                ps.setDate(2, Date.valueOf(record.getDate()));
                ps.setString(3, entry.getKey());
                ps.setBoolean(4, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            logger.error("Error inserting attendance for section {}: {}", record.getSectionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert attendance", ex);
        }
    }

    public void deleteBySectionAndDate(String sectionCode, LocalDate date) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SECTION_DATE)) {
            ps.setString(1, sectionCode);
            ps.setDate(2, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting attendance for section {} on {}: {}", sectionCode, date, ex.getMessage(), ex);
        }
    }

    public void deleteBySection(String sectionCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SECTION)) {
            ps.setString(1, sectionCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting attendance for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
    }
}
