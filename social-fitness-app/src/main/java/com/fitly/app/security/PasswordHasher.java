package com.fitly.app.security;

/**
 * Strategy pattern + Open/Closed Principle.
 *
 * How a password is turned into a stored value is a decision that changes
 * over time. AuthService only knows this interface, so a new algorithm is a
 * new class and AuthService stays untouched.
 *
 * Contract (every implementation must follow it):
 * - hash() never returns the raw password.
 * - matches() returns false, and does not throw, for a wrong password or a
 *   stored value it cannot understand.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String storedHash);
}
