package com.fitly.app.service;

import com.fitly.app.persistence.entity.PostEntity;
import com.fitly.app.persistence.entity.PostLikeId;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.PostJpaRepository;
import com.fitly.app.persistence.repository.PostLikeJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The activity feed: your posts and your friends' posts, with likes. */
@Service
public class FeedService {

    private static final int FEED_SIZE = 50;

    /** One post as the feed shows it. */
    public record FeedItem(Long id, String authorEmail, String authorName, String content,
                           LocalDateTime createdAt, int likeCount, boolean likedByMe, boolean mine) {
    }

    private final UserJpaRepository users;
    private final FriendService friends;
    private final PostJpaRepository posts;
    private final PostLikeJpaRepository likes;
    private final PostLikeService likeService;

    public FeedService(UserJpaRepository users, FriendService friends, PostJpaRepository posts,
                       PostLikeJpaRepository likes, PostLikeService likeService) {
        this.users = users;
        this.friends = friends;
        this.posts = posts;
        this.likes = likes;
        this.likeService = likeService;
    }

    /** The newest posts from this user and their friends. */
    @Transactional(readOnly = true)
    public List<FeedItem> feed(String email) {
        UserEntity me = user(email);
        return posts.findByUserIdInOrderByCreatedAtDescIdDesc(visibleAuthorIds(email, me),
                        PageRequest.of(0, FEED_SIZE))
                .stream()
                .map(post -> item(post, me))
                .toList();
    }

    /** @return true if the like was added, false if it was already there */
    @Transactional
    public boolean like(String email, Long postId) {
        UserEntity me = user(email);
        requireVisible(email, me, postId);
        return likeService.like(postId, me.getId());
    }

    /** @return true if a like was removed, false if there was none */
    @Transactional
    public boolean unlike(String email, Long postId) {
        UserEntity me = user(email);
        requireVisible(email, me, postId);
        return likeService.unlike(postId, me.getId());
    }

    private void requireVisible(String email, UserEntity me, Long postId) {
        PostEntity post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        if (!visibleAuthorIds(email, me).contains(post.getUser().getId())) {
            throw new NotFoundException("Post not found");
        }
    }

    private Set<Long> visibleAuthorIds(String email, UserEntity me) {
        Set<Long> ids = new HashSet<>();
        ids.add(me.getId());
        friends.friends(email).forEach(friend -> ids.add(friend.userId()));
        return ids;
    }

    private FeedItem item(PostEntity post, UserEntity me) {
        UserEntity author = post.getUser();
        boolean liked = likes.existsById(new PostLikeId(post.getId(), me.getId()));
        return new FeedItem(post.getId(), author.getEmail(), author.getDisplayName(), post.getContent(),
                post.getCreatedAt(), post.getLikeCount(), liked, author.getId().equals(me.getId()));
    }

    private UserEntity user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Unknown user"));
    }
}
