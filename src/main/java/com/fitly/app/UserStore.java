package com.fitly.app;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary in-memory user storage. Users disappear when the app restarts.
 * TODO: replace with a real database and hash passwords (for example BCrypt)
 * before storing them. Plain text passwords are for early testing only.
 */
@Component
public class UserStore {

    private final Map<String, String> users = new ConcurrentHashMap<>();

    public boolean exists(String email) {
        return users.containsKey(email);
    }

    public void addUser(String email, String password) {
        users.put(email, password);
    }

    public boolean checkCredentials(String email, String password) {
        String stored = users.get(email);
        return stored != null && stored.equals(password);
    }
}
