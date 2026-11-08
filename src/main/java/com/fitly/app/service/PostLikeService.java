package com.fitly.app.service;

import com.fitly.app.persistence.entity.PostEntity;
import com.fitly.app.persistence.entity.PostLikeEntity;
import com.fitly.app.persistence.entity.PostLikeId;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.PostJpaRepository;
import com.fitly.app.persistence.repository.PostLikeJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only code allowed to change posts.like_count, the denormalized counter.
 * Each method writes the post_likes row and the counter in one transaction,
 * so either both change or neither does.
 */
@Service
public class PostLikeService {

    private final PostJpaRepository posts;
    private final PostLikeJpaRepository likes;
    private final UserJpaRepository users;

    public PostLikeService(PostJpaRepository posts, PostLikeJpaRepository likes, UserJpaRepository users) {
        this.posts = posts;
        this.likes = likes;
        this.users = users;
    }

    /** @return true if the like was added, false if this user already liked the post */
    @Transactional
    public boolean like(Long postId, Long userId) {
        PostLikeId id = new PostLikeId(postId, userId);
        if (likes.existsById(id)) {
            return false;
        }
        PostEntity post = posts.getReferenceById(postId);
        UserEntity user = users.getReferenceById(userId);
        likes.save(new PostLikeEntity(post, user));
        posts.adjustLikeCount(postId, 1);
        return true;
    }

    /** @return true if a like was removed, false if there was none */
    @Transactional
    public boolean unlike(Long postId, Long userId) {
        PostLikeId id = new PostLikeId(postId, userId);
        if (!likes.existsById(id)) {
            return false;
        }
        likes.deleteById(id);
        posts.adjustLikeCount(postId, -1);
        return true;
    }
}
