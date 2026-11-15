package com.fitly.app.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Gives API controllers the logged in user's email, or answers 401 when nobody is logged in. */
final class CurrentUser {

    private CurrentUser() {
    }

    static String email(HttpSession session) {
        String email = LoginSession.email(session);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Log in first");
        }
        return email;
    }
}
