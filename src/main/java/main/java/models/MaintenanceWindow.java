package main.java.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a scheduled maintenance window with lifecycle state.
 */
public class MaintenanceWindow implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SCHEDULED,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    private final long id;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String message;
    private final Status status;
    private final String createdBy;
    private final LocalDateTime createdAt;

    public MaintenanceWindow(long id,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String message,
            Status status,
            String createdBy,
            LocalDateTime createdAt) {
        this.id = id;
        this.startAt = startAt;
        this.endAt = endAt;
        this.message = message;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isUpcoming(LocalDateTime reference) {
        return status == Status.SCHEDULED && startAt.isAfter(reference);
    }

    public boolean isActive(LocalDateTime reference) {
        return (status == Status.ACTIVE || status == Status.SCHEDULED)
                && !reference.isBefore(startAt)
                && reference.isBefore(endAt);
    }

    public Duration timeUntilStart(LocalDateTime reference) {
        if (reference.isAfter(startAt)) {
            return Duration.ZERO;
        }
        return Duration.between(reference, startAt);
    }

    public Duration timeUntilEnd(LocalDateTime reference) {
        if (reference.isAfter(endAt)) {
            return Duration.ZERO;
        }
        return Duration.between(reference, endAt);
    }

    public MaintenanceWindow withStatus(Status newStatus) {
        if (Objects.equals(this.status, newStatus)) {
            return this;
        }
        return new MaintenanceWindow(id, startAt, endAt, message, newStatus, createdBy, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MaintenanceWindow))
            return false;
        MaintenanceWindow that = (MaintenanceWindow) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
