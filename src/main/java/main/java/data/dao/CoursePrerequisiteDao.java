package main.java.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for course prerequisite relationships.
 */
public class CoursePrerequisiteDao extends BaseDao {
    private static final String SELECT_BY_COURSE = "SELECT prerequisite_code FROM course_prerequisites WHERE course_code = ? ORDER BY prerequisite_code";
    private static final String DELETE_BY_COURSE = "DELETE FROM course_prerequisites WHERE course_code = ?";
    private static final String INSERT = "INSERT INTO course_prerequisites (course_code, prerequisite_code) VALUES (?, ?)";

    public CoursePrerequisiteDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Finds all prerequisites for a course.
     * 
     * @param courseCode the course code (must not be null or empty)
     * @return list of prerequisite course codes, never null
     * @throws IllegalArgumentException if courseCode is null or empty
     */
    public List<String> findPrerequisites(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        List<String> prereqs = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_COURSE)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prereqs.add(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading prerequisites for {}: {}", courseCode, ex.getMessage(), ex);
        }
        return prereqs;
    }

    /**
     * Replaces all prerequisites for a course.
     * 
     * @param courseCode    the course code (must not be null or empty)
     * @param prerequisites the list of prerequisite codes (can be null or empty)
     * @throws IllegalArgumentException if courseCode is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public void replacePrerequisites(String courseCode, List<String> prerequisites) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement delete = conn.prepareStatement(DELETE_BY_COURSE);
                PreparedStatement insert = conn.prepareStatement(INSERT)) {
            delete.setString(1, courseCode);
            delete.executeUpdate();
            if (prerequisites != null) {
                for (String prereq : prerequisites) {
                    if (prereq == null || prereq.isBlank()) {
                        continue;
                    }
                    insert.setString(1, courseCode);
                    insert.setString(2, prereq.trim());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException ex) {
            logger.error("Error saving prerequisites for {}: {}", courseCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to save prerequisites", ex);
        }
    }
}
