# Secure Blog REST API

A production-ready, secure blogging REST API built with Spring Boot 3.x, featuring JWT authentication, user management, and full CRUD operations for blog posts.

## Features

- User registration and authentication with JWT (access + refresh tokens)
- Secure password storage using BCrypt (strength 12)
- Full CRUD operations for blog posts with ownership enforcement
- Pagination support for post listings
- Comprehensive error handling with standardized error responses
- Database connection pooling with HikariCP
- MySQL 8.x with proper indexes for performance
- Stateless architecture for horizontal scaling

## Tech Stack

- **Backend:** Spring Boot 3.x (Java 17+)
- **Database:** MySQL 8.x
- **ORM:** Spring Data JPA (Hibernate)
- **Authentication:** Spring Security + JWT (jjwt)
- **Build Tool:** Maven
- **API Style:** RESTful with JSON payloads

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- MySQL 8.x (or MariaDB 10.5+)

## Quick Start

### 1. Clone and Setup

```bash
cd /path/to/secure-blog
```

### 2. Database Setup

Create the MySQL database and run the schema:

```bash
mysql -u root -p < artifacts/db_schema.sql
```

Or manually:

```sql
CREATE DATABASE secure_blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE secure_blog;
-- Execute the contents of artifacts/db_schema.sql
```

### 3. Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
app.jwt.secret=your-256-bit-secret-key-here
```

**Important:** The JWT secret must be at least 256 bits (32 bytes). Generate one using:

```bash
openssl rand -base64 32
```

Alternatively, use environment variables:

```bash
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export APP_JWT_SECRET=$(openssl rand -base64 32)
```

### 4. Build the Application

```bash
mvn clean package
```

### 5. Run the Application

```bash
java -jar target/secure-blog-1.0.0.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

The API will be available at: `http://localhost:8080/api/v1`

### 6. Verify Health

Check the health endpoint:

```bash
curl http://localhost:8080/api/v1/actuator/health
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user account |
| POST | `/auth/login` | Authenticate and receive JWT tokens |
| POST | `/auth/refresh` | Refresh an expired access token |

#### Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890"
  }'
```

#### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePassword123!"
  }'
```

Response includes `accessToken` and `refreshToken`.

### Posts

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| GET | `/posts` | No | List all posts (public) |
| GET | `/posts/{id}` | No | Get single post (public) |
| POST | `/posts` | Yes | Create new post |
| PUT | `/posts/{id}` | Yes | Update own post |
| DELETE | `/posts/{id}` | Yes | Delete own post |

#### Create Post (Authenticated)

```bash
curl -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "title": "My First Blog Post",
    "content": "This is the content of my blog post...",
    "published": false
  }'
```

#### List Posts (Public)

```bash
curl http://localhost:8080/api/v1/posts
```

## Sample API Calls

### Complete Authentication Flow

1. **Register a user:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"alice","email":"alice@example.com","password":"SecurePass123","firstName":"Alice","lastName":"Smith"}'
   ```

2. **Login:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"alice","password":"SecurePass123"}'
   ```

   Save the `accessToken` from the response.

3. **Create a post:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/posts \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_TOKEN_HERE" \
     -d '{"title":"Welcome","content":"Hello world!","published":true}'
   ```

4. **Get your post:**
   ```bash
   curl http://localhost:8080/api/v1/posts/1
   ```

## Project Structure

```
secure-blog/
├── src/main/java/com/example/blog/
│   ├── SecureBlogApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── JwtAuthFilter.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── PostController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── PostService.java
│   │   └── impl/
│   │       ├── AuthServiceImpl.java
│   │       └── PostServiceImpl.java
│   ├── service/
│   │   └── mapper/
│   │       └── BlogMapper.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── PostRepository.java
│   ├── model/
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   └── Post.java
│   │   └── dto/
│   │       ├── RegisterUserRequest.java
│   │       ├── LoginRequest.java
│   │       ├── RefreshTokenRequest.java
│   │       ├── UserResponse.java
│   │       ├── AuthorInfo.java
│   │       ├── CreatePostRequest.java
│   │       ├── UpdatePostRequest.java
│   │       ├── PostResponse.java
│   │       ├── LoginResponse.java
│   │       └── ErrorResponse.java
│   ├── security/
│   │   ├── JwtUtil.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── UserDetailsServiceImpl.java
│   └── exception/
│       ├── ResourceNotFoundException.java
│       ├── ConflictException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.properties
│   └── application-dev.properties
├── pom.xml
└── README.md
```

## Environment Variables

You can configure the application using environment variables instead of editing `application.properties`:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | (empty) |
| `APP_JWT_SECRET` | JWT signing secret | dev placeholder |
| `SERVER_PORT` | Server port | `8080` |

Example:

```bash
export DB_USERNAME=blog_user
export DB_PASSWORD=strong_password
export APP_JWT_SECRET=$(openssl rand -base64 32)
java -jar target/secure-blog-1.0.0.jar
```

## Testing

Run tests:

```bash
mvn test
```

Basic test coverage has been implemented for service layer logic. More comprehensive tests can be added as needed.

## Error Handling

The API returns standardized error responses:

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Post not found with id: 999",
  "path": "/api/posts/999"
}
```

Common error scenarios:
- **400 Bad Request:** Validation failures, duplicate username/email
- **401 Unauthorized:** Invalid or missing JWT token
- **403 Forbidden:** User attempting to modify another user's post
- **404 Not Found:** Requested resource does not exist
- **500 Internal Server Error:** Unexpected server errors (details hidden in production)

## Security Considerations

1. **JWT Secret:** Use a strong, random secret (minimum 256 bits). Never use the development secret in production.
2. **Database Credentials:** Store in environment variables or secrets manager. Never commit to version control.
3. **HTTPS:** Deploy behind a load balancer with TLS termination in production.
4. **Rate Limiting:** Consider adding rate limiting at the API gateway or load balancer level.
5. **Password Policy:** Enforce strong passwords in production (add complexity rules).
6. **Email Verification:** Recommended for production to verify user emails.

## Database Schema

See `artifacts/db_schema.sql` for the complete schema with indexes.

Key tables:
- `users`: Stores user accounts with BCrypt password hashes
- `posts`: Blog posts with foreign key to users (cascade delete)

Indexes optimize common queries:
- `idx_email` and `idx_username` for user lookups
- `idx_author`, `idx_published`, `idx_created_at` for post queries

## Development Notes

- The application uses **stateless JWT authentication**. No server-side sessions.
- **Authorization** is enforced at the service layer for ownership checks.
- **Transactions** are used at the service layer with `@Transactional`.
- **Lombok** is used to reduce boilerplate code.
- **SLF4J** with Logback is configured for logging.

## Future Enhancements

- Email verification workflow
- Password reset functionality
- Role-based access control (ADMIN role)
- Pagination with cursor-based approach for large datasets
- Redis caching for frequently accessed posts
- API rate limiting
- Swagger/OpenAPI UI for interactive documentation
- Social login integration (OAuth2)
- Post categories and tags
- Comments on posts
- Image/file uploads for rich content

## Deployment

### Docker (Recommended)

A Dockerfile can be created following these steps:

```dockerfile
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17-jre-slim
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build and run:

```bash
docker build -t secure-blog .
docker run -p 8080:8080 -e APP_JWT_SECRET=your-secret secure-blog
```

### Manual Deployment

Ensure Java 17+ and MySQL 8 are installed. Set environment variables and run the JAR.

## License

This project is created for demonstration purposes.

## Support

For issues or questions, please refer to the project repository.