package com.fitly.app.service;

import com.fitly.app.repository.InMemoryUserRepository;
import com.fitly.app.repository.UserRepository;
import com.fitly.app.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain Java tests: no Spring, no web server. This is possible because
 * AuthService depends on interfaces (Dependency Inversion).
 */
class AuthServiceTest {

    private UserRepository repository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        authService = new AuthService(repository, new Pbkdf2PasswordHasher(1_000));
    }

    @Test
    void registerCreatesAnAccount() {
        assertTrue(authService.register("a@example.com", "secret123"));
        assertTrue(repository.existsByEmail("a@example.com"));
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        authService.register("a@example.com", "secret123");

        assertFalse(authService.register("a@example.com", "different"));
    }

    @Test
    void passwordIsStoredAsAHashNotAsPlainText() {
        authService.register("a@example.com", "secret123");

        String stored = repository.findByEmail("a@example.com").orElseThrow().passwordHash();
        assertNotEquals("secret123", stored);
    }

    @Test
    void authenticateAcceptsTheCorrectPassword() {
        authService.register("a@example.com", "secret123");

        assertTrue(authService.authenticate("a@example.com", "secret123"));
    }

    @Test
    void authenticateRejectsAWrongPassword() {
        authService.register("a@example.com", "secret123");

        assertFalse(authService.authenticate("a@example.com", "wrong"));
    }

    @Test
    void authenticateRejectsAnUnknownEmail() {
        assertFalse(authService.authenticate("nobody@example.com", "secret123"));
    }
}
