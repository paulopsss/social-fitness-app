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

    /** True when an account with this email already exists. */
    boolean existsByEmail(String email);

    /** The account with this email, or empty when there is none. */
    Optional<User> findByEmail(String email);

    /** Stores a new account, or replaces the one with the same email. */
    void save(User user);
}
