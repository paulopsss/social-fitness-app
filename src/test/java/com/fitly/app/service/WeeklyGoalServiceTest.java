package com.fitly.app.service;

import com.fitly.app.persistence.entity.GoalEntity;
import com.fitly.app.persistence.entity.GoalStatus;
import com.fitly.app.persistence.entity.GoalType;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.repository.GoalJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import com.fitly.app.service.WeeklyGoalService.GoalProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyGoalServiceTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 11);
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    @Mock private UserJpaRepository users;
    @Mock private GoalJpaRepository goals;
    @Mock private WorkoutJpaRepository workouts;

    private WeeklyGoalService service;
    private UserEntity ann;

    @BeforeEach
    void setUp() {
        service = new WeeklyGoalService(users, goals, workouts);
        ann = Fixtures.user(1L, "ann@x.com");
    }

    private void minutesThisWeek(int... minutes) {
        List<com.fitly.app.persistence.entity.WorkoutEntity> list = new java.util.ArrayList<>();
        for (int m : minutes) {
            list.add(Fixtures.workout(1L, ann, "Running", WEDNESDAY, m, 0));
        }
        when(workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(eq(1L), any(), any())).thenReturn(list);
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 10, 11, 12, 13, 14, 15})
    void weekAlwaysStartsOnMonday(int dayOfNovember) {
        LocalDate start = WeeklyGoalService.weekStart(LocalDate.of(2026, 11, dayOfNovember));

        assertEquals(DayOfWeek.MONDAY, start.getDayOfWeek());
        assertEquals(MONDAY, start);
    }

    @Test
    void sundayBelongsToTheWeekThatStartedSixDaysEarlier() {
        assertEquals(MONDAY, WeeklyGoalService.weekStart(LocalDate.of(2026, 11, 15)));
        assertEquals(LocalDate.of(2026, 11, 16), WeeklyGoalService.weekStart(LocalDate.of(2026, 11, 16)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 10_081})
    void impossibleTargetsAreRejected(int target) {
        assertThrows(IllegalArgumentException.class, () -> service.setWeeklyMinutes("ann@x.com", target, WEDNESDAY));
    }

    @Test
    void progressIsMinutesDoneOverTarget() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(goals.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                .thenReturn(List.of(new GoalEntity(ann, GoalType.WEEKLY_MINUTES, 200, MONDAY)));
        minutesThisWeek(30, 20);

        GoalProgress progress = service.current("ann@x.com", WEDNESDAY).orElseThrow();

        assertEquals(50, progress.minutesDone());
        assertEquals(25, progress.percent());
        assertFalse(progress.completed());
        assertEquals(MONDAY, progress.weekStart());
        assertEquals(LocalDate.of(2026, 11, 15), progress.weekEnd());
    }

    @Test
    void goalIsCompletedAtOrAboveTargetAndPercentStopsAt100() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(goals.findByUserIdAndStatus(1L, GoalStatus.ACTIVE))
                .thenReturn(List.of(new GoalEntity(ann, GoalType.WEEKLY_MINUTES, 100, MONDAY)));
        minutesThisWeek(60, 70);

        GoalProgress progress = service.current("ann@x.com", WEDNESDAY).orElseThrow();

        assertTrue(progress.completed());
        assertEquals(100, progress.percent());
        assertEquals(130, progress.minutesDone());
    }

    @Test
    void noGoalMeansEmpty() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(goals.findByUserIdAndStatus(1L, GoalStatus.ACTIVE)).thenReturn(List.of());

        assertTrue(service.current("ann@x.com", WEDNESDAY).isEmpty());
    }

    @Test
    void aNewTargetReplacesTheOldOne() {
        GoalEntity old = new GoalEntity(ann, GoalType.WEEKLY_MINUTES, 100, MONDAY);
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(goals.findByUserIdAndStatus(1L, GoalStatus.ACTIVE)).thenReturn(List.of(old));
        when(goals.save(any(GoalEntity.class))).thenAnswer(call -> call.getArgument(0));
        minutesThisWeek();

        GoalProgress progress = service.setWeeklyMinutes("ann@x.com", 150, WEDNESDAY);

        assertEquals(GoalStatus.ABANDONED, old.getStatus());
        assertEquals(150, progress.targetMinutes());
        assertEquals(0, progress.minutesDone());
    }
}
