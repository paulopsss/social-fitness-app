package com.fitly.app.service;

import com.fitly.app.model.Workout;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reacts to a workout by adding its minutes to that user's total for the
 * week the workout happened in (weeks follow the ISO calendar, Monday first).
 * Comparing this total with a WeeklyGoal target comes with the goals feature.
 */
@Component
public class GoalProgressObserver implements WorkoutObserver {

    private final Map<WeekKey, Integer> minutesByWeek = new ConcurrentHashMap<>();

    @Override
    public void onWorkoutLogged(Workout workout) {
        minutesByWeek.merge(WeekKey.of(workout.userEmail(), workout.date()),
                workout.durationMinutes(), Integer::sum);
    }

    /** Total minutes the user logged in the week that contains the given date. */
    public int minutesInWeekOf(String userEmail, LocalDate dateInWeek) {
        return minutesByWeek.getOrDefault(WeekKey.of(userEmail, dateInWeek), 0);
    }

    private record WeekKey(String userEmail, int weekBasedYear, int week) {
        static WeekKey of(String userEmail, LocalDate date) {
            return new WeekKey(userEmail,
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        }
    }
}
