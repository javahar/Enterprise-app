# Enterprise App

Multi-phase enterprise application with Organizations, Locations, Users, and a geographic hierarchy.

## Tech Stack

| Layer | Tech |
|---|---|
| Frontend | React 18 + Vite + MUI v5 + TanStack Query + React Router v6 |
| Backend | Spring Boot 3.3 + Spring Data JPA (Hibernate) + Java 21 |
| Database | PostgreSQL |
| Migrations | Flyway |

## Project Structure

```
enterprise-app/
├── backend/          Spring Boot API
│   └── src/main/
│       ├── java/com/enterprise/app/
│       │   ├── entity/       JPA entities
│       │   ├── repository/   Spring Data repositories
│       │   ├── service/      Business logic
│       │   ├── controller/   REST controllers
│       │   ├── dto/          Request/response DTOs
│       │   ├── config/       JPA, CORS config
│       │   └── exception/    Global error handler
│       └── resources/
│           ├── application.yml
│           └── db/migration/ Flyway SQL migrations
└── frontend/         React app
    └── src/
        ├── api/      Axios client + service modules
        ├── components/
        ├── pages/
        └── theme/
```

## Prerequisites

- Java 21
- Maven 3.9+
- Node 20+
- PostgreSQL 15+ running locally

## Database Setup

```sql
CREATE DATABASE enterprise_db;
```

Flyway will apply all migrations automatically on first startup.

## Running the Backend

```bash
cd backend

# With default postgres/postgres credentials:
./mvnw spring-boot:run

# Or with custom credentials:
DB_USERNAME=myuser DB_PASSWORD=mypass ./mvnw spring-boot:run
```

The API starts on http://localhost:8080.

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The UI starts on http://localhost:5173.

## Database Schema (Flyway migrations)

| Migration | Description |
|---|---|
| V1 | Core tables: organization, location, app_user, access_role, user_location |
| V2 | Hierarchy: hierarchy_node (self-referencing), node_location |
| V3 | User-node assignment: user_node_assignment |

## Phase Plan

- **Phase 1** — Organization/Location/User CRUD + direct user-location assignment
- **Phase 2** — HierarchyNode tree, NodeLocation assignment with constraint enforcement, hierarchy portal UI
- **Phase 3** — UserNodeAssignment, effective locations via recursive CTE, inherited access views
