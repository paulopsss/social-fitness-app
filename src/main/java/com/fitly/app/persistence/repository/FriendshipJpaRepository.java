package com.fitly.app.persistence.repository;

import com.fitly.app.persistence.entity.FriendshipEntity;
import com.fitly.app.persistence.entity.FriendshipStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these methods from their names. */
public interface FriendshipJpaRepository extends JpaRepository<FriendshipEntity, Long> {

    /** Friend requests waiting for this user to answer. Uses index ix_friendships_addressee. */
    List<FriendshipEntity> findByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status);

    /** True if a request exists in either direction, so A->B and B->A are never both stored. */
    boolean existsByRequesterIdAndAddresseeIdOrRequesterIdAndAddresseeId(Long a, Long b, Long b2, Long a2);
}
