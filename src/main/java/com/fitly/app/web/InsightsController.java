package com.fitly.app.web;

import com.fitly.app.service.AnalyticsService;
import com.fitly.app.service.AnalyticsService.Summary;
import com.fitly.app.service.NotificationObserver;
import com.fitly.app.service.ProfileService;
import com.fitly.app.service.ProfileService.ProfileView;
import com.fitly.app.service.WeeklyGoalService;
import com.fitly.app.service.WeeklyGoalService.GoalProgress;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** HTTP for the profile, the weekly goal, the analytics numbers and notifications. */
@RestController
@RequestMapping("/api")
public class InsightsController {

    private static final int ANALYTICS_DAYS = 30;

    /** The JSON for a new weekly goal. */
    public record GoalRequest(Integer targetMinutes) {
    }

    /** The JSON for a profile change. */
    public record ProfileRequest(String displayName, String bio) {
    }

    private final WeeklyGoalService goals;
    private final AnalyticsService analytics;
    private final ProfileService profiles;
    private final NotificationObserver notifications;

    public InsightsController(WeeklyGoalService goals, AnalyticsService analytics, ProfileService profiles,
                              NotificationObserver notifications) {
        this.goals = goals;
        this.analytics = analytics;
        this.profiles = profiles;
        this.notifications = notifications;
    }

    /** Progress toward this week's goal, or an empty object when no goal is set. */
    @GetMapping("/goal")
    public Object goal(HttpSession session) {
        return goals.current(CurrentUser.email(session), LocalDate.now())
                .<Object>map(progress -> progress).orElse(Map.of());
    }

    /** Sets the weekly goal in minutes. */
    @PutMapping("/goal")
    public GoalProgress setGoal(@RequestBody GoalRequest body, HttpSession session) {
        if (body.targetMinutes() == null) {
            throw new IllegalArgumentException("targetMinutes is required");
        }
        return goals.setWeeklyMinutes(CurrentUser.email(session), body.targetMinutes(), LocalDate.now());
    }

    /** Totals for the last 30 days. */
    @GetMapping("/analytics")
    public Summary analytics(HttpSession session) {
        LocalDate today = LocalDate.now();
        return analytics.summary(CurrentUser.email(session), today.minusDays(ANALYTICS_DAYS - 1L), today);
    }

    /** The user's profile. */
    @GetMapping("/profile")
    public ProfileView profile(HttpSession session) {
        return profiles.get(CurrentUser.email(session));
    }

    /** Changes the display name and bio. */
    @PutMapping("/profile")
    public ProfileView updateProfile(@RequestBody ProfileRequest body, HttpSession session) {
        return profiles.update(CurrentUser.email(session), body.displayName(), body.bio());
    }

    /** Messages for the user since the app started. They are kept in memory only. */
    @GetMapping("/notifications")
    public List<String> notifications(HttpSession session) {
        return notifications.messagesFor(CurrentUser.email(session));
    }
}
