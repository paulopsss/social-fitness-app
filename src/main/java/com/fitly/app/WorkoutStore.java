package com.fitly.app;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorkoutStore {

    private final Map<Long, Workout> workouts = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<String, Integer> goals = new ConcurrentHashMap<>();
    public Workout add(String owner, String type, int minutes, LocalDate date, String notes) {
        Workout workout = new Workout(nextId.getAndIncrement(), owner, type, minutes, date, notes);
        workouts.put(workout.id(), workout);
        return workout;
    }

    public List<Workout> forUser(String owner) {
        return workouts.values().stream()
                .filter(w -> w.owner().equals(owner))
                .toList();
    }

    /** Returns empty if the workout doesn't exist or belongs to someone else. */
    public Optional<Workout> update(long id, String owner, String type, int minutes,
                                    LocalDate date, String notes) {
        Workout[] result = new Workout[1];
        workouts.computeIfPresent(id, (key, old) -> {
            if (!old.owner().equals(owner)) {
                return old;
            }
            result[0] = new Workout(id, owner, type, minutes, date, notes);
            return result[0];
        });
        return Optional.ofNullable(result[0]);
    }

    /** Returns false if the workout doesn't exist or belongs to someone else. */
    public boolean delete(long id, String owner) {
        boolean[] deleted = new boolean[1];
        workouts.computeIfPresent(id, (key, old) -> {
            if (!old.owner().equals(owner)) {
                return old;
            }
            deleted[0] = true;
            return null;
        });
        return deleted[0];
    }

    /** How many of this user's workouts are dated from "from" to "to", both included. */
    public int countBetween(String owner, LocalDate from, LocalDate to) {
        return (int) workouts.values().stream()
                .filter(w -> w.owner().equals(owner))
                .filter(w -> !w.date().isBefore(from) && !w.date().isAfter(to))
                .count();
    }

    /** Workouts per week the user wants. 0 means no goal set. */
    public int getGoal(String owner) {
        return goals.getOrDefault(owner, 0);
    }

    public void setGoal(String owner, int goal) {
        goals.put(owner, goal);
    }
}