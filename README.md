<div align="center">

[![ESPRIT Tunisia](https://upload.wikimedia.org/wikipedia/commons/b/b6/Logo_ESPRIT_-_Tunisie.png)](https://www.esprit.tn)

**École Supérieure Privée d'Ingénierie et de Technologie — Tunisie**

# Smart Freelance & Project Matching Platform

*PI Dev — 4SAE11 — Academic Year 2024/2025*

A microservices-based platform connecting freelancers and clients for project collaboration, featuring AI-powered skill verification, portfolio management, and real-time notifications.

[Features](#features) • [Architecture](#architecture) • [Getting Started](#getting-started) • [Documentation](#documentation)

</div>

---

## Overview

**Smart Freelance & Project Matching Platform** is a full-stack web application built with a microservices architecture. It enables clients to post projects, browse freelancer profiles, and hire talent; freelancers can showcase portfolios, apply to jobs, manage offers, and track progress with calendar and GitHub integration.

---

## Features

| Role | Capabilities |
|------|--------------|
| **Freelancers** | Portfolio with experiences & skills, AI skill verification, browse jobs, submit applications, manage offers, reviews & ratings, contracts, notifications, calendar, GitHub sync |
| **Clients** | Project CRUD, job posting, browse freelancers/offers, progress tracking |
| **Admins** | User management, projects/contracts/offers oversight, planning, reviews, evaluations |

---

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────────────────────────────────────┐
│   Angular   │────│ API Gateway │────│              Microservices (Eureka)               │
│  Frontend   │     │   (8078)    │     │ User │ Project │ Offer │ Contract │ Portfolio │  │
│  (4200)     │     └──────────────┘     │ Review │ Planning │ Notification │ Keycloak   │  │
└─────────────┘            │             └─────────────────────────────────────────────────┘
                           │
                    ┌──────┴──────┐
                    │   Config    │
                    │   Server    │
                    │   (8888)    │
                    └─────────────┘
```

### Tech Stack

| Layer | Technologies |
|-------|--------------|
| **Frontend** | Angular 21, Bootstrap 5, Chart.js, TypeScript 5.9, SCSS |
| **Backend** | Java 17, Spring Boot 3.4/4.0, Spring Cloud (Eureka, Config, Gateway), OpenFeign, Resilience4j |
| **Security** | Keycloak (OAuth2/JWT) |
| **Database** | MySQL 8 (one DB per microservice) |
| **APIs** | SpringDoc / OpenAPI (Swagger) |
| **Extras** | Firebase (notifications), AI API (skill verification), GitHub integration |

---

## Getting Started

### Prerequisites

- **Java 17**
- **Maven 3.8+**
- **Node.js 18+** and **npm**
- **MySQL 8** on `localhost:3306`
- **Keycloak** (standalone) on `localhost:8080` with realm `smart-freelance`

### Service Ports

| Service | Port | Database |
|---------|------|----------|
| Eureka | 8420 | — |
| Config Server | 8888 | — |
| API Gateway | 8078 | — |
| Keycloak Auth | 8079 | — |
| User | 8090 | `userdb` |
| Planning | 8081 | `planningdb` |
| Offer | 8082 | `gestion_offre_db` |
| Contract | 8083 | `gestion_contract_db` |
| Project | 8084 | `projectdb` |
| Review | 8085 | `reviewdb` |
| Portfolio | 8086 | `portfolio_db` |
| Notification | 8087 | Firebase |
| Task | 8091 | `taskdb` |

### Startup Order

1. MySQL  
2. **Eureka** → `backEnd/Eureka`  
3. **Config Server** → `backEnd/ConfigServer` *(optional; services work without it)*  
4. **API Gateway** → `backEnd/apiGateway`  
5. **Keycloak** (standalone) — [see Keycloak setup](backEnd/KeyCloak/README.md)  
6. **Keycloak Auth** → `backEnd/KeyCloak`  
7. **Microservices** — User, Project, Offer, Contract, Portfolio, Review, Planning, Notification, Task  

### Run the Backend

```bash
# Example: start Eureka
cd backEnd/Eureka
mvn spring-boot:run

# Example: start User service
cd backEnd/Microservices/user
mvn spring-boot:run
```

### Run the Frontend

```bash
cd frontend/smart-freelance-app
npm install
npm start
```

Open **http://localhost:4200**

### API Documentation

Swagger UI is available via the Gateway for services that expose it:
- **http://localhost:8078/{service-name}/swagger-ui.html**

---

## Project Structure

```
├── backEnd/
│   ├── apiGateway/          # Spring Cloud Gateway
│   ├── ConfigServer/        # Centralized configuration
│   ├── Eureka/              # Service discovery
│   ├── KeyCloak/            # Auth microservice (OAuth2/JWT)
│   └── Microservices/
│       ├── Contract/        # Contract management
│       ├── Notification/    # Push notifications (Firebase)
│       ├── Offer/           # Offers & applications
│       ├── planning/        # Calendar, GitHub sync
│       ├── task/            # Tasks, subtasks, calendar integration
│       ├── Portfolio/       # Portfolio, skills, AI verification
│       ├── Project/         # Project management
│       ├── review/          # Reviews & ratings
│       └── user/            # User profiles
├── frontend/
│   └── smart-freelance-app/ # Angular SPA
├── Documentation/           # Test plans, specs
└── plans/                   # Implementation specs
```

---

## Optional Integrations

| Integration | Purpose | Configuration |
|-------------|---------|---------------|
| **Google Translate** | Offer translations | API key in Offer service |
| **Firebase** | Push notifications | Credentials in Notification service |
| **GitHub** | Planning sync | See [credentials/README.md](credentials/README.md) — set `$env:GITHUB_TOKEN` (token never committed) |
| **AI API** | Skill verification | API key in Portfolio service |

---

## Documentation

- [Keycloak Setup](backEnd/KeyCloak/README.md) — Auth & realm configuration
- [Portfolio Test Plan](Documentation/TEST_PLAN_PORTFOLIO.md)
- [Implementation Specs](plans/)

---

## Contributing

This project is developed as part of the **PI Dev** course at ESPRIT Tunisia. For contributions, please follow the existing code style and open a pull request.

---

<div align="center">

**ESPRIT — École Supérieure Privée d'Ingénierie et de Technologie — Tunisie**

*4SAE11 • PI Dev • 2024/2025*

`#Angular` `#SpringBoot` `#Microservices` `#Keycloak` `#MySQL` `#Freelance` `#FullStack` `#TypeScript` `#Java` `#ESPIRIT` `#Tunisia`

</div>
