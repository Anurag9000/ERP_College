package main.java.service;

import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.util.List;

/**
 * Student-facing read helpers with access checks.
 */
public final class StudentService {

    private StudentService() {
    }

    public static Student getProfile(User actor) {
        requireStudent(actor);
        Student student = DatabaseUtil.findStudentByUsername(actor.getUsername());
        if (student == null) {
            throw new IllegalStateException("No student profile linked to username " + actor.getUsername());
        }
        return student;
    }

    public static List<Section> getSchedule(User actor) {
        Student profile = getProfile(actor);
        return DatabaseUtil.getScheduleForStudent(profile.getStudentId());
    }

    private static void requireStudent(User actor) {
        if (actor == null || !"Student".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Student role required.");
        }
    }
}
