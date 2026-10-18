# Verification record

Verified on 19 October 2026 (Asia/Colombo) on this Windows PC.

| Check | Result |
|---|---|
| Angular strict TypeScript and template compilation | Passed; production bundle approximately 246 kB raw / 66 kB estimated transfer |
| Java 21 / Spring Boot Maven package | Passed; executable JAR contains Angular frontend |
| JUnit/MockMvc with H2 | 6 tests, 0 failures, 0 errors, 0 skipped |
| Same JUnit/MockMvc suite with real MySQL 8.0.46 | 6 tests, 0 failures, 0 errors, 0 skipped; see evidence/mysql-tests.txt |
| Flyway migration and Hibernate schema validation on MySQL | Passed on isolated database supporthub_test, port 3308 |
| Chrome end-to-end workflow | Passed: sign-in, ticket creation, assignment, status changes, comments, team view, sign-out, requester isolation and registration |
| Desktop and 390-pixel mobile screenshots | Inspected; mobile horizontal page overflow fixed |
| Dialog Escape handling | Passed in browser workflow; keyboard focus remains within dialog while open |
| Browser JavaScript errors | None during final acceptance workflow |
| npm audit of frontend runtime dependency graph | 0 reported vulnerabilities at verification time; not a guarantee of future safety |

## Test coverage
The six JUnit scenarios cover authentication/session logout/CSRF, registration validation and privilege escalation prevention, requester isolation across list/detail/comments/dashboard, assignment and lifecycle rules, stale-version conflicts, account administration and live disabling, and malformed requests. Tests use the actual Spring security filter chain and database repositories. Tests are not a load test or penetration test.

Browser checks use the real Angular production build served by Spring Boot. They run against a separate in-memory demo database. Final local demo uses a fresh persistent H2 database, so browser-test accounts and tickets are not carried into the user's workspace.

## Boundaries
Docker Desktop did not become available in this environment. The Docker image, Compose MySQL 8.4 deployment, GitHub Actions runner and internet deployment were **not executed**. Real MySQL validation used the already installed MySQL 8.0.46 binary with a separate data directory at `D:\DeveloperTools\SupportHub\mysql-test`, not the user's normal MySQL service. The test instance was stopped after verification.

No email delivery, attachments, password recovery, shared-session scaling or cloud deployment is claimed. Production exposure requires the deployment steps in README.md, including HTTPS and ingress authentication rate limits.

## Reproduce
- `scripts/dev.ps1 -Action build` builds the frontend and executes the default H2 suite.
- For MySQL, configure `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` for a dedicated disposable database, then run `mvn test` in backend. **Tests clear that database's application tables.**
- `node scripts/browser-test.cjs` runs against port 8082; requires Playwright and Chrome. Set `PLAYWRIGHT_MODULE` if using a bundled module installation. Use a disposable demo database because this test creates records.
- Screenshots in `docs/screenshots` show the final tested frontend.
