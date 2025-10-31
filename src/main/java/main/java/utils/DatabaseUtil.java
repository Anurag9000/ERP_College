
import main.java.config.ConfigLoader;
import main.java.data.AuthUserDao;
import main.java.data.dao.CourseDao;
import main.java.data.dao.StudentDao;
import main.java.data.dao.InstructorDao;
import main.java.data.dao.SectionDao;
import main.java.utils.PasswordPolicy;
import main.java.utils.AuditLogService;

import main.java.models.*;
import java.io.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Database utility class for managing data persistence
 * Uses file-based storage for simplicity
 */
public class DatabaseUtil {
    private static final String DATA_DIR = "data/";
    private static final String STUDENTS_FILE = DATA_DIR + "students.dat";
    private static final String FACULTY_FILE = DATA_DIR + "faculty.dat";
    private static final String COURSES_FILE = DATA_DIR + "courses.dat";
    private static final String SECTIONS_FILE = DATA_DIR + "sections.dat";
    private static final String ENROLLMENTS_FILE = DATA_DIR + "enrollments.dat";
    private static final String ATTENDANCE_FILE = DATA_DIR + "attendance.dat";
    private static final String NOTIFICATIONS_FILE = DATA_DIR + "notifications.dat";
    private static final String SETTINGS_FILE = DATA_DIR + "settings.dat";
    
    private static Map<String, Student> students = new ConcurrentHashMap<>();
    private static Map<String, Faculty> faculty = new ConcurrentHashMap<>();
    private static Map<String, Course> courses = new ConcurrentHashMap<>();
    private static Map<String, Section> sections = new ConcurrentHashMap<>();
    private static List<EnrollmentRecord> enrollments = Collections.synchronizedList(new ArrayList<>());
    private static Map<String, AttendanceRecord> attendanceRecords = new ConcurrentHashMap<>();
    private static List<NotificationMessage> notifications = Collections.synchronizedList(new ArrayList<>());
    private static Map<String, String> settings = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS = parseIntConfig("security.maxFailedAttempts", 5);
    private static final int LOCKOUT_MINUTES = parseIntConfig("security.lockoutMinutes", 15);
    private static final int PASSWORD_HISTORY_SIZE = parseIntConfig("security.passwordHistorySize", PasswordPolicy.historySize());
    private static final AuthUserDao authUserDao = new AuthUserDao();
    private static final StudentDao studentDao = new StudentDao();
    private static final CourseDao courseDao = new CourseDao();
    private static final InstructorDao instructorDao = new InstructorDao();
    private static final SectionDao sectionDao = new SectionDao();

    private static int parseIntConfig(String key, int defaultValue) {
        String value = ConfigLoader.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
    
    public static void initializeDatabase() {
        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Load existing data or create sample data
        loadData();
        boolean hasUsers = !authUserDao.findAll().isEmpty();
        if (!hasUsers) {
            createSampleData();
            saveData();
        }

        if (sections == null) {
            sections = new ConcurrentHashMap<>();
        }
        if (enrollments == null) {
            enrollments = Collections.synchronizedList(new ArrayList<>());
        }
        if (attendanceRecords == null) {
            attendanceRecords = new ConcurrentHashMap<>();
        }
        if (notifications == null) {
            notifications = Collections.synchronizedList(new ArrayList<>());
        }
        if (settings == null) {
            settings = new ConcurrentHashMap<>();
        }

        settings.putIfAbsent("maintenance", "false");

        refreshCourseCache();
        refreshStudentCache();
        refreshInstructorCache();
        refreshSectionCache();

    }

    private static void createSampleData() {
        if (authUserDao.findByUsername("admin").isPresent()) {
            return;
        }
        // Create default admin user
        addUser("admin", "Admin", "Administrator", "admin@college.edu", "admin123");
        addUser("inst1", "Instructor", "John Smith", "john.smith@college.edu", "inst123");
        addUser("stu1", "Student", "Alice Johnson", "alice.johnson@student.college.edu", "stud123");
        addUser("stu2", "Student", "Bob Williams", "bob.williams@student.college.edu", "stud456");
        
        // Create sample faculty
        Faculty f1 = new Faculty("FAC001", "John", "Smith", "john.smith@college.edu", 
                                "123-456-7890", "Computer Science", "Professor", "Ph.D", 75000);
        f1.getSubjects().addAll(Arrays.asList("Data Structures", "Algorithms", "Java Programming"));
        f1.setUsername("inst1");
        faculty.put(f1.getFacultyId(), f1);
        
        Faculty f2 = new Faculty("FAC002", "Jane", "Davis", "jane.davis@college.edu", 
                                "123-456-7891", "Mathematics", "Associate Professor", "M.Sc", 65000);
        f2.getSubjects().addAll(Arrays.asList("Calculus", "Linear Algebra", "Statistics"));
        faculty.put(f2.getFacultyId(), f2);
        
        // Create sample courses
        Course c1 = new Course("CSE101", "Computer Science Engineering", "Computer Science", 
                              8, 200000, "4-year undergraduate program in Computer Science", 60);
        c1.getSubjects().addAll(Arrays.asList("Programming", "Data Structures", "Algorithms", 
                                            "Database Systems", "Operating Systems"));
        courses.put(c1.getCourseId(), c1);
        
        Course c2 = new Course("MATH101", "Mathematics", "Mathematics", 
                              6, 150000, "3-year undergraduate program in Mathematics", 40);
        c2.getSubjects().addAll(Arrays.asList("Calculus", "Linear Algebra", "Statistics", 
                                            "Discrete Mathematics"));
        courses.put(c2.getCourseId(), c2);
        
        // Create sample students
        Student s1 = new Student("STU001", "Alice", "Johnson", "alice.johnson@student.college.edu", 
                               "987-654-3210", LocalDate.of(2000, 5, 15), 
                               "123 Main St, City", "CSE101", 3);
        s1.setTotalFees(200000);
        s1.setFeesPaid(150000);
        s1.setCreditsCompleted(72);
        s1.setCreditsInProgress(18);
        s1.setCgpa(7.8);
        s1.setNextFeeDueDate(LocalDate.now().plusDays(45));
        s1.setUsername("stu1");
        students.put(s1.getStudentId(), s1);
        
        Student s2 = new Student("STU002", "Bob", "Williams", "bob.williams@student.college.edu", 
                               "987-654-3211", LocalDate.of(1999, 8, 22), 
                               "456 Oak Ave, City", "MATH101", 2);
        s2.setTotalFees(150000);
        s2.setFeesPaid(100000);
        s2.setCreditsCompleted(36);
        s2.setCreditsInProgress(18);
        s2.setCgpa(8.4);
        s2.setNextFeeDueDate(LocalDate.now().plusDays(20));
        s2.setUsername("stu2");
        students.put(s2.getStudentId(), s2);
        
        // Update course seats
        c1.setAvailableSeats(c1.getAvailableSeats() - 1);
        c2.setAvailableSeats(c2.getAvailableSeats() - 1);

        // Create sample sections
        Section sec1 = new Section(
            "SEC101A",
            c1.getCourseId(),
            "Data Structures - A",
            f1.getFacultyId(),
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            "Room CS-101",
            30
        );
        sec1.setSemester("Fall");
        sec1.setYear(LocalDate.now().getYear());
        sec1.setDropDeadline(LocalDate.now().plusDays(25));
        sec1.enrollStudent(s1.getStudentId());
        sections.put(sec1.getSectionId(), sec1);

        Section sec2 = new Section(
            "SEC101B",
            c1.getCourseId(),
            "Algorithms - A",
            f1.getFacultyId(),
            DayOfWeek.WEDNESDAY,
            LocalTime.of(11, 0),
            LocalTime.of(12, 30),
            "Room CS-201",
            30
        );
        sec2.setSemester("Fall");
        sec2.setYear(LocalDate.now().getYear());
        sec2.setDropDeadline(LocalDate.now().plusDays(25));
        sec2.getWaitlistedStudentIds().add("STU002");
        sections.put(sec2.getSectionId(), sec2);

        Section sec3 = new Section(
            "SEC201A",
            c2.getCourseId(),
            "Statistics - A",
            f2.getFacultyId(),
            DayOfWeek.TUESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(15, 30),
            "Room MATH-101",
            25
        );
        sec3.setSemester("Fall");
        sec3.setYear(LocalDate.now().getYear());
        sec3.setDropDeadline(LocalDate.now().plusDays(25));
        sec3.enrollStudent(s2.getStudentId());
        sections.put(sec3.getSectionId(), sec3);

        enrollments.add(new EnrollmentRecord(s1.getStudentId(), sec1.getSectionId(), EnrollmentRecord.Status.ENROLLED));
        enrollments.add(new EnrollmentRecord(s2.getStudentId(), sec3.getSectionId(), EnrollmentRecord.Status.ENROLLED));
        enrollments.add(new EnrollmentRecord(s2.getStudentId(), sec2.getSectionId(), EnrollmentRecord.Status.WAITLISTED));

        // Seed welcome notifications
        notifications.add(new NotificationMessage(
            NotificationMessage.Audience.ALL,
            null,
            "📅 Semester opens next Monday. Check your timetable for clashes.",
            "General"));
        notifications.add(new NotificationMessage(
            NotificationMessage.Audience.STUDENT,
            s1.getStudentId(),
            "Fees due in 45 days. Outstanding balance ₹" + String.format("%.0f", s1.getOutstandingFees()),
            "Finance"));
        notifications.add(new NotificationMessage(
            NotificationMessage.Audience.STUDENT,
            s2.getStudentId(),
            "You are waitlisted for Algorithms - A. We'll auto-enrol if a seat frees up.",
            "Registration"));
    }
    
    @SuppressWarnings("unchecked")
    private static void loadData() {
        try {
            if (new File(STUDENTS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(STUDENTS_FILE));
                students = (Map<String, Student>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading students: " + e.getMessage());
        }
        
        try {
            if (new File(FACULTY_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FACULTY_FILE));
                faculty = (Map<String, Faculty>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading faculty: " + e.getMessage());
        }
        
        try {
            if (new File(COURSES_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(COURSES_FILE));
                courses = (Map<String, Course>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading courses: " + e.getMessage());
        }

        try {
            if (new File(SECTIONS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SECTIONS_FILE));
                sections = (Map<String, Section>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading sections: " + e.getMessage());
        }

        try {
            if (new File(ENROLLMENTS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ENROLLMENTS_FILE));
                enrollments = (List<EnrollmentRecord>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading enrollments: " + e.getMessage());
        }

        try {
            if (new File(ATTENDANCE_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ATTENDANCE_FILE));
                attendanceRecords = (Map<String, AttendanceRecord>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading attendance: " + e.getMessage());
        }

        try {
            if (new File(NOTIFICATIONS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(NOTIFICATIONS_FILE));
                notifications = (List<NotificationMessage>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading notifications: " + e.getMessage());
        }

        try {
            if (new File(SETTINGS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SETTINGS_FILE));
                settings = (Map<String, String>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }
    
    public static void saveData() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STUDENTS_FILE));
            oos.writeObject(students);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(FACULTY_FILE));
            oos.writeObject(faculty);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(COURSES_FILE));
            oos.writeObject(courses);
            oos.close();

            oos = new ObjectOutputStream(new FileOutputStream(SECTIONS_FILE));
            oos.writeObject(sections);
            oos.close();

            oos = new ObjectOutputStream(new FileOutputStream(ENROLLMENTS_FILE));
            oos.writeObject(enrollments);
            oos.close();

            oos = new ObjectOutputStream(new FileOutputStream(ATTENDANCE_FILE));
            oos.writeObject(attendanceRecords);
            oos.close();

            oos = new ObjectOutputStream(new FileOutputStream(NOTIFICATIONS_FILE));
            oos.writeObject(notifications);
            oos.close();

            oos = new ObjectOutputStream(new FileOutputStream(SETTINGS_FILE));
            oos.writeObject(settings);
            oos.close();
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }
    
    // User operations
    public static synchronized User authenticateUser(String username, String password) {
        LocalDateTime now = LocalDateTime.now();
        Optional<User> optionalUser = authUserDao.findByUsername(username);
        if (optionalUser.isEmpty()) {
            AuditLogService.log(AuditLogService.EventType.LOGIN_FAILURE, username, "Unknown user");
            return null;
        }
        User user = optionalUser.get();
        if (!user.isActive()) {
            AuditLogService.log(AuditLogService.EventType.LOGIN_FAILURE, username, "Inactive account");
            return null;
        }

        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            AuditLogService.log(AuditLogService.EventType.ACCOUNT_LOCKED, username,
                    "Account locked until " + user.getLockedUntil());
            return null;
        }

        boolean matched;
        String salt = user.getSalt();
        String hash = user.getPasswordHash();
        if (salt == null || hash == null) {
            matched = false;
        } else {
            matched = PasswordUtil.verifyPassword(password.toCharArray(), salt, hash);
        }

        if (matched) {
            user.resetFailedAttempts();
            user.setLockedUntil(null);
            user.setLastLogin(now);
            authUserDao.recordLoginSuccess(user);
            AuditLogService.log(AuditLogService.EventType.LOGIN_SUCCESS, username, "Login successful");
            return user;
        } else {
            int failedAttempts = user.getFailedAttempts() + 1;
            LocalDateTime lockUntil = null;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                lockUntil = now.plusMinutes(LOCKOUT_MINUTES);
                AuditLogService.log(AuditLogService.EventType.ACCOUNT_LOCKED, username,
                        "Exceeded failed login attempts");
                failedAttempts = 0;
            } else {
                AuditLogService.log(AuditLogService.EventType.LOGIN_FAILURE, username,
                        "Invalid credentials (" + failedAttempts + "/" + MAX_FAILED_ATTEMPTS + ")");
            }
            user.setFailedAttempts(failedAttempts);
            user.setLockedUntil(lockUntil);
            authUserDao.recordLoginFailure(user, failedAttempts, lockUntil);
            return null;
        }
    }
    
    public static Collection<User> getAllUsers() {
        return authUserDao.findAll();
    }

    public static User getUser(String username) {
        return authUserDao.findByUsername(username).orElse(null);
    }

    public static synchronized User addUser(String username, String role, String fullName, String email, String rawPassword) {
        PasswordPolicy.validateComplexity(rawPassword);
        if (authUserDao.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword.toCharArray(), salt);
        User user = new User(username, hash, salt, role, fullName, email);
        user.setActive(true);
        user.setMustChangePassword(false);
        user.addPasswordHistory(salt, hash, PASSWORD_HISTORY_SIZE);
        return authUserDao.insert(user);
    }

    public static synchronized void updateUserProfile(String username, String fullName, String email, boolean active) {
        User user = requireUser(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(active);
        authUserDao.updateProfile(user);
    }

    public static synchronized void changePasswordSelf(String username, String currentPassword, String newPassword) {
        User user = requireUser(username);
        if (!PasswordUtil.verifyPassword(currentPassword.toCharArray(), user.getSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        applyNewPassword(user, newPassword, false, false);
        AuditLogService.log(AuditLogService.EventType.PASSWORD_CHANGED, username, "User-initiated change");
    }

    public static synchronized void resetPasswordByAdmin(String username, String newPassword) {
        User user = requireUser(username);
        applyNewPassword(user, newPassword, true, false);
        AuditLogService.log(AuditLogService.EventType.PASSWORD_RESET, username, "Admin reset password");
    }
    
    // Student operations
    public static void addStudent(Student student) {
        studentDao.insert(student);
        students.put(student.getStudentId(), student);
    }
    
    public static void updateStudent(Student student) {
        studentDao.update(student);
        students.put(student.getStudentId(), student);
    }
    
    public static void deleteStudent(String studentId) {
        studentDao.delete(studentId);
        students.remove(studentId);
    }
    
    public static Student getStudent(String studentId) {
        Student student = students.get(studentId);
        if (student == null) {
            studentDao.findByCode(studentId).ifPresent(st -> students.put(studentId, st));
            student = students.get(studentId);
        }
        return student;
    }
    
    public static Collection<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }

    public static Student findStudentByUsername(String username) {
        return studentDao.findByUsername(username).orElse(null);
    }
    
    // Faculty operations
    public static void addFaculty(Faculty facultyMember) {
        instructorDao.insert(facultyMember);
        faculty.put(facultyMember.getFacultyId(), facultyMember);
    }

    public static void updateFaculty(Faculty facultyMember) {
        instructorDao.update(facultyMember);
        faculty.put(facultyMember.getFacultyId(), facultyMember);
    }

    public static void deleteFaculty(String facultyId) {
        instructorDao.delete(facultyId);
        faculty.remove(facultyId);
    }

    public static Faculty getFaculty(String facultyId) {
        Faculty member = faculty.get(facultyId);
        if (member == null) {
            instructorDao.findByCode(facultyId).ifPresent(f -> faculty.put(facultyId, f));
            member = faculty.get(facultyId);
        }
        return member;
    }

    public static Collection<Faculty> getAllFaculty() {
        return new ArrayList<>(faculty.values());
    }

    public static Faculty findFacultyByUsername(String username) {
        return instructorDao.findByUsername(username).orElse(null);
    }
    
    // Course operations
    public static void addCourse(Course course) {
        courseDao.insert(course);
        courses.put(course.getCourseId(), course);
    }

    public static void updateCourse(Course course) {
        courseDao.update(course);
        courses.put(course.getCourseId(), course);
    }

    public static void deleteCourse(String courseId) {
        courseDao.delete(courseId);
        courses.remove(courseId);
    }

    public static Course getCourse(String courseId) {
        Course course = courses.get(courseId);
        if (course == null) {
            courseDao.findByCode(courseId).ifPresent(c -> courses.put(courseId, c));
            course = courses.get(courseId);
        }
        return course;
    }

    public static Collection<Course> getAllCourses() {
        return new ArrayList<>(courses.values());
    }
    
    public static String generateNextId(String prefix, Collection<?> collection) {
        int maxId = 0;
        for (Object obj : collection) {
            String id = "";
            if (obj instanceof Student) {
                id = ((Student) obj).getStudentId();
            } else if (obj instanceof Faculty) {
                id = ((Faculty) obj).getFacultyId();
            } else if (obj instanceof Course) {
                id = ((Course) obj).getCourseId();
            } else if (obj instanceof Section) {
                id = ((Section) obj).getSectionId();
            }
            
            if (id.startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(id.substring(prefix.length()));
                    maxId = Math.max(maxId, num);
                } catch (NumberFormatException e) {
                    // Ignore non-numeric suffixes
                }
            }
        }
        return prefix + String.format("%03d", maxId + 1);
    }

    // Section operations
    public static Collection<Section> getAllSections() {
        return new ArrayList<>(sections.values());
    }

    public static Section getSection(String sectionId) {
        Section section = sections.get(sectionId);
        if (section == null) {
            section = sectionDao.findByCode(sectionId).orElse(null);
            if (section != null) {
                section.getEnrolledStudentIds().clear();
                section.getWaitlistedStudentIds().clear();
                sections.put(sectionId, section);
                populateSectionEnrollmentState();
            }
        }
        return section;
    }

    public static void addSection(Section section) {
        sectionDao.insert(section);
        sections.put(section.getSectionId(), section);
    }

    public static void updateSection(Section section) {
        sectionDao.update(section);
        sections.put(section.getSectionId(), section);
    }

    public static void deleteSection(String sectionId) {
        sectionDao.delete(sectionId);
        sections.remove(sectionId);
        enrollments.removeIf(rec -> rec.getSectionId().equals(sectionId));
        attendanceRecords.entrySet().removeIf(entry -> entry.getKey().startsWith(sectionId + "::"));
    }

    // Enrollment operations
    public static List<EnrollmentRecord> getEnrollmentsForStudent(String studentId) {
        return enrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    public static List<EnrollmentRecord> getEnrollmentsForSection(String sectionId) {
        return enrollments.stream()
                .filter(rec -> rec.getSectionId().equals(sectionId))
                .collect(Collectors.toList());
    }

    public static synchronized EnrollmentRecord registerStudentToSection(String studentId, String sectionId) {
        Section section = sections.get(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found");
        }
        Student student = students.get(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found");
        }

        ensureEnrollmentCollections();

        if (section.hasStudent(studentId)) {
            throw new IllegalStateException("Student already enrolled or waitlisted in this section");
        }

        if (hasScheduleConflict(studentId, section)) {
            throw new IllegalStateException("Schedule conflict detected with another section");
        }

        EnrollmentRecord record;
        if (!section.isFull()) {
            section.enrollStudent(studentId);
            record = new EnrollmentRecord(studentId, sectionId, EnrollmentRecord.Status.ENROLLED);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "You are enrolled in " + section.getTitle() + " (" + section.getSectionId() + ").",
                    "Registration"));
        } else {
            section.waitlistStudent(studentId);
            record = new EnrollmentRecord(studentId, sectionId, EnrollmentRecord.Status.WAITLISTED);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "Section " + section.getTitle() + " is full. You are #"
                        + section.getWaitlistedStudentIds().size() + " on the waitlist.",
                    "Registration"));
        }

        enrollments.add(record);
        saveData();
        return record;
    }

    private static boolean hasScheduleConflict(String studentId, Section targetSection) {
        return enrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(rec -> sections.get(rec.getSectionId()))
                .filter(Objects::nonNull)
                .anyMatch(existing -> overlaps(existing, targetSection));
    }

    private static boolean overlaps(Section a, Section b) {
        if (a.getDayOfWeek() != b.getDayOfWeek()) {
            return false;
        }
        return !(b.getEndTime().isBefore(a.getStartTime()) || b.getStartTime().isAfter(a.getEndTime()));
    }

    public static synchronized void dropStudentFromSection(String studentId, String sectionId) {
        Section section = sections.get(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found");
        }

        ensureEnrollmentCollections();

        Optional<EnrollmentRecord> recordOpt = enrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId) && rec.getSectionId().equals(sectionId))
                .findFirst();

        if (!recordOpt.isPresent()) {
            throw new IllegalStateException("Student not enrolled in the section");
        }

        EnrollmentRecord record = recordOpt.get();
        record.setStatus(EnrollmentRecord.Status.DROPPED);
        section.removeStudent(studentId);

        String promotedStudent = section.promoteNextWaitlisted();
        if (promotedStudent != null) {
            enrollments.stream()
                    .filter(rec -> rec.getStudentId().equals(promotedStudent) && rec.getSectionId().equals(sectionId))
                    .findFirst()
                    .ifPresent(promoted -> promoted.setStatus(EnrollmentRecord.Status.ENROLLED));
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    promotedStudent,
                    "Great news! A seat opened up in " + section.getTitle() + " and you are now enrolled.",
                    "Registration"));
        }

        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "You dropped " + section.getTitle() + " (" + section.getSectionId() + ").",
                "Registration"));

        saveData();
    }

    private static void ensureEnrollmentCollections() {
        if (enrollments == null) {
            enrollments = Collections.synchronizedList(new ArrayList<>());
        }
        if (sections == null) {
            sections = new ConcurrentHashMap<>();
        }
    }

    public static List<Section> getScheduleForStudent(String studentId) {
        return enrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(rec -> sections.get(rec.getSectionId()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Section::getDayOfWeek).thenComparing(Section::getStartTime))
                .collect(Collectors.toList());
    }

    // Attendance operations
    public static void recordAttendance(String sectionId, LocalDate date, Map<String, Boolean> attendance) {
        String key = sectionId + "::" + date.toString();
        AttendanceRecord record = attendanceRecords.getOrDefault(key, new AttendanceRecord(sectionId, date));
        record.getAttendanceByStudent().clear();
        record.getAttendanceByStudent().putAll(attendance);
        attendanceRecords.put(key, record);
        saveData();
    }

    public static List<AttendanceRecord> getAttendanceForSection(String sectionId) {
        return attendanceRecords.values().stream()
                .filter(rec -> rec.getSectionId().equals(sectionId))
                .sorted(Comparator.comparing(AttendanceRecord::getDate))
                .collect(Collectors.toList());
    }

    // Notification operations
    public static List<NotificationMessage> getNotifications(NotificationMessage.Audience audience, String targetId) {
        return notifications.stream()
                .filter(msg -> msg.getAudience() == NotificationMessage.Audience.ALL
                        || msg.getAudience() == audience
                        || (msg.getAudience() == NotificationMessage.Audience.USER
                            && Objects.equals(msg.getTargetId(), targetId)))
                .collect(Collectors.toList());
    }

    public static void addNotification(NotificationMessage notification) {
        notifications.add(notification);
        saveData();
    }

    public static Map<String, Long> getWaitlistCountsByCourse() {
        return sections.values().stream()
                .collect(Collectors.groupingBy(
                        Section::getCourseId,
                        Collectors.summingLong(sec -> sec.getWaitlistedStudentIds().size())
                ));
    }

    public static double getAverageAttendanceForSection(String sectionId) {
        List<AttendanceRecord> records = getAttendanceForSection(sectionId);
        if (records.isEmpty()) {
            return 100.0;
        }
        return records.stream()
                .mapToDouble(AttendanceRecord::getAttendancePercentage)
                .average()
                .orElse(100.0);
    }

    private static User requireUser(String username) {
        return authUserDao.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private static void applyNewPassword(User user, String newPassword, boolean mustChangeNext, boolean skipValidation) {
        if (!skipValidation) {
            PasswordPolicy.validateComplexity(newPassword);
            ensureNotInHistory(user, newPassword);
        }

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword.toCharArray(), newSalt);
        user.addPasswordHistory(newSalt, newHash, PASSWORD_HISTORY_SIZE);
        user.setSalt(newSalt);
        user.setPasswordHash(newHash);
        user.resetFailedAttempts();
        user.setLockedUntil(null);
        user.setMustChangePassword(mustChangeNext);
        authUserDao.updatePassword(user, newSalt, newHash, mustChangeNext);
    }

    private static void ensureNotInHistory(User user, String candidate) {
        if (user.getPasswordHistory().isEmpty()) {
            return;
        }
        for (String entry : user.getPasswordHistory()) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            if (PasswordUtil.verifyPassword(candidate.toCharArray(), parts[0], parts[1])) {
                throw new IllegalArgumentException("Password was used recently. Choose a different password.");
            }
        }
    }

    // Settings and maintenance
    public static boolean isMaintenanceMode() {
        return Boolean.parseBoolean(settings.getOrDefault("maintenance", "false"));
    }

    public static void setMaintenanceMode(boolean maintenanceOn) {
        settings.put("maintenance", Boolean.toString(maintenanceOn));
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.ALL,
                null,
                "Maintenance mode is now " + (maintenanceOn ? "ON" : "OFF") + ".",
                "System"));
        AuditLogService.log(AuditLogService.EventType.MAINTENANCE_TOGGLE,
                "system",
                "Maintenance mode set to " + maintenanceOn);
        saveData();
    }

    public static boolean isUserLocked(String username) {
        return authUserDao.findByUsername(username)
                .map(user -> user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil()))
                .orElse(false);
    }

    public static int remainingAttempts(String username) {
        return authUserDao.findByUsername(username)
                .map(user -> Math.max(0, MAX_FAILED_ATTEMPTS - user.getFailedAttempts()))
                .orElse(MAX_FAILED_ATTEMPTS);
    }

    private static void refreshStudentCache() {
        students = new ConcurrentHashMap<>();
        for (Student student : studentDao.findAll()) {
            students.put(student.getStudentId(), student);
        }
    }

    private static void refreshCourseCache() {
        courses = new ConcurrentHashMap<>();
        for (Course course : courseDao.findAll()) {
            courses.put(course.getCourseId(), course);
        }
    }

    private static void refreshInstructorCache() {
        faculty = new ConcurrentHashMap<>();
        for (Faculty member : instructorDao.findAll()) {
            faculty.put(member.getFacultyId(), member);
        }
    }

    private static void refreshSectionCache() {
        sections = new ConcurrentHashMap<>();
        for (Section section : sectionDao.findAll()) {
            section.getEnrolledStudentIds().clear();
            section.getWaitlistedStudentIds().clear();
            sections.put(section.getSectionId(), section);
        }
        populateSectionEnrollmentState();
    }

    private static void populateSectionEnrollmentState() {
        DataSourceRegistry.erpDataSource().ifPresent(ds -> {
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT section_code, student_code, status FROM enrollments")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String sectionCode = rs.getString(1);
                        String studentCode = rs.getString(2);
                        String status = rs.getString(3);
                        Section section = sections.get(sectionCode);
                        if (section == null) {
                            continue;
                        }
                        if ("WAITLISTED".equalsIgnoreCase(status)) {
                            section.getWaitlistedStudentIds().add(studentCode);
                        } else if ("ENROLLED".equalsIgnoreCase(status)) {
                            section.getEnrolledStudentIds().add(studentCode);
                        }
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error populating section enrollments: " + ex.getMessage());
            }

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT section_code, student_code FROM section_waitlist ORDER BY section_code, position")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String sectionCode = rs.getString(1);
                        String studentCode = rs.getString(2);
                        Section section = sections.get(sectionCode);
                        if (section != null && !section.getWaitlistedStudentIds().contains(studentCode)) {
                            section.getWaitlistedStudentIds().add(studentCode);
                        }
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error populating section waitlists: " + ex.getMessage());
            }
        });
    }
}
