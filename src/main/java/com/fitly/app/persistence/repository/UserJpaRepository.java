package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /** The user with this email, or empty when there is none. */
    Optional<UserEntity> findByEmail(String email);

    /** True when a user with this email exists. */
    boolean existsByEmail(String email);
}
