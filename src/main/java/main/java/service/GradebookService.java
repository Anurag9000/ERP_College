package main.java.service;

import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.AuditLogService;
import main.java.utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles instructor gradebook operations and summary statistics.
 */
public final class GradebookService {

    private GradebookService() {
    }

    public static void defineAssessments(User instructor, String sectionId, Map<String, Double> weights) {
        ensureInstructorAccess(instructor, sectionId);
        Section section = DatabaseUtil.getSection(sectionId);
        section.clearAssessmentWeights();
        weights.forEach(section::setAssessmentWeight);
        DatabaseUtil.updateSection(section);
        AuditLogService.log(AuditLogService.EventType.GRADE_EDIT,
                instructor.getUsername(),
                String.format("Defined assessments for %s (%d components)", sectionId, weights.size()));
    }

    public static void recordScore(User instructor, String sectionId, String studentId, String component, double score) {
        ensureInstructorAccess(instructor, sectionId);
        EnrollmentRecord record = locateEnrollment(sectionId, studentId);
        record.putScore(component, score);
        DatabaseUtil.saveData();
        AuditLogService.log(AuditLogService.EventType.GRADE_EDIT,
                instructor.getUsername(),
                String.format("Recorded %s=%.2f for %s in %s", component, score, studentId, sectionId));
    }

    public static double computeFinal(User instructor, String sectionId, String studentId) {
        ensureInstructorAccess(instructor, sectionId);
        EnrollmentRecord record = locateEnrollment(sectionId, studentId);
        Section section = DatabaseUtil.getSection(sectionId);
        double finalGrade = section.computeFinalScore(record.getComponentScores());
        record.setFinalGrade(finalGrade);
        record.setWeighting(new HashMap<>(section.getAssessmentWeights()));
        record.setUpdatedAt(LocalDateTime.now());
        DatabaseUtil.saveData();
        AuditLogService.log(AuditLogService.EventType.GRADE_EDIT,
                instructor.getUsername(),
                String.format("Computed final grade %.2f for %s in %s", finalGrade, studentId, sectionId));
        return finalGrade;
    }

    public static DoubleSummaryStatistics statsForSection(User instructor, String sectionId) {
        ensureInstructorAccess(instructor, sectionId);
        List<EnrollmentRecord> records = DatabaseUtil.getEnrollmentsForSection(sectionId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .collect(Collectors.toList());
        return records.stream()
                .mapToDouble(EnrollmentRecord::getFinalGrade)
                .summaryStatistics();
    }

    private static void ensureInstructorAccess(User instructor, String sectionId) {
        if (instructor == null) {
            throw new SecurityException("Missing instructor session.");
        }
        if (DatabaseUtil.isMaintenanceMode()) {
            throw new IllegalStateException("System is in maintenance mode.");
        }
        Section section = DatabaseUtil.getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        Faculty faculty = DatabaseUtil.findFacultyByUsername(instructor.getUsername());
        if (faculty == null || !Objects.equals(faculty.getFacultyId(), section.getFacultyId())) {
            throw new SecurityException("You are not assigned to this section.");
        }
    }

    private static EnrollmentRecord locateEnrollment(String sectionId, String studentId) {
        return DatabaseUtil.getEnrollmentsForSection(sectionId).stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Student not enrolled in section."));
    }
}
