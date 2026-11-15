package com.fitly.app.web;

/** Every page path in one place, so a renamed page is a one line change. */
public final class Pages {

    public static final String HOME = "/";
    public static final String LOGIN = "/Login_page.html";
    public static final String REGISTER = "/Register_page.html";
    public static final String DASHBOARD = "/Main_page.html";
    public static final String ERROR_QUERY = "?error=1";

    private static final String REDIRECT_PREFIX = "redirect:";

    private Pages() {
    }

    /** The Spring MVC view name that redirects the browser to the given path. */
    public static String redirectTo(String path) {
        return REDIRECT_PREFIX + path;
    }
}
