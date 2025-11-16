
import main.java.config.ConfigLoader;
import main.java.data.AuthUserDao;
import main.java.data.dao.CourseDao;
import main.java.data.dao.StudentDao;
import main.java.data.dao.InstructorDao;
import main.java.data.dao.SectionDao;
import main.java.data.dao.EnrollmentDao;
import main.java.data.dao.WaitlistDao;
import main.java.data.dao.AttendanceDao;
import main.java.data.dao.NotificationDao;
import main.java.data.dao.SettingsDao;
import main.java.data.dao.CoursePrerequisiteDao;
import main.java.data.dao.CourseRelationshipDao;
import main.java.data.dao.PaymentTransactionDao;
import main.java.data.dao.FeeInstallmentDao;
import main.java.data.migration.LegacyDataMigrator;
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
 * Database utility facade exposing high-level operations backed by the DAO layer.
 */
public class DatabaseUtil {
    private static final String DATA_DIR = "data/";
    private static final String STUDENTS_FILE = DATA_DIR + "students.dat";
    private static final String FACULTY_FILE = DATA_DIR + "faculty.dat";
    private static final String COURSES_FILE = DATA_DIR + "courses.dat";
    private static final String SECTIONS_FILE = DATA_DIR + "sections.dat";
    private static Map<String, Student> students = new ConcurrentHashMap<>();
    private static Map<String, Faculty> faculty = new ConcurrentHashMap<>();
    private static Map<String, Course> courses = new ConcurrentHashMap<>();
    private static Map<String, Section> sections = new ConcurrentHashMap<>();
    private static Map<String, String> settings = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS = parseIntConfig("security.maxFailedAttempts", 5);
    private static final int LOCKOUT_MINUTES = parseIntConfig("security.lockoutMinutes", 15);
    private static final int PASSWORD_HISTORY_SIZE = parseIntConfig("security.passwordHistorySize", PasswordPolicy.historySize());
    private static final int MAX_TERM_CREDITS = parseIntConfig("registration.maxCredits", 24);
    private static final AuthUserDao authUserDao = new AuthUserDao();
    private static final StudentDao studentDao = new StudentDao();
    private static final CourseDao courseDao = new CourseDao();
    private static final InstructorDao instructorDao = new InstructorDao();
    private static final SectionDao sectionDao = new SectionDao();
    private static final EnrollmentDao enrollmentDao = new EnrollmentDao();
    private static final WaitlistDao waitlistDao = new WaitlistDao();
    private static final AttendanceDao attendanceDao = new AttendanceDao();
    private static final NotificationDao notificationDao = new NotificationDao();
    private static final SettingsDao settingsDao = new SettingsDao();
    private static final CoursePrerequisiteDao coursePrerequisiteDao = new CoursePrerequisiteDao();
    private static final CourseRelationshipDao courseRelationshipDao = new CourseRelationshipDao();
    private static final PaymentTransactionDao paymentTransactionDao = new PaymentTransactionDao();
    private static final FeeInstallmentDao feeInstallmentDao = new FeeInstallmentDao();

    private static final Map<String, List<String>> coursePrerequisiteCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> courseCorequisiteCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> courseAntirequisiteCache = new ConcurrentHashMap<>();
    private static final double PASSING_GRADE_THRESHOLD = 40.0;

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

    private static void seedFinanceData(Student... sampleStudents) {
        LocalDate today = LocalDate.now();
        for (Student student : sampleStudents) {
            List<PaymentTransaction> transactions = new ArrayList<>();
            List<FeeInstallment> installments = new ArrayList<>();

            double paid = student.getFeesPaid();
            if (paid > 0) {
                double split = Math.max(1, Math.round(paid / 2.0 / 1000) * 1000);
                PaymentTransaction t1 = new PaymentTransaction(
                        student.getStudentId(),
                        Math.min(split, paid),
                        today.minusMonths(4),
                        "UPI",
                        "TXN-" + student.getStudentId() + "-A",
                        "Initial tuition payment");
                PaymentTransaction t2 = new PaymentTransaction(
                        student.getStudentId(),
                        Math.max(0, paid - t1.getAmount()),
                        today.minusMonths(2),
                        "Bank Transfer",
                        "TXN-" + student.getStudentId() + "-B",
                        "Mid-term installment");
                transactions.add(t1);
                if (t2.getAmount() > 0) {
                    transactions.add(t2);
                }
            }

            double remaining = Math.max(0.0, student.getTotalFees() - paid);
            if (remaining > 0) {
                double installmentAmount = Math.max(1, Math.round(remaining / 3.0 / 1000) * 1000);
                for (int i = 1; i <= 3; i++) {
                    FeeInstallment installment = new FeeInstallment(
                            student.getStudentId(),
                            today.plusMonths(i).withDayOfMonth(5),
                            Math.min(remaining, installmentAmount),
                            "Installment " + i + " for AY " + today.getYear());
                    if (installment.getDueDate().isBefore(today)) {
                        installment.setStatus(FeeInstallment.Status.OVERDUE);
                    } else {
                        installment.setStatus(FeeInstallment.Status.DUE);
                    }
                    installments.add(installment);
                    remaining -= installment.getAmount();
                    if (remaining <= 0) {
                        break;
                    }
                }
            }

            for (PaymentTransaction tx : transactions) {
                paymentTransactionDao.insert(tx);
            }
            for (FeeInstallment installment : installments) {
                feeInstallmentDao.insert(installment);
            }
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
        try {
            LegacyDataMigrator.defaultMigrator().migrateAll();
        } catch (Exception ex) {
            System.err.println("Legacy data migration failed: " + ex.getMessage());
        }
        boolean hasUsers = !authUserDao.findAll().isEmpty();
        if (!hasUsers) {
            createSampleData();
            saveData();
        }

        if (sections == null) {
            sections = new ConcurrentHashMap<>();
        }
        if (settings == null) {
            settings = new ConcurrentHashMap<>();
        }
        if (settings.putIfAbsent("maintenance", "false") == null) {
            settingsDao.upsert("maintenance", "false");
        }

        refreshCourseCache();
        refreshStudentCache();
        refreshInstructorCache();
        refreshSectionCache();
        coursePrerequisiteCache.clear();

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
        f1.setUsername("inst1");
        instructorDao.insert(f1);
        faculty.put(f1.getFacultyId(), f1);
        
        Faculty f2 = new Faculty("FAC002", "Jane", "Davis", "jane.davis@college.edu", 
                                "123-456-7891", "Mathematics", "Associate Professor", "M.Sc", 65000);
        instructorDao.insert(f2);
        faculty.put(f2.getFacultyId(), f2);
        
        Course c1 = new Course("CSE101", "Computer Science Engineering", "Computer Science", 
                              8, 200000, "4-year undergraduate program in Computer Science", 60);
        addCourse(c1);
        
        Course c2 = new Course("MATH101", "Mathematics", "Mathematics", 
                              6, 150000, "3-year undergraduate program in Mathematics", 40);
        addCourse(c2);
        
        Student s1 = new Student("STU001", "Alice", "Johnson", "alice.johnson@student.college.edu", 
                               "987-654-3210", LocalDate.of(2000, 5, 15), 
                               "123 Main St, City", "CSE101", 3);
        s1.setTotalFees(200000);
        s1.setFeesPaid(150000);
        s1.setCreditsCompleted(72);
        s1.setCreditsInProgress(4);
        s1.setCgpa(7.8);
        s1.setNextFeeDueDate(LocalDate.now().plusDays(45));
        s1.setUsername("stu1");
        addStudent(s1);
        
        Student s2 = new Student("STU002", "Bob", "Williams", "bob.williams@student.college.edu", 
                               "987-654-3211", LocalDate.of(1999, 8, 22), 
                               "456 Oak Ave, City", "MATH101", 2);
        s2.setTotalFees(150000);
        s2.setFeesPaid(100000);
        s2.setCreditsCompleted(36);
        s2.setCreditsInProgress(3);
        s2.setCgpa(8.4);
        s2.setNextFeeDueDate(LocalDate.now().plusDays(20));
        s2.setUsername("stu2");
        addStudent(s2);

        seedFinanceData(s1, s2);
        
        Course course1 = getCourse("CSE101");
        course1.setAvailableSeats(course1.getAvailableSeats() - 1);
        updateCourse(course1);
        Course course2 = getCourse("MATH101");
        course2.setAvailableSeats(course2.getAvailableSeats() - 1);
        updateCourse(course2);

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
        addSection(sec1);

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
        addSection(sec2);

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
        addSection(sec3);

        EnrollmentRecord er1 = new EnrollmentRecord(s1.getStudentId(), sec1.getSectionId(), EnrollmentRecord.Status.ENROLLED);
        enrollmentDao.insert(er1);
        EnrollmentRecord er2 = new EnrollmentRecord(s2.getStudentId(), sec3.getSectionId(), EnrollmentRecord.Status.ENROLLED);
        enrollmentDao.insert(er2);
        EnrollmentRecord er3 = new EnrollmentRecord(s2.getStudentId(), sec2.getSectionId(), EnrollmentRecord.Status.WAITLISTED);
        enrollmentDao.insert(er3);
        waitlistDao.insert(sec2.getSectionId(), s2.getStudentId(), 1);

        // Seed welcome notifications
        addNotification(new NotificationMessage(
            NotificationMessage.Audience.ALL,
            null,
            "Semester opens next Monday. Check your timetable for clashes.",
            "General"));
        addNotification(new NotificationMessage(
            NotificationMessage.Audience.STUDENT,
            s1.getStudentId(),
            "Fees due in 45 days. Outstanding balance Rs " + String.format("%.0f", s1.getOutstandingFees()),
            "Finance"));
        addNotification(new NotificationMessage(
            NotificationMessage.Audience.STUDENT,
            s2.getStudentId(),
            "You are waitlisted for Algorithms - A. We'll auto-enrol if a seat frees up.",
            "Registration"));
    }
    
    @SuppressWarnings("unchecked")
    private static void loadData() {
        students = new ConcurrentHashMap<>();
        faculty = new ConcurrentHashMap<>();
        courses = new ConcurrentHashMap<>();
        sections = new ConcurrentHashMap<>();
        settings = new ConcurrentHashMap<>(settingsDao.findAll());
    }
    
    public static void saveData() {
        // No-op retained for backward compatibility with legacy callers.
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
        user.setMustChangePassword(true);
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

    // Finance operations
    public static List<PaymentTransaction> getPaymentHistoryForStudent(String studentId) {
        return paymentTransactionDao.findByStudent(studentId).stream()
                .map(DatabaseUtil::copyTransaction)
                .collect(Collectors.toList());
    }

    public static List<FeeInstallment> getInstallmentsForStudent(String studentId) {
        return feeInstallmentDao.findByStudent(studentId).stream()
                .map(DatabaseUtil::cloneInstallment)
                .collect(Collectors.toList());
    }

    public static synchronized PaymentTransaction recordPayment(String actorUsername,
                                                                String studentId,
                                                                double amount,
                                                                String method,
                                                                String reference,
                                                                String notes) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        Student student = getStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }

        PaymentTransaction transaction = new PaymentTransaction(studentId, amount, LocalDate.now(), method, reference, notes);
        paymentTransactionDao.insert(transaction);

        double updatedPaid = Math.min(student.getTotalFees(), student.getFeesPaid() + amount);
        student.setFeesPaid(updatedPaid);
        updateStudent(student);

        List<FeeInstallment> schedule = feeInstallmentDao.findByStudent(studentId);
        schedule.sort(Comparator.comparing(installment -> installment.getDueDate() == null
                ? LocalDate.MAX
                : installment.getDueDate()));
        double remaining = amount;
        for (FeeInstallment installment : schedule) {
            if (installment.getStatus() == FeeInstallment.Status.PAID) {
                continue;
            }
            double installmentAmount = installment.getAmount();
            if (remaining + 1e-3 >= installmentAmount) {
                installment.setStatus(FeeInstallment.Status.PAID);
                installment.setPaidOn(LocalDate.now());
                remaining -= installmentAmount;
                feeInstallmentDao.update(installment);
            } else {
                break;
            }
        }

        AuditLogService.log(AuditLogService.EventType.FINANCE_PAYMENT,
                actorUsername != null ? actorUsername : "system",
                String.format(Locale.ENGLISH, "Recorded payment %.2f for %s", amount, studentId));
        return transaction;
    }

    public static void upsertInstallment(String studentId, FeeInstallment installment) {
        installment.setStudentId(studentId);
        if (installment.getInstallmentId() == null || installment.getInstallmentId().isBlank()) {
            installment.setInstallmentId(UUID.randomUUID().toString());
        }
        if (!feeInstallmentDao.update(installment)) {
            feeInstallmentDao.insert(installment);
        }
    }

    public static void deleteInstallment(String studentId, String installmentId) {
        feeInstallmentDao.delete(installmentId);
    }

    public static void markInstallmentReminderSent(String studentId, String installmentId) {
        List<FeeInstallment> schedule = feeInstallmentDao.findByStudent(studentId);
        schedule.stream()
                .filter(inst -> inst.getInstallmentId().equals(installmentId))
                .findFirst()
                .ifPresent(inst -> {
                    inst.setLastReminderSent(LocalDate.now());
                    feeInstallmentDao.update(inst);
                });
    }

    public static FeeInstallment nextDueInstallment(String studentId) {
        return feeInstallmentDao.findByStudent(studentId).stream()
                .filter(inst -> inst.getStatus() != FeeInstallment.Status.PAID)
                .sorted(Comparator.comparing(FeeInstallment::getDueDate))
                .findFirst()
                .map(DatabaseUtil::cloneInstallment)
                .orElse(null);
    }

    private static PaymentTransaction copyTransaction(PaymentTransaction source) {
        return new PaymentTransaction(
                source.getTransactionId(),
                source.getStudentId(),
                source.getAmount(),
                source.getPaidOn(),
                source.getMethod(),
                source.getReference(),
                source.getNotes()
        );
    }

    private static FeeInstallment cloneInstallment(FeeInstallment source) {
        return FeeInstallment.copyOf(source);
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
        coursePrerequisiteCache.remove(course.getCourseId());
    }

    public static void updateCourse(Course course) {
        courseDao.update(course);
        courses.put(course.getCourseId(), course);
        coursePrerequisiteCache.remove(course.getCourseId());
    }

    public static void deleteCourse(String courseId) {
        courseDao.delete(courseId);
        courses.remove(courseId);
        coursePrerequisiteCache.remove(courseId);
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
        enrollmentDao.deleteBySection(sectionId);
        waitlistDao.deleteAll(sectionId);
        attendanceDao.deleteBySection(sectionId);
    }

    public static synchronized void assignInstructorToSection(String sectionId, String facultyId, String performedBy) {
        if (sectionId == null || sectionId.isBlank()) {
            throw new IllegalArgumentException("Section ID is required.");
        }
        if (facultyId == null || facultyId.isBlank()) {
            throw new IllegalArgumentException("Instructor ID is required.");
        }
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        Faculty instructor = getFaculty(facultyId);
        if (instructor == null) {
            throw new IllegalArgumentException("Instructor not found: " + facultyId);
        }
        if (facultyId.equals(section.getFacultyId())) {
            return;
        }
        section.setFacultyId(facultyId);
        updateSection(section);
        refreshSectionCache();

        String actor = performedBy == null || performedBy.isBlank() ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.SECTION_ASSIGNMENT,
                actor,
                String.format("Assigned %s to section %s", facultyId, sectionId));
    }

    // Enrollment operations
    public static List<EnrollmentRecord> getEnrollmentsForStudent(String studentId) {
        return enrollmentDao.findByStudent(studentId);
    }

    public static List<EnrollmentRecord> getEnrollmentsForSection(String sectionId) {
        return enrollmentDao.findBySection(sectionId);
    }

    public static synchronized EnrollmentRecord registerStudentToSection(String studentId, String sectionId) {
        return registerStudentToSection(null, studentId, sectionId);
    }

    public static synchronized EnrollmentRecord registerStudentToSection(String performedBy, String studentId, String sectionId) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found");
        }
        Student student = getStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found");
        }

        List<String> missingPrereqs = getMissingPrerequisites(studentId, section.getCourseId());
        if (!missingPrereqs.isEmpty()) {
            throw new IllegalStateException("Missing prerequisite(s): " + String.join(", ", missingPrereqs));
        }

        List<EnrollmentRecord> existing = enrollmentDao.findBySection(sectionId);
        boolean already = existing.stream()
                .anyMatch(rec -> rec.getStudentId().equals(studentId)
                        && rec.getStatus() != EnrollmentRecord.Status.DROPPED);
        if (already) {
            throw new IllegalStateException("Student already enrolled or waitlisted in this section");
        }

        if (hasScheduleConflict(studentId, section)) {
            throw new IllegalStateException("Schedule conflict detected with another section");
        }

        long enrolledCount = existing.stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .count();
        boolean hasSeat = enrolledCount < section.getCapacity();

        int courseCredits = getCourseCreditHours(section.getCourseId());
        if (hasSeat) {
            int currentCredits = calculateEnrolledCredits(studentId);
            if (currentCredits + courseCredits > MAX_TERM_CREDITS) {
                throw new IllegalStateException("Credit load would exceed the maximum of "
                        + MAX_TERM_CREDITS + " hours.");
            }
        }

        EnrollmentRecord record = new EnrollmentRecord(studentId, sectionId,
                hasSeat ? EnrollmentRecord.Status.ENROLLED : EnrollmentRecord.Status.WAITLISTED);
        enrollmentDao.insert(record);

        if (hasSeat) {
            Course course = getCourse(section.getCourseId());
            if (course != null) {
                course.setAvailableSeats(Math.max(0, course.getAvailableSeats() - 1));
                updateCourse(course);
            }
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "You are enrolled in " + section.getTitle() + " (" + section.getSectionId() + ").",
                    "Registration"));
            refreshStudentEnrollmentMetrics(studentId);
        } else {
            int position = waitlistDao.findWaitlist(sectionId).size() + 1;
            waitlistDao.insert(sectionId, studentId, position);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "Section " + section.getTitle() + " is full. You are #" + position + " on the waitlist.",
                    "Registration"));
        }

        refreshSectionCache();

        String actor = performedBy == null ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE, actor,
                String.format("Registered %s in %s (%s)", studentId, section.getTitle(), record.getStatus()));
        return record;
    }

    private static boolean hasScheduleConflict(String studentId, Section targetSection) {
        return enrollmentDao.findByStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(rec -> getSection(rec.getSectionId()))
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
        dropStudentFromSection(null, studentId, sectionId);
    }

    public static synchronized void dropStudentFromSection(String performedBy, String studentId, String sectionId) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found");
        }

        List<EnrollmentRecord> sectionEnrollments = enrollmentDao.findBySection(sectionId);
        EnrollmentRecord record = sectionEnrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId)
                        && rec.getStatus() != EnrollmentRecord.Status.DROPPED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Student not enrolled in the section"));

        EnrollmentRecord.Status previousStatus = record.getStatus();
        record.setStatus(EnrollmentRecord.Status.DROPPED);
        enrollmentDao.updateStatus(record);
        waitlistDao.delete(sectionId, studentId);

        String promotedStudent = null;
        if (previousStatus == EnrollmentRecord.Status.ENROLLED) {
            int courseCredits = getCourseCreditHours(section.getCourseId());
            List<String> waitlist = new ArrayList<>(waitlistDao.findWaitlist(sectionId));
            for (String candidate : waitlist) {
                int candidateCredits = calculateEnrolledCredits(candidate);
                if (candidateCredits + courseCredits <= MAX_TERM_CREDITS) {
                    promotedStudent = candidate;
                    waitlistDao.delete(sectionId, promotedStudent);
                    EnrollmentRecord promotedRecord = sectionEnrollments.stream()
                            .filter(rec -> rec.getStudentId().equals(promotedStudent))
                            .findFirst()
                            .orElse(null);
                    if (promotedRecord != null) {
                        promotedRecord.setStatus(EnrollmentRecord.Status.ENROLLED);
                        enrollmentDao.updateStatus(promotedRecord);
                    }
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            promotedStudent,
                            "Great news! A seat opened up in " + section.getTitle() + " and you are now enrolled.",
                            "Registration"));
                    break;
                } else {
                    waitlistDao.delete(sectionId, candidate);
                    EnrollmentRecord candidateRecord = sectionEnrollments.stream()
                            .filter(rec -> rec.getStudentId().equals(candidate))
                            .findFirst()
                            .orElse(null);
                    if (candidateRecord != null) {
                        candidateRecord.setStatus(EnrollmentRecord.Status.DROPPED);
                        enrollmentDao.updateStatus(candidateRecord);
                    }
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            candidate,
                            "A seat opened in " + section.getTitle()
                                    + " but your current credit load exceeds the limit (" + MAX_TERM_CREDITS + ").",
                            "Registration"));
                }
            }

            if (promotedStudent == null) {
                Course course = getCourse(section.getCourseId());
                if (course != null) {
                    course.setAvailableSeats(Math.min(course.getTotalSeats(), course.getAvailableSeats() + 1));
                    updateCourse(course);
                }
            }
        }

        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "You dropped " + section.getTitle() + " (" + section.getSectionId() + ").",
                "Registration"));

        refreshSectionCache();
        refreshStudentEnrollmentMetrics(studentId);
        if (promotedStudent != null) {
            refreshStudentEnrollmentMetrics(promotedStudent);
        }

        String actor = performedBy == null ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE, actor,
                String.format("Dropped %s from %s (promoted: %s)", studentId, section.getTitle(),
                        promotedStudent == null ? "none" : promotedStudent));
    }

    public static List<Section> getScheduleForStudent(String studentId) {
        return enrollmentDao.findByStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(rec -> getSection(rec.getSectionId()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Section::getDayOfWeek).thenComparing(Section::getStartTime))
                .collect(Collectors.toList());
    }

    // Attendance operations
    public static void recordAttendance(String sectionId, LocalDate date, Map<String, Boolean> attendance) {
        AttendanceRecord record = new AttendanceRecord(sectionId, date);
        record.getAttendanceByStudent().putAll(attendance);
        attendanceDao.deleteBySectionAndDate(sectionId, date);
        attendanceDao.insert(record);
    }

    public static List<AttendanceRecord> getAttendanceForSection(String sectionId) {
        return attendanceDao.findBySection(sectionId);
    }

    // Notification operations
    public static List<NotificationMessage> getNotifications(NotificationMessage.Audience audience, String targetId) {
        NotificationMessage.Audience resolvedAudience =
                audience == null ? NotificationMessage.Audience.ALL : audience;
        return notificationDao.findVisible(resolvedAudience, targetId);
    }

    public static List<NotificationMessage> getNotificationsForStudent(String studentId) {
        return getNotifications(NotificationMessage.Audience.STUDENT, studentId);
    }

    public static void addNotification(NotificationMessage notification) {
        notificationDao.insert(notification);
    }

    public static void markNotificationRead(long notificationId, boolean read) {
        notificationDao.markRead(notificationId, read);
    }

    public static Map<String, Long> getWaitlistCountsByCourse() {
        return sections.values().stream()
                .collect(Collectors.groupingBy(
                        Section::getCourseId,
                        Collectors.summingLong(sec -> sec.getWaitlistedStudentIds().size())
                ));
    }

    public static List<String> getCoursePrerequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return coursePrerequisiteCache.computeIfAbsent(courseId, coursePrerequisiteDao::findPrerequisites);
    }

    public static Set<String> getCompletedCourseIds(String studentId) {
        if (studentId == null) {
            return Collections.emptySet();
        }
        Set<String> completed = new HashSet<>();
        for (EnrollmentRecord record : getEnrollmentsForStudent(studentId)) {
            Section section = getSection(record.getSectionId());
            if (section == null) {
                continue;
            }
            if (record.getFinalGrade() >= PASSING_GRADE_THRESHOLD) {
                completed.add(section.getCourseId());
            }
        }
        return completed;
    }

    public static Set<String> getActiveCourseIds(String studentId) {
        if (studentId == null) {
            return Collections.emptySet();
        }
        Set<String> active = new HashSet<>();
        for (EnrollmentRecord record : getEnrollmentsForStudent(studentId)) {
            if (record.getStatus() == EnrollmentRecord.Status.ENROLLED
                    || record.getStatus() == EnrollmentRecord.Status.WAITLISTED) {
                Section section = getSection(record.getSectionId());
                if (section != null) {
                    active.add(section.getCourseId());
                }
            }
        }
        return active;
    }

    public static List<String> getMissingPrerequisites(String studentId, String courseId) {
        List<String> prereqs = getCoursePrerequisites(courseId);
        if (prereqs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> completed = getCompletedCourseIds(studentId);
        Set<String> active = getActiveCourseIds(studentId);
        List<String> missing = new ArrayList<>();
        for (String prereq : prereqs) {
            if (!completed.contains(prereq) && !active.contains(prereq)) {
                missing.add(prereq);
            }
        }
        return missing;
    }

    public static int getCourseCreditHours(String courseId) {
        Course course = getCourse(courseId);
        if (course == null || course.getCreditHours() <= 0) {
            return 3;
        }
        return course.getCreditHours();
    }

    private static int calculateEnrolledCredits(String studentId) {
        return enrollmentDao.findByStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .mapToInt(rec -> {
                    Section section = getSection(rec.getSectionId());
                    return section != null ? getCourseCreditHours(section.getCourseId()) : 0;
                })
                .sum();
    }

    public static int getMaxTermCredits() {
        return MAX_TERM_CREDITS;
    }

    private static void refreshStudentEnrollmentMetrics(String studentId) {
        if (studentId == null) {
            return;
        }
        Student student = getStudent(studentId);
        if (student == null) {
            return;
        }
        int creditsInProgress = calculateEnrolledCredits(studentId);
        student.setCreditsInProgress(creditsInProgress);
        updateStudent(student);
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
    public static String getSetting(String key) {
        return settings.get(key);
    }

    public static void setSetting(String key, String value) {
        settings.put(key, value);
        settingsDao.upsert(key, value);
    }

    public static boolean isMaintenanceMode() {
        return Boolean.parseBoolean(settings.getOrDefault("maintenance", "false"));
    }

    public static void setMaintenanceMode(boolean maintenanceOn) {
        String value = Boolean.toString(maintenanceOn);
        settings.put("maintenance", value);
        settingsDao.upsert("maintenance", value);
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.ALL,
                null,
                "Maintenance mode is now " + (maintenanceOn ? "ON" : "OFF") + ".",
                "System"));
        AuditLogService.log(AuditLogService.EventType.MAINTENANCE_TOGGLE,
                "system",
                "Maintenance mode set to " + maintenanceOn);
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
        for (Section section : sections.values()) {
            section.getEnrolledStudentIds().clear();
            section.getWaitlistedStudentIds().clear();
            for (EnrollmentRecord record : enrollmentDao.findBySection(section.getSectionId())) {
                if (record.getStatus() == EnrollmentRecord.Status.ENROLLED) {
                    section.getEnrolledStudentIds().add(record.getStudentId());
                } else if (record.getStatus() == EnrollmentRecord.Status.WAITLISTED) {
                    section.getWaitlistedStudentIds().add(record.getStudentId());
                }
            }
            List<String> waitlist = waitlistDao.findWaitlist(section.getSectionId());
            for (String studentCode : waitlist) {
                if (!section.getWaitlistedStudentIds().contains(studentCode)) {
                    section.getWaitlistedStudentIds().add(studentCode);
                }
            }
        }
    }
}




