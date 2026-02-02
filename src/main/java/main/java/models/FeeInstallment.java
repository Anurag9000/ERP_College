package main.java.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a scheduled tuition fee installment for a student.
 */
public class FeeInstallment implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        DUE,
        PAID,
        OVERDUE
    }

    private String installmentId;
    private String studentId;
    private LocalDate dueDate;
    private double amount;
    private double paidAmount;
    private Status status;
    private String description;
    private LocalDate paidOn;
    private LocalDate lastReminderSent;

    public FeeInstallment() {
        this(UUID.randomUUID().toString());
    }

    public FeeInstallment(String studentId, LocalDate dueDate, double amount, String description) {
        this(UUID.randomUUID().toString());
        this.studentId = studentId;
        this.dueDate = dueDate;
        this.amount = amount;
        this.description = description;
    }

    public FeeInstallment(String installmentId, String studentId, LocalDate dueDate,
            double amount, double paidAmount, Status status, String description,
            LocalDate paidOn, LocalDate lastReminderSent) {
        this(installmentId);
        this.studentId = studentId;
        this.dueDate = dueDate;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.status = status != null ? status : Status.DUE;
        this.description = description;
        this.paidOn = paidOn;
        this.lastReminderSent = lastReminderSent;
    }

    private FeeInstallment(String installmentId) {
        this.installmentId = installmentId;
        this.status = Status.DUE;
    }

    public static FeeInstallment copyOf(FeeInstallment source) {
        FeeInstallment copy = new FeeInstallment(source.installmentId);
        copy.studentId = source.studentId;
        copy.dueDate = source.dueDate;
        copy.amount = source.amount;
        copy.paidAmount = source.paidAmount;
        copy.status = source.status;
        copy.description = source.description;
        copy.paidOn = source.paidOn;
        copy.lastReminderSent = source.lastReminderSent;
        return copy;
    }

    public String getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(String installmentId) {
        this.installmentId = installmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public double getAmount() {
        return amount;
    }

    /**
     * Sets the installment amount.
     * 
     * @param amount the amount (must be greater than 0)
     * @throws IllegalArgumentException if amount is not positive
     */
    public void setAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Fee installment amount must be positive, got: " + amount);
        }
        this.amount = amount;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    /**
     * Sets the paid amount.
     * 
     * @param paidAmount the paid amount (must not be negative and not exceed total
     *                   amount)
     * @throws IllegalArgumentException if paidAmount is negative or exceeds amount
     */
    public void setPaidAmount(double paidAmount) {
        if (paidAmount < 0) {
            throw new IllegalArgumentException("Paid amount cannot be negative, got: " + paidAmount);
        }
        if (paidAmount > amount) {
            throw new IllegalArgumentException(
                    "Paid amount (" + paidAmount + ") cannot exceed installment amount (" + amount + ")");
        }
        this.paidAmount = paidAmount;
    }

    public double getRemainingAmount() {
        return Math.max(0, amount - paidAmount);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public LocalDate getLastReminderSent() {
        return lastReminderSent;
    }

    public void setLastReminderSent(LocalDate lastReminderSent) {
        this.lastReminderSent = lastReminderSent;
    }

    public boolean isOverdue(LocalDate today) {
        return status != Status.PAID && dueDate != null && dueDate.isBefore(today);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FeeInstallment))
            return false;
        FeeInstallment that = (FeeInstallment) o;
        return Objects.equals(installmentId, that.installmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(installmentId);
    }
}
