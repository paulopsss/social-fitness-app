package com.fitly.app.model;

import java.time.LocalDate;

/** One logged workout (Use Case 2: Log Workout). */
public record Workout(String userEmail, String type, int durationMinutes, int calories, LocalDate date) {

    /** Rejects bad data at the door so every later step can trust a Workout. */
    public Workout {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than 0");
        }
        if (calories < 0) {
            throw new IllegalArgumentException("calories cannot be negative");
        }
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
    }
}
