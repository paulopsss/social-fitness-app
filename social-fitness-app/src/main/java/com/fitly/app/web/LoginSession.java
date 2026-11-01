package com.fitly.app.web;

import jakarta.servlet.http.HttpSession;

/**
 * The one place that knows how "logged in" is stored in the session.
 * Before this class existed, the controller and the filter each repeated
 * the string "user", and they had to stay in sync by hand.
 */
public final class LoginSession {

    private static final String USER_KEY = "user";

    private LoginSession() {
    }

    public static void start(HttpSession session, String email) {
        session.setAttribute(USER_KEY, email);
    }

    /** The session can be null when the visitor has never had one. */
    public static boolean isActive(HttpSession session) {
        return session != null && session.getAttribute(USER_KEY) != null;
    }

    public static void end(HttpSession session) {
        session.invalidate();
    }
}
