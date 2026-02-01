package main.java.data.dao;

import main.java.models.EnrollmentRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnrollmentDao extends BaseDao {
    private static final String SELECT_BY_STUDENT = "SELECT id, student_code, section_code, status, final_grade, updated_at FROM enrollments WHERE student_code = ?";
    private static final String SELECT_BY_SECTION = "SELECT id, student_code, section_code, status, final_grade, updated_at FROM enrollments WHERE section_code = ?";
    private static final String SELECT_BY_SECTION_AND_STUDENT = "SELECT id, student_code, section_code, status, final_grade, updated_at FROM enrollments WHERE section_code = ? AND student_code = ?";
    private static final String INSERT = "INSERT INTO enrollments (student_code, section_code, status, final_grade) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_STATUS = "UPDATE enrollments SET status = ?, final_grade = ?, updated_at = CURRENT_TIMESTAMP WHERE student_code = ? AND section_code = ?";
    private static final String DELETE_BY_SECTION = "DELETE FROM enrollments WHERE section_code = ?";

    private static final String DELETE_GRADES = "DELETE FROM grades WHERE enrollment_id = ?";
    private static final String INSERT_GRADE = "INSERT INTO grades (enrollment_id, component, score, feedback) VALUES (?, ?, ?, ?)";

    public EnrollmentDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    public List<EnrollmentRecord> findByStudent(String studentCode) {
        return fetchList(SELECT_BY_STUDENT, studentCode);
    }

    public List<EnrollmentRecord> findByStudent(Connection conn, String studentCode) {
        return fetchList(conn, SELECT_BY_STUDENT, studentCode);
    }

    public List<EnrollmentRecord> findBySection(String sectionCode) {
        return fetchList(SELECT_BY_SECTION, sectionCode);
    }

    public List<EnrollmentRecord> findBySection(Connection conn, String sectionCode) {
        return fetchList(conn, SELECT_BY_SECTION, sectionCode);
    }

    public EnrollmentRecord findBySectionAndStudent(String sectionCode, String studentCode) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_SECTION_AND_STUDENT)) {
            ps.setString(1, sectionCode);
            ps.setString(2, studentCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EnrollmentRecord record = mapRecord(rs);
                    // Load grades for this single record
                    loadGradesForBatch(conn, java.util.Collections.singletonList(record));
                    return record;
                }
            }
        } catch (SQLException ex) {
            logger.error("Error finding enrollment {} in {}: {}", studentCode, sectionCode, ex.getMessage(), ex);
        }
        return null;
    }

    public void lockEnrollment(Connection conn, String sectionId, String studentId) throws SQLException {
        String sql = "SELECT 1 FROM enrollments WHERE section_code = ? AND student_code = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sectionId);
            ps.setString(2, studentId);
            ps.execute();
        }
    }

    public void insert(EnrollmentRecord record) {
        try (Connection conn = getConnection()) {
            insert(conn, record);
        } catch (SQLException ex) {
            logger.error("Error inserting enrollment {}:{} - {}", record.getStudentId(), record.getSectionId(),
                    ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert enrollment", ex);
        }
    }

    public Map<String, Long> countEnrolledBySections() {
        Map<String, Long> counts = new HashMap<>();
        String sql = "SELECT section_code, COUNT(*) as enrolled FROM enrollments WHERE status = 'ENROLLED' GROUP BY section_code";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("section_code"), rs.getLong("enrolled"));
            }
        } catch (SQLException ex) {
            logger.error("Error counting enrollments: {}", ex.getMessage(), ex);
        }
        return counts;
    }

    public void insert(Connection conn, EnrollmentRecord record) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getStudentId());
            ps.setString(2, record.getSectionId());
            ps.setString(3, record.getStatus().name());
            if (record.getFinalGrade() > 0) {
                ps.setDouble(4, record.getFinalGrade());
            } else {
                ps.setNull(4, java.sql.Types.DECIMAL);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setId(rs.getLong(1));
                }
            }
            saveGrades(conn, record);
        }
    }

    public void update(EnrollmentRecord record) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
                    update(conn, record);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            logger.error("Error updating enrollment {}:{} - {}", record.getStudentId(), record.getSectionId(),
                    ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update enrollment", ex);
        }
    }

    public void update(Connection conn, EnrollmentRecord record) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, record.getStatus().name());
            if (record.getFinalGrade() > 0) {
                ps.setDouble(2, record.getFinalGrade());
            } else {
                ps.setNull(2, java.sql.Types.DECIMAL);
            }
            ps.setString(3, record.getStudentId());
            ps.setString(4, record.getSectionId());
            ps.executeUpdate();
        }
        saveGrades(conn, record);
    }

    public void updateStatus(EnrollmentRecord record) {
        update(record);
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
        try (Connection conn = getConnection()) {
            return fetchList(conn, sql, param);
        } catch (SQLException ex) {
            logger.error("Error loading enrollments for {}: {}", param, ex.getMessage(), ex);
        }
        return new ArrayList<>();
    }

    private List<EnrollmentRecord> fetchList(Connection conn, String sql, String param) {
        List<EnrollmentRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
            if (!list.isEmpty()) {
                loadGradesForBatch(conn, list);
            }
        } catch (SQLException ex) {
            logger.error("Error loading enrollments for {}: {}", param, ex.getMessage(), ex);
        }
        return list;
    }

    private void loadGradesForBatch(Connection conn, List<EnrollmentRecord> records) throws SQLException {
        if (records == null || records.isEmpty())
            return;

        StringBuilder sql = new StringBuilder(
                "SELECT enrollment_id, component, score, feedback FROM grades WHERE enrollment_id IN (");
        for (int i = 0; i < records.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        Map<Long, EnrollmentRecord> recordMap = records.stream()
                .collect(Collectors.toMap(EnrollmentRecord::getId, r -> r));

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < records.size(); i++) {
                ps.setLong(i + 1, records.get(i).getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long enrollmentId = rs.getLong("enrollment_id");
                    EnrollmentRecord record = recordMap.get(enrollmentId);
                    if (record != null) {
                        String component = rs.getString("component");
                        record.putScore(component, rs.getDouble("score"));
                        String feedback = rs.getString("feedback");
                        if (feedback != null) {
                            record.putFeedback(component, feedback);
                        }
                    }
                }
            }
        }
    }

    private void saveGrades(Connection conn, EnrollmentRecord record) throws SQLException {
        try (PreparedStatement psDel = conn.prepareStatement(DELETE_GRADES)) {
            psDel.setLong(1, record.getId());
            psDel.executeUpdate();
        }
        if (record.getComponentScores().isEmpty()) {
            return;
        }
        try (PreparedStatement psIns = conn.prepareStatement(INSERT_GRADE)) {
            // Combine components from both scores and feedback
            Set<String> components = new HashSet<>(record.getComponentScores().keySet());
            components.addAll(record.getComponentFeedback().keySet());

            for (String comp : components) {
                psIns.setLong(1, record.getId());
                psIns.setString(2, comp);

                Double score = record.getComponentScores().get(comp);
                if (score != null) {
                    psIns.setDouble(3, score);
                } else {
                    psIns.setNull(3, java.sql.Types.DECIMAL);
                }

                String feedback = record.getComponentFeedback().get(comp);
                if (feedback != null) {
                    psIns.setString(4, feedback);
                } else {
                    psIns.setNull(4, java.sql.Types.VARCHAR);
                }
                psIns.addBatch();
            }
            psIns.executeBatch();
        }
    }

    private EnrollmentRecord mapRecord(ResultSet rs) throws SQLException {
        EnrollmentRecord record = new EnrollmentRecord();
        record.setId(rs.getLong("id"));
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
