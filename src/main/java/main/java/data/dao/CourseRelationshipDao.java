package main.java.data.dao;

import main.java.config.DataSourceRegistry;

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
    private static final String SELECT_COREQS =
            "SELECT corequisite_code FROM course_corequisites WHERE course_code = ? ORDER BY corequisite_code";
    private static final String SELECT_ANTIREQS =
            "SELECT antirequisite_code FROM course_antirequisites WHERE course_code = ? ORDER BY antirequisite_code";

    public CourseRelationshipDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<String> findCorequisites(String courseCode) {
        return loadRelationships(SELECT_COREQS, courseCode);
    }

    public List<String> findAntirequisites(String courseCode) {
        return loadRelationships(SELECT_ANTIREQS, courseCode);
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
}
