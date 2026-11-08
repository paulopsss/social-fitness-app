package com.fitly.app.persistence;

import com.fitly.app.persistence.entity.ExerciseEntity;
import com.fitly.app.persistence.entity.GoalStatus;
import com.fitly.app.persistence.entity.PostEntity;
import com.fitly.app.persistence.entity.UserEntity;
import com.fitly.app.persistence.entity.WorkoutEntity;
import com.fitly.app.persistence.entity.WorkoutTypeEntity;
import com.fitly.app.persistence.repository.ExerciseJpaRepository;
import com.fitly.app.persistence.repository.GoalJpaRepository;
import com.fitly.app.persistence.repository.PostJpaRepository;
import com.fitly.app.persistence.repository.UserJpaRepository;
import com.fitly.app.persistence.repository.WorkoutJpaRepository;
import com.fitly.app.persistence.repository.WorkoutTypeJpaRepository;
import com.fitly.app.service.AuthService;
import com.fitly.app.service.PostLikeService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the app against a fresh in-memory H2 database. Flyway runs V1, V2 and the seed
 * script, then Hibernate validates every entity against the real tables. If any of that
 * fails, every test here fails. Each test rolls back, so they cannot affect each other.
 */
@SpringBootTest
@Transactional
class DatabaseIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private UserJpaRepository users;
    @Autowired private WorkoutTypeJpaRepository workoutTypes;
    @Autowired private ExerciseJpaRepository exercises;
    @Autowired private WorkoutJpaRepository workouts;
    @Autowired private GoalJpaRepository goals;
    @Autowired private PostJpaRepository posts;
    @Autowired private PostLikeService postLikes;
    @Autowired private AuthService authService;

    @ParameterizedTest
    @ValueSource(strings = {"users", "workout_types", "exercises", "workouts", "workout_exercises",
                            "goals", "friendships", "posts", "comments", "post_likes"})
    void everyTableHasAtLeastTwentySeedRows(String table) {
        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        assertTrue(rows >= 20, table + " has only " + rows + " rows");
    }

    @Test
    void migrationsRanInOrder() {
        assertEquals(List.of("V1__create_schema.sql", "V2__add_post_like_count.sql", "R__seed_data.sql"),
                jdbc.queryForList("SELECT \"script\" FROM \"flyway_schema_history\" "
                        + "WHERE \"success\" AND \"installed_rank\" > 0 ORDER BY \"installed_rank\"", String.class));
    }

    @Test
    void seededUsersCanLogInThroughTheRealAuthService() {
        assertTrue(authService.authenticate("paulo@fitly.test", "Fitly2026!"));
        assertFalse(authService.authenticate("paulo@fitly.test", "wrong"));
    }

    @Test
    void cachedLikeCountMatchesThePostLikesTableForEveryPost() {
        Integer mismatches = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts p WHERE p.like_count <> "
                        + "(SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id)", Integer.class);
        assertEquals(0, mismatches);
    }

    @Test
    void likingAndUnlikingKeepsTheCounterInStep() {
        PostEntity post = posts.findAll().get(0);
        UserEntity fan = users.save(new UserEntity("newfan@fitly.test", "x"));
        int before = post.getLikeCount();

        assertTrue(postLikes.like(post.getId(), fan.getId()));
        assertFalse(postLikes.like(post.getId(), fan.getId()), "a second like is ignored");
        assertEquals(before + 1, posts.findById(post.getId()).orElseThrow().getLikeCount());

        assertTrue(postLikes.unlike(post.getId(), fan.getId()));
        assertEquals(before, posts.findById(post.getId()).orElseThrow().getLikeCount());
    }

    @Test
    void ormSavesAWorkoutWithItsExercisesAndReadsThemBack() {
        UserEntity user = users.findByEmail("marcus@fitly.test").orElseThrow();
        WorkoutTypeEntity lifting = workoutTypes.findByName("Weightlifting").orElseThrow();
        ExerciseEntity squat = exercises.findAll().get(0);

        WorkoutEntity workout = new WorkoutEntity(user, lifting, LocalDate.of(2026, 11, 8), 45, 300);
        workout.addExercise(squat, 5, 5, new BigDecimal("100.00"));
        Long id = workouts.save(workout).getId();
        em.flush();
        em.clear();

        WorkoutEntity loaded = workouts.findById(id).orElseThrow();
        assertEquals("marcus@fitly.test", loaded.getUser().getEmail());
        assertEquals(1, loaded.getExercises().size());
        assertEquals(new BigDecimal("100.00"), loaded.getExercises().get(0).getWeightKg());
    }

    @Test
    void derivedQueriesUseTheIndexedColumns() {
        Long userId = jdbc.queryForObject("SELECT user_id FROM workouts WHERE id = 1", Long.class);
        assertFalse(workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDate(
                userId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).isEmpty());
        long active = jdbc.queryForObject("SELECT COUNT(*) FROM goals WHERE status = 'ACTIVE'", Long.class);
        long viaOrm = users.findAll().stream()
                .mapToLong(u -> goals.findByUserIdAndStatus(u.getId(), GoalStatus.ACTIVE).size()).sum();
        assertEquals(active, viaOrm);
    }

    @Test
    void uniqueEmailIsEnforcedByTheDatabase() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO users (email, password_hash) VALUES ('paulo@fitly.test', 'x')"));
    }

    @Test
    void aUserCannotFriendThemselves() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO friendships (requester_id, addressee_id) VALUES (1, 1)"));
    }

    @Test
    void workoutDurationMustBePositive() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO workouts (user_id, workout_type_id, workout_date, duration_minutes) "
                        + "VALUES (1, 1, DATE '2026-11-08', 0)"));
    }

    @Test
    void workoutMustPointAtARealUser() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO workouts (user_id, workout_type_id, workout_date, duration_minutes) "
                        + "VALUES (9999, 1, DATE '2026-11-08', 30)"));
    }

    @Test
    void deletingAUserCascadesToTheirWorkouts() {
        Long userId = jdbc.queryForObject("SELECT user_id FROM workouts WHERE id = 1", Long.class);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM workouts WHERE user_id = ?", Integer.class, userId));
    }

    @Test
    void aPostAboutSomeoneElsesWorkoutIsRejected() {
        WorkoutEntity workout = workouts.findById(1L).orElseThrow();
        UserEntity stranger = users.findAll().stream()
                .filter(u -> !u.getId().equals(workout.getUser().getId())).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> new PostEntity(stranger, workout, "Not mine"));
    }
}
