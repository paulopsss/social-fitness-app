package com.fitly.app.service;

import com.fitly.app.persistence.entity.WorkoutEntity;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Usage and fitness numbers for a period: workouts, minutes, calories, active days. */
@Service
public class AnalyticsService {

    /** The totals for one user over a period. minutesByType lists the biggest type first. */
    public record Summary(LocalDate from, LocalDate to, int totalWorkouts, int totalMinutes,
                          int totalCalories, int activeDays, Map<String, Integer> minutesByType) {
    }

    private final UserJpaRepository users;
    private final WorkoutJpaRepository workouts;

    public AnalyticsService(UserJpaRepository users, WorkoutJpaRepository workouts) {
        this.users = users;
        this.workouts = workouts;
    }

    /** Totals for the workouts between the two dates, both included. */
    @Transactional(readOnly = true)
    public Summary summary(String email, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date");
        }
        Long userId = users.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Unknown user")).getId();
        List<WorkoutEntity> found =
                workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(userId, from, to);
        Set<LocalDate> days = new HashSet<>();
        Map<String, Integer> byType = new TreeMap<>();
        int minutes = 0;
        int calories = 0;
        for (WorkoutEntity workout : found) {
            days.add(workout.getWorkoutDate());
            minutes += workout.getDurationMinutes();
            calories += workout.getCalories();
            byType.merge(workout.getWorkoutType().getName(), workout.getDurationMinutes(), Integer::sum);
        }
        return new Summary(from, to, found.size(), minutes, calories, days.size(), biggestFirst(byType));
    }

    private static Map<String, Integer> biggestFirst(Map<String, Integer> byType) {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        byType.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }
}
