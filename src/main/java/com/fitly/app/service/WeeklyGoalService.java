package com.fitly.app.service;

import com.fitly.app.persistence.entity.GoalEntity;
import com.fitly.app.persistence.entity.GoalStatus;
import com.fitly.app.persistence.entity.GoalType;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.entity.WorkoutEntity;
import com.fitly.app.persistence.repository.GoalJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Weekly minutes goal and progress toward it (ISO weeks, Monday first).
 * Progress is worked out from the logged workouts each time, never stored,
 * so it cannot drift out of step with them.
 */
@Service
public class WeeklyGoalService {

    private static final int MINUTES_PER_WEEK = 7 * 24 * 60;
    private static final int PERCENT = 100;

    /** Progress for the week that contains today. */
    public record GoalProgress(Long id, int targetMinutes, int minutesDone, int percent,
                               boolean completed, LocalDate weekStart, LocalDate weekEnd) {
    }

    private final UserJpaRepository users;
    private final GoalJpaRepository goals;
    private final WorkoutJpaRepository workouts;

    public WeeklyGoalService(UserJpaRepository users, GoalJpaRepository goals,
                             WorkoutJpaRepository workouts) {
        this.users = users;
        this.goals = goals;
        this.workouts = workouts;
    }

    /** Sets the weekly target in minutes. A new target replaces the one before it. */
    @Transactional
    public GoalProgress setWeeklyMinutes(String email, int targetMinutes, LocalDate today) {
        if (targetMinutes <= 0 || targetMinutes > MINUTES_PER_WEEK) {
            throw new IllegalArgumentException(
                    "targetMinutes must be between 1 and " + MINUTES_PER_WEEK);
        }
        UserEntity user = user(email);
        for (GoalEntity old : activeGoals(user)) {
            old.finish(GoalStatus.ABANDONED, today);
        }
        GoalEntity goal = goals.save(
                new GoalEntity(user, GoalType.WEEKLY_MINUTES, targetMinutes, weekStart(today)));
        return progress(user, goal, today);
    }

    /** Progress toward the current goal, or empty when no goal is set. */
    @Transactional(readOnly = true)
    public Optional<GoalProgress> current(String email, LocalDate today) {
        UserEntity user = user(email);
        return activeGoals(user).stream().findFirst().map(goal -> progress(user, goal, today));
    }

    /** The Monday of the week that contains the date. */
    static LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private List<GoalEntity> activeGoals(UserEntity user) {
        return goals.findByUserIdAndStatus(user.getId(), GoalStatus.ACTIVE).stream()
                .filter(goal -> goal.getGoalType() == GoalType.WEEKLY_MINUTES)
                .toList();
    }

    private GoalProgress progress(UserEntity user, GoalEntity goal, LocalDate today) {
        LocalDate start = weekStart(today);
        LocalDate end = start.plusDays(DayOfWeek.values().length - 1L);
        int done = workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(user.getId(), start, end)
                .stream().mapToInt(WorkoutEntity::getDurationMinutes).sum();
        int target = goal.getTargetValue();
        int percent = Math.min(PERCENT, done * PERCENT / target);
        return new GoalProgress(goal.getId(), target, done, percent, done >= target, start, end);
    }

    private UserEntity user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Unknown user"));
    }
}
