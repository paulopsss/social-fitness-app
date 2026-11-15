package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.PostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Spring Data writes the SQL for these methods from their names, or from the @Query text. */
public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {

    /** Uses index ix_posts_user_created. */
    /** The newest posts written by any of these users. */
    List<PostEntity> findByUserIdInOrderByCreatedAtDescIdDesc(Collection<Long> userIds, Pageable page);

    /** The post that was created for this workout, if there is one. */
    Optional<PostEntity> findByWorkoutId(Long workoutId);

    List<PostEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Changes the cached like_count by +1 or -1 in one UPDATE statement, with no read first. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PostEntity p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    int adjustLikeCount(@Param("postId") Long postId, @Param("delta") int delta);
}
