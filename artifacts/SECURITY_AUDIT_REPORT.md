# Secure Blog REST API — Security Audit Report

## Executive Summary

- **Overall Risk Level:** **Critical**
- **Audit Date:** 2025-03-31 (based on code review of commit at handoff)
- **Auditor:** PKI-Specialist
- **Verdict:** **REJECTED**

The backend implementation contains **1 Critical compilation error**, **2 Critical security design flaws**, and **multiple High-severity vulnerabilities** that must be addressed before proceeding to QA or production. The code does not compile in its current state, and even if compilation issues were patched, the authentication/authorization logic is fundamentally broken due to incorrect user ID handling.

**Blocking Issues:**
- Missing method `generateTokenFromUsername` prevents compilation.
- User ID is derived by parsing the username as a `Long`, causing runtime failures for any non-numeric username.
- Database connections are configured to disable SSL (`useSSL=false`), exposing all data in transit.
- JWT access tokens expire after 24 hours, violating OAuth 2.0 best practices (should be ≤15 minutes).

---

## Compliance Check

| Framework | Status | Notes |
|-----------|--------|-------|
| GDPR (EU user data) | N/A | No EU-specific requirements defined in PRD. However, personal data (email, phone) should be protected; current state insufficient due to DB encryption disabled and weak token lifetimes. |
| HIPAA (healthcare data) | N/A | No healthcare data involved. |
| PCI-DSS (payment card data) | N/A | No payment processing. |
| SOC2 (audit logging) | Partial | Basic logging exists, but audit trail for security events (failed logins, token refresh) is not structured. Recommend JSON logging. |
| ISO 27001 (information security) | Non-Compliant | Multiple cryptographic and access control failures prevent compliance. |

---

## Key Findings

### 1. Authentication Security

**JWT Implementation: Flawed**

- **Secret Management:** Uses environment variable `APP_JWT_SECRET`. Good practice, but default placeholder is weak if not overridden. Secret length check uses character count instead of byte length. Should enforce ≥256 bits (32 bytes).
- **Token Expiration:** Access token = 24 hours (86400000 ms) — **Too long** (High severity). Refresh token = 7 days — acceptable if rotation and revocation are properly implemented.
- **Refresh Token Rotation:** Implemented (new refresh token issued on each refresh) but no invalidation of old tokens (stateless). Without a token blacklist/allowlist, stolen refresh tokens remain valid until expiry. High risk.
- **Revocation Strategy:** None. Cannot revoke tokens before expiration. High severity.
- **Token Location:** Access and refresh tokens returned in response body, encouraging localStorage storage (XSS risk). Medium severity.
- **Token Claims:** Missing `aud` (audience) and `iss` (issuer) claims, reducing token binding. Low severity.
- **Algorithm:** HMAC-SHA256 using `Keys.hmacShaKeyFor` — correct.

**Critical Code Defects:**

- `JwtUtil.generateAccessToken(Authentication)` calls non-existent method `generateTokenFromUsername` → **compilation failure**.
- The method incorrectly attempts `Long.parseLong(userDetails.getUsername())` assuming username is numeric. In `PostController`, they also parse `Long.parseLong(userDetails.getUsername())` to get userId. This design mixes username with numeric ID and will crash for any non-numeric username. The proper approach is to store the `userId` in the JWT claims (already done) and retrieve it via `jwtUtil.extractUserId(token)`, or include it in a custom `UserDetails` implementation.

**Password Hashing: Strong**

- Algorithm: BCrypt
- Strength: 12 rounds — appropriate.
- Salt: Per-user unique (handled by BCrypt).
- Peppering: Not implemented (optional but recommended).

### 2. Secrets Management

- **Hardcoded Secrets:** None found in source code (JWT secret and DB credentials use environment variable placeholders). Good.
- **Environment Variables:** Used via `${VAR:default}` pattern. Correct.
- **`.env` Files:** Not present. `application.properties` contains placeholder secrets; acceptable as long as defaults are changed in production.
- **Note:** Database URL contains `useSSL=false` — this is a configuration secret that disables encryption. See TLS/SSL section.

### 3. TLS/SSL Configuration

- **Application-Level HTTPS:** Not configured. Deployments should be behind a TLS-terminating reverse proxy (e.g., Nginx, ALB). This is acceptable if enforced at infrastructure, but not verified.
- **Database TLS:** **Disabled** (`useSSL=false` in JDBC URL). **CRITICAL** for production. All database traffic, including credentials and user content, will be transmitted in clear text. Must enable `useSSL=true` and configure proper certificates.
- **TLS Version:** Not applicable at app level; dependent on infrastructure.
- **Certificate Validation:** Not shown; assume handled by proxy.
- **HSTS:** Not implemented. Recommended for browsers.

### 4. Authorization (RBAC/ABAC)

- **Role Definitions:** Single role `ROLE_USER`. No admin role present. All authenticated users have same privileges. This is acceptable given scope.
- **Privilege Escalation:** None beyond single role.
- **Ownership Checks:** Implemented in `PostServiceImpl.updatePost` and `deletePost` with explicit checks `if (!post.getAuthor().getId().equals(userId))` — correct. However, the `userId` is derived incorrectly from parsing the username, so these checks may never match or throw exceptions.
- **Method Security:** Uses `@AuthenticationPrincipal` in controllers to inject user details; consistent.

### 5. Cryptographic Primitives

- **SecureRandom:** Not explicitly used for token IDs; JWT uses `j	jwt` library which uses secure randomness for generated tokens (if using standard JJWT). Good.
- **Encryption:** None in this codebase; not required.
- **Signing:** HMAC-SHA256 with strong key (provided secret is strong). Good.
- **Hashing (non-password):** Not applicable.

### 6. Input Validation & Injection Prevention

- **SQL Injection:** Prevented via Spring Data JPA (no native queries found). All repository methods are derived safely. Good.
- **XSS:** Frontend responsibility, but note JWT tokens in localStorage can be stolen via XSS. Not directly backend, but token storage recommendation is relevant.
- **SSRF:** Not applicable.
- **Command Injection:** No `Runtime.exec` observed.

### 7. Rate Limiting

- **Status:** Not implemented in code.
- **Recommendation:** Implement at API gateway or via Spring Security's `RequestRateLimiter` (if using resilience4j or bucket4j). Required to mitigate brute force attacks on `/api/auth/login`.

### 8. Logging & Monitoring (Security)

- **Sensitive Data in Logs:** Current logs at DEBUG level include usernames and some object dumps. Ensure production logging filters out passwords and tokens. No apparent leakage of tokens or passwords, but should audit log statements.
- **Structured Logging:** Not implemented; uses plain text layout.
- **Audit Trail:** Minimal. Failed login attempts logged via `JwtAuthenticationEntryPoint` at WARN level. Good start but not comprehensive.
- **Alerting:** Not configured.

### 9. Dependency Security

- **Spring Boot:** 3.2.0 — current stable, good.
- **Java:** 17 — current.
- **JJWT:** 0.11.5 — latest, no known critical CVEs as of 2024. Good.
- **Pinned Dependencies:** All versions explicitly set; no floating `LATEST`. Good.
- **Known CVEs:** Not scanned, but based on versions, none obvious.

---

## Detailed Issue Log

| Issue ID | Severity | Category | File | Line | Description | Recommendation |
|----------|----------|----------|------|------|-------------|----------------|
| C1 | Critical | Compilation | JwtUtil.java | 75 | Calls undefined method `generateTokenFromUsername`. Code does not compile. | Replace with call to existing `generateAccessToken(String username, Long userId)` after obtaining userId from authentication principal or token claims. |
| C2 | Critical | Auth Logic | JwtUtil.java, PostController.java, AuthController.java | 75, 67, 57 | User ID derived via `Long.parseLong(userDetails.getUsername())`. Assumes username is numeric. Will cause `NumberFormatException` for typical usernames. | Store `userId` in JWT claims (already done) and retrieve via `jwtUtil.extractUserId(token)`. Alternatively, create a custom `UserDetails` that includes `userId`. |
| C3 | Critical | Data-in-Transit | application.properties, application-dev.properties | 5, 2 | JDBC URL sets `useSSL=false`. Database traffic unencrypted. | Change to `useSSL=true` and configure server/client certificates. For dev, may keep false, but prod must enforce TLS. |
| H1 | High | Token Lifetime | application.properties | 10 | Access token expiration = 24 hours. Violates best practices (max 15 minutes). | Reduce to 15 minutes (`900000` ms). |
| H2 | High | Refresh Token Security | AuthServiceImpl.java | 78-85 | Refresh tokens are stateless, no invalidation of old tokens. Stolen refresh token can be reused until expiry (7 days). | Implement refresh token persistence in DB with revocation/rotation. Invalidate previous token on each refresh. |
| H3 | High | Brute Force | AuthController.java, SecurityConfig.java | 24, 45 | No rate limiting on login endpoint. | Add rate limiting via gateway or Spring Security config (e.g., `Bucket4j`). |
| H4 | High | Token Storage | Frontend (not in scope) | N/A | Tokens returned in response body likely stored in localStorage, vulnerable to XSS. | Use HTTP-only, SameSite=Strict cookies for refresh tokens; access token in memory. Document secure storage. |
| M1 | Medium | Exception Handling | JwtAuthFilter.java | 33-38 | `validateToken` may throw `JwtException` (expired, invalid signature) leading to 500 instead of 401. | Wrap `validateToken` in try-catch, treat any exception as invalid, and continue chain (filter will eventually trigger entry point returning 401). |
| M2 | Medium | Information Leakage | UserDetailsServiceImpl.java | 23-27 | Different messages for "not found" vs "deactivated" allow username enumeration. | Return generic "Invalid username or password" for both cases. |
| M3 | Medium | Secret Validation | JwtUtil.java | 32-34 | Secret length check uses `secret.length() < 32` (characters) not bytes. For non-ASCII secrets, could be weak. | Check `secret.getBytes(StandardCharsets.UTF_8).length >= 32`. |
| L1 | Low | Token Claims | JwtUtil.java | 68-70 | JWT lacks `aud` (audience) and `iss` (issuer) claims. | Add configuration for `issuer` and `audience`, include in token. |
| L2 | Low | Swagger Exposure | SecurityConfig.java | 29 | Swagger UI and API docs are publicly accessible. Acceptable for dev but may expose API structure in prod. | Secure with `permitAll()` only for dev profile or disable in prod. |

---

## Recommendations

### Immediate Fixes (Before Production)

1. **Fix Compilation Error** (C1): Rename call to `generateAccessToken(String username, Long userId)`. Implement correct extraction of `userId` from the `Authentication` principal. The `UserDetails` currently does not carry `userId`. Options:
   - Create a custom `UserDetails` implementation that includes `Long userId`.
   - In `generateAccessToken(Authentication)`, retrieve the username, then fetch the `User` entity from DB to get `userId`. (Adds DB hit; acceptable for login frequency.)
   - Store `userId` in a claim during authentication and reuse from token? No, token not yet created.
2. **Correct User ID Handling** (C2): Ensure all code retrieves `userId` from a reliable source, not by parsing username.
3. **Enable Database SSL** (C3): Set `useSSL=true` in JDBC URL and configure certificates. For MySQL, set `requireSSL=true` and `verifyServerCertificate=true` as needed.
4. **Reduce Access Token Expiration** (H1): Set `app.jwt.expiration-ms=900000` (15 minutes).
5. **Implement Refresh Token Revocation** (H2): Create a `RefreshToken` entity with fields `token`, `userId`, `expiry`, `revoked`. On refresh, mark old token revoked and delete or keep for audit. Validate against DB. This requires a new table and service.
6. **Add Rate Limiting** (H3): Apply to `/api/auth/login` (and `/api/auth/refresh`). Recommended limit: 5 attempts per minute per IP.
7. **Fix Token Validation Exception Handling** (M1): In `JwtAuthFilter.doFilterInternal`, catch `JwtException` when calling `jwtUtil.validateToken` and treat as invalid, allowing the request to proceed to `filterChain` which will trigger `AuthenticationEntryPoint` (401). Do not rethrow.
8. **Remove Username Enumeration** (M2): In `UserDetailsServiceImpl`, throw `UsernameNotFoundException` with same generic message for both not-found and deactivated cases.
9. **Improve Secret Length Validation**: Change `JwtUtil.init()` to check `secret.getBytes(StandardCharsets.UTF_8).length >= 32`. Add a logger error if too short and refuse to start (fail fast) in production.

### Long-Term Improvements

1. **Secure Token Storage**: Frontend guidance: store refresh token in HTTP-only, Secure, SameSite=Strict cookie. Access token in memory (e.g., React state). Consider CSRF considerations when using cookies.
2. **Add HSTS**: In production reverse proxy, add `Strict-Transport-Security` header.
3. **Structured JSON Logging**: Use `logstash-logback-encoder` or similar to emit JSON logs for SIEM integration.
4. **Add `aud` and `iss` Claims**: Enhance JWT with audience and issuer to prevent misuse.
5. **Security Headers**: Use Spring Security's `HeadersWriterFilter` to add `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`.
6. **Penetration Testing**: Perform dynamic scanning (OWASP ZAP) and dependency checks (`mvn dependency:check`).
7. **Secret Management**: Integrate with AWS Secrets Manager or HashiCorp Vault for JWT secret and DB credentials, removing reliance on environment variables.
8. **Audit Logging**: Log security events (login success/failure, refresh, post create/update/delete) with user ID, IP, user agent.

---

## Conclusion

The current codebase is **NOT APPROVED** for QA or deployment. There are **Critical** issues that prevent the application from compiling and functioning correctly, as well as **High**-severity security misconfigurations that expose the system to credential theft, data interception, and account takeover.

**All Critical and High issues must be fixed** and a re-audit requested before the project can advance to the QA stage.

---

## Appendix: Test Cases Executed

This audit was performed via:
- Static code review of all Java source files under `src/main/java/`
- Configuration review of `application.properties` and `application-dev.properties`
- Dependency version analysis against `pom.xml`
- Repository query method inspection for injection safety
- Checklist validation against OWASP Top 10 and NIST SP 800-63B

No dynamic runtime tests were executed.

**Re-audit Request:** After applying fixes, resubmit the code for a focused re-audit of modified modules. The PKI-Specialist will verify all issues are resolved.
