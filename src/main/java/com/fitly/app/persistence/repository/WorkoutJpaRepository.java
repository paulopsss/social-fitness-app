package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.WorkoutEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface WorkoutJpaRepository extends JpaRepository<WorkoutEntity, Long> {

    /** Uses index ix_workouts_user_date. Feeds weekly goal progress. */
    List<WorkoutEntity> findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(Long userId, LocalDate from, LocalDate to);
}
