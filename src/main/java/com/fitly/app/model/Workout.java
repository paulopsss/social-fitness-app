package com.fitly.app.model;

import java.time.LocalDate;

/** One logged workout (Use Case 2: Log Workout). */
public record Workout(String userEmail, String type, int durationMinutes, int calories, LocalDate date) {

    /** Rejects bad data at the door so every later step can trust a Workout. */
    public Workout {
        requireText(userEmail, "userEmail");
        requireText(type, "type");
        require(durationMinutes > 0, "durationMinutes must be greater than 0");
        require(calories >= 0, "calories cannot be negative");
        require(date != null, "date is required");
    }

    private static void requireText(String value, String name) {
        require(value != null && !value.isBlank(), name + " is required");
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
