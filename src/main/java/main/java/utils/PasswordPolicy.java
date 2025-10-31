package main.java.utils;

import java.util.regex.Pattern;

/**
 * Validates password complexity and history rules.
 */
public final class PasswordPolicy {
    private static final int MIN_LENGTH = 10;
    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");
    private static final int HISTORY_COUNT = 5;

    private PasswordPolicy() {
    }

    public static void validateComplexity(String candidate) {
        if (candidate == null || candidate.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (!UPPER.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase character.");
        }
        if (!LOWER.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase character.");
        }
        if (!DIGIT.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Password must contain at least one digit.");
        }
        if (!SPECIAL.matcher(candidate).matches()) {
            throw new IllegalArgumentException("Password must contain at least one special character.");
        }
    }

    public static int historySize() {
        return HISTORY_COUNT;
    }
}
