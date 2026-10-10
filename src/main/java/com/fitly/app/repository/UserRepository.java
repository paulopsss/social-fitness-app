package com.fitly.app.repository;

import com.fitly.app.model.User;

import java.util.Optional;

/**
 * Repository pattern + Dependency Inversion.
 *
 * The login code talks to this interface and never to a map or a database.
 * Today InMemoryUserRepository implements it. When the team moves to H2
 * (ADR 5), a JPA based class can implement it instead and nothing above
 * this layer has to change.
 *
 * The interface is deliberately small: every method is used by AuthService.
 */
public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    void save(User user);
}
