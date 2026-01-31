package main.java.data.dao;

import main.java.models.Announcement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDao extends BaseDao {

    public AnnouncementDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insertAnnouncement(Announcement announcement) {
        String sql = "INSERT INTO announcements (category, department, title, content, posted_by, expires_at, priority) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            throw new RuntimeException("Error creating announcement", e);
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
            e.printStackTrace();
        }
        return list;
    }

    public List<Announcement> getAnnouncementsByCategory(Announcement.Category category) {
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
            e.printStackTrace();
        }
        return list;
    }

    public List<Announcement> getAnnouncementsByDepartment(String department) {
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
            e.printStackTrace();
        }
        return list;
    }

    public void subscribeToCategory(String studentCode, Announcement.Category category, String department) {
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

    public void unsubscribeFromCategory(String studentCode, Announcement.Category category, String department) {
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
        a.setPostedAt(rs.getTimestamp("posted_at").toLocalDateTime());
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null)
            a.setExpiresAt(expires.toLocalDateTime());
        a.setPriority(Announcement.Priority.valueOf(rs.getString("priority")));
        return a;
    }
}
