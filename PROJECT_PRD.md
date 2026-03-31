# PROJECT_PRD.md — Secure Blog REST API

**Project Name:** Secure Blog REST API  
**Target Repository:** https://github.com/Muneebqureshi90/Testing-Jarvis  
**Owner:** Muneeb Qureshi  
**Created:** 2026-03-31  
**Agent:** Product-Manager  

---

## Executive Summary

Build a production-ready, secure blogging REST API using Spring Boot with JWT authentication, MySQL persistence, and full CRUD operations for blog posts. The API must enforce authentication for all write operations and maintain clear author-post relationships.

---

## Business Objectives

- Provide a secure backend for blog content management
- Enable user registration/login with JWT-based stateless authentication
- Support full lifecycle management of blog posts (create, read, update, delete)
- Ensure data integrity and safe SQL practices via JPA
- Prepare for future frontend integration (React/Vue/Angular)

---

## User Stories

### Authentication
- As a new user, I want to register an account so I can create and manage my blog posts
- As a registered user, I want to log in and receive a JWT token to access protected endpoints
- As a logged-in user, I want my token to be validated on subsequent requests

### Blog Posts
- As an authenticated author, I want to create a new blog post with title and content
- As an authenticated author, I want to view all my posts
- As an authenticated author, I want to update my own posts
- As an authenticated author, I want to delete my posts
- As a reader (authenticated or not), I want to view all published posts (read-only public endpoint)

---

## Epics & Requirements

### Epic 1: Security & Auth
**Goal:** Implement Spring Security with JWT authentication for user login and registration.

**User Entity:**
- `id` (Long, primary key)
- `username` (String, unique, not null)
- `email` (String, unique, not null)
- `password` (String, encoded, not null)
- `createdAt` (Timestamp)
- `enabled` (Boolean, default true)

**Endpoints:**
- `POST /api/auth/register` — Register new user (username, email, password)
- `POST /api/auth/login` — Authenticate and return JWT token
- `POST /api/auth/refresh` — Refresh token endpoint (optional but recommended)

**JWT Configuration:**
- Use `jjwt` library
- Sign with HMAC-SHA256 (strong secret key stored in application.properties)
- Token contains: `userId`, `username`, `exp`, `iat`
- Expiration: 24 hours (configurable)
- Validate token on each request to protected endpoints

**Security Rules:**
- `/api/auth/**` — Public access
- `/api/posts` — `GET` public, `POST/PUT/DELETE` require authentication
- All other endpoints — protected
- Passwords must be encoded with `BCryptPasswordEncoder`

---

### Epic 2: Blog Engine
**Goal:** Full CRUD for Blog Posts with proper author relationship.

**Post Entity:**
- `id` (Long, primary key)
- `title` (String, not null, max 255)
- `content` (Text / long string, not null)
- `author` (ManyToOne relationship to User)
- `createdAt` (Timestamp)
- `updatedAt` (Timestamp)
- `published` (Boolean, default false) — optional draft/publish flag

**Endpoints:**
- `GET /api/posts` — List all posts (public, paginated optional)
- `GET /api/posts/{id}` — Get single post by ID (public)
- `POST /api/posts` — Create new post (authenticated, user becomes author)
- `PUT /api/posts/{id}` — Update post (authenticated, only author can edit)
- `DELETE /api/posts/{id}` — Delete post (authenticated, only author can delete)

**Authorization Rules:**
- Users can only modify their own posts
- Return 403 Forbidden if user attempts to edit/delete another user's post

---

### Epic 3: Build Integrity
**Goal:** Complete, working `pom.xml` with all required dependencies and build success.

**Mandatory Dependencies:**
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `mysql-connector-java` (or `mysql-connector-j`)
- `lombok`
- `jjwt` (Java JWT library)
- `spring-boot-starter-validation` (recommended for request validation)

**Project Configuration:**
- Java 17+ (or 11 minimum)
- Maven build
- `application.properties` with MySQL connection properties (use placeholders)
- JWT secret key placeholder (to be configured in deployment)

**Build Success:**
- `mvn clean compile` must succeed
- All unit tests (if any) must pass
- No compilation errors

---

## Technical Stack

- **Backend Framework:** Spring Boot 3.x (Java 17+)
- **Database:** MySQL 8.x
- **ORM:** Spring Data JPA (Hibernate)
- **Authentication:** Spring Security + JWT (jjwt)
- **Password Encoding:** BCrypt
- **Build Tool:** Maven
- **API Style:** RESTful JSON

---

## Acceptance Criteria

1. **Registration:** User can register via `POST /api/auth/register` with JSON body `{username, email, password}`. Returns 201 Created with user data (no password in response).
2. **Login:** User can login via `POST /api/auth/login` with `{username, password}`. Returns 200 OK with `{token, tokenType, expiresIn}`.
3. **JWT Validation:** All protected endpoints require `Authorization: Bearer <token>` header. Invalid/missing token returns 401.
4. **Create Post:** Authenticated user can create post via `POST /api/posts` with `{title, content}`. Post is linked to authenticated user as author. Returns 201 with post data.
5. **Read Posts:** `GET /api/posts` returns array of all posts (public, no auth required).
6. **Update Post:** Authenticated user can update their own post via `PUT /api/posts/{id}`. Returns 200 with updated post. 403 if not owner.
7. **Delete Post:** Authenticated user can delete their own post via `DELETE /api/posts/{id}`. Returns 204. 403 if not owner.
8. **Build:** `mvn clean package` produces a runnable JAR and passes all tests.

---

## Out of Scope

- Frontend UI (API only)
- Email verification on registration
- Password reset functionality
- Advanced blog features (categories, tags, comments, likes)
- Rate limiting (may be added later by DevOps)
- API documentation (Swagger/OpenAPI) — nice to have but not blocking

---

## Repository Instructions

- Target repository: https://github.com/Muneebqureshi90/Testing-Jarvis
- Create a new branch: `feature/secure-blog-api`
- Commit all code to that branch
- Do not merge to main until QA approval
- Include README with build/run instructions

---

## Quality Gates

1. **Code Quality:** Clean Java code following Spring conventions; proper exception handling; no debug logs in production
2. **Security:** No plain-text passwords; JWT secrets not hardcoded; SQL injection prevented by JPA
3. **Testing:** At least 2 unit tests for Authentication and 2 for Post Service layer
4. **Documentation:** README with setup instructions and sample API calls

---

## Handoff Notes for System-Architect

**Next Stage:** System Design  
**Task:** Design the DB Schema (Users, Posts) and REST API contracts for the Secure Blog.  
**Deliverables:**
- ER diagram ( Users, Posts) showing relationships
- API contract document (endpoints, methods, request/response schemas, status codes)
- Database schema DDL (MySQL CREATE TABLE statements)
- Any additional design notes (indexes, constraints, performance considerations)

Refer to this PRD for scope boundaries. Keep the design minimal but production-ready.

---

**END OF PRD**
