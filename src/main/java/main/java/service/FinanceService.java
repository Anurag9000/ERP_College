package main.java.service;

import main.java.data.dao.FeeInstallmentDao;
import main.java.data.dao.PaymentTransactionDao;
import main.java.models.FeeInstallment;
import main.java.models.PaymentTransaction;
import main.java.models.Student;
import main.java.utils.AuditLogService;
import main.java.utils.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service class for financial operations.
 */
public class FinanceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinanceService.class);

    private static final PaymentTransactionDao paymentTransactionDao = new PaymentTransactionDao();
    private static final FeeInstallmentDao feeInstallmentDao = new FeeInstallmentDao();
    private static final main.java.data.dao.StudentDao studentDao = new main.java.data.dao.StudentDao();

    public static synchronized PaymentTransaction recordPayment(String actorUsername,
            String studentId,
            double amount,
            String method,
            String reference,
            String notes) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        Student student = DatabaseUtil.getStudent(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }

        PaymentTransaction transaction = new PaymentTransaction(studentId, amount, LocalDate.now(), method, reference,
                notes);

        javax.sql.DataSource ds = main.java.config.DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP database connection not available."));

        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                paymentTransactionDao.insert(conn, transaction);

                // Update student's fees paid
                double updatedPaid = student.getFeesPaid() + amount;
                student.setFeesPaid(updatedPaid);
                studentDao.update(conn, student);

                // Allocate payment to installments
                List<FeeInstallment> schedule = feeInstallmentDao.findByStudent(studentId);
                schedule.sort(Comparator
                        .comparing(inst -> inst.getDueDate() == null ? LocalDate.MAX : inst.getDueDate()));

                double remaining = amount;
                final double EPSILON = 1e-9;

                for (FeeInstallment installment : schedule) {
                    if (installment.getStatus() == FeeInstallment.Status.PAID)
                        continue;

                    double due = installment.getRemainingAmount();
                    if (due <= EPSILON)
                        continue;

                    if (remaining >= due - EPSILON) {
                        installment.setPaidAmount(installment.getAmount());
                        installment.setStatus(FeeInstallment.Status.PAID);
                        installment.setPaidOn(LocalDate.now());
                        remaining -= due;
                    } else {
                        installment.setPaidAmount(installment.getPaidAmount() + remaining);
                        remaining = 0;
                    }
                    feeInstallmentDao.update(conn, installment);
                    if (remaining <= EPSILON)
                        break;
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException ex) {
            LOGGER.error("Transaction failed for payment: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Payment processing failed due to database error", ex);
        }

        AuditLogService.log(AuditLogService.EventType.FINANCE_PAYMENT,
                actorUsername != null ? actorUsername : "system",
                String.format(Locale.ENGLISH, "Recorded payment %.2f for %s", amount, studentId));

        return transaction;
    }

    public static List<PaymentTransaction> getPaymentHistory(String studentId) {
        return paymentTransactionDao.findByStudent(studentId);
    }

    public static List<FeeInstallment> getInstallments(String studentId) {
        return feeInstallmentDao.findByStudent(studentId);
    }

    public static void addInstallment(String studentId, FeeInstallment installment) {
        installment.setStudentId(studentId);
        if (installment.getInstallmentId() == null || installment.getInstallmentId().isBlank()) {
            installment.setInstallmentId(UUID.randomUUID().toString());
        }
        feeInstallmentDao.insert(installment);
    }
}
