package main.java.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for course co-requisite and anti-requisite relationships.
 */
public class CourseRelationshipDao extends BaseDao {
    private static final String SELECT_COREQS = "SELECT corequisite_code FROM course_corequisites WHERE course_code = ? ORDER BY corequisite_code";
    private static final String SELECT_ANTIREQS = "SELECT antirequisite_code FROM course_antirequisites WHERE course_code = ? ORDER BY antirequisite_code";
    private static final String DELETE_COREQS = "DELETE FROM course_corequisites WHERE course_code = ?";
    private static final String DELETE_ANTIREQS = "DELETE FROM course_antirequisites WHERE course_code = ?";
    private static final String INSERT_COREQ = "INSERT INTO course_corequisites (course_code, corequisite_code) VALUES (?, ?)";
    private static final String INSERT_ANTIREQ = "INSERT INTO course_antirequisites (course_code, antirequisite_code) VALUES (?, ?)";

    public CourseRelationshipDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Finds all corequisites for a course.
     * 
     * @param courseCode the course code (must not be null or empty)
     * @return list of corequisite course codes, never null
     * @throws IllegalArgumentException if courseCode is null or empty
     */
    public List<String> findCorequisites(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        return loadRelationships(SELECT_COREQS, courseCode);
    }

    /**
     * Finds all antirequisites for a course.
     * 
     * @param courseCode the course code (must not be null or empty)
     * @return list of antirequisite course codes, never null
     * @throws IllegalArgumentException if courseCode is null or empty
     */
    public List<String> findAntirequisites(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        return loadRelationships(SELECT_ANTIREQS, courseCode);
    }

    /**
     * Replaces all corequisites for a course.
     * 
     * @param courseCode the course code (must not be null or empty)
     * @param coreqs     the list of corequisite codes (can be null or empty)
     * @throws IllegalArgumentException if courseCode is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public void replaceCorequisites(String courseCode, List<String> coreqs) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        replace(DELETE_COREQS, INSERT_COREQ, courseCode, coreqs);
    }

    /**
     * Replaces all antirequisites for a course.
     * 
     * @param courseCode the course code (must not be null or empty)
     * @param antireqs   the list of antirequisite codes (can be null or empty)
     * @throws IllegalArgumentException if courseCode is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public void replaceAntirequisites(String courseCode, List<String> antireqs) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        replace(DELETE_ANTIREQS, INSERT_ANTIREQ, courseCode, antireqs);
    }

    private List<String> loadRelationships(String sql, String courseCode) {
        List<String> results = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading course relationship for {}: {}", courseCode, ex.getMessage(), ex);
        }
        return results;
    }

    private void replace(String deleteSql, String insertSql, String courseCode, List<String> values) {
        try (Connection conn = getConnection();
                PreparedStatement delete = conn.prepareStatement(deleteSql);
                PreparedStatement insert = conn.prepareStatement(insertSql)) {
            delete.setString(1, courseCode);
            delete.executeUpdate();
            if (values != null) {
                for (String value : values) {
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    insert.setString(1, courseCode);
                    insert.setString(2, value.trim());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException ex) {
            logger.error("Error saving relationships for {}: {}", courseCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to save relationships", ex);
        }
    }
}
