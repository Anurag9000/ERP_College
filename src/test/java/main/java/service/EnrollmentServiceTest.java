package main.java.service;

import main.java.data.dao.*;
import main.java.data.dao.AuthUserDao;
import main.java.models.*;
import main.java.utils.DatabaseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EnrollmentServiceTest {

    private SectionDao mockSectionDao;
    private StudentDao mockStudentDao;
    private EnrollmentDao mockEnrollmentDao;
    private WaitlistDao mockWaitlistDao;
    private CourseDao mockCourseDao;
    private NotificationDao mockNotificationDao;
    private AuthUserDao mockAuthUserDao;
    private AuditLogDao mockAuditLogDao;
    private CourseRelationshipDao mockCourseRelationshipDao;
    private CoursePrerequisiteDao mockCoursePrerequisiteDao;
    private MaintenanceWindowDao mockMaintenanceWindowDao;

    @BeforeEach
    void setUp() {
        mockSectionDao = mock(SectionDao.class);
        mockStudentDao = mock(StudentDao.class);
        mockEnrollmentDao = mock(EnrollmentDao.class);
        mockWaitlistDao = mock(WaitlistDao.class);
        mockCourseDao = mock(CourseDao.class);
        mockNotificationDao = mock(NotificationDao.class);
        mockAuthUserDao = mock(AuthUserDao.class);
        mockAuditLogDao = mock(AuditLogDao.class);
        mockCourseRelationshipDao = mock(CourseRelationshipDao.class);
        mockCoursePrerequisiteDao = mock(CoursePrerequisiteDao.class);

        DatabaseUtil.setSectionDao(mockSectionDao);
        DatabaseUtil.setStudentDao(mockStudentDao);
        DatabaseUtil.setEnrollmentDao(mockEnrollmentDao);
        DatabaseUtil.setWaitlistDao(mockWaitlistDao);
        DatabaseUtil.setCourseDao(mockCourseDao);
        DatabaseUtil.setNotificationDao(mockNotificationDao);
        DatabaseUtil.setAuthUserDao(mockAuthUserDao);
        DatabaseUtil.setAuditLogDao(mockAuditLogDao);
        DatabaseUtil.setCourseRelationshipDao(mockCourseRelationshipDao);
        DatabaseUtil.setCoursePrerequisiteDao(mockCoursePrerequisiteDao);
        mockMaintenanceWindowDao = mock(MaintenanceWindowDao.class);
        DatabaseUtil.setMaintenanceWindowDao(mockMaintenanceWindowDao);
        when(mockMaintenanceWindowDao.findAll()).thenReturn(Collections.emptyList());

        // Stub getConnection calls for transactional methods
        try {
            Connection mockConn = mock(Connection.class);
            when(mockEnrollmentDao.getConnection()).thenReturn(mockConn);
            when(mockCourseDao.getConnection()).thenReturn(mockConn);
            when(mockWaitlistDao.getConnection()).thenReturn(mockConn);
        } catch (SQLException e) {
            // Ignore in test setup
        }

        // Ensure not in maintenance mode by default
        DatabaseUtil.setSettingsDao(mock(SettingsDao.class));
    }

    @Test
    void testRegisterSection_Success() throws SQLException {
        User studentUser = new User("stu1", "hash", "salt", "Student", "Alice", "alice@test.com");
        Student studentProfile = new Student("stu1", "Alice", "Last", "email", "phone", LocalDate.now(), "addr", "CSE",
                1);

        Section section = new Section("SEC1", "CSE101", "Intro", "FAC1", DayOfWeek.MONDAY, LocalTime.NOON,
                LocalTime.NOON.plusHours(1), "Room 1", 30);
        section.setEnrollmentDeadline(LocalDate.now().plusDays(5));

        Course course = new Course("CSE101", "Intro CS", "CSE", 4, 1000, "Desc", 60);

        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));
        when(mockStudentDao.findByCode("stu1")).thenReturn(Optional.of(studentProfile));
        when(mockCourseDao.findByCode("CSE101")).thenReturn(Optional.of(course));
        when(mockEnrollmentDao.findBySection("SEC1")).thenReturn(new ArrayList<>()); // Empty enrollment
        when(mockEnrollmentDao.findByStudent("stu1")).thenReturn(new ArrayList<>()); // Empty enrollment for credit
                                                                                     // check

        when(mockCourseRelationshipDao.findCorequisites("CSE101")).thenReturn(Collections.emptyList());
        when(mockCourseRelationshipDao.findAntirequisites("CSE101")).thenReturn(Collections.emptyList());
        when(mockCoursePrerequisiteDao.findPrerequisites("CSE101")).thenReturn(Collections.emptyList());
        try {
            when(mockCourseDao.decrementAvailableSeats(any(Connection.class), eq("CSE101"))).thenReturn(true);
        } catch (SQLException e) {
            // Ignore for test
        }

        EnrollmentRecord result = EnrollmentService.registerSection(studentUser, "stu1", "SEC1");

        assertNotNull(result);
        assertEquals(EnrollmentRecord.Status.ENROLLED, result.getStatus());
        verify(mockEnrollmentDao).insert(any(Connection.class), any(EnrollmentRecord.class));
    }

    @Test
    void testRegisterSection_DeadlinePassed() {
        User studentUser = new User("stu1", "hash", "salt", "Student", "Alice", "alice@test.com");
        Student studentProfile = new Student("stu1", "Alice", "Last", "email", "phone", LocalDate.now(), "addr", "CSE",
                1);

        Section section = new Section("SEC1", "CSE101", "Intro", "FAC1", DayOfWeek.MONDAY, LocalTime.NOON,
                LocalTime.NOON.plusHours(1), "Room 1", 30);
        section.setEnrollmentDeadline(LocalDate.now().minusDays(1)); // PAST DEADLINE

        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));
        when(mockStudentDao.findByCode("stu1")).thenReturn(Optional.of(studentProfile));

        assertThrows(IllegalStateException.class, () -> {
            EnrollmentService.registerSection(studentUser, "stu1", "SEC1");
        });

        verify(mockEnrollmentDao, never()).insert(any(EnrollmentRecord.class));
    }

    @Test
    void testDropSection_Success() throws SQLException {
        User studentUser = new User("stu1", "hash", "salt", "Student", "Alice", "alice@test.com");
        Student studentProfile = new Student("stu1", "Alice", "Last", "email", "phone", LocalDate.now(), "addr", "CSE",
                1);

        Section section = new Section("SEC1", "CSE101", "Intro", "FAC1", DayOfWeek.MONDAY, LocalTime.NOON,
                LocalTime.NOON.plusHours(1), "Room 1", 30);
        section.setDropDeadline(LocalDate.now().plusDays(5));

        EnrollmentRecord record = new EnrollmentRecord("stu1", "SEC1", EnrollmentRecord.Status.ENROLLED);
        ArrayList<EnrollmentRecord> records = new ArrayList<>();
        records.add(record);

        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));
        when(mockStudentDao.findByCode("stu1")).thenReturn(Optional.of(studentProfile));
        when(mockEnrollmentDao.findBySection("SEC1")).thenReturn(records);
        when(mockCourseDao.findByCode("CSE101")).thenReturn(Optional.of(new Course("CSE101", "", "", 3, 0, "", 10)));

        EnrollmentService.dropSection(studentUser, "stu1", "SEC1");

        assertEquals(EnrollmentRecord.Status.DROPPED, record.getStatus());
        verify(mockEnrollmentDao).update(any(Connection.class), eq(record));
    }
}
