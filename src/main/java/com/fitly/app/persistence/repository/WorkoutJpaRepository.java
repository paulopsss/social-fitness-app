package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.WorkoutEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface WorkoutJpaRepository extends JpaRepository<WorkoutEntity, Long> {

    /** Uses index ix_workouts_user_date. Feeds weekly goal progress. */
    /** All workouts of one user, newest first. */
    List<WorkoutEntity> findByUserIdOrderByWorkoutDateDescIdDesc(Long userId);

    List<WorkoutEntity> findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(Long userId, LocalDate from, LocalDate to);
}
