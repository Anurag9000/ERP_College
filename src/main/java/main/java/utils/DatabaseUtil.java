package main.java.utils;

import main.java.models.*;
import java.io.*;
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
    private static final String USERS_FILE = DATA_DIR + "users.dat";
    private static final String STUDENTS_FILE = DATA_DIR + "students.dat";
    private static final String FACULTY_FILE = DATA_DIR + "faculty.dat";
    private static final String COURSES_FILE = DATA_DIR + "courses.dat";
    private static final String SECTIONS_FILE = DATA_DIR + "sections.dat";
    private static final String ENROLLMENTS_FILE = DATA_DIR + "enrollments.dat";
    private static final String ATTENDANCE_FILE = DATA_DIR + "attendance.dat";
    private static final String NOTIFICATIONS_FILE = DATA_DIR + "notifications.dat";
    private static final String SETTINGS_FILE = DATA_DIR + "settings.dat";
    
    private static Map<String, User> users = new ConcurrentHashMap<>();
    private static Map<String, Student> students = new ConcurrentHashMap<>();
    private static Map<String, Faculty> faculty = new ConcurrentHashMap<>();
    private static Map<String, Course> courses = new ConcurrentHashMap<>();
    private static Map<String, Section> sections = new ConcurrentHashMap<>();
    private static List<EnrollmentRecord> enrollments = Collections.synchronizedList(new ArrayList<>());
    private static Map<String, AttendanceRecord> attendanceRecords = new ConcurrentHashMap<>();
    private static List<NotificationMessage> notifications = Collections.synchronizedList(new ArrayList<>());
    private static Map<String, String> settings = new ConcurrentHashMap<>();
    
    public static void initializeDatabase() {
        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Load existing data or create sample data
        loadData();
        if (users.isEmpty()) {
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

        // Upgrade legacy plaintext passwords if encountered
        boolean upgraded = false;
        for (User user : users.values()) {
            if (user.getSalt() == null || user.getPasswordHash() == null) {
                upgradePassword(user, "ChangeMe123!".toCharArray(), false);
                upgraded = true;
            }
        }
        if (upgraded) {
            saveData();
        }
    }
    
    private static void createSampleData() {
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
            if (new File(USERS_FILE).exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILE));
                users = (Map<String, User>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        
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
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE));
            oos.writeObject(users);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(STUDENTS_FILE));
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
        User user = users.get(username);
        if (user == null || !user.isActive()) {
            return null;
        }
        String salt = user.getSalt();
        String hash = user.getPasswordHash();
        if (salt == null || hash == null) {
            // Legacy record fallback: treat stored passwordHash as plaintext
            if (hash != null && hash.equals(password)) {
                upgradePassword(user, password.toCharArray());
                return user;
            }
            return null;
        }
        boolean matched = PasswordUtil.verifyPassword(password.toCharArray(), salt, hash);
        if (matched) {
            user.setLastLogin(java.time.LocalDateTime.now());
            saveData();
            return user;
        }
        return null;
    }
    
    public static Collection<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public static User getUser(String username) {
        return users.get(username);
    }

    public static synchronized User addUser(String username, String role, String fullName, String email, String rawPassword) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword.toCharArray(), salt);
        User user = new User(username, hash, salt, role, fullName, email);
        users.put(username, user);
        saveData();
        return user;
    }

    public static synchronized void updateUserProfile(String username, String fullName, String email, boolean active) {
        User user = users.get(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(active);
        saveData();
    }

    public static synchronized void changePassword(String username, String newPassword) {
        User user = users.get(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        upgradePassword(user, newPassword.toCharArray());
    }

    private static void upgradePassword(User user, char[] newPassword) {
        upgradePassword(user, newPassword, true);
    }

    private static void upgradePassword(User user, char[] newPassword, boolean persist) {
        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword, newSalt);
        user.setSalt(newSalt);
        user.setPasswordHash(newHash);
        if (persist) {
            saveData();
        }
    }
    
    // Student operations
    public static void addStudent(Student student) {
        students.put(student.getStudentId(), student);
        saveData();
    }
    
    public static void updateStudent(Student student) {
        students.put(student.getStudentId(), student);
        saveData();
    }
    
    public static void deleteStudent(String studentId) {
        students.remove(studentId);
        saveData();
    }
    
    public static Student getStudent(String studentId) {
        return students.get(studentId);
    }
    
    public static Collection<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }

    public static Student findStudentByUsername(String username) {
        return students.values().stream()
                .filter(student -> username.equalsIgnoreCase(student.getUsername()))
                .findFirst()
                .orElse(null);
    }
    
    // Faculty operations
    public static void addFaculty(Faculty facultyMember) {
        faculty.put(facultyMember.getFacultyId(), facultyMember);
        saveData();
    }
    
    public static void updateFaculty(Faculty facultyMember) {
        faculty.put(facultyMember.getFacultyId(), facultyMember);
        saveData();
    }
    
    public static void deleteFaculty(String facultyId) {
        faculty.remove(facultyId);
        saveData();
    }
    
    public static Faculty getFaculty(String facultyId) {
        return faculty.get(facultyId);
    }
    
    public static Collection<Faculty> getAllFaculty() {
        return new ArrayList<>(faculty.values());
    }

    public static Faculty findFacultyByUsername(String username) {
        return faculty.values().stream()
                .filter(member -> username.equalsIgnoreCase(member.getUsername()))
                .findFirst()
                .orElse(null);
    }
    
    // Course operations
    public static void addCourse(Course course) {
        courses.put(course.getCourseId(), course);
        saveData();
    }
    
    public static void updateCourse(Course course) {
        courses.put(course.getCourseId(), course);
        saveData();
    }
    
    public static void deleteCourse(String courseId) {
        courses.remove(courseId);
        saveData();
    }
    
    public static Course getCourse(String courseId) {
        return courses.get(courseId);
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
        return sections.get(sectionId);
    }

    public static void addSection(Section section) {
        sections.put(section.getSectionId(), section);
        saveData();
    }

    public static void updateSection(Section section) {
        sections.put(section.getSectionId(), section);
        saveData();
    }

    public static void deleteSection(String sectionId) {
        sections.remove(sectionId);
        enrollments.removeIf(rec -> rec.getSectionId().equals(sectionId));
        attendanceRecords.entrySet().removeIf(entry -> entry.getKey().startsWith(sectionId + "::"));
        saveData();
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
        saveData();
    }
}
