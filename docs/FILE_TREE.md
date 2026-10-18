# Source tree

```text
supporthub/
  README.md
  compose.yaml
  Dockerfile
  .env.example
  backend/
    pom.xml
    src/main/java/lk/supporthub/
      SupportHubApplication.java
      Domain.java                 # Persisted model and enums
      Repositories.java           # Data access and locking
      Dtos.java                   # Validated request / response contracts
      SecurityConfig.java         # Session and CSRF security
      AuthController.java         # Registration, login, current account
      HelpdeskService.java        # Permissions and business transactions
      ApiController.java          # Ticket and team REST endpoints
      ApiErrors.java              # Consistent error responses
      SeedData.java               # Demo fixtures / production bootstrap
    src/main/resources/
      application.yml
      application-demo.yml
      application-prod.yml
      db/migration/V1__helpdesk.sql
    src/test/java/lk/supporthub/HelpdeskIntegrationTest.java
  frontend/
    package.json
    package-lock.json
    angular.json
    tsconfig.json
    proxy.conf.json
    src/
      index.html
      main.ts
      styles.css
      app/
        models.ts
        api.service.ts
        dialog-focus.directive.ts # Keyboard focus and Escape handling
        app.component.ts
        app.component.html
  scripts/
    dev.ps1
    demo.ps1
    browser-test.cjs
  docs/
    ARCHITECTURE.md
    FILE_TREE.md
    API.md
    LEARNING_GUIDE.md
    VERIFICATION.md
    evidence/mysql-tests.txt
    screenshots/
```

Build outputs (`target`, `dist`, `node_modules`), local databases, secrets and logs are ignored by Git.

CI lives at the repository root: `.github/workflows/supporthub.yml`. The distribution archive also includes the executable `backend/target/supporthub-1.0.0.jar`.
