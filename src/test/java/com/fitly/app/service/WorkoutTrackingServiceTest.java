package com.fitly.app.service;

import com.fitly.app.persistence.entity.PostEntity;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.entity.WorkoutEntity;
import com.fitly.app.persistence.repository.PostJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import com.fitly.app.persistence.repository.WorkoutTypeJpaRepository;
import com.fitly.app.service.WorkoutTrackingService.WorkoutView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutTrackingServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Mock private UserJpaRepository users;
    @Mock private WorkoutTypeJpaRepository types;
    @Mock private WorkoutJpaRepository workouts;
    @Mock private PostJpaRepository posts;
    @Mock private WorkoutService observers;

    private WorkoutTrackingService service;
    private UserEntity ann;

    @BeforeEach
    void setUp() {
        service = new WorkoutTrackingService(users, types, workouts, posts, observers);
        ann = Fixtures.user(1L, "ann@x.com");
    }

    private void annAndRunningExist() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(types.findByName("Running")).thenReturn(Optional.of(Fixtures.type("Running")));
    }

    @Test
    void loggingSavesTheWorkoutAndItsPostAndNotifiesObservers() {
        annAndRunningExist();

        WorkoutView view = service.log("ann@x.com", "Running", 30, 250, DAY, " easy ");

        ArgumentCaptor<PostEntity> post = ArgumentCaptor.forClass(PostEntity.class);
        verify(workouts).save(any(WorkoutEntity.class));
        verify(posts).save(post.capture());
        verify(observers).logWorkout("ann@x.com", "Running", 30, 250, DAY);
        assertEquals("Logged Running for 30 min (250 kcal)", post.getValue().getContent());
        assertEquals("easy", view.notes());
        assertEquals(30, view.durationMinutes());
    }

    @Test
    void badMinutesAreRejectedBeforeAnythingIsSaved() {
        annAndRunningExist();

        assertThrows(IllegalArgumentException.class, () -> service.log("ann@x.com", "Running", 0, 0, DAY, null));

        verify(workouts, never()).save(any());
        verify(posts, never()).save(any());
    }

    @Test
    void unknownWorkoutTypeIsRejected() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(types.findByName("Nope")).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.log("ann@x.com", "Nope", 10, 0, DAY, null));

        assertEquals("Unknown workout type: Nope", e.getMessage());
    }

    @Test
    void notesLongerThan500CharactersAreRejected() {
        annAndRunningExist();

        assertThrows(IllegalArgumentException.class,
                () -> service.log("ann@x.com", "Running", 10, 0, DAY, "x".repeat(501)));
    }

    @Test
    void blankNotesAreStoredAsNothing() {
        annAndRunningExist();

        assertEquals(null, service.log("ann@x.com", "Running", 10, 0, DAY, "   ").notes());
    }

    @Test
    void editingSomeoneElsesWorkoutLooksLikeItDoesNotExist() {
        UserEntity ben = Fixtures.user(2L, "ben@x.com");
        when(workouts.findById(5L)).thenReturn(Optional.of(Fixtures.workout(5L, ben, "Running", DAY, 30, 0)));

        assertThrows(NotFoundException.class,
                () -> service.update("ann@x.com", 5L, "Running", 10, 0, DAY, null));
    }

    @Test
    void editingChangesTheWorkoutAndRewritesItsPost() {
        WorkoutEntity workout = Fixtures.workout(5L, ann, "Running", DAY, 30, 250);
        PostEntity post = new PostEntity(ann, workout, "old text");
        when(workouts.findById(5L)).thenReturn(Optional.of(workout));
        when(types.findByName("Cycling")).thenReturn(Optional.of(Fixtures.type("Cycling")));
        when(posts.findByWorkoutId(5L)).thenReturn(Optional.of(post));

        WorkoutView view = service.update("ann@x.com", 5L, "Cycling", 45, 400, DAY, null);

        assertEquals("Cycling", view.type());
        assertEquals(45, workout.getDurationMinutes());
        assertEquals("Logged Cycling for 45 min (400 kcal)", post.getContent());
    }

    @Test
    void deletingRemovesThePostAndThenTheWorkout() {
        WorkoutEntity workout = Fixtures.workout(5L, ann, "Running", DAY, 30, 250);
        PostEntity post = new PostEntity(ann, workout, "text");
        when(workouts.findById(5L)).thenReturn(Optional.of(workout));
        when(posts.findByWorkoutId(5L)).thenReturn(Optional.of(post));

        service.delete("ann@x.com", 5L);

        verify(posts).delete(post);
        verify(workouts).delete(workout);
    }

    @Test
    void deletingAMissingWorkoutIsNotFound() {
        when(workouts.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("ann@x.com", 9L));
    }

    @Test
    void listReturnsTheUsersWorkouts() {
        when(users.findByEmail("ann@x.com")).thenReturn(Optional.of(ann));
        when(workouts.findByUserIdOrderByWorkoutDateDescIdDesc(1L))
                .thenReturn(List.of(Fixtures.workout(5L, ann, "Running", DAY, 30, 250)));

        List<WorkoutView> list = service.list("ann@x.com");

        assertEquals(1, list.size());
        assertEquals("Running", list.get(0).type());
        assertEquals(DAY, list.get(0).date());
    }

    @Test
    void unknownUserIsNotFound() {
        when(users.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.list("ghost@x.com"));
    }
}
