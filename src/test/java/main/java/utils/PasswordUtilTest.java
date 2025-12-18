package main.java.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    void testHashPassword() {
        char[] password = "TestPassword@123".toCharArray();
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        assertNotNull(hash);
        assertNotEquals(new String(password), hash);
        assertTrue(PasswordUtil.verifyPassword(password, salt, hash));
    }

    @Test
    void testVerifyPasswordInvalid() {
        char[] password = "TestPassword@123".toCharArray();
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        assertFalse(PasswordUtil.verifyPassword("WrongPassword".toCharArray(), salt, hash));
    }
}
