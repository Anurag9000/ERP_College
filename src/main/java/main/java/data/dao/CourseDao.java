package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.Course;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseDao extends BaseDao {
    private static final String SELECT_ALL = "SELECT id, course_code, course_name, department, duration_semesters, fees, description, total_seats, available_seats FROM courses";
    private static final String SELECT_BY_CODE = SELECT_ALL + " WHERE course_code = ?";
    private static final String INSERT = "INSERT INTO courses (course_code, course_name, department, duration_semesters, fees, description, total_seats, available_seats) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE courses SET course_name = ?, department = ?, duration_semesters = ?, fees = ?, description = ?, total_seats = ?, available_seats = ? WHERE course_code = ?";
    private static final String DELETE = "DELETE FROM courses WHERE course_code = ?";

    public CourseDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<Course> findAll() {
        List<Course> courses = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courses.add(mapCourse(rs));
            }
        } catch (SQLException ex) {
            logger.error("Error loading courses: {}", ex.getMessage(), ex);
        }
        return courses;
    }

    public Optional<Course> findByCode(String courseCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCourse(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error fetching course {}: {}", courseCode, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public void insert(Course course) {
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
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting course {}: {}", course.getCourseId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert course", ex);
        }
    }

    public void update(Course course) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, course.getCourseName());
            ps.setString(2, course.getDepartment());
            ps.setInt(3, course.getDuration());
            ps.setDouble(4, course.getFees());
            ps.setString(5, course.getDescription());
            ps.setInt(6, course.getTotalSeats());
            ps.setInt(7, course.getAvailableSeats());
            ps.setString(8, course.getCourseId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating course {}: {}", course.getCourseId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update course", ex);
        }
    }

    public void delete(String courseCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, courseCode);
            ps.executeUpdate();
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
        return course;
    }
}
