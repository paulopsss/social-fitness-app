package com.fitly.app.web;

import com.fitly.app.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Only translates between HTTP and the AuthService: read the form, ask the
 * service, start or end the session, pick the redirect. The rules live in
 * AuthService (Single Responsibility).
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Checks the form details; on success starts a session and opens the dashboard. */
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session) {
        if (authService.authenticate(email, password)) {
            LoginSession.start(session, email);
            return Pages.redirectTo(Pages.DASHBOARD);
        }
        return Pages.redirectTo(Pages.LOGIN + Pages.ERROR_QUERY);
    }

    /** Creates the account; on success logs the new user in and opens the dashboard. */
    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session) {
        if (!authService.register(email, password)) {
            return Pages.redirectTo(Pages.REGISTER + Pages.ERROR_QUERY);
        }
        LoginSession.start(session, email);
        return Pages.redirectTo(Pages.DASHBOARD);
    }

    /** Ends the session and returns to the landing page. */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        LoginSession.end(session);
        return Pages.redirectTo(Pages.HOME);
    }
}
