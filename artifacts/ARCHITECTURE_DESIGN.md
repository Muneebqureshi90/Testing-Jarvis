# Secure Blog REST API — Architecture Design

**Project:** Secure Blog REST API  
**Version:** 1.0  
**Architect:** System-Architect Agent  
**Date:** 2026-03-31

---

## 1. Architectural Overview

### High-Level Design
The Secure Blog REST API is built as a **single Spring Boot monolith** suitable for initial deployment and scaling. The architecture follows **RESTful principles** with **stateless JWT authentication**.

```
┌─────────────────────────────────────────────────────────┐
│                    Load Balancer (optional)             │
│              (nginx / AWS ALB / Cloudflare)            │
└───────────────────────────┬─────────────────────────────┘
                            │ HTTPS (TLS 1.3)
                            ▼
┌─────────────────────────────────────────────────────────┐
│               Spring Boot Application                  │
│   ┌─────────────────────────────────────────────────┐ │
│   │  Controllers (REST Endpoints)                   │ │
│   │  ┌────────────┐  ┌────────────┐  ┌──────────┐ │ │
│   │  │ AuthCtrl   │  │ PostCtrl   │  │ ...      │ │ │
│   │  └────────────┘  └────────────┘  └──────────┘ │ │
│   └─────────────────────────────────────────────────┘ │
│   ┌─────────────────────────────────────────────────┐ │
│   │  Service Layer (Business Logic)                 │ │
│   │  ┌────────────┐  ┌────────────┐  ┌──────────┐ │ │
│   │  │ AuthSvc    │  │ PostSvc    │  │ ...      │ │ │
│   │  └────────────┘  └────────────┘  └──────────┘ │ │
│   └─────────────────────────────────────────────────┘ │
│   ┌─────────────────────────────────────────────────┐ │
│   │  Repository Layer (Spring Data JPA)             │ │
│   │  ┌────────────┐  ┌────────────┐                │ │
│   │  │ UserRepo   │  │ PostRepo   │                │ │
│   │  └────────────┘  └────────────┘                │ │
│   └─────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────┘
                            │ JDBC over TLS
                            ▼
┌─────────────────────────────────────────────────────────┐
│           MySQL 8.x (RDS or self-hosted)               │
│   ┌──────────────────┐      ┌──────────────────┐      │
│   │     users        │      │      posts       │      │
│   └──────────────────┘      └──────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Technology Stack
- **Backend Framework:** Spring Boot 3.x (Java 17+)
- **Database:** MySQL 8.x
- **ORM:** Spring Data JPA (Hibernate)
- **Authentication:** Spring Security + JWT (jjwt library)
- **Password Encoding:** BCrypt (strength 12)
- **Build Tool:** Maven 3.8+
- **API Protocol:** HTTPS with TLS 1.3 (terminated at load balancer or app)
- **Port:** 8080 (configurable)

### Design Principles
- **Stateless:** No server-side session storage. JWT tokens carry authentication state.
- **Single Responsibility:** Controllers handle HTTP, Services handle business logic, Repositories handle persistence.
- **Defense in Depth:** Multiple security layers (HTTPS, JWT validation, authorization checks, input validation).
- **Principle of Least Privilege:** Database user has only SELECT, INSERT, UPDATE, DELETE on its schema.
- **Fail Fast:** Validations at API layer before hitting database.

---

## 2. Microservice Decomposition

**Note:** This project is implemented as a **single service** due to simplicity and bounded context size. Future expansion may split into separate services (e.g., `auth-service`, `blog-service`, `user-service`).

### Service: Secure Blog API (Monolith)

| Aspect | Detail |
|--------|--------|
| **Purpose** | Provides authentication, user management, and blog post CRUD operations |
| **Database** | Dedicated MySQL database `secure_blog` |
| **Dependencies** | None (standalone) |
| **API Base Path** | `/api/v1` |
| **Port** | 8080 |

The monolith structure:
```
com.secureblog
├── controller
│   ├── AuthController.java
│   └── PostController.java
├── service
│   ├── AuthService.java
│   └── PostService.java
├── repository
│   ├── UserRepository.java
│   └── PostRepository.java
├── model
│   ├── User.java
│   ├── Post.java
│   └── (DTOs: RegisterUserRequest, LoginRequest, CreatePostRequest, etc.)
├── security
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── SecurityConfig.java
└── exception
    ├── ResourceNotFoundException.java
    ├── AccessDeniedException.java
    └── GlobalExceptionHandler.java
```

---

## 3. Database Design (MySQL)

### ER Diagram

```mermaid
erDiagram
    USERS {
        BIGINT UNSIGNED id PK
        CHAR(36) uuid UK
        VARCHAR(50) username UK
        VARCHAR(255) email UK
        VARCHAR(255) password_hash
        VARCHAR(100) first_name
        VARCHAR(100) last_name
        VARCHAR(20) phone NULL
        BOOLEAN is_active
        BOOLEAN is_verified
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    POSTS {
        BIGINT UNSIGNED id PK
        VARCHAR(255) title
        LONGTEXT content
        BIGINT UNSIGNED author_id FK
        BOOLEAN published
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    USERS ||--o{ POSTS : "writes"
    POSTS }o--|| USERS : "written_by"
```

### Relationship Notes
- **One-to-Many:** One User (author) can write many Posts.
- **Foreign Key:** `posts.author_id` references `users.id` with `ON DELETE CASCADE` (deleting a user removes their posts).
- **Indexes:**
  - `idx_email` on `users.email` for login lookups
  - `idx_username` on `users.username` for login lookups
  - `idx_author` on `posts.author_id` for queries filtering by author
  - `idx_published` on `posts.published` for filtering public posts
  - `idx_created_at` on `posts.created_at` for chronological queries

### Table Definitions

#### Table: `users`

| Column | Type | Nullable | Default | Constraints | Description |
|--------|------|----------|---------|-------------|-------------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | PRIMARY KEY | Surrogate key, sequential |
| `uuid` | CHAR(36) | NO | UUID() | UNIQUE NOT NULL | Globally unique identifier |
| `username` | VARCHAR(50) | NO | - | UNIQUE NOT NULL | Login username (3-50 chars) |
| `email` | VARCHAR(255) | NO | - | UNIQUE NOT NULL | User email (validated format) |
| `password_hash` | VARCHAR(255) | NO | - | NOT NULL | BCrypt-encoded password |
| `first_name` | VARCHAR(100) | NO | - | NOT NULL | User's legal first name |
| `last_name` | VARCHAR(100) | NO | - | NOT NULL | User's legal last name |
| `phone` | VARCHAR(20) | YES | NULL | - | Optional phone number |
| `is_active` | BOOLEAN | NO | TRUE | DEFAULT TRUE | Soft-delete flag |
| `is_verified` | BOOLEAN | NO | FALSE | DEFAULT FALSE | Email verification status |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | - | Account creation time |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | - | Auto-updated on changes |

**Indexes on `users`:**
- PRIMARY KEY on `id`
- UNIQUE KEY `uq_users_uuid` on `uuid`
- UNIQUE KEY `uq_users_username` on `username`
- UNIQUE KEY `uq_users_email` on `email`
- INDEX `idx_email` on `email` (covers login queries)
- INDEX `idx_username` on `username` (covers login queries)

#### Table: `posts`

| Column | Type | Nullable | Default | Constraints | Description |
|--------|------|----------|---------|-------------|-------------|
| `id` | BIGINT UNSIGNED | NO | AUTO_INCREMENT | PRIMARY KEY | Surrogate key |
| `title` | VARCHAR(255) | NO | - | NOT NULL | Post title (max 255 chars) |
| `content` | LONGTEXT | NO | - | NOT NULL | Full post content (unlimited) |
| `author_id` | BIGINT UNSIGNED | NO | - | FOREIGN KEY (users.id) ON DELETE CASCADE | Owner of the post |
| `published` | BOOLEAN | NO | FALSE | DEFAULT FALSE | Draft vs published |
| `created_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP | - | Creation timestamp |
| `updated_at` | TIMESTAMP | NO | CURRENT_TIMESTAMP ON UPDATE | - | Last update timestamp |

**Indexes on `posts`:**
- PRIMARY KEY on `id`
- FOREIGN KEY `fk_posts_author_id` references `users(id)` ON DELETE CASCADE
- INDEX `idx_author` on `author_id` (fast lookups of user's posts)
- INDEX `idx_published` on `published` (filter published posts)
- INDEX `idx_created_at` on `created_at` (chronological ordering)
- (Optional) Composite INDEX `idx_author_published` on `(author_id, published)` for common queries

---

## 4. API Design Summary

### Base URL Pattern
```
https://<host>:<port>/api/v1/
```

### Endpoint Matrix

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | No | Register new user account |
| POST | `/auth/login` | No | Authenticate and get JWT tokens |
| POST | `/auth/refresh` | No | Refresh expired access token |
| GET | `/posts` | No | List all published posts (public) |
| GET | `/posts/{id}` | No | Get single post by ID (public) |
| POST | `/posts` | Yes | Create new post (authenticated) |
| PUT | `/posts/{id}` | Yes | Update own post (authenticated) |
| DELETE | `/posts/{id}` | Yes | Delete own post (authenticated) |

### Authorization Rules
- Public endpoints: `GET /posts`, `GET /posts/{id}`, all `/auth/**` routes
- Protected endpoints: all `POST/PUT/DELETE` on `/posts/**` require valid JWT
- Ownership enforcement: Users can only modify their own posts (checked in service layer)

### JWT Configuration
- **Algorithm:** HMAC-SHA256 (HS256)
- **Secret Key:** Must be strong (256+ bits), stored in `application.properties` as `app.jwt.secret`
- **Access Token Expiry:** 24 hours (86400 seconds)
- **Refresh Token Expiry:** 7 days (604800 seconds)
- **Token Claims:**
  - `sub` (subject): user ID
  - `username`: username
  - `iat` (issued at)
  - `exp` (expiration)
  - `roles` (optional array, currently only `USER`)

---

## 5. Security Architecture

### Layered Security Model

```
Layer 1: Network/Transport
  - TLS 1.3 encryption (HTTPS)
  - Secure cipher suites only (no TLS 1.2 fallback if possible)

Layer 2: Application Security (Spring Security)
  - JWT authentication filter extracts and validates token
  - BCrypt password encoder for stored credentials
  - CSRF protection disabled for stateless JWT API
  - CORS configuration allowing specific trusted origins

Layer 3: Authorization
  - Role-based access control: `ROLE_USER` for authenticated users
  - Method-level security: `@PreAuthorize("isAuthenticated()")` for write endpoints
  - Ownership checks: `post.getAuthor().getId().equals(getCurrentUserId())`

Layer 4: Data & Secrets
  - No hardcoded secrets (JWT secret, DB passwords in application.properties or environment variables)
  - Database connections use SSL/TLS in production
  - Passwords never returned in API responses
  - Sensitive fields (password_hash) restricted from serialization with `@JsonIgnore`

Layer 5: Operational
  - Rate limiting recommended (100 req/min per IP) — implement via gateway in future
  - Audit logging: All authentication attempts, post modifications logged with user ID and timestamp
  - Input validation: `@Valid` annotations on DTOs with `@NotNull`, `@Size`, `@Email`
```

### Password Policy
- Minimum 8 characters
- BCrypt with strength 12 (configurable)
- No plaintext storage; only `password_hash` persisted

### JWT Security Considerations
- Secret key length: minimum 256 bits (32 bytes) for HS256
- Refresh tokens are single-use and rotated on each use
- Access tokens are short-lived (24h) to limit exposure if stolen
- Revocation strategy: Blacklist/whitelist stored in Redis (optional for v1, can be added later)

---

## 6. Infrastructure & DevOps Considerations

### Deployment Strategies

**Option A: AWS (Recommended for Production)**
- **Compute:** EC2 (t3.micro or larger) or ECS Fargate
- **Database:** RDS MySQL (db.t3.micro, Multi-AZ for production)
- **Secrets:** AWS Secrets Manager (for DB passwords, JWT secret)
- **Load Balancing:** Application Load Balancer (HTTPS termination)
- **Containerization:** Docker (multi-stage build to minimize image size)
- **Orchestration:** ECS or EKS (if scaling to multiple instances)

**Option B: Docker Compose (Development/Testing)**
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: secure_blog
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password

  app:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/secure_blog
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      APP_JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      - mysql

volumes:
  mysql-data:
```

### Environment Configuration

**application-dev.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/secure_blog
spring.datasource.username=root
spring.datasource.password=devpassword
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
app.jwt.secret=dev-secret-key-change-in-production
```

**application-prod.properties**
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
app.jwt.secret=${JWT_SECRET}
server.ssl.enabled=true
server.ssl.key-store=${SSL_KEYSTORE}
server.ssl.key-store-password=${SSL_PASSWORD}
```

### Database Migrations

**Approach:** Use Flyway or Liquibase for versioned schema migrations. Not required for MVP but recommended for production.

**Initial migration (V1__init.sql):**
```sql
-- Include the db_schema.sql content here
-- Add additional migrations as V2, V3 for schema changes
```

### CI/CD Pipeline

**GitHub Actions Example:**
```yaml
name: Build and Test
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean compile
      - name: Run Unit Tests
        run: mvn test
      - name: Build Docker image
        run: docker build -t secure-blog:${{ github.sha }} .
```

### Monitoring & Observability

- **Logging:** Logback with JSON formatter for structured logs (ELK stack compatible)
- **Metrics:** Spring Boot Actuator with Prometheus endpoint (`/actuator/prometheus`)
- **Health Checks:** `/actuator/health` endpoint for load balancer probes
- **Tracing:** OpenTelemetry optional for distributed tracing (add later)

---

## 7. Error Handling & Resilience

### HTTP Status Code Usage

| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 OK | Success | GET /posts, PUT /posts/{id} (successful update) |
| 201 Created | Resource created | POST /auth/register, POST /posts |
| 204 No Content | Success with no body | DELETE /posts/{id} |
| 400 Bad Request | Invalid input | Validation errors, malformed JSON |
| 401 Unauthorized | Missing/invalid JWT | Protected endpoint without token |
| 403 Forbidden | Insufficient permissions | User editing another's post |
| 404 Not Found | Resource not found | GET/PUT/DELETE /posts/{id} with non-existent ID |
| 500 Internal Server Error | Unhandled exception | Database errors, unexpected failures |

### Idempotency

- **POST /posts:** Not idempotent (creates new resource each time)
- **PUT /posts/{id}:** Idempotent (same update repeated yields same result)
- **DELETE /posts/{id}:** Idempotent (deleting already-deleted resource returns 404, not error)

**Recommendation:** For production, add `Idempotency-Key` header to POST/PUT to prevent duplicate requests. Not required for initial version.

### Retry Strategy

- **Client-side:** Retry on 429 (rate limit) and 5xx errors with exponential backoff (2^retry * 100ms)
- **Database:** Connection pool (HikariCP) configured with retry logic
- **Transient failures:** Circuit breaker not needed for simple use case; add Resilience4j if external dependencies introduced

### Exception Handling

Global `@ControllerAdvice` with `@ExceptionHandler` methods:

- `MethodArgumentNotValidException` → 400 Bad Request with field errors
- `AccessDeniedException` → 403 Forbidden
- `UsernameNotFoundException`, `BadCredentialsException` → wrap in 401
- `EntityNotFoundException` → 404 Not Found
- `DataIntegrityViolationException` → 400 (duplicate username/email)
- `Exception` → 500 Internal Server Error (masked details in production)

---

## 8. API Versioning Strategy

**Version:** 1.0 (current)

All endpoints include `/api/v1/` prefix. Future breaking changes will introduce `/api/v2/` while maintaining v1 for backward compatibility.

**Deprecation Policy:**
- Deprecation notice posted 6 months before endpoint removal
- `317 Deprecated` status code and `Deprecated: true` in OpenAPI spec
- Documentation updated to mark deprecated endpoints

---

## 9. Scalability & Performance

### Expected Throughput
- Initial: < 10 QPS (queries per second)
- Target with scaling: 100+ QPS

### Performance Optimizations

**Database:**
- Read-heavy workload: Add read replica if traffic grows
- Connection pool: HikariCP (default in Spring Boot)
   - `maximumPoolSize`: 10-20 (depends on instance size)
   - `minimumIdle`: 5
- Indexes on `posts.published` and `posts.created_at` for list queries

**Caching:**
- **Phase 1 (now):** No caching; serve directly from DB
- **Phase 2 (if needed):** Redis cache for:
   - Frequently accessed posts (by ID)
   - Homepage post list (cache 60 sec with cache-busting on new post create)

**Pagination:**
- List endpoints should use cursor-based pagination for large datasets (>1000 posts)
- Current implementation: offset-based `page` and `size` query parameters (acceptable for small datasets)

### Horizontal Scaling Readiness
- Stateless app design allows adding more instances behind load balancer
- JWT tokens are self-contained; no sticky sessions needed
- Database connection string must be externalized (NOT hardcoded)

---

## 10. Compliance & Auditing

### GDPR Considerations
- **Data Export:** Implement (future) endpoint `GET /api/me/export` returning user's data in JSON
- **Data Deletion:** `DELETE /auth/account` to remove user and cascade delete posts (GDPR right to erasure)
- **Consent:** Checkbox on registration for terms/privacy policy (not implemented yet)

### PCI-DSS / HIPAA
- **Not applicable** for this project (no payment or health data)
- If handling sensitive personal data in future: add encryption-at-rest and strict access controls

### Audit Logging (Recommended)
Log entries:
- Authentication: success/failure, IP address, user agent
- Post operations: create, update, delete with user ID and post ID
- Format: JSON with fields: `timestamp`, `userId`, `action`, `resourceType`, `resourceId`, `ip`, `userAgent`

---

## 11. Migration & Seeding

### Initial Data
- No seed data required
- Optional: Create default admin user via SQL init script (if admin role added later)

### Database Migration from Zero
1. Run `db_schema.sql` to create tables and indexes
2. (Optional) Run `V1__init.sql` via Flyway for version control
3. Application starts; Spring Data JPA validates mappings

---

## 12. Data Consistency & Transaction Management

### Transaction Boundaries
- **Service layer methods** annotated with `@Transactional`
- **Read operations (GET):** `@Transactional(readOnly = true)`
- **Write operations (POST/PUT/DELETE):** `@Transactional` with default propagation (REQUIRED)

### Consistency Guarantees
- **Strong consistency:** Single database ensures ACID transactions
- **Cascading deletes:** ON DELETE CASCADE on `posts.author_id` ensures no orphaned posts
- **Optimistic locking:** Not implemented (no version column); assumes low conflict for posts

---

## 13. Quality Attributes Summary

| Attribute | Target | Implementation |
|-----------|--------|----------------|
| Availability | 99.5% | Single AZ deployment acceptable for MVP |
| Security | High | JWT, BCrypt, TLS, input validation, principle of least privilege |
| Performance | < 200ms P95 response time | Indexes on critical queries, connection pooling |
| Maintainability | High | Clean code structure (Controller-Service-Repository), documentation |
| Testability | High | Service layer unit tests with mocks; integration tests with @SpringBootTest |

---

## 14. Open Questions / Future Enhancements

1. **Email verification flow:** Not in MVP but needed for production
2. **Password reset:** Forgot password endpoint with email token
3. **Role-based permissions:** `ADMIN` role to manage any post
4. **Soft delete:** Currently `DELETE` is hard delete; consider `@SQLDelete` with `is_deleted` flag for recovery
5. **Post categories/tags:** Separate tables and relationships
6. **Comments on posts:** New entity with self-referencing or separate `comments` table
7. **File/image uploads:** For rich blog posts with images; requires S3 integration
8. **API rate limiting:** Via API Gateway or Spring Security filters
9. **Swagger/OpenAPI UI:** Springdoc OpenAPI for interactive docs (nice-to-have)
10. **Internationalization (i18n):** If multi-language content needed

---

## 15. References

- **PRD:** `/Users/munib/.openclaw/workspace/projects/secure-blog/PROJECT_PRD.md`
- **OpenAPI Spec:** `/Users/munib/.openclaw/workspace/projects/secure-blog/artifacts/api_contracts.json`
- **Database Schema:** `/Users/munib/.openclaw/workspace/projects/secure-blog/artifacts/db_schema.sql`
- **Spring Boot Docs:** https://docs.spring.io/spring-boot/docs/3.x/reference/html/
- **JWT Best Practices:** https://tools.ietf.org/html/rfc8725

---

**End of Architecture Design Document**
