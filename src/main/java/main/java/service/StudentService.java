package main.java.service;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.util.List;

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
            return "O";
        if (score >= 80)
            return "A+";
        if (score >= 70)
            return "A";
        if (score >= 60)
            return "B+";
        if (score >= 55)
            return "B";
        if (score >= 50)
            return "C+";
        if (score >= 45)
            return "C";
        if (score >= 40)
            return "D";
        if (score >= 33)
            return "P";
        return "F";
    }

    public static double calculatePoints(double score, String sectionId) {
        return GradebookService.calculateRelativePoints(score, sectionId);
    }

    public static int getTotalCredits(String studentId) {
        return DatabaseUtil.getEnrollmentsForStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .mapToInt(rec -> {
                    Section s = DatabaseUtil.getSection(rec.getSectionId());
                    return s != null ? DatabaseUtil.getCourseCreditHours(s.getCourseId()) : 0;
                }).sum();
    }

    public static double calculateSGPA(String username, String semester) {
        return calculateGPA(username, semester);
    }

    public static double calculateCGPA(String username) {
        return calculateGPA(username, null);
    }

    private static double calculateGPA(String username, String semesterFilter) {
        Student student = DatabaseUtil.findStudentByUsername(username);
        if (student == null)
            return 0.0;

        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(student.getStudentId());
        double totalPoints = 0.0;
        int totalCredits = 0;

        for (EnrollmentRecord rec : enrollments) {
            // Only include ENROLLED courses that have been graded (finalGrade >= 0)
            if (rec.getStatus() != EnrollmentRecord.Status.ENROLLED || rec.getFinalGrade() < 0)
                continue;

            Section section = DatabaseUtil.getSection(rec.getSectionId());
            if (section == null)
                continue;

            // Filter by semester if provided
            if (semesterFilter != null && !semesterFilter.equalsIgnoreCase(section.getSemester())) {
                continue;
            }

            int credits = DatabaseUtil.getCourseCreditHours(section.getCourseId());
            double points = calculatePoints(rec.getFinalGrade(), rec.getSectionId());
            totalPoints += points * credits;
            totalCredits += credits;
        }

        return totalCredits > 0 ? totalPoints / totalCredits : 0.0;
    }
}
