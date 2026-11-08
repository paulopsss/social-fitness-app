package com.fitly.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Maps workout_exercises, the associative table between workouts and exercises.
 * It is an entity of its own (not a plain @ManyToMany) because each pair carries data:
 * sets, reps and weight.
 */
@Entity
@Table(name = "workout_exercises")
public class WorkoutExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private WorkoutEntity workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private int reps;

    /** Empty for bodyweight moves such as push-ups. */
    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    protected WorkoutExerciseEntity() {
    }

    WorkoutExerciseEntity(WorkoutEntity workout, ExerciseEntity exercise, int sets, int reps, BigDecimal weightKg) {
        this.workout = workout;
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.weightKg = weightKg;
    }

    public Long getId() {
        return id;
    }

    public WorkoutEntity getWorkout() {
        return workout;
    }

    public ExerciseEntity getExercise() {
        return exercise;
    }

    public int getSets() {
        return sets;
    }

    public int getReps() {
        return reps;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }
}
