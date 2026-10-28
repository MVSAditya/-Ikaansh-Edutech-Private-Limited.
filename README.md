# Fieldwork | Employee Task Management

A full-stack employee and project task management application built with Java 17, Spring Boot, MySQL, JWT, and React. The API provides role-based access, validation, paginated/filterable task queries, project lifecycle management, OpenAPI documentation, Flyway migrations, and persistence audit logs.

## Run the full stack

Requirements: Docker with the Compose plugin.

```bash
cp .env.example .env
# Change the local passwords and JWT secret in .env before exposing the app.
docker compose up --build
```

Open the application at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`. Compose starts MySQL, waits for its health check, builds the API and frontend images, and applies the schema migration automatically. The development bootstrap ADMIN credentials come from `.env` (`BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD`). Change the sample values before use outside a local demo. The database is persisted in the `mysql_data` volume.

Compose also publishes MySQL at `127.0.0.1:3306` for local development. If your Docker environment blocks container-to-container networking, stop the Compose backend and frontend with `docker compose stop backend frontend`, then run the backend and Vite dev server using the local development steps below. The host-run backend can connect to the published MySQL port.

Stop the services with `docker compose down`. This keeps the database volume. `docker compose down -v` deletes the database data.

## Local development

Requirements: Java 17+, Maven 3.9+, Node.js 20+, and MySQL 8.0+.

Configure the API database and token secret, then run the backend:

```bash
export DB_URL='jdbc:mysql://localhost:3306/employee_tasks?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='root'
export DB_PASSWORD='your-mysql-password'
export JWT_SECRET='replace-with-a-random-secret-at-least-32-characters-long'
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='choose-a-strong-password'
mvn spring-boot:run
```

In a second terminal, start the React dev server. Vite proxies `/api` requests to the backend on port 8080.

```bash
cd frontend
npm ci
npm run dev
```

Open the Vite URL printed in the terminal. Flyway applies `src/main/resources/db/migration/V1__create_employee_task_tables.sql`; Hibernate validates the schema at startup. Never use development credentials or the sample JWT secret in a deployed environment.

## First use

1. Sign in with the bootstrap ADMIN credentials configured in `.env`.
2. Register employee accounts through the UI with a name, email, department, and password. Registration creates the `EMPLOYEE` login and matching employee profile in one transaction.
3. Create a project in the Projects view and set its lifecycle to Planning, Active, On hold, or Completed.
4. Create and assign tasks from the Tasks view, optionally linking each task to a project. Employees see only their own assigned tasks; managers and admins see the team view.

Example: create an additional employee profile as an ADMIN (replace the token and values):

```bash
curl -X POST http://localhost:8080/api/employees \
  -H 'Authorization: Bearer <admin-token>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Asha Rao","email":"asha@example.com","department":"Engineering"}'
```

Sign in to the UI with `asha@example.com` and the password used during registration. Then assign tasks to that employee from the manager account.

## Authentication and roles

`POST /api/auth/register` creates an `EMPLOYEE` account and matching Employee profile in one transaction, then returns a JWT. Provide `name`, `email`, `department`, and `password`. `POST /api/auth/login` issues a JWT for an existing account. Send protected requests with `Authorization: Bearer <accessToken>`.

| Role | Permissions |
| --- | --- |
| `ADMIN` | Employee CRUD; task CRUD; view all tasks |
| `MANAGER` | Employee read/create/update; task CRUD; view all tasks |
| `EMPLOYEE` | Read tasks assigned to an employee record with the same email |

Public registration cannot assign privileged roles. The optional bootstrap variables create the first ADMIN if the email does not exist. Create additional manager accounts only through a trusted administrative process. The bootstrap process does not overwrite an existing account's password or role.

## API reference

| Method | Endpoint | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public, creates EMPLOYEE |
| POST | `/api/auth/login` | Public |
| GET, POST | `/api/employees` | ADMIN, MANAGER |
| GET, PUT | `/api/employees/{id}` | ADMIN, MANAGER |
| DELETE | `/api/employees/{id}` | ADMIN |
| GET, POST | `/api/projects` | ADMIN, MANAGER |
| GET, PUT, DELETE | `/api/projects/{id}` | ADMIN, MANAGER |
| GET | `/api/tasks` | Authenticated; employees see assigned tasks |
| GET | `/api/tasks/{id}` | Same visibility as task list |
| POST, PUT, DELETE | `/api/tasks`, `/api/tasks/{id}` | ADMIN, MANAGER |

Task listing is paginated and supports sorting and combined filters, for example:

```text
GET /api/tasks?page=0&size=10&sort=dueDate,asc&status=IN_PROGRESS&priority=HIGH&projectId=1
```

Project statuses: `PLANNING`, `ACTIVE`, `ON_HOLD`, `COMPLETED`. Task statuses: `TODO`, `IN_PROGRESS`, `DONE`. Priorities: `LOW`, `MEDIUM`, `HIGH`.

Example task payload:

```json
{
  "title": "Prepare release",
  "description": "Complete the release checklist",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "dueDate": "2026-11-05",
  "projectId": 1,
  "assignedEmployeeId": 1
}
```

`projectId` is optional. Deleting a project keeps its tasks and clears their project association.

Validation, authentication, authorization, conflicts, and missing resources return structured JSON errors. Swagger UI and OpenAPI JSON are available at `/swagger-ui.html` and `/v3/api-docs`.

## Audit events

The API writes structured create, update, and delete events for Employee, Project, and Task persistence to the backend application log. Events include the action, entity type, and entity ID; they intentionally omit personal field values. With Compose, view them using `docker compose logs -f backend`.

## Tests and builds

Run the backend service tests and production frontend build:

```bash
mvn test
cd frontend && npm ci && npm run build
```

Service tests use Mockito and do not require a live MySQL server. `AuthServiceTest` verifies employee-only registration and successful password-based token login. The Compose build packages the backend and frontend into separate images.

## Design choices

- MySQL is used as required by the core assignment; Flyway owns the schema and Hibernate runs in `validate` mode.
- Self-registration is employee-only to prevent users from granting themselves manager/admin privileges.
- Employee task visibility is constrained server-side by account email, including direct task lookups.
- Request/response DTOs avoid exposing password hashes and persistence entities.
- The React interface uses a same-origin API proxy in development and production, avoiding broad CORS settings.