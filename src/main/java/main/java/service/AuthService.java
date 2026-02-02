package main.java.service;

import main.java.data.dao.AuthUserDao;
import main.java.models.User;
import main.java.utils.AuditLogService;
import main.java.utils.PasswordPolicy;
import main.java.utils.PasswordUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for authentication and user management.
 */
public class AuthService {

    // Injected or statically retrieved DAO as per project pattern
    private static final AuthUserDao authUserDao = new AuthUserDao();

    private static int getSafeIntConfig(String key, int defaultValue) {
        try {
            String val = main.java.config.ConfigLoader.get(key);
            return val != null ? Integer.parseInt(val.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static final int MAX_FAILED_ATTEMPTS = getSafeIntConfig("auth.maxFailedAttempts", 5);
    private static final int LOCKOUT_MINUTES = getSafeIntConfig("auth.lockoutMinutes", 15);
    private static final int PASSWORD_HISTORY_SIZE = getSafeIntConfig("auth.passwordHistorySize", 5);
    private static final int LOCKOUT_INCREMENT_MINUTES = getSafeIntConfig("auth.lockoutIncrement", 0);

    /**
     * Authenticates a user with username and password.
     * 
     * @param username the username (must not be null or empty)
     * @param password the password (must not be null or empty)
     * @return the authenticated user if successful, null otherwise
     * @throws IllegalArgumentException if username or password is null or empty
     */
    public static User authenticateUser(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        LocalDateTime now = LocalDateTime.now();
        Optional<User> optionalUser = authUserDao.findByUsername(username);

        if (optionalUser.isEmpty()) {
            AuditLogService.log(AuditLogService.EventType.LOGIN_FAILURE, username, "Unknown user");
            return null;
        }

        User user = optionalUser.get();
        if (!user.isActive()) {
            AuditLogService.log(AuditLogService.EventType.LOGIN_FAILURE, username, "Inactive account");
            return null;
        }

        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            AuditLogService.log(AuditLogService.EventType.ACCOUNT_LOCKED, username,
                    "Account locked until " + user.getLockedUntil());
            return null;
        }

        boolean matched = false;
        String salt = user.getSalt();
        String hash = user.getPasswordHash();

        if (salt != null && hash != null) {
            matched = PasswordUtil.verifyPassword(password.toCharArray(), salt, hash);
        }

        if (matched) {
            user.resetFailedAttempts();
            user.setLockedUntil(null);
            user.setLastLogin(now);
            authUserDao.recordLoginSuccess(user);
            AuditLogService.log(AuditLogService.EventType.LOGIN_SUCCESS, username, "Login successful");
            return user;
        } else {
            int failedAttempts = user.getFailedAttempts() + 1;
            LocalDateTime lockUntil = null;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                int extraMinutes = (failedAttempts - MAX_FAILED_ATTEMPTS) * LOCKOUT_INCREMENT_MINUTES;
                lockUntil = now.plusMinutes(LOCKOUT_MINUTES + extraMinutes);
                AuditLogService.log(AuditLogService.EventType.ACCOUNT_LOCKED, username,
                        "Exceeded failed login attempts. Locked for " + (LOCKOUT_MINUTES + extraMinutes) + " mins.");
                // We keep incrementing failedAttempts to increase next lockout if they try
                // before it expires?
                // Actually, standard behavior is to reset or keep it.
                // Let's keep it so next failure adds more time.
            } else {
                AuditLogService.log(AuditLogService.EventType.LOGIN_FAILURE, username,
                        "Invalid credentials (" + failedAttempts + "/" + MAX_FAILED_ATTEMPTS + ")");
            }
            user.setFailedAttempts(failedAttempts);
            user.setLockedUntil(lockUntil);
            authUserDao.recordLoginFailure(user, failedAttempts, lockUntil);
            return null;
        }
    }

    /**
     * Changes a user's password after verifying the current password.
     * 
     * @param username        the username (must not be null or empty)
     * @param currentPassword the current password (must not be null or empty)
     * @param newPassword     the new password (must not be null or empty)
     * @throws IllegalArgumentException if parameters are invalid or current
     *                                  password is incorrect
     */
    public static void changePassword(String username, String currentPassword, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("Current password cannot be null or empty");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);

        if (!PasswordUtil.verifyPassword(currentPassword.toCharArray(), user.getSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        applyNewPassword(user, newPassword, false);
        AuditLogService.log(AuditLogService.EventType.PASSWORD_CHANGED, username, "User-initiated change");
    }

    /**
     * Resets a user's password (admin operation).
     * 
     * @param username    the username (must not be null or empty)
     * @param newPassword the new password (must not be null or empty)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static void resetPassword(String username, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);

        applyNewPassword(user, newPassword, true);
        AuditLogService.log(AuditLogService.EventType.PASSWORD_RESET, username, "Admin reset password");
    }

    /**
     * Applies a new password to a user account.
     * 
     * @param user           the user (must not be null)
     * @param newPassword    the new password (must not be null or empty)
     * @param mustChangeNext whether user must change password on next login
     * @throws IllegalArgumentException if parameters are invalid or password is in
     *                                  history
     */
    private static void applyNewPassword(User user, String newPassword, boolean mustChangeNext) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }
        PasswordPolicy.validateComplexity(newPassword);
        ensureNotInHistory(user, newPassword);

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword.toCharArray(), newSalt);

        user.addPasswordHistory(newSalt, newHash, PASSWORD_HISTORY_SIZE);
        user.setSalt(newSalt);
        user.setPasswordHash(newHash);
        user.resetFailedAttempts();
        user.setLockedUntil(null);
        user.setMustChangePassword(mustChangeNext);

        authUserDao.updatePassword(user, newSalt, newHash, mustChangeNext);
    }

    private static void ensureNotInHistory(User user, String candidate) {
        for (String entry : user.getPasswordHistory()) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                if (PasswordUtil.verifyPassword(candidate.toCharArray(), parts[0], parts[1])) {
                    throw new IllegalArgumentException("Password has been used recently.");
                }
            }
        }
    }

    /**
     * Retrieves a user by username.
     * 
     * @param username the username (must not be null or empty)
     * @return the user if found, null otherwise
     * @throws IllegalArgumentException if username is null or empty
     */
    public static User getUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return authUserDao.findByUsername(username).orElse(null);
    }

    public static List<User> getAllUsers() {
        return authUserDao.findAll();
    }

    /**
     * Updates a user's profile information.
     * 
     * @param username the username (must not be null or empty)
     * @param fullName the full name (can be null)
     * @param email    the email (can be null)
     * @param active   the active status
     * @throws IllegalArgumentException if username is null or empty or user not
     *                                  found
     */
    public static void updateProfile(String username, String fullName, String email, boolean active) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);

        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(active);
        authUserDao.updateProfile(user);
    }
}
