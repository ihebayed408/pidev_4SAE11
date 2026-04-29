# Jenkins CI/CD Setup - Smart Freelance Platform

Complete guide to install Jenkins, configure it, and run CI/CD pipelines for all 21 services.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Start Jenkins with Docker](#2-start-jenkins-with-docker)
3. [First-Time Jenkins Setup](#3-first-time-jenkins-setup)
4. [Install Required Plugins](#4-install-required-plugins)
5. [Configure Credentials](#5-configure-credentials)
6. [Configure SonarQube Server](#6-configure-sonarqube-server)
7. [Create the Seed Job (Job DSL)](#7-create-the-seed-job-job-dsl)
8. [Pipeline Architecture](#8-pipeline-architecture)
9. [Run Pipelines](#9-run-pipelines)
10. [Service Reference](#10-service-reference)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. Architecture Overview

```
Jenkins (8080)
  |
  |-- orchestration/full-stack-main     (dependency-aware build of everything)
  |
  |-- services/eureka                   (infrastructure - builds first)
  |-- services/config-server
  |-- services/keycloak-auth
  |
  |-- services/user                     (core - builds in parallel)
  |-- services/project
  |-- services/notification
  |-- services/contract
  |-- services/portfolio
  |-- services/chat
  |-- services/meeting
  |-- services/freelancia-job
  |-- services/aimodel
  |
  |-- services/planning                 (dependent - sequential/parallel)
  |-- services/task
  |-- services/review
  |-- services/offer
  |-- services/gamification
  |-- services/ticket-service
  |-- services/subcontracting
  |
  |-- services/api-gateway              (gateway - after all services)
  |-- services/frontend                 (frontend - last)
```

Each pipeline stage: **Checkout -> Build -> Test -> SonarQube -> Quality Gate -> Docker Build -> Docker Push**

---

## 2. Start Jenkins with Docker

### Quick start

```bash
docker run -d --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

### With Docker-in-Docker support (recommended)

The pipelines build Docker images, so Jenkins needs access to Docker:

```bash
# Create a custom Jenkins image with Docker CLI
cat > /tmp/Dockerfile.jenkins <<'EOF'
FROM jenkins/jenkins:lts
USER root
RUN apt-get update && apt-get install -y docker.io && rm -rf /var/lib/apt/lists/*
RUN usermod -aG docker jenkins
USER jenkins
EOF

docker build -t jenkins-docker -f /tmp/Dockerfile.jenkins /tmp

docker run -d --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-docker
```

### With SonarQube side-by-side

```bash
# Jenkins
docker run -d --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins-docker

# SonarQube
docker run -d --name sonarqube \
  -p 9000:9000 \
  -v sonarqube_data:/opt/sonarqube/data \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  sonarqube:lts-community
```

Wait ~2 minutes, then open:
- **Jenkins**: http://localhost:8080
- **SonarQube**: http://localhost:9000

---

## 3. First-Time Jenkins Setup

### A. Unlock Jenkins

```bash
# Get the initial admin password
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Paste it at http://localhost:8080 to unlock.

### B. Install suggested plugins

Select **"Install suggested plugins"** when prompted. This installs Git, Pipeline, etc.

### C. Create admin user

Create your admin account (or skip and use `admin` with the initial password).

### D. Set Jenkins URL

Set to `http://localhost:8080/` (or your server IP).

---

## 4. Install Required Plugins

Go to **Manage Jenkins > Plugins > Available plugins** and install:

| Plugin | Purpose |
|--------|---------|
| **Pipeline** | Core pipeline support (usually pre-installed) |
| **Git** | SCM checkout (usually pre-installed) |
| **Docker Pipeline** | `docker.build()`, `docker.withRegistry()` |
| **Docker Commons** | Docker credential binding |
| **Job DSL** | Auto-generate jobs from `jobs.groovy` |
| **SonarQube Scanner** | `withSonarQubeEnv()`, `waitForQualityGate()` |
| **JUnit** | Test result publishing |
| **Credentials Binding** | `withCredentials()` |
| **Timestamper** | `timestamps()` in pipelines |
| **NodeJS** | (Optional) For frontend/aimodel-node builds on agent |
| **Pipeline: Stage View** | Visual pipeline progress |

After installing, **restart Jenkins** (Manage Jenkins > Restart).

---

## 5. Configure Credentials

Go to **Manage Jenkins > Credentials > System > Global credentials > Add Credentials**.

### A. GitHub credentials

| Field | Value |
|-------|-------|
| Kind | Username with password |
| ID | `GithubCredentials` |
| Username | Your GitHub username |
| Password | GitHub Personal Access Token (with `repo` scope) |

### B. Docker Hub credentials

| Field | Value |
|-------|-------|
| Kind | Username with password |
| ID | `DockerHubCrendentials` |
| Username | `ridhaferchichi` (your Docker Hub username) |
| Password | Docker Hub password or access token |

### C. SonarQube token

First, generate a token in SonarQube:
1. Open http://localhost:9000, login (`admin`/`admin`)
2. Go to **My Account > Security > Generate Token**
3. Copy the token

Then in Jenkins:

| Field | Value |
|-------|-------|
| Kind | Secret text |
| ID | `SonarQubeToken` |
| Secret | The token from SonarQube |

---

## 6. Configure SonarQube Server

Go to **Manage Jenkins > System > SonarQube servers**:

| Field | Value |
|-------|-------|
| Name | `SonarQube` |
| Server URL | `http://localhost:9000` (or `http://host.docker.internal:9000` from Docker) |
| Server authentication token | Select `SonarQubeToken` from dropdown |

> **Docker networking note**: If Jenkins runs in Docker and SonarQube also runs in Docker, use `http://host.docker.internal:9000` or put both on the same Docker network.

---

## 7. Create the Seed Job (Job DSL)

The seed job reads `Jenkins/jobs.groovy` and auto-generates all 21 pipeline jobs + the orchestration job.

### Step by step

1. **New Item** > name: `seed-job` > type: **Freestyle project** > OK

2. **Source Code Management** > Git:
   - Repository URL: `https://github.com/RidhaFerchichi404/pidev_4SAE11.git`
   - Credentials: `GithubCredentials`
   - Branch: `*/main`

3. **Build Steps** > **Add build step** > **Process Job DSLs**:
   - Look on Filesystem: `Jenkins/jobs.groovy`

4. **Save** and **Build Now**

After the seed job runs, you'll see two new folders in Jenkins:
- `services/` -- 21 pipeline jobs (one per service)
- `orchestration/` -- `full-stack-main` orchestration pipeline

### Re-run the seed job

Whenever `jobs.groovy` changes, re-run the seed job to update/create jobs.

---

## 8. Pipeline Architecture

### Files overview

```
Jenkins/
  Jenkinsfile                 # Orchestration pipeline (dependency-aware build order)
  jobs.groovy                 # Job DSL: auto-generates all 21 service jobs + orchestration
  microservicePipeline.groovy # Shared library: reusable pipeline logic (optional)

Jenkinsfile                   # Root: Docker Compose full-stack build (alternative approach)

backEnd/Eureka/Jenkinsfile              # Infrastructure
backEnd/ConfigServer/Jenkinsfile
backEnd/apiGateway/Jenkinsfile
backEnd/KeyCloak/Jenkinsfile

backEnd/Microservices/*/Jenkinsfile     # Each microservice has its own Jenkinsfile
frontend/smart-freelance-app/Jenkinsfile
```

### Per-service pipeline stages

```
Checkout
  |
Build & Test  (mvn clean verify / npm ci && npm test)
  |
SonarQube Analysis  (mvn sonar:sonar / npx sonar-scanner)
  |
Quality Gate  (waitForQualityGate, non-blocking)
  |
Docker Build  (docker build -t image:tag .)
  |
Docker Push  (push to Docker Hub with tag + latest)
  |
[Optional] Prometheus metrics push
```

### Orchestration build order

The `orchestration/full-stack-main` job runs all services respecting dependencies:

```
1. [Sequential]  Eureka -> Config Server -> Keycloak Auth
2. [Parallel]    User, Project, Notification, Contract, Portfolio, Chat, Meeting, FreelanciaJob, AIModel
3. [Sequential]  Planning -> Task
4. [Parallel]    Review, Offer, Gamification, Ticket-Service, Subcontracting
5. [Sequential]  API Gateway -> Frontend
```

### Parameters available on every job

| Parameter | Default | Description |
|-----------|---------|-------------|
| `REPO_URL` | GitHub repo URL | Git repository |
| `BRANCH` | `main` | Branch to build |
| `IMAGE_REPO` | `docker.io/ridhaferchichi` | Docker registry prefix |
| `IMAGE_TAG` | (build number) | Image tag override |
| `PUSH_IMAGE` | `true` | Push to Docker Hub |
| `RUN_SONARQUBE` | `true` | Run SonarQube analysis |
| `TRIGGER_DOWNSTREAM` | `false` | Trigger dependent services |
| `DOWNSTREAM_JOBS` | (auto-configured) | Comma-separated downstream jobs |

---

## 9. Run Pipelines

### Build a single service

1. Go to `services/<service-name>` (e.g., `services/freelancia-job`)
2. Click **Build with Parameters**
3. Adjust parameters if needed
4. Click **Build**

### Build everything (orchestration)

1. Go to `orchestration/full-stack-main`
2. Click **Build with Parameters**
3. Click **Build**

This builds all 21 services in the correct dependency order.

### Trigger from Git push (webhook)

1. In GitHub repo: **Settings > Webhooks > Add webhook**
   - Payload URL: `http://<jenkins-ip>:8080/github-webhook/`
   - Content type: `application/json`
   - Events: **Just the push event**

2. In each Jenkins job (or the seed job config): enable **GitHub hook trigger for GITScm polling**

---

## 10. Service Reference

| Job Name | Jenkinsfile Path | Docker Image | SonarQube Key |
|----------|-----------------|--------------|---------------|
| `services/eureka` | `backEnd/Eureka/Jenkinsfile` | `ridhaferchichi/eureka` | `eureka` |
| `services/config-server` | `backEnd/ConfigServer/Jenkinsfile` | `ridhaferchichi/config-server` | `config-server` |
| `services/api-gateway` | `backEnd/apiGateway/Jenkinsfile` | `ridhaferchichi/api-gateway` | `api-gateway` |
| `services/keycloak-auth` | `backEnd/KeyCloak/Jenkinsfile` | `ridhaferchichi/keycloak-auth` | `keycloak-auth` |
| `services/user` | `backEnd/Microservices/user/Jenkinsfile` | `ridhaferchichi/user` | `user` |
| `services/project` | `backEnd/Microservices/Project/Jenkinsfile` | `ridhaferchichi/project` | `project` |
| `services/offer` | `backEnd/Microservices/Offer/Jenkinsfile` | `ridhaferchichi/offer` | `offer` |
| `services/contract` | `backEnd/Microservices/Contract/Jenkinsfile` | `ridhaferchichi/contract` | `contract` |
| `services/portfolio` | `backEnd/Microservices/Portfolio/Jenkinsfile` | `ridhaferchichi/portfolio` | `portfolio` |
| `services/review` | `backEnd/Microservices/review/Jenkinsfile` | `ridhaferchichi/review` | `review` |
| `services/planning` | `backEnd/Microservices/planning/Jenkinsfile` | `ridhaferchichi/planning` | `planning` |
| `services/task` | `backEnd/Microservices/task/Jenkinsfile` | `ridhaferchichi/task` | `task` |
| `services/notification` | `backEnd/Microservices/Notification/Jenkinsfile` | `ridhaferchichi/notification` | `notification` |
| `services/gamification` | `backEnd/Microservices/gamification/Jenkinsfile` | `ridhaferchichi/gamification` | `gamification` |
| `services/chat` | `backEnd/Microservices/Chat/Jenkinsfile` | `ridhaferchichi/chat` | `chat` |
| `services/meeting` | `backEnd/Microservices/Meeting/Jenkinsfile` | `ridhaferchichi/meeting` | `meeting` |
| `services/freelancia-job` | `backEnd/Microservices/FreelanciaJob/Jenkinsfile` | `ridhaferchichi/freelancia-job` | `freelancia-job` |
| `services/ticket-service` | `backEnd/Microservices/ticket-service/Jenkinsfile` | `ridhaferchichi/ticket-service` | `ticket-service` |
| `services/subcontracting` | `backEnd/Microservices/Subcontracting/Jenkinsfile` | `ridhaferchichi/subcontracting` | `subcontracting` |
| `services/aimodel` | `backEnd/Microservices/aimodel-node/Jenkinsfile` | `ridhaferchichi/aimodel` | -- (Node.js) |
| `services/frontend` | `frontend/smart-freelance-app/Jenkinsfile` | `ridhaferchichi/frontend` | -- (Angular) |

---

## 11. Troubleshooting

### "docker: not found" in pipeline

Jenkins agent needs Docker CLI. Either:
- Mount the Docker socket: `-v /var/run/docker.sock:/var/run/docker.sock`
- Install Docker inside the Jenkins image (see section 2)
- Use a Jenkins agent with Docker pre-installed

### "mvnw: Permission denied"

The Jenkinsfiles already handle this with `chmod +x mvnw`. If it still fails, check that `mvnw` exists in the service directory. If missing, generate it:

```bash
cd backEnd/Microservices/<service>
mvn wrapper:wrapper
```

### SonarQube: "Not authorized" or "401"

- Verify `SonarQubeToken` credential exists in Jenkins
- Verify the token is valid in SonarQube (My Account > Security)
- Check the SonarQube server name in Jenkins matches: `SonarQube`

### SonarQube: "Quality Gate timeout"

This is non-blocking by design. The pipelines use `catchError` and `abortPipeline: false`. The build continues even if the gate times out.

### Docker Push: "denied: access forbidden"

- Check `DockerHubCrendentials` in Jenkins
- Verify Docker Hub username matches the image prefix (`ridhaferchichi/`)
- Try regenerating a Docker Hub access token

### Seed job: "ERROR: script not yet approved"

Go to **Manage Jenkins > In-process Script Approval** and approve the pending scripts.

### Jenkins runs out of disk space

```bash
# Clean old Docker images on Jenkins host
docker system prune -f

# Or limit build history (already configured in Jenkinsfiles):
# logRotator(numToKeepStr: "25")
```

### How to change the Docker Hub username

Update in two places:
1. `Jenkins/jobs.groovy` line: `stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', ...)`
2. Each `Jenkinsfile` `DOCKER_IMAGE` environment variable

Or override at build time via the `IMAGE_REPO` parameter.
