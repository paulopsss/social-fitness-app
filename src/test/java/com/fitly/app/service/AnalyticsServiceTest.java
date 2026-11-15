package com.fitly.app.service;

import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import com.fitly.app.service.AnalyticsService.Summary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Mock private UserJpaRepository users;
    @Mock private WorkoutJpaRepository workouts;

    private AnalyticsService service;
    private UserEntity ann;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(users, workouts);
        ann = Fixtures.user(1L, "ann@x.com");
    }

    @Test
    void totalsAndActiveDaysAreAddedUp() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(1L, FROM, TO)).thenReturn(List.of(
                Fixtures.workout(1L, ann, "Running", LocalDate.of(2026, 11, 2), 30, 300),
                Fixtures.workout(2L, ann, "Running", LocalDate.of(2026, 11, 2), 20, 200),
                Fixtures.workout(3L, ann, "Cycling", LocalDate.of(2026, 11, 4), 60, 500)));

        Summary summary = service.summary("ann@x.com", FROM, TO);

        assertEquals(3, summary.totalWorkouts());
        assertEquals(110, summary.totalMinutes());
        assertEquals(1000, summary.totalCalories());
        assertEquals(2, summary.activeDays());
    }

    @Test
    void biggestWorkoutTypeComesFirst() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(1L, FROM, TO)).thenReturn(List.of(
                Fixtures.workout(1L, ann, "Running", FROM, 30, 0),
                Fixtures.workout(2L, ann, "Cycling", FROM, 60, 0)));

        Summary summary = service.summary("ann@x.com", FROM, TO);

        assertEquals(List.of("Cycling", "Running"), List.copyOf(summary.minutesByType().keySet()));
    }

    @Test
    void noWorkoutsGivesZeros() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(1L, FROM, TO)).thenReturn(List.of());

        Summary summary = service.summary("ann@x.com", FROM, TO);

        assertEquals(0, summary.totalWorkouts());
        assertEquals(0, summary.activeDays());
        assertEquals(0, summary.minutesByType().size());
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.summary("ann@x.com", TO, FROM));
    }

    @Test
    void unknownUserIsNotFound() {
        when(users.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.summary("ghost@x.com", FROM, TO));
    }
}
