package com.fitly.app.repository;

import com.fitly.app.model.User;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps users in a map. No longer used by the running app, which now uses
 * JpaUserRepository (H2). Kept because it lets unit tests such as
 * AuthServiceTest run in plain Java with no database.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public boolean existsByEmail(String email) {
        return users.containsKey(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(users.get(email));
    }

    @Override
    public void save(User user) {
        users.put(user.email(), user);
    }
}
