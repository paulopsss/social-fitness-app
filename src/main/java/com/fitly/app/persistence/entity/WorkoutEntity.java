package com.fitly.app.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Maps the workouts table: one logged session. */
@Entity
@Table(name = "workouts")
public class WorkoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many workouts belong to one user (N:1). LAZY: the user is loaded only when asked for. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_type_id", nullable = false)
    private WorkoutTypeEntity workoutType;

    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private int calories;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The exercises done in this workout. Saving or deleting the workout does the same to these rows. */
    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutExerciseEntity> exercises = new ArrayList<>();

    protected WorkoutEntity() {
    }

    public WorkoutEntity(UserEntity user, WorkoutTypeEntity workoutType, LocalDate workoutDate,
                         int durationMinutes, int calories) {
        this.user = user;
        this.workoutType = workoutType;
        this.workoutDate = workoutDate;
        this.durationMinutes = durationMinutes;
        this.calories = calories;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Keeps both sides of the relationship in step. */
    public WorkoutExerciseEntity addExercise(ExerciseEntity exercise, int sets, int reps,
                                             BigDecimal weightKg) {
        WorkoutExerciseEntity entry = new WorkoutExerciseEntity(this, exercise, sets, reps, weightKg);
        exercises.add(entry);
        return entry;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public WorkoutTypeEntity getWorkoutType() {
        return workoutType;
    }

    public LocalDate getWorkoutDate() {
        return workoutDate;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getCalories() {
        return calories;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<WorkoutExerciseEntity> getExercises() {
        return exercises;
    }
}
