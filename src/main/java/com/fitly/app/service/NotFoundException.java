package com.fitly.app.service;

/** Thrown when something does not exist, or belongs to someone else. The web layer turns it into HTTP 404. */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
