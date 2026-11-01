package com.fitly.app.service;

import com.fitly.app.model.Workout;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Reacts to a workout by creating a confirmation message for that user. */
@Component
public class NotificationObserver implements WorkoutObserver {

    private final Map<String, List<String>> inboxes = new ConcurrentHashMap<>();

    @Override
    public void onWorkoutLogged(Workout workout) {
        String message = "Workout logged: " + workout.type()
                + ", " + workout.durationMinutes() + " min";
        inboxes.computeIfAbsent(workout.userEmail(), email -> new CopyOnWriteArrayList<>())
                .add(message);
    }

    public List<String> messagesFor(String userEmail) {
        return List.copyOf(inboxes.getOrDefault(userEmail, List.of()));
    }
}
