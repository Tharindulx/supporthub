# SupportHub API

All endpoints use `/api`. JSON requests use `Content-Type: application/json`. Errors return a meaningful `message` (framework errors may return RFC problem details). Timestamps are UTC ISO 8601. IDs are database-generated integers.

## Authentication
1. `GET /auth/csrf` issues an XSRF-TOKEN cookie and returns `{token}`.
2. Include `X-XSRF-TOKEN: <token>` on every POST/PUT, including login and logout. Keep the cookie jar.
3. `POST /auth/register` accepts `{name,email,password}`. Passwords must contain 12–64 characters. Always creates REQUESTER; unknown fields are rejected.
4. `POST /auth/login` accepts `{email,password}` and returns the current person. Preserve the HttpOnly JSESSIONID cookie. Existing session IDs rotate on login.
5. `GET /auth/me` returns `{id,name,email,role,enabled}`.
6. `POST /auth/logout` returns 204 and invalidates the server session.

Angular sends the CSRF header automatically for same-origin requests. No permissive CORS policy is configured. Password hashes are never returned.

## Tickets
| Method / path | Request / response |
|---|---|
| GET /tickets | Optional `q`, `status`, `priority`, `mine=true`, zero-based `page`. Fixed 12 per page, latest updated first. Returns `{content,totalElements,totalPages,number}`. |
| POST /tickets | `{title,description,category,priority}` → 201 ticket. |
| GET /tickets/{id} | `{ticket,comments,activities}` scoped to the current user. |
| PUT /tickets/{id} | Staff only: `{version,status,priority,assigneeId}`. `null` unassigns. → updated ticket. |
| POST /tickets/{id}/comments | `{body}` up to 4000 characters → 201 comment. Closed tickets reject comments. |
| GET /dashboard | `{total,open,inProgress,waiting,resolved,closed,urgent}` scoped to the current user. Urgent counts active tickets only. |
| GET /agents | Active agents and administrators; staff only. |
| GET /users | Full team directory; admin only. |
| PUT /users/{id} | `{role,enabled}`; admin only. Cannot change oneself; active assigned tickets must be reassigned before disabling/demoting staff. |
| GET /health | Public liveness response `{status:"UP"}`. This does not check database readiness. |

Example ticket:
```json
{"title":"VPN connection fails","description":"The VPN reports a timeout after signing in.","category":"NETWORK","priority":"HIGH"}
```

Example update (use the version returned from GET):
```json
{"version":0,"status":"IN_PROGRESS","priority":"HIGH","assigneeId":2}
```

Status transitions and roles are documented in ARCHITECTURE.md. Requester attempts to access another person's ticket return 404. Stale versions return 409. Invalid transitions, malformed enums and validation failures return 400. Unauthenticated requests return 401; insufficient permissions or missing CSRF return 403. Duplicate emails return 409.

The ticket mutation and corresponding activity entries commit atomically. Comments touch the ticket version, so staff must refresh before saving if a comment arrived in the meantime. Search escapes SQL LIKE wildcard characters and uses parameterized criteria queries.
