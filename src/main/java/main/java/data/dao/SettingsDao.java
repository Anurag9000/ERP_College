package main.java.data.dao;

import main.java.config.DataSourceRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO for application key/value settings.
 */
public class SettingsDao extends BaseDao {
    private static final String SELECT_ALL_SQL = "SELECT setting_key, setting_value FROM settings";
    private static final String UPSERT_SQL = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";

    public SettingsDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public Map<String, String> findAll() {
        Map<String, String> map = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException ex) {
            logger.error("Error loading settings: {}", ex.getMessage(), ex);
        }
        return map;
    }

    public void upsert(String key, String value) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error saving setting {}: {}", key, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to persist setting " + key, ex);
        }
    }
}
