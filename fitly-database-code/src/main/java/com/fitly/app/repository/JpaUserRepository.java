package com.fitly.app.repository;

import com.fitly.app.model.User;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The H2 backed UserRepository (ADR 5), replacing InMemoryUserRepository in the running app.
 *
 * This is the payoff of the Repository pattern from the design patterns assignment:
 * AuthService did not change at all. It still sees the small UserRepository interface,
 * and this class translates between the User record and the UserEntity table mapping.
 *
 * The unique constraint uq_users_email now guarantees one account per email,
 * even if two registrations race past existsByEmail at the same moment.
 */
@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(e -> new User(e.getEmail(), e.getPasswordHash()));
    }

    @Override
    public void save(User user) {
        UserEntity entity = jpa.findByEmail(user.email())
                .orElseGet(() -> new UserEntity(user.email(), user.passwordHash()));
        entity.setPasswordHash(user.passwordHash());
        jpa.save(entity);
    }
}
