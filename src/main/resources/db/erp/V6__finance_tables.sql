CREATE TABLE IF NOT EXISTS fee_installments (
    installment_id VARCHAR(64) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    due_date DATE NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    description VARCHAR(255) NULL,
    paid_on DATE NULL,
    last_reminder_sent DATE NULL,
    CONSTRAINT fk_fee_installments_student FOREIGN KEY (student_id)
        REFERENCES students(student_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    paid_on DATE NOT NULL,
    method VARCHAR(64) NULL,
    reference VARCHAR(64) NULL,
    notes VARCHAR(255) NULL,
    CONSTRAINT fk_payment_transactions_student FOREIGN KEY (student_id)
        REFERENCES students(student_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_fee_installments_student ON fee_installments(student_id, due_date);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_student ON payment_transactions(student_id, paid_on DESC);
