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

    public static Map<String, Double> parseWeights(String input) {
        Map<String, Double> weights = new LinkedHashMap<>();
        if (input == null || input.isBlank()) {
            return weights;
        }

        // Try comma/colon format first (UI format: "Quiz:20,Midterm:30,Final:50")
        if (input.contains(":") || input.contains(",")) {
            String[] tokens = input.split(",");
            for (String token : tokens) {
                String[] parts = token.split(":");
                if (parts.length == 2) {
                    try {
                        String name = parts[0].trim();
                        if (!name.isEmpty()) {
                            weights.put(name, Double.parseDouble(parts[1].trim()));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // If weights is still empty or we want to support both, try newline/equals
        // format (DB template format: "Quiz=20\nMidterm=30")
        if (weights.isEmpty() || input.contains("=") || input.contains("\n")) {
            String[] lines = input.split("\\n");
            for (String line : lines) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String component = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!component.isEmpty()) {
                        try {
                            weights.put(component, Double.parseDouble(value));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        return weights;
    }

    public static String formatWeights(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        weights.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(entry.getKey().replace("\n", " ").replace("=", " "))
                            .append('=')
                            .append(entry.getValue());
                });
        return builder.toString();
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

    public static void recordScore(User instructor, String sectionId, String studentId, String component,
            double score) {
        ensureInstructorAccess(instructor, sectionId);
        EnrollmentRecord record = locateEnrollment(sectionId, studentId);
        record.putScore(component, score);
        DatabaseUtil.updateEnrollment(record);
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
        DatabaseUtil.updateEnrollment(record);
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
        buckets.put("O (1.5σ+)", 0L);
        buckets.put("A+ (1.0σ-1.5σ)", 0L);
        buckets.put("A (0.5σ-1.0σ)", 0L);
        buckets.put("B+ (0σ-0.5σ)", 0L);
        buckets.put("B (-0.5σ-0σ)", 0L);
        buckets.put("C (-1.0σ--0.5σ)", 0L);
        buckets.put("P (-1.5σ--1.0σ)", 0L);
        buckets.put("F (<-1.5σ)", 0L);

        long passCount = 0;
        long failCount = 0;
        double min = Double.MAX_VALUE;
        double max = -1.0;
        double total = 0.0;
        int counted = 0;

        for (EnrollmentRecord record : records) {
            double grade = record.getFinalGrade();
            if (grade <= 0 && !record.getComponentScores().isEmpty()) {
                grade = section.computeFinalScore(record.getComponentScores());
            }
            counted++;
            total += grade;
            min = Math.min(min, grade);
            max = Math.max(max, grade);

            double points = DatabaseUtil.calculateRelativePoints(grade, sectionId);
            if (points >= 4.0) {
                passCount++;
            } else {
                failCount++;
            }
            buckets.compute(bucketFor(grade, sectionId), (k, v) -> v == null ? 1 : v + 1);
        }

        if (counted == 0) {
            return new GradeAnalytics(0.0, 0.0, 0.0, 0, 0, Map.copyOf(buckets));
        }
        double average = total / counted;
        return new GradeAnalytics(average, max, min, passCount, failCount, Map.copyOf(buckets));
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

    public static void saveFeedback(User instructor, String sectionId, String studentId, String component,
            String comment) {
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
        EnrollmentRecord record = DatabaseUtil.getEnrollment(sectionId, studentId);
        if (record == null) {
            throw new IllegalArgumentException("Student not enrolled in section.");
        }
        return record;
    }

    private static String bucketFor(double grade, String sectionId) {
        double points = DatabaseUtil.calculateRelativePoints(grade, sectionId);
        if (points >= 10.0)
            return "O (1.5σ+)";
        if (points >= 9.0)
            return "A+ (1.0σ-1.5σ)";
        if (points >= 8.0)
            return "A (0.5σ-1.0σ)";
        if (points >= 7.0)
            return "B+ (0σ-0.5σ)";
        if (points >= 6.0)
            return "B (-0.5σ-0σ)";
        if (points >= 5.0)
            return "C (-1.0σ--0.5σ)";
        if (points >= 4.0)
            return "P (-1.5σ--1.0σ)";
        return "F (<-1.5σ)";
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
