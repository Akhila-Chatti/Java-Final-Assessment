# SmartDesk – Manual API Test Guide (Postman)

Every call goes through the **API Gateway: `http://localhost:8080`**.
Run the steps **in order** – later steps use the token / id you copied earlier.

---

## 0. Setup (once)

1. Start: Eureka (8761) → api-gateway (8080) → ticket (8081) → ai-triage (8082) → notification (8083).
   Open http://localhost:8761 – you must see 4 apps: `API-GATEWAY, TICKET-SERVICE, AI-TRIAGE-SERVICE, NOTIFICATION-SERVICE`.
2. In Postman create an **Environment** called `SmartDesk` with these variables (leave values empty, you fill them while testing):

   | Variable | You fill it from |
   |---|---|
   | `baseUrl` | `http://localhost:8080` |
   | `adminToken` `mgrToken` `agentToken` `empToken` | `accessToken` of each verify-OTP |
   | `empId` `agentId` `mgrId` | `id` of each register |
   | `ticketId` `linkedId` `opsId` | `id` of each created ticket |

3. For every request:
   - **Headers** tab → `Content-Type: application/json`
   - Protected calls → **Authorization** tab → Type **Bearer Token** → `{{empToken}}` (or the one the step says)
   - **Body** → **raw** → **JSON**

4. Pick a number for this test run, e.g. `101`. Use it in all emails (`ravi101@corp.com`).
   Next time use `102` – otherwise register says *400 An active account already exists*.

> **There are no passwords.**
> - **Register** is 2 steps: `register/request` (fullName + email) → returns `otp` → `register/verify` → account becomes active + `accessToken`.
> - **Login** is 2 steps: `otp/request` (email only) → returns `otp` → `otp/verify` → `accessToken`.
> - An OTP is valid **5 minutes**, a new request replaces the old one, and it locks after **3 wrong tries**.

---

## 1. Auth & roles

### 1.1 Admin login – request OTP
`POST {{baseUrl}}/api/v1/auth/otp/request`
```json
{ "email": "admin@smartdesk.local" }
```
✅ **200**
```json
{ "message": "Verification code sent to your email.", "status": 200, "timestamp": "2026-11-13T18:00:00", "otp": "591286" }
```
👉 copy `otp`.

### 1.2 Admin login – verify OTP
`POST {{baseUrl}}/api/v1/auth/otp/verify`
```json
{ "email": "admin@smartdesk.local", "otp": "591286" }
```
✅ **200**
```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "user": { "id": "fde8...", "fullName": "System Admin", "email": "admin@smartdesk.local",
            "role": "ADMIN", "department": null, "isRootAdmin": true, "isActive": true,
            "createdAt": "2026-11-13T06:47:00Z" }
}
```
👉 copy `accessToken` → **`adminToken`**.

### 1.3 Register 3 users (2 calls each, no token)
**a)** `POST {{baseUrl}}/api/v1/auth/register/request`

| Who | Body |
|---|---|
| Ravi (employee) | `{ "fullName": "Ravi Kumar", "email": "ravi101@corp.com" }` |
| Ram (future agent) | `{ "fullName": "Ram Teja", "email": "ram101@corp.com" }` |
| Sita (future manager) | `{ "fullName": "Sita Rao", "email": "sita101@corp.com" }` |

✅ **200** – `{ "message": "Verification code sent to your email.", "status": 200, ..., "otp": "123456" }` 👉 copy `otp`.
The account exists now but is **not active** (`account_status = PENDING`) – it cannot log in yet.

**b)** `POST {{baseUrl}}/api/v1/auth/register/verify` – `{ "email": "ravi101@corp.com", "otp": "123456" }`
✅ **200** – same shape as 1.2, `"isActive": true`, `"role": "EMPLOYEE"` 👉 save `user.id` as `empId` (Ram → `agentId`, Sita → `mgrId`).
Self-signup is **always EMPLOYEE** (a `role` sent in the body is ignored).

### 1.4 ❌ Register Ravi again
`POST /api/v1/auth/register/request` with Ravi's body →
✅ **400**
```json
{ "status": 400, "error": "Bad Request", "message": "An active account already exists with this email address. Please sign in instead.", "timestamp": "..." }
```

### 1.5 ❌ Unknown email
`POST /api/v1/auth/otp/request` `{ "email": "nobody101@corp.com" }`
✅ **401** – `"message": "Email is not registered"`

### 1.6 Admin lists employees
`GET {{baseUrl}}/api/v1/admin/users?role=EMPLOYEE` – Bearer `{{adminToken}}`
✅ **200** – array of users, Ravi / Ram / Sita are in it with `"role": "EMPLOYEE"`.

### 1.7 Admin makes Sita MANAGER of IT
`PATCH {{baseUrl}}/api/v1/admin/users/{{mgrId}}/role` – Bearer `{{adminToken}}`
```json
{ "role": "MANAGER", "department": "IT" }
```
✅ **200** – `"role": "MANAGER", "department": "IT"`

### 1.8 Sita logs in (otp/request → otp/verify, like 1.1/1.2)
Email `sita101@corp.com`.
✅ verify returns `"user": { "role": "MANAGER", "department": "IT" }` 👉 save **`mgrToken`**.

> A role change only shows up in the **next login** – that is why Sita logs in *after* 1.7.

### 1.9 Manager Sita makes Ram AGENT of IT
`PATCH {{baseUrl}}/api/v1/admin/users/{{agentId}}/role` – Bearer `{{mgrToken}}`
```json
{ "role": "AGENT", "department": "IT" }
```
✅ **200** – `"role": "AGENT", "department": "IT"`

### 1.10 ❌ Sita tries to make Ram an HR agent
Same URL, body `{ "role": "AGENT", "department": "HR" }`
✅ **403** – a manager can only create agents of **her own** department.

### 1.11 Ram logs in → save **`agentToken`** (role must show `AGENT`, dept `IT`)
### 1.12 Ravi logs in → save **`empToken`** (role `EMPLOYEE`)

### 1.13 ❌ Wrong OTP
`POST /api/v1/auth/otp/verify` `{ "email": "ravi101@corp.com", "otp": "000000" }`
✅ **401** – `"message": "Invalid OTP. Attempts remaining: 2"` (after 3 wrong tries: `"Maximum verification attempts exceeded. Please request a new OTP."`)

### 1.14 ❌ Employee calls admin API
`GET /api/v1/admin/users` – Bearer `{{empToken}}`
✅ **403** (empty body)

---

## 2. Ticket workflow

Every ticket response has this shape (example):
```json
{
  "id": 5,
  "title": "Wifi not working",
  "description": "Wifi keeps dropping on floor 3 since morning",
  "status": "OPEN",
  "department": "IT",
  "category": "IT",
  "priority": "HIGH",
  "summary": "Wifi dropping on floor 3",
  "triageSource": "AI",
  "requesterId": "d6be1bed-...",
  "requesterName": "Ravi Kumar",
  "assigneeId": null,
  "assigneeName": null,
  "parentTicketId": null,
  "reportCount": 1,
  "createdAt": "2026-11-13T07:14:10Z",
  "resolvedAt": null
}
```
The frontend shows `id` 5 as **SD-5**.

### 2.1 Ravi creates a wifi ticket
`POST {{baseUrl}}/api/v1/tickets` – Bearer `{{empToken}}`
Header: `Idempotency-Key: 101-1`
```json
{ "title": "Wifi not working", "description": "Wifi keeps dropping on floor 3 since morning" }
```
✅ **201** – `"status": "OPEN"`, `"department": "IT"`, `"triageSource": "AI"` 👉 save `id` → **`ticketId`**

> If the AI service is down you still get 201, but `"department": "UNCLASSIFIED"`, `"triageSource": "FALLBACK"`.

### 2.2 Same request again (same Idempotency-Key)
Press **Send** again on 2.1, without changes.
✅ **200** (not 201) and the **same `id`** – no duplicate ticket created.

### 2.3 Sita reports the same wifi problem
`POST /api/v1/tickets` – Bearer `{{mgrToken}}` (no Idempotency-Key)
```json
{ "title": "Wifi down", "description": "The wifi is not working for my whole team" }
```
✅ **201** – `"parentTicketId": <ticketId>` 👉 save `id` → **`linkedId`**
It was linked to Ravi's ticket (same department + same problem within 4 hours).

### 2.4 Ravi: my tickets
`GET /api/v1/tickets` – Bearer `{{empToken}}`
✅ **200** – `content` has only Ravi's tickets, plus `page` info:
```json
{ "content": [ { "id": 5, ... } ], "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 } }
```
Optional filters: `?status=OPEN`, `?category=IT`, `?page=0&size=10`.

### 2.5 Ram: IT pool
`GET /api/v1/tickets/pool?size=100` – Bearer `{{agentToken}}`
✅ **200** – contains `ticketId`, does **not** contain `linkedId` (duplicates are hidden).

### 2.6 ❌ Employee opens the pool
`GET /api/v1/tickets/pool` – Bearer `{{empToken}}`
✅ **403** – `"detail": "Requires role [AGENT, MANAGER, ADMIN]"`

### 2.7 Ram: daily claim limit
`GET /api/v1/tickets/claim-limit` – Bearer `{{agentToken}}`
✅ **200** (numbers depend on today's tickets; `remaining` = `limit` − `used`)
```json
{ "used": 0, "limit": 5, "remaining": 5, "departmentInflowToday": 1, "activeAgents": 0 }
```

### 2.8 Ram claims the ticket
`POST /api/v1/tickets/{{ticketId}}/claim` – Bearer `{{agentToken}}` (no body)
✅ **200** – `"status": "IN_PROGRESS"`, `"assigneeName": "Ram Teja"`

### 2.9 ❌ Ram claims it again
Same request again →
✅ **409** – `"detail": "Illegal status change IN_PROGRESS -> IN_PROGRESS"`

### 2.10 Ram: my bucket
`GET /api/v1/tickets/assigned` – Bearer `{{agentToken}}`
✅ **200** – `content` contains `ticketId`.

### 2.11 ❌ Ram jumps straight to RESOLVED
`PATCH /api/v1/tickets/{{ticketId}}/status` – Bearer `{{agentToken}}`
```json
{ "status": "RESOLVED" }
```
✅ **400** – `"detail": "RESOLVED is set via /claim, /assign or /verify"` (only the employee can resolve)

### 2.12 Ram: "fixed, please check"
Same URL, body `{ "status": "WAITING_ON_EMPLOYEE" }`
✅ **200** – `"status": "WAITING_ON_EMPLOYEE"`

### 2.13 Ravi rejects the fix
`POST /api/v1/tickets/{{ticketId}}/verify` – Bearer `{{empToken}}`
```json
{ "accepted": false }
```
✅ **200** – `"status": "IN_PROGRESS"` (back to Ram)

### 2.14 Ram marks fixed again
Repeat 2.12 → ✅ **200**, `WAITING_ON_EMPLOYEE`

### 2.15 ❌ Ram tries to confirm for Ravi
`POST /api/v1/tickets/{{ticketId}}/verify` – Bearer `{{agentToken}}`, body `{ "accepted": true }`
✅ **403** – `"detail": "Only the requester can confirm a resolution"`

### 2.16 Ravi confirms
Same, but Bearer `{{empToken}}`
✅ **200** – `"status": "RESOLVED"`, `"resolvedAt"` is filled

### 2.17 Sita's linked ticket followed along
`GET /api/v1/tickets/{{linkedId}}` – Bearer `{{mgrToken}}`
✅ **200** – `"status": "RESOLVED"` (it mirrors the parent incident)

### 2.18 Ravi closes the ticket
`PATCH /api/v1/tickets/{{ticketId}}/status` – Bearer `{{empToken}}`, body `{ "status": "CLOSED" }`
✅ **200** – `"status": "CLOSED"`

### 2.19 ❌ Change after CLOSED
Same URL – Bearer `{{agentToken}}`, body `{ "status": "WAITING_ON_EMPLOYEE" }`
✅ **409** – `"detail": "Illegal status change CLOSED -> WAITING_ON_EMPLOYEE"`

### 2.20 Ravi asks for a new chair
`POST /api/v1/tickets` – Bearer `{{empToken}}`
```json
{ "title": "Broken chair", "description": "My chair is broken, need a replacement" }
```
✅ **201** – `"department": "OPERATIONS"` 👉 save `id` → **`opsId`**

### 2.21 ❌ IT manager re-routes an OPERATIONS ticket
`PATCH /api/v1/tickets/{{opsId}}/triage` – Bearer `{{mgrToken}}`
```json
{ "category": "IT", "priority": "LOW" }
```
✅ **403** – `"detail": "Not your department"`

### 2.22 Admin overrides the AI
Same URL – Bearer `{{adminToken}}`, body `{ "category": "HR", "priority": "LOW" }`
✅ **200** – `"department": "HR"`, `"priority": "LOW"`, `"triageSource": "MANUAL"`

### 2.23 Manager summary
`GET /api/v1/tickets/manager-summary` – Bearer `{{mgrToken}}`
✅ **200** (numbers depend on your data)
```json
{ "byStatus": { "CLOSED": 1 }, "byCategory": { "IT": 2 }, "avgResolutionHours": 0.01 }
```

### 2.24 Admin: closed tickets
`GET /api/v1/tickets?status=CLOSED&page=0&size=10` – Bearer `{{adminToken}}`
✅ **200** – Ravi's wifi ticket is in `content`.

### 2.25 Ravi: my notifications (simulated emails)
`GET /api/v1/notifications/me` – Bearer `{{empToken}}`
✅ **200**
```json
{
  "content": [
    { "id": "...", "ticketId": 5, "eventType": "STATUS_CHANGED",
      "subject": "...", "message": "...", "deliveryStatus": "SENT", "createdAt": "..." }
  ],
  "page": { ... }
}
```
You should see: `OTP`, `TICKET_CREATED`, `STATUS_CHANGED` (claimed, closed…), `VERIFICATION_REQUESTED` (twice).
Emails are sent in the background – if one is missing, wait 2 s and send again.

---

## 3. Security checks

| # | Request | Expected |
|---|---|---|
| 3.1 | `GET /api/v1/tickets` with **no** Authorization | **401** |
| 3.2 | `GET /api/v1/tickets/manager-summary` – Bearer `{{empToken}}` + header `X-User-Role: ADMIN` | **403** – the gateway throws away fake `X-User-*` headers |
| 3.3 | `POST /api/v1/triage` – Bearer `{{empToken}}`, body `{ "title": "x", "description": "y" }` | **404** – the AI service is not reachable from outside |
| 3.4 | `POST /api/v1/tickets` – Bearer `{{empToken}}`, body `{ "title": "" }` | **400** – `"detail": "Validation failure"` |

---

## 4. Check the database (pgAdmin)

Right-click the database → **Query Tool** (the tab title must show that database name).

**smartdesk_auth_db**
```sql
SELECT email, role, department FROM users ORDER BY email;  -- Sita MANAGER/IT, Ram AGENT/IT, Ravi EMPLOYEE
SELECT user_id, expires_at FROM otps;                      -- OTP is stored hashed, never plain
```

**smartdesk_ticket_db**
```sql
SELECT ticket_id, title, status, department_name, issue_key, created_at, resolved_at FROM tickets ORDER BY ticket_id;
-- wifi ticket: CLOSED ; Sita's: same issue_key, RESOLVED (API shows parentTicketId = wifi id, reportCount 2) ; chair: HR

SELECT ticket_id, category, priority, source FROM ai_analysis_results;   -- chair: source MANUAL
SELECT ticket_id, agent_id, assigned_at, completed_at FROM ticket_assignments;  -- Ram's user id is the agent_id of the wifi ticket
```

**smartdesk_notification_db**
```sql
SELECT event_type, recipient_email, subject, ticket_id, delivery_status
FROM notifications ORDER BY created_at DESC;
```

---

## 5. Common problems

| You see | Why | Fix |
|---|---|---|
| **401** on login | email not registered (or typo) | register first |
| **401** on everything | token missing / expired (8 h) / wrong variable | log in again, check Authorization tab |
| **503** "Could not deliver the OTP" on login | notification-service down or not in Eureka yet | start it, wait ~30 s, retry |
| **400** on register/request | email already active | change the run number (`ravi102@...`) |
| **401** on otp/verify with the right code | OTP older than 5 min, 3 wrong tries, or you requested a new one | request a new OTP, use the newest one |
| ticket `department` = `UNCLASSIFIED` | AI service down | still OK; a manager fixes it with `PATCH /triage`, or it is re-classified within 5 min |
| Sita's ticket **not** linked (no `parentTicketId`) | AI gave a different `issueKey`, or Ravi's ticket is older than 4 h / already closed | create it right after 2.1 |
| **429** on claim | agent reached today's claim limit | expected; see `/claim-limit` |
| **500** | look at that service's console in IntelliJ | send me the error lines |
