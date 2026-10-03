package com.fitly.app;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Runs before any protected page is served. If the visitor has no logged in
 * session, they are sent to the login page instead.
 */
public class SessionAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("user") != null;

        if (!loggedIn) {
            if (req.getRequestURI().startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                res.sendRedirect("/Login_page.html");
            }
            return;
        }
        chain.doFilter(request, response);
    }
}
