package com.fitly.app.service;

import com.fitly.app.model.Workout;

/**
 * Observer pattern: anything that needs to react when a workout is logged
 * implements this one method.
 *
 * Observers must not depend on each other or on running in a set order.
 */
public interface WorkoutObserver {

    void onWorkoutLogged(Workout workout);
}
