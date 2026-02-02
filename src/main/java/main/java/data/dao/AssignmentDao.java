package main.java.data.dao;

import main.java.models.Assignment;
import main.java.models.AssignmentSubmission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDao extends BaseDao {

    public AssignmentDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Inserts a new assignment into the database.
     * 
     * @param assignment the assignment to insert (must not be null with valid data)
     * @throws IllegalArgumentException if assignment is null or has invalid data
     * @throws RuntimeException         if database operation fails
     */
    public void insertAssignment(Assignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }
        if (assignment.getSectionCode() == null || assignment.getSectionCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title cannot be null or empty");
        }
        if (assignment.getDueDate() == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        if (assignment.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Max marks must be greater than 0");
        }
        if (assignment.getAssignmentType() == null) {
            throw new IllegalArgumentException("Assignment type cannot be null");
        }
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

    /**
     * Retrieves all assignments for a specific section.
     * 
     * @param sectionCode the section code (must not be null or empty)
     * @return list of assignments, never null (empty if none found)
     * @throws IllegalArgumentException if sectionCode is null or empty
     */
    public List<Assignment> getAssignmentsBySection(String sectionCode) {
        if (sectionCode == null || sectionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
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
            logger.error("Error fetching assignments for section {}: {}", sectionCode, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieves upcoming assignments for a specific student.
     * 
     * @param studentCode the student code (must not be null or empty)
     * @return list of upcoming assignments, never null (empty if none found)
     * @throws IllegalArgumentException if studentCode is null or empty
     */
    public List<Assignment> getUpcomingAssignments(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
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
            logger.error("Error fetching upcoming assignments for student {}: {}", studentCode, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Submits an assignment for a student.
     * 
     * @param submission the assignment submission (must not be null with valid
     *                   data)
     * @throws IllegalArgumentException if submission is null or has invalid data
     * @throws RuntimeException         if database operation fails
     */
    public void submitAssignment(AssignmentSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission cannot be null");
        }
        if (submission.getAssignmentId() <= 0) {
            throw new IllegalArgumentException("Assignment ID must be greater than 0");
        }
        if (submission.getStudentCode() == null || submission.getStudentCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
        if (submission.getFilePath() == null || submission.getFilePath().trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (submission.getStatus() == null) {
            throw new IllegalArgumentException("Submission status cannot be null");
        }
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

    /**
     * Grades a student's assignment submission.
     * 
     * @param submissionId the submission ID (must be greater than 0)
     * @param marks        the marks obtained (must be non-negative)
     * @param feedback     optional feedback text
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException         if database operation fails
     */
    public void gradeSubmission(long submissionId, double marks, String feedback) {
        if (submissionId <= 0) {
            throw new IllegalArgumentException("Submission ID must be greater than 0");
        }
        if (marks < 0) {
            throw new IllegalArgumentException("Marks cannot be negative");
        }
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
            logger.error("Error fetching submissions for assignment {}: {}", assignmentId, e.getMessage(), e);
        }
        return list;
    }

    public List<AssignmentSubmission> getSubmissionsByStudent(String studentCode) {
        String sql = "SELECT s.*, a.title FROM assignment_submissions s " +
                "JOIN assignments a ON s.assignment_id = a.assignment_id " +
                "WHERE s.student_code = ? ORDER BY s.submitted_at DESC";
        List<AssignmentSubmission> list = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AssignmentSubmission sub = mapSubmission(rs);
                    sub.setAssignmentTitle(rs.getString("title")); // Need to ensure AssignmentSubmission has title
                                                                   // field
                    list.add(sub);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching submissions for student {}: {}", studentCode, e.getMessage(), e);
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
            logger.error("Error fetching submission for assignment {} and student {}: {}", assignmentId, studentCode,
                    e.getMessage(), e);
        }
        return null;
    }

    private Assignment mapAssignment(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setAssignmentId(rs.getLong("assignment_id"));
        a.setSectionCode(rs.getString("section_code"));
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        Timestamp dueDate = rs.getTimestamp("due_date");
        if (dueDate != null) {
            a.setDueDate(dueDate.toLocalDateTime());
        }
        a.setMaxMarks(rs.getDouble("max_marks"));
        a.setAssignmentType(Assignment.AssignmentType.valueOf(rs.getString("assignment_type")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            a.setCreatedAt(createdAt.toLocalDateTime());
        }
        return a;
    }

    private AssignmentSubmission mapSubmission(ResultSet rs) throws SQLException {
        AssignmentSubmission s = new AssignmentSubmission();
        s.setSubmissionId(rs.getLong("submission_id"));
        s.setAssignmentId(rs.getLong("assignment_id"));
        s.setStudentCode(rs.getString("student_code"));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            s.setSubmittedAt(submittedAt.toLocalDateTime());
        }
        s.setFilePath(rs.getString("file_path"));
        Double marks = rs.getDouble("marks_obtained");
        if (!rs.wasNull())
            s.setMarksObtained(marks);
        s.setFeedback(rs.getString("feedback"));
        s.setStatus(Assignment.SubmissionStatus.valueOf(rs.getString("status")));
        return s;
    }
}
