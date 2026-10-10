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

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session) {
        if (authService.authenticate(email, password)) {
            LoginSession.start(session, email);
            return "redirect:" + Pages.DASHBOARD;
        }
        return "redirect:" + Pages.LOGIN + Pages.ERROR_QUERY;
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session) {
        if (!authService.register(email, password)) {
            return "redirect:" + Pages.REGISTER + Pages.ERROR_QUERY;
        }
        LoginSession.start(session, email);
        return "redirect:" + Pages.DASHBOARD;
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        LoginSession.end(session);
        return "redirect:" + Pages.HOME;
    }
}
