package main.java.data.dao;

import main.java.config.DataSourceRegistry;
import main.java.models.PaymentTransaction;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for reading and persisting payment transactions.
 */
public class PaymentTransactionDao extends BaseDao {
    private static final String INSERT_SQL = """
            INSERT INTO payment_transactions
            (transaction_id, student_code, amount, paid_on, method, reference, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE payment_transactions
            SET student_code = ?, amount = ?, paid_on = ?, method = ?, reference = ?, notes = ?
            WHERE transaction_id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM payment_transactions WHERE transaction_id = ?";
    private static final String FIND_BY_STUDENT_SQL = """
            SELECT transaction_id, student_code, amount, paid_on, method, reference, notes
            FROM payment_transactions
            WHERE student_code = ?
            ORDER BY paid_on DESC, transaction_id
            """;
    private static final String FIND_ALL_SQL = """
            SELECT transaction_id, student_code, amount, paid_on, method, reference, notes
            FROM payment_transactions
            ORDER BY paid_on DESC, transaction_id
            """;

    public PaymentTransactionDao() {
        super(DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Inserts a new payment transaction.
     * 
     * @param transaction the payment transaction to insert (must not be null with
     *                    valid data)
     * @throws IllegalArgumentException if transaction is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void insert(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Payment transaction cannot be null");
        }
        if (transaction.getTransactionId() == null || transaction.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (transaction.getStudentId() == null || transaction.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        if (transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        if (transaction.getPaidOn() == null) {
            throw new IllegalArgumentException("Payment date cannot be null");
        }
        try (Connection conn = getConnection()) {
            insert(conn, transaction);
        } catch (SQLException ex) {
            logger.error("Failed to insert payment transaction {}: {}", transaction.getTransactionId(), ex.getMessage(),
                    ex);
            throw new IllegalStateException("Unable to persist payment transaction", ex);
        }
    }

    public void insert(Connection conn, PaymentTransaction transaction) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, transaction.getTransactionId());
            ps.setString(2, transaction.getStudentId());
            ps.setDouble(3, transaction.getAmount());
            ps.setDate(4, Date.valueOf(transaction.getPaidOn()));
            ps.setString(5, transaction.getMethod());
            ps.setString(6, transaction.getReference());
            ps.setString(7, transaction.getNotes());
            ps.executeUpdate();
        }
    }

    /**
     * Updates an existing payment transaction.
     * 
     * @param transaction the payment transaction to update (must not be null with
     *                    valid data)
     * @throws IllegalArgumentException if transaction is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void update(PaymentTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Payment transaction cannot be null");
        }
        if (transaction.getTransactionId() == null || transaction.getTransactionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, transaction.getStudentId());
            ps.setDouble(2, transaction.getAmount());
            ps.setDate(3, Date.valueOf(transaction.getPaidOn()));
            ps.setString(4, transaction.getMethod());
            ps.setString(5, transaction.getReference());
            ps.setString(6, transaction.getNotes());
            ps.setString(7, transaction.getTransactionId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to update payment transaction {}: {}", transaction.getTransactionId(), ex.getMessage(),
                    ex);
            throw new IllegalStateException("Unable to update payment transaction", ex);
        }
    }

    /**
     * Deletes a payment transaction by ID.
     * 
     * @param transactionId the transaction ID (must not be null or empty)
     * @throws IllegalArgumentException if transactionId is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public void delete(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to delete payment transaction {}: {}", transactionId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete payment transaction", ex);
        }
    }

    /**
     * Finds all payment transactions for a specific student.
     * 
     * @param studentId the student ID (must not be null or empty)
     * @return list of payment transactions, never null
     * @throws IllegalArgumentException if studentId is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public List<PaymentTransaction> findByStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_BY_STUDENT_SQL)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapResult(rs);
            }
        } catch (SQLException ex) {
            logger.error("Failed to load payment transactions for {}: {}", studentId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to load payment transactions", ex);
        }
    }

    public List<PaymentTransaction> findAll() {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_ALL_SQL);
                ResultSet rs = ps.executeQuery()) {
            return mapResult(rs);
        } catch (SQLException ex) {
            logger.error("Failed to load payment transactions: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to load payment transactions", ex);
        }
    }

    private List<PaymentTransaction> mapResult(ResultSet rs) throws SQLException {
        List<PaymentTransaction> list = new ArrayList<>();
        while (rs.next()) {
            PaymentTransaction tx = new PaymentTransaction();
            tx.setTransactionId(rs.getString("transaction_id"));
            tx.setStudentId(rs.getString("student_code"));
            tx.setAmount(rs.getDouble("amount"));
            Date paidOn = rs.getDate("paid_on");
            tx.setPaidOn(paidOn != null ? paidOn.toLocalDate() : LocalDate.now());
            tx.setMethod(rs.getString("method"));
            tx.setReference(rs.getString("reference"));
            tx.setNotes(rs.getString("notes"));
            list.add(tx);
        }
        return list;
    }
}
