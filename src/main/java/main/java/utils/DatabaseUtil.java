package main.java.utils;

import main.java.config.ConfigLoader;
import main.java.data.dao.AuthUserDao;

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
import main.java.data.dao.NotificationPreferenceDao;
import main.java.data.dao.InstructorMessageDao;
import main.java.data.dao.RegistrationRequestDao;
import main.java.data.migration.LegacyDataMigrator;
import main.java.data.dao.AuditLogDao;

import main.java.service.NotificationDeliveryService;
import main.java.service.GradebookService;
import main.java.models.*;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
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
 * Database utility facade exposing high-level operations backed by the DAO
 * layer.
 */
public class DatabaseUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUtil.class);
    private static final String DATA_DIR = "data/";

    private static final Map<String, String> settings = new ConcurrentHashMap<>();
    private static Map<String, Student> students = new ConcurrentHashMap<>();
    private static Map<String, Course> courses = new ConcurrentHashMap<>();

    private static final int MAX_TERM_CREDITS = parseIntConfig("registration.maxCredits", 24);

    private static AuthUserDao authUserDao;
    private static StudentDao studentDao;
    private static CourseDao courseDao;
    private static InstructorDao instructorDao;
    private static SectionDao sectionDao;
    private static EnrollmentDao enrollmentDao;
    private static WaitlistDao waitlistDao;
    private static AttendanceDao attendanceDao;
    private static NotificationDao notificationDao;
    private static SettingsDao settingsDao;
    private static CoursePrerequisiteDao coursePrerequisiteDao;
    private static CourseRelationshipDao courseRelationshipDao;
    private static AssessmentTemplateDao assessmentTemplateDao;
    private static PaymentTransactionDao paymentTransactionDao;
    private static FeeInstallmentDao feeInstallmentDao;
    private static RegistrationRequestDao registrationRequestDao;
    private static InstructorMessageDao instructorMessageDao;
    private static FeeScheduleTemplateDao feeScheduleTemplateDao;
    private static MaintenanceWindowDao maintenanceWindowDao;
    private static NotificationPreferenceDao notificationPreferenceDao;
    private static AuditLogDao auditLogDao;

    // Getters with lazy initialization
    public static synchronized AuthUserDao getAuthUserDao() {
        if (authUserDao == null)
            authUserDao = new AuthUserDao();
        return authUserDao;
    }

    public static synchronized StudentDao getStudentDao() {
        if (studentDao == null)
            studentDao = new StudentDao();
        return studentDao;
    }

    public static synchronized CourseDao getCourseDao() {
        if (courseDao == null)
            courseDao = new CourseDao();
        return courseDao;
    }

    public static synchronized InstructorDao getInstructorDao() {
        if (instructorDao == null)
            instructorDao = new InstructorDao();
        return instructorDao;
    }

    public static synchronized SectionDao getSectionDao() {
        if (sectionDao == null)
            sectionDao = new SectionDao();
        return sectionDao;
    }

    public static synchronized EnrollmentDao getEnrollmentDao() {
        if (enrollmentDao == null)
            enrollmentDao = new EnrollmentDao();
        return enrollmentDao;
    }

    public static EnrollmentRecord getEnrollment(String sectionId, String studentId) {
        return getEnrollmentDao().findBySectionAndStudent(sectionId, studentId);
    }

    public static synchronized WaitlistDao getWaitlistDao() {
        if (waitlistDao == null)
            waitlistDao = new WaitlistDao();
        return waitlistDao;
    }

    public static synchronized AttendanceDao getAttendanceDao() {
        if (attendanceDao == null)
            attendanceDao = new AttendanceDao();
        return attendanceDao;
    }

    public static synchronized NotificationDao getNotificationDao() {
        if (notificationDao == null)
            notificationDao = new NotificationDao();
        return notificationDao;
    }

    public static synchronized SettingsDao getSettingsDao() {
        if (settingsDao == null)
            settingsDao = new SettingsDao();
        return settingsDao;
    }

    public static synchronized CoursePrerequisiteDao getCoursePrerequisiteDao() {
        if (coursePrerequisiteDao == null)
            coursePrerequisiteDao = new CoursePrerequisiteDao();
        return coursePrerequisiteDao;
    }

    public static synchronized CourseRelationshipDao getCourseRelationshipDao() {
        if (courseRelationshipDao == null)
            courseRelationshipDao = new CourseRelationshipDao();
        return courseRelationshipDao;
    }

    public static synchronized AssessmentTemplateDao getAssessmentTemplateDao() {
        if (assessmentTemplateDao == null)
            assessmentTemplateDao = new AssessmentTemplateDao();
        return assessmentTemplateDao;
    }

    public static synchronized PaymentTransactionDao getPaymentTransactionDao() {
        if (paymentTransactionDao == null)
            paymentTransactionDao = new PaymentTransactionDao();
        return paymentTransactionDao;
    }

    public static synchronized FeeInstallmentDao getFeeInstallmentDao() {
        if (feeInstallmentDao == null)
            feeInstallmentDao = new FeeInstallmentDao();
        return feeInstallmentDao;
    }

    public static synchronized RegistrationRequestDao getRegistrationRequestDao() {
        if (registrationRequestDao == null)
            registrationRequestDao = new RegistrationRequestDao();
        return registrationRequestDao;
    }

    public static synchronized InstructorMessageDao getInstructorMessageDao() {
        if (instructorMessageDao == null)
            instructorMessageDao = new InstructorMessageDao();
        return instructorMessageDao;
    }

    public static synchronized FeeScheduleTemplateDao getFeeScheduleTemplateDao() {
        if (feeScheduleTemplateDao == null)
            feeScheduleTemplateDao = new FeeScheduleTemplateDao();
        return feeScheduleTemplateDao;
    }

    public static synchronized MaintenanceWindowDao getMaintenanceWindowDaoInternal() {
        if (maintenanceWindowDao == null)
            maintenanceWindowDao = new MaintenanceWindowDao();
        return maintenanceWindowDao;
    }

    public static synchronized NotificationPreferenceDao getNotificationPreferenceDao() {
        if (notificationPreferenceDao == null)
            notificationPreferenceDao = new NotificationPreferenceDao();
        return notificationPreferenceDao;
    }

    public static synchronized AuditLogDao getAuditLogDaoInternal() {
        if (auditLogDao == null)
            auditLogDao = new AuditLogDao();
        return auditLogDao;
    }

    public static void setAuthUserDao(AuthUserDao dao) {
        authUserDao = dao;
    }

    public static void setStudentDao(StudentDao dao) {
        studentDao = dao;
    }

    public static void setCourseDao(CourseDao dao) {
        courseDao = dao;
    }

    public static void setInstructorDao(InstructorDao dao) {
        instructorDao = dao;
    }

    public static void setSectionDao(SectionDao dao) {
        sectionDao = dao;
    }

    public static void setEnrollmentDao(EnrollmentDao dao) {
        enrollmentDao = dao;
    }

    public static void setWaitlistDao(WaitlistDao dao) {
        waitlistDao = dao;
    }

    public static void setAttendanceDao(AttendanceDao dao) {
        attendanceDao = dao;
    }

    public static void setNotificationDao(NotificationDao dao) {
        notificationDao = dao;
    }

    public static void setSettingsDao(SettingsDao dao) {
        settingsDao = dao;
    }

    public static void setCoursePrerequisiteDao(CoursePrerequisiteDao dao) {
        coursePrerequisiteDao = dao;
    }

    public static void setCourseRelationshipDao(CourseRelationshipDao dao) {
        courseRelationshipDao = dao;
    }

    public static void setAssessmentTemplateDao(AssessmentTemplateDao dao) {
        assessmentTemplateDao = dao;
    }

    public static void setPaymentTransactionDao(PaymentTransactionDao dao) {
        paymentTransactionDao = dao;
    }

    public static void setFeeInstallmentDao(FeeInstallmentDao dao) {
        feeInstallmentDao = dao;
    }

    public static void setRegistrationRequestDao(RegistrationRequestDao dao) {
        registrationRequestDao = dao;
    }

    public static void setNotificationPreferenceDao(NotificationPreferenceDao dao) {
        notificationPreferenceDao = dao;
    }

    public static void setInstructorMessageDao(InstructorMessageDao dao) {
        instructorMessageDao = dao;
    }

    public static void setFeeScheduleTemplateDao(FeeScheduleTemplateDao dao) {
        feeScheduleTemplateDao = dao;
    }

    public static void setMaintenanceWindowDao(MaintenanceWindowDao dao) {
        maintenanceWindowDao = dao;
    }

    public static void setAuditLogDao(AuditLogDao dao) {
        auditLogDao = dao;
    }

    public static AuditLogDao getAuditLogDao() {
        return getAuditLogDaoInternal();
    }

    public static MaintenanceWindowDao getMaintenanceWindowDao() {
        return getMaintenanceWindowDaoInternal();
    }

    private static final Map<String, List<String>> coursePrerequisiteCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> courseCorequisiteCache = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> courseAntirequisiteCache = new ConcurrentHashMap<>();

    private static final double PASSING_GRADE_THRESHOLD = 60.0;
    private static final List<MaintenanceWindow> maintenanceWindowCache = new CopyOnWriteArrayList<>();
    private static final ScheduledExecutorService maintenanceExecutor = Executors
            .newSingleThreadScheduledExecutor(r -> {
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
            getSettingsDao().upsert(key, defaultValue);
        }
    }

    public static synchronized void initializeDatabase() {
        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        refreshMaintenanceWindowCache();
        evaluateMaintenanceSchedule();
        startMaintenanceScheduler();
        try {
            LegacyDataMigrator.defaultMigrator().migrateAll();
        } catch (Exception ex) {
            LOGGER.error("Legacy data migration failed: ", ex);
        }
        boolean hasUsers = !getAuthUserDao().findAll().isEmpty();
        if (!hasUsers) {
            createSampleData();
        }

        ensureSettingDefault("maintenance", "false");
        ensureSettingDefault(MAINTENANCE_ORIGIN_KEY, "manual");

    }

    private static void createSampleData() {
        if (getAuthUserDao().findByUsername("admin").isPresent()) {
            return;
        }
        // Create default accounts with policy-compliant passwords
        String adminPass = ConfigLoader.getOrDefault("admin.default.password", "Admin@12345");
        addUser("admin", "Admin", "Administrator", "admin@college.edu", adminPass);
        addUser("inst1", "Instructor", "John Smith", "john.smith@college.edu", "Instructor@123");
        addUser("stu1", "Student", "Alice Johnson", "alice.johnson@student.college.edu", "Student@1231");
        addUser("stu2", "Student", "Bob Williams", "bob.williams@student.college.edu", "Student@1232");

        // Create sample faculty
        Faculty f1 = new Faculty("FAC001", "John", "Smith", "john.smith@college.edu",
                "123-456-7890", "Computer Science", "Professor", "Ph.D", 75000);
        f1.setUsername("inst1");
        getInstructorDao().insert(f1);

        Faculty f2 = new Faculty("FAC002", "Jane", "Davis", "jane.davis@college.edu",
                "123-456-7891", "Mathematics", "Associate Professor", "M.Sc", 65000);
        getInstructorDao().insert(f2);

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
                30);
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
                30);
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
                25);
        sec3.setSemester("Fall");
        sec3.setYear(LocalDate.now().getYear());
        sec3.setDropDeadline(LocalDate.now().plusDays(25));
        addSection(sec3);

        EnrollmentRecord er1 = new EnrollmentRecord(s1.getStudentId(), sec1.getSectionId(),
                EnrollmentRecord.Status.ENROLLED);
        getEnrollmentDao().insert(er1);
        EnrollmentRecord er2 = new EnrollmentRecord(s2.getStudentId(), sec3.getSectionId(),
                EnrollmentRecord.Status.ENROLLED);
        getEnrollmentDao().insert(er2);
        EnrollmentRecord er3 = new EnrollmentRecord(s2.getStudentId(), sec2.getSectionId(),
                EnrollmentRecord.Status.WAITLISTED);
        getEnrollmentDao().insert(er3);
        getWaitlistDao().insert(sec2.getSectionId(), s2.getStudentId(), 1, true);

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

    public static void saveData() {
        // No-op retained for backward compatibility with legacy callers.
    }

    // User operations
    public static User authenticateUser(String username, String password) {
        return main.java.service.AuthService.authenticateUser(username, password);
    }

    public static List<User> getAllUsers() {
        return new ArrayList<>(getAuthUserDao().findAll());
    }

    public static User getUser(String username) {
        return getAuthUserDao().findByUsername(username).orElse(null);
    }

    public static synchronized User addUser(String username, String role, String fullName, String email,
            String rawPassword) {
        PasswordPolicy.validateComplexity(rawPassword);
        if (getAuthUserDao().findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword.toCharArray(), salt);
        User user = new User(username, hash, salt, role, fullName, email);
        user.setActive(true);
        user.setMustChangePassword(true);
        user.addPasswordHistory(salt, hash, 5); // Default history size
        return getAuthUserDao().insert(user);
    }

    public static synchronized void updateUserProfile(String username, String fullName, String email, boolean active) {
        main.java.service.AuthService.updateProfile(username, fullName, email, active);
    }

    public static synchronized void updateUserRole(String username, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);
        user.setRole(role.trim());
        getAuthUserDao().updateRole(user);
    }

    public static synchronized void setUserActive(String username, boolean active) {
        User user = getUser(username);
        if (user == null || user.isActive() == active) {
            return;
        }
        user.setActive(active);
        getAuthUserDao().updateProfile(user);
    }

    public static synchronized void updateUserContact(String username, String fullName, String email) {
        User user = getUser(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);
        user.setFullName(fullName);
        user.setEmail(email);
        getAuthUserDao().updateProfile(user);
    }

    public static synchronized void changePasswordSelf(String username, String currentPassword, String newPassword) {
        main.java.service.AuthService.changePassword(username, currentPassword, newPassword);
    }

    public static synchronized void resetPasswordByAdmin(String username, String newPassword) {
        main.java.service.AuthService.resetPassword(username, newPassword);
    }

    // Student operations
    public static void addStudent(Student student) {
        getStudentDao().insert(student);
        students.put(student.getStudentId(), student);
    }

    public static void updateStudent(Student student) {
        getStudentDao().update(student);
        students.put(student.getStudentId(), student);
    }

    public static void deleteStudent(String studentId) {
        getStudentDao().delete(studentId);
        students.remove(studentId);
    }

    public static Student getStudent(String studentId) {
        Student cached = students.get(studentId);
        if (cached != null)
            return cached;
        Student s = getStudentDao().findByCode(studentId).orElse(null);
        if (s != null)
            students.put(studentId, s);
        return s;
    }

    public static List<Student> getAllStudents() {
        return getStudentDao().findAll();
    }

    public static Student findStudentByUsername(String username) {
        return getStudentDao().findByUsername(username).orElse(null);
    }

    public static List<Section> getSectionsForFaculty(String facultyId) {
        return getSectionDao().findByFaculty(facultyId);
    }

    // Finance operations
    public static List<PaymentTransaction> getPaymentHistoryForStudent(String studentId) {
        return getPaymentTransactionDao().findByStudent(studentId).stream()
                .map(DatabaseUtil::copyTransaction)
                .collect(Collectors.toList());
    }

    public static List<FeeInstallment> getInstallmentsForStudent(String studentId) {
        return getFeeInstallmentDao().findByStudent(studentId).stream()
                .map(DatabaseUtil::cloneInstallment)
                .collect(Collectors.toList());
    }

    public static synchronized PaymentTransaction recordPayment(String actorUsername,
            String studentId,
            double amount,
            String method,
            String reference,
            String notes) {
        return main.java.service.FinanceService.recordPayment(actorUsername, studentId, amount, method, reference,
                notes);
    }

    public static void upsertInstallment(String studentId, FeeInstallment installment) {
        installment.setStudentId(studentId);
        if (installment.getInstallmentId() == null || installment.getInstallmentId().isBlank()) {
            installment.setInstallmentId(UUID.randomUUID().toString());
        }
        if (!getFeeInstallmentDao().update(installment)) {
            getFeeInstallmentDao().insert(installment);
        }
    }

    public static void deleteInstallment(String studentId, String installmentId) {
        getFeeInstallmentDao().delete(installmentId);
    }

    public static void markInstallmentReminderSent(String studentId, String installmentId) {
        List<FeeInstallment> schedule = getFeeInstallmentDao().findByStudent(studentId);
        schedule.stream()
                .filter(inst -> inst.getInstallmentId().equals(installmentId))
                .findFirst()
                .ifPresent(inst -> {
                    inst.setLastReminderSent(LocalDate.now());
                    getFeeInstallmentDao().update(inst);
                });
    }

    public static FeeInstallment nextDueInstallment(String studentId) {
        return getFeeInstallmentDao().findByStudent(studentId).stream()
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
        return new ArrayList<>(getFeeScheduleTemplateDao().findByCourse(courseCode));
    }

    public static FeeScheduleTemplateDao.TemplateRecord addFeeScheduleTemplate(String courseCode,
            String label,
            double amount,
            int offsetDays) {
        validateTemplateInput(courseCode, label, amount, offsetDays);
        return getFeeScheduleTemplateDao().insert(courseCode.trim(), label.trim(), amount, offsetDays);
    }

    public static void updateFeeScheduleTemplate(long templateId,
            String courseCode,
            String label,
            double amount,
            int offsetDays) {
        validateTemplateInput(courseCode, label, amount, offsetDays);
        getFeeScheduleTemplateDao().update(templateId, label.trim(), amount, offsetDays);
    }

    public static void deleteFeeScheduleTemplate(long templateId) {
        getFeeScheduleTemplateDao().delete(templateId);
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

        List<FeeScheduleTemplateDao.TemplateRecord> templates = getFeeScheduleTemplateDao().findByCourse(targetCourse);
        if (templates.isEmpty()) {
            throw new IllegalStateException("No templates configured for " + targetCourse);
        }

        double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
        if (outstanding <= 0.0) {
            throw new IllegalStateException("Student has no outstanding balance.");
        }

        // Remove un-paid installments so the template can take over.
        for (FeeInstallment installment : getFeeInstallmentDao().findByStudent(studentId)) {
            if (installment.getStatus() != FeeInstallment.Status.PAID) {
                getFeeInstallmentDao().delete(installment.getInstallmentId());
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
                    0.0,
                    FeeInstallment.Status.DUE,
                    template.label(),
                    null,
                    null);
            getFeeInstallmentDao().insert(installment);
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
                    0.0,
                    FeeInstallment.Status.DUE,
                    "Balance",
                    null,
                    null);
            getFeeInstallmentDao().insert(balanceInstallment);
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
        return GradebookService.formatWeights(weights);
    }

    private static Map<String, Double> deserializeWeights(String payload) {
        return GradebookService.parseWeights(payload);
    }

    // Gradebook template + moderation operations
    public static List<AssessmentTemplateDao.AssessmentTemplate> getAssessmentTemplates(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return Collections.emptyList();
        }
        return getAssessmentTemplateDao().findByCourse(courseCode.trim());
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
        return getAssessmentTemplateDao().insert(courseCode.trim(), templateName.trim(), payload,
                createdBy == null ? "system" : createdBy);
    }

    public static void deleteAssessmentTemplate(long templateId) {
        getAssessmentTemplateDao().delete(templateId);
    }

    public static void applyAssessmentTemplate(long templateId, String sectionId) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }
        AssessmentTemplateDao.AssessmentTemplate template = getAssessmentTemplateDao().findById(templateId);
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
        getEnrollmentDao().update(record);
    }

    public static void saveComponentFeedbackEntry(String sectionId, String studentId, String component,
            String comment) {
        EnrollmentRecord record = findEnrollmentRecord(sectionId, studentId);
        record.putFeedback(component, comment);
        getEnrollmentDao().update(record);
    }

    public static List<InstructorMessageDao.MessageLog> getInstructorMessageLog(String instructorUsername) {
        if (instructorUsername == null || instructorUsername.isBlank()) {
            return Collections.emptyList();
        }
        return getInstructorMessageDao().findByInstructor(instructorUsername.trim());
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
        getInstructorMessageDao().insert(
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
                source.getNotes());
    }

    private static FeeInstallment cloneInstallment(FeeInstallment source) {
        return FeeInstallment.copyOf(source);
    }

    // Faculty operations
    public static void addFaculty(Faculty facultyMember) {
        getInstructorDao().insert(facultyMember);
    }

    public static void updateFaculty(Faculty facultyMember) {
        getInstructorDao().update(facultyMember);
    }

    public static void deleteFaculty(String facultyId) {
        getInstructorDao().delete(facultyId);
    }

    public static Faculty getFaculty(String facultyId) {
        return getInstructorDao().findByCode(facultyId).orElse(null);
    }

    public static List<Faculty> getAllFaculty() {
        return getInstructorDao().findAll();
    }

    public static Faculty findFacultyByUsername(String username) {
        return getInstructorDao().findByUsername(username).orElse(null);
    }

    // Course operations
    public static void addCourse(Course course) {
        getCourseDao().insert(course);
        courses.put(course.getCourseId(), course);
        clearCourseRelationshipCaches(course.getCourseId());
    }

    public static void updateCourse(Course course) {
        getCourseDao().update(course);
        courses.put(course.getCourseId(), course);
        clearCourseRelationshipCaches(course.getCourseId());
    }

    public static void deleteCourse(String courseId) {
        getCourseDao().delete(courseId);
        courses.remove(courseId);
        coursePrerequisiteCache.remove(courseId);
    }

    public static Course getCourse(String courseId) {
        Course cached = courses.get(courseId);
        if (cached != null)
            return cached;
        Course c = getCourseDao().findByCode(courseId).orElse(null);
        if (c != null)
            courses.put(courseId, c);
        return c;
    }

    public static List<Course> getAllCourses() {
        return getCourseDao().findAll();
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
    public static List<Section> getAllSections() {
        return getSectionDao().findAll();
    }

    public static Section getSection(String sectionId) {
        return getSectionDao().findByCode(sectionId).orElse(null);
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
        Map<String, Long> enrollmentCounts = getEnrollmentDao().countEnrolledBySections();
        for (Section section : getAllSections()) {
            long enrolled = enrollmentCounts.getOrDefault(section.getSectionId(), 0L);
            if (enrolled > section.getCapacity()) {
                warnings.add(new CapacityWarning(section.getSectionId(), section.getCapacity(), (int) enrolled));
            }
        }
        return warnings;
    }

    public static void addSection(Section section) {
        enforceRoomScheduleClash(section);
        getSectionDao().insert(section);
    }

    public static void updateSection(Section section) {
        enforceRoomScheduleClash(section);
        getSectionDao().update(section);
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
        getSectionDao().delete(sectionId);
        getEnrollmentDao().deleteBySection(sectionId);
        getWaitlistDao().deleteAll(sectionId);
        getAttendanceDao().deleteBySection(sectionId);
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

        String actor = performedBy == null || performedBy.isBlank() ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.SECTION_ASSIGNMENT,
                actor,
                String.format("Assigned %s to section %s", facultyId, sectionId));
    }

    // Enrollment operations
    public static List<EnrollmentRecord> getEnrollmentsForStudent(String studentId) {
        return getEnrollmentDao().findByStudent(studentId);
    }

    public static void updateEnrollment(EnrollmentRecord record) {
        getEnrollmentDao().update(record);
    }

    public static boolean isStudentEnrolledInCourse(String studentId, String courseId) {
        return getEnrollmentDao().findByStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(rec -> getSection(rec.getSectionId()))
                .filter(Objects::nonNull)
                .anyMatch(section -> section.getCourseId().equals(courseId));
    }

    public static List<EnrollmentRecord> getEnrollmentsForSection(String sectionId) {
        return getEnrollmentDao().findBySection(sectionId);
    }

    public static synchronized EnrollmentRecord registerStudentToSection(String studentId, String sectionId) {
        return registerStudentToSection(null, studentId, sectionId);
    }

    public static synchronized EnrollmentRecord registerStudentToSection(User actor, String studentId,
            String sectionId) {
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

        if (hasScheduleConflict(studentId, section)) {
            throw new IllegalStateException("Schedule conflict detected with another section");
        }

        EnrollmentRecord record = null;
        try (Connection conn = getEnrollmentDao().getConnection()) {
            conn.setAutoCommit(false);
            try {
                getSectionDao().lockSection(conn, sectionId);
                List<EnrollmentRecord> existing = getEnrollmentDao().findBySection(sectionId);
                boolean already = existing.stream()
                        .anyMatch(rec -> rec.getStudentId().equals(studentId)
                                && rec.getStatus() != EnrollmentRecord.Status.DROPPED);
                if (already) {
                    throw new IllegalStateException("Student already enrolled or waitlisted in this section");
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

                if (hasSeat) {
                    // Try to reserve seat in course (atomic)
                    boolean seatReserved = getCourseDao().decrementAvailableSeats(conn, section.getCourseId());
                    if (!seatReserved) {
                        hasSeat = false; // Course-level capacity exceeded
                    }
                }

                record = new EnrollmentRecord(studentId, sectionId,
                        hasSeat ? EnrollmentRecord.Status.ENROLLED : EnrollmentRecord.Status.WAITLISTED);
                getEnrollmentDao().insert(conn, record);

                if (hasSeat) {
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            studentId,
                            "You are enrolled in " + section.getTitle() + " (" + section.getSectionId() + ").",
                            "Registration"));
                    refreshStudentEnrollmentMetrics(studentId);
                } else {
                    int position = getWaitlistDao().findEntries(sectionId).size() + 1;
                    getWaitlistDao().insert(conn, sectionId, studentId, position, autoApproved);
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
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            LoggerFactory.getLogger(DatabaseUtil.class).error("Database error during enrollment: {}", ex.getMessage(),
                    ex);
            throw new IllegalStateException("System error during registration", ex);
        }
        String actorName = performedBy == null ? "system" : performedBy;
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE, actorName,
                String.format("Registered %s in %s (%s)", studentId, section.getTitle(), record.getStatus()));
        return record;
    }

    private static boolean hasScheduleConflict(String studentId, Section targetSection) {
        return getEnrollmentDao().findByStudent(studentId).stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .map(rec -> getSection(rec.getSectionId()))
                .filter(Objects::nonNull)
                .anyMatch(existing -> overlaps(existing, targetSection));
    }

    private static boolean overlaps(Section a, Section b) {
        if (a.getDayOfWeek() != b.getDayOfWeek()) {
            return false;
        }
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }

    private static void enforceRoomScheduleClash(Section candidate) {
        if (candidate.getLocation() == null || candidate.getLocation().isBlank()
                || candidate.getDayOfWeek() == null || candidate.getStartTime() == null
                || candidate.getEndTime() == null) {
            return;
        }
        List<Section> conflicts = getSectionDao().findByLocationAndSchedule(
                candidate.getLocation(),
                candidate.getDayOfWeek(),
                candidate.getStartTime(),
                candidate.getEndTime());

        for (Section existing : conflicts) {
            if (existing.getSectionId().equalsIgnoreCase(candidate.getSectionId())) {
                continue;
            }
            throw new IllegalStateException(String.format(
                    "Room %s is already in use by %s (%s %s-%s).",
                    existing.getLocation(),
                    existing.getSectionId(),
                    existing.getDayOfWeek(),
                    existing.getStartTime(),
                    existing.getEndTime()));
        }
    }

    public static synchronized void dropStudentFromSection(String studentId, String sectionId) {
        dropStudentFromSection(null, studentId, sectionId);
    }

    public static synchronized void dropStudentFromSection(String performedBy, String studentId, String sectionId) {
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found");
        }

        try (Connection conn = getEnrollmentDao().getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<EnrollmentRecord> sectionEnrollments = getEnrollmentDao().findBySection(sectionId);
                EnrollmentRecord record = sectionEnrollments.stream()
                        .filter(rec -> rec.getStudentId().equals(studentId)
                                && rec.getStatus() != EnrollmentRecord.Status.DROPPED)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Student not enrolled in the section"));

                EnrollmentRecord.Status previousStatus = record.getStatus();
                record.setStatus(EnrollmentRecord.Status.DROPPED);
                getEnrollmentDao().update(conn, record);
                getWaitlistDao().delete(conn, sectionId, studentId);

                if (previousStatus == EnrollmentRecord.Status.ENROLLED) {
                    boolean promoted = promoteApprovedWaitlistedIfPossible(conn, section, sectionEnrollments);
                    if (!promoted) {
                        getCourseDao().incrementAvailableSeats(conn, section.getCourseId());
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            LOGGER.error("Database error during drop: {}", ex.getMessage(), ex);
            throw new IllegalStateException("System error during drop", ex);
        }

        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "You dropped " + section.getTitle() + " (" + section.getSectionId() + ").",
                "Registration"));

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
            boolean ignoreRequisites,
            boolean ignoreCredits) {
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
        EnrollmentRecord record = null;
        EnrollmentRecord.Status status = null;
        try (Connection conn = getEnrollmentDao().getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<EnrollmentRecord> existing = getEnrollmentDao().findBySection(sectionId);
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

                if (enrollNow) {
                    // Try to reserve seat in course (atomic) - but skip if ignoreCapacity is true?
                    // Usually override bypasses section capacity, but course availability might
                    // still be tracked.
                    // For now, let's treat override as priority enrollment.
                    if (seatAvailable) {
                        getCourseDao().decrementAvailableSeats(conn, section.getCourseId());
                    }
                }

                status = enrollNow ? EnrollmentRecord.Status.ENROLLED
                        : EnrollmentRecord.Status.WAITLISTED;
                record = new EnrollmentRecord(studentId, sectionId, status);
                getEnrollmentDao().insert(conn, record);

                if (status == EnrollmentRecord.Status.ENROLLED) {
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            studentId,
                            "You were force-enrolled into " + section.getTitle() + " (" + section.getSectionId() + ").",
                            "Registration"));
                    refreshStudentEnrollmentMetrics(studentId);
                } else {
                    int position = getWaitlistDao().findEntries(sectionId).size() + 1;
                    getWaitlistDao().insert(conn, sectionId, studentId, position, true);
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            studentId,
                            "You were added to the waitlist for " + section.getTitle() + " (" + section.getSectionId()
                                    + ").",
                            "Registration"));
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            LOGGER.error("Database error during override enroll: {}", ex.getMessage(), ex);
            throw new IllegalStateException("System error during override enrollment", ex);
        }

        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                admin.getUsername(),
                String.format("Override enroll %s into %s (status %s)", studentId, sectionId, status));
        return record;
    }

    private static boolean promoteApprovedWaitlistedIfPossible(Connection conn, Section section,
            List<EnrollmentRecord> sectionEnrollments) throws SQLException {
        List<WaitlistDao.WaitlistEntry> waitlist = getWaitlistDao().findEntries(section.getSectionId());
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
                getWaitlistDao().delete(conn, section.getSectionId(), candidate);
                EnrollmentRecord promotedRecord = sectionEnrollments.stream()
                        .filter(rec -> rec.getStudentId().equals(candidate))
                        .findFirst()
                        .orElse(null);
                if (promotedRecord != null) {
                    promotedRecord.setStatus(EnrollmentRecord.Status.ENROLLED);
                    getEnrollmentDao().update(conn, promotedRecord);
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
                getWaitlistDao().delete(conn, section.getSectionId(), candidate);
                EnrollmentRecord candidateRecord = sectionEnrollments.stream()
                        .filter(rec -> rec.getStudentId().equals(candidate))
                        .findFirst()
                        .orElse(null);
                if (candidateRecord != null) {
                    candidateRecord.setStatus(EnrollmentRecord.Status.DROPPED);
                    getEnrollmentDao().update(conn, candidateRecord);
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
        return getEnrollmentDao().findByStudent(studentId).stream()
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
                status != null ? status : AttendanceRecord.AttendanceStatus.ABSENT));
        getAttendanceDao().deleteBySectionAndDate(sectionId, date);
        getAttendanceDao().insert(record);
    }

    public static List<AttendanceRecord> getAttendanceForSection(String sectionId) {
        return getAttendanceDao().findBySection(sectionId);
    }

    // Notification operations
    public static List<NotificationMessage> getNotifications(NotificationMessage.Audience audience, String targetId) {
        NotificationMessage.Audience resolvedAudience = audience == null ? NotificationMessage.Audience.ALL : audience;
        return getNotificationDao().findVisible(resolvedAudience, targetId);
    }

    public static List<NotificationMessage> getNotificationsForStudent(String studentId) {
        return getNotifications(NotificationMessage.Audience.STUDENT, studentId);
    }

    public static void addNotification(NotificationMessage notification) {
        getNotificationDao().insert(notification);
    }

    public static void markNotificationRead(long notificationId, boolean read) {
        getNotificationDao().markRead(notificationId, read);
    }

    public static List<NotificationMessage> getNotificationsForAdmin(NotificationMessage.Audience audience,
            LocalDateTime from,
            LocalDateTime to,
            String category) {
        return getNotificationDao().findAdminHistory(audience, from, to, category);
    }

    public static NotificationPreference getNotificationPreference(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        return getNotificationPreferenceDao().findByUserId(userId)
                .orElse(NotificationPreference.defaultPreference(userId));
    }

    public static NotificationPreference saveNotificationPreference(NotificationPreference preference) {
        if (preference == null || preference.getUserId() == null || preference.getUserId().isBlank()) {
            throw new IllegalArgumentException("Preference with user id is required");
        }
        NotificationPreference persisted = getNotificationPreferenceDao().upsert(preference);
        return persisted != null ? persisted : preference;
    }

    public static void broadcastNotification(User actor, NotificationRequest request) {
        ensureAdmin(actor);
        Objects.requireNonNull(request, "Notification request is required.");
        String message = request.getMessage().trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        List<ContactRecipient> contacts = new ArrayList<>();
        switch (request.getTargetType()) {
            case ALL -> {
                addNotification(new NotificationMessage(NotificationMessage.Audience.ALL, null, message,
                        request.getCategory()));
                contacts.addAll(collectContactsForAudience(NotificationMessage.Audience.ALL));
            }
            case STUDENTS -> {
                addNotification(new NotificationMessage(NotificationMessage.Audience.STUDENT, null, message,
                        request.getCategory()));
                contacts.addAll(collectContactsForAudience(NotificationMessage.Audience.STUDENT));
            }
            case INSTRUCTORS -> {
                addNotification(new NotificationMessage(NotificationMessage.Audience.INSTRUCTOR, null, message,
                        request.getCategory()));
                contacts.addAll(collectContactsForAudience(NotificationMessage.Audience.INSTRUCTOR));
            }
            case ADMINS -> {
                addNotification(new NotificationMessage(NotificationMessage.Audience.ADMIN, null, message,
                        request.getCategory()));
                contacts.addAll(collectContactsForAudience(NotificationMessage.Audience.ADMIN));
            }
            case USER -> {
                contacts.add(broadcastToUser(request, message));
            }
            case STUDENT_DEPARTMENT -> {
                contacts.addAll(broadcastToStudentDepartment(request, message));
            }
            case INSTRUCTOR_DEPARTMENT -> {
                contacts.addAll(broadcastToInstructorDepartment(request, message));
            }
            default -> throw new IllegalArgumentException("Unsupported target type.");
        }
        if ((request.isEmailChannel() || request.isSmsChannel()) && !contacts.isEmpty()) {
            deliverNotificationStubs(contacts, request, message);
        }
        AuditLogService.log(AuditLogService.EventType.NOTIFICATION_BROADCAST,
                actor.getUsername(),
                "Broadcast " + request.getTargetType() + " [" + request.getCategory() + "]");
    }

    public static Map<String, Long> getWaitlistCountsByCourse() {
        return getSectionDao().findAll().stream()
                .collect(Collectors.groupingBy(
                        Section::getCourseId,
                        Collectors.summingLong(sec -> (long) getWaitlistDao().findEntries(sec.getSectionId()).size())));
    }

    public static List<WaitlistDao.WaitlistEntry> getWaitlistEntries(String sectionId) {
        return getWaitlistDao().findEntries(sectionId);
    }

    public static List<WaitlistSnapshot> getWaitlistSnapshot(String sectionId) {
        List<WaitlistSnapshot> snapshot = new ArrayList<>();
        for (WaitlistDao.WaitlistEntry entry : getWaitlistDao().findEntries(sectionId)) {
            Student student = getStudent(entry.studentCode());
            String name = student != null ? student.getFullName() : entry.studentCode();
            snapshot.add(new WaitlistSnapshot(entry.studentCode(), name, entry.position(), entry.advisorApproved()));
        }
        return snapshot;
    }

    public static boolean isWaitlistApproved(String studentId, String sectionId) {
        return getWaitlistDao().findEntries(sectionId).stream()
                .filter(entry -> entry.studentCode().equals(studentId))
                .map(WaitlistDao.WaitlistEntry::advisorApproved)
                .findFirst()
                .orElse(false);
    }

    public static void setWaitlistApproval(User actor, String sectionId, String studentId, boolean approved) {
        if (actor == null || !"Admin".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Administrator privileges required.");
        }
        Section section = getSection(sectionId);
        try (Connection conn = getEnrollmentDao().getConnection()) {
            conn.setAutoCommit(false);
            try {
                getWaitlistDao().updateApproval(sectionId, studentId, approved);
                if (approved && section != null) {
                    List<EnrollmentRecord> enrollments = getEnrollmentDao().findBySection(sectionId);
                    boolean promoted = promoteApprovedWaitlistedIfPossible(conn, section, enrollments);
                    if (promoted) {
                        getCourseDao().decrementAvailableSeats(conn, section.getCourseId());
                    }
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            studentId,
                            "Advisor approval granted for " + section.getTitle()
                                    + ". You'll be enrolled automatically when a seat opens.",
                            "Registration"));
                } else if (!approved && section != null) {
                    addNotification(new NotificationMessage(
                            NotificationMessage.Audience.STUDENT,
                            studentId,
                            "Advisor approval revoked for " + section.getTitle() + ". Contact advising for details.",
                            "Registration"));
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            LOGGER.error("Database error during waitlist approval: {}", ex.getMessage(), ex);
            throw new IllegalStateException("System error during waitlist approval", ex);
        }
    }

    public static class WaitlistSnapshot {
        private final String studentId;
        private final String studentName;
        private final int position;
        private final boolean approved;

        public WaitlistSnapshot(String studentId, String studentName, int position, boolean approved) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.position = position;
            this.approved = approved;
        }

        public String studentId() {
            return studentId;
        }

        public String studentName() {
            return studentName;
        }

        public int position() {
            return position;
        }

        public boolean approved() {
            return approved;
        }
    }

    public static synchronized void promoteWaitlistedStudent(User admin, String sectionId, String studentId) {
        ensureAdmin(admin);
        Section section = getSection(sectionId);
        if (section == null) {
            throw new IllegalArgumentException("Section not found: " + sectionId);
        }

        int courseCredits = getCourseCreditHours(section.getCourseId());
        int currentCredits = calculateEnrolledCredits(studentId);
        if (currentCredits + courseCredits > MAX_TERM_CREDITS) {
            throw new IllegalStateException(
                    "Promotion failed: Student would exceed credit limit of " + MAX_TERM_CREDITS + " hours.");
        }

        List<EnrollmentRecord> enrollments = getEnrollmentDao().findBySection(sectionId);
        EnrollmentRecord record = enrollments.stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Student not on waitlist."));
        record.setStatus(EnrollmentRecord.Status.ENROLLED);
        getEnrollmentDao().updateStatus(record);
        getWaitlistDao().delete(sectionId, studentId);
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
        getWaitlistDao().delete(sectionId, studentId);
        getEnrollmentDao().findBySection(sectionId).stream()
                .filter(rec -> rec.getStudentId().equals(studentId))
                .findFirst()
                .ifPresent(rec -> {
                    rec.setStatus(EnrollmentRecord.Status.DROPPED);
                    getEnrollmentDao().updateStatus(rec);
                });
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                admin.getUsername(),
                "Removed " + studentId + " from waitlist for " + sectionId);
    }

    // Registration request operations
    public static void submitRegistrationRequest(User actor, String studentId, String sectionId) {
        getRegistrationRequestDao().findByStudentSection(studentId, sectionId).ifPresent(existing -> {
            if ("PENDING".equalsIgnoreCase(existing.status())) {
                throw new IllegalStateException("Registration request already pending advisor approval.");
            }
            if ("APPROVED".equalsIgnoreCase(existing.status())) {
                throw new IllegalStateException("Registration request already approved.");
            }
        });
        String requestedBy = actor != null ? actor.getUsername() : "student";
        getRegistrationRequestDao().insert(studentId, sectionId, requestedBy);
        Section section = getSection(sectionId);
        addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentId,
                "Registration request submitted for " + (section != null ? section.getTitle() : sectionId) + ".",
                "Registration"));
    }

    public static List<RegistrationRequestView> getPendingRegistrationRequests() {
        List<RegistrationRequestView> views = new ArrayList<>();
        for (RegistrationRequestDao.RequestRecord record : getRegistrationRequestDao().findPending()) {
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
        RegistrationRequestDao.RequestRecord record = getRegistrationRequestDao().findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(record.status())) {
            throw new IllegalStateException("Request already processed.");
        }
        try {
            registerStudentToSection(admin, record.studentCode(), record.sectionCode());
            getRegistrationRequestDao().updateStatus(requestId, "APPROVED", admin.getUsername(), notes);
            addNotification(new NotificationMessage(
                    NotificationMessage.Audience.STUDENT,
                    record.studentCode(),
                    "Advisor approved your registration for " + record.sectionCode() + ".",
                    "Registration"));
        } catch (RuntimeException ex) {
            getRegistrationRequestDao().updateStatus(requestId, "REJECTED", admin.getUsername(), ex.getMessage());
            throw ex;
        }
    }

    public static void rejectRegistrationRequest(User admin, long requestId, String notes) {
        ensureAdmin(admin);
        RegistrationRequestDao.RequestRecord record = getRegistrationRequestDao().findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (!"PENDING".equalsIgnoreCase(record.status())) {
            throw new IllegalStateException("Request already processed.");
        }
        getRegistrationRequestDao().updateStatus(requestId, "REJECTED", admin.getUsername(), notes);
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

    public static class RegistrationRequestView {
        private final long id;
        private final String studentId;
        private final String studentName;
        private final String sectionId;
        private final String sectionTitle;
        private final String requestedBy;
        private final java.time.Instant requestedAt;

        public RegistrationRequestView(long id, String studentId, String studentName, String sectionId,
                String sectionTitle, String requestedBy, java.time.Instant requestedAt) {
            this.id = id;
            this.studentId = studentId;
            this.studentName = studentName;
            this.sectionId = sectionId;
            this.sectionTitle = sectionTitle;
            this.requestedBy = requestedBy;
            this.requestedAt = requestedAt;
        }

        public long id() {
            return id;
        }

        public String studentId() {
            return studentId;
        }

        public String studentName() {
            return studentName;
        }

        public String sectionId() {
            return sectionId;
        }

        public String sectionTitle() {
            return sectionTitle;
        }

        public String requestedBy() {
            return requestedBy;
        }

        public java.time.Instant requestedAt() {
            return requestedAt;
        }
    }

    public static List<String> getCoursePrerequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return coursePrerequisiteCache.computeIfAbsent(courseId, getCoursePrerequisiteDao()::findPrerequisites);
    }

    public static List<String> getCourseCorequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseCorequisiteCache.computeIfAbsent(courseId, getCourseRelationshipDao()::findCorequisites);
    }

    public static List<String> getCourseAntirequisites(String courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        return courseAntirequisiteCache.computeIfAbsent(courseId, getCourseRelationshipDao()::findAntirequisites);
    }

    public static void updateCoursePrerequisites(String courseId, List<String> prerequisites) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        getCoursePrerequisiteDao().replacePrerequisites(courseId.trim(), normalizeCourseList(prerequisites));
        coursePrerequisiteCache.remove(courseId);
    }

    public static void updateCourseCorequisites(String courseId, List<String> coreqs) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        getCourseRelationshipDao().replaceCorequisites(courseId.trim(), normalizeCourseList(coreqs));
        courseCorequisiteCache.remove(courseId);
    }

    public static void updateCourseAntirequisites(String courseId, List<String> antireqs) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        getCourseRelationshipDao().replaceAntirequisites(courseId.trim(), normalizeCourseList(antireqs));
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
        return getEnrollmentDao().findByStudent(studentId).stream()
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

    // Settings and maintenance
    public static String getSetting(String key) {
        return settings.get(key);
    }

    public static void setSetting(String key, String value) {
        settings.put(key, value);
        getSettingsDao().upsert(key, value);
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

        MaintenanceWindow window = getMaintenanceWindowDaoInternal()
                .insert(start, end, safeMessage, status, actor.getUsername())
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
        getMaintenanceWindowDaoInternal().updateStatus(windowId, MaintenanceWindow.Status.CANCELLED);
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
        List<MaintenanceWindow> windows = getMaintenanceWindowDaoInternal().findAll();
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
                getMaintenanceWindowDaoInternal().updateStatus(window.getId(), MaintenanceWindow.Status.ACTIVE);
                window = window.withStatus(MaintenanceWindow.Status.ACTIVE);
                replaceWindowInCache(window);
                announceWindowStart(window);
                hasActive = true;
                activeWindow = window;
                continue;
            }
            if (window.getStatus() == MaintenanceWindow.Status.ACTIVE) {
                if (now.isAfter(window.getEndAt())) {
                    getMaintenanceWindowDaoInternal().updateStatus(window.getId(), MaintenanceWindow.Status.COMPLETED);
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
        getSettingsDao().upsert("maintenance", value);
        settings.put(MAINTENANCE_ORIGIN_KEY, origin);
        getSettingsDao().upsert(MAINTENANCE_ORIGIN_KEY, origin);
        if (windowContext != null && maintenanceOn) {
            String windowId = Long.toString(windowContext.getId());
            settings.put(MAINTENANCE_WINDOW_KEY, windowId);
            getSettingsDao().upsert(MAINTENANCE_WINDOW_KEY, windowId);
        } else if (!maintenanceOn) {
            settings.put(MAINTENANCE_WINDOW_KEY, "");
            getSettingsDao().upsert(MAINTENANCE_WINDOW_KEY, "");
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
        return getAuthUserDao().findByUsername(username)
                .map(user -> user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil()))
                .orElse(false);
    }

    public static int remainingAttempts(String username) {
        return getAuthUserDao().findByUsername(username)
                .map(user -> Math.max(0, 5 - user.getFailedAttempts()))
                .orElse(5);
    }

    private static void clearCourseRelationshipCaches(String courseId) {
        coursePrerequisiteCache.remove(courseId);
        courseCorequisiteCache.remove(courseId);
        courseAntirequisiteCache.remove(courseId);
    }

    public static List<TermGpa> getStudentGpaHistory(String studentId) {
        Map<TermKey, List<EnrollmentRecord>> enrollmentsByTerm = new HashMap<>();
        for (EnrollmentRecord record : getEnrollmentDao().findByStudent(studentId)) {
            if (record.getStatus() != EnrollmentRecord.Status.ENROLLED) {
                continue;
            }
            Section section = getSection(record.getSectionId());
            if (section == null) {
                continue;
            }
            TermKey key = new TermKey(section.getYear(), section.getSemester());
            enrollmentsByTerm.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        return enrollmentsByTerm.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().orderValue()))
                .map(entry -> {
                    double totalPoints = 0;
                    int totalCredits = 0;
                    for (EnrollmentRecord record : entry.getValue()) {
                        int credits = getCourseCreditHours(getSection(record.getSectionId()).getCourseId());
                        double score = record.getFinalGrade();

                        // Use centralized relative grading logic
                        double points = GradebookService.calculateRelativePoints(score, record.getSectionId());

                        totalPoints += points * credits;
                        totalCredits += credits;
                    }
                    double gpa = totalCredits == 0 ? 0.0 : totalPoints / totalCredits;
                    boolean probation = gpa < 6.0;
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

    public static class TermGpa {
        private final String termLabel;
        private final double gpa;
        private final boolean probation;

        public TermGpa(String termLabel, double gpa, boolean probation) {
            this.termLabel = termLabel;
            this.gpa = gpa;
            this.probation = probation;
        }

        public String termLabel() {
            return termLabel;
        }

        public double gpa() {
            return gpa;
        }

        public boolean probation() {
            return probation;
        }
    }

    public static class SectionConflict {
        public enum Type {
            ROOM, FACULTY
        }

        private final Type type;
        private final String sectionA;
        private final String sectionB;
        private final String detail;

        public SectionConflict(Type type, String sectionA, String sectionB, String detail) {
            this.type = type;
            this.sectionA = sectionA;
            this.sectionB = sectionB;
            this.detail = detail;
        }

        public Type type() {
            return type;
        }

        public String sectionA() {
            return sectionA;
        }

        public String sectionB() {
            return sectionB;
        }

        public String detail() {
            return detail;
        }
    }

    public static class CapacityWarning {
        private final String sectionId;
        private final int capacity;
        private final int enrolled;

        public CapacityWarning(String sectionId, int capacity, int enrolled) {
            this.sectionId = sectionId;
            this.capacity = capacity;
            this.enrolled = enrolled;
        }

        public String sectionId() {
            return sectionId;
        }

        public int capacity() {
            return capacity;
        }

        public int enrolled() {
            return enrolled;
        }

        public int overBy() {
            return Math.max(0, enrolled - capacity);
        }
    }

    private static EnrollmentRecord findEnrollmentRecord(String sectionId, String studentId) {
        return getEnrollmentDao().findBySection(sectionId).stream()
                .filter(rec -> rec.getStudentId().equalsIgnoreCase(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student " + studentId + " not enrolled in section " + sectionId));
    }

    private static List<String> resolveSectionRecipients(String sectionId, List<String> requested) {
        List<String> enrolled = getEnrollmentDao().findBySection(sectionId).stream()
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

    private static ContactRecipient broadcastToUser(NotificationRequest request, String message) {
        String username = request.getTargetValue();
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a username to target.");
        }
        User user = getUser(username.trim());
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        String targetId = resolveUserTargetId(user);
        addNotification(
                new NotificationMessage(NotificationMessage.Audience.USER, targetId, message, request.getCategory()));
        Student student = findStudentByUsername(user.getUsername());
        if (student != null) {
            return contactFromStudent(student);
        }
        Faculty faculty = findFacultyByUsername(user.getUsername());
        if (faculty != null) {
            return contactFromFaculty(faculty);
        }
        return contactFromUser(user);
    }

    private static List<ContactRecipient> broadcastToStudentDepartment(NotificationRequest request, String message) {
        String department = normalizeDepartment(request.getTargetValue());
        if (department.isEmpty()) {
            throw new IllegalArgumentException("Select a student department.");
        }
        List<ContactRecipient> contacts = new ArrayList<>();
        for (Student student : getAllStudents()) {
            if (department.equals(normalizeDepartment(student.getDepartment()))) {
                addNotification(new NotificationMessage(NotificationMessage.Audience.USER,
                        student.getStudentId(), message, request.getCategory()));
                contacts.add(contactFromStudent(student));
            }
        }
        if (contacts.isEmpty()) {
            throw new IllegalStateException("No students found for department " + request.getTargetValue());
        }
        return contacts;
    }

    private static List<ContactRecipient> broadcastToInstructorDepartment(NotificationRequest request, String message) {
        String department = normalizeDepartment(request.getTargetValue());
        if (department.isEmpty()) {
            throw new IllegalArgumentException("Select an instructor department.");
        }
        List<ContactRecipient> contacts = new ArrayList<>();
        for (Faculty faculty : getAllFaculty()) {
            if (department.equals(normalizeDepartment(faculty.getDepartment()))) {
                addNotification(new NotificationMessage(NotificationMessage.Audience.USER,
                        faculty.getFacultyId(), message, request.getCategory()));
                contacts.add(contactFromFaculty(faculty));
            }
        }
        if (contacts.isEmpty()) {
            throw new IllegalStateException("No instructors found for department " + request.getTargetValue());
        }
        return contacts;
    }

    private static List<ContactRecipient> collectContactsForAudience(NotificationMessage.Audience audience) {
        List<ContactRecipient> contacts = new ArrayList<>();
        if (audience == NotificationMessage.Audience.ALL || audience == NotificationMessage.Audience.STUDENT) {
            for (Student student : getAllStudents()) {
                contacts.add(contactFromStudent(student));
            }
        }
        if (audience == NotificationMessage.Audience.ALL || audience == NotificationMessage.Audience.INSTRUCTOR) {
            for (Faculty faculty : getAllFaculty()) {
                contacts.add(contactFromFaculty(faculty));
            }
        }
        if (audience == NotificationMessage.Audience.ALL || audience == NotificationMessage.Audience.ADMIN) {
            for (User user : getAllUsers()) {
                if ("Admin".equalsIgnoreCase(user.getRole())) {
                    contacts.add(contactFromUser(user));
                }
            }
        }
        return contacts;
    }

    private static void deliverNotificationStubs(List<ContactRecipient> contacts,
            NotificationRequest request,
            String message) {
        Set<String> emailed = new HashSet<>();
        Set<String> texted = new HashSet<>();
        for (ContactRecipient recipient : contacts) {
            if (request.isEmailChannel() && recipient.email() != null) {
                String emailKey = recipient.email().toLowerCase(Locale.ENGLISH);
                if (emailed.add(emailKey)) {
                    NotificationDeliveryService.sendEmailStub(recipient.email(), request.getCategory(), message);
                }
            }
            if (request.isSmsChannel() && recipient.phone() != null) {
                if (texted.add(recipient.phone())) {
                    NotificationDeliveryService.sendSmsStub(recipient.phone(), message);
                }
            }
        }
    }

    private static ContactRecipient contactFromStudent(Student student) {
        return new ContactRecipient(student.getEmail(), student.getPhone());
    }

    private static ContactRecipient contactFromFaculty(Faculty faculty) {
        return new ContactRecipient(faculty.getEmail(), faculty.getPhone());
    }

    private static ContactRecipient contactFromUser(User user) {
        return new ContactRecipient(user.getEmail(), null);
    }

    private static String resolveUserTargetId(User user) {
        Student student = findStudentByUsername(user.getUsername());
        if (student != null) {
            return student.getStudentId();
        }
        Faculty faculty = findFacultyByUsername(user.getUsername());
        if (faculty != null) {
            return faculty.getFacultyId();
        }
        return user.getUsername();
    }

    public static Map<String, Double> getGrades(String studentId, String sectionId) {
        try {
            EnrollmentRecord record = findEnrollmentRecord(sectionId, studentId);
            return record.getComponentScores();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static Double getFinalGrade(String studentId, String sectionId) {
        try {
            EnrollmentRecord record = findEnrollmentRecord(sectionId, studentId);
            return record.getFinalGrade();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String normalizeDepartment(String department) {
        return department == null ? "" : department.trim().toLowerCase(Locale.ENGLISH);
    }

    private static class ContactRecipient {
        private final String email;
        private final String phone;

        public ContactRecipient(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }

        public String email() {
            return email;
        }

        public String phone() {
            return phone;
        }
    }
}
