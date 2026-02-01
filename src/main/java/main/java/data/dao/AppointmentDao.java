package main.java.data.dao;

import main.java.models.Appointment;
import main.java.models.OfficeHour;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao extends BaseDao {

    public AppointmentDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insertAppointment(Appointment apt) {
        String sql = "INSERT INTO appointments (student_code, instructor_code, office_hour_id, appointment_date, start_time, end_time, purpose, status) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, apt.getStudentId());
            stmt.setString(2, apt.getFacultyId());
            if (apt.getOfficeHourId() != null) {
                stmt.setLong(3, apt.getOfficeHourId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }
            stmt.setDate(4, Date.valueOf(apt.getAppointmentDate()));
            stmt.setTime(5, Time.valueOf(apt.getStartTime()));
            stmt.setTime(6, Time.valueOf(apt.getEndTime()));
            stmt.setString(7, apt.getPurpose());
            stmt.setString(8, apt.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    apt.setAppointmentId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error booking appointment", e);
        }
    }

    public List<Appointment> getAppointmentsForStudent(String studentId) {
        String sql = "SELECT a.*, i.first_name, i.last_name FROM appointments a " +
                "JOIN instructors i ON a.instructor_code = i.instructor_code " +
                "WHERE a.student_code = ? ORDER BY a.appointment_date DESC, a.start_time DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Appointment a = mapRow(rs);
                    a.setFacultyName(rs.getString("first_name") + " " + rs.getString("last_name"));
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching appointments for student {}: {}", studentId, e.getMessage(), e);
        }
        return list;
    }

    public List<OfficeHour> getOfficeHours(String facultyId) {
        String sql = "SELECT * FROM instructor_office_hours WHERE instructor_code = ?";
        List<OfficeHour> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, facultyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OfficeHour oh = new OfficeHour();
                    oh.setOfficeHourId(rs.getLong("office_hour_id"));
                    oh.setFacultyId(rs.getString("instructor_code"));
                    oh.setDayOfWeek(DayOfWeek.valueOf(rs.getString("day_of_week")));
                    oh.setStartTime(rs.getTime("start_time").toLocalTime());
                    oh.setEndTime(rs.getTime("end_time").toLocalTime());
                    oh.setLocation(rs.getString("location"));
                    list.add(oh);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching office hours for faculty {}: {}", facultyId, e.getMessage(), e);
        }
        return list;
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getLong("appointment_id"));
        a.setStudentId(rs.getString("student_code"));
        a.setFacultyId(rs.getString("instructor_code"));
        long ohId = rs.getLong("office_hour_id");
        if (!rs.wasNull())
            a.setOfficeHourId(ohId);
        a.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        a.setStartTime(rs.getTime("start_time").toLocalTime());
        a.setEndTime(rs.getTime("end_time").toLocalTime());
        a.setStatus(Appointment.Status.valueOf(rs.getString("status")));
        a.setPurpose(rs.getString("purpose"));
        a.setRejectionReason(rs.getString("rejection_reason"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            a.setCreatedAt(createdAt.toLocalDateTime());
        }
        return a;
    }
}
