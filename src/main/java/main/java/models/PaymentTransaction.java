package main.java.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single fee payment made by a student.
 */
public class PaymentTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String studentId;
    private double amount;
    private LocalDate paidOn;
    private String method;
    private String reference;
    private String notes;

    public PaymentTransaction() {
        this(UUID.randomUUID().toString());
    }

    public PaymentTransaction(String studentId, double amount, LocalDate paidOn,
            String method, String reference, String notes) {
        this(UUID.randomUUID().toString());
        this.studentId = studentId;
        this.amount = amount;
        this.paidOn = paidOn != null ? paidOn : LocalDate.now();
        this.method = method;
        this.reference = reference;
        this.notes = notes;
    }

    public PaymentTransaction(String transactionId, String studentId, double amount,
            LocalDate paidOn, String method, String reference, String notes) {
        this(transactionId);
        this.studentId = studentId;
        this.amount = amount;
        this.paidOn = paidOn != null ? paidOn : LocalDate.now();
        this.method = method;
        this.reference = reference;
        this.notes = notes;
    }

    private PaymentTransaction(String transactionId) {
        this.transactionId = transactionId;
        this.paidOn = LocalDate.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public double getAmount() {
        return amount;
    }

    /**
     * Sets the payment amount.
     * 
     * @param amount the amount (must be greater than 0)
     * @throws IllegalArgumentException if amount is not positive
     */
    public void setAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive, got: " + amount);
        }
        this.amount = amount;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PaymentTransaction))
            return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}
