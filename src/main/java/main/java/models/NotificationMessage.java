package main.java.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Simple notification message persisted for users.
 */
public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Audience {
        ALL,
        STUDENT,
        INSTRUCTOR,
        ADMIN,
        USER
    }

    private Long id;
    private Audience audience;
    private String targetId;
    private String message;
    private String category;
    private LocalDateTime createdAt;
    private boolean read;
    private LocalDateTime readAt;

    public NotificationMessage(Audience audience, String targetId,
                               String message, String category) {
        this(null, audience, targetId, message, category, LocalDateTime.now(), false, null);
    }

    public NotificationMessage(Long id, Audience audience, String targetId,
                               String message, String category, LocalDateTime createdAt) {
        this(id, audience, targetId, message, category, createdAt, false, null);
    }

    public NotificationMessage(Long id, Audience audience, String targetId,
                               String message, String category, LocalDateTime createdAt,
                               boolean read, LocalDateTime readAt) {
        this.id = id;
        this.audience = audience;
        this.targetId = targetId;
        this.message = message;
        this.category = category;
        this.createdAt = createdAt;
        this.read = read;
        this.readAt = readAt;
    }

    public NotificationMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
