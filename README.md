# SupportHub

A full-stack helpdesk with Angular, TypeScript, Java 21, Spring Boot 3.5, Maven, Spring Data JPA, MySQL and JUnit. Includes a responsive workspace, role-based access, ticket creation, assignment, priorities, lifecycle tracking, comments, an activity trail and team administration.

Start with [architecture](docs/ARCHITECTURE.md), [file tree](docs/FILE_TREE.md), [API reference](docs/API.md), [walkthrough](docs/LEARNING_GUIDE.md) and [verification evidence](docs/VERIFICATION.md).

## Run on this Windows PC
From this folder:
```powershell
.\scripts\dev.ps1 -Action run
```
Open **http://localhost:8082**. Stop with Ctrl+C. The demo is bound to loopback and persists data in `backend/data`. Java/Maven reuse `D:\DeveloperTools\GamingPlatform`; Angular dependencies and npm cache are in `D:\DeveloperTools\SupportHub`. The frontend `node_modules` is a junction to D. No system PATH changes are made.

| Demo role | Email | Password |
|---|---|---|
| Administrator | admin@supporthub.test | DemoAdmin!2026 |
| Agent | agent@supporthub.test | DemoAgent!2026 |
| Requester | requester@supporthub.test | DemoUser!2026 |

These fictional accounts are created only in the `demo` profile, on an empty database. Use **Create an account** for additional requesters. Existing data is preserved on restart. Do not expose demo mode to the network.

To manage the demo in the background on this PC, use `scripts/demo.ps1 -Action start` and `scripts/demo.ps1 -Action stop`. Logs are in `logs/`. This helper checks the saved process identity before stopping it.

## Build and test
```powershell
.\scripts\dev.ps1 -Action install  # Restore locked Angular dependencies on D
.\scripts\dev.ps1 -Action build    # Angular production build + JUnit + executable JAR
.\scripts\dev.ps1 -Action test     # JUnit API and security tests
```
The `run` action uses the packaged JAR; run `build` after source changes. Reports are in `backend/target/surefire-reports`. The distributable JAR includes the compiled Angular app.

For another machine with Node 24 and Java 21/Maven installed:
```powershell
cd frontend
npm ci
npm run build
cd ..
New-Item -ItemType Directory -Force backend/src/main/resources/static
Copy-Item frontend/dist/supporthub/browser/* backend/src/main/resources/static -Recurse -Force
cd backend
mvn clean verify
java -jar target/supporthub-1.0.0.jar --spring.profiles.active=demo
```

For live frontend development, run the backend and `npm start` in `frontend`; open http://localhost:4200. Angular proxies `/api` to port 8082.

The same JUnit suite can run against a disposable MySQL database using `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`, followed by `mvn test`. **Use a dedicated test database:** the suite clears its accounts, tickets, comments and activities before each test. CI at `.github/workflows/supporthub.yml` provisions an isolated MySQL service.

`scripts/browser-test.cjs` runs a real Chrome acceptance workflow with Playwright. It creates a ticket and a requester; run it against a disposable demo database. Set `PLAYWRIGHT_MODULE` to an existing Playwright module directory (or install Playwright in your own development environment) and run `node scripts/browser-test.cjs`. The script saves screenshots and checks mobile page width.

## MySQL with Docker Compose
Start Docker Desktop with Linux containers, then:
```powershell
Copy-Item .env.example .env
# Replace every example value with your own credentials.
docker compose up --build -d
docker compose logs -f app
docker compose down
```
Open http://localhost:8082. Compose binds only to loopback, uses MySQL 8.4 and does not publish the database port. Named volume `mysql-data` survives `down`. Never use `down -v` unless you intend to delete that database. The bootstrap administrator is created if its email does not exist; changing the environment password does not reset an existing user's password.

For internet deployment, put the application behind HTTPS, activate the `prod` Spring profile for Secure cookies, configure secrets through your host's secret manager, and add ingress login rate limits, backups and monitoring. The current app is a single-instance portfolio implementation. It does not include password recovery, email delivery, file attachments, shared sessions or an external identity provider. Review dependencies and deployment configuration before public exposure. No cloud deployment has been performed.

## Security and data
Server-side authorization applies on every business operation, independently of UI controls. Passwords are BCrypt hashed. Authentication uses HttpOnly sessions and CSRF protection. Account changes take effect for existing sessions on their next business request. Only staff can assign or change ticket state. The API never returns password hashes. Ticket updates include version checks and transaction locks; immutable activity entries explain changes. Search and list results are scoped to the requester. There is no hard-delete API.

Framework compatibility references: [Angular version matrix](https://angular.dev/reference/versions), [Spring Boot system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
