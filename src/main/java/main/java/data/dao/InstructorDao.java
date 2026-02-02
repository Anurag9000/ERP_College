package main.java.data.dao;

import main.java.models.Faculty;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstructorDao extends BaseDao {
    private static final String BASE_SELECT = "SELECT id, instructor_code, auth_username, first_name, last_name, email, phone, department, designation, qualification, status, joining_date, salary FROM instructors";
    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY instructor_code";
    private static final String SELECT_BY_CODE = BASE_SELECT + " WHERE instructor_code = ?";
    private static final String SELECT_BY_USERNAME = BASE_SELECT + " WHERE auth_username = ?";
    private static final String INSERT = "INSERT INTO instructors (instructor_code, auth_username, first_name, last_name, email, phone, department, designation, qualification, status, joining_date, salary) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE instructors SET auth_username = ?, first_name = ?, last_name = ?, email = ?, phone = ?, department = ?, designation = ?, qualification = ?, status = ?, joining_date = ?, salary = ? WHERE instructor_code = ?";
    private static final String DELETE = "DELETE FROM instructors WHERE instructor_code = ?";

    public InstructorDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    public List<Faculty> findAll() {
        List<Faculty> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapFaculty(rs));
            }
        } catch (SQLException ex) {
            logger.error("Error loading instructors: {}", ex.getMessage(), ex);
        }
        return list;
    }

    /**
     * Finds an instructor by instructor code.
     * 
     * @param code the instructor code (must not be null or empty)
     * @return Optional containing the instructor if found, empty otherwise
     * @throws IllegalArgumentException if code is null or empty
     */
    public Optional<Faculty> findByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Instructor code cannot be null or empty");
        }
        return fetchSingle(SELECT_BY_CODE, code);
    }

    /**
     * Finds an instructor by username.
     * 
     * @param username the username (must not be null or empty)
     * @return Optional containing the instructor if found, empty otherwise
     * @throws IllegalArgumentException if username is null or empty
     */
    public Optional<Faculty> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return fetchSingle(SELECT_BY_USERNAME, username);
    }

    /**
     * Inserts a new instructor into the database.
     * 
     * @param faculty the instructor to insert (must not be null with valid data)
     * @throws IllegalArgumentException if faculty is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void insert(Faculty faculty) {
        if (faculty == null) {
            throw new IllegalArgumentException("Faculty cannot be null");
        }
        if (faculty.getFacultyId() == null || faculty.getFacultyId().trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be null or empty");
        }
        if (faculty.getFirstName() == null || faculty.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (faculty.getLastName() == null || faculty.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        try (Connection conn = getConnection()) {
            insert(conn, faculty);
        } catch (SQLException ex) {
            logger.error("Error inserting instructor {}: {}", faculty.getFacultyId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert instructor", ex);
        }
    }

    public void insert(Connection conn, Faculty faculty) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            bind(ps, faculty, true);
            ps.executeUpdate();
        }
    }

    /**
     * Updates an existing instructor.
     * 
     * @param faculty the instructor to update (must not be null with valid data)
     * @throws IllegalArgumentException if faculty is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void update(Faculty faculty) {
        if (faculty == null) {
            throw new IllegalArgumentException("Faculty cannot be null");
        }
        if (faculty.getFacultyId() == null || faculty.getFacultyId().trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            bind(ps, faculty, false);
            ps.setString(12, faculty.getFacultyId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating instructor {}: {}", faculty.getFacultyId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update instructor", ex);
        }
    }

    /**
     * Deletes an instructor by code.
     * 
     * @param code the instructor code (must not be null or empty)
     * @throws IllegalArgumentException if code is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public void delete(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Instructor code cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, code);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting instructor {}: {}", code, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete instructor", ex);
        }
    }

    private Optional<Faculty> fetchSingle(String sql, String param) {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapFaculty(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error fetching instructor {}: {}", param, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    private Faculty mapFaculty(ResultSet rs) throws SQLException {
        Faculty faculty = new Faculty();
        faculty.setFacultyId(rs.getString("instructor_code"));
        faculty.setUsername(rs.getString("auth_username"));
        faculty.setFirstName(rs.getString("first_name"));
        faculty.setLastName(rs.getString("last_name"));
        faculty.setEmail(rs.getString("email"));
        faculty.setPhone(rs.getString("phone"));
        faculty.setDepartment(rs.getString("department"));
        faculty.setDesignation(rs.getString("designation"));
        faculty.setQualification(rs.getString("qualification"));
        faculty.setStatus(rs.getString("status"));
        Date joining = rs.getDate("joining_date");
        if (joining != null) {
            faculty.setJoiningDate(joining.toLocalDate());
        }
        faculty.setSalary(rs.getDouble("salary"));
        return faculty;
    }

    private void bind(PreparedStatement ps, Faculty faculty, boolean includeCode) throws SQLException {
        int idx = 1;
        if (includeCode) {
            ps.setString(idx++, faculty.getFacultyId());
        }
        ps.setString(idx++, faculty.getUsername());
        ps.setString(idx++, faculty.getFirstName());
        ps.setString(idx++, faculty.getLastName());
        ps.setString(idx++, faculty.getEmail());
        ps.setString(idx++, faculty.getPhone());
        ps.setString(idx++, faculty.getDepartment());
        ps.setString(idx++, faculty.getDesignation());
        ps.setString(idx++, faculty.getQualification());
        ps.setString(idx++, faculty.getStatus());
        if (faculty.getJoiningDate() != null) {
            ps.setDate(idx++, Date.valueOf(faculty.getJoiningDate()));
        } else {
            ps.setNull(idx++, java.sql.Types.DATE);
        }
        ps.setDouble(idx, faculty.getSalary());
    }
}
