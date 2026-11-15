// Fitly dashboard. Talks to the JSON API under /api and draws the page.
// All user supplied text goes in with textContent, never innerHTML, so it cannot inject markup.
"use strict";

const $ = (id) => document.getElementById(id);
let editingId = null;
let pendingDelete = null;

function el(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function showMessage(text) {
  const box = $("message");
  box.textContent = text || "";
  box.hidden = !text;
}

async function api(method, url, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  const response = await fetch(url, options);
  if (response.status === 401) {
    window.location.href = "/Login_page.html";
    throw new Error("Not logged in");
  }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || "Something went wrong");
  return data;
}

async function run(action) {
  try {
    showMessage("");
    await action();
  } catch (e) {
    showMessage(e.message);
  }
}

function fill(listId, items, draw, emptyText) {
  const list = $(listId);
  list.replaceChildren();
  if (items.length === 0) {
    const li = el("li", emptyText, "empty");
    list.appendChild(li);
    return;
  }
  items.forEach((item) => list.appendChild(draw(item)));
}

function greeting(name) {
  const hour = new Date().getHours();
  const part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  return name ? part + ", " + name : part;
}

async function loadProfile() {
  const profile = await api("GET", "/api/profile");
  $("greeting").textContent = greeting(profile.displayName);
  $("who").textContent = profile.email;
  $("display-name").value = profile.displayName || "";
  $("bio").value = profile.bio || "";
}

async function loadGoal() {
  const goal = await api("GET", "/api/goal");
  if (!goal.targetMinutes) {
    $("goal-text").textContent = "No goal yet. Set a target in minutes per week.";
    $("goal-bar").hidden = true;
    return;
  }
  const done = goal.completed ? " Goal reached!" : "";
  $("goal-text").textContent = goal.minutesDone + " of " + goal.targetMinutes + " minutes this week (" + goal.percent + "%)." + done;
  $("goal-fill").style.width = goal.percent + "%";
  $("goal-bar").hidden = false;
}

async function loadWorkouts() {
  const workouts = await api("GET", "/api/workouts");
  fill("workouts", workouts, drawWorkout, "No workouts yet. Log your first one above.");
}

function drawWorkout(w) {
  const li = el("li");
  li.appendChild(el("div", w.type + ", " + w.durationMinutes + " min, " + w.calories + " kcal"));
  li.appendChild(el("div", w.date + (w.notes ? " - " + w.notes : ""), "meta"));
  const actions = el("div", null, "actions");
  const edit = el("button", "Edit", "small");
  edit.type = "button";
  edit.addEventListener("click", () => startEdit(w));
  const del = el("button", pendingDelete === w.id ? "Sure? Click again" : "Delete", "small");
  del.type = "button";
  del.addEventListener("click", () => run(async () => {
    if (pendingDelete !== w.id) { pendingDelete = w.id; await loadWorkouts(); return; }
    pendingDelete = null;
    await api("DELETE", "/api/workouts/" + w.id);
    await refreshAll();
  }));
  actions.append(edit, del);
  li.appendChild(actions);
  return li;
}

function startEdit(w) {
  editingId = w.id;
  $("type").value = w.type;
  $("minutes").value = w.durationMinutes;
  $("calories").value = w.calories;
  $("date").value = w.date;
  $("notes").value = w.notes || "";
  $("workout-heading").textContent = "Edit workout";
  $("workout-submit").textContent = "Save changes";
  $("workout-cancel").hidden = false;
  $("workout-form").scrollIntoView();
}

function resetWorkoutForm() {
  editingId = null;
  $("workout-form").reset();
  $("date").value = new Date().toISOString().slice(0, 10);
  $("workout-heading").textContent = "Log a workout";
  $("workout-submit").textContent = "Log workout";
  $("workout-cancel").hidden = true;
}

async function loadAnalytics() {
  const a = await api("GET", "/api/analytics");
  const rows = [
    a.totalWorkouts + " workouts on " + a.activeDays + " active days",
    a.totalMinutes + " minutes, " + a.totalCalories + " calories",
  ];
  Object.entries(a.minutesByType).forEach(([type, minutes]) => rows.push(type + ": " + minutes + " min"));
  fill("analytics", rows, (text) => el("li", text), "");
}

async function loadFriends() {
  const requests = await api("GET", "/api/friends/requests");
  fill("requests", requests, (r) => {
    const li = el("li", (r.fromName || r.fromEmail) + " wants to be your friend");
    const actions = el("div", null, "actions");
    [["Accept", "accept"], ["Decline", "decline"]].forEach(([label, path]) => {
      const b = el("button", label, "small");
      b.type = "button";
      b.addEventListener("click", () => run(async () => {
        await api("POST", "/api/friends/requests/" + r.id + "/" + path);
        await refreshAll();
      }));
      actions.appendChild(b);
    });
    li.appendChild(actions);
    return li;
  }, "No requests.");
  const friends = await api("GET", "/api/friends");
  fill("friends", friends, (f) => el("li", f.displayName ? f.displayName + " (" + f.email + ")" : f.email), "No friends yet.");
}

async function loadFeed() {
  const feed = await api("GET", "/api/feed");
  fill("feed", feed, (p) => {
    const li = el("li");
    li.appendChild(el("div", p.content));
    li.appendChild(el("div", (p.mine ? "You" : p.authorName || p.authorEmail) + " - " + p.createdAt.replace("T", " ").slice(0, 16), "meta"));
    const actions = el("div", null, "actions");
    const like = el("button", (p.likedByMe ? "Unlike" : "Like") + " (" + p.likeCount + ")", "small");
    like.type = "button";
    like.addEventListener("click", () => run(async () => {
      await api(p.likedByMe ? "DELETE" : "POST", "/api/posts/" + p.id + "/like");
      await loadFeed();
    }));
    actions.appendChild(like);
    li.appendChild(actions);
    return li;
  }, "Nothing here yet. Log a workout or add a friend.");
}

async function loadNotifications() {
  const messages = await api("GET", "/api/notifications");
  fill("notifications", messages.slice().reverse(), (m) => el("li", m), "No notifications since the app started.");
}

async function refreshAll() {
  await Promise.all([loadProfile(), loadGoal(), loadWorkouts(), loadAnalytics(), loadFriends(), loadFeed(), loadNotifications()]);
}

function workoutBody() {
  const calories = $("calories").value;
  return {
    type: $("type").value,
    durationMinutes: Number($("minutes").value),
    calories: calories === "" ? 0 : Number(calories),
    date: $("date").value,
    notes: $("notes").value,
  };
}

$("workout-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    if (editingId === null) await api("POST", "/api/workouts", workoutBody());
    else await api("PUT", "/api/workouts/" + editingId, workoutBody());
    resetWorkoutForm();
    await refreshAll();
  });
});
$("workout-cancel").addEventListener("click", resetWorkoutForm);

$("goal-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    await api("PUT", "/api/goal", { targetMinutes: Number($("goal-minutes").value) });
    $("goal-form").reset();
    await loadGoal();
  });
});

$("friend-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    await api("POST", "/api/friends/requests", { email: $("friend-email").value });
    $("friend-form").reset();
    showMessage("");
    $("friend-email").placeholder = "Request sent";
  });
});

$("profile-form").addEventListener("submit", (event) => {
  event.preventDefault();
  run(async () => {
    await api("PUT", "/api/profile", { displayName: $("display-name").value, bio: $("bio").value });
    await loadProfile();
  });
});

run(async () => {
  const types = await api("GET", "/api/workout-types");
  types.forEach((name) => $("type").appendChild(el("option", name)));
  resetWorkoutForm();
  await refreshAll();
});
