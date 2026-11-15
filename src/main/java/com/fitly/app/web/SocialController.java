package com.fitly.app.web;

import com.fitly.app.service.FeedService;
import com.fitly.app.service.FeedService.FeedItem;
import com.fitly.app.service.FriendService;
import com.fitly.app.service.FriendService.FriendView;
import com.fitly.app.service.FriendService.RequestView;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** HTTP for friends, the activity feed and likes. */
@RestController
@RequestMapping("/api")
public class SocialController {

    /** The JSON for a friend request: the email of the person to add. */
    public record FriendRequest(String email) {
    }

    private final FriendService friends;
    private final FeedService feed;

    public SocialController(FriendService friends, FeedService feed) {
        this.friends = friends;
        this.feed = feed;
    }

    /** The user's friends. */
    @GetMapping("/friends")
    public List<FriendView> friends(HttpSession session) {
        return friends.friends(CurrentUser.email(session));
    }

    /** Requests other people sent to the user that are waiting for an answer. */
    @GetMapping("/friends/requests")
    public List<RequestView> requests(HttpSession session) {
        return friends.pendingRequests(CurrentUser.email(session));
    }

    /** Sends a friend request to the person with this email. */
    @PostMapping("/friends/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendRequest(@RequestBody FriendRequest body, HttpSession session) {
        friends.sendRequest(CurrentUser.email(session), body.email());
    }

    /** Accepts a request that was sent to the user. */
    @PostMapping("/friends/requests/{id}/accept")
    public void accept(@PathVariable Long id, HttpSession session) {
        friends.respond(CurrentUser.email(session), id, true);
    }

    /** Declines a request that was sent to the user. */
    @PostMapping("/friends/requests/{id}/decline")
    public void decline(@PathVariable Long id, HttpSession session) {
        friends.respond(CurrentUser.email(session), id, false);
    }

    /** The user's own posts and their friends' posts, newest first. */
    @GetMapping("/feed")
    public List<FeedItem> feed(HttpSession session) {
        return feed.feed(CurrentUser.email(session));
    }

    /** Likes a post. The answer says whether the like was new. */
    @PostMapping("/posts/{id}/like")
    public Map<String, Boolean> like(@PathVariable Long id, HttpSession session) {
        return Map.of("changed", feed.like(CurrentUser.email(session), id));
    }

    /** Removes the user's like from a post. */
    @DeleteMapping("/posts/{id}/like")
    public Map<String, Boolean> unlike(@PathVariable Long id, HttpSession session) {
        return Map.of("changed", feed.unlike(CurrentUser.email(session), id));
    }
}
