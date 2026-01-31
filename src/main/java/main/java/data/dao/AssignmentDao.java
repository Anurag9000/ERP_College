package main.java.data.dao;

import main.java.models.Assignment;
import main.java.models.AssignmentSubmission;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDao extends BaseDao {

    public AssignmentDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insertAssignment(Assignment assignment) {
        String sql = "INSERT INTO assignments (section_code, title, description, due_date, max_marks, assignment_type) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, assignment.getSectionCode());
            stmt.setString(2, assignment.getTitle());
            stmt.setString(3, assignment.getDescription());
            stmt.setTimestamp(4, Timestamp.valueOf(assignment.getDueDate()));
            stmt.setDouble(5, assignment.getMaxMarks());
            stmt.setString(6, assignment.getAssignmentType().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    assignment.setAssignmentId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating assignment", e);
        }
    }

    public List<Assignment> getAssignmentsBySection(String sectionCode) {
        String sql = "SELECT * FROM assignments WHERE section_code = ? ORDER BY due_date";
        List<Assignment> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sectionCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAssignment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Assignment> getUpcomingAssignments(String studentCode) {
        String sql = "SELECT a.* FROM assignments a " +
                "JOIN enrollments e ON a.section_code = e.section_code " +
                "WHERE e.student_code = ? AND a.due_date > NOW() " +
                "ORDER BY a.due_date LIMIT 10";
        List<Assignment> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAssignment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void submitAssignment(AssignmentSubmission submission) {
        String sql = "INSERT INTO assignment_submissions (assignment_id, student_code, file_path, status) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE file_path = ?, submitted_at = NOW(), status = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, submission.getAssignmentId());
            stmt.setString(2, submission.getStudentCode());
            stmt.setString(3, submission.getFilePath());
            stmt.setString(4, submission.getStatus().name());
            stmt.setString(5, submission.getFilePath());
            stmt.setString(6, submission.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    submission.setSubmissionId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error submitting assignment", e);
        }
    }

    public void gradeSubmission(long submissionId, double marks, String feedback) {
        String sql = "UPDATE assignment_submissions SET marks_obtained = ?, feedback = ?, status = 'GRADED' " +
                "WHERE submission_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, marks);
            stmt.setString(2, feedback);
            stmt.setLong(3, submissionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error grading submission", e);
        }
    }

    public List<AssignmentSubmission> getSubmissionsByAssignment(long assignmentId) {
        String sql = "SELECT s.*, st.first_name, st.last_name FROM assignment_submissions s " +
                "JOIN students st ON s.student_code = st.student_code " +
                "WHERE s.assignment_id = ?";
        List<AssignmentSubmission> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, assignmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AssignmentSubmission sub = mapSubmission(rs);
                    sub.setStudentName(rs.getString("first_name") + " " + rs.getString("last_name"));
                    list.add(sub);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public AssignmentSubmission getSubmission(long assignmentId, String studentCode) {
        String sql = "SELECT * FROM assignment_submissions WHERE assignment_id = ? AND student_code = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, assignmentId);
            stmt.setString(2, studentCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapSubmission(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Assignment mapAssignment(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setAssignmentId(rs.getLong("assignment_id"));
        a.setSectionCode(rs.getString("section_code"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        a.setDueDate(rs.getTimestamp("due_date").toLocalDateTime());
        a.setMaxMarks(rs.getDouble("max_marks"));
        a.setAssignmentType(Assignment.AssignmentType.valueOf(rs.getString("assignment_type")));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    }

    private AssignmentSubmission mapSubmission(ResultSet rs) throws SQLException {
        AssignmentSubmission s = new AssignmentSubmission();
        s.setSubmissionId(rs.getLong("submission_id"));
        s.setAssignmentId(rs.getLong("assignment_id"));
        s.setStudentCode(rs.getString("student_code"));
        s.setSubmittedAt(rs.getTimestamp("submitted_at").toLocalDateTime());
        s.setFilePath(rs.getString("file_path"));
        Double marks = rs.getDouble("marks_obtained");
        if (!rs.wasNull())
            s.setMarksObtained(marks);
        s.setFeedback(rs.getString("feedback"));
        s.setStatus(Assignment.SubmissionStatus.valueOf(rs.getString("status")));
        return s;
    }
}
