package main.java.data.dao;

import main.java.models.Course;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CourseDao extends BaseDao {
    private static final String SELECT_ALL = "SELECT id, course_code, course_name, department, duration_semesters, fees, description, total_seats, available_seats, credit_hours FROM courses";
    private static final String SELECT_BY_CODE = SELECT_ALL + " WHERE course_code = ?";
    private static final String INSERT = "INSERT INTO courses (course_code, course_name, department, duration_semesters, fees, description, total_seats, available_seats, credit_hours) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE courses SET course_name = ?, department = ?, duration_semesters = ?, fees = ?, description = ?, total_seats = ?, available_seats = ?, credit_hours = ? WHERE course_code = ?";
    private static final String ATOMIC_DECREMENT_SEATS = "UPDATE courses SET available_seats = available_seats - 1 WHERE course_code = ? AND available_seats > 0";
    private static final String ATOMIC_INCREMENT_SEATS = "UPDATE courses SET available_seats = LEAST(total_seats, available_seats + 1) WHERE course_code = ?";
    private static final String DELETE = "DELETE FROM courses WHERE course_code = ?";

    private final Map<String, Course> courseCache = new ConcurrentHashMap<>();
    private volatile boolean cacheInitialized = false;

    public CourseDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    public List<Course> findAll() {
        if (!cacheInitialized) {
            synchronized (courseCache) {
                if (!cacheInitialized) {
                    refreshCache();
                }
            }
        }
        return new ArrayList<>(courseCache.values());
    }

    private void refreshCache() {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            courseCache.clear();
            while (rs.next()) {
                Course c = mapCourse(rs);
                courseCache.put(c.getCourseId(), c);
            }
            cacheInitialized = true;
        } catch (SQLException ex) {
            logger.error("Error loading courses for cache: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Finds a course by its course code.
     * Uses cache for performance, falls back to database if not cached.
     * 
     * @param courseCode the course code to search for (must not be null or empty)
     * @return Optional containing the course if found, empty otherwise
     * @throws IllegalArgumentException if courseCode is null or empty
     */
    public Optional<Course> findByCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        if (!cacheInitialized) {
            findAll(); // Force cache initialization
        }
        Course cached = courseCache.get(courseCode);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Fallback to DB if not in cache (e.g. newly added but cache not synced?)
        // OR if cache init failed partially.
        return fetchFromDb(courseCode);
    }

    /**
     * Fetches a course from the database by code.
     * Updates cache with the result.
     * 
     * @param courseCode the course code (must not be null or empty)
     * @return Optional containing the course if found, empty otherwise
     */
    private Optional<Course> fetchFromDb(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return Optional.empty();
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Course c = mapCourse(rs);
                    courseCache.put(c.getCourseId(), c); // repair cache
                    return Optional.of(c);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error fetching course {}: {}", courseCode, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /**
     * Inserts a new course into the database and updates cache.
     * 
     * @param course the course to insert (must not be null with valid data)
     * @throws IllegalArgumentException if course is null or has invalid data
     * @throws RuntimeException         if database operation fails
     */
    public void insert(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course cannot be null");
        }
        if (course.getCourseId() == null || course.getCourseId().trim().isEmpty()) {
            throw new IllegalArgumentException("Course ID cannot be null or empty");
        }
        if (course.getCourseName() == null || course.getCourseName().trim().isEmpty()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }
        if (course.getTotalSeats() < 0) {
            throw new IllegalArgumentException("Total seats cannot be negative");
        }
        if (course.getAvailableSeats() < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative");
        }
        if (course.getCreditHours() <= 0) {
            throw new IllegalArgumentException("Credit hours must be greater than 0");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, course.getCourseId());
            ps.setString(2, course.getCourseName());
            ps.setString(3, course.getDepartment());
            ps.setInt(4, course.getDuration());
            ps.setDouble(5, course.getFees());
            ps.setString(6, course.getDescription());
            ps.setInt(7, course.getTotalSeats());
            ps.setInt(8, course.getAvailableSeats());
            ps.setInt(9, course.getCreditHours());
            ps.executeUpdate();

            // Update cache
            courseCache.put(course.getCourseId(), course);
        } catch (SQLException ex) {
            logger.error("Error inserting course {}: {}", course.getCourseId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert course", ex);
        }
    }

    public void update(Course course) {
        try (Connection conn = getConnection()) {
            update(conn, course);
            // Cache update handled in overloaded method if successful,
            // but since that method takes a connection and might be part of a transaction,
            // we should be careful.
            // However, this simple method is standalone.
            courseCache.put(course.getCourseId(), course);
        } catch (SQLException ex) {
            logger.error("Error updating course {}: {}", course.getCourseId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update course", ex);
        }
    }

    public void update(Connection conn, Course course) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, course.getCourseName());
            ps.setString(2, course.getDepartment());
            ps.setInt(3, course.getDuration());
            ps.setDouble(4, course.getFees());
            ps.setString(5, course.getDescription());
            ps.setInt(6, course.getTotalSeats());
            ps.setInt(7, course.getAvailableSeats());
            ps.setInt(8, course.getCreditHours());
            ps.setString(9, course.getCourseId());
            ps.executeUpdate();

            // Note: Cache invalidation should be handled by the caller after transaction
            // commits
            // to avoid cache inconsistency if transaction rolls back
        }
    }

    public boolean decrementAvailableSeats(Connection conn, String courseCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ATOMIC_DECREMENT_SEATS)) {
            ps.setString(1, courseCode);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                // Invalidate to force reload
                courseCache.remove(courseCode);
                return true;
            }
            return false;
        }
    }

    public void incrementAvailableSeats(Connection conn, String courseCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ATOMIC_INCREMENT_SEATS)) {
            ps.setString(1, courseCode);
            ps.executeUpdate();
            courseCache.remove(courseCode);
        }
    }

    public void delete(String courseCode) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, courseCode);
            ps.executeUpdate();
            courseCache.remove(courseCode);
        } catch (SQLException ex) {
            logger.error("Error deleting course {}: {}", courseCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete course", ex);
        }
    }

    private Course mapCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getString("course_code"));
        course.setCourseName(rs.getString("course_name"));
        course.setDepartment(rs.getString("department"));
        course.setDuration(rs.getInt("duration_semesters"));
        course.setFees(rs.getDouble("fees"));
        course.setDescription(rs.getString("description"));
        int total = rs.getInt("total_seats");
        course.setTotalSeats(total);
        course.setAvailableSeats(rs.getInt("available_seats"));
        course.setCreditHours(rs.getInt("credit_hours"));
        return course;
    }
}
