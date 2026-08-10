# API Documentation

Base URL (via API Gateway): `http://localhost:8080`

All authenticated endpoints require:

```
Authorization: Bearer <accessToken>
```

Content type for request bodies: `application/json`

Error responses use [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807) where applicable.

---

## Authentication

### Register user

Creates a new user account and returns a JWT.

**`POST /api/v1/auth/register`**

No authentication required.

**Request body:**

```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "password123"
}
```

| Field | Type | Rules |
|-------|------|-------|
| fullName | string | Required, max 100 chars |
| email | string | Required, valid email, max 255 chars |
| password | string | Required, 8–72 chars |

**Response `201 Created`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failed |
| 409 | Email already registered |

**Side effect:** Publishes `USER_CREATED` event to NATS (via outbox). Notification Service sends a welcome notification asynchronously.

---

### Login

**`POST /api/v1/auth/login`**

No authentication required.

**Request body:**

```json
{
  "email": "jane@example.com",
  "password": "password123"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failed |
| 401 | Invalid email or password |

---

## Users

### Get current user profile

**`GET /api/v1/users/me`**

Authentication required.

**Response `200 OK`:**

```json
{
  "id": 1,
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "createdAt": "2026-08-10T10:15:30Z"
}
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid JWT |
| 404 | User not found |

---

### Update current user profile

**`PUT /api/v1/users/me`**

Authentication required.

**Request body:**

```json
{
  "fullName": "Jane Smith"
}
```

**Response `200 OK`:**

```json
{
  "id": 1,
  "fullName": "Jane Smith",
  "email": "jane@example.com",
  "createdAt": "2026-08-10T10:15:30Z"
}
```

**Side effect:** Publishes `USER_UPDATED` event to NATS.

---

## Notifications

### List my notifications

Returns notifications generated from user lifecycle events (newest first).

**`GET /api/v1/notifications`**

Authentication required.

**Response `200 OK`:**

```json
[
  {
    "id": 1,
    "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "eventType": "USER_CREATED",
    "message": "Welcome, Jane Doe! Your account has been created.",
    "status": "SENT",
    "createdAt": "2026-08-10T10:15:31Z"
  }
]
```

**Errors:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid JWT |

---

## Health checks

| Service | Endpoint |
|---------|----------|
| API Gateway | `GET /actuator/health` |
| User Service | `GET /actuator/health` (internal) |
| Notification Service | `GET /actuator/health` (internal) |

---

## NATS event schema (internal — not HTTP)

User Service publishes to subjects:

- `events.user.user_created`
- `events.user.user_updated`

**Payload (`UserEvent`):**

```json
{
  "eventId": "uuid",
  "eventType": "USER_CREATED",
  "userId": 1,
  "email": "jane@example.com",
  "fullName": "Jane Doe",
  "occurredAt": "2026-08-10T10:15:30Z"
}
```

Notification Service consumes from JetStream stream `USER_EVENTS` with durable consumer `notification-service-consumer`.

---

## Example workflow (cURL)

```bash
# 1. Register
REGISTER=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","password":"password123"}')

TOKEN=$(echo $REGISTER | jq -r .accessToken)

# 2. Profile
curl -s http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN" | jq

# 3. Update profile
curl -s -X PUT http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Smith"}' | jq

# 4. Notifications (after ~3 seconds)
sleep 3
curl -s http://localhost:8080/api/v1/notifications -H "Authorization: Bearer $TOKEN" | jq
```

---

## Gateway routing

| Path prefix | Upstream service |
|-------------|------------------|
| `/api/v1/auth/**` | User Service |
| `/api/v1/users/**` | User Service |
| `/api/v1/notifications/**` | Notification Service |

Public (no JWT): `/api/v1/auth/register`, `/api/v1/auth/login`, `/actuator/health`, `/actuator/info`
