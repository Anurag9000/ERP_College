package main.java.data;

import main.java.config.DataSourceRegistry;
import main.java.models.User;
import main.java.utils.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO handling CRUD operations for authentication users.
 */
public class AuthUserDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthUserDao.class);
    private static final String BASE_SELECT = "SELECT id, username, password_hash, salt, role, full_name, email, " +
            "active, failed_attempts, locked_until, must_change_password, last_login FROM users WHERE username = ?";
    private static final String HISTORY_SELECT = "SELECT password_hash, salt FROM password_history WHERE user_id = ? ORDER BY created_at DESC";
    private static final String INSERT_USER = "INSERT INTO users (username, password_hash, salt, role, full_name, email, active, must_change_password) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_HISTORY = "INSERT INTO password_history (user_id, password_hash, salt) VALUES (?, ?, ?)";
    private static final String UPDATE_PROFILE = "UPDATE users SET full_name = ?, email = ?, active = ? WHERE id = ?";
    private static final String UPDATE_LOGIN_SUCCESS = "UPDATE users SET failed_attempts = 0, locked_until = NULL, last_login = ?, must_change_password = ? WHERE id = ?";
    private static final String UPDATE_LOGIN_FAILURE = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?";
    private static final String UPDATE_PASSWORD = "UPDATE users SET password_hash = ?, salt = ?, must_change_password = ?, failed_attempts = 0, locked_until = NULL WHERE id = ?";

    private final DataSource dataSource;

    public AuthUserDao() {
        this.dataSource = DataSourceRegistry.authDataSource()
                .orElseThrow(() -> new IllegalStateException("Auth datasource is not configured."));
    }

    public Optional<User> findByUsername(String username) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapUser(rs);
                    user.setPasswordHistory(loadHistory(conn, user.getId()));
                    return Optional.of(user);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Error fetching user {}: {}", username, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public User insert(User user) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getFullName());
            ps.setString(6, user.getEmail());
            ps.setBoolean(7, user.isActive());
            ps.setBoolean(8, user.isMustChangePassword());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            insertHistory(conn, user.getId(), user.getPasswordHash(), user.getSalt());
            return user;
        } catch (SQLException ex) {
            LOGGER.error("Error inserting user {}: {}", user.getUsername(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to create user", ex);
        }
    }

    public void updateProfile(User user) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_PROFILE)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setBoolean(3, user.isActive());
            ps.setLong(4, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("Error updating profile for {}: {}", user.getUsername(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update profile", ex);
        }
    }

    public void recordLoginSuccess(User user) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOGIN_SUCCESS)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBoolean(2, user.isMustChangePassword());
            ps.setLong(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("Error updating login success for {}: {}", user.getUsername(), ex.getMessage(), ex);
        }
    }

    public void recordLoginFailure(User user, int failedAttempts, LocalDateTime lockedUntil) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LOGIN_FAILURE)) {
            ps.setInt(1, failedAttempts);
            if (lockedUntil != null) {
                ps.setTimestamp(2, Timestamp.valueOf(lockedUntil));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setLong(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("Error updating login failure for {}: {}", user.getUsername(), ex.getMessage(), ex);
        }
    }

    public void updatePassword(User user, String salt, String hash, boolean mustChange) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_PASSWORD)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setBoolean(3, mustChange);
            ps.setLong(4, user.getId());
            ps.executeUpdate();
            insertHistory(conn, user.getId(), hash, salt);
        } catch (SQLException ex) {
            LOGGER.error("Error updating password for {}: {}", user.getUsername(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update password", ex);
        }
    }

    private void insertHistory(Connection conn, long userId, String hash, String salt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_HISTORY)) {
            ps.setLong(1, userId);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setRole(rs.getString("role"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setActive(rs.getBoolean("active"));
        user.setFailedAttempts(rs.getInt("failed_attempts"));
        Timestamp locked = rs.getTimestamp("locked_until");
        if (locked != null) {
            user.setLockedUntil(locked.toLocalDateTime());
        }
        user.setMustChangePassword(rs.getBoolean("must_change_password"));
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }
        return user;
    }

    private java.util.Deque<String> loadHistory(Connection conn, long userId) throws SQLException {
        java.util.Deque<String> history = new java.util.ArrayDeque<>();
        try (PreparedStatement ps = conn.prepareStatement(HISTORY_SELECT)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(rs.getString("salt") + ":" + rs.getString("password_hash"));
                }
            }
        }
        while (history.size() > PasswordPolicy.historySize()) {
            history.removeLast();
        }
        return history;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, username, password_hash, salt, role, full_name, email, active, failed_attempts, locked_until, must_change_password, last_login FROM users")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = mapUser(rs);
                    user.setPasswordHistory(loadHistory(conn, user.getId()));
                    users.add(user);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Error loading users: {}", ex.getMessage(), ex);
        }
        return users;
    }
}
