"""
Generates src/main/resources/db/seed/R__seed_data.sql, the Fitly test data.

Run from the repo root:  python scripts/generate_seed_data.py
The output is deterministic (fixed random seed), so re-running it gives the
same file unless this script changes. Every seeded user's password is
Fitly2026!  (hashed with the same PBKDF2 format the app uses).
"""
import base64
import hashlib
import random
from datetime import date, datetime, timedelta
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/db/seed/R__seed_data.sql"
PASSWORD = "Fitly2026!"
ITERATIONS = 600_000
rng = random.Random(2026)


def pbkdf2(password: str) -> str:
    salt = rng.randbytes(16)
    key = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, ITERATIONS, dklen=32)
    b64 = lambda b: base64.b64encode(b).decode()
    return f"pbkdf2${ITERATIONS}${b64(salt)}${b64(key)}"


def q(value):
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def ts(d: datetime) -> str:
    return f"TIMESTAMP '{d:%Y-%m-%d %H:%M:%S}'"


def insert(table, cols, rows):
    lines = [f"INSERT INTO {table} ({', '.join(cols)}) VALUES"]
    body = []
    for r in rows:
        body.append("    (" + ", ".join(v if isinstance(v, Raw) else q(v) for v in r) + ")")
    return "\n".join(lines) + "\n" + ",\n".join(body) + ";\n"


class Raw(str):
    """A value written into the SQL as is (dates, timestamps)."""


START = date(2026, 9, 1)

# ---- users -----------------------------------------------------------
people = [
    ("Paxton", "Lifting is my therapy."), ("Paulo", "Runner chasing a sub-25 5K."),
    ("Raymond", "Yoga in the morning, code at night."), ("Maya", "Cyclist. Coffee. Repeat."),
    ("Jordan", "Training for my first half marathon."), ("Aisha", "Swimmer turned climber."),
    ("Diego", "Soccer every Sunday."), ("Priya", "Pilates and long walks."),
    ("Marcus", "Powerlifting, slowly."), ("Lena", "HIIT fan, hates treadmills."),
    ("Tomas", "Rowing club member."), ("Grace", "Getting back into shape after a long break."),
    ("Kenji", "Martial arts and mobility."), ("Sofia", "Dance cardio is the best cardio."),
    ("Ethan", "Weekend hiker."), ("Nadia", "Couch to 5K graduate."),
    ("Omar", "Basketball and bodyweight."), ("Chloe", "Spin class regular."),
    ("Ravi", "Tracking every rep."), ("Hannah", "Trail running and stretching."),
    ("Leo", None), ("Zara", "Boxing three times a week."),
    ("Isaac", None), ("Mei", "Tai chi and tennis."),
]
users = []
for i, (name, bio) in enumerate(people, start=1):
    email = f"{name.lower()}@fitly.test"
    created = datetime(2026, 8, 15) + timedelta(days=i, hours=rng.randint(8, 20))
    users.append((i, email, pbkdf2(PASSWORD), name, bio, Raw(ts(created))))

# ---- workout types ---------------------------------------------------
types = [
    ("Running", "CARDIO"), ("Cycling", "CARDIO"), ("Swimming", "CARDIO"), ("Rowing", "CARDIO"),
    ("Walking", "CARDIO"), ("Hiking", "CARDIO"), ("HIIT", "CARDIO"), ("Spin Class", "CARDIO"),
    ("Weightlifting", "STRENGTH"), ("Powerlifting", "STRENGTH"), ("Bodyweight", "STRENGTH"),
    ("CrossFit", "STRENGTH"), ("Yoga", "FLEXIBILITY"), ("Pilates", "FLEXIBILITY"),
    ("Stretching", "FLEXIBILITY"), ("Soccer", "SPORT"), ("Basketball", "SPORT"),
    ("Tennis", "SPORT"), ("Boxing", "SPORT"), ("Tai Chi", "MIND_BODY"), ("Dance", "CARDIO"),
    ("Climbing", "STRENGTH"),
]
workout_types = [(i, n, c) for i, (n, c) in enumerate(types, start=1)]
type_id = {n: i for i, n, _ in workout_types}
strength_types = {type_id[n] for n in ("Weightlifting", "Powerlifting", "Bodyweight", "CrossFit")}

# ---- exercises -------------------------------------------------------
ex = [
    ("Back Squat", "LEGS"), ("Front Squat", "LEGS"), ("Deadlift", "BACK"), ("Romanian Deadlift", "LEGS"),
    ("Bench Press", "CHEST"), ("Incline Dumbbell Press", "CHEST"), ("Overhead Press", "SHOULDERS"),
    ("Lateral Raise", "SHOULDERS"), ("Barbell Row", "BACK"), ("Pull-Up", "BACK"),
    ("Lat Pulldown", "BACK"), ("Push-Up", "CHEST"), ("Dip", "ARMS"), ("Bicep Curl", "ARMS"),
    ("Tricep Extension", "ARMS"), ("Walking Lunge", "LEGS"), ("Leg Press", "LEGS"),
    ("Calf Raise", "LEGS"), ("Plank", "CORE"), ("Hanging Leg Raise", "CORE"),
    ("Russian Twist", "CORE"), ("Kettlebell Swing", "FULL_BODY"), ("Burpee", "FULL_BODY"),
    ("Box Jump", "LEGS"), ("Hip Thrust", "LEGS"),
]
exercises = [(i, n, g) for i, (n, g) in enumerate(ex, start=1)]
bodyweight_ex = {"Pull-Up", "Push-Up", "Dip", "Plank", "Hanging Leg Raise", "Burpee", "Box Jump", "Russian Twist"}

# ---- workouts --------------------------------------------------------
favorite = {}
for uid, *_ in users:
    favorite[uid] = rng.sample(range(1, len(workout_types) + 1), 3)
notes_pool = [None, None, None, "Felt strong today.", "Legs were tired.", "New personal best!",
              "Easy recovery session.", "Rained the whole time.", "Went with a friend.",
              "Short on time, kept it quick.", "Tried a new route."]
workouts = []
for wid in range(1, 81):
    uid = rng.randint(1, len(users))
    tid = rng.choice(favorite[uid])
    day = START + timedelta(days=rng.randint(0, 66))
    minutes = rng.choice([20, 25, 30, 35, 40, 45, 50, 60, 75, 90])
    calories = int(minutes * rng.uniform(5.0, 11.0))
    created = datetime.combine(day, datetime.min.time()) + timedelta(hours=rng.randint(6, 21))
    workouts.append((wid, uid, tid, Raw(f"DATE '{day}'"), minutes, calories,
                     rng.choice(notes_pool), Raw(ts(created))))

# ---- workout_exercises (only on strength workouts) ---------------------
workout_exercises = []
we_id = 1
for w in workouts:
    if w[2] not in strength_types:
        continue
    for eid in rng.sample(range(1, len(exercises) + 1), rng.randint(3, 5)):
        name = exercises[eid - 1][1]
        weight = None if name in bodyweight_ex else round(rng.choice(range(10, 141, 5)) * 1.0, 2)
        workout_exercises.append((we_id, w[0], eid, rng.randint(3, 5), rng.choice([5, 6, 8, 10, 12, 15]), weight))
        we_id += 1
# make sure the table clears 20 rows even if strength workouts are rare
extra_w = [w for w in workouts if w[2] not in strength_types]
while len(workout_exercises) < 40:
    w = extra_w.pop()
    for eid in rng.sample([12, 19, 23, 16], 2):
        workout_exercises.append((we_id, w[0], eid, 3, rng.choice([10, 12, 15]), None))
        we_id += 1

# ---- goals -----------------------------------------------------------
goals = []
for gid, uid in enumerate(range(1, len(users) + 1), start=1):
    gtype = rng.choice(["WEEKLY_MINUTES", "WEEKLY_WORKOUTS", "WEEKLY_CALORIES"])
    target = {"WEEKLY_MINUTES": rng.choice([90, 120, 150, 180, 240]),
              "WEEKLY_WORKOUTS": rng.choice([2, 3, 4, 5]),
              "WEEKLY_CALORIES": rng.choice([1200, 1500, 2000, 2500])}[gtype]
    start = START + timedelta(days=rng.randint(0, 20))
    status = rng.choice(["ACTIVE", "ACTIVE", "ACTIVE", "COMPLETED", "ABANDONED"])
    end = None if status == "ACTIVE" else start + timedelta(weeks=rng.randint(2, 6))
    goals.append((gid, uid, gtype, target, Raw(f"DATE '{start}'"),
                  Raw(f"DATE '{end}'") if end else None, status,
                  Raw(ts(datetime.combine(start, datetime.min.time()) + timedelta(hours=9)))))

# ---- friendships (unordered pairs, never both directions) ---------------
pairs = set()
friendships = []
while len(friendships) < 36:
    a, b = rng.sample(range(1, len(users) + 1), 2)
    if (a, b) in pairs or (b, a) in pairs:
        continue
    pairs.add((a, b))
    status = rng.choice(["ACCEPTED", "ACCEPTED", "ACCEPTED", "PENDING", "DECLINED"])
    created = datetime(2026, 9, 1) + timedelta(days=rng.randint(0, 50), hours=rng.randint(7, 22))
    responded = None if status == "PENDING" else Raw(ts(created + timedelta(hours=rng.randint(1, 48))))
    friendships.append((len(friendships) + 1, a, b, status, Raw(ts(created)), responded))

# ---- posts (a post about a workout is always by that workout's owner) ----
post_lines = [
    "Crushed my {t} session today!", "{t} done. {m} minutes, feeling great.",
    "Slow {t} today but I showed up.", "Anyone want to join me for {t} this weekend?",
    "Week {n} of my streak. {t} again.", "New personal best on {t}!",
]
standalone = ["Rest day. Stretching and lots of water.", "What is everyone's favorite pre-workout snack?",
              "Signed up for a 10K in December!", "Hit my weekly goal two days early.",
              "Looking for a gym buddy near campus.", "Sore from yesterday, worth it."]
posts = []
for pid, w in enumerate(rng.sample(workouts, 24), start=1):
    tname = workout_types[w[2] - 1][1]
    text = rng.choice(post_lines).format(t=tname, m=w[4], n=rng.randint(2, 9))
    created = datetime.fromisoformat(str(w[3])[6:-1]) + timedelta(hours=rng.randint(10, 23))
    posts.append([pid, w[1], w[0], text, Raw(ts(created))])
for text in standalone:
    pid = len(posts) + 1
    created = datetime(2026, 9, 5) + timedelta(days=rng.randint(0, 60), hours=rng.randint(8, 22))
    posts.append([pid, rng.randint(1, len(users)), None, text, Raw(ts(created))])

# ---- comments --------------------------------------------------------
comment_pool = ["Nice work!", "Let's go!", "So jealous, I skipped today.", "Count me in.",
                "How long did that take you?", "That's a big jump from last week.",
                "Recovery is training too.", "Proud of you!", "Which route was this?",
                "Same time next week?", "Banana and peanut butter, always.", "You inspired me to get out today."]
comments = []
for cid in range(1, 49):
    post = rng.choice(posts)
    author = rng.choice([u for u in range(1, len(users) + 1) if u != post[1]])
    created = datetime.fromisoformat(post[4][11:-1]) + timedelta(minutes=rng.randint(5, 2000))
    comments.append((cid, post[0], author, rng.choice(comment_pool), Raw(ts(created))))

# ---- post_likes ------------------------------------------------------
likes = set()
post_likes = []
while len(post_likes) < 75:
    post = rng.choice(posts)
    uid = rng.randint(1, len(users))
    if uid == post[1] or (post[0], uid) in likes:
        continue
    likes.add((post[0], uid))
    created = datetime.fromisoformat(post[4][11:-1]) + timedelta(minutes=rng.randint(1, 3000))
    post_likes.append((post[0], uid, Raw(ts(created))))

# ---- write -----------------------------------------------------------
tables_in_delete_order = ["post_likes", "comments", "posts", "friendships", "goals",
                          "workout_exercises", "workouts", "exercises", "workout_types", "users"]
sql = [
    "-- =====================================================================",
    "-- Fitly seed data (test data for development). GENERATED FILE.",
    "-- Do not edit by hand: change scripts/generate_seed_data.py and re-run it.",
    "--",
    "-- This is a Flyway *repeatable* migration (R__ prefix). Flyway runs it after",
    "-- the versioned migrations, and runs it again whenever this file changes.",
    "-- It first empties every table, so running it again never duplicates rows.",
    "-- Every seeded user can log in with the password  Fitly2026!",
    "-- =====================================================================",
    "",
]
sql += [f"DELETE FROM {t};" for t in tables_in_delete_order]
sql.append("")
sql.append(insert("users", ["id", "email", "password_hash", "display_name", "bio", "created_at"], users))
sql.append(insert("workout_types", ["id", "name", "category"], workout_types))
sql.append(insert("exercises", ["id", "name", "muscle_group"], exercises))
sql.append(insert("workouts", ["id", "user_id", "workout_type_id", "workout_date", "duration_minutes",
                               "calories", "notes", "created_at"], workouts))
sql.append(insert("workout_exercises", ["id", "workout_id", "exercise_id", "sets", "reps", "weight_kg"],
                  workout_exercises))
sql.append(insert("goals", ["id", "user_id", "goal_type", "target_value", "start_date", "end_date",
                            "status", "created_at"], goals))
sql.append(insert("friendships", ["id", "requester_id", "addressee_id", "status", "created_at",
                                  "responded_at"], friendships))
sql.append(insert("posts", ["id", "user_id", "workout_id", "content", "created_at"], posts))
sql.append(insert("comments", ["id", "post_id", "user_id", "content", "created_at"], comments))
sql.append(insert("post_likes", ["post_id", "user_id", "created_at"], post_likes))
sql.append("-- Fill the cached counter (the intentional denormalization from V2).")
sql.append("UPDATE posts p SET like_count = (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id);\n")
sql.append("-- Rows above used fixed ids, so move each id counter past them.")
counts = {"users": len(users), "workout_types": len(workout_types), "exercises": len(exercises),
          "workouts": len(workouts), "workout_exercises": len(workout_exercises), "goals": len(goals),
          "friendships": len(friendships), "posts": len(posts), "comments": len(comments)}
for t, n in counts.items():
    sql.append(f"ALTER TABLE {t} ALTER COLUMN id RESTART WITH {n + 1};")
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(sql) + "\n")
counts["post_likes"] = len(post_likes)
print("wrote", OUT)
for t, n in counts.items():
    print(f"  {t:18} {n}")
