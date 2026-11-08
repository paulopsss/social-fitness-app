package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.WorkoutTypeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface WorkoutTypeJpaRepository extends JpaRepository<WorkoutTypeEntity, Long> {

    Optional<WorkoutTypeEntity> findByName(String name);
}
