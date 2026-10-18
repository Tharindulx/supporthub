# Build understanding, then make it yours

## A complete walkthrough
1. Start the demo and sign in as `requester@supporthub.test` / `DemoUser!2026`.
2. Create a NETWORK ticket with HIGH priority. Note its SH number.
3. Sign out and sign in as `agent@supporthub.test` / `DemoAgent!2026`.
4. Open the ticket, assign Jamie Chen, select In progress and save.
5. Add a comment explaining your investigation. Try the Waiting status, resume work and resolve the ticket.
6. Return as the requester and verify that the comment, assignment and activity are visible.
7. Sign in as `admin@supporthub.test` / `DemoAdmin!2026` to view Team members. Register a new requester in another session, then promote that account to Agent.
8. Open the same ticket in two browser tabs. Save in one; saving the old version in the other should show a conflict. Refresh the ticket and retry.

## Trace the code
- Start at `frontend/src/app/app.component.ts`: a user action calls ApiService.
- Follow `/api/tickets` into `ApiController`, then `HelpdeskService.create`.
- Validation happens on request records in `Dtos`; authorization lives in the service, independent of hidden UI controls.
- Repository operations occur inside the service transaction. Flyway owns schema changes; Hibernate validates the schema at startup.
- Read `HelpdeskIntegrationTest` and explain why testing the requester boundary is more valuable than testing getters.
- Compare local H2 configuration with MySQL Compose. Both persist data; test H2 is isolated in memory.

## Engineering decisions to explain in an interview
Why server sessions? The UI and API share one origin, so HttpOnly session cookies avoid storing bearer tokens in JavaScript. Why CSRF? Browsers send cookies automatically. Why DTOs? Entities contain sensitive fields and persistence details. Why version checks? Two agents can edit the same ticket. Why no delete button? Support history should remain explainable.

## Ethical portfolio use
This is an AI-assisted implementation. Study it, run the tests, modify features yourself and describe your contribution honestly. The supplied resume describes completed work; use those statements only when they accurately reflect what you have built and understand. Do not claim independent authorship, real customer use, load testing or production deployment without that evidence. Demo people and tickets are fictional; the resume's personal information is not embedded in the application.

## Useful next exercises
Add a category filter end to end; write a failing test for it first. Add accessible keyboard shortcuts. Then investigate password reset with expiring one-use tokens, email notifications through an outbox, attachments with scanning, and shared sessions for multiple instances. These are extensions, not implemented features.
