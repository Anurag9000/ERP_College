package main.java.data.dao;

import main.java.config.DataSourceRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for persisted assessment templates per course.
 */
public class AssessmentTemplateDao extends BaseDao {
    private static final String SELECT_BY_COURSE = """
            SELECT id, course_code, template_name, weights_json, created_by, created_at
            FROM assessment_templates
            WHERE course_code = ?
            ORDER BY template_name
            """;
    private static final String INSERT = """
            INSERT INTO assessment_templates (course_code, template_name, weights_json, created_by)
            VALUES (?, ?, ?, ?)
            """;
    private static final String DELETE = "DELETE FROM assessment_templates WHERE id = ?";
    private static final String SELECT_BY_ID = """
            SELECT id, course_code, template_name, weights_json, created_by, created_at
            FROM assessment_templates
            WHERE id = ?
            """;

    public AssessmentTemplateDao() {
        this(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public AssessmentTemplateDao(DataSource dataSource) {
        super(dataSource);
    }

    public List<AssessmentTemplate> findByCourse(String courseCode) {
        List<AssessmentTemplate> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_COURSE)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Failed to load assessment templates for {}: {}", courseCode, ex.getMessage(), ex);
        }
        return results;
    }

    public AssessmentTemplate insert(String courseCode, String templateName, String weightsJson, String createdBy) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, courseCode);
            ps.setString(2, templateName);
            ps.setString(3, weightsJson);
            ps.setString(4, createdBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new AssessmentTemplate(id, courseCode, templateName, weightsJson, createdBy, LocalDateTime.now());
                }
            }
        } catch (SQLException ex) {
            logger.error("Failed to save assessment template {} for {}: {}", templateName, courseCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to save assessment template", ex);
        }
        throw new IllegalStateException("Unable to save assessment template");
    }

    public void delete(long templateId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setLong(1, templateId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to delete assessment template {}: {}", templateId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete assessment template", ex);
        }
    }

    public AssessmentTemplate findById(long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException ex) {
            logger.error("Failed to load assessment template {}: {}", id, ex.getMessage(), ex);
        }
        return null;
    }

    private AssessmentTemplate map(ResultSet rs) throws SQLException {
        return new AssessmentTemplate(
                rs.getLong("id"),
                rs.getString("course_code"),
                rs.getString("template_name"),
                rs.getString("weights_json"),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    public record AssessmentTemplate(long id,
                                     String courseCode,
                                     String templateName,
                                     String weightsJson,
                                     String createdBy,
                                     LocalDateTime createdAt) {
    }
}
