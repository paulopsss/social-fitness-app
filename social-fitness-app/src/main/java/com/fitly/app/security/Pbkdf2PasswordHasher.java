package com.fitly.app.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The strategy used today: PBKDF2 with HMAC-SHA256 and a random salt.
 * Uses only the Java standard library, so no new dependency is needed.
 *
 * Stored format: pbkdf2$iterations$salt$hash
 * The iteration count is saved inside each hash, so old hashes still verify
 * after the default count is raised.
 */
@Component
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    /** Used by Spring. */
    public Pbkdf2PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /** Lets tests use a small count so they run fast. */
    public Pbkdf2PasswordHasher(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be greater than 0");
        }
        this.iterations = iterations;
    }

    @Override
    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(rawPassword, salt, iterations);
        return PREFIX + "$" + iterations + "$" + encode(salt) + "$" + encode(key);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(rawPassword, salt, storedIterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // Bad number, bad Base64, or an unusable salt: not a match.
            return false;
        }
    }

    private static byte[] derive(String rawPassword, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
