package com.fitly.app.service;

import com.fitly.app.model.Workout;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Observer pattern tests. WorkoutService is tested with tiny fake observers,
 * with no GoalService or NotificationService needed.
 */
class WorkoutServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 28);

    /** A fake observer that just remembers what it was told. */
    private static class RecordingObserver implements WorkoutObserver {
        final List<Workout> seen = new ArrayList<>();

        @Override
        public void onWorkoutLogged(Workout workout) {
            seen.add(workout);
        }
    }

    @Test
    void everyObserverIsToldAboutALoggedWorkout() {
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();
        WorkoutService service = new WorkoutService(List.of(first, second));

        Workout logged = service.logWorkout("a@example.com", "Run", 30, 300, MONDAY);

        assertEquals(List.of(logged), first.seen);
        assertEquals(List.of(logged), second.seen);
    }

    @Test
    void anObserverAddedLaterIsToldAboutLaterWorkouts() {
        WorkoutService service = new WorkoutService(List.of());
        RecordingObserver late = new RecordingObserver();
        service.subscribe(late);

        service.logWorkout("a@example.com", "Run", 30, 300, MONDAY);

        assertEquals(1, late.seen.size());
    }

    @Test
    void anInvalidWorkoutIsRejectedAndNobodyIsNotified() {
        RecordingObserver observer = new RecordingObserver();
        WorkoutService service = new WorkoutService(List.of(observer));

        assertThrows(IllegalArgumentException.class,
                () -> service.logWorkout("a@example.com", "Run", 0, 300, MONDAY));
        assertTrue(observer.seen.isEmpty());
    }

    @Test
    void goalProgressAddsUpMinutesWithinTheSameWeekOnly() {
        GoalProgressObserver goals = new GoalProgressObserver();
        WorkoutService service = new WorkoutService(List.of(goals));

        service.logWorkout("a@example.com", "Run", 30, 300, MONDAY);                       // Mon
        service.logWorkout("a@example.com", "Lift", 45, 250, MONDAY.plusDays(4));          // Fri, same week
        service.logWorkout("a@example.com", "Walk", 20, 90, MONDAY.plusDays(7));           // next Monday
        service.logWorkout("b@example.com", "Run", 60, 500, MONDAY);                       // other user

        assertEquals(75, goals.minutesInWeekOf("a@example.com", MONDAY));
        assertEquals(20, goals.minutesInWeekOf("a@example.com", MONDAY.plusDays(7)));
        assertEquals(60, goals.minutesInWeekOf("b@example.com", MONDAY));
        assertEquals(0, goals.minutesInWeekOf("nobody@example.com", MONDAY));
    }

    @Test
    void notificationObserverCreatesAMessageForTheRightUser() {
        NotificationObserver notifications = new NotificationObserver();
        WorkoutService service = new WorkoutService(List.of(notifications));

        service.logWorkout("a@example.com", "Run", 30, 300, MONDAY);

        assertEquals(List.of("Workout logged: Run, 30 min"), notifications.messagesFor("a@example.com"));
        assertTrue(notifications.messagesFor("b@example.com").isEmpty());
    }
}
