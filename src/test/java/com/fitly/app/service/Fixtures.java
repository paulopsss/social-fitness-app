package com.fitly.app.service;

import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.entity.WorkoutCategory;
import com.fitly.app.persistence.entity.WorkoutEntity;
import com.fitly.app.persistence.entity.WorkoutTypeEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/** Builds entities for unit tests. The database normally assigns ids, so tests set them by hand. */
final class Fixtures {

    private Fixtures() {
    }

    static UserEntity user(long id, String email) {
        UserEntity user = new UserEntity(email, "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    static WorkoutTypeEntity type(String name) {
        return new WorkoutTypeEntity(name, WorkoutCategory.CARDIO);
    }

    static WorkoutEntity workout(long id, UserEntity user, String typeName, LocalDate date, int minutes,
                                 int calories) {
        WorkoutEntity workout = new WorkoutEntity(user, type(typeName), date, minutes, calories);
        ReflectionTestUtils.setField(workout, "id", id);
        return workout;
    }
}
