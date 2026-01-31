package main.java.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Per-user notification preference and digest configuration.
 */
public class NotificationPreference implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum DigestFrequency {
        IMMEDIATE,
        DAILY,
        WEEKLY,
        NONE
    }

    private final String userId;
    private DigestFrequency digestFrequency;
    private int digestHour;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private LocalDateTime updatedAt;

    public NotificationPreference(String userId,
            DigestFrequency digestFrequency,
            int digestHour,
            boolean emailEnabled,
            boolean smsEnabled,
            LocalDateTime updatedAt) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.digestFrequency = digestFrequency == null ? DigestFrequency.IMMEDIATE : digestFrequency;
        this.digestHour = digestHour;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreference defaultPreference(String userId) {
        return new NotificationPreference(userId, DigestFrequency.IMMEDIATE, 8, false, false, LocalDateTime.now());
    }

    public String getUserId() {
        return userId;
    }

    public DigestFrequency getDigestFrequency() {
        return digestFrequency;
    }

    public void setDigestFrequency(DigestFrequency digestFrequency) {
        if (digestFrequency != null) {
            this.digestFrequency = digestFrequency;
        }
    }

    public int getDigestHour() {
        return digestHour;
    }

    public void setDigestHour(int digestHour) {
        this.digestHour = digestHour;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NotificationPreference))
            return false;
        NotificationPreference that = (NotificationPreference) o;
        return java.util.Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId);
    }
}
