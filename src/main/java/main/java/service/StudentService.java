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

    /**
     * Calculates grade points for a score in a section.
     * 
     * @param score     the score
     * @param sectionId the section ID (must not be null or empty)
     * @return the calculated points
     * @throws IllegalArgumentException if sectionId is null or empty
     */
    public static double calculatePoints(double score, String sectionId) {
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        return GradebookService.calculateRelativePoints(score, sectionId);
    }

    /**
     * Gets total credits for a student.
     * 
     * @param studentId the student ID (must not be null or empty)
     * @return total credits
     * @throws IllegalArgumentException if studentId is null or empty
     */
    public static int getTotalCredits(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        return DatabaseUtil.getEnrollmentsForStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .mapToInt(rec -> {
                    Section s = DatabaseUtil.getSection(rec.getSectionId());
                    return s != null ? DatabaseUtil.getCourseCreditHours(s.getCourseId()) : 0;
                }).sum();
    }

    /**
     * Calculates semester GPA.
     * 
     * @param username the username (must not be null or empty)
     * @param semester the semester (must not be null or empty)
     * @return the SGPA
     * @throws IllegalArgumentException if parameters are null or empty
     */
    public static double calculateSGPA(String username, String semester) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (semester == null || semester.trim().isEmpty()) {
            throw new IllegalArgumentException("Semester cannot be null or empty");
        }
        return calculateGPA(username, semester);
    }

    /**
     * Calculates cumulative GPA.
     * 
     * @param username the username (must not be null or empty)
     * @return the CGPA
     * @throws IllegalArgumentException if username is null or empty
     */
    public static double calculateCGPA(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
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
