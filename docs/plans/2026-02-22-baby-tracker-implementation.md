# Baby Tracker App — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a web-based baby tracking app for Taiwanese mothers (0–6 yr), with one-tap daily logs, growth charts, Taiwan CDC vaccination schedule, complementary food diary with allergy tracking, and family sharing via invite link.

**Architecture:** Spring Boot 3 REST API (Java) + Next.js 14 frontend (TypeScript). PostgreSQL for persistence with Flyway migrations. JWT auth (access token in memory, refresh token in HttpOnly cookie). Resend for transactional email. Deploy backend on Railway, frontend on Vercel.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Mockito; Next.js 14, React 18, TypeScript, Tailwind CSS, Recharts (growth chart), Jest, React Testing Library.

---

## Phase 0 — Project Scaffolding

### Task 0-A: Bootstrap Spring Boot backend

**Files:**
- Create: `backend/` (project root)

**Step 1: Generate project via Spring Initializr**

```bash
curl -s https://start.spring.io/starter.zip \
  -d type=gradle-project \
  -d language=java \
  -d bootVersion=3.4.2 \
  -d baseDir=backend \
  -d groupId=com.ccweb \
  -d artifactId=baby-tracker \
  -d packageName=com.ccweb.babytracker \
  -d javaVersion=21 \
  -d dependencies=web,data-jpa,postgresql,security,validation,flyway,mail \
  -o backend.zip && unzip backend.zip && rm backend.zip
```

**Step 2: Verify structure**

```
backend/
├── build.gradle
├── src/main/java/com/ccweb/babytracker/
└── src/test/java/com/ccweb/babytracker/
```

**Step 3: Add missing dependencies to `backend/build.gradle`**

Inside `dependencies {}` add:
```groovy
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
implementation 'com.resend:resend-java:3.1.0'
```

**Step 4: Commit**

```bash
git add backend/
git commit -m "chore: scaffold Spring Boot 3 backend"
```

---

### Task 0-B: Bootstrap Next.js frontend

**Files:**
- Create: `frontend/` (project root)

**Step 1: Create Next.js app**

```bash
npx create-next-app@14 frontend \
  --typescript \
  --tailwind \
  --eslint \
  --app \
  --no-src-dir \
  --import-alias "@/*"
```

**Step 2: Add dependencies**

```bash
cd frontend && npm install \
  axios \
  recharts \
  react-hook-form \
  zod \
  @hookform/resolvers \
  date-fns \
  js-cookie
npm install -D @types/js-cookie
```

**Step 3: Delete boilerplate from `frontend/app/page.tsx`** — replace with:

```tsx
export default function Home() {
  return <main className="p-4">Baby Tracker</main>;
}
```

**Step 4: Commit**

```bash
git add frontend/
git commit -m "chore: scaffold Next.js 14 frontend"
```

---

### Task 0-C: Docker Compose for local PostgreSQL

**Files:**
- Create: `docker-compose.yml`

**Step 1: Write `docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: babytracker
      POSTGRES_USER: bt_user
      POSTGRES_PASSWORD: bt_pass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

**Step 2: Write `backend/src/main/resources/application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/babytracker
    username: bt_user
    password: bt_pass
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-in-prod-min-32chars}
    access-expiry-ms: 900000       # 15 min
    refresh-expiry-ms: 604800000   # 7 days
  resend:
    api-key: ${RESEND_API_KEY:}
    from: noreply@babytracker.app
```

**Step 3: Write `backend/src/test/resources/application.yml`** (H2 for tests)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
```

Add H2 test dependency to `backend/build.gradle`:
```groovy
testRuntimeOnly 'com.h2database:h2'
```

**Step 4: Start database and verify**

```bash
docker compose up -d
docker compose ps   # db should be "running"
```

**Step 5: Commit**

```bash
git add docker-compose.yml backend/src/main/resources/ backend/src/test/resources/ backend/build.gradle
git commit -m "chore: add Docker Compose + Spring Boot config"
```

---

## Phase 1 — Database Migrations

### Task 1-A: Core schema migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__core_schema.sql`

**Step 1: Write migration**

```sql
-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  name          TEXT NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Babies
CREATE TABLE babies (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name       TEXT NOT NULL,
  birth_date DATE NOT NULL,
  gender     CHAR(1),           -- 'M' | 'F' | NULL
  extra      JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Family members (join table)
CREATE TABLE family_members (
  baby_id UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role    TEXT NOT NULL DEFAULT 'viewer',   -- 'owner' | 'viewer'
  PRIMARY KEY (baby_id, user_id)
);

-- Invitations
CREATE TABLE invitations (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id    UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  token      TEXT UNIQUE NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at    TIMESTAMPTZ
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT UNIQUE NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked    BOOLEAN NOT NULL DEFAULT false
);
```

**Step 2: Write migration for tracking and growth**

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__tracking_growth.sql`

```sql
-- Daily tracking logs
CREATE TABLE tracking_logs (
  id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id   UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  user_id   UUID NOT NULL REFERENCES users(id),
  type      TEXT NOT NULL,   -- FEED | SLEEP_START | SLEEP_END | DIAPER_WET | DIAPER_DIRTY
  value     NUMERIC(8,2),
  note      TEXT,
  metadata  JSONB NOT NULL DEFAULT '{}',
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tracking_baby_date ON tracking_logs(baby_id, logged_at DESC);

-- Growth records
CREATE TABLE growth_records (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id     UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  weight_kg   NUMERIC(5,3),
  height_cm   NUMERIC(5,1),
  head_cm     NUMERIC(5,1),
  measured_at DATE NOT NULL,
  note        TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Step 3: Write migration for food, vaccines, content**

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__food_vaccines_content.sql`

```sql
-- Complementary food catalogue per baby
CREATE TABLE baby_foods (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id          UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  name             TEXT NOT NULL,
  category         TEXT,   -- 蔬菜|水果|蛋白質|穀類|其他
  first_introduced DATE,
  allergy_status   TEXT NOT NULL DEFAULT 'normal',  -- normal|suspected|confirmed
  notes            TEXT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Food logs (daily)
CREATE TABLE food_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id       UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  food_id       UUID NOT NULL REFERENCES baby_foods(id),
  amount        TEXT,
  texture       TEXT,   -- 泥狀|碎末|小塊
  reaction      TEXT NOT NULL DEFAULT 'normal',  -- normal|rash|vomit|other
  reaction_note TEXT,
  metadata      JSONB NOT NULL DEFAULT '{}',
  logged_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Vaccine definitions (pre-seeded)
CREATE TABLE vaccines (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  age_months  INT NOT NULL,
  dose_number INT NOT NULL DEFAULT 1,
  funded      BOOLEAN NOT NULL DEFAULT true,
  description TEXT,
  source_url  TEXT,
  sort_order  INT NOT NULL DEFAULT 0
);

-- Vaccine records per baby
CREATE TABLE vaccine_records (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  baby_id         UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  vaccine_id      UUID NOT NULL REFERENCES vaccines(id),
  administered_at DATE,
  clinic          TEXT,
  note            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Health education articles
CREATE TABLE health_articles (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title          TEXT NOT NULL,
  content        TEXT NOT NULL,
  age_min_months INT NOT NULL,
  age_max_months INT NOT NULL,
  tags           TEXT[] NOT NULL DEFAULT '{}',
  source_url     TEXT,
  published_at   TIMESTAMPTZ,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Notifications
CREATE TABLE notifications (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       TEXT NOT NULL,  -- VACCINE_REMINDER|MILESTONE|ARTICLE
  title      TEXT NOT NULL,
  body       TEXT,
  is_read    BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Scheduled email reminders
CREATE TABLE email_reminders (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  baby_id      UUID NOT NULL REFERENCES babies(id) ON DELETE CASCADE,
  type         TEXT NOT NULL,
  scheduled_at TIMESTAMPTZ NOT NULL,
  sent_at      TIMESTAMPTZ
);
```

**Step 4: Write Taiwan CDC vaccine seed migration**

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__seed_vaccines.sql`

```sql
INSERT INTO vaccines (name, age_months, dose_number, funded, description, source_url, sort_order) VALUES
('B型肝炎疫苗 第1劑',  0,  1, true, '出生24小時內接種，預防B型肝炎', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 10),
('B型肝炎疫苗 第2劑',  1,  2, true, '出生滿1個月接種', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 20),
('五合一疫苗 第1劑',   2,  1, true, '預防白喉、破傷風、百日咳、小兒麻痺、b型嗜血桿菌', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 30),
('13價肺炎鏈球菌 第1劑', 2, 1, true, '預防肺炎鏈球菌感染', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 31),
('五合一疫苗 第2劑',   4,  2, true, '預防白喉、破傷風、百日咳、小兒麻痺、b型嗜血桿菌', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 40),
('13價肺炎鏈球菌 第2劑', 4, 2, true, '預防肺炎鏈球菌感染', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 41),
('卡介苗',             5,  1, true, '預防結核病（滿5個月接種）', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 50),
('五合一疫苗 第3劑',   6,  3, true, '預防白喉、破傷風、百日咳、小兒麻痺、b型嗜血桿菌', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 60),
('B型肝炎疫苗 第3劑',  6,  3, true, '出生滿6個月接種', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 61),
('13價肺炎鏈球菌 追加劑', 12, 3, true, '出生滿12-15個月追加', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 70),
('MMR三合一疫苗',      12, 1, true, '預防麻疹、腮腺炎、德國麻疹', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 71),
('水痘疫苗',           12, 1, true, '預防水痘', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 72),
('日本腦炎疫苗 第1劑', 15, 1, true, '預防日本腦炎', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 73),
('日本腦炎疫苗 第2劑', 15, 2, true, '與第1劑間隔2週', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 74),
('五合一疫苗 第4劑',   18, 4, true, '出生滿18個月追加', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 80),
('A型肝炎疫苗 第1劑',  18, 1, true, '114年起新增，出生滿18個月接種', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 81),
('A型肝炎疫苗 第2劑',  27, 2, true, '與第1劑間隔9個月', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 82),
('日本腦炎追加劑',     60, 3, true, '滿5歲入小學前追加', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 90),
('DTaP-IPV',           60, 1, true, '入小學前追加，預防白喉、破傷風、百日咳、小兒麻痺', 'https://www.cdc.gov.tw/Category/Page/TxRW-x3WzvPhvEtxM628GA', 91);
```

**Step 5: Run migrations**

```bash
cd backend && ./gradlew flywayMigrate
```

Expected: `Successfully applied 4 migrations`

**Step 6: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add Flyway migrations and Taiwan CDC vaccine seed data"
```

---

## Phase 2 — Backend: Auth

### Task 2-A: JPA entities for User and RefreshToken

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/user/User.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/user/RefreshToken.java`

**Step 1: Write `User.java`**

```java
package com.ccweb.babytracker.domain.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // getters / setters omitted — use Lombok @Data or generate with IDE
}
```

**Step 2: Write `RefreshToken.java`**

```java
package com.ccweb.babytracker.domain.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;
}
```

**Step 3: Create repositories**

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/user/UserRepository.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/user/RefreshTokenRepository.java`

```java
// UserRepository.java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// RefreshTokenRepository.java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String hash);
    void deleteAllByUserId(UUID userId);
}
```

**Step 4: Commit**

```bash
git add backend/src/main/java/
git commit -m "feat: add User and RefreshToken JPA entities"
```

---

### Task 2-B: JWT utility

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/security/JwtUtils.java`
- Create: `backend/src/test/java/com/ccweb/babytracker/security/JwtUtilsTest.java`

**Step 1: Write failing test**

```java
@SpringBootTest
class JwtUtilsTest {
    @Autowired JwtUtils jwtUtils;

    @Test void generateAndValidateAccessToken() {
        String token = jwtUtils.generateAccessToken("user-id-123");
        assertTrue(jwtUtils.isValid(token));
        assertEquals("user-id-123", jwtUtils.extractUserId(token));
    }

    @Test void expiredTokenIsInvalid() {
        // Test with a pre-generated expired token string (hardcoded)
        assertFalse(jwtUtils.isValid("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4IiwiZXhwIjoxfQ.abc"));
    }
}
```

**Step 2: Run test — expect failure**

```bash
cd backend && ./gradlew test --tests "*.JwtUtilsTest"
```

Expected: FAIL — `JwtUtils` not found

**Step 3: Implement `JwtUtils.java`**

```java
package com.ccweb.babytracker.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {
    private final SecretKey key;
    private final long accessExpiryMs;

    public JwtUtils(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-expiry-ms}") long expiryMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiryMs = expiryMs;
    }

    public String generateAccessToken(String userId) {
        return Jwts.builder()
            .subject(userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpiryMs))
            .signWith(key)
            .compact();
    }

    public boolean isValid(String token) {
        try { getClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
```

**Step 4: Run test — expect pass**

```bash
./gradlew test --tests "*.JwtUtilsTest"
```

Expected: PASS

**Step 5: Commit**

```bash
git add backend/src/main/java/com/ccweb/babytracker/security/ backend/src/test/
git commit -m "feat: add JWT utility with access token generation"
```

---

### Task 2-C: Auth endpoints (register + login + refresh + logout)

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/auth/AuthController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/auth/AuthService.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/auth/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/auth/dto/AuthResponse.java`
- Create: `backend/src/test/java/com/ccweb/babytracker/auth/AuthControllerTest.java`

**Step 1: Write failing integration test**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test void registerAndLogin() throws Exception {
        // Register
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                    "email", "test@example.com",
                    "password", "Secret123!",
                    "name", "Test User"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists());

        // Login
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                    "email", "test@example.com",
                    "password", "Secret123!"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test void loginWithWrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                    "email", "nobody@example.com",
                    "password", "wrong"
                ))))
            .andExpect(status().isUnauthorized());
    }
}
```

**Step 2: Run — expect failure**

```bash
./gradlew test --tests "*.AuthControllerTest"
```

**Step 3: Implement DTOs, service, and controller**

`RegisterRequest.java`:
```java
public record RegisterRequest(
    @Email @NotBlank String email,
    @Size(min=8) @NotBlank String password,
    @NotBlank String name
) {}
```

`LoginRequest.java`:
```java
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
```

`AuthResponse.java`:
```java
public record AuthResponse(String accessToken) {}
```

`AuthService.java` — key logic:
```java
@Service @Transactional
public class AuthService {
    // inject UserRepository, RefreshTokenRepository, JwtUtils, PasswordEncoder

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setName(req.name());
        userRepository.save(user);
        return new AuthResponse(jwtUtils.generateAccessToken(user.getId().toString()));
    }

    public AuthResponse login(LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        setRefreshCookie(user, response);
        return new AuthResponse(jwtUtils.generateAccessToken(user.getId().toString()));
    }

    private void setRefreshCookie(User user, HttpServletResponse response) {
        String raw = UUID.randomUUID().toString();
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(DigestUtils.sha256Hex(raw));
        rt.setExpiresAt(Instant.now().plusMillis(refreshExpiryMs));
        refreshTokenRepository.save(rt);
        Cookie cookie = new Cookie("refreshToken", raw);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int)(refreshExpiryMs / 1000));
        response.addCookie(cookie);
    }
}
```

`AuthController.java`:
```java
@RestController @RequestMapping("/api/auth")
public class AuthController {
    @Autowired AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        return authService.login(req, res);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue("refreshToken") String token) {
        return authService.refresh(token);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(value = "refreshToken", required = false) String token,
                       HttpServletResponse res) {
        authService.logout(token, res);
    }
}
```

Configure `SecurityFilterChain` to allow `/api/auth/**` without auth, protect everything else.

**Step 4: Run tests — expect pass**

```bash
./gradlew test --tests "*.AuthControllerTest"
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/ccweb/babytracker/auth/ backend/src/test/
git commit -m "feat: add register/login/refresh/logout auth endpoints"
```

---

## Phase 3 — Backend: Baby + Family

### Task 3-A: Baby CRUD endpoints

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/baby/Baby.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/baby/BabyRepository.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/baby/BabyController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/baby/BabyService.java`
- Test: `backend/src/test/java/com/ccweb/babytracker/baby/BabyControllerTest.java`

**Step 1: Write failing test**

```java
@SpringBootTest(webEnvironment = RANDOM_PORT) @AutoConfigureMockMvc
class BabyControllerTest {
    // Helpers: registerUser(), loginAndGetToken()

    @Test void createBabyAndList() throws Exception {
        String token = loginAndGetToken("mom@test.com", "Secret123!");

        // Create
        mvc.perform(post("/api/babies")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("""
                    {"name":"小明","birthDate":"2025-11-01","gender":"M"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("小明"));

        // List
        mvc.perform(get("/api/babies")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("小明"));
    }
}
```

**Step 2: Run — expect failure, then implement**

Key implementation notes:
- `POST /api/babies` → create baby, add creator as `owner` in `family_members`
- `GET /api/babies` → return all babies where calling user is a family member
- `GET /api/babies/{id}` → return single baby (must be family member)
- `PUT /api/babies/{id}` → update name/gender/extra (owner only)
- All endpoints extract `userId` from JWT via Spring Security `Authentication`

**Step 3: Run tests — expect pass, then commit**

```bash
./gradlew test --tests "*.BabyControllerTest"
git add backend/src/main/java/ backend/src/test/
git commit -m "feat: add baby CRUD endpoints with family scoping"
```

---

### Task 3-B: Family invite endpoints

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/baby/Invitation.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/baby/InviteController.java`
- Test: `backend/src/test/java/com/ccweb/babytracker/baby/InviteControllerTest.java`

**Endpoints:**
- `POST /api/babies/{id}/invitations` → owner only → returns `{ token, expiresAt }`
  - Token is a UUID, expires in 48 hours
- `POST /api/invitations/{token}/accept` → authenticated user accepts → added as viewer

**Step 1: Write failing tests**

```java
@Test void inviteAndAccept() throws Exception {
    String ownerToken = loginAndGetToken("owner@test.com", "Secret123!");
    String guestToken = loginAndGetToken("dad@test.com", "Secret123!");
    UUID babyId = createBaby(ownerToken, "小明");

    // Owner creates invite
    String inviteToken = mvc.perform(post("/api/babies/" + babyId + "/invitations")
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isCreated())
        .andReturn().getResponse()...  // extract token

    // Guest accepts
    mvc.perform(post("/api/invitations/" + inviteToken + "/accept")
            .header("Authorization", "Bearer " + guestToken))
        .andExpect(status().isOk());

    // Guest can now list the baby
    mvc.perform(get("/api/babies")
            .header("Authorization", "Bearer " + guestToken))
        .andExpect(jsonPath("$[0].name").value("小明"));
}
```

**Step 2: Implement, run tests, commit**

```bash
./gradlew test --tests "*.InviteControllerTest"
git commit -m "feat: add family invite link system"
```

---

## Phase 4 — Backend: Daily Tracking

### Task 4-A: Tracking log endpoints

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/tracking/TrackingController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/tracking/TrackingService.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/domain/tracking/TrackingLog.java`
- Test: `backend/src/test/java/com/ccweb/babytracker/tracking/TrackingControllerTest.java`

**Endpoints:**
- `POST /api/babies/{id}/tracking` → body: `{ type, value?, note?, metadata? }`
- `GET  /api/babies/{id}/tracking?date=YYYY-MM-DD` → list logs for that day
- `GET  /api/babies/{id}/tracking/summary?date=YYYY-MM-DD` → aggregated counts/totals

**Step 1: Write failing tests**

```java
@Test void logDiaperAndRetrieve() throws Exception {
    // POST diaper wet — no body needed beyond type
    mvc.perform(post("/api/babies/" + babyId + "/tracking")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content("""{"type":"DIAPER_WET"}"""))
        .andExpect(status().isCreated());

    // GET logs for today
    mvc.perform(get("/api/babies/" + babyId + "/tracking?date=" + LocalDate.now())
            .header("Authorization", "Bearer " + token))
        .andExpect(jsonPath("$[0].type").value("DIAPER_WET"));
}

@Test void sleepStartAndEnd() throws Exception {
    mvc.perform(post("/api/babies/" + babyId + "/tracking")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content("""{"type":"SLEEP_START"}"""))
        .andExpect(status().isCreated());

    Thread.sleep(100);

    mvc.perform(post("/api/babies/" + babyId + "/tracking")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content("""{"type":"SLEEP_END"}"""))
        .andExpect(status().isCreated());

    // Summary should show sleep duration > 0
    mvc.perform(get("/api/babies/" + babyId + "/tracking/summary?date=" + LocalDate.now())
            .header("Authorization", "Bearer " + token))
        .andExpect(jsonPath("$.sleepMinutes").isNumber());
}
```

**Step 2: Implement, run tests, commit**

```bash
./gradlew test --tests "*.TrackingControllerTest"
git commit -m "feat: add daily tracking endpoints (feed/sleep/diaper)"
```

---

## Phase 5 — Backend: Growth Records

### Task 5-A: Growth record endpoints + percentile data

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/growth/GrowthController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/growth/GrowthService.java`
- Create: `backend/src/main/resources/data/who_percentiles.json`
- Test: `backend/src/test/java/com/ccweb/babytracker/growth/GrowthControllerTest.java`

**Endpoints:**
- `POST /api/babies/{id}/growth` → record weight/height/head
- `GET  /api/babies/{id}/growth` → list all records with computed percentile
- `GET  /api/babies/{id}/growth/chart` → returns records + WHO reference lines for chart

**Step 1: Write failing test**

```java
@Test void recordAndRetrieveGrowth() throws Exception {
    mvc.perform(post("/api/babies/" + babyId + "/growth")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content("""{"weightKg":5.5,"heightCm":58.0,"headCm":38.5,"measuredAt":"2026-01-15"}"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.weightPercentile").isNumber());
}
```

**Step 2: WHO percentile data file `who_percentiles.json`**

Store pre-computed P3/P15/P50/P85/P97 weight-for-age and height-for-age tables for boys and girls from WHO standards. Structure:
```json
{
  "weight_boys": [
    {"months": 0, "p3": 2.5, "p15": 2.9, "p50": 3.3, "p85": 3.9, "p97": 4.4},
    ...
  ],
  "weight_girls": [...],
  "height_boys": [...],
  "height_girls": [...]
}
```

**Step 3: Implement, run tests, commit**

```bash
./gradlew test --tests "*.GrowthControllerTest"
git commit -m "feat: add growth records with WHO percentile calculation"
```

---

## Phase 6 — Backend: Vaccination

### Task 6-A: Vaccination schedule + record endpoints

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/vaccine/VaccineController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/vaccine/VaccineService.java`
- Test: `backend/src/test/java/com/ccweb/babytracker/vaccine/VaccineControllerTest.java`

**Endpoints:**
- `GET  /api/babies/{id}/vaccines` → full schedule: each vaccine with status (DONE/UPCOMING/OVERDUE)
  - Status is computed from `vaccine_records` and baby's birth date
- `POST /api/babies/{id}/vaccines/{vaccineId}/record` → mark as administered
- `DELETE /api/babies/{id}/vaccines/{vaccineId}/record` → undo

**Step 1: Write failing test**

```java
@Test void vaccineScheduleHasCorrectStatus() throws Exception {
    // Baby born today — first vaccine should be UPCOMING/OVERDUE for age 0
    mvc.perform(get("/api/babies/" + babyId + "/vaccines")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("B型肝炎疫苗 第1劑"))
        .andExpect(jsonPath("$[0].status").value("UPCOMING"));
}
```

**Step 2: Implement, run tests, commit**

```bash
./gradlew test --tests "*.VaccineControllerTest"
git commit -m "feat: add vaccination schedule with status computation"
```

---

## Phase 7 — Backend: Complementary Food

### Task 7-A: Food catalogue + log endpoints

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/food/FoodController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/food/FoodService.java`
- Test: `backend/src/test/java/com/ccweb/babytracker/food/FoodControllerTest.java`

**Endpoints:**
- `POST /api/babies/{id}/foods` → add new food to catalogue (sets `first_introduced`)
- `GET  /api/babies/{id}/foods` → list all tried foods
- `POST /api/babies/{id}/foods/{foodId}/logs` → log today's eating
- `GET  /api/babies/{id}/foods/logs?date=YYYY-MM-DD` → today's food logs
- `PATCH /api/babies/{id}/foods/{foodId}` → update allergy status

**Business rule:** When a new food is created, if the baby is under 12 months old, response includes `firstTimeTip: true` → frontend shows the 3-day observation tip.

**Step 1: Write failing test**

```java
@Test void addNewFoodAndLog() throws Exception {
    // Add new food
    String foodId = mvc.perform(post("/api/babies/" + babyId + "/foods")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content("""{"name":"紅蘿蔔泥","category":"蔬菜"}"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.firstTimeTip").value(true))
        .andReturn()... // extract id

    // Log eating
    mvc.perform(post("/api/babies/" + babyId + "/foods/" + foodId + "/logs")
            .header("Authorization", "Bearer " + token)
            .contentType(APPLICATION_JSON)
            .content("""{"amount":"2湯匙","texture":"泥狀","reaction":"normal"}"""))
        .andExpect(status().isCreated());
}
```

**Step 2: Implement, run tests, commit**

```bash
./gradlew test --tests "*.FoodControllerTest"
git commit -m "feat: add complementary food catalogue and daily log endpoints"
```

---

## Phase 8 — Backend: Health Articles + Admin

### Task 8-A: Health articles endpoints

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/content/ArticleController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/content/ArticleService.java`
- Test: `backend/src/test/java/com/ccweb/babytracker/content/ArticleControllerTest.java`

**Endpoints:**
- `GET  /api/babies/{id}/articles` → articles matching baby's current age in months
- `GET  /api/articles/{articleId}` → single article
- `POST /api/admin/articles` → create (admin role only)
- `PUT  /api/admin/articles/{id}` → update
- `DELETE /api/admin/articles/{id}` → delete

Admin role: add `role` column to `users` table (V5 migration: `ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user'`).

**Step 1: Write migration V5**

```sql
ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user';
```

**Step 2: Write failing tests, implement, commit**

```bash
./gradlew test --tests "*.ArticleControllerTest"
git commit -m "feat: add age-filtered health articles and admin CRUD"
```

---

## Phase 9 — Backend: Notifications + Email

### Task 9-A: In-app notifications

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/notification/NotificationController.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/notification/NotificationService.java`

**Endpoints:**
- `GET  /api/notifications` → list unread + recent
- `POST /api/notifications/{id}/read` → mark read
- `POST /api/notifications/read-all` → mark all read

**Step 1: Write failing tests, implement, commit**

```bash
git commit -m "feat: add in-app notification endpoints"
```

---

### Task 9-B: Scheduled email reminders (Resend)

**Files:**
- Create: `backend/src/main/java/com/ccweb/babytracker/notification/EmailReminderScheduler.java`
- Create: `backend/src/main/java/com/ccweb/babytracker/notification/ResendEmailService.java`

**Logic:**
- Runs daily at 09:00 Taiwan time (`@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Taipei")`)
- For each baby, check if any vaccine is due in 7 days or 1 day
- If reminder not already sent, send via Resend and mark `email_reminders.sent_at`

**`ResendEmailService.java`** (simplified):
```java
@Service
public class ResendEmailService {
    private final ResendClient resend;

    public void sendVaccineReminder(String toEmail, String babyName, String vaccineName, LocalDate dueDate) {
        CreateEmailOptions options = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(toEmail)
            .subject("提醒：" + babyName + " 的 " + vaccineName + " 即將到期")
            .html("<p>親愛的家長您好，<b>" + babyName + "</b> 的 <b>" + vaccineName
                + "</b> 建議於 " + dueDate + " 前接種。</p>")
            .build();
        resend.emails().send(options);
    }
}
```

**Step 1: Write unit test with mocked Resend client, implement, commit**

```bash
git commit -m "feat: add scheduled vaccine email reminders via Resend"
```

---

## Phase 10 — Frontend

### Task 10-A: API client + auth store

**Files:**
- Create: `frontend/lib/api.ts`
- Create: `frontend/store/authStore.ts`

**`frontend/lib/api.ts`** — axios instance with JWT interceptor:
```ts
import axios from 'axios';

const api = axios.create({ baseURL: process.env.NEXT_PUBLIC_API_URL, withCredentials: true });

let accessToken = '';
export const setAccessToken = (t: string) => { accessToken = t; };

api.interceptors.request.use(cfg => {
  if (accessToken) cfg.headers.Authorization = `Bearer ${accessToken}`;
  return cfg;
});

api.interceptors.response.use(
  r => r,
  async err => {
    if (err.response?.status === 401) {
      const res = await axios.post(`${process.env.NEXT_PUBLIC_API_URL}/auth/refresh`, {}, { withCredentials: true });
      setAccessToken(res.data.accessToken);
      return api(err.config);
    }
    return Promise.reject(err);
  }
);

export default api;
```

**Step 1: Write Jest test**

```ts
// lib/__tests__/api.test.ts
import api from '../api';
test('api instance has correct baseURL', () => {
  expect(api.defaults.baseURL).toBe(process.env.NEXT_PUBLIC_API_URL);
});
```

**Step 2: Run, implement, commit**

```bash
cd frontend && npm test -- --testPathPattern=api
git commit -m "feat: add axios API client with JWT refresh interceptor"
```

---

### Task 10-B: Auth pages (register + login)

**Files:**
- Create: `frontend/app/(auth)/login/page.tsx`
- Create: `frontend/app/(auth)/register/page.tsx`
- Create: `frontend/components/auth/LoginForm.tsx`
- Create: `frontend/components/auth/RegisterForm.tsx`

**Key UX:** Large inputs, mobile-first, Tailwind. Use `react-hook-form` + `zod` for validation.

```tsx
// components/auth/LoginForm.tsx (simplified)
export function LoginForm() {
  const { register, handleSubmit } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) });
  const onSubmit = async (data: LoginInput) => {
    const res = await api.post('/auth/login', data);
    setAccessToken(res.data.accessToken);
    router.push('/dashboard');
  };
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <input {...register('email')} type="email" placeholder="電子郵件" className="input" />
      <input {...register('password')} type="password" placeholder="密碼" className="input" />
      <button type="submit" className="btn-primary">登入</button>
    </form>
  );
}
```

**Step 1: Write React Testing Library test, implement, commit**

```bash
npm test -- --testPathPattern=LoginForm
git commit -m "feat: add login and register pages"
```

---

### Task 10-C: Dashboard page

**Files:**
- Create: `frontend/app/(app)/dashboard/page.tsx`
- Create: `frontend/components/dashboard/BabySummary.tsx`
- Create: `frontend/components/dashboard/QuickLog.tsx`
- Create: `frontend/components/dashboard/UpcomingVaccine.tsx`

**`QuickLog` component** — the one-tap bottom bar:
```tsx
const QUICK_ACTIONS = [
  { label: '餵奶', emoji: '🍼', type: 'FEED', needsInput: true },
  { label: '睡覺', emoji: '😴', type: 'SLEEP_START', needsInput: false },
  { label: '尿布', emoji: '💧', type: 'DIAPER_WET', needsInput: false },
  { label: '便便', emoji: '💩', type: 'DIAPER_DIRTY', needsInput: false },
  { label: '副食品', emoji: '🥣', type: 'FOOD', needsInput: true },
];
```

Actions without `needsInput` call `POST /api/babies/{id}/tracking` immediately on tap. `FEED` and `FOOD` open a bottom sheet.

**Step 1: Write component test, implement, commit**

```bash
git commit -m "feat: add dashboard with quick-log bottom bar"
```

---

### Task 10-D: Growth chart page

**Files:**
- Create: `frontend/app/(app)/babies/[id]/growth/page.tsx`
- Create: `frontend/components/growth/GrowthChart.tsx`

Use **Recharts** `LineChart` with:
- Baby's weight/height data points
- P3, P50, P97 reference lines from API response
- Toggle between weight / height / head circumference

**Step 1: Write test for chart data transformation, implement, commit**

```bash
git commit -m "feat: add growth chart with WHO percentile reference lines"
```

---

### Task 10-E: Vaccination page

**Files:**
- Create: `frontend/app/(app)/babies/[id]/vaccines/page.tsx`
- Create: `frontend/components/vaccines/VaccineTimeline.tsx`

Timeline grouped by status: OVERDUE (red), UPCOMING (yellow), DONE (green). Each item shows vaccine name, recommended age, and link to CDC source.

**Step 1: Implement, commit**

```bash
git commit -m "feat: add vaccination timeline page"
```

---

### Task 10-F: Complementary food page

**Files:**
- Create: `frontend/app/(app)/babies/[id]/food/page.tsx`
- Create: `frontend/components/food/FoodList.tsx`
- Create: `frontend/components/food/AddFoodSheet.tsx`

`AddFoodSheet` — bottom sheet with:
1. Search/select from existing foods OR type new name
2. Category selector (4 buttons)
3. Amount text input
4. Texture selector (3 buttons: 泥狀/碎末/小塊)
5. Reaction selector (4 buttons: 正常/紅疹/嘔吐/其他)
6. Submit

**Step 1: Implement, commit**

```bash
git commit -m "feat: add complementary food diary with allergy tracking"
```

---

### Task 10-G: Health articles page

**Files:**
- Create: `frontend/app/(app)/babies/[id]/articles/page.tsx`

Fetches age-appropriate articles from API, displays title + summary, links to full content. Each article shows a "資料來源" link to the 國健署 source URL.

**Step 1: Implement, commit**

```bash
git commit -m "feat: add age-filtered health education articles"
```

---

### Task 10-H: Family invite flow

**Files:**
- Create: `frontend/app/(app)/babies/[id]/settings/page.tsx`
- Create: `frontend/app/invite/[token]/page.tsx`

Settings page shows "邀請家人" button → generates invite link → copy to clipboard.
`/invite/[token]` page: if logged in → accept automatically; if not logged in → redirect to register then accept.

**Step 1: Implement, commit**

```bash
git commit -m "feat: add family invite generation and acceptance flow"
```

---

## Phase 11 — E2E Smoke Test + Deployment

### Task 11-A: Environment variables checklist

**Backend (Railway env vars):**
```
JWT_SECRET=<random 64-char string>
RESEND_API_KEY=<from resend.com>
SPRING_DATASOURCE_URL=jdbc:postgresql://<railway-host>/babytracker
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
```

**Frontend (Vercel env vars):**
```
NEXT_PUBLIC_API_URL=https://<railway-app>.railway.app/api
```

### Task 11-B: Manual E2E smoke test checklist

Run through these in a browser before calling Phase 1 done:

- [ ] Register new account
- [ ] Login and see dashboard
- [ ] Create a baby profile
- [ ] Tap 尿布 → log appears in today's summary
- [ ] Tap 睡覺 → start timer; tap again → duration shown
- [ ] Add growth record → see point on chart
- [ ] View vaccination schedule → B型肝炎 shows as UPCOMING
- [ ] Add a new food (紅蘿蔔泥) → see 3-day tip
- [ ] Log food reaction (正常) → appears in today's log
- [ ] Invite link → open in incognito → register + accept → see same baby

---

## Appendix: Running Locally

```bash
# 1. Start database
docker compose up -d

# 2. Start backend (from /backend)
./gradlew bootRun

# 3. Start frontend (from /frontend)
npm run dev

# 4. Run backend tests
./gradlew test

# 5. Run frontend tests
npm test
```
