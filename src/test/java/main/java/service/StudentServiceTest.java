package main.java.service;

import main.java.data.dao.EnrollmentDao;
import main.java.data.dao.SectionDao;
import main.java.data.dao.StudentDao;
import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StudentServiceTest {

    private StudentDao mockStudentDao;
    private EnrollmentDao mockEnrollmentDao;
    private SectionDao mockSectionDao;

    @BeforeEach
    void setUp() {
        mockStudentDao = mock(StudentDao.class);
        mockEnrollmentDao = mock(EnrollmentDao.class);
        mockSectionDao = mock(SectionDao.class);

        DatabaseUtil.setStudentDao(mockStudentDao);
        DatabaseUtil.setEnrollmentDao(mockEnrollmentDao);
        DatabaseUtil.setSectionDao(mockSectionDao);
    }

    @Test
    void testGetProfile_Success() {
        User user = new User("stu1", "hash", "salt", "Student", "Alice", "alice@test.com");
        Student student = new Student("stu1", "Alice", "Johnson", "alice@test.com", "123", LocalDate.now(), "Addr",
                "CS", 1);
        student.setUsername("stu1");

        when(mockStudentDao.findByUsername("stu1")).thenReturn(Optional.of(student));

        Student result = StudentService.getProfile(user);
        assertNotNull(result);
        assertEquals("stu1", result.getStudentId());
    }

    @Test
    void testGetProfile_SecurityException() {
        User user = new User("admin", "hash", "salt", "Admin", "Admin", "admin@test.com");

        assertThrows(SecurityException.class, () -> {
            StudentService.getProfile(user);
        });
    }

    @Test
    void testGetSchedule_Success() {
        User user = new User("stu1", "hash", "salt", "Student", "Alice", "alice@test.com");
        Student student = new Student("stu1", "Alice", "Johnson", "alice@test.com", "123", LocalDate.now(), "Addr",
                "CS", 1);
        student.setUsername("stu1");

        Section section = new Section("SEC1", "CS101", "Intro to CS", "fac1", null, null, null, "Room 101", 30);
        EnrollmentRecord record = new EnrollmentRecord("stu1", "SEC1", EnrollmentRecord.Status.ENROLLED);

        when(mockStudentDao.findByUsername("stu1")).thenReturn(Optional.of(student));
        when(mockEnrollmentDao.findByStudent("stu1")).thenReturn(List.of(record));
        // Assuming DatabaseUtil.getSection falls back to DAO.
        // If DatabaseUtil uses a cache, we rely on it being empty or checking DAO.
        // Let's assume DAO fallback for now.
        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));

        List<Section> schedule = StudentService.getSchedule(user);
        assertNotNull(schedule);
        assertEquals(1, schedule.size());
        assertEquals("SEC1", schedule.get(0).getSectionId());
    }
}
