# social-fitness-app

Fitly is a social fitness app MVP: track workouts and connect with friends.
Built with Java (Spring Boot + Maven) and plain HTML/CSS.

## Project layout
- `src/main/java/com/fitly/app` : Java backend (login, register, session filter)
- `src/main/resources/static` : HTML and CSS pages served by the app
- `src/test/java/com/fitly/app` : automated tests

## Run it in IntelliJ
1. File > Open, then pick the repo folder (the one with `pom.xml`).
2. Wait for Maven to finish loading. Use JDK 17 or newer (21 recommended).
3. Open `FitlyApplication.java` and click the green Run arrow next to `main`.
4. Visit http://localhost:8080

## Run the tests
Right click `src/test/java` and choose Run 'All Tests', or run `mvn test`.

## Adding a protected page
Any page that requires login must be added to `SecurityConfig.java`.

## Known temporary shortcuts
- Users are stored in memory and vanish on restart (`UserStore`).
- Passwords are not hashed yet. Fix this before any real use.
