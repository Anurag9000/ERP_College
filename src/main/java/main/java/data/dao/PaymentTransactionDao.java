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
            (transaction_id, student_id, amount, paid_on, method, reference, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE payment_transactions
            SET student_id = ?, amount = ?, paid_on = ?, method = ?, reference = ?, notes = ?
            WHERE transaction_id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM payment_transactions WHERE transaction_id = ?";
    private static final String FIND_BY_STUDENT_SQL = """
            SELECT transaction_id, student_id, amount, paid_on, method, reference, notes
            FROM payment_transactions
            WHERE student_id = ?
            ORDER BY paid_on DESC, transaction_id
            """;
    private static final String FIND_ALL_SQL = """
            SELECT transaction_id, student_id, amount, paid_on, method, reference, notes
            FROM payment_transactions
            ORDER BY paid_on DESC, transaction_id
            """;

    public PaymentTransactionDao() {
        super(DataSourceRegistry.erpDataSource()
                .orElseThrow(() -> new IllegalStateException("ERP datasource not configured.")));
    }

    public void insert(PaymentTransaction transaction) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, transaction.getTransactionId());
            ps.setString(2, transaction.getStudentId());
            ps.setDouble(3, transaction.getAmount());
            ps.setDate(4, Date.valueOf(transaction.getPaidOn()));
            ps.setString(5, transaction.getMethod());
            ps.setString(6, transaction.getReference());
            ps.setString(7, transaction.getNotes());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to insert payment transaction {}: {}", transaction.getTransactionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to persist payment transaction", ex);
        }
    }

    public void update(PaymentTransaction transaction) {
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
            logger.error("Failed to update payment transaction {}: {}", transaction.getTransactionId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update payment transaction", ex);
        }
    }

    public void delete(String transactionId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, transactionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to delete payment transaction {}: {}", transactionId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete payment transaction", ex);
        }
    }

    public List<PaymentTransaction> findByStudent(String studentId) {
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
            tx.setStudentId(rs.getString("student_id"));
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
