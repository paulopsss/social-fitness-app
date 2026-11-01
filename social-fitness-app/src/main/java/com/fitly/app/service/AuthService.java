package com.fitly.app.service;

import com.fitly.app.model.User;
import com.fitly.app.repository.UserRepository;
import com.fitly.app.security.PasswordHasher;
import org.springframework.stereotype.Service;

/**
 * The login and registration rules, with no HTTP in sight (Single Responsibility).
 *
 * Both collaborators are interfaces (Dependency Inversion), so this class can
 * be tested with plain Java and works unchanged when storage moves to H2 or
 * the hashing algorithm changes.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /** @return true if the account was created, false if the email is already taken */
    public boolean register(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            return false;
        }
        userRepository.save(new User(email, passwordHasher.hash(password)));
        return true;
    }

    /** @return true only if the email exists and the password matches */
    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> passwordHasher.matches(password, user.passwordHash()))
                .orElse(false);
    }
}
