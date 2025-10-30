package main.java.service;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.time.LocalDate;

/**
 * Coordinates registration and drop flows with access-rule enforcement.
 */
public final class EnrollmentService {

    private EnrollmentService() {
    }

    public static EnrollmentRecord registerSection(User actor, String studentId, String sectionId) {
        ensureCanMutate(actor);

        Section section = requireSection(sectionId);
        Student student = requireStudent(studentId);

        if (LocalDate.now().isAfter(section.getEnrollmentDeadline())) {
            throw new IllegalStateException("Enrollment deadline has passed for this section.");
        }

        if (isStudent(actor)) {
            enforceStudentOwnsRecord(actor, student);
        }

        return DatabaseUtil.registerStudentToSection(studentId, sectionId);
    }

    public static void dropSection(User actor, String studentId, String sectionId) {
        ensureCanMutate(actor);

        Section section = requireSection(sectionId);
        Student student = requireStudent(studentId);

        if (LocalDate.now().isAfter(section.getDropDeadline())) {
            throw new IllegalStateException("Drop deadline has passed for this section.");
        }

        if (isStudent(actor)) {
            enforceStudentOwnsRecord(actor, student);
        }

        DatabaseUtil.dropStudentFromSection(studentId, sectionId);
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
