package main.java.service;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * Coordinates registration and drop flows with access-rule enforcement.
 */
public final class EnrollmentService {

    private EnrollmentService() {
    }

    /**
     * Registers a student to a section with prerequisite and deadline validation.
     * 
     * @param actor     the user performing the action (must not be null)
     * @param studentId the student ID (must not be null or empty)
     * @param sectionId the section ID (must not be null or empty)
     * @return the enrollment record
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException    if enrollment rules are violated
     * @throws SecurityException        if student tries to register another student
     */
    public static EnrollmentRecord registerSection(User actor, String studentId, String sectionId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        ensureCanMutate(actor);

        Section section = requireSection(sectionId);
        Student student = requireStudent(studentId);

        if (section.getEnrollmentDeadline() != null && LocalDate.now().isAfter(section.getEnrollmentDeadline())) {
            throw new IllegalStateException("Enrollment deadline has passed for this section.");
        }

        List<String> missingPrereqs = DatabaseUtil.getMissingPrerequisites(studentId, section.getCourseId());
        if (!missingPrereqs.isEmpty()) {
            throw new IllegalStateException("Missing prerequisite(s): " + String.join(", ", missingPrereqs));
        }

        if (DatabaseUtil.isStudentEnrolledInCourse(studentId, section.getCourseId())) {
            throw new IllegalStateException("Student is already enrolled in another section of this course.");
        }

        if (isStudent(actor)) {
            enforceStudentOwnsRecord(actor, student);
        }

        return DatabaseUtil.registerStudentToSection(actor, studentId, sectionId);
    }

    /**
     * Drops a student from a section with deadline validation.
     * 
     * @param actor     the user performing the action (must not be null)
     * @param studentId the student ID (must not be null or empty)
     * @param sectionId the section ID (must not be null or empty)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException    if drop deadline has passed
     * @throws SecurityException        if student tries to drop another student
     */
    public static void dropSection(User actor, String studentId, String sectionId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        if (sectionId == null || sectionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Section ID cannot be null or empty");
        }
        ensureCanMutate(actor);

        Section section = requireSection(sectionId);
        Student student = requireStudent(studentId);

        if (section.getDropDeadline() != null && LocalDate.now().isAfter(section.getDropDeadline())) {
            throw new IllegalStateException("Drop deadline has passed for this section.");
        }

        if (isStudent(actor)) {
            enforceStudentOwnsRecord(actor, student);
        }

        DatabaseUtil.dropStudentFromSection(actor.getUsername(), studentId, sectionId);
    }

    private static void ensureCanMutate(User actor) {
        if (actor == null) {
            throw new IllegalArgumentException("No user session present.");
        }
        if (DatabaseUtil.isMaintenanceMode() && !"Admin".equalsIgnoreCase(actor.getRole())) {
            throw new IllegalStateException("System is in maintenance mode.");
        }
    }

    private static boolean isStudent(User actor) {
        return "Student".equalsIgnoreCase(actor.getRole());
    }

    private static void enforceStudentOwnsRecord(User actor, Student student) {
        if (student == null || !actor.getUsername().equalsIgnoreCase(student.getUsername())) {
            throw new SecurityException("Students may only manage their own sections.");
        }
    }

    private static Section requireSection(String sectionId) {
        Section section = DatabaseUtil.getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        return section;
    }

    private static Student requireStudent(String studentId) {
        Student student = DatabaseUtil.getStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        return student;
    }
}
