# Smart Freelance Platform - SonarQube & Code Coverage Guide

Complete guide to running SonarQube analysis and generating code coverage reports for all microservices.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Start SonarQube with Docker](#2-start-sonarqube-with-docker)
3. [First-Time SonarQube Setup](#3-first-time-sonarqube-setup)
4. [Generate Coverage & Run Analysis](#4-generate-coverage--run-analysis)
5. [Run All Microservices at Once](#5-run-all-microservices-at-once)
6. [Service Reference Table](#6-service-reference-table)
7. [Reading the Reports](#7-reading-the-reports)
8. [Useful Commands](#8-useful-commands)
9. [Troubleshooting](#9-troubleshooting)
10. [Stop / Remove SonarQube](#10-stop--remove-sonarqube)

---

## 1. Prerequisites

- **Java 17**
- **Maven 3.9+**
- **Docker** (to run SonarQube)

Every Java microservice already includes in its `pom.xml`:
- **JaCoCo Maven Plugin** (`0.8.13`) -- instruments tests and generates coverage XML/HTML
- **SonarQube Scanner Plugin** (`5.1.0.4751`) -- sends source + coverage to SonarQube

---

## 2. Start SonarQube with Docker

### Quick start (ephemeral)

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:lts-community
```

### Persistent (data survives restarts)

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  -v sonarqube_data:/opt/sonarqube/data \
  -v sonarqube_logs:/opt/sonarqube/logs \
  -v sonarqube_extensions:/opt/sonarqube/extensions \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:lts-community
```

Wait ~1-2 minutes for startup, then open **http://localhost:9000**.

---

## 3. First-Time SonarQube Setup

1. Open http://localhost:9000
2. Log in: **admin** / **admin**
3. Change the default password when prompted
4. Navigate to **My Account > Security > Generate Token**
5. Create a token (type: **Project Analysis Token**), copy it
6. Export it for convenience:

```bash
export SONAR_TOKEN=your_token_here
```

---

## 4. Generate Coverage & Run Analysis

### Single microservice

```bash
cd backEnd/Microservices/<service-name>

# Step 1: Run tests + generate JaCoCo coverage report
mvn clean test jacoco:report

# Step 2: Push results to SonarQube
mvn sonar:sonar \
  -Dsonar.projectKey=<service-name> \
  -Dsonar.projectName="<service-name>" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=$SONAR_TOKEN
```

Or combined in one command:

```bash
mvn clean test jacoco:report sonar:sonar \
  -Dsonar.projectKey=<service-name> \
  -Dsonar.projectName="<service-name>" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=$SONAR_TOKEN
```

### Example for FreelanciaJob

```bash
cd backEnd/Microservices/FreelanciaJob

mvn clean test jacoco:report sonar:sonar \
  -Dsonar.projectKey=FreelanciaJob \
  -Dsonar.projectName="FreelanciaJob" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=$SONAR_TOKEN
```

Then open: http://localhost:9000/dashboard?id=FreelanciaJob

---

## 5. Run All Microservices at Once

### Bash script -- analyze every service

Save and run from the project root:

```bash
#!/bin/bash
# run-sonar-all.sh -- Run tests + SonarQube analysis for all microservices
# Usage: SONAR_TOKEN=xxx bash run-sonar-all.sh

SONAR_URL="http://localhost:9000"
TOKEN="${SONAR_TOKEN:?Set SONAR_TOKEN before running}"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# Java microservices under backEnd/Microservices/
SERVICES=(
  Chat
  Contract
  FreelanciaJob
  Meeting
  Notification
  Offer
  Portfolio
  Project
  Subcontracting
  gamification
  planning
  review
  task
  ticket-service
  user
)

# Infrastructure services
INFRA=(
  apiGateway
  Eureka
  ConfigServer
  KeyCloak
)

echo "=== Analyzing Microservices ==="
for svc in "${SERVICES[@]}"; do
  DIR="$BASE_DIR/backEnd/Microservices/$svc"
  if [ ! -f "$DIR/pom.xml" ]; then
    echo "SKIP $svc (no pom.xml)"
    continue
  fi
  echo ""
  echo "--- $svc ---"
  (cd "$DIR" && mvn clean test jacoco:report sonar:sonar \
    -Dsonar.projectKey="$svc" \
    -Dsonar.projectName="$svc" \
    -Dsonar.host.url="$SONAR_URL" \
    -Dsonar.token="$TOKEN" \
    -q) && echo "OK $svc" || echo "FAIL $svc"
done

echo ""
echo "=== Analyzing Infrastructure Services ==="
for svc in "${INFRA[@]}"; do
  DIR="$BASE_DIR/backEnd/$svc"
  if [ ! -f "$DIR/pom.xml" ]; then
    echo "SKIP $svc (no pom.xml)"
    continue
  fi
  echo ""
  echo "--- $svc ---"
  (cd "$DIR" && mvn clean test jacoco:report sonar:sonar \
    -Dsonar.projectKey="$svc" \
    -Dsonar.projectName="$svc" \
    -Dsonar.host.url="$SONAR_URL" \
    -Dsonar.token="$TOKEN" \
    -q) && echo "OK $svc" || echo "FAIL $svc"
done

echo ""
echo "=== Done. Open $SONAR_URL to see all projects ==="
```

**Run it:**

```bash
chmod +x run-sonar-all.sh
SONAR_TOKEN=your_token_here bash run-sonar-all.sh
```

### Coverage-only (no SonarQube, just JaCoCo HTML reports)

```bash
for dir in backEnd/Microservices/*/; do
  if [ -f "$dir/pom.xml" ]; then
    echo "--- $(basename $dir) ---"
    (cd "$dir" && mvn clean test jacoco:report -q) && echo "OK" || echo "FAIL"
  fi
done
```

Then open any service's report:

```bash
open backEnd/Microservices/FreelanciaJob/target/site/jacoco/index.html
open backEnd/Microservices/Offer/target/site/jacoco/index.html
# etc.
```

---

## 6. Service Reference Table

| Service | Port | Directory | SonarQube Key |
|---------|------|-----------|---------------|
| **Eureka** | 8420 | `backEnd/Eureka` | `Eureka` |
| **Config Server** | 8888 | `backEnd/ConfigServer` | `ConfigServer` |
| **API Gateway** | 8078 | `backEnd/apiGateway` | `apiGateway` |
| **Keycloak Auth** | 8079 | `backEnd/KeyCloak` | `KeyCloak` |
| **user** | 8090 | `backEnd/Microservices/user` | `user` |
| **Project** | 8084 | `backEnd/Microservices/Project` | `Project` |
| **Offer** | 8082 | `backEnd/Microservices/Offer` | `Offer` |
| **Contract** | 8083 | `backEnd/Microservices/Contract` | `Contract` |
| **FreelanciaJob** | 8097 | `backEnd/Microservices/FreelanciaJob` | `FreelanciaJob` |
| **Portfolio** | 8086 | `backEnd/Microservices/Portfolio` | `Portfolio` |
| **review** | 8085 | `backEnd/Microservices/review` | `review` |
| **planning** | 8081 | `backEnd/Microservices/planning` | `planning` |
| **task** | 8091 | `backEnd/Microservices/task` | `task` |
| **Notification** | 8087 | `backEnd/Microservices/Notification` | `Notification` |
| **gamification** | 8088 | `backEnd/Microservices/gamification` | `gamification` |
| **Meeting** | 8101 | `backEnd/Microservices/Meeting` | `Meeting` |
| **Chat** | 8093 | `backEnd/Microservices/Chat` | `Chat` |
| **ticket-service** | 8096 | `backEnd/Microservices/ticket-service` | `ticket-service` |
| **Subcontracting** | 8110 | `backEnd/Microservices/Subcontracting` | `Subcontracting` |

> **Note:** `AImodel` (Python/Spring AI) and `aimodel-node` (Node.js) are not Java Maven projects and are excluded from SonarQube Maven analysis.

---

## 7. Reading the Reports

### JaCoCo HTML Report (local, no SonarQube needed)

After running `mvn clean test jacoco:report`, open:

```
target/site/jacoco/index.html
```

The report shows:
- **Instructions** -- bytecode instruction coverage (most accurate)
- **Branches** -- if/else and switch branch coverage
- **Lines** -- source line coverage
- **Methods** -- method-level coverage
- **Classes** -- class-level coverage

Color coding: green = covered, yellow = partially covered, red = not covered.

### JaCoCo XML Report (consumed by SonarQube)

Located at:
```
target/site/jacoco/jacoco.xml
```

Quick CLI coverage check after generating the report:

```bash
python3 -c "
import xml.etree.ElementTree as ET, sys
tree = ET.parse('target/site/jacoco/jacoco.xml')
for c in tree.getroot().findall('counter'):
    t = c.get('type')
    m, cov = int(c.get('missed')), int(c.get('covered'))
    total = m + cov
    print(f'{t}: {cov}/{total} = {cov*100.0/total:.1f}%' if total else f'{t}: 0/0')
"
```

### SonarQube Dashboard

Once analysis is pushed, each project appears at:

```
http://localhost:9000/dashboard?id=<projectKey>
```

Key tabs:
- **Overview** -- quality gate status, new code metrics
- **Measures > Coverage** -- line and branch coverage with drill-down
- **Issues** -- bugs, vulnerabilities, code smells
- **Code** -- browse source with inline coverage highlighting

---

## 8. Useful Commands

```bash
# --- Test Commands ---

# Run all tests for a microservice
cd backEnd/Microservices/<service> && mvn test

# Run a single test class
mvn test -Dtest=JobServiceTest

# Run tests matching a pattern
mvn test -Dtest="*ServiceTest"

# Skip specific test classes
mvn test -Dtest="!FreelanciaJobIntegrationTest"

# Run tests without compiling (if already compiled)
mvn surefire:test

# --- Coverage Commands ---

# Generate JaCoCo report only (no SonarQube)
mvn clean test jacoco:report

# Open HTML report (macOS)
open target/site/jacoco/index.html

# Check coverage thresholds (services with jacoco:check configured)
mvn clean verify

# --- SonarQube Commands ---

# Full pipeline: test + coverage + sonar
mvn clean test jacoco:report sonar:sonar \
  -Dsonar.projectKey=MyService \
  -Dsonar.token=$SONAR_TOKEN

# Sonar analysis only (reuse existing coverage report)
mvn sonar:sonar \
  -Dsonar.projectKey=MyService \
  -Dsonar.token=$SONAR_TOKEN

# Skip tests during build but still analyze
mvn clean package -DskipTests jacoco:report sonar:sonar \
  -Dsonar.projectKey=MyService \
  -Dsonar.token=$SONAR_TOKEN
```

---

## 9. Troubleshooting

### SonarQube won't start

```bash
# Check logs
docker logs sonarqube

# Common fix: increase vm.max_map_count (Linux)
sudo sysctl -w vm.max_map_count=262144

# macOS: not needed with SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
```

### "Not authorized" error during analysis

- Verify your token: **My Account > Security** in SonarQube UI
- Make sure the token is passed correctly:
  ```bash
  mvn sonar:sonar -Dsonar.token=squ_xxxxx
  ```

### Coverage shows 0% in SonarQube but JaCoCo report has data

- Make sure `jacoco:report` runs **before** `sonar:sonar`
- Verify the XML report exists:
  ```bash
  ls target/site/jacoco/jacoco.xml
  ```
- Check the Sonar property points to it:
  ```xml
  <sonar.coverage.jacoco.xmlReportPaths>
    ${project.build.directory}/site/jacoco/jacoco.xml
  </sonar.coverage.jacoco.xmlReportPaths>
  ```

### Tests fail but you still want Sonar analysis

```bash
# Generate coverage for passing tests, ignore failures
mvn test jacoco:report -Dmaven.test.failure.ignore=true

# Then run Sonar
mvn sonar:sonar -Dsonar.projectKey=MyService -Dsonar.token=$SONAR_TOKEN
```

### H2 / test profile issues

All microservices use `application-test.properties` with H2 in-memory database. If tests fail with database errors:
- Ensure `spring.jpa.hibernate.ddl-auto=create-drop` is set in test properties
- Ensure `@ActiveProfiles("test")` is on integration test classes
- Ensure `h2` dependency exists in pom.xml with `<scope>test</scope>`

---

## 10. Stop / Remove SonarQube

```bash
# Stop
docker stop sonarqube

# Start again (data preserved if using volumes)
docker start sonarqube

# Remove completely
docker stop sonarqube && docker rm sonarqube

# Remove volumes too
docker volume rm sonarqube_data sonarqube_logs sonarqube_extensions
```
