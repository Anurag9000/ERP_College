package main.java.service;

import main.java.models.User;
import main.java.utils.DatabaseUtil;
import main.java.data.AuthUserDao;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminServiceTest {

    @Test
    void testEnsureAdminSuccess() {
        User admin = new User("admin", "hash", "salt", "Admin", "Admin User", "admin@test.com");
        assertDoesNotThrow(() -> AdminService.ensureAdmin(admin));
    }

    @Test
    void testEnsureAdminFailure() {
        User student = new User("stu1", "hash", "salt", "Student", "Student User", "stu@test.com");
        assertThrows(SecurityException.class, () -> AdminService.ensureAdmin(student));
    }

    @Test
    void testCreateUserAsAdmin() {
        User admin = new User("admin", "hash", "salt", "Admin", "Admin User", "admin@test.com");
        User newUser = new User("newuser", "hash", "salt", "Student", "New User", "new@test.com");

        AuthUserDao mockDao = mock(AuthUserDao.class);
        DatabaseUtil.setAuthUserDao(mockDao);

        when(mockDao.findByUsername("newuser")).thenReturn(Optional.empty());
        when(mockDao.insert(any(User.class))).thenReturn(newUser);

        User created = AdminService.createUser(admin, "newuser", "Student", "New User", "new@test.com",
                "NewUser@12345!");

        assertNotNull(created);
        assertEquals("newuser", created.getUsername());
        verify(mockDao).insert(any(User.class));
    }

    @Test
    void testCreateUserAsNonAdmin() {
        User student = new User("stu1", "hash", "salt", "Student", "Student User", "stu@test.com");
        assertThrows(SecurityException.class, () -> AdminService.createUser(student, "hacker", "Admin", "Hacker",
                "hacker@test.com", "Hacker@12345!"));
    }
}
