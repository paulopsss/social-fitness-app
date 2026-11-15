package com.fitly.app.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: real HTTP layer, real services, real H2 database with the Flyway schema
 * and seed data. Each test rolls back, so they cannot affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    private static final String TODAY = LocalDate.now().toString();
    private static final String ID_PATTERN = ".*\"id\":(\\d+).*";

    @Autowired
    private MockMvc mvc;

    /** Registers a new account through the real endpoint and returns its logged in session. */
    private MockHttpSession login(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/register").param("email", email).param("password", "secret1").session(session));
        return session;
    }

    private ResultActions json(MockHttpServletRequestBuilder request, MockHttpSession session, String body)
            throws Exception {
        return mvc.perform(request.session(session).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private String firstId(String json) {
        return json.replaceAll(ID_PATTERN, "$1");
    }

    private String logRunning(MockHttpSession session, int minutes) throws Exception {
        String request = "{\"type\":\"Running\",\"durationMinutes\":" + minutes
                + ",\"calories\":300,\"date\":\"" + TODAY + "\",\"notes\":\"easy\"}";
        return firstId(body(json(post("/api/workouts"), session, request).andExpect(status().isCreated())));
    }

    /** Ann sends Ben a request and Ben accepts it. */
    private void makeFriends(MockHttpSession from, String toEmail, MockHttpSession to) throws Exception {
        json(post("/api/friends/requests"), from, "{\"email\":\"" + toEmail + "\"}").andExpect(status().isCreated());
        String requestId = firstId(body(mvc.perform(get("/api/friends/requests").session(to))));
        mvc.perform(post("/api/friends/requests/" + requestId + "/accept").session(to)).andExpect(status().isOk());
    }

    @Test
    void apiAnswers401WhenNotLoggedIn() throws Exception {
        mvc.perform(get("/api/workouts")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Log in first"));
    }

    @Test
    void loggedWorkoutAppearsInTheListAndInTheFeed() throws Exception {
        MockHttpSession me = login("runner@example.com");
        logRunning(me, 30);

        mvc.perform(get("/api/workouts").session(me)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("Running"))
                .andExpect(jsonPath("$[0].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].notes").value("easy"));
        mvc.perform(get("/api/feed").session(me)).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Logged Running for 30 min (300 kcal)"))
                .andExpect(jsonPath("$[0].mine").value(true));
    }

    @Test
    void editingAWorkoutChangesItAndItsFeedPost() throws Exception {
        MockHttpSession me = login("editor@example.com");
        String id = logRunning(me, 30);

        json(put("/api/workouts/" + id), me,
                "{\"type\":\"Cycling\",\"durationMinutes\":45,\"calories\":400,\"date\":\"" + TODAY + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("Cycling"));

        mvc.perform(get("/api/feed").session(me))
                .andExpect(jsonPath("$[0].content").value("Logged Cycling for 45 min (400 kcal)"));
    }

    @Test
    void deletingAWorkoutRemovesItAndItsFeedPost() throws Exception {
        MockHttpSession me = login("deleter@example.com");
        String id = logRunning(me, 20);

        mvc.perform(delete("/api/workouts/" + id).session(me)).andExpect(status().isNoContent());

        mvc.perform(get("/api/workouts").session(me)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/feed").session(me)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nobodyCanEditOrDeleteAnotherUsersWorkout() throws Exception {
        MockHttpSession owner = login("owner@example.com");
        MockHttpSession other = login("other@example.com");
        String id = logRunning(owner, 30);

        mvc.perform(delete("/api/workouts/" + id).session(other)).andExpect(status().isNotFound());
        json(put("/api/workouts/" + id), other,
                "{\"type\":\"Running\",\"durationMinutes\":5,\"date\":\"" + TODAY + "\"}")
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/workouts").session(owner)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void badWorkoutInputIsRejectedWith400() throws Exception {
        MockHttpSession me = login("bad@example.com");

        json(post("/api/workouts"), me, "{\"type\":\"Running\",\"durationMinutes\":-5}")
                .andExpect(status().isBadRequest());
        json(post("/api/workouts"), me, "{\"type\":\"Underwater Basket Weaving\",\"durationMinutes\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown workout type: Underwater Basket Weaving"));
        json(post("/api/workouts"), me, "{\"type\":\"Running\"}").andExpect(status().isBadRequest());
    }

    @Test
    void friendRequestCanBeSentAcceptedAndBothSeeTheFriendship() throws Exception {
        MockHttpSession ann = login("ann@example.com");
        MockHttpSession ben = login("ben@example.com");

        json(post("/api/friends/requests"), ann, "{\"email\":\"ben@example.com\"}").andExpect(status().isCreated());
        mvc.perform(get("/api/friends/requests").session(ben))
                .andExpect(jsonPath("$[0].fromEmail").value("ann@example.com"));
        makeFriendsAnswer(ben);

        mvc.perform(get("/api/friends").session(ann)).andExpect(jsonPath("$[*].email", hasItem("ben@example.com")));
        mvc.perform(get("/api/friends").session(ben)).andExpect(jsonPath("$[*].email", hasItem("ann@example.com")));
        mvc.perform(get("/api/friends/requests").session(ben)).andExpect(jsonPath("$", hasSize(0)));
    }

    private void makeFriendsAnswer(MockHttpSession to) throws Exception {
        String requestId = firstId(body(mvc.perform(get("/api/friends/requests").session(to))));
        mvc.perform(post("/api/friends/requests/" + requestId + "/accept").session(to)).andExpect(status().isOk());
    }

    @Test
    void friendRequestRulesAreEnforced() throws Exception {
        MockHttpSession ann = login("ann2@example.com");
        login("ben2@example.com");

        json(post("/api/friends/requests"), ann, "{\"email\":\"ann2@example.com\"}").andExpect(status().isBadRequest());
        json(post("/api/friends/requests"), ann, "{\"email\":\"nobody@example.com\"}").andExpect(status().isNotFound());
        json(post("/api/friends/requests"), ann, "{\"email\":\"ben2@example.com\"}").andExpect(status().isCreated());
        json(post("/api/friends/requests"), ann, "{\"email\":\"ben2@example.com\"}").andExpect(status().isBadRequest());
    }

    @Test
    void feedShowsFriendsPostsOnlyAfterTheyAcceptAndLikesCanBeToggled() throws Exception {
        MockHttpSession ann = login("ann3@example.com");
        MockHttpSession ben = login("ben3@example.com");
        logRunning(ben, 40);
        mvc.perform(get("/api/feed").session(ann)).andExpect(jsonPath("$", hasSize(0)));

        makeFriends(ann, "ben3@example.com", ben);

        String postId = firstId(body(mvc.perform(get("/api/feed").session(ann)).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorEmail").value("ben3@example.com"))
                .andExpect(jsonPath("$[0].likeCount").value(0))));
        mvc.perform(post("/api/posts/" + postId + "/like").session(ann)).andExpect(jsonPath("$.changed").value(true));
        mvc.perform(post("/api/posts/" + postId + "/like").session(ann)).andExpect(jsonPath("$.changed").value(false));
        mvc.perform(get("/api/feed").session(ann)).andExpect(jsonPath("$[0].likeCount").value(1))
                .andExpect(jsonPath("$[0].likedByMe").value(true));
        mvc.perform(delete("/api/posts/" + postId + "/like").session(ann)).andExpect(jsonPath("$.changed").value(true));
        mvc.perform(get("/api/feed").session(ann)).andExpect(jsonPath("$[0].likeCount").value(0));
    }

    @Test
    void strangersPostsCannotBeLiked() throws Exception {
        MockHttpSession ann = login("ann4@example.com");
        MockHttpSession ben = login("ben4@example.com");
        logRunning(ben, 40);
        String postId = firstId(body(mvc.perform(get("/api/feed").session(ben))));

        mvc.perform(post("/api/posts/" + postId + "/like").session(ann)).andExpect(status().isNotFound());
    }

    @Test
    void weeklyGoalProgressGrowsAndCompletes() throws Exception {
        MockHttpSession me = login("goal@example.com");
        mvc.perform(get("/api/goal").session(me)).andExpect(jsonPath("$.targetMinutes").doesNotExist());

        json(put("/api/goal"), me, "{\"targetMinutes\":100}").andExpect(status().isOk())
                .andExpect(jsonPath("$.minutesDone").value(0)).andExpect(jsonPath("$.completed").value(false));
        logRunning(me, 60);
        mvc.perform(get("/api/goal").session(me)).andExpect(jsonPath("$.minutesDone").value(60))
                .andExpect(jsonPath("$.percent").value(60)).andExpect(jsonPath("$.completed").value(false));
        logRunning(me, 50);
        mvc.perform(get("/api/goal").session(me)).andExpect(jsonPath("$.percent").value(100))
                .andExpect(jsonPath("$.completed").value(true));
        json(put("/api/goal"), me, "{\"targetMinutes\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void analyticsAddsUpWorkoutsMinutesCaloriesAndActiveDays() throws Exception {
        MockHttpSession me = login("stats@example.com");
        logRunning(me, 30);
        logRunning(me, 20);
        json(post("/api/workouts"), me, "{\"type\":\"Cycling\",\"durationMinutes\":10,\"calories\":100,\"date\":\""
                + LocalDate.now().minusDays(1) + "\"}");

        mvc.perform(get("/api/analytics").session(me)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkouts").value(3)).andExpect(jsonPath("$.totalMinutes").value(60))
                .andExpect(jsonPath("$.totalCalories").value(700)).andExpect(jsonPath("$.activeDays").value(2))
                .andExpect(jsonPath("$.minutesByType.Running").value(50));
    }

    @Test
    void profileCanBeReadAndChanged() throws Exception {
        MockHttpSession me = login("profile@example.com");

        json(put("/api/profile"), me, "{\"displayName\":\"Pat\",\"bio\":\"Likes running\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Pat"));
        mvc.perform(get("/api/profile").session(me)).andExpect(jsonPath("$.bio").value("Likes running"))
                .andExpect(jsonPath("$.email").value("profile@example.com"));
        json(put("/api/profile"), me, "{\"displayName\":\"" + "x".repeat(51) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void loggingAWorkoutAddsANotification() throws Exception {
        MockHttpSession me = login("notify@example.com");
        logRunning(me, 25);

        mvc.perform(get("/api/notifications").session(me))
                .andExpect(jsonPath("$", hasItem("Workout logged: Running, 25 min")));
    }
}
