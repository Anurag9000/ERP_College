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

    public static double calculatePoints(double score) {
        if (score >= 90)
            return 10.0;
        if (score >= 85)
            return 9.0;
        if (score >= 80)
            return 8.0;
        if (score >= 75)
            return 7.0;
        if (score >= 70)
            return 6.0;
        if (score >= 65)
            return 5.0;
        if (score >= 60)
            return 4.0;
        return 0.0;
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

            // Note: In this system, we might need to filter by term if 'term' is provided.
            // For now, let's assume getEnrollmentsForStudent returns current enrollments.

            int credits = DatabaseUtil.getCourseCreditHours(section.getCourseId());
            double score = record.getFinalGrade();
            if (score <= 0 && !record.getComponentScores().isEmpty()) {
                score = section.computeFinalScore(record.getComponentScores());
            }

            if (score > 0) {
                totalPoints += calculatePoints(score) * credits;
                totalCredits += credits;
            }
        }

        return totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
    }

    public static double calculateCGPA(String studentId) {
        // In a real system, this would fetch historical grades too.
        // For simplicity, we'll use current enrollments as a proxy or assume historical
        // data is same format.
        return calculateSGPA(studentId, null);
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
