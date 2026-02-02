package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.AttendanceRecord;
import main.java.models.AttendanceRecord.AttendanceStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao extends BaseDao {
    private static final String SELECT_BY_SECTION = "SELECT section_code, attendance_date, student_code, present, status FROM attendance_records WHERE section_code = ?";
    private static final String INSERT = "INSERT INTO attendance_records (section_code, attendance_date, student_code, present, status) VALUES (?, ?, ?, ?, ?)";
    private static final String DELETE_SECTION = "DELETE FROM attendance_records WHERE section_code = ?";
    private static final String DELETE_SECTION_DATE = "DELETE FROM attendance_records WHERE section_code = ? AND attendance_date = ?";

    public AttendanceDao() {
        super(DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Retrieves all attendance records for a specific section.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @return list of attendance records, never null (empty if none found)
     * @throws IllegalArgumentException if sectionCode is null or empty
     */
    public List<AttendanceRecord> findBySection(String sectionCode) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_SECTION)) {
            ps.setString(1, sectionCode);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.LinkedHashMap<LocalDate, AttendanceRecord> map = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    LocalDate date = rs.getDate("attendance_date").toLocalDate();
                    AttendanceRecord record = map.computeIfAbsent(date, d -> new AttendanceRecord(sectionCode, d));
                    AttendanceStatus status = resolveStatus(rs.getString("status"), rs.getBoolean("present"));
                    record.markStatus(rs.getString("student_code"), status);
                }
                list.addAll(map.values());
            }
        } catch (SQLException ex) {
            logger.error("Error loading attendance for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
        return list;
    }

    /**
     * Inserts an attendance record into the database.
     * 
     * @param record the attendance record to insert (must not be null)
     * @throws IllegalArgumentException if record is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void insert(AttendanceRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Attendance record cannot be null");
        }
        if (record.getSectionId() == null || record.getSectionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        if (record.getDate() == null) {
            throw new IllegalArgumentException("Attendance date cannot be null");
        }
        var statusEntries = record.getStatusByStudent().entrySet();
        if (statusEntries.isEmpty()) {
            return;
        }

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT)) {
            for (var entry : statusEntries) {
                AttendanceStatus status = entry.getValue() == null ? AttendanceStatus.ABSENT : entry.getValue();
                ps.setString(1, record.getSectionId());
                ps.setDate(2, Date.valueOf(record.getDate()));
                ps.setString(3, entry.getKey());
                ps.setBoolean(4, status != AttendanceStatus.ABSENT);
                ps.setString(5, status.name());
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException ex) {
            logger.error("Error inserting attendance for section {}: {}", record.getSectionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert attendance", ex);
        }
    }

    private AttendanceStatus resolveStatus(String statusRaw, boolean presentFlag) {
        if (statusRaw != null && !statusRaw.isBlank()) {
            try {
                return AttendanceStatus.valueOf(statusRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return presentFlag ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
    }

    /**
     * Deletes attendance records for a specific section and date.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @param date        the attendance date (must not be null)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void deleteBySectionAndDate(String sectionCode, LocalDate date) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE_SECTION_DATE)) {
            ps.setString(1, sectionCode);
            ps.setDate(2, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting attendance for section {} on {}: {}", sectionCode, date, ex.getMessage(), ex);
        }
    }

    /**
     * Deletes all attendance records for a specific section.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @throws IllegalArgumentException if sectionCode is null or empty
     */
    public void deleteBySection(String sectionCode) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE_SECTION)) {
            ps.setString(1, sectionCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting attendance for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
    }
}
