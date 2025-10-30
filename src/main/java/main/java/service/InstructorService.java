package main.java.service;

import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Instructor context helpers.
 */
public final class InstructorService {

    private InstructorService() {
    }

    public static List<Section> getAssignedSections(User instructor) {
        requireInstructor(instructor);
        Faculty faculty = DatabaseUtil.findFacultyByUsername(instructor.getUsername());
        return DatabaseUtil.getAllSections().stream()
                .filter(section -> Objects.equals(section.getFacultyId(), faculty.getFacultyId()))
                .collect(Collectors.toList());
    }

    private static void requireInstructor(User instructor) {
        if (instructor == null || !"Instructor".equalsIgnoreCase(instructor.getRole())) {
            throw new SecurityException("Instructor role required.");
        }
    }
}
