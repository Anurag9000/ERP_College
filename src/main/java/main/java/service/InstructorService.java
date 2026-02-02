package main.java.service;

import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.util.List;

/**
 * Instructor context helpers.
 */
public final class InstructorService {

    private InstructorService() {
    }

    /**
     * Gets sections assigned to an instructor.
     * 
     * @param instructor the instructor user (must not be null with Instructor role)
     * @return list of assigned sections, never null
     * @throws SecurityException if instructor is null or not an instructor
     */
    public static List<Section> getAssignedSections(User instructor) {
        requireInstructor(instructor);
        Faculty faculty = DatabaseUtil.findFacultyByUsername(instructor.getUsername());
        if (faculty == null) {
            return java.util.Collections.emptyList();
        }
        return DatabaseUtil.getSectionsForFaculty(faculty.getFacultyId());
    }

    private static void requireInstructor(User instructor) {
        if (instructor == null || !"Instructor".equalsIgnoreCase(instructor.getRole())) {
            throw new SecurityException("Instructor role required.");
        }
    }
}
