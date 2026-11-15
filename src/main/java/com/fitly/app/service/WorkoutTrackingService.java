package com.fitly.app.service;

import com.fitly.app.model.Workout;
import com.fitly.app.persistence.entity.PostEntity;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.entity.WorkoutEntity;
import com.fitly.app.persistence.entity.WorkoutTypeEntity;
import com.fitly.app.persistence.repository.PostJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import com.fitly.app.persistence.repository.WorkoutTypeJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Log, list, edit and delete workouts, stored in the database (Use Case 2).
 *
 * Logging also writes a post so friends can see it in the feed, and tells the
 * workout observers (Observer pattern, see WorkoutService). Every method takes
 * the logged in user's email, and a workout that belongs to someone else is
 * treated as if it did not exist.
 */
@Service
public class WorkoutTrackingService {

    private static final int MAX_NOTES_LENGTH = 500;

    /** What the web layer sends back for one workout. */
    public record WorkoutView(Long id, String type, int durationMinutes, int calories,
                              LocalDate date, String notes) {
    }

    private final UserJpaRepository users;
    private final WorkoutTypeJpaRepository types;
    private final WorkoutJpaRepository workouts;
    private final PostJpaRepository posts;
    private final WorkoutService observers;

    public WorkoutTrackingService(UserJpaRepository users, WorkoutTypeJpaRepository types,
                                  WorkoutJpaRepository workouts, PostJpaRepository posts,
                                  WorkoutService observers) {
        this.users = users;
        this.types = types;
        this.workouts = workouts;
        this.posts = posts;
        this.observers = observers;
    }

    /** Saves a new workout, posts it to the feed and notifies the observers. */
    @Transactional
    public WorkoutView log(String email, String typeName, int minutes, int calories,
                           LocalDate date, String notes) {
        UserEntity user = user(email);
        WorkoutTypeEntity type = type(typeName);
        Workout valid = new Workout(email, typeName, minutes, calories, date);
        WorkoutEntity entity = new WorkoutEntity(user, type, date, minutes, calories);
        entity.setNotes(cleanNotes(notes));
        workouts.save(entity);
        posts.save(new PostEntity(user, entity, describe(valid)));
        observers.logWorkout(email, typeName, minutes, calories, date);
        return view(entity);
    }

    /** All workouts of this user, newest first. */
    @Transactional(readOnly = true)
    public List<WorkoutView> list(String email) {
        return workouts.findByUserIdOrderByWorkoutDateDescIdDesc(user(email).getId())
                .stream().map(WorkoutTrackingService::view).toList();
    }

    /** Changes a workout and the feed post that describes it. */
    @Transactional
    public WorkoutView update(String email, Long id, String typeName, int minutes, int calories,
                              LocalDate date, String notes) {
        WorkoutEntity entity = owned(email, id);
        WorkoutTypeEntity type = type(typeName);
        Workout valid = new Workout(email, typeName, minutes, calories, date);
        entity.update(type, date, minutes, calories, cleanNotes(notes));
        posts.findByWorkoutId(id).ifPresent(post -> post.setContent(describe(valid)));
        return view(entity);
    }

    /** Removes a workout together with the feed post about it. */
    @Transactional
    public void delete(String email, Long id) {
        WorkoutEntity entity = owned(email, id);
        posts.findByWorkoutId(id).ifPresent(posts::delete);
        workouts.delete(entity);
    }

    /** The workout types a user can choose from, in alphabetical order. */
    @Transactional(readOnly = true)
    public List<String> typeNames() {
        return types.findAll().stream().map(WorkoutTypeEntity::getName)
                .sorted(Comparator.naturalOrder()).toList();
    }

    private UserEntity user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("Unknown user"));
    }

    private WorkoutTypeEntity type(String name) {
        return types.findByName(name == null ? "" : name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workout type: " + name));
    }

    private WorkoutEntity owned(String email, Long id) {
        return workouts.findById(id)
                .filter(workout -> workout.getUser().getEmail().equals(email))
                .orElseThrow(() -> new NotFoundException("Workout not found"));
    }

    private static String cleanNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        if (notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("notes must be at most " + MAX_NOTES_LENGTH + " characters");
        }
        return notes.strip();
    }

    private static String describe(Workout workout) {
        return "Logged " + workout.type() + " for " + workout.durationMinutes() + " min ("
                + workout.calories() + " kcal)";
    }

    private static WorkoutView view(WorkoutEntity entity) {
        return new WorkoutView(entity.getId(), entity.getWorkoutType().getName(),
                entity.getDurationMinutes(), entity.getCalories(), entity.getWorkoutDate(),
                entity.getNotes());
    }
}
