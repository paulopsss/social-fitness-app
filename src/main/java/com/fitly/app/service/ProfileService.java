package com.fitly.app.service;

import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and edits the public profile: display name and bio. */
@Service
public class ProfileService {

    private static final int MAX_NAME = 50;
    private static final int MAX_BIO = 500;

    /** What the profile page shows. */
    public record ProfileView(String email, String displayName, String bio) {
    }

    private final UserJpaRepository users;

    public ProfileService(UserJpaRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ProfileView get(String email) {
        return view(user(email));
    }

    /** Saves the new name and bio. Blank text clears the field. */
    @Transactional
    public ProfileView update(String email, String displayName, String bio) {
        String name = clean(displayName, MAX_NAME, "displayName");
        String about = clean(bio, MAX_BIO, "bio");
        UserEntity user = user(email);
        user.setDisplayName(name);
        user.setBio(about);
        return view(user);
    }

    private static String clean(String text, int max, String field) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (text.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return text.strip();
    }

    private static ProfileView view(UserEntity user) {
        return new ProfileView(user.getEmail(), user.getDisplayName(), user.getBio());
    }

    private UserEntity user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Unknown user"));
    }
}
