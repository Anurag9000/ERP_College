package main.java.data.dao;

import main.java.config.DataSourceRegistry;

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
    private static final String SELECT_BY_COURSE =
            "SELECT prerequisite_code FROM course_prerequisites WHERE course_code = ? ORDER BY prerequisite_code";

    public CoursePrerequisiteDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<String> findPrerequisites(String courseCode) {
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
}
