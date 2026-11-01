package com.fitly.app.config;

import com.fitly.app.web.Pages;
import com.fitly.app.web.SessionAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    /**
     * Add every page that requires login to this list.
     * Public pages (index, Login_page, Register_page) stay out of it.
     */
    @Bean
    public FilterRegistrationBean<SessionAuthFilter> sessionAuthFilter() {
        FilterRegistrationBean<SessionAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SessionAuthFilter());
        registration.addUrlPatterns(Pages.DASHBOARD);
        return registration;
    }
}
