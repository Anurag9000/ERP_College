package main.java.models;

/**
 * User model class representing system users
 */
public class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String passwordHash;
    private String salt;
    private String role;
    private String fullName;
    private String email;
    private boolean isActive;
    private java.time.LocalDateTime lastLogin;
    
    public User() {}
    
    public User(String username, String passwordHash, String salt, String role, String fullName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.isActive = true;
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
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
}
