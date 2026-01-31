package main.java.models;

import java.util.Objects;

/**
 * Represents an admin-originated notification broadcast request.
 */
public class NotificationRequest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum TargetType {
        ALL,
        STUDENTS,
        INSTRUCTORS,
        ADMINS,
        USER,
        STUDENT_DEPARTMENT,
        INSTRUCTOR_DEPARTMENT
    }

    private final TargetType targetType;
    private final String targetValue;
    private final String category;
    private final String message;
    private final boolean emailChannel;
    private final boolean smsChannel;

    public NotificationRequest(TargetType targetType,
            String targetValue,
            String category,
            String message,
            boolean emailChannel,
            boolean smsChannel) {
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.targetValue = targetValue;
        this.category = category == null || category.isBlank() ? "General" : category.trim();
        this.message = Objects.requireNonNull(message, "message");
        this.emailChannel = emailChannel;
        this.smsChannel = smsChannel;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEmailChannel() {
        return emailChannel;
    }

    public boolean isSmsChannel() {
        return smsChannel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NotificationRequest))
            return false;
        NotificationRequest that = (NotificationRequest) o;
        return emailChannel == that.emailChannel &&
                smsChannel == that.smsChannel &&
                targetType == that.targetType &&
                java.util.Objects.equals(targetValue, that.targetValue) &&
                java.util.Objects.equals(category, that.category) &&
                java.util.Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(targetType, targetValue, category, message, emailChannel, smsChannel);
    }
}
