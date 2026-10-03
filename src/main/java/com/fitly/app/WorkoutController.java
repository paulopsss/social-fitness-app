package com.fitly.app;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkoutController {

    public record WorkoutRequest(String type, Integer minutes, LocalDate date, String notes) {
    }

    public record GoalRequest(Integer goal) {
    }

    // goal = workouts per week (0 if not set), completed = workouts so far this week
    public record GoalResponse(int goal, int completed) {
    }

    private final WorkoutStore store;

    public WorkoutController(WorkoutStore store) {
        this.store = store;
    }

    @GetMapping("/workouts")
    public List<Workout> list(HttpSession session) {
        return store.forUser(currentUser(session));
    }

    @PostMapping("/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public Workout create(@RequestBody WorkoutRequest req, HttpSession session) {
        validate(req);
        return store.add(currentUser(session), req.type().trim(), req.minutes(),
                req.date(), cleanNotes(req.notes()));
    }

    @PutMapping("/workouts/{id}")
    public Workout update(@PathVariable long id, @RequestBody WorkoutRequest req,
                          HttpSession session) {
        validate(req);
        return store.update(id, currentUser(session), req.type().trim(), req.minutes(),
                        req.date(), cleanNotes(req.notes()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/workouts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, HttpSession session) {
        if (!store.delete(id, currentUser(session))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/goal")
    public GoalResponse goal(HttpSession session) {
        return progress(currentUser(session));
    }

    @PutMapping("/goal")
    public GoalResponse setGoal(@RequestBody GoalRequest req, HttpSession session) {
        if (req.goal() == null || req.goal() < 1 || req.goal() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String user = currentUser(session);
        store.setGoal(user, req.goal());
        return progress(user);
    }

    // The week runs Monday to Sunday. Workouts dated in the future don't count yet.
    private GoalResponse progress(String user) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new GoalResponse(store.getGoal(user), store.countBetween(user, monday, today));
    }

    private static String currentUser(HttpSession session) {
        return (String) session.getAttribute("user");
    }

    private static void validate(WorkoutRequest req) {
        boolean ok = req.type() != null && !req.type().isBlank() && req.type().trim().length() <= 50
                && req.minutes() != null && req.minutes() >= 1 && req.minutes() <= 1440
                && req.date() != null
                && (req.notes() == null || req.notes().length() <= 500);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static String cleanNotes(String notes) {
        return notes == null ? "" : notes.trim();
    }
}