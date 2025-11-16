package main.java.service;

import main.java.data.dao.AssessmentTemplateDao;
import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.AuditLogService;
import main.java.utils.DatabaseUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public static GradeAnalytics gradeAnalyticsForSection(User instructor, String sectionId) {
        ensureInstructorAccess(instructor, sectionId);
        Section section = DatabaseUtil.getSection(sectionId);
        List<EnrollmentRecord> records = DatabaseUtil.getEnrollmentsForSection(sectionId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .collect(Collectors.toList());
        if (records.isEmpty()) {
            return new GradeAnalytics(0.0, 0.0, 0.0, 0, 0, Map.of());
        }
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("A (85+)", 0L);
        buckets.put("B (70-84)", 0L);
        buckets.put("C (55-69)", 0L);
        buckets.put("D (40-54)", 0L);
        buckets.put("F (<40)", 0L);

        double passingThreshold = DatabaseUtil.getPassingGradeThreshold();
        long pass = 0;
        long fail = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double total = 0.0;
        int counted = 0;

        for (EnrollmentRecord record : records) {
            double grade = record.getFinalGrade();
            if (grade <= 0) {
                grade = section.computeFinalScore(record.getComponentScores());
            }
            if (grade <= 0) {
                continue;
            }
            counted++;
            total += grade;
            min = Math.min(min, grade);
            max = Math.max(max, grade);

            if (grade >= passingThreshold) {
                pass++;
            } else {
                fail++;
            }
            buckets.compute(bucketFor(grade), (k, v) -> v == null ? 1 : v + 1);
        }

        if (counted == 0) {
            return new GradeAnalytics(0.0, 0.0, 0.0, pass, fail, Map.copyOf(buckets));
        }
        double average = total / counted;
        return new GradeAnalytics(average, max, min, pass, fail, Map.copyOf(buckets));
    }

    public static AttendanceAnalytics attendanceAnalyticsForSection(User instructor, String sectionId) {
        ensureInstructorAccess(instructor, sectionId);
        List<main.java.models.AttendanceRecord> history = DatabaseUtil.getAttendanceForSection(sectionId);
        if (history.isEmpty()) {
            return new AttendanceAnalytics(0.0, 0, List.of());
        }
        List<AttendanceSnapshot> snapshots = history.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .map(record -> new AttendanceSnapshot(record.getDate(), record.getAttendancePercentage()))
                .collect(Collectors.toList());
        double average = snapshots.stream().mapToDouble(AttendanceSnapshot::percentage).average().orElse(0.0);
        return new AttendanceAnalytics(average, snapshots.size(), snapshots);
    }

    public static List<AssessmentTemplateDao.AssessmentTemplate> listTemplates(User instructor, String courseCode) {
        if (instructor == null) {
            throw new SecurityException("Missing instructor session.");
        }
        return DatabaseUtil.getAssessmentTemplates(courseCode);
    }

    public static AssessmentTemplateDao.AssessmentTemplate saveTemplate(User instructor,
                                                                        String courseCode,
                                                                        String templateName,
                                                                        Map<String, Double> weights) {
        if (instructor == null) {
            throw new SecurityException("Missing instructor session.");
        }
        return DatabaseUtil.createAssessmentTemplate(courseCode, templateName, weights, instructor.getUsername());
    }

    public static void deleteTemplate(User instructor, long templateId) {
        if (instructor == null) {
            throw new SecurityException("Missing instructor session.");
        }
        DatabaseUtil.deleteAssessmentTemplate(templateId);
    }

    public static void applyTemplate(User instructor, long templateId, String sectionId) {
        ensureInstructorAccess(instructor, sectionId);
        DatabaseUtil.applyAssessmentTemplate(templateId, sectionId);
    }

    public static Section.GradebookState getGradebookState(User instructor, String sectionId) {
        ensureInstructorAccess(instructor, sectionId);
        return DatabaseUtil.getGradebookState(sectionId);
    }

    public static void updateGradebookState(User instructor, String sectionId, Section.GradebookState state) {
        ensureInstructorAccess(instructor, sectionId);
        DatabaseUtil.updateGradebookState(sectionId, state);
    }

    public static Map<String, String> getFeedback(User instructor, String sectionId, String studentId) {
        ensureInstructorAccess(instructor, sectionId);
        return DatabaseUtil.getComponentFeedback(sectionId, studentId);
    }

    public static void saveFeedback(User instructor, String sectionId, String studentId, Map<String, String> feedback) {
        ensureInstructorAccess(instructor, sectionId);
        DatabaseUtil.saveComponentFeedback(sectionId, studentId,
                feedback == null ? Collections.emptyMap() : feedback);
    }

    public static void saveFeedback(User instructor, String sectionId, String studentId, String component, String comment) {
        ensureInstructorAccess(instructor, sectionId);
        DatabaseUtil.saveComponentFeedbackEntry(sectionId, studentId, component, comment);
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

    private static String bucketFor(double grade) {
        if (grade >= 85) {
            return "A (85+)";
        }
        if (grade >= 70) {
            return "B (70-84)";
        }
        if (grade >= 55) {
            return "C (55-69)";
        }
        if (grade >= 40) {
            return "D (40-54)";
        }
        return "F (<40)";
    }

    public record GradeAnalytics(double average, double max, double min,
                                 long passCount, long failCount,
                                 Map<String, Long> buckets) {
    }

    public record AttendanceSnapshot(LocalDate date, double percentage) {
    }

    public record AttendanceAnalytics(double averagePercent,
                                      long sessions,
                                      List<AttendanceSnapshot> snapshots) {
    }
}
