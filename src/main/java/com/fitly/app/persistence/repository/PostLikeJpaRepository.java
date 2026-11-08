package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.PostLikeEntity;
import com.fitly.app.persistence.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface PostLikeJpaRepository extends JpaRepository<PostLikeEntity, PostLikeId> {

    long countByPostId(Long postId);
}
