package com.fitly.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Maps post_likes: who liked which post. The composite key stops double likes. */
@Entity
@Table(name = "post_likes")
public class PostLikeEntity {

    @EmbeddedId
    private PostLikeId id;

    /** @MapsId fills the postId half of the key from this relationship. */
    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PostLikeEntity() {
    }

    public PostLikeEntity(PostEntity post, UserEntity user) {
        this.id = new PostLikeId(post.getId(), user.getId());
        this.post = post;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public PostLikeId getId() {
        return id;
    }

    public PostEntity getPost() {
        return post;
    }

    public UserEntity getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
