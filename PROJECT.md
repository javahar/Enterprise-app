# Enterprise App — Project Documentation

> **Living document.** Update this as phases complete and decisions change.  
> Keep this open in your IDE alongside the code (IntelliJ / VS Code both render markdown preview).

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [System Design](#system-design)
3. [Database Schema](#database-schema)
4. [Architectural Decisions](#architectural-decisions)
5. [Phase 1 — Foundation](#phase-1--foundation)
6. [Phase 2 — Hierarchy](#phase-2--hierarchy)
7. [Phase 3 — User × Hierarchy](#phase-3--user--hierarchy)
8. [Running the Project](#running-the-project)
9. [Key Conventions](#key-conventions)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + Vite + Material UI (MUI) v5 |
| State / Data fetching | TanStack Query (React Query) + Axios |
| Routing | React Router v6 |
| Backend | Spring Boot 3.3 + Java 21 |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Migrations | Flyway |
| Build | Maven 3.9 |

---

## System Design

### Core Entities

```
Organization
  ├── Location (many, scoped to org)
  └── AppUser  (many, scoped to org)

UserLocation        — direct user ↔ location assignment + role   [Phase 1]

HierarchyNode       — self-referencing tree (Region / State / County)  [Phase 2]
NodeLocation        — which locations belong to which node              [Phase 2]

UserNodeAssignment  — user assigned to a node with a role               [Phase 3]
                      role inherits to ALL locations under that node
```

### Entity Relationship Overview

```
Organization ──< Location
Organization ──< AppUser

AppUser >──< Location          (via UserLocation,       role: READ/WRITE/ADMIN)
AppUser >──< HierarchyNode     (via UserNodeAssignment, role: READ/WRITE/ADMIN)

HierarchyNode ──< HierarchyNode   (self-ref: parent / children)
HierarchyNode >──< Location       (via NodeLocation)
```

### Hierarchy Levels

| Level | Name | Examples |
|---|---|---|
| 1 | Region | EAST, WEST, CENTRAL, MOUNTAIN |
| 2 | State | New York, California, Texas |
| 3 | County / City / District | Kings County, Los Angeles, Austin |

### Access Model (important — read this)

- A user is assigned a **single role** at a **hierarchy node** (e.g. James = ADMIN on EAST Region).
- That role **automatically applies to every location under that node** — no additional assignment needed.
- There is **no conflict resolution** — the model is purely additive/inherited.
- The "effective locations" query uses a **PostgreSQL recursive CTE** walking down `HierarchyNode → NodeLocation`.
- This same query is reusable for **finance reporting** by region/state/county (out of scope now but designed for it).

### Hierarchy Constraint Rule

> A location **cannot** be added to Level 3 unless it is already in Level 2.  
> A location **cannot** be added to Level 2 unless it is already in Level 1.  
> Enforced in the **service layer** (not DB triggers) so violations surface as clean API errors.

---

## Database Schema

### Flyway Migrations

| File | Phase | Description |
|---|---|---|
| `V1__baseline_schema.sql` | 1 | organization, location, app_user, access_role, user_location |
| `V2__hierarchy_schema.sql` | 2 | hierarchy_node (self-ref), node_location |
| `V3__user_node_assignment_schema.sql` | 3 | user_node_assignment |
| `V4__fix_role_columns.sql` | fix | Converts role_id from SMALLINT FK → VARCHAR(50) to match Java enum |

### Table Summary

```sql
organization          (id, name, description, is_active, created_at, updated_at)
location              (id, name, address, city, state_code, zip, organization_id, is_active, ...)
app_user              (id, first_name, last_name, email, organization_id, is_active, ...)
access_role           (id SMALLINT, name VARCHAR)           -- lookup: READ/WRITE/ADMIN
user_location         (user_id, location_id, role_id VARCHAR, assigned_at)
hierarchy_node        (id, name, level SMALLINT, parent_id, organization_id, ...)
node_location         (node_id, location_id, assigned_at)
user_node_assignment  (user_id, node_id, role_id VARCHAR, assigned_at)
```

### Effective Locations Query (Phase 3 preview)

```sql
-- All locations a user has access to via hierarchy assignment
WITH RECURSIVE subtree AS (
  -- Start: nodes directly assigned to the user
  SELECT hn.id
  FROM hierarchy_node hn
  JOIN user_node_assignment una ON una.node_id = hn.id
  WHERE una.user_id = :userId

  UNION ALL

  -- Walk down: children of nodes already in the set
  SELECT hn.id
  FROM hierarchy_node hn
  JOIN subtree s ON hn.parent_id = s.id
)
SELECT DISTINCT l.*
FROM location l
JOIN node_location nl ON nl.location_id = l.id
WHERE nl.node_id IN (SELECT id FROM subtree);
```

---

## Architectural Decisions

### Why `HierarchyNode` as a single self-referencing table?

**Chosen:** One table with `parent_id` and `level` (1/2/3).  
**Rejected:** Three separate tables (Region, State, County).

Reasons:
- Adding a 4th level in future requires no schema change.
- Recursive CTE works on a single table — simpler query.
- The finance reporting reuse case benefits from a uniform node reference.

### Why `AppUser` instead of `User`?

`user` is a reserved word in PostgreSQL. Using it as a table name requires quoting everywhere. `app_user` avoids the friction entirely.

### Why `AccessRole` as a Java enum, not a JPA entity?

The lookup has exactly 3 stable values (READ, WRITE, ADMIN). A full entity adds a repository, service, and DTO for zero benefit. The enum is stored as `VARCHAR` via `@Enumerated(EnumType.STRING)` so DB values are human-readable.

### Why `BaseEntity` with `@MappedSuperclass`?

All entities share `id` (UUID), `createdAt`, and `updatedAt`. The `@EntityListeners(AuditingEntityListener.class)` on the base class + `@EnableJpaAuditing` in `JpaConfig` sets timestamps automatically — never set manually.

### Why service-layer constraint enforcement (not DB triggers)?

The "must be in parent level before child level" rule lives in the service layer so violations surface as clean, structured API errors (HTTP 400 with a message). DB triggers are harder to test, harder to surface meaningful errors from, and harder to change.

### Why Flyway instead of `ddl-auto: create`?

`ddl-auto: validate` means Hibernate only validates the schema — Flyway owns creation and migration. This is the production-safe approach: schema changes are versioned, reviewable, and repeatable across environments.

---

## Phase 1 — Foundation

**Goal:** Working CRUD for Organizations, Locations, Users. Direct user-location assignment. Frontend and backend wired up.

### Tasks

| ID | Task | Status |
|---|---|---|
| P1-1 | Project scaffolding — Spring Boot, Flyway, React + Vite, MUI, repo structure | ✅ Done |
| P1-2 | Organization entity + REST API (CRUD) + React UI | ⬜ Next |
| P1-3 | Location entity + REST API (CRUD, org-scoped) + React UI | ⬜ |
| P1-4 | User entity + REST API (CRUD, org-scoped) + React UI | ⬜ |
| P1-5 | UserLocation — assign users to locations with a role, UI for assignment | ⬜ |
| P1-6 | Frontend ↔ backend wiring — React Query hooks, Axios client, CORS verified | ⬜ |
| P1-7 | Push to GitHub | ⬜ |

### API Endpoints (Phase 1 target)

```
GET    /api/organizations
POST   /api/organizations
GET    /api/organizations/:id
PUT    /api/organizations/:id
DELETE /api/organizations/:id

GET    /api/organizations/:orgId/locations
POST   /api/organizations/:orgId/locations
GET    /api/locations/:id
PUT    /api/locations/:id
DELETE /api/locations/:id

GET    /api/organizations/:orgId/users
POST   /api/organizations/:orgId/users
GET    /api/users/:id
PUT    /api/users/:id
DELETE /api/users/:id

GET    /api/users/:userId/locations
POST   /api/users/:userId/locations          body: { locationId, role }
DELETE /api/users/:userId/locations/:locationId
```

---

## Phase 2 — Hierarchy

**Goal:** Region/State/County tree. Assign locations to hierarchy nodes with constraint enforcement. Search locations by node.

### Tasks

| ID | Task | Status |
|---|---|---|
| P2-1 | HierarchyNode entity + REST API (tree CRUD, org-scoped) | ⬜ |
| P2-2 | NodeLocation join + service-layer constraint enforcement | ⬜ |
| P2-3 | Hierarchy portal UI — tree view + location search + assign flow | ⬜ |
| P2-4 | Location search by node — recursive CTE + API endpoint + UI | ⬜ |

### API Endpoints (Phase 2 target)

```
GET    /api/organizations/:orgId/hierarchy          -- full tree
POST   /api/organizations/:orgId/hierarchy          -- create node (body includes level + parentId)
PUT    /api/hierarchy/:nodeId
DELETE /api/hierarchy/:nodeId

GET    /api/hierarchy/:nodeId/locations             -- locations directly assigned to this node
POST   /api/hierarchy/:nodeId/locations             -- assign location (enforces level rule)
DELETE /api/hierarchy/:nodeId/locations/:locationId

GET    /api/hierarchy/:nodeId/locations/effective   -- recursive: all locations under this node
```

### Constraint Logic (service layer)

```
Assigning location L to node N (level X):
  if X == 2: verify L is already assigned to N's parent (a level-1 node)
  if X == 3: verify L is already assigned to N's parent (a level-2 node)
  if X == 1: no restriction
```

---

## Phase 3 — User × Hierarchy

**Goal:** Assign users to hierarchy nodes. Role inherits to all locations under that node. UI shows effective locations per user.

### Tasks

| ID | Task | Status |
|---|---|---|
| P3-1 | UserNodeAssignment entity + REST API | ⬜ |
| P3-2 | Effective locations query — recursive CTE, service + endpoint | ⬜ |
| P3-3 | Assign user to hierarchy node UI | ⬜ |
| P3-4 | User's location view — inherited locations with role shown | ⬜ |

### API Endpoints (Phase 3 target)

```
GET    /api/users/:userId/nodes                          -- nodes assigned to user
POST   /api/users/:userId/nodes                          -- assign user to node + role
DELETE /api/users/:userId/nodes/:nodeId

GET    /api/users/:userId/locations/effective            -- all locations via hierarchy (recursive CTE)
```

---

## Running the Project

### Prerequisites

- Java 21
- Maven 3.9+
- Node 20+
- PostgreSQL running (Docker or local)

### Database

```sql
-- Run once in DBeaver or psql
CREATE DATABASE enterprise_db;
```

### Backend

```bash
cd backend
mvn spring-boot:run

# Custom DB credentials:
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run

# Custom port (default is configured in application.yml):
SERVER_PORT=9090 mvn spring-boot:run
```

API base URL: `http://localhost:<port>/api`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173`  
Configured via `frontend/.env` → `VITE_API_BASE_URL`

---

## Key Conventions

### Backend

| Convention | Detail |
|---|---|
| Package structure | `entity`, `repository`, `service`, `controller`, `dto`, `config`, `exception` |
| Entity base class | Extend `BaseEntity` for `id`, `createdAt`, `updatedAt` |
| DTOs | Separate request/response DTOs — never expose entities directly from controllers |
| Error handling | `GlobalExceptionHandler` — returns `ErrorResponse` record with status + message |
| Validation | `@Valid` on controller params + `@NotBlank` / `@NotNull` on DTOs |
| Naming | Tables: snake_case. Java: camelCase. Booleans: `isActive` (Java) / `is_active` (DB) |

### Frontend

| Convention | Detail |
|---|---|
| API calls | All in `src/api/services.js` — never call `apiClient` directly from components |
| Server state | TanStack Query (`useQuery` / `useMutation`) — no raw `useEffect` for API calls |
| Routing | React Router v6 — routes defined in `App.jsx` |
| Components | Feature folders: `src/components/organizations/`, `locations/`, `users/` |
| Pages | Thin — compose components, pass query data down |

### Git

- Branch per phase: `phase-1`, `phase-2`, `phase-3`
- Commit after each completed task (P1-2, P1-3, etc.)
- Never commit `.env.local` or `application-local.yml`
