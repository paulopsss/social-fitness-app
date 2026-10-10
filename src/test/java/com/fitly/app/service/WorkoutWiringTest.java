package com.fitly.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that Spring really passes both observers into WorkoutService,
 * with no registration code anywhere.
 */
@SpringBootTest
class WorkoutWiringTest {

    @Autowired
    private WorkoutService workoutService;

    @Autowired
    private GoalProgressObserver goals;

    @Autowired
    private NotificationObserver notifications;

    @Test
    void springInjectsTheObserversIntoTheService() {
        LocalDate day = LocalDate.of(2026, 9, 28);
        String email = "wiring-test@example.com";   // unique, because the beans are shared

        workoutService.logWorkout(email, "Run", 30, 300, day);

        assertEquals(30, goals.minutesInWeekOf(email, day));
        assertEquals(1, notifications.messagesFor(email).size());
    }
}
