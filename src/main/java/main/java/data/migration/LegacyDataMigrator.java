package main.java.data.migration;

import main.java.data.AuthUserDao;
import main.java.data.dao.AttendanceDao;
import main.java.data.dao.CourseDao;
import main.java.data.dao.EnrollmentDao;
import main.java.data.dao.InstructorDao;
import main.java.data.dao.SectionDao;
import main.java.data.dao.StudentDao;
import main.java.data.dao.WaitlistDao;
import main.java.models.AttendanceRecord;
import main.java.models.Course;
import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imports legacy {@code .dat} snapshot files into the SQL-backed schema.
 */
public final class LegacyDataMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyDataMigrator.class);

    private final Path dataDirectory;
    private final AuthUserDao authUserDao;
    private final StudentDao studentDao;
    private final InstructorDao instructorDao;
    private final CourseDao courseDao;
    private final SectionDao sectionDao;
    private final EnrollmentDao enrollmentDao;
    private final WaitlistDao waitlistDao;
    private final AttendanceDao attendanceDao;

    private LegacyDataMigrator(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.authUserDao = new AuthUserDao();
        this.studentDao = new StudentDao();
        this.instructorDao = new InstructorDao();
        this.courseDao = new CourseDao();
        this.sectionDao = new SectionDao();
        this.enrollmentDao = new EnrollmentDao();
        this.waitlistDao = new WaitlistDao();
        this.attendanceDao = new AttendanceDao();
    }

    public static LegacyDataMigrator defaultMigrator() {
        return new LegacyDataMigrator(Paths.get("data"));
    }

    /**
     * Migrates every supported legacy file into the database.
     *
     * @return true if any records were imported or updated
     */
    public boolean migrateAll() {
        if (!Files.isDirectory(dataDirectory)) {
            LOGGER.debug("Legacy data directory {} not found; skipping migration.", dataDirectory.toAbsolutePath());
            return false;
        }
        boolean migrated = false;
        migrated |= migrateUsers();
        migrated |= migrateCourses();
        migrated |= migrateFaculty();
        migrated |= migrateStudents();
        migrated |= migrateSections();
        migrated |= migrateEnrollments();
        migrated |= migrateAttendance();
        if (migrated) {
            LOGGER.info("Legacy data migration completed from {}.", dataDirectory.toAbsolutePath());
        } else {
            LOGGER.debug("No legacy records imported from {}.", dataDirectory.toAbsolutePath());
        }
        return migrated;
    }

    private boolean migrateUsers() {
        List<User> users = readEntities("users.dat", User.class);
        boolean changed = false;
        for (User user : users) {
            if (user == null || user.getUsername() == null) {
                continue;
            }
            if (authUserDao.findByUsername(user.getUsername()).isPresent()) {
                continue;
            }
            if (user.getSalt() == null || user.getPasswordHash() == null) {
                LOGGER.warn("Skipping legacy user {} due to missing credentials.", user.getUsername());
                continue;
            }
            if (user.getPasswordHistory() == null) {
                user.setPasswordHistory(new ArrayDeque<>());
            }
            authUserDao.insert(user);
            changed = true;
        }
        return changed;
    }

    private boolean migrateCourses() {
        List<Course> courses = readEntities("courses.dat", Course.class);
        boolean changed = false;
        for (Course course : courses) {
            if (course == null || course.getCourseId() == null) {
                continue;
            }
            Optional<Course> existing = courseDao.findByCode(course.getCourseId());
            if (existing.isPresent()) {
                courseDao.update(course);
            } else {
                courseDao.insert(course);
            }
            changed = true;
        }
        return changed;
    }

    private boolean migrateFaculty() {
        List<Faculty> faculty = readEntities("faculty.dat", Faculty.class);
        boolean changed = false;
        for (Faculty member : faculty) {
            if (member == null || member.getFacultyId() == null) {
                continue;
            }
            Optional<Faculty> existing = instructorDao.findByCode(member.getFacultyId());
            if (existing.isPresent()) {
                instructorDao.update(member);
            } else {
                instructorDao.insert(member);
            }
            changed = true;
        }
        return changed;
    }

    private boolean migrateStudents() {
        List<Student> students = readEntities("students.dat", Student.class);
        boolean changed = false;
        for (Student student : students) {
            if (student == null || student.getStudentId() == null) {
                continue;
            }
            Optional<Student> existing = studentDao.findByCode(student.getStudentId());
            if (existing.isPresent()) {
                studentDao.update(student);
            } else {
                studentDao.insert(student);
            }
            changed = true;
        }
        return changed;
    }

    private boolean migrateSections() {
        List<Section> sections = readEntities("sections.dat", Section.class);
        boolean changed = false;
        for (Section section : sections) {
            if (section == null || section.getSectionId() == null) {
                continue;
            }
            Optional<Section> existing = sectionDao.findByCode(section.getSectionId());
            if (existing.isPresent()) {
                sectionDao.update(section);
            } else {
                sectionDao.insert(section);
            }
            changed = true;
        }
        return changed;
    }

    private boolean migrateEnrollments() {
        List<EnrollmentRecord> records = readEntities("enrollments.dat", EnrollmentRecord.class);
        if (records.isEmpty()) {
            return false;
        }
        Map<String, Section> sectionCache = new HashMap<>();
        sectionDao.findAll().forEach(section -> sectionCache.put(section.getSectionId(), section));
        boolean changed = false;
        Map<String, Integer> enrolledByCourse = new HashMap<>();

        for (EnrollmentRecord record : records) {
            if (record == null || record.getStudentId() == null || record.getSectionId() == null) {
                continue;
            }
            List<EnrollmentRecord> existing = enrollmentDao.findBySection(record.getSectionId());
            Optional<EnrollmentRecord> match = existing.stream()
                    .filter(enrollment -> record.getStudentId().equals(enrollment.getStudentId()))
                    .findFirst();
            if (match.isPresent()) {
                EnrollmentRecord current = match.get();
                current.setStatus(record.getStatus());
                current.setFinalGrade(record.getFinalGrade());
                enrollmentDao.updateStatus(current);
            } else {
                enrollmentDao.insert(record);
                changed = true;
            }
            if (record.getStatus() == EnrollmentRecord.Status.WAITLISTED) {
                List<String> waitlist = waitlistDao.findWaitlist(record.getSectionId());
                if (!waitlist.contains(record.getStudentId())) {
                    waitlistDao.insert(record.getSectionId(), record.getStudentId(), waitlist.size() + 1);
                }
            }
            if (record.getStatus() == EnrollmentRecord.Status.ENROLLED) {
                Section section = sectionCache.computeIfAbsent(
                        record.getSectionId(),
                        id -> sectionDao.findByCode(id).orElse(null));
                if (section != null && section.getCourseId() != null) {
                    enrolledByCourse.merge(section.getCourseId(), 1, Integer::sum);
                }
            }
        }

        enrolledByCourse.forEach((courseId, enrolledCount) -> courseDao.findByCode(courseId).ifPresent(course -> {
            int available = Math.max(0, course.getTotalSeats() - enrolledCount);
            course.setAvailableSeats(available);
            courseDao.update(course);
        }));
        return changed || !enrolledByCourse.isEmpty();
    }

    private boolean migrateAttendance() {
        List<AttendanceRecord> records = readEntities("attendance.dat", AttendanceRecord.class);
        boolean changed = false;
        for (AttendanceRecord record : records) {
            if (record == null || record.getSectionId() == null || record.getDate() == null) {
                continue;
            }
            attendanceDao.deleteBySectionAndDate(record.getSectionId(), record.getDate());
            // Defensive copy of attendance map to avoid shared references
            Map<String, Boolean> snapshot = new HashMap<>(record.getAttendanceByStudent());
            AttendanceRecord fresh = new AttendanceRecord(record.getSectionId(), record.getDate());
            snapshot.forEach(fresh::markAttendance);
            attendanceDao.insert(fresh);
            changed = true;
        }
        return changed;
    }

    private <T> List<T> readEntities(String fileName, Class<T> type) {
        Path file = dataDirectory.resolve(fileName);
        if (!Files.exists(file)) {
            return List.of();
        }
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
            Object data = ois.readObject();
            return castToList(data, type);
        } catch (Exception ex) {
            LOGGER.warn("Unable to read legacy file {}: {}", fileName, ex.getMessage());
            return List.of();
        }
    }

    private <T> List<T> castToList(Object data, Class<T> type) {
        List<T> results = new ArrayList<>();
        if (data == null) {
            return results;
        }
        if (type.isInstance(data)) {
            results.add(type.cast(data));
            return results;
        }
        if (data instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                appendIfInstance(results, value, type);
            }
            return results;
        }
        if (data instanceof Collection<?> collection) {
            for (Object value : collection) {
                appendIfInstance(results, value, type);
            }
            return results;
        }
        LOGGER.warn("Unexpected legacy payload type {} for {}", data.getClass().getName(), type.getSimpleName());
        return results;
    }

    private <T> void appendIfInstance(List<T> target, Object candidate, Class<T> type) {
        if (type.isInstance(candidate)) {
            target.add(type.cast(candidate));
        }
    }
}
