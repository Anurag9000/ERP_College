package main.java.data.dao;

import main.java.models.MaintenanceWindow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO managing persistence of maintenance windows.
 */
public class MaintenanceWindowDao extends BaseDao {
    private static final String SELECT_ALL = """
            SELECT id, start_at, end_at, message, status, created_by, created_at
            FROM maintenance_windows
            ORDER BY start_at DESC
            """;
    private static final String INSERT = """
            INSERT INTO maintenance_windows (start_at, end_at, message, status, created_by)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_STATUS = """
            UPDATE maintenance_windows SET status = ? WHERE id = ?
            """;
    private static final String DELETE = "DELETE FROM maintenance_windows WHERE id = ?";

    public MaintenanceWindowDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    public List<MaintenanceWindow> findAll() {
        List<MaintenanceWindow> result = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        } catch (SQLException ex) {
            logger.error("Failed to load maintenance windows: {}", ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * Inserts a new maintenance window.
     * 
     * @param start     the start time (must not be null)
     * @param end       the end time (must not be null and after start)
     * @param message   the maintenance message (must not be null or empty)
     * @param status    the status (must not be null)
     * @param createdBy who created it (must not be null or empty)
     * @return Optional containing the created window if successful
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException    if database operation fails
     */
    public Optional<MaintenanceWindow> insert(LocalDateTime start,
            LocalDateTime end,
            String message,
            MaintenanceWindow.Status status,
            String createdBy) {
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (start.isAfter(end) || start.equals(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setString(3, message);
            ps.setString(4, status.name());
            ps.setString(5, createdBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return Optional.of(new MaintenanceWindow(
                            id,
                            start,
                            end,
                            message,
                            status,
                            createdBy,
                            LocalDateTime.now()));
                }
            }
        } catch (SQLException ex) {
            logger.error("Failed to insert maintenance window: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to schedule maintenance window", ex);
        }
        return Optional.empty();
    }

    /**
     * Updates the status of a maintenance window.
     * 
     * @param id     the window ID (must be greater than 0)
     * @param status the new status (must not be null)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException    if database operation fails
     */
    public void updateStatus(long id, MaintenanceWindow.Status status) {
        if (id <= 0) {
            throw new IllegalArgumentException("Maintenance window ID must be greater than 0");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to update maintenance window {} status: {}", id, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update maintenance window", ex);
        }
    }

    /**
     * Deletes a maintenance window.
     * 
     * @param id the window ID (must be greater than 0)
     * @throws IllegalArgumentException if id is invalid
     * @throws IllegalStateException    if database operation fails
     */
    public void delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Maintenance window ID must be greater than 0");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to delete maintenance window {}: {}", id, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete maintenance window", ex);
        }
    }

    private MaintenanceWindow map(ResultSet rs) throws SQLException {
        return new MaintenanceWindow(
                rs.getLong("id"),
                rs.getTimestamp("start_at").toLocalDateTime(),
                rs.getTimestamp("end_at").toLocalDateTime(),
                rs.getString("message"),
                MaintenanceWindow.Status.valueOf(rs.getString("status")),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
