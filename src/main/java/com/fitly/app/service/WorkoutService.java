package com.fitly.app.service;

import com.fitly.app.model.Workout;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The subject in the Observer pattern (Use Case 2: Log Workout).
 *
 * IMPROVEMENT over the "before" version: this class no longer knows who
 * reacts to a workout. To add a reaction, for example analytics, write a new
 * WorkoutObserver. This class, and its tests, do not change.
 *
 * Spring finds every WorkoutObserver bean and passes the list in, so there
 * is no registration code to write. subscribe() is the classic Observer
 * "attach" operation and is handy in tests.
 */
@Service
public class WorkoutService {

    private final List<WorkoutObserver> observers;

    public WorkoutService(List<WorkoutObserver> observers) {
        this.observers = new CopyOnWriteArrayList<>(observers);
    }

    public void subscribe(WorkoutObserver observer) {
        observers.add(observer);
    }

    public Workout logWorkout(String userEmail, String type, int durationMinutes,
                              int calories, LocalDate date) {
        Workout workout = new Workout(userEmail, type, durationMinutes, calories, date);
        // Saving the workout goes here once the H2 database is added.
        for (WorkoutObserver observer : observers) {
            observer.onWorkoutLogged(workout);
        }
        return workout;
    }
}
