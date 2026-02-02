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

    /**
     * Finds all enrollment records for a specific student.
     * 
     * @param studentCode the student code (must not be null or empty)
     * @return list of enrollment records, never null (empty if none found)
     * @throws IllegalArgumentException if studentCode is null or empty
     */
    public List<EnrollmentRecord> findByStudent(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
        return fetchList(SELECT_BY_STUDENT, studentCode);
    }

    public List<EnrollmentRecord> findByStudent(Connection conn, String studentCode) {
        return fetchList(conn, SELECT_BY_STUDENT, studentCode);
    }

    /**
     * Finds all enrollment records for a specific section.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @return list of enrollment records, never null (empty if none found)
     * @throws IllegalArgumentException if sectionCode is null or empty
     */
    public List<EnrollmentRecord> findBySection(String sectionCode) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        return fetchList(SELECT_BY_SECTION, sectionCode);
    }

    public List<EnrollmentRecord> findBySection(Connection conn, String sectionCode) {
        return fetchList(conn, SELECT_BY_SECTION, sectionCode);
    }

    /**
     * Finds an enrollment record for a specific section and student.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @param studentCode the student code (must not be null or empty)
     * @return the enrollment record if found, null otherwise
     * @throws IllegalArgumentException if parameters are null or empty
     */
    public EnrollmentRecord findBySectionAndStudent(String sectionCode, String studentCode) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
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

    /**
     * Locks an enrollment record for update within a transaction.
     * 
     * @param conn      the database connection (must not be null)
     * @param sectionId the section ID (must not be null or empty)
     * @param studentId the student ID (must not be null or empty)
     * @throws IllegalArgumentException if parameters are null or empty
     * @throws SQLException             if database operation fails
     */
    public void lockEnrollment(Connection conn, String sectionId, String studentId) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        String sql = "SELECT 1 FROM enrollments WHERE section_code = ? AND student_code = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sectionId);
            ps.setString(2, studentId);
            ps.execute();
        }
    }

    /**
     * Inserts a new enrollment record.
     * 
     * @param record the enrollment record to insert (must not be null with valid
     *               data)
     * @throws IllegalArgumentException if record is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void insert(EnrollmentRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Enrollment record cannot be null");
        }
        if (record.getStudentId() == null || record.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        if (record.getSectionId() == null || record.getSectionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        if (record.getStatus() == null) {
            throw new IllegalArgumentException("Enrollment status cannot be null");
        }
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

    /**
     * Updates an enrollment record with transaction support.
     * 
     * @param record the enrollment record to update (must not be null with valid
     *               data)
     * @throws IllegalArgumentException if record is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void update(EnrollmentRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Enrollment record cannot be null");
        }
        if (record.getStudentId() == null || record.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        if (record.getSectionId() == null || record.getSectionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        if (record.getStatus() == null) {
            throw new IllegalArgumentException("Enrollment status cannot be null");
        }
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                update(conn, record);
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

    /**
     * Deletes all enrollment records for a specific section.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @throws IllegalArgumentException if sectionCode is null or empty
     */
    public void deleteBySection(String sectionCode) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
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

        // Filter out records with null or zero IDs to prevent SQL errors
        List<EnrollmentRecord> validRecords = records.stream()
                .filter(r -> {
                    Long id = r.getId();
                    return id != null && id > 0;
                })
                .collect(java.util.stream.Collectors.toList());

        if (validRecords.isEmpty())
            return;

        StringBuilder sql = new StringBuilder(
                "SELECT enrollment_id, component, score, feedback FROM grades WHERE enrollment_id IN (");
        for (int i = 0; i < validRecords.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");

        Map<Long, EnrollmentRecord> recordMap = validRecords.stream()
                .collect(Collectors.toMap(EnrollmentRecord::getId, r -> r));

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < validRecords.size(); i++) {
                ps.setLong(i + 1, validRecords.get(i).getId());
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
