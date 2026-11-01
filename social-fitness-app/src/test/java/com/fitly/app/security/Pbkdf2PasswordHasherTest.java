package com.fitly.app.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pbkdf2PasswordHasherTest {

    // A small iteration count keeps the tests fast. Production uses the default.
    private final PasswordHasher hasher = new Pbkdf2PasswordHasher(1_000);

    @Test
    void hashDoesNotContainThePlainPassword() {
        assertFalse(hasher.hash("secret123").contains("secret123"));
    }

    @Test
    void sameInputGivesDifferentHashesBecauseOfTheSalt() {
        assertNotEquals(hasher.hash("secret123"), hasher.hash("secret123"));
    }

    @Test
    void correctPasswordMatchesAndWrongPasswordDoesNot() {
        String stored = hasher.hash("secret123");

        assertTrue(hasher.matches("secret123", stored));
        assertFalse(hasher.matches("wrong", stored));
    }

    @Test
    void unreadableStoredValuesReturnFalseInsteadOfThrowing() {
        assertFalse(hasher.matches("secret123", "garbage"));
        assertFalse(hasher.matches("secret123", "pbkdf2$notanumber$AAAA$AAAA"));
        assertFalse(hasher.matches("secret123", "pbkdf2$1000$!!!$!!!"));
        assertFalse(hasher.matches("secret123", null));
    }

    @Test
    void oldHashesStillVerifyAfterTheIterationCountChanges() {
        String storedWithOldSetting = new Pbkdf2PasswordHasher(1_000).hash("secret123");
        PasswordHasher newerHasher = new Pbkdf2PasswordHasher(2_000);

        assertTrue(newerHasher.matches("secret123", storedWithOldSetting));
    }

    @Test
    void iterationCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Pbkdf2PasswordHasher(0));
    }
}
