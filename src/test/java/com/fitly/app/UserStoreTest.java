package com.fitly.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStoreTest {

    @Test
    void newUserCanLogIn() {
        UserStore store = new UserStore();
        store.addUser("test@example.com", "password123");
        assertTrue(store.checkCredentials("test@example.com", "password123"));
    }

    @Test
    void wrongPasswordFailsLogin() {
        UserStore store = new UserStore();
        store.addUser("test@example.com", "password123");
        assertFalse(store.checkCredentials("test@example.com", "wrong"));
    }

    @Test
    void unknownEmailFailsLogin() {
        UserStore store = new UserStore();
        assertFalse(store.checkCredentials("nobody@example.com", "password123"));
    }
}
