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

    private static final int MAX_FAILED_ATTEMPTS = 5; // Should ideally come from config
    private static final int LOCKOUT_MINUTES = 15;
    private static final int PASSWORD_HISTORY_SIZE = 5;

    public static User authenticateUser(String username, String password) {
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
                lockUntil = now.plusMinutes(LOCKOUT_MINUTES);
                AuditLogService.log(AuditLogService.EventType.ACCOUNT_LOCKED, username,
                        "Exceeded failed login attempts");
                failedAttempts = 0;
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

    public static void changePassword(String username, String currentPassword, String newPassword) {
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);

        if (!PasswordUtil.verifyPassword(currentPassword.toCharArray(), user.getSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        applyNewPassword(user, newPassword, false);
        AuditLogService.log(AuditLogService.EventType.PASSWORD_CHANGED, username, "User-initiated change");
    }

    public static void resetPassword(String username, String newPassword) {
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);

        applyNewPassword(user, newPassword, true);
        AuditLogService.log(AuditLogService.EventType.PASSWORD_RESET, username, "Admin reset password");
    }

    private static void applyNewPassword(User user, String newPassword, boolean mustChangeNext) {
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

    public static User getUser(String username) {
        return authUserDao.findByUsername(username).orElse(null);
    }

    public static List<User> getAllUsers() {
        return authUserDao.findAll();
    }

    public static void updateProfile(String username, String fullName, String email, boolean active) {
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);

        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(active);
        authUserDao.updateProfile(user);
    }
}
