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
  repository/  UserRepository, InMemoryUserRepository
  model/       User, Workout
src/main/resources/static     HTML and CSS pages served by the app
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

## Known temporary shortcuts
- Users and workout results are kept in memory and vanish on restart.
- The H2 database (ADR 5) is chosen but not added yet.
