package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.Section;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SectionDao extends BaseDao {
    private static final String BASE_SELECT = "SELECT id, section_code, course_code, title, instructor_code, day_of_week, start_time, end_time, location, capacity, enrollment_deadline, drop_deadline, semester, year FROM sections";
    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY section_code";
    private static final String SELECT_BY_CODE = BASE_SELECT + " WHERE section_code = ?";
    private static final String INSERT = "INSERT INTO sections (section_code, course_code, title, instructor_code, day_of_week, start_time, end_time, location, capacity, enrollment_deadline, drop_deadline, semester, year) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE sections SET course_code = ?, title = ?, instructor_code = ?, day_of_week = ?, start_time = ?, end_time = ?, location = ?, capacity = ?, enrollment_deadline = ?, drop_deadline = ?, semester = ?, year = ? WHERE section_code = ?";
    private static final String DELETE = "DELETE FROM sections WHERE section_code = ?";

    public SectionDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public List<Section> findAll() {
        List<Section> sections = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sections.add(mapSection(rs));
            }
        } catch (SQLException ex) {
            logger.error("Error loading sections: {}", ex.getMessage(), ex);
        }
        return sections;
    }

    public Optional<Section> findByCode(String code) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSection(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Error fetching section {}: {}", code, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public void insert(Section section) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            bind(ps, section, true);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error inserting section {}: {}", section.getSectionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to insert section", ex);
        }
    }

    public void update(Section section) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            bind(ps, section, false);
            ps.setString(13, section.getSectionId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error updating section {}: {}", section.getSectionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update section", ex);
        }
    }

    public void delete(String sectionCode) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setString(1, sectionCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Error deleting section {}: {}", sectionCode, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete section", ex);
        }
    }

    private Section mapSection(ResultSet rs) throws SQLException {
        Section section = new Section();
        section.setSectionId(rs.getString("section_code"));
        section.setCourseId(rs.getString("course_code"));
        section.setTitle(rs.getString("title"));
        section.setFacultyId(rs.getString("instructor_code"));
        String day = rs.getString("day_of_week");
        if (day != null) {
            section.setDayOfWeek(DayOfWeek.valueOf(day));
        }
        Time start = rs.getTime("start_time");
        if (start != null) {
            section.setStartTime(start.toLocalTime());
        }
        Time end = rs.getTime("end_time");
        if (end != null) {
            section.setEndTime(end.toLocalTime());
        }
        section.setLocation(rs.getString("location"));
        section.setCapacity(rs.getInt("capacity"));
        Date enroll = rs.getDate("enrollment_deadline");
        if (enroll != null) {
            section.setEnrollmentDeadline(enroll.toLocalDate());
        }
        Date drop = rs.getDate("drop_deadline");
        if (drop != null) {
            section.setDropDeadline(drop.toLocalDate());
        }
        section.setSemester(rs.getString("semester"));
        section.setYear(rs.getInt("year"));
        return section;
    }

    private void bind(PreparedStatement ps, Section section, boolean includeCode) throws SQLException {
        int idx = 1;
        if (includeCode) {
            ps.setString(idx++, section.getSectionId());
        }
        ps.setString(idx++, section.getCourseId());
        ps.setString(idx++, section.getTitle());
        ps.setString(idx++, section.getFacultyId());
        if (section.getDayOfWeek() != null) {
            ps.setString(idx++, section.getDayOfWeek().name());
        } else {
            ps.setNull(idx++, java.sql.Types.VARCHAR);
        }
        if (section.getStartTime() != null) {
            ps.setTime(idx++, Time.valueOf(section.getStartTime()));
        } else {
            ps.setNull(idx++, java.sql.Types.TIME);
        }
        if (section.getEndTime() != null) {
            ps.setTime(idx++, Time.valueOf(section.getEndTime()));
        } else {
            ps.setNull(idx++, java.sql.Types.TIME);
        }
        ps.setString(idx++, section.getLocation());
        ps.setInt(idx++, section.getCapacity());
        if (section.getEnrollmentDeadline() != null) {
            ps.setDate(idx++, Date.valueOf(section.getEnrollmentDeadline()));
        } else {
            ps.setNull(idx++, java.sql.Types.DATE);
        }
        if (section.getDropDeadline() != null) {
            ps.setDate(idx++, Date.valueOf(section.getDropDeadline()));
        } else {
            ps.setNull(idx++, java.sql.Types.DATE);
        }
        ps.setString(idx++, section.getSemester());
        ps.setInt(idx, section.getYear());
    }
}
