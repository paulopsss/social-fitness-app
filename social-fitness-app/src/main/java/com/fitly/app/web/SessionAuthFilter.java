package com.fitly.app.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * One link in the servlet filter chain (Chain of Responsibility).
 * It either stops the request with a redirect to login, or passes it on.
 * Other checks, such as a future admin check, are separate filters.
 */
public class SessionAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!LoginSession.isActive(req.getSession(false))) {
            res.sendRedirect(Pages.LOGIN);
            return;
        }
        chain.doFilter(request, response);
    }
}
