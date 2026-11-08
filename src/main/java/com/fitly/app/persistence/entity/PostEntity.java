package com.fitly.app.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Maps the posts table: a feed entry, optionally about one workout. */
@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** Optional (0..1): a post does not have to be about a workout. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private WorkoutEntity workout;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Cached copy of the number of rows in post_likes for this post (intentional denormalization, V2).
     * Only PostLikeService changes it, in the same transaction as the like itself.
     */
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CommentEntity> comments = new ArrayList<>();

    protected PostEntity() {
    }

    public PostEntity(UserEntity user, String content) {
        this.user = user;
        this.content = content;
    }

    /** A post about a workout must be written by the person who did that workout. */
    public PostEntity(UserEntity user, WorkoutEntity workout, String content) {
        this(user, content);
        if (workout != null && !workout.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("A user can only post about their own workout");
        }
        this.workout = workout;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public CommentEntity addComment(UserEntity author, String text) {
        CommentEntity comment = new CommentEntity(this, author, text);
        comments.add(comment);
        return comment;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public WorkoutEntity getWorkout() {
        return workout;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public List<CommentEntity> getComments() {
        return comments;
    }
}
