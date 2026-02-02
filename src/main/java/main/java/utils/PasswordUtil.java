package main.java.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * PBKDF2-based password hashing helper to emulate UNIX shadow-style storage.
 */
public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2.
     * 
     * @param password the password (must not be null)
     * @param salt     the salt (must not be null or empty)
     * @return the hashed password
     * @throws IllegalArgumentException if parameters are null
     */
    public static String hashPassword(char[] password, String salt) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(password, Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    /**
     * Verifies a password against an expected hash.
     * 
     * @param candidate    the candidate password (must not be null)
     * @param salt         the salt (must not be null or empty)
     * @param expectedHash the expected hash (must not be null or empty)
     * @return true if password matches
     * @throws IllegalArgumentException if parameters are null
     */
    public static boolean verifyPassword(char[] candidate, String salt, String expectedHash) {
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate password cannot be null");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt cannot be null or empty");
        }
        if (expectedHash == null || expectedHash.isEmpty()) {
            throw new IllegalArgumentException("Expected hash cannot be null or empty");
        }
        String candidateHash = hashPassword(candidate, salt);
        return constantTimeEquals(candidateHash, expectedHash);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
