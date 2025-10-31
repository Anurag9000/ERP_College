package main.java.models;

/**
 * User model class representing system users
 */
public class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String passwordHash;
    private String salt;
    private String role;
    private String fullName;
    private String email;
    private boolean isActive;
    private java.time.LocalDateTime lastLogin;
    private java.time.LocalDateTime lockedUntil;
    private int failedAttempts;
    private boolean mustChangePassword;
    private java.util.Deque<String> passwordHistory; // Entries stored as salt:hash
    
    public User() {}
    
    public User(String username, String passwordHash, String salt, String role, String fullName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.isActive = true;
        this.failedAttempts = 0;
        this.mustChangePassword = false;
        this.passwordHistory = new java.util.ArrayDeque<>();
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public java.time.LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(java.time.LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public java.time.LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(java.time.LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public java.util.Deque<String> getPasswordHistory() {
        if (passwordHistory == null) {
            passwordHistory = new java.util.ArrayDeque<>();
        }
        return passwordHistory;
    }

    public void setPasswordHistory(java.util.Deque<String> history) {
        this.passwordHistory = history;
    }

    public void addPasswordHistory(String salt, String hash, int maxHistory) {
        String entry = salt + ":" + hash;
        getPasswordHistory().addFirst(entry);
        while (passwordHistory.size() > maxHistory) {
            passwordHistory.removeLast();
        }
    }
}
