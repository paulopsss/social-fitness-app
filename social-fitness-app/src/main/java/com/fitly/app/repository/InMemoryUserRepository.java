package com.fitly.app.repository;

import com.fitly.app.model.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary storage: users vanish when the app restarts.
 * TODO: replace with an H2 backed implementation of UserRepository.
 * TODO: with a database, enforce one account per email using a unique
 * constraint, so existsByEmail followed by save cannot race.
 */
@Repository
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
