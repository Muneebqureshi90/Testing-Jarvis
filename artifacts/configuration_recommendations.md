# Configuration Recommendations — Secure Blog REST API

This document provides concrete configuration changes to resolve the security issues identified in the audit. It is meant to be a practical guide for the backend development team.

---

## 1. JWT Secret Generation & Validation

### Generate a Strong Secret

Use OpenSSL to generate a cryptographically secure 512-bit (64-byte) secret:

```bash
# Generate 64 random bytes and base64 encode
openssl rand -base64 64
```

Example output (do not use this example, generate your own):
```
pZ0aV3nG7tK2rS9wX5yZ8A4qL1mN0pO3rI9uV6cE7sT2yB5nH8jK0lM2=
```

### Set in Production Environment

Export as environment variable:

```bash
export APP_JWT_SECRET="pZ0aV3nG7tK2rS9wX5yZ8A4qL1mN0pO3rI9uV6cE7sT2yB5nH8jK0lM2="
```

Or provide in your deployment configuration (Docker/K8s secrets). Do not commit this value to source control.

### Adjust JwtUtil Secret Check

In `JwtUtil.java`, replace the length check to validate byte length and UTF-8 encoding:

```java
import java.nio.charset.StandardCharsets;

@PostConstruct
public void init() {
    if (secret == null || secret.isBlank()) {
        logger.error("JWT secret is not set. Application will not start in production.");
        throw new IllegalStateException("JWT secret must be provided via APP_JWT_SECRET");
    }
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
        logger.error("JWT secret is too weak ({} bytes). Minimum required: 32 bytes (256 bits).", secretBytes.length);
        throw new IllegalStateException("JWT secret too short");
    }
    this.key = Keys.hmacShaKeyFor(secretBytes);
}
```

This will cause the application to fail fast on startup if the secret is missing or too short.

---

## 2. Reduce Access Token Expiration

Update `application.properties`:

```properties
# JWT Configuration
app.jwt.secret=${APP_JWT_SECRET}
app.jwt.expiration-ms=900000       # 15 minutes
app.jwt.refresh-expiration-ms=604800000  # 7 days (consider reducing if refresh token revocation is not implemented)
```

**Note:** After implementing refresh token persistence (see below), you may keep 7 days or reduce further.

---

## 3. Enable Database SSL/TLS

### Production Application Properties

Modify the JDBC URL to enforce SSL and verify server certificate:

```properties
spring.datasource.url=jdbc:mysql://db-hostname:3306/secure_blog?useSSL=true&requireSSL=true&verifyServerCertificate=true&serverTimezone=UTC&characterEncoding=utf8mb4
```

You will also need to:
- Ensure the MySQL server is configured with `require_secure_transport = ON` and has a server certificate (self-signed or from a CA).
- Import the CA certificate (or server cert if self-signed) into the Java truststore used by the application, or set `trustCertificateKeyStoreUrl` and `trustCertificateKeyStorePassword` in the connection properties.

Example with truststore:

```properties
spring.datasource.url=jdbc:mysql://db-hostname:3306/secure_blog?useSSL=true&requireSSL=true&verifyServerCertificate=true&trustCertificateKeyStoreUrl=file:/path/to/keystore.jks&trustCertificateKeyStorePassword=changeit&serverTimezone=UTC
```

### Development Override

In `application-dev.properties`, you may keep `useSSL=false` for local development convenience, but only if no sensitive data is used. For realistic testing, enable SSL on local DB as well.

---

## 4. Fix JWT Token Generation and User ID Extraction

### Option A: Custom UserDetails (Recommended)

Create a custom `UserDetails` implementation that includes the `userId`:

```java
// New file: src/main/java/com/example/blog/security/CustomUserDetails.java
package com.example.blog.security;

import com.example.blog.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final Collection<SimpleGrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Long getUserId() {
        return userId;
    }

    @Override public Collection<SimpleGrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

Modify `UserDetailsServiceImpl` to return `CustomUserDetails`:

```java
@Override
@Transactional
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    if (!user.isActive()) {
        throw new UsernameNotFoundException("User account is deactivated");
    }
    return new CustomUserDetails(user);
}
```

Update `JwtUtil.generateAccessToken(Authentication)`:

```java
public String generateAccessToken(Authentication authentication) {
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    return generateAccessToken(userDetails.getUsername(), userDetails.getUserId());
}
```

Similarly, fix `generateRefreshToken` if you add an overload.

### Option B: Fetch User from DB in JwtUtil (Simpler but adds DB hit)

Modify `JwtUtil.generateAccessToken(Authentication)`:

```java
public String generateAccessToken(Authentication authentication, UserRepository userRepository) {
    String username = ((UserDetails) authentication.getPrincipal()).getUsername();
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found during token generation"));
    return generateAccessToken(username, user.getId());
}
```

But then `JwtUtil` would need `UserRepository` injected, which complicates. Option A is cleaner.

### Update Controllers

In `PostController`, replace:

```java
Long userId = Long.parseLong(userDetails.getUsername());
```

with:

```java
CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
Long userId = customUserDetails.getUserId();
```

Alternatively, extract `userId` from the JWT token directly using `jwtUtil.extractUserId(request.getHeader("Authorization").substring(7))`, but that would parse token again. Better to rely on `CustomUserDetails`.

---

## 5. Implement Refresh Token Persistence and Revocation

### Database Migration

Add table `refresh_tokens`:

```sql
CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);
```

### Entity

```java
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
```

### Service Changes

- On successful login, generate refresh token, store in DB.
- On refresh: validate token signature and expiry via `JwtUtil`, then check DB: token exists, not revoked, not expired. If valid, revoke old token (`setRevoked(true)`) and issue new refresh token.
- On logout: revoke the refresh token.

In `AuthServiceImpl.authenticate()`:

```java
// after generating tokens
refreshTokenService.createRefreshToken(user, refreshToken);
```

In `AuthServiceImpl.refreshToken()`:

```java
// Validate token signature (already done) and then:
RefreshToken storedToken = refreshTokenService.findByToken(refreshToken)
    .orElseThrow(() -> new RuntimeException("Refresh token not found"));
if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
    throw new RuntimeException("Refresh token expired or revoked");
}
storedToken.setRevoked(true);
refreshTokenRepository.save(storedToken);

// proceed to generate new tokens and store new one
```

### Adjust JwtUtil Expiration Constants

Use the same refresh expiration from properties to set `expiresAt`.

---

## 6. Rate Limiting

Add dependency to `pom.xml` (e.g., bucket4j with Spring Boot starter):

```xml
<dependency>
    <groupId>com.gupaoedu</groupId>
    <artifactId>bucket4j-spring-boot-starter</artifactId>
    <version>0.9.0</version>
</dependency>
```

Or implement a simple filter:

```java
@Component
public class LoginRateLimiterFilter extends OncePerRequestFilter {

    private final Map<String, PaymentBucket> buckets = new ConcurrentHashMap<>();
    private final Bucket4jExtension extension = Bucket4j.extension();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().equals("/api/v1/auth/login") && request.getMethod().equals("POST")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket4j.builder()
                    .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)))
                    .build());

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(429);
                response.getWriter().write("Too many login attempts. Please try again later.");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
```

Then add the filter before `JwtAuthFilter` in `SecurityConfig`.

---

## 7. Fix Token Validation Exception Handling

Update `JwtAuthFilter.doFilterInternal`:

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        filterChain.doFilter(request, response);
        return;
    }

    final String jwt = authHeader.substring(7);
    try {
        final String username = jwtUtil.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Authenticated user: {}", username);
            }
        }
    } catch (JwtException | IllegalArgumentException e) {
        // Invalid token, just continue without setting authentication
        logger.debug("Invalid JWT token: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
}
```

The `AuthenticationEntryPoint` will handle unauthorized access when a protected endpoint is reached without valid authentication.

---

## 8. Generic Error Messages for Authentication

In `UserDetailsServiceImpl`:

```java
@Override
@Transactional
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                logger.warn("Authentication failed for username: {}", username);
                return new UsernameNotFoundException("Invalid username or password");
            });

    if (!user.isActive()) {
        logger.warn("Deactivated account login attempt: {}", username);
        throw new UsernameNotFoundException("Invalid username or password");
    }

    return new CustomUserDetails(user);
}
```

---

## 9. Disable Debug Logging in Production

In `application-prod.properties` (create if needed):

```properties
logging.level.com.example.blog=INFO
logging.level.org.springframework.security=WARN
logging.level.org.hibernate.SQL=WARN
```

Ensure `application.properties` does not set DEBUG globally. Use profile-specific overrides.

---

## 10. HSTS Header (Production)

Add to `SecurityConfig`:

```java
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'")
            .and()
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000) // 1 year
            )
        )
        // ... rest of config
}
```

But note: HSTS only works over HTTPS; ensure TLS termination is in place.

---

## Summary of Required Code Changes

| File | Change |
|------|--------|
| `JwtUtil.java` | Replace call to `generateTokenFromUsername` with correct overload; improve secret validation; import `StandardCharsets`. |
| `CustomUserDetails.java` | New class to carry `userId`. |
| `UserDetailsServiceImpl.java` | Return `CustomUserDetails`; use generic error messages. |
| `PostController.java` | Cast `UserDetails` to `CustomUserDetails` to obtain `userId`. |
| `AuthServiceImpl.java` | Ensure `generateAccessToken` and `generateRefreshToken` are called with username and userId (already using correct overload in refresh). The `authenticate` method currently uses the broken overload; fix to call `generateAccessToken(username, user.getId())`. |
| `AuthController.java` | May need to adjust to use correct service calls. |
| `application*.properties` | Set `useSSL=true`; reduce `app.jwt.expiration-ms` to `900000`; ensure secret env var overrides placeholder. |
| Create `RefreshToken` entity, repository, service, and migration. |
| Add rate limiting filter or gateway config. |
| `JwtAuthFilter.java` | Add try-catch for `JwtException` around token validation. |

---

After applying all critical fixes, re-run the PKI audit before proceeding to QA.
