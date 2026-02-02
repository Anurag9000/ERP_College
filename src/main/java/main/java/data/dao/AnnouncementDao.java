package main.java.data.dao;

import main.java.models.Announcement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDao extends BaseDao {

    public AnnouncementDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Inserts a new announcement into the database.
     * 
     * @param announcement the announcement to insert (must not be null)
     * @throws IllegalArgumentException if announcement is null or has invalid data
     * @throws RuntimeException         if database operation fails
     */
    public void insertAnnouncement(Announcement announcement) {
        if (announcement == null) {
            throw new IllegalArgumentException("Announcement cannot be null");
        }
        if (announcement.getCategory() == null) {
            throw new IllegalArgumentException("Announcement category cannot be null");
        }
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement title cannot be null or empty");
        }
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement content cannot be null or empty");
        }
        if (announcement.getPostedBy() == null || announcement.getPostedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement posted_by cannot be null or empty");
        }
        if (announcement.getPriority() == null) {
            throw new IllegalArgumentException("Announcement priority cannot be null");
        }

        String sql = "INSERT INTO announcements (category, department, title, content, posted_by, expires_at, priority) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, announcement.getCategory().name());
            stmt.setString(2, announcement.getDepartment());
            stmt.setString(3, announcement.getTitle());
            stmt.setString(4, announcement.getContent());
            stmt.setString(5, announcement.getPostedBy());
            if (announcement.getExpiresAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(announcement.getExpiresAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            stmt.setString(7, announcement.getPriority().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    announcement.setAnnouncementId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to insert announcement with title '{}': {}",
                    announcement.getTitle(), e.getMessage(), e);
            throw new RuntimeException("Error creating announcement: " + e.getMessage(), e);
        }
    }

    public List<Announcement> getAllAnnouncements() {
        String sql = "SELECT * FROM announcements WHERE expires_at IS NULL OR expires_at > NOW() " +
                "ORDER BY priority DESC, posted_at DESC";
        List<Announcement> list = new ArrayList<>();
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapAnnouncement(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all announcements: {}", e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all active announcements for a specific category.
     * 
     * @param category the category to filter by (must not be null)
     * @return list of announcements, never null (empty if none found)
     * @throws IllegalArgumentException if category is null
     */
    public List<Announcement> getAnnouncementsByCategory(Announcement.Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        String sql = "SELECT * FROM announcements WHERE category = ? AND (expires_at IS NULL OR expires_at > NOW()) " +
                "ORDER BY posted_at DESC";
        List<Announcement> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAnnouncement(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching announcements for category {}: {}", category, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves all active announcements for a specific department.
     * 
     * @param department the department to filter by (must not be null or empty)
     * @return list of announcements, never null (empty if none found)
     * @throws IllegalArgumentException if department is null or empty
     */
    public List<Announcement> getAnnouncementsByDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be null or empty");
        }
        String sql = "SELECT * FROM announcements WHERE department = ? AND (expires_at IS NULL OR expires_at > NOW()) "
                +
                "ORDER BY posted_at DESC";
        List<Announcement> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, department);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAnnouncement(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching announcements for department {}: {}", department, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Subscribes a student to announcements for a specific category and department.
     * 
     * @param studentCode the student code (must not be null or empty)
     * @param category    the category to subscribe to (must not be null)
     * @param department  the department (must not be null or empty)
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws RuntimeException         if database operation fails
     */
    public void subscribeToCategory(String studentCode, Announcement.Category category, String department) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be null or empty");
        }
        String sql = "INSERT INTO announcement_subscriptions (student_code, category, department, subscribed) " +
                "VALUES (?, ?, ?, TRUE) " +
                "ON DUPLICATE KEY UPDATE subscribed = TRUE";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            stmt.setString(2, category.name());
            stmt.setString(3, department);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error subscribing to category", e);
        }
    }

    /**
     * Unsubscribes a student from announcements for a specific category and
     * department.
     * 
     * @param studentCode the student code (must not be null or empty)
     * @param category    the category to unsubscribe from (must not be null)
     * @param department  the department (must not be null or empty)
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws RuntimeException         if database operation fails
     */
    public void unsubscribeFromCategory(String studentCode, Announcement.Category category, String department) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be null or empty");
        }
        String sql = "UPDATE announcement_subscriptions SET subscribed = FALSE " +
                "WHERE student_code = ? AND category = ? AND department = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            stmt.setString(2, category.name());
            stmt.setString(3, department);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error unsubscribing from category", e);
        }
    }

    private Announcement mapAnnouncement(ResultSet rs) throws SQLException {
        Announcement a = new Announcement();
        a.setAnnouncementId(rs.getLong("announcement_id"));
        a.setCategory(Announcement.Category.valueOf(rs.getString("category")));
        a.setDepartment(rs.getString("department"));
        a.setTitle(rs.getString("title"));
        a.setContent(rs.getString("content"));
        a.setPostedBy(rs.getString("posted_by"));
        Timestamp postedAt = rs.getTimestamp("posted_at");
        if (postedAt != null) {
            a.setPostedAt(postedAt.toLocalDateTime());
        }
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null)
            a.setExpiresAt(expires.toLocalDateTime());
        a.setPriority(Announcement.Priority.valueOf(rs.getString("priority")));
        return a;
    }
}
