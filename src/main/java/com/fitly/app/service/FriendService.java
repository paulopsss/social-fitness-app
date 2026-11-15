package com.fitly.app.service;

import com.fitly.app.persistence.entity.FriendshipEntity;
import com.fitly.app.persistence.entity.FriendshipStatus;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.FriendshipJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Friend requests and friendships (Use Case 3: Add Friend). */
@Service
public class FriendService {

    /** A person in someone's friend list. */
    public record FriendView(Long userId, String email, String displayName) {
    }

    /** A friend request waiting for an answer. */
    public record RequestView(Long id, String fromEmail, String fromName) {
    }

    private final UserJpaRepository users;
    private final FriendshipJpaRepository friendships;

    public FriendService(UserJpaRepository users, FriendshipJpaRepository friendships) {
        this.users = users;
        this.friendships = friendships;
    }

    /** Sends a request. Fails if the person does not exist, is yourself, or a request already exists. */
    @Transactional
    public void sendRequest(String fromEmail, String toEmail) {
        UserEntity from = user(fromEmail);
        String target = toEmail == null ? "" : toEmail.strip();
        if (from.getEmail().equals(target)) {
            throw new IllegalArgumentException("You cannot add yourself");
        }
        UserEntity to = users.findByEmail(target)
                .orElseThrow(() -> new NotFoundException("No user with that email"));
        if (friendships.existsByRequesterIdAndAddresseeIdOrRequesterIdAndAddresseeId(
                from.getId(), to.getId(), to.getId(), from.getId())) {
            throw new IllegalArgumentException("A request or friendship already exists");
        }
        friendships.save(new FriendshipEntity(from, to));
    }

    /** Requests other people sent to this user that nobody has answered yet. */
    @Transactional(readOnly = true)
    public List<RequestView> pendingRequests(String email) {
        return friendships.findByAddresseeIdAndStatus(user(email).getId(), FriendshipStatus.PENDING)
                .stream()
                .map(f -> new RequestView(f.getId(), f.getRequester().getEmail(),
                        f.getRequester().getDisplayName()))
                .toList();
    }

    /** Accepts or declines a request. Only the person it was sent to can answer, once. */
    @Transactional
    public void respond(String email, Long friendshipId, boolean accept) {
        FriendshipEntity friendship = friendships.findById(friendshipId)
                .filter(f -> f.getAddressee().getEmail().equals(email))
                .filter(f -> f.getStatus() == FriendshipStatus.PENDING)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        friendship.respond(accept);
    }

    /** The people this user is friends with. */
    @Transactional(readOnly = true)
    public List<FriendView> friends(String email) {
        Long me = user(email).getId();
        return friendships.findForUser(me, FriendshipStatus.ACCEPTED).stream()
                .map(f -> f.getRequester().getId().equals(me) ? f.getAddressee() : f.getRequester())
                .map(u -> new FriendView(u.getId(), u.getEmail(), u.getDisplayName()))
                .toList();
    }

    private UserEntity user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Unknown user"));
    }
}
