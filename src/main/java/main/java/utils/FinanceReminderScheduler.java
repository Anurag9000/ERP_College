package main.java.utils;

import main.java.models.FeeInstallment;
import main.java.models.Student;
import main.java.models.NotificationMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically inspects finance data to send payment reminders and daily digests.
 */
public final class FinanceReminderScheduler {
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "finance-reminder-scheduler");
                t.setDaemon(true);
                return t;
            });
    private static final int REMINDER_LOOKAHEAD_DAYS = 7;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static volatile boolean started = false;

    private FinanceReminderScheduler() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        EXECUTOR.scheduleAtFixedRate(FinanceReminderScheduler::runCycle, 0, 1, TimeUnit.HOURS);
    }

    public static synchronized void stop() {
        if (!started) {
            return;
        }
        EXECUTOR.shutdownNow();
        started = false;
    }

    private static void runCycle() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate reminderThreshold = today.plusDays(REMINDER_LOOKAHEAD_DAYS);

            for (Student student : DatabaseUtil.getAllStudents()) {
                processInstallmentReminders(student, today, reminderThreshold);
                processDailyDigest(student, today);
            }
        } catch (Exception ex) {
            System.err.println("Finance reminder scheduler error: " + ex.getMessage());
        }
    }

    private static void processInstallmentReminders(Student student, LocalDate today, LocalDate threshold) {
        List<FeeInstallment> installments = DatabaseUtil.getInstallmentsForStudent(student.getStudentId());
        for (FeeInstallment installment : installments) {
            if (installment.getStatus() == FeeInstallment.Status.PAID) {
                continue;
            }
            LocalDate due = installment.getDueDate();
            if (due == null) {
                continue;
            }
            if (due.isBefore(today) || !due.isAfter(threshold)) {
                LocalDate lastReminder = installment.getLastReminderSent();
                if (lastReminder != null && !lastReminder.isBefore(today)) {
                    continue;
                }
                double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
                String message = String.format(Locale.ENGLISH,
                        "Upcoming installment of \u20B9%,.0f due by %s. Outstanding balance: \u20B9%,.0f.",
                        installment.getAmount(),
                        DATE_FORMATTER.format(due),
                        outstanding);
                DatabaseUtil.addNotification(new NotificationMessage(
                        NotificationMessage.Audience.STUDENT,
                        student.getStudentId(),
                        message,
                        "Finance Reminder"));
                DatabaseUtil.markInstallmentReminderSent(student.getStudentId(), installment.getInstallmentId());
            }
        }
    }

    private static void processDailyDigest(Student student, LocalDate today) {
        double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
        if (outstanding <= 0.0) {
            return;
        }
        String settingKey = "finance.digest.lastSent." + student.getStudentId();
        String lastSentRaw = DatabaseUtil.getSetting(settingKey);
        LocalDate lastSent = null;
        if (lastSentRaw != null && !lastSentRaw.isBlank()) {
            try {
                lastSent = LocalDate.parse(lastSentRaw);
            } catch (Exception ignored) {
                lastSent = null;
            }
        }
        if (lastSent != null && !lastSent.isBefore(today)) {
            return;
        }
        FeeInstallment next = DatabaseUtil.nextDueInstallment(student.getStudentId());
        String nextDueText = next != null && next.getDueDate() != null
                ? DATE_FORMATTER.format(next.getDueDate())
                : "Not scheduled";
        String message = String.format(Locale.ENGLISH,
                "Daily finance digest: Outstanding balance \u20B9%,.0f. Next due date: %s.",
                outstanding,
                nextDueText);
        DatabaseUtil.addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                student.getStudentId(),
                message,
                "Finance Digest"));
        DatabaseUtil.setSetting(settingKey, today.toString());
    }
}
