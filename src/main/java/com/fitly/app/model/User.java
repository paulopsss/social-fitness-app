package com.fitly.app.model;

/**
 * A registered user.
 * The password is kept only as a hash, never as plain text.
 */
public record User(String email, String passwordHash) {
}
