package main.java.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordPolicyTest {

    @Test
    void testValidPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validateComplexity("Valid@12345"));
    }

    @Test
    void testTooShort() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validateComplexity("Sh0rt@"));
    }

    @Test
    void testNoUppercase() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validateComplexity("nouppercas3@123"));
    }

    @Test
    void testNoLowercase() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validateComplexity("NOLOWERCASE@123"));
    }

    @Test
    void testNoDigit() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validateComplexity("NoDigits@Special"));
    }

    @Test
    void testNoSpecialChar() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validateComplexity("NoSpecialChar123"));
    }
}
