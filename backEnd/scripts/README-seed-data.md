# Seed Test Data Script

This script inserts test data into the **Offer**, **Project**, **Planning**, and **Review** microservices (excluding User and Evaluation).

## Prerequisites

- **Node.js** 18+ (for `fetch`)
- **MySQL** running (each service uses its own DB: `gestion_offre_db`, `projectdb`, `planningdb`, `reviewdb`)
- **Microservices** and optionally **API Gateway** running

## Option A: Via API Gateway (recommended)

1. Start API Gateway (port **8078**).
2. Start Offer (8082), Project (8084), Planning (8081), Review (8085). If you use Eureka, register them; otherwise ensure gateway routes point to these ports.
3. From the **project root** run:

```bash
node backEnd/scripts/seed-test-data.mjs
```

Or with explicit gateway URL:

```bash
GATEWAY_URL=http://localhost:8078 node backEnd/scripts/seed-test-data.mjs
```

## Option B: Direct to each service (no gateway)

If the gateway is not running, call each microservice on its port:

```bash
DIRECT_SERVICES=1 node backEnd/scripts/seed-test-data.mjs
```

Default ports:

| Service  | Port |
|----------|------|
| Offer    | 8082 |
| Project  | 8084 |
| Planning | 8081 |
| Review   | 8085 |

## What gets created

- **Offers**: 3 offers (freelancerIds 1, 15) – web dev, DevOps, mobile.
- **Projects**: 3 projects (clientIds 1, 2; freelancerIds 1, 15).
- **Progress updates**: 2 updates per project (for the first 2 projects), freelancerId 15.
- **Reviews**: 2 reviews per project (reviewer/reviewee 1 and 15).

User IDs (1, 2, 15) are placeholders; you can change them in the script to match existing users in your User service if needed.

## Run once

Execute the script **once**; data is inserted via the services’ REST APIs and stored in their databases. Running it again will create duplicate records.
