# Testing Plan - Smart Freelance Platform

## Current State

| Service | Source Files | Test Files | Test Props | Status |
|---------|:-----------:|:----------:|:----------:|--------|
| **FreelanciaJob** | 66 | 17 | Y | **84% coverage** |
| **planning** | 62 | 23 | N | Has tests, needs test properties |
| **task** | 80 | 23 | N | Has tests, needs test properties |
| **gamification** | 52 | 18 | N | Has tests, needs test properties |
| **Project** | 29 | 7 | N | Has tests, needs test properties |
| **Meeting** | 37 | 6 | Y | Has tests, ready |
| **Offer** | 60 | 5 | N | Has tests, needs test properties |
| **review** | 26 | 4 | N | Has tests, needs test properties |
| **ticket-service** | 36 | 4 | N | Has tests, needs test properties |
| **Subcontracting** | 135 | 2 | N | Minimal tests, biggest service |
| **Chat** | 21 | 1 | Y | Context-load only |
| **user** | 19 | 1 | N | Context-load only |
| **Notification** | 11 | 1 | N | Context-load only |
| **Contract** | 22 | 0 | N | No tests at all |
| **Portfolio** | 40 | 0 | N | No tests at all |
| **KeyCloak** | 17 | 1 | N | Context-load only |
| **apiGateway** | 5 | 1 | N | Context-load only |
| **Eureka** | 1 | 1 | N | Context-load only |
| **ConfigServer** | 1 | 0 | N | No tests at all |

---

## Plan Overview

### 4 Phases

```
Phase 1  Foundations         Make every service testable (test properties, H2, mvnw)
Phase 2  High-Impact First   Write tests for the 6 biggest untested/undertested services
Phase 3  Full Coverage       Cover the remaining services
Phase 4  CI Gate             Enforce coverage thresholds in Jenkins
```

### Priority order (Phase 2)

Services ranked by **source file count x test gap** -- the ones where tests add the most value:

1. **Subcontracting** (135 sources, 2 tests) -- largest service, barely tested
2. **Offer** (60 sources, 5 tests) -- large service, low test ratio
3. **Portfolio** (40 sources, 0 tests) -- medium service, zero tests
4. **ticket-service** (36 sources, 4 tests) -- medium service, low tests
5. **Contract** (22 sources, 0 tests) -- medium service, zero tests
6. **user** (19 sources, 1 test) -- core service, no real tests

---

## Phase 1 -- Foundations (Day 1)

**Goal:** Every Java service can run `mvn test` without errors.

### 1.1 Add `application-test.properties` to every service missing it

Template (adapt per service):

```properties
# ── H2 in-memory database ──
spring.datasource.url=jdbc:h2:mem:<service>test;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# ── Disable Cloud Config / Eureka ──
spring.config.import=optional:
spring.cloud.discovery.enabled=false
eureka.client.enabled=false
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

# ── Disable Feign circuit breaker ──
spring.cloud.openfeign.circuitbreaker.enabled=false

# ── Disable mail ──
spring.mail.host=
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
```

**Services that need this** (12):
Contract, gamification, Notification, Offer, planning, Portfolio, Project, review, Subcontracting, task, ticket-service, user

### 1.2 Add Maven wrapper where missing

```bash
cd backEnd/Microservices/<service>
mvn wrapper:wrapper
```

**Services that need this** (6): Chat, Contract, Notification, Portfolio, Subcontracting, ticket-service

### 1.3 Create `src/test/java` directory where missing

```bash
mkdir -p backEnd/Microservices/Contract/src/test/java
mkdir -p backEnd/Microservices/Portfolio/src/test/java
mkdir -p backEnd/ConfigServer/src/test/java
```

### 1.4 Verify with a dry run

```bash
for svc in backEnd/Microservices/*/; do
  echo "--- $(basename $svc) ---"
  (cd "$svc" && mvn test -q 2>&1 | tail -1)
done
```

---

## Phase 2 -- High-Impact Services (Days 2-5)

**Goal:** Get 6 priority services to 80%+ coverage.

### What to test per service (in order)

For each service, follow this layer order:

```
1. Service layer    (business logic -- highest value)
2. Controller layer (HTTP endpoints -- verifies routing/status codes)
3. Repository layer (custom @Query methods -- only if they exist)
4. Mapper layer     (DTO mapping -- quick wins)
5. Event/Listener   (if applicable)
```

### 2.1 Subcontracting (135 source files)

This is the largest service. Focus on:

| Layer | What to test | Test type |
|-------|-------------|-----------|
| Services | All service implementations (CRUD, business rules) | Unit (Mockito) |
| Controllers | All REST endpoints (status codes, request/response) | Unit (mock service) |
| AI/Analytics | Fallback behavior when AI unavailable | Unit (mock RestTemplate) |
| Repositories | Custom queries (if any) | @DataJpaTest + H2 |

Expected: ~15-20 test files to reach 80%.

### 2.2 Offer (60 source files, 5 tests exist)

| Layer | What to test |
|-------|-------------|
| Services | OfferService, SmartMatchingService, DashboardService |
| Controllers | OfferController endpoints |
| Chat/Translation | ChatAssistantService (mock AI), TranslationService |

Expected: ~8-10 new test files.

### 2.3 Portfolio (40 source files, 0 tests)

| Layer | What to test |
|-------|-------------|
| Services | SkillService, ExperienceService, PortfolioService |
| Controllers | All REST endpoints |
| Repositories | Custom queries |

Expected: ~6-8 test files.

### 2.4 ticket-service (36 source files, 4 tests exist)

| Layer | What to test |
|-------|-------------|
| Services | TicketService, CommentService |
| Controllers | TicketController, CommentController |

Expected: ~4-6 new test files.

### 2.5 Contract (22 source files, 0 tests)

| Layer | What to test |
|-------|-------------|
| Services | ContractService (CRUD, status transitions, PDF generation) |
| Controllers | ContractController |
| Repository | Custom queries |

Expected: ~4-5 test files.

### 2.6 user (19 source files, 1 test)

| Layer | What to test |
|-------|-------------|
| Services | UserService (CRUD, role assignment, Keycloak integration) |
| Controllers | UserController |

Expected: ~3-4 test files.

---

## Phase 3 -- Remaining Services (Days 6-8)

**Goal:** Every service at 80%+ coverage.

### Services with existing tests that need expansion

| Service | Current tests | Needs |
|---------|:------------:|-------|
| **planning** | 23 files | Check current coverage, fill gaps |
| **task** | 23 files | Check current coverage, fill gaps |
| **gamification** | 18 files | Check current coverage, fill gaps |
| **Project** | 7 files | Add controller tests, edge cases |
| **Meeting** | 6 files | Add service layer tests |
| **review** | 4 files | Add controller + more service tests |

### Services that only need basic tests

| Service | What to add |
|---------|-------------|
| **Chat** | Service unit tests (ChatService, WebSocket handler) |
| **Notification** | Service unit tests (Firebase notification logic) |
| **KeyCloak** | Service tests (Keycloak admin operations) |
| **apiGateway** | Filter tests (GatewayOnlyFilter, routing config) |
| **Eureka** | Context-load is sufficient (1 source file) |
| **ConfigServer** | Context-load is sufficient (1 source file) |

---

## Phase 4 -- CI Enforcement (Day 9)

**Goal:** Jenkins fails the build if coverage drops below 80%.

### 4.1 Add JaCoCo check rule to each pom.xml

```xml
<execution>
    <id>check</id>
    <phase>verify</phase>
    <goals><goal>check</goal></goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### 4.2 SonarQube Quality Gate

In SonarQube (http://localhost:9000):

1. Go to **Quality Gates > Create**
2. Name: `Smart Freelance Gate`
3. Add conditions:
   - Coverage on New Code >= 80%
   - Duplicated Lines on New Code < 3%
   - Maintainability Rating on New Code = A
4. Set as **Default**

Now every Jenkins pipeline that runs SonarQube will enforce these rules.

---

## Test Writing Patterns

### Unit test (Service layer) -- use for all services

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MyService – Unit Tests")
class MyServiceTest {

    @Mock private MyRepository repository;
    @Mock private SomeClient feignClient;
    @InjectMocks private MyServiceImpl service;

    @Test
    @DisplayName("should create entity and return DTO")
    void create_validRequest_returnsDto() {
        // Arrange
        when(repository.save(any())).thenReturn(buildEntity());

        // Act
        var result = service.create(buildRequest());

        // Assert
        assertThat(result).isNotNull();
        verify(repository).save(any());
    }
}
```

### Unit test (Controller layer)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MyController – Unit Tests")
class MyControllerTest {

    @Mock private MyService service;
    @InjectMocks private MyController controller;

    @Test
    @DisplayName("POST /items should return 201")
    void create_returns201() {
        when(service.create(any())).thenReturn(buildResponse());

        ResponseEntity<?> result = controller.create(new Request());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

### Repository test (custom queries)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class MyRepositoryTest {

    @Autowired private MyRepository repository;

    @Test
    void findByStatus_returnsFiltered() {
        repository.save(buildEntity(Status.ACTIVE));
        repository.save(buildEntity(Status.CLOSED));

        List<MyEntity> result = repository.findByStatus(Status.ACTIVE);

        assertThat(result).hasSize(1);
    }
}
```

### What NOT to test

- Lombok-generated getters/setters/builders
- Spring configuration classes (unless they have logic)
- DTOs with no methods
- Enums with no methods
- `@PostConstruct` that loads external resources (mock the dependency instead)

---

## Quick Commands

```bash
# Run tests for one service
cd backEnd/Microservices/<service> && mvn test

# Run tests + coverage report
cd backEnd/Microservices/<service> && mvn test jacoco:report
open target/site/jacoco/index.html

# Run a single test class
mvn test -Dtest=MyServiceTest

# Run all tests ignoring failures (for CI audit)
mvn test -Dmaven.test.failure.ignore=true

# Check coverage from CLI after jacoco:report
python3 -c "
import xml.etree.ElementTree as ET
tree = ET.parse('target/site/jacoco/jacoco.xml')
for c in tree.getroot().findall('counter'):
    t, m, cov = c.get('type'), int(c.get('missed')), int(c.get('covered'))
    total = m + cov
    print(f'{t}: {cov}/{total} = {cov*100.0/total:.1f}%' if total else f'{t}: 0/0')
"

# Run coverage for ALL services at once
bash run-sonar-all.sh --coverage-only
```

---

## Timeline Summary

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| **Phase 1** Foundations | Day 1 | Every service runs `mvn test` cleanly |
| **Phase 2** High-Impact | Days 2-5 | 6 priority services at 80%+ |
| **Phase 3** Full Coverage | Days 6-8 | All 19 Java services at 80%+ |
| **Phase 4** CI Gate | Day 9 | Jenkins enforces 80% on every build |

**End state:** 80%+ line coverage across all services, enforced by Jenkins + SonarQube quality gate on every push.
