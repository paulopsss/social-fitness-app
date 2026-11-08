# social-fitness-app

Fitly is a social fitness app MVP: track workouts and connect with friends.
Built with Java (Spring Boot + Maven) and plain HTML/CSS.

## Project layout
```
src/main/java/com/fitly/app
  FitlyApplication            app entry point
  config/      SecurityConfig             which pages need login
  web/         AuthController, SessionAuthFilter, LoginSession, Pages
  service/     AuthService, WorkoutService, WorkoutObserver,
               GoalProgressObserver, NotificationObserver
  security/    PasswordHasher, Pbkdf2PasswordHasher
  repository/  UserRepository, JpaUserRepository (H2), InMemoryUserRepository (unit tests)
  model/       User, Workout
  persistence/entity/      Hibernate entity classes, one per table
  persistence/repository/  Spring Data JPA repositories
src/main/resources/static     HTML and CSS pages served by the app
src/main/resources/db/migration  Flyway schema migrations (V1, V2, ...)
src/main/resources/db/seed       Flyway seed data (test data, 20+ rows per table)
scripts/generate_seed_data.py    regenerates the seed file
src/test/java                 automated tests
docs/                         design patterns and SOLID report, plus the original "before" code
```
Dependencies point one way: web -> service -> repository / security -> model.
Nothing in `service`, `repository`, `security`, or `model` imports servlet or web classes.

## Run it in IntelliJ
1. File > Open, then pick the repo folder (the one with `pom.xml`).
2. Wait for Maven to finish loading. Use JDK 17 or newer (21 recommended).
3. Open `FitlyApplication.java` and click the green Run arrow next to `main`.
4. Visit http://localhost:8080

## Run the tests
Right click `src/test/java` and choose Run 'All Tests', or run `mvn test`.

## Design patterns and SOLID (this branch)
- Observer: `WorkoutService` + `WorkoutObserver` (Log Workout)
- Repository: `UserRepository`
- Strategy: `PasswordHasher`
- Chain of Responsibility: `SessionAuthFilter`
- Report: `docs/Fitly_Design_Patterns_SOLID.pdf`
- Original code before the refactor: `docs/before/`

## Adding a protected page
Add its path to `Pages` and to `SecurityConfig.java`.

## Database (H2 + Flyway + Hibernate)
- Starting the app creates `./data/fitly.mv.db` and runs every migration in
  `db/migration`, then the seed data in `db/seed`. Nothing to install.
- To start from scratch, stop the app and delete the `data` folder.
- Browse the tables at http://localhost:8080/h2-console
  (JDBC URL `jdbc:h2:file:./data/fitly;AUTO_SERVER=TRUE`, user `sa`, no password).
- Every seeded user (for example `paulo@fitly.test`) logs in with `Fitly2026!`.
- Change the schema by adding a new file such as `V3__add_something.sql`.
  Never edit a migration that has already been merged.
- Hibernate runs with `ddl-auto=validate`: it never changes tables, it only
  checks the entity classes match them.
- Tests use a separate in-memory database (`src/test/resources/application.properties`).

## Known temporary shortcuts
- Logged workouts and goal progress are still kept in memory; the tables and
  entities exist, but WorkoutService does not save to them yet.
- The H2 console is on for development. Turn it off before any real deployment.
