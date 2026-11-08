package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.GoalEntity;
import com.fitly.app.persistence.entity.GoalStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface GoalJpaRepository extends JpaRepository<GoalEntity, Long> {

    /** Uses index ix_goals_user_status. */
    List<GoalEntity> findByUserIdAndStatus(Long userId, GoalStatus status);
}
