package main.java.service;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for student-specific operations and academic calculations.
 */
public final class StudentService {

    private StudentService() {
    }

    public static Student getProfile(User user) {
        if (user == null)
            return null;
        if (!"Student".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Only students can access their profiles via this service.");
        }
        return DatabaseUtil.findStudentByUsername(user.getUsername());
    }

    public static List<Section> getSchedule(User user) {
        if (user == null)
            return java.util.Collections.emptyList();
        if (!"Student".equalsIgnoreCase(user.getRole())) {
            return java.util.Collections.emptyList();
        }
        Student profile = getProfile(user);
        if (profile == null)
            return java.util.Collections.emptyList();
        return DatabaseUtil.getScheduleForStudent(profile.getStudentId());
    }

    public static String calculateLetterGrade(double score) {
        if (score >= 90)
            return "A+";
        if (score >= 85)
            return "A";
        if (score >= 80)
            return "B+";
        if (score >= 75)
            return "B";
        if (score >= 70)
            return "C+";
        if (score >= 65)
            return "C";
        if (score >= 60)
            return "D";
        return "F";
    }

    public static double calculateSGPA(String studentId, String term) {
        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .collect(Collectors.toList());

        double totalPoints = 0;
        int totalCredits = 0;

        for (EnrollmentRecord record : enrollments) {
            Section section = DatabaseUtil.getSection(record.getSectionId());
            if (section == null)
                continue;

            // Filter by term if provided
            if (term != null && !term.isBlank() && !section.getSemester().equalsIgnoreCase(term)) {
                continue;
            }

            int credits = DatabaseUtil.getCourseCreditHours(section.getCourseId());
            double score = record.getFinalGrade();
            if (score <= 0 && !record.getComponentScores().isEmpty()) {
                score = section.computeFinalScore(record.getComponentScores());
            }

            double points = DatabaseUtil.calculateRelativePoints(score, section.getSectionId());
            totalPoints += points * credits;
            totalCredits += credits;
        }

        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public static double calculateCGPA(String studentId) {
        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .collect(Collectors.toList());

        double totalPoints = 0;
        int totalCredits = 0;

        for (EnrollmentRecord record : enrollments) {
            Section section = DatabaseUtil.getSection(record.getSectionId());
            if (section == null)
                continue;

            int credits = DatabaseUtil.getCourseCreditHours(section.getCourseId());
            double score = record.getFinalGrade();
            if (score <= 0 && !record.getComponentScores().isEmpty()) {
                score = section.computeFinalScore(record.getComponentScores());
            }

            double points = DatabaseUtil.calculateRelativePoints(score, section.getSectionId());

            // CGPA excludes failures (Fail = 2.0 per user request)
            if (points > 2.0) {
                totalPoints += points * credits;
                totalCredits += credits;
            }
        }

        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public static double calculatePoints(double score) {
        return DatabaseUtil.calculateRelativePoints(score, null);
    }

    public static int getTotalCredits(String studentId) {
        return DatabaseUtil.getEnrollmentsForStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .mapToInt(rec -> {
                    Section s = DatabaseUtil.getSection(rec.getSectionId());
                    return s != null ? DatabaseUtil.getCourseCreditHours(s.getCourseId()) : 0;
                }).sum();
    }
}
