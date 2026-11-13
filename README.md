# SmartDesk – AI helpdesk (Spring Boot 3.5 · Java 25 · microservices)

| Service | Port | Database | Does |
|---|---|---|---|
| eureka-server | 8761 | – | Service registry (dashboard: http://localhost:8761) |
| api-gateway | **8080** | smartdesk_auth_db | Register, login + OTP, JWT, roles, routes everything |
| ticket-service | 8081 | smartdesk_ticket_db | Tickets, pool, claim, daily limit, verify, incidents, summary |
| ai-triage-service | 8082 | Redis (cache) | Masks personal data, classifies ticket (LLM or keyword demo mode) |
| notification-service | 8083 | smartdesk_notification_db | Simulated emails (console log) + "my notifications" |

Each service: `controller / service / dto / entity / repository / mapper / exception / client / config`.

---

## 1. Start everything

### Option A – Docker (easiest)
```bash
docker compose up --build
```
First build takes a few minutes. Then wait **~1 minute** and open http://localhost:8761. You should see
`API-GATEWAY, TICKET-SERVICE, AI-TRIAGE-SERVICE, NOTIFICATION-SERVICE` registered.
If a request gives **503** right after start, a service hasn't registered yet. Wait 30 s and retry.

### Option B – IntelliJ (JDK 25)
1. Start Postgres + Redis only: `docker compose up postgres redis`
   (or your own Postgres: run the scripts in `infra/db/` (3 databases + all tables), and set `DB_USER` / `DB_PASSWORD` for each service).
   No Redis? Set env `CACHE_TYPE=simple` on ai-triage-service.
2. Enable annotation processing: *Settings → Build → Compiler → Annotation Processors → Enable*.
3. Run in this order: `EurekaServerApplication` → `NotificationApplication` → `AiTriageApplication`
   → `TicketServiceApplication` → `GatewayApplication`.

Default admin: `admin@smartdesk.local` (created automatically on first start). There are no passwords: log in with your email, then the 6-digit OTP.

---

## 2. Test with Postman

1. Postman → **Import** → `postman/SmartDesk.postman_collection.json`
2. Right-click the collection → **Run collection** → **Run**.
3. All **47 requests** should be green. Tokens, OTPs and IDs are passed between requests automatically, and
   every run uses new emails, so you can run it again and again.

Run single requests by hand top to bottom if you want to watch each step. Each one depends on the ones above it.

### What each folder proves

**1. Auth & roles**
| Request | Expected | Shows |
|---|---|---|
| Admin login → verify OTP | 200, token | OTP login. In demo mode the OTP is in the response; it's also in the notification-service console |
| Register Ravi / Ram / Sita | 201 | Sign-up always creates EMPLOYEE |
| Register Ravi again | 400 | Unique email |
| Unknown email on login | 401 | |
| Admin makes Sita MANAGER of IT | 200 | Admin power |
| Sita makes Ram AGENT of IT | 200 | Manager can recruit agents for **her** department |
| Sita makes Ram HR agent | 403 | …but not for other departments |
| Wrong OTP | 401 | |
| Employee calls admin API | 403 | Role check in the gateway |

**2. Ticket workflow**
| Request | Expected | Shows |
|---|---|---|
| Ravi creates wifi ticket | 201, department IT | AI triage (category, priority, summary) |
| Same Idempotency-Key again | 200, same id | **Idempotency**: no duplicate on double-click/retry |
| Sita reports wifi too | 201, `parentTicketId` = Ravi's ticket | **Incident grouping**: same problem → linked, not a new pool item |
| Ram: IT pool | incident shown, duplicate hidden | Department pool |
| Employee opens pool | 403 | |
| Ram: claim-limit | `used / limit / remaining` | **Daily limit** |
| Ram claims → claims again | 200 → 409 | Pool → bucket; no double claim |
| Ram sets RESOLVED directly | 400 | Agent **can't** resolve alone |
| Ram → WAITING_ON_EMPLOYEE | 200 | Asks Ravi to confirm |
| Ravi rejects | IN_PROGRESS | Back to the agent |
| Ram tries to confirm for Ravi | 403 | Only the requester verifies |
| Ravi confirms | RESOLVED | |
| Sita's linked ticket | RESOLVED too | Duplicates follow their incident |
| Ravi closes → change after close | 200 → 409 | Status state machine |
| Chair ticket | department OPERATIONS | Different category |
| IT manager re-routes it | 403 | |
| Admin overrides AI → HR | 200 | **Human override** |
| Manager summary / admin list | 200 | Counts + average resolution hours |
| Ravi: my notifications | 200 | Every email that was sent to him |

**3. Security checks**
| Request | Expected | Shows |
|---|---|---|
| No token | 401 | |
| Employee sends fake `X-User-Role: ADMIN` | 403 | Gateway strips spoofed identity headers |
| Call `/api/v1/triage` from outside | 404 | Internal services are not exposed |

### Where to "see it working"
- **Emails**: notification-service console shows `OUTGOING EMAIL SIMULATION` blocks (OTP, created, assigned, confirm-fix, status).
- **Fallback**: stop ai-triage-service and create a ticket. It is still created, as `UNCLASSIFIED`, and shows in the
  manager's pool. When the AI is back, a job re-classifies it within 5 minutes.
- **Daily limit (429)**: restart ticket-service with `CLAIM_LIMIT_BASE=1 CLAIM_LIMIT_BUFFER=0 CLAIM_LIMIT_MAX=1`, then
  let one agent claim two different tickets. The second claim returns 429 with `used` and `limit`.
- **Database**: connect to `localhost:5432` (smartdesk/smartdesk) and look at `tickets`, `ai_analysis_results`,
  `ticket_assignments`, `users`, `notifications`.

---

## 3. All APIs (via gateway http://localhost:8080, header `Authorization: Bearer <token>`)

| Method | Path | Who |
|---|---|---|
| POST | /api/v1/auth/register/request `{fullName, email}` | public |
| POST | /api/v1/auth/register/verify `{email, otp}` | public |
| POST | /api/v1/auth/otp/request `{email}` | public |
| POST | /api/v1/auth/otp/verify `{email, otp}` | public |
| GET | /api/v1/admin/users?role=&department= | ADMIN, MANAGER |
| PATCH | /api/v1/admin/users/{id}/role | ADMIN, MANAGER |
| POST | /api/v1/tickets (header `Idempotency-Key` optional) | any |
| GET | /api/v1/tickets?mine=&status=&category=&page=&size= | any (scoped by role) |
| GET | /api/v1/tickets/{id} | requester, department staff, admin |
| GET | /api/v1/tickets/pool | AGENT, MANAGER, ADMIN |
| GET | /api/v1/tickets/assigned | AGENT |
| GET | /api/v1/tickets/claim-limit | AGENT |
| POST | /api/v1/tickets/{id}/claim | AGENT |
| POST | /api/v1/tickets/{id}/assign `{agentId}` | MANAGER, ADMIN |
| PATCH | /api/v1/tickets/{id}/status `{status}` | agent → WAITING_ON_EMPLOYEE; requester/manager → CLOSED |
| POST | /api/v1/tickets/{id}/verify `{accepted}` | requester |
| PATCH | /api/v1/tickets/{id}/triage `{category, priority}` | MANAGER, ADMIN |
| GET | /api/v1/tickets/manager-summary | MANAGER, ADMIN |
| GET | /api/v1/notifications/me | any |

Ticket lifecycle: `OPEN → IN_PROGRESS → WAITING_ON_EMPLOYEE → RESOLVED → CLOSED` (employee can reject back to IN_PROGRESS).

## 4. Settings
| Env var | Default | |
|---|---|---|
| `LLM_API_KEY` | empty | empty = keyword demo mode; set a key to use OpenAI |
| `LLM_BASE_URL`, `LLM_MODEL` | OpenAI, gpt-4o-mini | Gemini: `https://generativelanguage.googleapis.com/v1beta/openai`, `gemini-2.0-flash` |
| `OTP_EXPOSE` | true | OTP in login response for Postman. **Set false in production** |
| `JWT_SECRET` | demo value | at least 32 characters |
| `CLAIM_LIMIT_BASE/BUFFER/MAX` | 5 / 2 / 25 | daily claim limit |
