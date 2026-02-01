package main.java.service;

import main.java.data.dao.*;
import main.java.models.*;
import main.java.utils.DatabaseUtil;
import main.java.utils.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GradebookServiceTest {

    private SectionDao mockSectionDao;
    private InstructorDao mockInstructorDao;
    private EnrollmentDao mockEnrollmentDao;
    private AuditLogDao mockAuditLogDao;
    private SettingsDao mockSettingsDao;
    private MaintenanceWindowDao mockMaintenanceWindowDao;

    @BeforeEach
    void setUp() {
        mockSectionDao = mock(SectionDao.class);
        mockInstructorDao = mock(InstructorDao.class);
        mockEnrollmentDao = mock(EnrollmentDao.class);
        mockAuditLogDao = mock(AuditLogDao.class);
        mockSettingsDao = mock(SettingsDao.class);

        DatabaseUtil.setSectionDao(mockSectionDao);
        DatabaseUtil.setInstructorDao(mockInstructorDao);
        DatabaseUtil.setEnrollmentDao(mockEnrollmentDao);
        DatabaseUtil.setAuditLogDao(mockAuditLogDao);
        mockMaintenanceWindowDao = mock(MaintenanceWindowDao.class);
        DatabaseUtil.setSettingsDao(mockSettingsDao);
        DatabaseUtil.setMaintenanceWindowDao(mockMaintenanceWindowDao);
        when(mockMaintenanceWindowDao.findAll()).thenReturn(Collections.emptyList());
    }

    @Test
    void testRecordScore_Success() {
        User instructorUser = new User("prof1", "hash", "salt", "Instructor", "Prof", "prof@test.com");
        // Correct Faculty constructor: ID, First, Last, Email, Phone, Dept,
        // Designation, Qual, Salary
        Faculty faculty = new Faculty("prof1", "Prof", "Name", "email", "phone", "Dept", "Prof", "Ph.D", 50000);
        Section section = new Section("SEC1", "CSE101", "Intro", "prof1", null, null, null, "Room 1", 30);

        EnrollmentRecord record = new EnrollmentRecord("stu1", "SEC1", EnrollmentRecord.Status.ENROLLED);
        List<EnrollmentRecord> records = new ArrayList<>();
        records.add(record);

        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));
        when(mockInstructorDao.findByUsername("prof1")).thenReturn(Optional.of(faculty));
        when(mockEnrollmentDao.findBySection("SEC1")).thenReturn(records);
        when(mockEnrollmentDao.findBySectionAndStudent("SEC1", "stu1")).thenReturn(record);

        GradebookService.recordScore(instructorUser, "SEC1", "stu1", "Midterm", 85.0);

        assertEquals(85.0, record.getComponentScores().get("Midterm"));

        verify(mockAuditLogDao, atLeastOnce()).insert(any(AuditLogService.AuditEvent.class));
    }

    @Test
    void testRecordScore_SecurityException_WrongInstructor() {
        User otherInstructor = new User("prof2", "hash", "salt", "Instructor", "Prof2", "prof2@test.com");
        Faculty faculty2 = new Faculty("prof2", "Prof2", "Name", "email", "phone", "Dept", "Prof", "M.Sc", 40000);
        Section section = new Section("SEC1", "CSE101", "Intro", "prof1", null, null, null, "Room 1", 30); // Owned by
                                                                                                           // prof1

        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));
        when(mockInstructorDao.findByUsername("prof2")).thenReturn(Optional.of(faculty2));

        assertThrows(SecurityException.class, () -> {
            GradebookService.recordScore(otherInstructor, "SEC1", "stu1", "Midterm", 90.0);
        });
    }

    @Test
    void testComputeFinal_Success() {
        User instructorUser = new User("prof1", "hash", "salt", "Instructor", "Prof", "prof@test.com");
        Faculty faculty = new Faculty("prof1", "Prof", "Name", "email", "phone", "Dept", "Prof", "Ph.D", 10000);
        Section section = new Section("SEC1", "CSE101", "Intro", "prof1", null, null, null, "Room 1", 30);
        section.setAssessmentWeight("Midterm", 50.0);
        section.setAssessmentWeight("Final", 50.0);

        EnrollmentRecord record = new EnrollmentRecord("stu1", "SEC1", EnrollmentRecord.Status.ENROLLED);
        record.putScore("Midterm", 80.0);
        record.putScore("Final", 100.0);
        List<EnrollmentRecord> records = new ArrayList<>();
        records.add(record);

        when(mockSectionDao.findByCode("SEC1")).thenReturn(Optional.of(section));
        when(mockInstructorDao.findByUsername("prof1")).thenReturn(Optional.of(faculty));
        when(mockEnrollmentDao.findBySection("SEC1")).thenReturn(records);
        when(mockEnrollmentDao.findBySectionAndStudent("SEC1", "stu1")).thenReturn(record);

        double finalGrade = GradebookService.computeFinal(instructorUser, "SEC1", "stu1");

        // 80*0.5 + 100*0.5 = 40 + 50 = 90
        assertEquals(90.0, finalGrade, 0.01);
        assertEquals(90.0, record.getFinalGrade(), 0.01);
    }
}
