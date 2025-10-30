package main.java.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

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

    private final String id;
    private final Audience audience;
    private final String targetId;
    private final String message;
    private final String category;
    private final LocalDateTime createdAt;

    public NotificationMessage(Audience audience, String targetId,
                               String message, String category) {
        this.id = UUID.randomUUID().toString();
        this.audience = audience;
        this.targetId = targetId;
        this.message = message;
        this.category = category;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public Audience getAudience() {
        return audience;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
