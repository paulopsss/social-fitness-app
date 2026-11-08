package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.WorkoutExerciseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface WorkoutExerciseJpaRepository extends JpaRepository<WorkoutExerciseEntity, Long> {
}
