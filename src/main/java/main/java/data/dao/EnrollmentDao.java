package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.EnrollmentRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDao extends BaseDao {
    private static final String SELECT_BY_STUDENT = "SELECT id, student_code, section_code, status, final_grade, updated_at FROM enrollments WHERE student_code = ?";
    private static final String SELECT_BY_SECTION = "SELECT id, student_code, section_code, status, final_grade, updated_at FROM enrollments WHERE section_code = ?";
    private static final String INSERT = "INSERT INTO enrollments (student_code, section_code, status, final_grade) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_STATUS = "UPDATE enrollments SET status = ?, final_grade = ?, updated_at = CURRENT_TIMESTAMP WHERE student_code = ? AND section_code = ?";
    private static final String DELETE_BY_SECTION = "DELETE FROM enrollments WHERE section_code = ?";

    public EnrollmentDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<EnrollmentRecord> findByStudent(String studentCode) {
        return fetchList(SELECT_BY_STUDENT, studentCode);
    }

    public List<EnrollmentRecord> findBySection(String sectionCode) {
        return fetchList(SELECT_BY_SECTION, sectionCode);
    }

    public void insert(EnrollmentRecord record) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, record.getStudentId());
            ps.setString(2, record.getSectionId());
            ps.setString(3, record.getStatus().name());
            if (record.getFinalGrade() > 0) {
                ps.setDouble(4, record.getFinalGrade());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting enrollment {}:{} - {}", record.getStudentId(), record.getSectionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert enrollment", ex);
        }
    }

    public void updateStatus(EnrollmentRecord record) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, record.getStatus().name());
            if (record.getFinalGrade() > 0) {
                ps.setDouble(2, record.getFinalGrade());
            } else {
                ps.setNull(2, java.sql.Types.DECIMAL);
            }
            ps.setString(3, record.getStudentId());
            ps.setString(4, record.getSectionId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating enrollment {}:{} - {}", record.getStudentId(), record.getSectionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update enrollment", ex);
        }
    }

    public void deleteBySection(String sectionCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_SECTION)) {
            ps.setString(1, sectionCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting enrollments for section {}: {}", sectionCode, ex.getMessage(), ex);
        }
    }

    private List<EnrollmentRecord> fetchList(String sql, String param) {
        List<EnrollmentRecord> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading enrollments for {}: {}", param, ex.getMessage(), ex);
        }
        return list;
    }

    private EnrollmentRecord mapRecord(ResultSet rs) throws SQLException {
        EnrollmentRecord record = new EnrollmentRecord();
        record.setStudentId(rs.getString("student_code"));
        record.setSectionId(rs.getString("section_code"));
        record.setStatus(EnrollmentRecord.Status.valueOf(rs.getString("status")));
        double grade = rs.getDouble("final_grade");
        if (!rs.wasNull()) {
            record.setFinalGrade(grade);
        }
        java.sql.Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) {
            record.setUpdatedAt(ts.toLocalDateTime());
        } else {
            record.setUpdatedAt(LocalDateTime.now());
        }
        return record;
    }
}
