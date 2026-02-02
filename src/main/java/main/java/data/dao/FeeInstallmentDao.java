package main.java.data.dao;

import main.java.models.FeeInstallment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for fee installment schedules.
 */
public class FeeInstallmentDao extends BaseDao {
    private static final String INSERT_SQL = """
            INSERT INTO fee_installments
            (installment_id, student_code, due_date, amount, paid_amount, status, description, paid_on, last_reminder_sent)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE fee_installments
            SET student_code = ?, due_date = ?, amount = ?, paid_amount = ?, status = ?, description = ?, paid_on = ?, last_reminder_sent = ?
            WHERE installment_id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM fee_installments WHERE installment_id = ?";
    private static final String FIND_BY_STUDENT_SQL = """
            SELECT installment_id, student_code, due_date, amount, paid_amount, status, description, paid_on, last_reminder_sent
            FROM fee_installments
            WHERE student_code = ?
            ORDER BY due_date, installment_id
            """;
    private static final String FIND_ALL_SQL = """
            SELECT installment_id, student_code, due_date, amount, paid_amount, status, description, paid_on, last_reminder_sent
            FROM fee_installments
            ORDER BY due_date, installment_id
            """;

    public FeeInstallmentDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Inserts a fee installment into the database.
     * 
     * @param installment the fee installment to insert (must not be null with valid
     *                    data)
     * @throws IllegalArgumentException if installment is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public void insert(FeeInstallment installment) {
        if (installment == null) {
            throw new IllegalArgumentException("Fee installment cannot be null");
        }
        if (installment.getInstallmentId() == null || installment.getInstallmentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Installment ID cannot be null or empty");
        }
        if (installment.getStudentId() == null || installment.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        if (installment.getAmount() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (installment.getPaidAmount() < 0) {
            throw new IllegalArgumentException("Paid amount cannot be negative");
        }
        if (installment.getStatus() == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setString(1, installment.getInstallmentId());
            ps.setString(2, installment.getStudentId());
            if (installment.getDueDate() != null) {
                ps.setDate(3, Date.valueOf(installment.getDueDate()));
            } else {
                ps.setNull(3, java.sql.Types.DATE);
            }
            ps.setDouble(4, installment.getAmount());
            ps.setDouble(5, installment.getPaidAmount());
            ps.setString(6, installment.getStatus().name());
            ps.setString(7, installment.getDescription());
            if (installment.getPaidOn() != null) {
                ps.setDate(8, Date.valueOf(installment.getPaidOn()));
            } else {
                ps.setNull(8, java.sql.Types.DATE);
            }
            if (installment.getLastReminderSent() != null) {
                ps.setDate(9, Date.valueOf(installment.getLastReminderSent()));
            } else {
                ps.setNull(9, java.sql.Types.DATE);
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to insert installment {}: {}", installment.getInstallmentId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to persist fee installment", ex);
        }
    }

    /**
     * Updates an existing fee installment.
     * 
     * @param installment the fee installment to update (must not be null with valid
     *                    data)
     * @return true if update was successful, false otherwise
     * @throws IllegalArgumentException if installment is null or has invalid data
     * @throws IllegalStateException    if database operation fails
     */
    public boolean update(FeeInstallment installment) {
        if (installment == null) {
            throw new IllegalArgumentException("Fee installment cannot be null");
        }
        if (installment.getInstallmentId() == null || installment.getInstallmentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Installment ID cannot be null or empty");
        }
        try (Connection conn = getConnection()) {
            return update(conn, installment);
        } catch (SQLException ex) {
            logger.error("Failed to update installment {}: {}", installment.getInstallmentId(), ex.getMessage(), ex);
            throw new IllegalStateException("Unable to update fee installment", ex);
        }
    }

    public boolean update(Connection conn, FeeInstallment installment) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setString(1, installment.getStudentId());
            if (installment.getDueDate() != null) {
                ps.setDate(2, Date.valueOf(installment.getDueDate()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setDouble(3, installment.getAmount());
            ps.setDouble(4, installment.getPaidAmount());
            ps.setString(5, installment.getStatus().name());
            ps.setString(6, installment.getDescription());
            if (installment.getPaidOn() != null) {
                ps.setDate(7, Date.valueOf(installment.getPaidOn()));
            } else {
                ps.setNull(7, java.sql.Types.DATE);
            }
            if (installment.getLastReminderSent() != null) {
                ps.setDate(8, Date.valueOf(installment.getLastReminderSent()));
            } else {
                ps.setNull(8, java.sql.Types.DATE);
            }
            ps.setString(9, installment.getInstallmentId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a fee installment by ID.
     * 
     * @param installmentId the installment ID (must not be null or empty)
     * @throws IllegalArgumentException if installmentId is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public void delete(String installmentId) {
        if (installmentId == null || installmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Installment ID cannot be null or empty");
        }
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setString(1, installmentId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Failed to delete installment {}: {}", installmentId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to delete fee installment", ex);
        }
    }

    /**
     * Retrieves all fee installments for a specific student.
     * 
     * @param studentId the student ID (must not be null or empty)
     * @return list of fee installments, never null
     * @throws IllegalArgumentException if studentId is null or empty
     * @throws IllegalStateException    if database operation fails
     */
    public List<FeeInstallment> findByStudent(String studentId) {
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
            logger.error("Failed to load installments for {}: {}", studentId, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to load fee installments", ex);
        }
    }

    public List<FeeInstallment> findAll() {
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_ALL_SQL);
                ResultSet rs = ps.executeQuery()) {
            return mapResult(rs);
        } catch (SQLException ex) {
            logger.error("Failed to load installments: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unable to load fee installments", ex);
        }
    }

    private List<FeeInstallment> mapResult(ResultSet rs) throws SQLException {
        List<FeeInstallment> list = new ArrayList<>();
        while (rs.next()) {
            FeeInstallment installment = new FeeInstallment();
            installment.setInstallmentId(rs.getString("installment_id"));
            installment.setStudentId(rs.getString("student_code"));
            Date due = rs.getDate("due_date");
            installment.setDueDate(due != null ? due.toLocalDate() : null);
            installment.setAmount(rs.getDouble("amount"));
            installment.setPaidAmount(rs.getDouble("paid_amount"));
            installment.setStatus(FeeInstallment.Status.valueOf(rs.getString("status")));
            installment.setDescription(rs.getString("description"));
            Date paid = rs.getDate("paid_on");
            installment.setPaidOn(paid != null ? paid.toLocalDate() : null);
            Date reminder = rs.getDate("last_reminder_sent");
            installment.setLastReminderSent(reminder != null ? reminder.toLocalDate() : null);
            list.add(installment);
        }
        return list;
    }
}
