package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.Student;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDao extends BaseDao {
    private static final String BASE_SELECT = "SELECT id, student_code, auth_username, first_name, last_name, email, phone, date_of_birth, address, course_code, semester, status, fees_paid, total_fees, cgpa, credits_completed, credits_in_progress, next_fee_due, advisor_id, academic_standing FROM students";
    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY student_code";
    private static final String SELECT_BY_CODE = BASE_SELECT + " WHERE student_code = ?";
    private static final String SELECT_BY_USERNAME = BASE_SELECT + " WHERE auth_username = ?";
    private static final String INSERT = "INSERT INTO students (student_code, auth_username, first_name, last_name, email, phone, date_of_birth, address, course_code, semester, status, fees_paid, total_fees, cgpa, credits_completed, credits_in_progress, next_fee_due, advisor_id, academic_standing) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE students SET auth_username = ?, first_name = ?, last_name = ?, email = ?, phone = ?, date_of_birth = ?, address = ?, course_code = ?, semester = ?, status = ?, fees_paid = ?, total_fees = ?, cgpa = ?, credits_completed = ?, credits_in_progress = ?, next_fee_due = ?, advisor_id = ?, academic_standing = ? WHERE student_code = ?";
    private static final String DELETE = "DELETE FROM students WHERE student_code = ?";

    public StudentDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<Student> findAll() {
        List<Student> students = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(mapStudent(rs));
            }
        } catch (SQLException ex) {
            logger.error("Error loading students: {}", ex.getMessage(), ex);
        }
        return students;
    }

    public Optional<Student> findByCode(String code) {
        return fetchSingle(SELECT_BY_CODE, code);
    }

    public Optional<Student> findByUsername(String username) {
        return fetchSingle(SELECT_BY_USERNAME, username);
    }

    private Optional<Student> fetchSingle(String sql, String param) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapStudent(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error fetching student {}: {}", param, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public void insert(Student student) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getUsername());
            ps.setString(3, student.getFirstName());
            ps.setString(4, student.getLastName());
            ps.setString(5, student.getEmail());
            ps.setString(6, student.getPhone());
            if (student.getDateOfBirth() != null) {
                ps.setDate(7, Date.valueOf(student.getDateOfBirth()));
            } else {
                ps.setNull(7, java.sql.Types.DATE);
            }
            ps.setString(8, student.getAddress());
            ps.setString(9, student.getCourse());
            ps.setInt(10, student.getSemester());
            ps.setString(11, student.getStatus());
            ps.setDouble(12, student.getFeesPaid());
            ps.setDouble(13, student.getTotalFees());
            ps.setDouble(14, student.getCgpa());
            ps.setInt(15, student.getCreditsCompleted());
            ps.setInt(16, student.getCreditsInProgress());
            if (student.getNextFeeDueDate() != null) {
                ps.setDate(17, Date.valueOf(student.getNextFeeDueDate()));
            } else {
                ps.setNull(17, java.sql.Types.DATE);
            }
            ps.setString(18, student.getAdvisorId());
            ps.setString(19, student.getAcademicStanding());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting student {}: {}", student.getStudentId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert student", ex);
        }
    }

    public void update(Student student) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, student.getUsername());
            ps.setString(2, student.getFirstName());
            ps.setString(3, student.getLastName());
            ps.setString(4, student.getEmail());
            ps.setString(5, student.getPhone());
            if (student.getDateOfBirth() != null) {
                ps.setDate(6, Date.valueOf(student.getDateOfBirth()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }
            ps.setString(7, student.getAddress());
            ps.setString(8, student.getCourse());
            ps.setInt(9, student.getSemester());
            ps.setString(10, student.getStatus());
            ps.setDouble(11, student.getFeesPaid());
            ps.setDouble(12, student.getTotalFees());
            ps.setDouble(13, student.getCgpa());
            ps.setInt(14, student.getCreditsCompleted());
            ps.setInt(15, student.getCreditsInProgress());
            if (student.getNextFeeDueDate() != null) {
                ps.setDate(16, Date.valueOf(student.getNextFeeDueDate()));
            } else {
                ps.setNull(16, java.sql.Types.DATE);
            }
            ps.setString(17, student.getAdvisorId());
            ps.setString(18, student.getAcademicStanding());
            ps.setString(19, student.getStudentId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating student {}: {}", student.getStudentId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update student", ex);
        }
    }

    public void delete(String studentCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, studentCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting student {}: {}", studentCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete student", ex);
        }
    }

    private Student mapStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setStudentId(rs.getString("student_code"));
        student.setUsername(rs.getString("auth_username"));
        student.setFirstName(rs.getString("first_name"));
        student.setLastName(rs.getString("last_name"));
        student.setEmail(rs.getString("email"));
        student.setPhone(rs.getString("phone"));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) {
            student.setDateOfBirth(dob.toLocalDate());
        }
        student.setAddress(rs.getString("address"));
        student.setCourse(rs.getString("course_code"));
        student.setSemester(rs.getInt("semester"));
        student.setStatus(rs.getString("status"));
        student.setFeesPaid(rs.getDouble("fees_paid"));
        student.setTotalFees(rs.getDouble("total_fees"));
        student.setCgpa(rs.getDouble("cgpa"));
        student.setCreditsCompleted(rs.getInt("credits_completed"));
        student.setCreditsInProgress(rs.getInt("credits_in_progress"));
        Date nextDue = rs.getDate("next_fee_due");
        if (nextDue != null) {
            student.setNextFeeDueDate(nextDue.toLocalDate());
        }
        student.setAdvisorId(rs.getString("advisor_id"));
        student.setAcademicStanding(rs.getString("academic_standing"));
        return student;
    }

}
