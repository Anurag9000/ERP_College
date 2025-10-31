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

    private final String transactionId;
    private String studentId;
    private double amount;
    private LocalDate paidOn;
    private String method;
    private String reference;
    private String notes;

    public PaymentTransaction() {
        this.transactionId = UUID.randomUUID().toString();
        this.paidOn = LocalDate.now();
    }

    public PaymentTransaction(String studentId, double amount, LocalDate paidOn, String method, String reference, String notes) {
        this();
        this.studentId = studentId;
        this.amount = amount;
        if (paidOn != null) {
            this.paidOn = paidOn;
        }
        this.method = method;
        this.reference = reference;
        this.notes = notes;
    }

    public String getTransactionId() {
        return transactionId;
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

    public void setAmount(double amount) {
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
        if (this == o) return true;
        if (!(o instanceof PaymentTransaction)) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}
