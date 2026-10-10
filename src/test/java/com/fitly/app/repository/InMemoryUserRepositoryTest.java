package com.fitly.app.repository;

import com.fitly.app.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUserRepositoryTest {

    @Test
    void savedUserCanBeFound() {
        UserRepository repository = new InMemoryUserRepository();
        repository.save(new User("a@example.com", "hash"));

        assertTrue(repository.existsByEmail("a@example.com"));
        assertEquals("hash", repository.findByEmail("a@example.com").orElseThrow().passwordHash());
    }

    @Test
    void unknownEmailIsNotFound() {
        UserRepository repository = new InMemoryUserRepository();

        assertFalse(repository.existsByEmail("nobody@example.com"));
        assertTrue(repository.findByEmail("nobody@example.com").isEmpty());
    }
}
