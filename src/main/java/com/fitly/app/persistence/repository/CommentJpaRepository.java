package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {
}
