
import main.java.config.ConfigLoader;
import main.java.data.AuthUserDao;
import main.java.data.dao.CourseDao;
import main.java.data.dao.AssessmentTemplateDao;
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
import main.java.data.dao.FeeScheduleTemplateDao;
import main.java.data.dao.MaintenanceWindowDao;
import main.java.data.dao.InstructorMessageDao;
import main.java.data.dao.RegistrationRequestDao;
import main.java.data.migration.LegacyDataMigrator;
import main.java.utils.PasswordPolicy;
import main.java.utils.AuditLogService;

import main.java.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Database utility facade exposing high-level operations backed by the DAO layer.
 */
public class DatabaseUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUtil.class);
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
    private static final AssessmentTemplateDao assessmentTemplateDao = new AssessmentTemplateDao();
    private static final PaymentTransactionDao paymentTransactionDao = new PaymentTransactionDao();
    private static final FeeInstallmentDao feeInstallmentDao = new FeeInstallmentDao();
    private static final RegistrationRequestDao registrationRequestDao = new RegistrationRequestDao();
    private static final InstructorMessageDao instructorMessageDao = new InstructorMessageDao();
    private static final FeeScheduleTemplateDao feeScheduleTemplateDao = new FeeScheduleTemplateDao();
    private static final MaintenanceWindowDao maintenanceWindowDao = new MaintenanceWindowDao();

    private static final Map<String, List<String>> coursePrerequisiteCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> courseCorequisiteCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> courseAntirequisiteCache = new ConcurrentHashMap<>();
    private static final double PASSING_GRADE_THRESHOLD = 40.0;
    private static final List<MaintenanceWindow> maintenanceWindowCache = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService maintenanceExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "maintenance-scheduler");
                thread.setDaemon(true);
                return thread;
            });
    private static final AtomicBoolean maintenanceSchedulerStarted = new AtomicBoolean(false);
    private static volatile long lastMaintenanceRefresh = 0L;
    private static final long MAINTENANCE_REFRESH_INTERVAL_MS = 30_000L;
    private static final String MAINTENANCE_ORIGIN_KEY = "maintenance_origin";
    private static final String MAINTENANCE_WINDOW_KEY = "maintenance_window_id";
    private static final DateTimeFormatter HUMAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

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

    public static double getPassingGradeThreshold() {
        return PASSING_GRADE_THRESHOLD;
    }

    private static void ensureSettingDefault(String key, String defaultValue) {
        if (settings.putIfAbsent(key, defaultValue) == null) {
            settingsDao.upsert(key, defaultValue);
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
        refreshMaintenanceWindowCache();
        evaluateMaintenanceSchedule();
        startMaintenanceScheduler();
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
        ensureSettingDefault("maintenance", "false");
        ensureSettingDefault(MAINTENANCE_ORIGIN_KEY, "manual");
        ensureSettingDefault(MAINTENANCE_WINDOW_KEY, "");

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
        waitlistDao.insert(sec2.getSectionId(), s2.getStudentId(), 1, true);

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

    public static synchronized void updateUserRole(String username, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        User user = requireUser(username);
        user.setRole(role.trim());
        authUserDao.updateRole(user);
    }

    public static synchronized void setUserActive(String username, boolean active) {
        User user = requireUser(username);
        if (user.isActive() == active) {
            return;
        }
        user.setActive(active);
        authUserDao.updateProfile(user);
    }

    public static synchronized void updateUserContact(String username, String fullName, String email) {
        User user = requireUser(username);
        user.setFullName(fullName);
        user.setEmail(email);
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

    public static List<FeeScheduleTemplateDao.TemplateRecord> getFeeScheduleTemplates(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(feeScheduleTemplateDao.findByCourse(courseCode));
    }

    public static FeeScheduleTemplateDao.TemplateRecord addFeeScheduleTemplate(String courseCode,
                                                                              String label,
                                                                              double amount,
                                                                              int offsetDays) {
        validateTemplateInput(courseCode, label, amount, offsetDays);
        return feeScheduleTemplateDao.insert(courseCode.trim(), label.trim(), amount, offsetDays);
    }

    public static void updateFeeScheduleTemplate(long templateId,
                                                 String courseCode,
                                                 String label,
                                                 double amount,
                                                 int offsetDays) {
        validateTemplateInput(courseCode, label, amount, offsetDays);
        feeScheduleTemplateDao.update(templateId, label.trim(), amount, offsetDays);
    }

    public static void deleteFeeScheduleTemplate(long templateId) {
        feeScheduleTemplateDao.delete(templateId);
    }

    public static synchronized void applyFeeTemplateToStudent(String courseCode, String studentId) {
        Student student = getStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        String targetCourse = (courseCode != null && !courseCode.isBlank())
                ? courseCode.trim()
                : student.getCourse();
        if (targetCourse == null || targetCourse.isBlank()) {
            throw new IllegalArgumentException("No course selected for template application.");
        }

        List<FeeScheduleTemplateDao.TemplateRecord> templates = feeScheduleTemplateDao.findByCourse(targetCourse);
        if (templates.isEmpty()) {
            throw new IllegalStateException("No templates configured for " + targetCourse);
        }

        double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
        if (outstanding <= 0.0) {
            throw new IllegalStateException("Student has no outstanding balance.");
        }

        // Remove un-paid installments so the template can take over.
        for (FeeInstallment installment : feeInstallmentDao.findByStudent(studentId)) {
            if (installment.getStatus() != FeeInstallment.Status.PAID) {
                feeInstallmentDao.delete(installment.getInstallmentId());
            }
        }

        LocalDate anchorDate = student.getAdmissionDate() != null ? student.getAdmissionDate() : LocalDate.now();
        double remaining = outstanding;
        for (FeeScheduleTemplateDao.TemplateRecord template : templates) {
            if (remaining <= 0) {
                break;
            }
            double amount = Math.min(template.amount(), remaining);
            if (amount <= 0) {
                continue;
            }
            LocalDate dueDate = anchorDate.plusDays(Math.max(0, template.offsetDays()));
            FeeInstallment installment = new FeeInstallment(
                    UUID.randomUUID().toString(),
                    studentId,
                    dueDate,
                    amount,
                    FeeInstallment.Status.DUE,
                    template.label(),
                    null,
                    null
            );
            feeInstallmentDao.insert(installment);
            remaining -= amount;
        }

        if (remaining > 0) {
            int lastOffset = templates.get(templates.size() - 1).offsetDays();
            LocalDate fallbackDue = anchorDate.plusDays(Math.max(30, lastOffset + 30));
            FeeInstallment balanceInstallment = new FeeInstallment(
                    UUID.randomUUID().toString(),
                    studentId,
                    fallbackDue,
                    remaining,
                    FeeInstallment.Status.DUE,
                    "Balance",
                    null,
                    null
            );
            feeInstallmentDao.insert(balanceInstallment);
        }
    }

    private static void validateTemplateInput(String courseCode,
                                              String label,
                                              double amount,
                                              int offsetDays) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Course is required.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Template label is required.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        if (offsetDays < 0) {
            throw new IllegalArgumentException("Offset days cannot be negative.");
        }
    }

    private static String serializeWeights(Map<String, Double> weights) {
        StringBuilder builder = new StringBuilder();
        weights.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(entry.getKey().replace("\n", " "))
                            .append('=')
                            .append(entry.getValue());
                });
        return builder.toString();
    }

    private static Map<String, Double> deserializeWeights(String payload) {
        Map<String, Double> weights = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) {
            return weights;
        }
        String[] lines = payload.split("\\n");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String component = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (component.isEmpty()) {
                continue;
            }
            try {
                weights.put(component, Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return weights;
    }

    // Gradebook template + moderation operations
    public static List<AssessmentTemplateDao.AssessmentTemplate> getAssessmentTemplates(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return Collections.emptyList();
        }
        return assessmentTemplateDao.findByCourse(courseCode.trim());
    }

    public static AssessmentTemplateDao.AssessmentTemplate createAssessmentTemplate(String courseCode,
                                                                                     String templateName,
                                                                                     Map<String, Double> weights,
                                                                                     String createdBy) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("Template name is required.");
        }
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one assessment component.");
        }
        String payload = serializeWeights(weights);
        return assessmentTemplateDao.insert(courseCode.trim(), templateName.trim(), payload,
                createdBy == null ? "system" : createdBy);
    }

    public static void deleteAssessmentTemplate(long templateId) {
        assessmentTemplateDao.delete(templateId);
    }

    public static void applyAssessmentTemplate(long templateId, String sectionId) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        AssessmentTemplateDao.AssessmentTemplate template = assessmentTemplateDao.findById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        if (!section.getCourseId().equalsIgnoreCase(template.courseCode())) {
            throw new IllegalArgumentException("Template course mismatch. Expected " + section.getCourseId());
        }
        Map<String, Double> weights = deserializeWeights(template.weightsJson());
        if (weights.isEmpty()) {
            throw new IllegalStateException("Template has no assessments.");
        }
        section.clearAssessmentWeights();
        weights.forEach(section::setAssessmentWeight);
        updateSection(section);
    }

    public static Section.GradebookState getGradebookState(String sectionId) {
        Section section = getSection(sectionId);
        return section == null ? Section.GradebookState.DRAFT : section.getGradebookState();
    }

    public static void updateGradebookState(String sectionId, Section.GradebookState state) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        section.setGradebookState(state);
        updateSection(section);
    }

    public static Map<String, String> getComponentFeedback(String sectionId, String studentId) {
        EnrollmentRecord record = findEnrollmentRecord(sectionId, studentId);
        return record.getComponentFeedback();
    }

    public static void saveComponentFeedback(String sectionId, String studentId, Map<String, String> feedback) {
        EnrollmentRecord record = findEnrollmentRecord(sectionId, studentId);
        record.setComponentFeedback(feedback);
    }

    public static void saveComponentFeedbackEntry(String sectionId, String studentId, String component, String comment) {
        EnrollmentRecord record = findEnrollmentRecord(sectionId, studentId);
        record.putFeedback(component, comment);
    }

    public static List<InstructorMessageDao.MessageLog> getInstructorMessageLog(String instructorUsername) {
        if (instructorUsername == null || instructorUsername.isBlank()) {
            return Collections.emptyList();
        }
        return instructorMessageDao.findByInstructor(instructorUsername.trim());
    }

    public static void sendInstructorMessage(User instructor,
                                             String sectionId,
                                             List<String> requestedRecipients,
                                             String subject,
                                             String body) {
        if (instructor == null || !"Instructor".equalsIgnoreCase(instructor.getRole())) {
            throw new SecurityException("Instructor session required.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message body is required.");
        }
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        Faculty faculty = findFacultyByUsername(instructor.getUsername());
        if (faculty == null || !Objects.equals(faculty.getFacultyId(), section.getFacultyId())) {
            throw new SecurityException("You are not assigned to this section.");
        }
        List<String> recipients = resolveSectionRecipients(sectionId, requestedRecipients);
        if (recipients.isEmpty()) {
            throw new IllegalStateException("No enrolled students to message.");
        }
        String header = "[" + sectionId + "] " + subject.trim();
        String bodyText = body.trim();
        for (String studentId : recipients) {
            NotificationMessage notification = new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    header + "\n" + bodyText,
                    "Instructor Message");
            addNotification(notification);
        }
        instructorMessageDao.insert(
                instructor.getUsername(),
                sectionId,
                subject.trim(),
                bodyText,
                String.join(",", recipients));
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
        clearCourseRelationshipCaches(course.getCourseId());
    }

    public static void updateCourse(Course course) {
        courseDao.update(course);
        courses.put(course.getCourseId(), course);
        clearCourseRelationshipCaches(course.getCourseId());
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

    public static List<SectionConflict> findSectionConflicts() {
        List<SectionConflict> conflicts = new ArrayList<>();
        List<Section> allSections = new ArrayList<>(getAllSections());
        for (int i = 0; i < allSections.size(); i++) {
            Section a = allSections.get(i);
            if (a.getDayOfWeek() == null || a.getStartTime() == null || a.getEndTime() == null) {
                continue;
            }
            for (int j = i + 1; j < allSections.size(); j++) {
                Section b = allSections.get(j);
                if (b.getDayOfWeek() == null || b.getStartTime() == null || b.getEndTime() == null) {
                    continue;
                }
                if (!a.getDayOfWeek().equals(b.getDayOfWeek())) {
                    continue;
                }
                if (!overlaps(a, b)) {
                    continue;
                }
                if (a.getLocation() != null && b.getLocation() != null
                        && a.getLocation().equalsIgnoreCase(b.getLocation())) {
                    String detail = String.format("%s %s-%s @ %s",
                            a.getDayOfWeek(), a.getStartTime(), a.getEndTime(), a.getLocation());
                    conflicts.add(new SectionConflict(SectionConflict.Type.ROOM,
                            a.getSectionId(), b.getSectionId(), detail));
                }
                if (a.getFacultyId() != null && b.getFacultyId() != null
                        && a.getFacultyId().equalsIgnoreCase(b.getFacultyId())) {
                    String detail = String.format("Faculty %s %s %s-%s",
                            a.getFacultyId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime());
                    conflicts.add(new SectionConflict(SectionConflict.Type.FACULTY,
                            a.getSectionId(), b.getSectionId(), detail));
                }
            }
        }
        return conflicts;
    }

    public static List<CapacityWarning> findCapacityWarnings() {
        List<CapacityWarning> warnings = new ArrayList<>();
        for (Section section : getAllSections()) {
            int enrolled = enrollmentDao.findBySection(section.getSectionId()).stream()
                    .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                    .mapToInt(rec -> 1)
                    .sum();
            if (enrolled > section.getCapacity()) {
                warnings.add(new CapacityWarning(section.getSectionId(), section.getCapacity(), enrolled));
            }
        }
        return warnings;
    }

    public static void addSection(Section section) {
        enforceRoomScheduleClash(section);
        sectionDao.insert(section);
        sections.put(section.getSectionId(), section);
    }

    public static void updateSection(Section section) {
        enforceRoomScheduleClash(section);
        sectionDao.update(section);
        sections.put(section.getSectionId(), section);
    }

    public static void updateSectionDeadlines(String sectionId, LocalDate enrollmentDeadline, LocalDate dropDeadline) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        boolean changed = false;
        if (enrollmentDeadline != null) {
            section.setEnrollmentDeadline(enrollmentDeadline);
            changed = true;
        }
        if (dropDeadline != null) {
            section.setDropDeadline(dropDeadline);
            changed = true;
        }
        if (changed) {
            updateSection(section);
        }
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

    public static synchronized EnrollmentRecord registerStudentToSection(User actor, String studentId, String sectionId) {
        String performedBy = actor == null ? null : actor.getUsername();
        boolean autoApproved = actor != null && !"Student".equalsIgnoreCase(actor.getRole());
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

        List<String> missingCoreqs = getMissingCorequisites(studentId, section.getCourseId());
        if (!missingCoreqs.isEmpty()) {
            throw new IllegalStateException("Missing co-requisite(s): " + String.join(", ", missingCoreqs));
        }

        List<String> conflicts = getAntirequisiteConflicts(studentId, section.getCourseId());
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Conflicts with anti-requisite(s): " + String.join(", ", conflicts));
        }

        if (section.isRequiresAdvisorApproval()
                && (actor == null || "Student".equalsIgnoreCase(actor.getRole()))) {
            submitRegistrationRequest(actor, studentId, sectionId);
            throw new IllegalStateException("Registration submitted for advisor approval.");
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
            int position = waitlistDao.findEntries(sectionId).size() + 1;
            waitlistDao.insert(sectionId, studentId, position, autoApproved);
            String approvalText = autoApproved
                    ? "You are approved and will be auto-enrolled when a seat opens."
                    : "Advisor approval is required before you can be auto-enrolled.";
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "Section " + section.getTitle() + " is full. You are #"
                            + position + " on the waitlist. " + approvalText,
                    "Registration"));
        }

        refreshSectionCache();

        String actorName = performedBy == null ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE, actorName,
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

    private static void enforceRoomScheduleClash(Section candidate) {
        if (candidate.getLocation() == null || candidate.getLocation().isBlank()) {
            return;
        }
        for (Section existing : sections.values()) {
            if (existing.getSectionId().equalsIgnoreCase(candidate.getSectionId())) {
                continue;
            }
            if (roomsConflict(existing, candidate)) {
                throw new IllegalStateException(String.format(
                        "Room %s is already in use by %s (%s %s-%s).",
                        existing.getLocation(),
                        existing.getSectionId(),
                        existing.getDayOfWeek(),
                        existing.getStartTime(),
                        existing.getEndTime()));
            }
        }
    }

    private static boolean roomsConflict(Section a, Section b) {
        if (a.getLocation() == null || b.getLocation() == null) {
            return false;
        }
        if (!a.getLocation().equalsIgnoreCase(b.getLocation())) {
            return false;
        }
        return overlaps(a, b);
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
            boolean promoted = promoteApprovedWaitlistedIfPossible(section, sectionEnrollments);
            if (!promoted) {
                Course course = getCourse(section.getCourseId());
                if (course != null) {
                    course.setAvailableSeats(Math.min(course.getTotalSeats(), course.getAvailableSeats() + 1));
                    updateCourse(course);
                }
            } else {
                promotedStudent = "auto";
            }
        }

        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "You dropped " + section.getTitle() + " (" + section.getSectionId() + ").",
                "Registration"));

        refreshSectionCache();
        refreshStudentEnrollmentMetrics(studentId);
        refreshSectionCache();
        refreshStudentEnrollmentMetrics(studentId);
        String actor = performedBy == null ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE, actor,
                String.format("Dropped %s from %s", studentId, section.getTitle()));
    }

    public static synchronized EnrollmentRecord overrideEnrollStudent(User admin,
                                                                      String studentId,
                                                                      String sectionId,
                                                                      boolean ignoreCapacity,
                                                                      boolean ignoreConflicts,
                                                                      boolean ignoreRequisites) {
        ensureAdmin(admin);
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        Student student = getStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        if (!ignoreRequisites) {
            List<String> missingPrereqs = getMissingPrerequisites(studentId, section.getCourseId());
            if (!missingPrereqs.isEmpty()) {
                throw new IllegalStateException("Missing prerequisite(s): " + String.join(", ", missingPrereqs));
            }
            List<String> missingCoreqs = getMissingCorequisites(studentId, section.getCourseId());
            if (!missingCoreqs.isEmpty()) {
                throw new IllegalStateException("Missing co-requisite(s): " + String.join(", ", missingCoreqs));
            }
            List<String> conflicts = getAntirequisiteConflicts(studentId, section.getCourseId());
            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("Conflicts with anti-requisite(s): " + String.join(", ", conflicts));
            }
        }
        if (!ignoreConflicts && hasScheduleConflict(studentId, section)) {
            throw new IllegalStateException("Schedule conflict detected. Enable overrides to bypass.");
        }
        List<EnrollmentRecord> existing = enrollmentDao.findBySection(sectionId);
        boolean already = existing.stream()
                .anyMatch(rec -> rec.getStudentId().equals(studentId)
                        && rec.getStatus() != EnrollmentRecord.Status.DROPPED);
        if (already) {
            throw new IllegalStateException("Student already enrolled or waitlisted for this section.");
        }
        long enrolledCount = existing.stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .count();
        boolean seatAvailable = enrolledCount < section.getCapacity();
        boolean enrollNow = seatAvailable || ignoreCapacity;
        EnrollmentRecord.Status status = enrollNow ? EnrollmentRecord.Status.ENROLLED : EnrollmentRecord.Status.WAITLISTED;
        EnrollmentRecord record = new EnrollmentRecord(studentId, sectionId, status);
        enrollmentDao.insert(record);

        if (status == EnrollmentRecord.Status.ENROLLED) {
            Course course = getCourse(section.getCourseId());
            if (course != null) {
                course.setAvailableSeats(Math.max(0, course.getAvailableSeats() - 1));
                updateCourse(course);
            }
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "You were force-enrolled into " + section.getTitle() + " (" + section.getSectionId() + ").",
                    "Registration"));
            refreshStudentEnrollmentMetrics(studentId);
        } else {
            int position = waitlistDao.findEntries(sectionId).size() + 1;
            waitlistDao.insert(sectionId, studentId, position, true);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "You were added to the waitlist for " + section.getTitle() + " (" + section.getSectionId() + ").",
                    "Registration"));
        }

        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                admin.getUsername(),
                String.format("Override enroll %s into %s (status %s)", studentId, sectionId, status));
        return record;
    }

    private static boolean promoteApprovedWaitlistedIfPossible(Section section, List<EnrollmentRecord> sectionEnrollments) {
        List<WaitlistDao.WaitlistEntry> waitlist = waitlistDao.findEntries(section.getSectionId());
        if (waitlist.isEmpty()) {
            return false;
        }
        long enrolledCount = sectionEnrollments.stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .count();
        if (enrolledCount >= section.getCapacity()) {
            return false;
        }
        int courseCredits = getCourseCreditHours(section.getCourseId());
        for (WaitlistDao.WaitlistEntry entry : waitlist) {
            if (!entry.advisorApproved()) {
                continue;
            }
            String candidate = entry.studentCode();
            int candidateCredits = calculateEnrolledCredits(candidate);
            if (candidateCredits + courseCredits <= MAX_TERM_CREDITS) {
                waitlistDao.delete(section.getSectionId(), candidate);
                EnrollmentRecord promotedRecord = sectionEnrollments.stream()
                        .filter(rec -> rec.getStudentId().equals(candidate))
                        .findFirst()
                        .orElse(null);
                if (promotedRecord != null) {
                    promotedRecord.setStatus(EnrollmentRecord.Status.ENROLLED);
                    enrollmentDao.updateStatus(promotedRecord);
                }
                addNotification(new NotificationMessage(
                        NotificationMessage.Audience.STUDENT,
                        candidate,
                        "Great news! A seat opened up in " + section.getTitle() + " and you are now enrolled.",
                        "Registration"));
                refreshStudentEnrollmentMetrics(candidate);
                AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                        "system",
                        String.format("Auto-promoted %s into %s from waitlist", candidate, section.getSectionId()));
                return true;
            } else {
                waitlistDao.delete(section.getSectionId(), candidate);
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
        return false;
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
    public static void recordAttendance(String sectionId,
                                        LocalDate date,
                                        Map<String, AttendanceRecord.AttendanceStatus> attendance) {
        AttendanceRecord record = new AttendanceRecord(sectionId, date);
        attendance.forEach((studentId, status) -> record.markStatus(
                studentId,
                status != null ? status : AttendanceRecord.AttendanceStatus.ABSENT
        ));
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

    public static List<WaitlistDao.WaitlistEntry> getWaitlistEntries(String sectionId) {
        return waitlistDao.findEntries(sectionId);
    }

    public static List<WaitlistSnapshot> getWaitlistSnapshot(String sectionId) {
        List<WaitlistSnapshot> snapshot = new ArrayList<>();
        for (WaitlistDao.WaitlistEntry entry : waitlistDao.findEntries(sectionId)) {
            Student student = getStudent(entry.studentCode());
            String name = student != null ? student.getFullName() : entry.studentCode();
            snapshot.add(new WaitlistSnapshot(entry.studentCode(), name, entry.position(), entry.advisorApproved()));
        }
        return snapshot;
    }

    public static boolean isWaitlistApproved(String studentId, String sectionId) {
        return waitlistDao.findEntries(sectionId).stream()
                .filter(entry -> entry.studentCode().equals(studentId))
                .map(WaitlistDao.WaitlistEntry::advisorApproved)
                .findFirst()
                .orElse(false);
    }

    public static void setWaitlistApproval(User actor, String sectionId, String studentId, boolean approved) {
        if (actor == null || !"Admin".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Administrator privileges required.");
        }
        waitlistDao.updateApproval(sectionId, studentId, approved);
        Section section = getSection(sectionId);
        if (approved && section != null) {
            List<EnrollmentRecord> enrollments = enrollmentDao.findBySection(sectionId);
            boolean promoted = promoteApprovedWaitlistedIfPossible(section, enrollments);
            if (promoted) {
                Course course = getCourse(section.getCourseId());
                if (course != null) {
                    course.setAvailableSeats(Math.max(0, course.getAvailableSeats() - 1));
                    updateCourse(course);
                }
            }
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                    "Advisor approval granted for " + section.getTitle() + ". You'll be enrolled automatically when a seat opens.",
                    "Registration"));
        } else if (!approved && section != null) {
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    studentId,
                "Advisor approval revoked for " + section.getTitle() + ". Contact advising for details.",
                "Registration"));
        }
        refreshSectionCache();
    }

    public record WaitlistSnapshot(String studentId, String studentName, int position, boolean approved) {
    }

    public static synchronized void promoteWaitlistedStudent(User admin, String sectionId, String studentId) {
        ensureAdmin(admin);
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        List<EnrollmentRecord> enrollments = enrollmentDao.findBySection(sectionId);
        EnrollmentRecord record = enrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Student not on waitlist."));
        record.setStatus(EnrollmentRecord.Status.ENROLLED);
        enrollmentDao.updateStatus(record);
        waitlistDao.delete(sectionId, studentId);
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "You were promoted from the waitlist to " + sectionId + ".",
                "Registration"));
        refreshStudentEnrollmentMetrics(studentId);
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                admin.getUsername(),
                "Promoted " + studentId + " into " + sectionId);
    }

    public static synchronized void removeWaitlistEntry(User admin, String sectionId, String studentId) {
        ensureAdmin(admin);
        waitlistDao.delete(sectionId, studentId);
        enrollmentDao.findBySection(sectionId).stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .findFirst()
                .ifPresent(rec -> {
                    rec.setStatus(EnrollmentRecord.Status.DROPPED);
                    enrollmentDao.updateStatus(rec);
                });
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                admin.getUsername(),
                "Removed " + studentId + " from waitlist for " + sectionId);
    }

    // Registration request operations
    public static void submitRegistrationRequest(User actor, String studentId, String sectionId) {
        registrationRequestDao.findByStudentSection(studentId, sectionId).ifPresent(existing -> {
            if ("PENDING".equalsIgnoreCase(existing.status())) {
                throw new IllegalStateException("Registration request already pending advisor approval.");
            }
            if ("APPROVED".equalsIgnoreCase(existing.status())) {
                throw new IllegalStateException("Registration request already approved.");
            }
        });
        String requestedBy = actor != null ? actor.getUsername() : "student";
        registrationRequestDao.insert(studentId, sectionId, requestedBy);
        Section section = getSection(sectionId);
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "Registration request submitted for " + (section != null ? section.getTitle() : sectionId) + ".",
                "Registration"));
    }

    public static List<RegistrationRequestView> getPendingRegistrationRequests() {
        List<RegistrationRequestView> views = new ArrayList<>();
        for (RegistrationRequestDao.RequestRecord record : registrationRequestDao.findPending()) {
            Student student = getStudent(record.studentCode());
            Section section = getSection(record.sectionCode());
            views.add(new RegistrationRequestView(
                    record.id(),
                    record.studentCode(),
                    student != null ? student.getFullName() : record.studentCode(),
                    record.sectionCode(),
                    section != null ? section.getTitle() : record.sectionCode(),
                    record.requestedBy(),
                    record.createdAt()));
        }
        return views;
    }

    public static void approveRegistrationRequest(User admin, long requestId, String notes) {
        ensureAdmin(admin);
        RegistrationRequestDao.RequestRecord record = registrationRequestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(record.status())) {
            throw new IllegalStateException("Request already processed.");
        }
        try {
            registerStudentToSection(admin, record.studentCode(), record.sectionCode());
            registrationRequestDao.updateStatus(requestId, "APPROVED", admin.getUsername(), notes);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    record.studentCode(),
                    "Advisor approved your registration for " + record.sectionCode() + ".",
                    "Registration"));
        } catch (RuntimeException ex) {
            registrationRequestDao.updateStatus(requestId, "REJECTED", admin.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    public static void rejectRegistrationRequest(User admin, long requestId, String notes) {
        ensureAdmin(admin);
        RegistrationRequestDao.RequestRecord record = registrationRequestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(record.status())) {
            throw new IllegalStateException("Request already processed.");
        }
        registrationRequestDao.updateStatus(requestId, "REJECTED", admin.getUsername(), notes);
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                record.studentCode(),
                "Advisor rejected your registration for " + record.sectionCode() + ". " + (notes == null ? "" : notes),
                "Registration"));
    }

    private static void ensureAdmin(User actor) {
        if (actor == null || !"Admin".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Administrator privileges required.");
        }
    }

    public record RegistrationRequestView(long id,
                                          String studentId,
                                          String studentName,
                                          String sectionId,
                                          String sectionTitle,
                                          String requestedBy,
                                          java.time.Instant requestedAt) {
    }

    public static List<String> getCoursePrerequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return coursePrerequisiteCache.computeIfAbsent(courseId, coursePrerequisiteDao::findPrerequisites);
    }

    public static List<String> getCourseCorequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseCorequisiteCache.computeIfAbsent(courseId, courseRelationshipDao::findCorequisites);
    }

    public static List<String> getCourseAntirequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseAntirequisiteCache.computeIfAbsent(courseId, courseRelationshipDao::findAntirequisites);
    }

    public static void updateCoursePrerequisites(String courseId, List<String> prerequisites) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        coursePrerequisiteDao.replacePrerequisites(courseId.trim(), normalizeCourseList(prerequisites));
        coursePrerequisiteCache.remove(courseId);
    }

    public static void updateCourseCorequisites(String courseId, List<String> coreqs) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        courseRelationshipDao.replaceCorequisites(courseId.trim(), normalizeCourseList(coreqs));
        courseCorequisiteCache.remove(courseId);
    }

    public static void updateCourseAntirequisites(String courseId, List<String> antireqs) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        courseRelationshipDao.replaceAntirequisites(courseId.trim(), normalizeCourseList(antireqs));
        courseAntirequisiteCache.remove(courseId);
    }

    private static List<String> normalizeCourseList(List<String> courses) {
        if (courses == null) {
            return Collections.emptyList();
        }
        return courses.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
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

    public static List<String> getMissingCorequisites(String studentId, String courseId) {
        List<String> coreqs = getCourseCorequisites(courseId);
        if (coreqs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> completed = getCompletedCourseIds(studentId);
        Set<String> active = getActiveCourseIds(studentId);
        List<String> missing = new ArrayList<>();
        for (String coreq : coreqs) {
            if (!completed.contains(coreq) && !active.contains(coreq)) {
                missing.add(coreq);
            }
        }
        return missing;
    }

    public static List<String> getAntirequisiteConflicts(String studentId, String courseId) {
        List<String> antireqs = getCourseAntirequisites(courseId);
        if (antireqs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> completed = getCompletedCourseIds(studentId);
        Set<String> active = getActiveCourseIds(studentId);
        List<String> conflicts = new ArrayList<>();
        for (String antireq : antireqs) {
            if (completed.contains(antireq) || active.contains(antireq)) {
                conflicts.add(antireq);
            }
        }
        return conflicts;
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
        evaluateMaintenanceSchedule();
        return Boolean.parseBoolean(settings.getOrDefault("maintenance", "false"));
    }

    public static void setMaintenanceMode(boolean maintenanceOn) {
        handleMaintenanceToggle(maintenanceOn, "manual", null, null, "system");
    }

    public static void setMaintenanceMode(User actor, boolean maintenanceOn) {
        String auditActor = actor != null ? actor.getUsername() : "system";
        handleMaintenanceToggle(maintenanceOn, "manual", null, null, auditActor);
    }

    public static List<MaintenanceWindow> getMaintenanceWindows() {
        refreshMaintenanceWindowCacheIfStale();
        List<MaintenanceWindow> copy = new ArrayList<>(maintenanceWindowCache);
        copy.sort(Comparator.comparing(MaintenanceWindow::getStartAt));
        return Collections.unmodifiableList(copy);
    }

    public static Optional<MaintenanceWindow> getNextMaintenanceWindow() {
        refreshMaintenanceWindowCacheIfStale();
        LocalDateTime now = LocalDateTime.now();
        return maintenanceWindowCache.stream()
                .filter(window -> window.getStatus() != MaintenanceWindow.Status.CANCELLED
                        && window.getStatus() != MaintenanceWindow.Status.COMPLETED)
                .filter(window -> window.isActive(now) || window.getStartAt().isAfter(now))
                .min(Comparator.comparing(MaintenanceWindow::getStartAt));
    }

    public static MaintenanceWindow scheduleMaintenanceWindow(User actor,
                                                              LocalDateTime start,
                                                              LocalDateTime end,
                                                              String message) {
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        LocalDateTime now = LocalDateTime.now();
        MaintenanceWindow.Status status = now.isBefore(start)
                ? MaintenanceWindow.Status.SCHEDULED
                : MaintenanceWindow.Status.ACTIVE;
        String safeMessage = (message == null || message.isBlank())
                ? "Scheduled infrastructure maintenance"
                : message.trim();

        MaintenanceWindow window = maintenanceWindowDao.insert(start, end, safeMessage, status, actor.getUsername())
                .orElseThrow(() -> new IllegalStateException("Unable to persist maintenance window"));

        maintenanceWindowCache.add(window);
        maintenanceWindowCache.sort(Comparator.comparing(MaintenanceWindow::getStartAt));
        lastMaintenanceRefresh = System.currentTimeMillis();

        if (status == MaintenanceWindow.Status.SCHEDULED) {
            String body = String.format(
                    "Maintenance scheduled on %s for %d minutes. %s",
                    HUMAN_DATE_FORMAT.format(start),
                    Duration.between(start, end).toMinutes(),
                    safeMessage);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.ALL,
                    null,
                    body,
                    "Maintenance"));
            AuditLogService.log(AuditLogService.EventType.MAINTENANCE_TOGGLE,
                    actor.getUsername(),
                    "Scheduled maintenance window #" + window.getId() + " " + describeWindow(window));
        } else {
            announceWindowStart(window);
        }

        evaluateMaintenanceSchedule();
        return window;
    }

    public static void cancelMaintenanceWindow(User actor, long windowId) {
        Objects.requireNonNull(actor, "actor is required");
        refreshMaintenanceWindowCacheIfStale();
        MaintenanceWindow window = maintenanceWindowCache.stream()
                .filter(w -> w.getId() == windowId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Maintenance window not found"));
        if (window.getStatus() == MaintenanceWindow.Status.COMPLETED
                || window.getStatus() == MaintenanceWindow.Status.CANCELLED) {
            throw new IllegalStateException("Window has already finished.");
        }
        maintenanceWindowDao.updateStatus(windowId, MaintenanceWindow.Status.CANCELLED);
        replaceWindowInCache(window.withStatus(MaintenanceWindow.Status.CANCELLED));
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.ALL,
                null,
                String.format("Maintenance window starting %s was cancelled by %s.",
                        HUMAN_DATE_FORMAT.format(window.getStartAt()),
                        actor.getFullName()),
                "Maintenance"));
        AuditLogService.log(AuditLogService.EventType.MAINTENANCE_TOGGLE,
                actor.getUsername(),
                "Cancelled maintenance window #" + windowId);
        evaluateMaintenanceSchedule();
    }

    private static void startMaintenanceScheduler() {
        if (maintenanceSchedulerStarted.compareAndSet(false, true)) {
            maintenanceExecutor.scheduleAtFixedRate(() -> {
                try {
                    refreshMaintenanceWindowCacheIfStale();
                    evaluateMaintenanceSchedule();
                } catch (Exception ex) {
                    LOGGER.error("Maintenance scheduler tick failed: {}", ex.getMessage(), ex);
                }
            }, 30, 30, TimeUnit.SECONDS);
        }
    }

    private static synchronized void refreshMaintenanceWindowCache() {
        List<MaintenanceWindow> windows = maintenanceWindowDao.findAll();
        windows.sort(Comparator.comparing(MaintenanceWindow::getStartAt));
        maintenanceWindowCache.clear();
        maintenanceWindowCache.addAll(windows);
        lastMaintenanceRefresh = System.currentTimeMillis();
    }

    private static void refreshMaintenanceWindowCacheIfStale() {
        if (System.currentTimeMillis() - lastMaintenanceRefresh > MAINTENANCE_REFRESH_INTERVAL_MS) {
            refreshMaintenanceWindowCache();
        }
    }

    private static void replaceWindowInCache(MaintenanceWindow updated) {
        for (int i = 0; i < maintenanceWindowCache.size(); i++) {
            if (maintenanceWindowCache.get(i).getId() == updated.getId()) {
                maintenanceWindowCache.set(i, updated);
                return;
            }
        }
        maintenanceWindowCache.add(updated);
        maintenanceWindowCache.sort(Comparator.comparing(MaintenanceWindow::getStartAt));
    }

    private static void evaluateMaintenanceSchedule() {
        refreshMaintenanceWindowCacheIfStale();
        LocalDateTime now = LocalDateTime.now();
        boolean hasActive = false;
        MaintenanceWindow activeWindow = null;

        for (MaintenanceWindow window : new ArrayList<>(maintenanceWindowCache)) {
            if (window.getStatus() == MaintenanceWindow.Status.CANCELLED
                    || window.getStatus() == MaintenanceWindow.Status.COMPLETED) {
                continue;
            }
            if (window.getStatus() == MaintenanceWindow.Status.SCHEDULED && !now.isBefore(window.getStartAt())) {
                maintenanceWindowDao.updateStatus(window.getId(), MaintenanceWindow.Status.ACTIVE);
                window = window.withStatus(MaintenanceWindow.Status.ACTIVE);
                replaceWindowInCache(window);
                announceWindowStart(window);
                hasActive = true;
                activeWindow = window;
                continue;
            }
            if (window.getStatus() == MaintenanceWindow.Status.ACTIVE) {
                if (now.isAfter(window.getEndAt())) {
                    maintenanceWindowDao.updateStatus(window.getId(), MaintenanceWindow.Status.COMPLETED);
                    replaceWindowInCache(window.withStatus(MaintenanceWindow.Status.COMPLETED));
                    announceWindowEnd(window);
                } else {
                    hasActive = true;
                    activeWindow = window;
                }
            }
        }

        if (!hasActive) {
            handleScheduledCompletionFallback();
        } else if (activeWindow != null) {
            String trackedWindowId = settings.getOrDefault(MAINTENANCE_WINDOW_KEY, "");
            if (!trackedWindowId.equals(Long.toString(activeWindow.getId()))) {
                setMaintenanceModeForWindow(true, activeWindow,
                        activeWindow.getMessage() + " window active until "
                                + HUMAN_DATE_FORMAT.format(activeWindow.getEndAt()) + ".");
            }
        }
    }

    private static void handleScheduledCompletionFallback() {
        boolean maintenanceOn = Boolean.parseBoolean(settings.getOrDefault("maintenance", "false"));
        boolean controlledBySchedule = "scheduled".equals(settings.getOrDefault(MAINTENANCE_ORIGIN_KEY, "manual"));
        if (maintenanceOn && controlledBySchedule) {
            handleMaintenanceToggle(false, "scheduled", null,
                    "Scheduled maintenance window complete. Services restored.",
                    "system");
        }
    }

    private static void announceWindowStart(MaintenanceWindow window) {
        String summary = window.getMessage() + " window active until "
                + HUMAN_DATE_FORMAT.format(window.getEndAt()) + ".";
        setMaintenanceModeForWindow(true, window, summary);
    }

    private static void announceWindowEnd(MaintenanceWindow window) {
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.ALL,
                null,
                String.format("Maintenance window from %s to %s has concluded.",
                        HUMAN_DATE_FORMAT.format(window.getStartAt()),
                        HUMAN_DATE_FORMAT.format(window.getEndAt())),
                "Maintenance"));
        String trackedWindowId = settings.getOrDefault(MAINTENANCE_WINDOW_KEY, "");
        if (trackedWindowId.equals(Long.toString(window.getId()))) {
            setMaintenanceModeForWindow(false, window,
                    "Scheduled maintenance window complete. Services restored.");
        }
    }

    private static String describeWindow(MaintenanceWindow window) {
        return "from " + HUMAN_DATE_FORMAT.format(window.getStartAt()) +
                " to " + HUMAN_DATE_FORMAT.format(window.getEndAt());
    }

    private static void setMaintenanceModeForWindow(boolean maintenanceOn,
                                                    MaintenanceWindow window,
                                                    String message) {
        handleMaintenanceToggle(maintenanceOn, "scheduled", window, message, "system");
    }

    private static void handleMaintenanceToggle(boolean maintenanceOn,
                                                String origin,
                                                MaintenanceWindow windowContext,
                                                String notificationMessage,
                                                String auditActor) {
        String value = Boolean.toString(maintenanceOn);
        settings.put("maintenance", value);
        settingsDao.upsert("maintenance", value);
        settings.put(MAINTENANCE_ORIGIN_KEY, origin);
        settingsDao.upsert(MAINTENANCE_ORIGIN_KEY, origin);
        if (windowContext != null && maintenanceOn) {
            String windowId = Long.toString(windowContext.getId());
            settings.put(MAINTENANCE_WINDOW_KEY, windowId);
            settingsDao.upsert(MAINTENANCE_WINDOW_KEY, windowId);
        } else if (!maintenanceOn) {
            settings.put(MAINTENANCE_WINDOW_KEY, "");
            settingsDao.upsert(MAINTENANCE_WINDOW_KEY, "");
        }

        String body = notificationMessage != null
                ? notificationMessage
                : "Maintenance mode is now " + (maintenanceOn ? "ON" : "OFF") + ".";
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.ALL,
                null,
                body,
                "Maintenance"));

        String auditMessage = "Maintenance mode set to " + maintenanceOn + " via " + origin;
        if (windowContext != null) {
            auditMessage += " (#" + windowContext.getId() + ")";
        }
        AuditLogService.log(AuditLogService.EventType.MAINTENANCE_TOGGLE,
                auditActor,
                auditMessage);
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

    private static void clearCourseRelationshipCaches(String courseId) {
        coursePrerequisiteCache.remove(courseId);
        courseCorequisiteCache.remove(courseId);
        courseAntirequisiteCache.remove(courseId);
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
            List<WaitlistDao.WaitlistEntry> waitlist = waitlistDao.findEntries(section.getSectionId());
            for (WaitlistDao.WaitlistEntry entry : waitlist) {
                String studentCode = entry.studentCode();
                if (!section.getWaitlistedStudentIds().contains(studentCode)) {
                    section.getWaitlistedStudentIds().add(studentCode);
                }
            }
        }
    }

    public static List<TermGpa> getStudentGpaHistory(String studentId) {
        Map<TermKey, List<Double>> gradesByTerm = new HashMap<>();
        for (EnrollmentRecord record : enrollmentDao.findByStudent(studentId)) {
            if (record.getFinalGrade() <= 0) {
                continue;
            }
            Section section = getSection(record.getSectionId());
            if (section == null) {
                continue;
            }
            TermKey key = new TermKey(section.getYear(), section.getSemester());
            gradesByTerm.computeIfAbsent(key, k -> new ArrayList<>()).add(record.getFinalGrade());
        }
        return gradesByTerm.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().orderValue()))
                .map(entry -> {
                    double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double gpa = Math.min(4.0, Math.max(0.0, (avg / 100.0) * 4.0));
                    boolean probation = gpa < 2.0;
                    return new TermGpa(entry.getKey().label(), gpa, probation);
                })
                .collect(Collectors.toList());
    }

    private record TermKey(int year, String semester) {
        String label() {
            return (semester == null ? "Unknown" : semester) + " " + year;
        }

        int orderValue() {
            return year * 10 + semesterOrder(semester);
        }

        private int semesterOrder(String semester) {
            if (semester == null) {
                return 0;
            }
            return switch (semester.toLowerCase(Locale.ENGLISH)) {
                case "spring" -> 1;
                case "summer" -> 2;
                case "fall" -> 3;
                case "winter" -> 0;
                default -> 5;
            };
        }
    }

    public record TermGpa(String termLabel, double gpa, boolean probation) {
    }

    public record SectionConflict(SectionConflict.Type type, String sectionA, String sectionB, String detail) {
        public enum Type {
            ROOM,
            FACULTY
        }
    }

    public record CapacityWarning(String sectionId, int capacity, int enrolled) {
        public int overBy() {
            return Math.max(0, enrolled - capacity);
        }
    }

    private static EnrollmentRecord findEnrollmentRecord(String sectionId, String studentId) {
        return enrollmentDao.findBySection(sectionId).stream()
                .filter(rec -> rec.getStudentId().equalsIgnoreCase(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Student " + studentId + " not enrolled in section " + sectionId));
    }

    private static List<String> resolveSectionRecipients(String sectionId, List<String> requested) {
        List<String> enrolled = enrollmentDao.findBySection(sectionId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(EnrollmentRecord::getStudentId)
                .collect(Collectors.toList());
        if (requested == null || requested.isEmpty()) {
            return enrolled;
        }
        Set<String> allowed = new LinkedHashSet<>();
        for (String candidate : requested) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String trimmed = candidate.trim();
            if (enrolled.stream().anyMatch(id -> id.equalsIgnoreCase(trimmed))) {
                allowed.add(trimmed);
            }
        }
        return new ArrayList<>(allowed);
    }
}




