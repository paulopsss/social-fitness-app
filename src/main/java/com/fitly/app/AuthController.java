package com.fitly.app;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserStore userStore;

    public AuthController(UserStore userStore) {
        this.userStore = userStore;
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session) {
        if (userStore.checkCredentials(email, password)) {
            session.setAttribute("user", email);
            return "redirect:/Main_page.html";
        }
        return "redirect:/Login_page.html?error=1";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session) {
        if (userStore.exists(email)) {
            return "redirect:/Register_page.html?error=1";
        }
        userStore.addUser(email, password);
        session.setAttribute("user", email);
        return "redirect:/Main_page.html";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
