package com.fitly.app.web;

import com.fitly.app.service.WorkoutTrackingService;
import com.fitly.app.service.WorkoutTrackingService.WorkoutView;
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

import java.time.LocalDate;
import java.util.List;

/** HTTP for workouts: only reads the request and calls WorkoutTrackingService. */
@RestController
@RequestMapping("/api")
public class WorkoutController {

    /** The JSON a client sends to log or change a workout. Calories and date are optional. */
    public record WorkoutRequest(String type, Integer durationMinutes, Integer calories,
                                 LocalDate date, String notes) {
    }

    private final WorkoutTrackingService service;

    public WorkoutController(WorkoutTrackingService service) {
        this.service = service;
    }

    /** The workout types to choose from. */
    @GetMapping("/workout-types")
    public List<String> types(HttpSession session) {
        CurrentUser.email(session);
        return service.typeNames();
    }

    /** The logged in user's workouts, newest first. */
    @GetMapping("/workouts")
    public List<WorkoutView> list(HttpSession session) {
        return service.list(CurrentUser.email(session));
    }

    /** Logs a workout. */
    @PostMapping("/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutView log(@RequestBody WorkoutRequest body, HttpSession session) {
        String email = CurrentUser.email(session);
        return service.log(email, body.type(), required(body), calories(body), date(body), body.notes());
    }

    /** Changes one of the user's workouts. */
    @PutMapping("/workouts/{id}")
    public WorkoutView update(@PathVariable Long id, @RequestBody WorkoutRequest body, HttpSession session) {
        String email = CurrentUser.email(session);
        return service.update(email, id, body.type(), required(body), calories(body), date(body), body.notes());
    }

    /** Deletes one of the user's workouts. */
    @DeleteMapping("/workouts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpSession session) {
        service.delete(CurrentUser.email(session), id);
    }

    private static int required(WorkoutRequest body) {
        if (body.durationMinutes() == null) {
            throw new IllegalArgumentException("durationMinutes is required");
        }
        return body.durationMinutes();
    }

    private static int calories(WorkoutRequest body) {
        return body.calories() == null ? 0 : body.calories();
    }

    private static LocalDate date(WorkoutRequest body) {
        return body.date() == null ? LocalDate.now() : body.date();
    }
}
