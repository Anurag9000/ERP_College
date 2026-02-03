package main.java.models;

import java.time.LocalDateTime;

public class Announcement implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum Category {
        DEPARTMENT, UNION, COLLEGE, UNIVERSITY, SOCIETY
    }

    public enum Priority {
        HIGH, NORMAL, LOW
    }

    private long announcementId;
    private Category category;
    private String department;
    private String title;
    private String content;
    private String postedBy;
    private LocalDateTime postedAt;
    private LocalDateTime expiresAt;
    private Priority priority;

    public Announcement() {
    }

    public long getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(long announcementId) {
        this.announcementId = announcementId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Sets the announcement title.
     * 
     * @param title the title (must not be null or empty)
     * @throws IllegalArgumentException if title is null or empty
     */
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    /**
     * Sets the announcement content.
     * 
     * @param content the content (must not be null or empty)
     * @throws IllegalArgumentException if content is null or empty
     */
    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        this.content = content;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Announcement))
            return false;
        Announcement that = (Announcement) o;
        return announcementId == that.announcementId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(announcementId);
    }
}
