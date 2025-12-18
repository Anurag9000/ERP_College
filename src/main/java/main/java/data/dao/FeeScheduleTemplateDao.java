package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FeeScheduleTemplateDao extends BaseDao {
    private static final String SELECT_BY_COURSE = "SELECT id, course_code, label, amount, offset_days FROM fee_schedule_templates WHERE course_code = ? ORDER BY offset_days";
    private static final String INSERT = "INSERT INTO fee_schedule_templates (course_code, label, amount, offset_days) VALUES (?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE fee_schedule_templates SET label = ?, amount = ?, offset_days = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM fee_schedule_templates WHERE id = ?";

    public FeeScheduleTemplateDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<TemplateRecord> findByCourse(String courseCode) {
        List<TemplateRecord> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_COURSE)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error loading fee templates for {}: {}", courseCode, ex.getMessage(), ex);
        }
        return list;
    }

    public TemplateRecord insert(String courseCode, String label, double amount, int offsetDays) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, courseCode);
            ps.setString(2, label);
            ps.setDouble(3, amount);
            ps.setInt(4, offsetDays);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new TemplateRecord(keys.getLong(1), courseCode, label, amount, offsetDays);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error inserting fee template {}:{} - {}", courseCode, label, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to save fee template", ex);
        }
        throw new IllegalStateException("Unable to save fee template");
    }

    public void update(long id, String label, double amount, int offsetDays) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, label);
            ps.setDouble(2, amount);
            ps.setInt(3, offsetDays);
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating fee template {}: {}", id, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update fee template", ex);
        }
    }

    public void delete(long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting fee template {}: {}", id, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete fee template", ex);
        }
    }

    private TemplateRecord map(ResultSet rs) throws SQLException {
        return new TemplateRecord(
                rs.getLong("id"),
                rs.getString("course_code"),
                rs.getString("label"),
                rs.getDouble("amount"),
                rs.getInt("offset_days"));
    }

    public record TemplateRecord(long id, String courseCode, String label, double amount, int offsetDays) {
    }
}
