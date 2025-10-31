package main.java.data.dao;

import main.java.config.DataSourceRegistry;
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
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
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

    public Optional<Faculty> findByCode(String code) {
        return fetchSingle(SELECT_BY_CODE, code);
    }

    public Optional<Faculty> findByUsername(String username) {
        return fetchSingle(SELECT_BY_USERNAME, username);
    }

    public void insert(Faculty faculty) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            bind(ps, faculty, true);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting instructor {}: {}", faculty.getFacultyId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert instructor", ex);
        }
    }

    public void update(Faculty faculty) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            bind(ps, faculty, false);
            ps.setString(11, faculty.getFacultyId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating instructor {}: {}", faculty.getFacultyId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update instructor", ex);
        }
    }

    public void delete(String code) {
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
