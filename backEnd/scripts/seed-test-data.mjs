#!/usr/bin/env node
/**
 * Seed test data for microservices: Offer, Project, Planning, Review.
 * Excludes: User, Evaluation.
 *
 * Prerequisites:
 * - API Gateway running (default http://localhost:8078)
 * - Offer, Project, Planning, Review microservices running and registered (or use DIRECT_SERVICES=1)
 *
 * Usage:
 *   node backEnd/scripts/seed-test-data.mjs
 *   GATEWAY_URL=http://localhost:8078 node backEnd/scripts/seed-test-data.mjs
 *   DIRECT_SERVICES=1 node backEnd/scripts/seed-test-data.mjs   # call each service on its port
 */

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8078';
const USE_DIRECT = process.env.DIRECT_SERVICES === '1' || process.env.DIRECT_SERVICES === 'true';

const bases = USE_DIRECT
  ? {
      offer: 'http://localhost:8082',
      project: 'http://localhost:8084',
      planning: 'http://localhost:8081',
      review: 'http://localhost:8085',
    }
  : {
      offer: GATEWAY_URL + '/offer',
      project: GATEWAY_URL + '/project',
      planning: GATEWAY_URL + '/planning',
      review: GATEWAY_URL + '/review',
    };

async function post(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}: ${text}`);
  return text ? JSON.parse(text) : null;
}

async function main() {
  console.log('Seeding test data (Offer, Project, Planning, Review)...');
  console.log('Base URLs:', USE_DIRECT ? 'direct ports' : GATEWAY_URL, '\n');

  const created = { offers: [], projects: [], progressUpdates: [], reviews: [] };

  // ----- Offers (freelancerId: 1, 15 - use IDs that may exist in User service or any number) -----
  const offerPayloads = [
    {
      freelancerId: 1,
      title: 'Full-stack web development with Angular and Spring',
      domain: 'Web Development',
      description: 'I will build a responsive SPA with Angular frontend and Spring Boot backend, including auth and REST APIs.',
      price: 150.0,
      durationType: 'fixed',
      deadline: '2025-06-30',
      category: 'Web',
      tags: 'Angular,Spring Boot,Java',
      isFeatured: true,
    },
    {
      freelancerId: 15,
      title: 'DevOps and CI/CD pipeline setup',
      domain: 'DevOps',
      description: 'Setting up Jenkins or GitHub Actions pipelines, Docker and Kubernetes for your application deployment.',
      price: 200.5,
      durationType: 'hourly',
      deadline: '2025-07-15',
      category: 'DevOps',
      tags: 'Docker,Kubernetes,CI/CD',
      isFeatured: false,
    },
    {
      freelancerId: 1,
      title: 'Mobile app development with React Native',
      domain: 'Mobile',
      description: 'Cross-platform mobile application using React Native for iOS and Android with clean architecture.',
      price: 120.0,
      durationType: 'monthly',
      deadline: '2025-08-01',
      category: 'Mobile',
      tags: 'React Native,TypeScript',
      isFeatured: true,
    },
  ];

  for (const body of offerPayloads) {
    try {
      const o = await post(bases.offer + '/api/offers', body);
      created.offers.push(o?.id ?? o);
      console.log('  Created offer:', o?.id ?? o, o?.title ?? body.title);
    } catch (e) {
      console.warn('  Offer create failed:', e.message);
    }
  }

  // ----- Projects (clientId, optional freelancerId) -----
  const projectPayloads = [
    {
      clientId: 1,
      freelancerId: 15,
      title: 'E-commerce platform MVP',
      description: 'Build an MVP for an online store with product catalog, cart and checkout.',
      budget: 2500.0,
      deadline: '2025-09-01T23:59:59',
      status: 'OPEN',
      category: 'E-commerce',
      skillsRequiered: 'Java,Spring,React',
    },
    {
      clientId: 2,
      freelancerId: 1,
      title: 'Internal dashboard and reporting',
      description: 'Dashboard to visualize sales and KPIs with charts and export to PDF.',
      budget: 1800.0,
      deadline: '2025-08-15T23:59:59',
      status: 'IN_PROGRESS',
      category: 'Business Intelligence',
      skillsRequiered: 'Angular,Chart.js',
    },
    {
      clientId: 1,
      title: 'API documentation and Postman collection',
      description: 'Document existing REST API and provide a Postman collection for testing.',
      budget: 400.0,
      status: 'OPEN',
      category: 'Documentation',
    },
  ];

  for (const body of projectPayloads) {
    try {
      const p = await post(bases.project + '/projects/add', body);
      const id = p?.id ?? p;
      created.projects.push(id);
      console.log('  Created project:', id, p?.title ?? body.title);
    } catch (e) {
      console.warn('  Project create failed:', e.message);
    }
  }

  // ----- Progress updates (projectId, freelancerId) - use first 2 projects -----
  const projectIds = created.projects.slice(0, 2).filter(Boolean);
  const progressPayloads = [];
  for (const projectId of projectIds) {
    progressPayloads.push(
      {
        projectId,
        contractId: null,
        freelancerId: 15,
        title: 'Sprint 1 completed',
        description: 'Backend API and database schema implemented.',
        progressPercentage: 25,
      },
      {
        projectId,
        contractId: null,
        freelancerId: 15,
        title: 'Sprint 2 in progress',
        description: 'Frontend components and integration with API.',
        progressPercentage: 50,
      }
    );
  }

  for (const body of progressPayloads) {
    try {
      const u = await post(bases.planning + '/api/progress-updates', body);
      created.progressUpdates.push(u?.id ?? u);
      console.log('  Created progress update:', u?.id ?? u, body.title);
    } catch (e) {
      console.warn('  Progress update create failed:', e.message);
    }
  }

  // ----- Reviews (reviewerId, revieweeId, projectId, rating 1-5) -----
  const reviewPayloads = [];
  for (const projectId of created.projects) {
    if (!projectId) continue;
    reviewPayloads.push(
      {
        reviewerId: 15,
        revieweeId: 1,
        projectId,
        rating: 5,
        comment: 'Great collaboration and clear requirements. Delivered on time.',
      },
      {
        reviewerId: 1,
        revieweeId: 15,
        projectId,
        rating: 4,
        comment: 'Professional work. Would hire again for the next project.',
      }
    );
  }

  for (const body of reviewPayloads) {
    try {
      const r = await post(bases.review + '/api/reviews', body);
      created.reviews.push(r?.id ?? r);
      console.log('  Created review:', r?.id ?? r, 'project', body.projectId);
    } catch (e) {
      console.warn('  Review create failed:', e.message);
    }
  }

  console.log('\nDone.');
  console.log('  Offers:', created.offers.length);
  console.log('  Projects:', created.projects.length);
  console.log('  Progress updates:', created.progressUpdates.length);
  console.log('  Reviews:', created.reviews.length);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
