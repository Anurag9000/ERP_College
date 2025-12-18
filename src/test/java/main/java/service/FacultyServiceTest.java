package main.java.service;

import main.java.data.dao.AppointmentDao;
import main.java.models.Faculty;
import main.java.models.OfficeHour;
import main.java.models.Section;
import main.java.utils.DatabaseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FacultyServiceTest {

    private AppointmentDao mockAppointmentDao;

    @BeforeEach
    void setUp() {
        mockAppointmentDao = mock(AppointmentDao.class);
        FacultyService.setAppointmentDao(mockAppointmentDao);

        // DatabaseUtil static mocks are trickier without correct setup.
        // FacultyService calls DatabaseUtil.getAllFaculty() ->
        // DatabaseUtil.faculty.values().
        // DatabaseUtil.getAllSections() -> DatabaseUtil.sections.values().
        // Since we can't easily inject maps into DatabaseUtil without reflection or
        // setters,
        // we might skip testing methods that rely heavily on DatabaseUtil statics if we
        // can't mock them.
        // However, we can try to test logic that doesn't explode.
    }

    @Test
    void testGetOfficeHours() {
        String facultyId = "FAC1";
        List<OfficeHour> hours = new ArrayList<>();
        OfficeHour oh = new OfficeHour();
        oh.setFacultyId("FAC1");
        oh.setDayOfWeek(java.time.DayOfWeek.MONDAY);
        oh.setStartTime(LocalTime.of(10, 0));
        oh.setEndTime(LocalTime.of(11, 0));
        oh.setLocation("Room 101");
        hours.add(oh);

        when(mockAppointmentDao.getOfficeHours(facultyId)).thenReturn(hours);

        List<OfficeHour> result = FacultyService.getOfficeHours(facultyId);
        assertEquals(1, result.size());
        assertEquals("Room 101", result.get(0).getLocation());
    }
}
