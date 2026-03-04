import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { UserService, User } from './user.service';
import { PortfolioService, Skill, Experience } from './portfolio.service';
import { ReviewService } from './review.service';

// ── Interfaces ─────────────────────────────────────────────────────────────────

export interface ProfileSkill {
  name: string;
  category: string;
  level: 'Beginner' | 'Intermediate' | 'Expert';
  verified: boolean;
}

export interface ProfileExperience {
  role: string;
  company: string;
  duration: string;
  description: string;
}

export interface ProfileProject {
  id: number;
  title: string;
  clientName: string;
  completionDate: string;
  rating: number;
  thumbnail: string;
}

export interface ProfileTestimonial {
  clientName: string;
  clientInitials: string;
  clientColor: string;
  projectName: string;
  rating: number;
  review: string;
}

export interface DailyVisit {
  date: string;
  visits: number;
}

export interface EngagementMetrics {
  totalVisits: number;
  uniqueVisits: number;
  thisWeekVisits: number;
  lastWeekVisits: number;
  dailyVisits: DailyVisit[];
}

export interface FreelancerCard {
  userId: number;
  firstName: string;
  lastName: string;
  title: string;
  avatarUrl?: string;
  rating: number;
  totalReviews: number;
  skills: ProfileSkill[];
}

export interface FreelancerProfile {
  userId: number;
  firstName: string;
  lastName: string;
  title: string;
  location: string;
  memberSince: string;
  avatarUrl?: string;
  rating: number;
  totalReviews: number;
  projectsCompleted: number;
  responseRate: number;
  hourlyRate: number;
  bio: string;
  yearsExperience: number;
  skills: ProfileSkill[];
  experiences: ProfileExperience[];
  recentProjects: ProfileProject[];
  testimonials: ProfileTestimonial[];
  metrics: EngagementMetrics;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function generateDailyVisits(base: number, amplitude: number): DailyVisit[] {
  const days: DailyVisit[] = [];
  const now = new Date();
  for (let i = 29; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    const label = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    const visits = Math.max(0, Math.round(base + Math.sin(i * 0.7) * amplitude + Math.cos(i * 0.4) * (amplitude * 0.5)));
    days.push({ date: label, visits });
  }
  return days;
}

function skillFromApi(s: Skill): ProfileSkill {
  const levelMap: Record<number, ProfileSkill['level']> = { 0: 'Beginner', 1: 'Intermediate', 2: 'Expert' };
  const score = s.score ?? 0;
  const level: ProfileSkill['level'] = score >= 80 ? 'Expert' : score >= 50 ? 'Intermediate' : 'Beginner';
  const category = s.domains?.[0]?.replace(/_/g, ' ').toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase()) ?? 'General';
  return { name: s.name, category, level, verified: !!s.verified };
}

function expFromApi(e: Experience): ProfileExperience {
  const start = e.startDate ? new Date(e.startDate).getFullYear() : '?';
  const end = e.endDate ? new Date(e.endDate).getFullYear() : 'Present';
  return {
    role: e.title,
    company: e.companyOrClientName,
    duration: `${start} – ${end}`,
    description: e.description,
  };
}

// ── Dummy Data ─────────────────────────────────────────────────────────────────

export const DUMMY_FREELANCERS: FreelancerProfile[] = [
  {
    userId: 101, firstName: 'Sarah', lastName: 'Chen', title: 'Senior Angular Developer',
    location: 'San Francisco, CA', memberSince: 'March 2021', avatarUrl: undefined,
    rating: 4.9, totalReviews: 127, projectsCompleted: 127, responseRate: 98,
    hourlyRate: 85, yearsExperience: 7,
    bio: 'Passionate front-end engineer with 7+ years building scalable web applications. Specialist in Angular, TypeScript, and design systems.',
    skills: [
      { name: 'Angular', category: 'Frontend', level: 'Expert', verified: true },
      { name: 'TypeScript', category: 'Frontend', level: 'Expert', verified: true },
      { name: 'RxJS', category: 'Frontend', level: 'Expert', verified: true },
      { name: 'UI/UX Design', category: 'Design', level: 'Intermediate', verified: false },
      { name: 'NgRx', category: 'Frontend', level: 'Intermediate', verified: true },
    ],
    experiences: [
      { role: 'Senior Frontend Engineer', company: 'TechCorp Inc.', duration: '2021 – Present', description: 'Led the Angular migration of a legacy React codebase for a 100k+ user SaaS platform.' },
      { role: 'Frontend Developer', company: 'StartupXYZ', duration: '2018 – 2021', description: 'Built interactive dashboards and data visualization tools for B2B clients.' },
    ],
    recentProjects: [
      { id: 1, title: 'E-Commerce Dashboard', clientName: 'RetailPro', completionDate: 'Jan 2024', rating: 5.0, thumbnail: 'https://picsum.photos/seed/ec1/600/400' },
      { id: 2, title: 'Analytics Platform UI', clientName: 'DataFlow', completionDate: 'Oct 2023', rating: 4.8, thumbnail: 'https://picsum.photos/seed/ap2/600/400' },
      { id: 3, title: 'Freelance Marketplace', clientName: 'FreelancePro', completionDate: 'Jul 2023', rating: 5.0, thumbnail: 'https://picsum.photos/seed/fm3/600/400' },
    ],
    testimonials: [
      { clientName: 'James Wilson', clientInitials: 'JW', clientColor: 'primary', projectName: 'E-Commerce Dashboard', rating: 5, review: 'Sarah delivered exceptional work on our Angular project. Her attention to detail and technical expertise made the project a huge success.' },
      { clientName: 'Maria Garcia', clientInitials: 'MG', clientColor: 'success', projectName: 'Analytics Platform UI', rating: 5, review: 'Outstanding developer who consistently exceeds expectations. I highly recommend Sarah for complex Angular projects.' },
    ],
    metrics: { totalVisits: 12450, uniqueVisits: 3280, thisWeekVisits: 342, lastWeekVisits: 298, dailyVisits: generateDailyVisits(55, 22) },
  },
  {
    userId: 102, firstName: 'Marco', lastName: 'Rivera', title: 'Full-Stack Java Developer',
    location: 'Barcelona, Spain', memberSince: 'June 2020', avatarUrl: undefined,
    rating: 4.8, totalReviews: 93, projectsCompleted: 93, responseRate: 95,
    hourlyRate: 72, yearsExperience: 6,
    bio: 'Full-stack developer specialising in Spring Boot microservices and Angular SPAs. Passionate about clean architecture and DDD.',
    skills: [
      { name: 'Spring Boot', category: 'Backend', level: 'Expert', verified: true },
      { name: 'Java', category: 'Backend', level: 'Expert', verified: true },
      { name: 'Angular', category: 'Frontend', level: 'Intermediate', verified: true },
      { name: 'MySQL', category: 'Database', level: 'Intermediate', verified: false },
      { name: 'Docker', category: 'DevOps', level: 'Intermediate', verified: true },
    ],
    experiences: [
      { role: 'Backend Lead', company: 'Microservices Hub', duration: '2020 – Present', description: 'Designed RESTful APIs and event-driven systems for a FinTech platform serving 50k+ daily users.' },
    ],
    recentProjects: [
      { id: 4, title: 'Payment Gateway API', clientName: 'FinTech Ltd', completionDate: 'Feb 2024', rating: 4.9, thumbnail: 'https://picsum.photos/seed/pg4/600/400' },
      { id: 5, title: 'HR Management System', clientName: 'HRCorp', completionDate: 'Nov 2023', rating: 4.8, thumbnail: 'https://picsum.photos/seed/hr5/600/400' },
    ],
    testimonials: [
      { clientName: 'Alice Johnson', clientInitials: 'AJ', clientColor: 'info', projectName: 'Payment Gateway API', rating: 5, review: 'Marco built a robust, well-documented API. Our integration was seamless thanks to his clean architecture.' },
    ],
    metrics: { totalVisits: 8200, uniqueVisits: 2100, thisWeekVisits: 210, lastWeekVisits: 195, dailyVisits: generateDailyVisits(38, 14) },
  },
  {
    userId: 103, firstName: 'Aisha', lastName: 'Patel', title: 'UI/UX Designer & Developer',
    location: 'London, UK', memberSince: 'January 2022', avatarUrl: undefined,
    rating: 4.95, totalReviews: 61, projectsCompleted: 61, responseRate: 99,
    hourlyRate: 90, yearsExperience: 5,
    bio: 'Creative designer with a developer mindset. I turn complex problems into elegant, accessible interfaces using Figma and Angular.',
    skills: [
      { name: 'Figma', category: 'Design', level: 'Expert', verified: true },
      { name: 'Angular', category: 'Frontend', level: 'Intermediate', verified: true },
      { name: 'SCSS', category: 'Frontend', level: 'Expert', verified: true },
      { name: 'Accessibility', category: 'Design', level: 'Expert', verified: false },
    ],
    experiences: [
      { role: 'Lead UX Designer', company: 'DesignCo Agency', duration: '2022 – Present', description: 'Led design systems for 12 enterprise clients across finance and healthcare verticals.' },
    ],
    recentProjects: [
      { id: 6, title: 'Healthcare Portal Redesign', clientName: 'MedTech', completionDate: 'Mar 2024', rating: 5.0, thumbnail: 'https://picsum.photos/seed/hp6/600/400' },
      { id: 7, title: 'Design System Library', clientName: 'TechCorp', completionDate: 'Dec 2023', rating: 4.9, thumbnail: 'https://picsum.photos/seed/ds7/600/400' },
    ],
    testimonials: [
      { clientName: 'Robert Chen', clientInitials: 'RC', clientColor: 'warning', projectName: 'Healthcare Portal Redesign', rating: 5, review: 'Aisha transformed our outdated portal into a beautiful, intuitive application. User satisfaction scores jumped 40%.' },
    ],
    metrics: { totalVisits: 15600, uniqueVisits: 4100, thisWeekVisits: 480, lastWeekVisits: 390, dailyVisits: generateDailyVisits(68, 30) },
  },
  {
    userId: 104, firstName: 'James', lastName: 'Kim', title: 'DevOps & Cloud Engineer',
    location: 'Seoul, South Korea', memberSince: 'September 2019', avatarUrl: undefined,
    rating: 4.7, totalReviews: 149, projectsCompleted: 149, responseRate: 92,
    hourlyRate: 95, yearsExperience: 8,
    bio: 'DevOps specialist with deep expertise in Kubernetes, AWS, and CI/CD pipelines. I help teams ship faster with confidence.',
    skills: [
      { name: 'Kubernetes', category: 'DevOps', level: 'Expert', verified: true },
      { name: 'AWS', category: 'Cloud', level: 'Expert', verified: true },
      { name: 'Docker', category: 'DevOps', level: 'Expert', verified: true },
      { name: 'Terraform', category: 'DevOps', level: 'Intermediate', verified: false },
      { name: 'GitHub Actions', category: 'DevOps', level: 'Expert', verified: true },
    ],
    experiences: [
      { role: 'Senior DevOps Engineer', company: 'CloudScale Inc.', duration: '2019 – Present', description: 'Built multi-region Kubernetes clusters and automated deployment pipelines for Fortune 500 clients.' },
    ],
    recentProjects: [
      { id: 8, title: 'Multi-Cloud Migration', clientName: 'FinServe Corp', completionDate: 'Jan 2024', rating: 4.7, thumbnail: 'https://picsum.photos/seed/mc8/600/400' },
    ],
    testimonials: [
      { clientName: 'Lisa Park', clientInitials: 'LP', clientColor: 'danger', projectName: 'Multi-Cloud Migration', rating: 5, review: 'James made our cloud migration painless. Zero downtime, comprehensive documentation, and proactive communication.' },
    ],
    metrics: { totalVisits: 9800, uniqueVisits: 2700, thisWeekVisits: 280, lastWeekVisits: 310, dailyVisits: generateDailyVisits(42, 18) },
  },
  {
    userId: 105, firstName: 'Elena', lastName: 'Vasquez', title: 'Mobile App Developer',
    location: 'Mexico City, Mexico', memberSince: 'April 2021', avatarUrl: undefined,
    rating: 4.85, totalReviews: 78, projectsCompleted: 78, responseRate: 97,
    hourlyRate: 68, yearsExperience: 5,
    bio: 'Mobile developer specialising in cross-platform apps with Flutter and React Native. Obsessed with smooth animations and great UX.',
    skills: [
      { name: 'Flutter', category: 'Mobile', level: 'Expert', verified: true },
      { name: 'React Native', category: 'Mobile', level: 'Expert', verified: true },
      { name: 'Dart', category: 'Mobile', level: 'Expert', verified: false },
      { name: 'Firebase', category: 'Backend', level: 'Intermediate', verified: true },
    ],
    experiences: [
      { role: 'Mobile Lead', company: 'AppWorks Studio', duration: '2021 – Present', description: 'Delivered 20+ cross-platform apps with 4.8+ App Store ratings for startups and enterprise clients.' },
    ],
    recentProjects: [
      { id: 9, title: 'Fitness Tracking App', clientName: 'FitLife Co', completionDate: 'Feb 2024', rating: 5.0, thumbnail: 'https://picsum.photos/seed/ft9/600/400' },
      { id: 10, title: 'Food Delivery Platform', clientName: 'QuickBite', completionDate: 'Oct 2023', rating: 4.8, thumbnail: 'https://picsum.photos/seed/fd10/600/400' },
    ],
    testimonials: [
      { clientName: 'Tom Bradley', clientInitials: 'TB', clientColor: 'success', projectName: 'Fitness Tracking App', rating: 5, review: 'Elena delivered a polished app ahead of schedule. The animations are buttery smooth and users love it.' },
    ],
    metrics: { totalVisits: 7600, uniqueVisits: 2000, thisWeekVisits: 195, lastWeekVisits: 180, dailyVisits: generateDailyVisits(33, 12) },
  },
  {
    userId: 106, firstName: 'David', lastName: 'Okonkwo', title: 'Data Science & ML Engineer',
    location: 'Lagos, Nigeria', memberSince: 'February 2020', avatarUrl: undefined,
    rating: 4.75, totalReviews: 55, projectsCompleted: 55, responseRate: 94,
    hourlyRate: 80, yearsExperience: 6,
    bio: 'ML engineer passionate about turning raw data into actionable insights. Expert in Python, TensorFlow, and building production-grade AI pipelines.',
    skills: [
      { name: 'Python', category: 'Data Science', level: 'Expert', verified: true },
      { name: 'TensorFlow', category: 'AI/ML', level: 'Expert', verified: true },
      { name: 'Pandas', category: 'Data Science', level: 'Expert', verified: false },
      { name: 'SQL', category: 'Database', level: 'Intermediate', verified: true },
    ],
    experiences: [
      { role: 'ML Engineer', company: 'DataMinds Lab', duration: '2020 – Present', description: 'Developed NLP and computer vision models deployed across 3 continents for retail and logistics clients.' },
    ],
    recentProjects: [
      { id: 11, title: 'Demand Forecasting Model', clientName: 'RetailGiant', completionDate: 'Mar 2024', rating: 4.8, thumbnail: 'https://picsum.photos/seed/df11/600/400' },
    ],
    testimonials: [
      { clientName: 'Yuki Tanaka', clientInitials: 'YT', clientColor: 'primary', projectName: 'Demand Forecasting Model', rating: 5, review: 'David built an incredibly accurate forecasting model that reduced our inventory costs by 23%. Exceptional work.' },
    ],
    metrics: { totalVisits: 6400, uniqueVisits: 1800, thisWeekVisits: 162, lastWeekVisits: 140, dailyVisits: generateDailyVisits(29, 11) },
  },
  {
    userId: 107, firstName: 'Priya', lastName: 'Sharma', title: 'Cybersecurity Specialist',
    location: 'Bangalore, India', memberSince: 'July 2020', avatarUrl: undefined,
    rating: 4.9, totalReviews: 44, projectsCompleted: 44, responseRate: 96,
    hourlyRate: 110, yearsExperience: 7,
    bio: 'Certified ethical hacker and security architect. I help companies identify vulnerabilities and build robust security postures.',
    skills: [
      { name: 'Penetration Testing', category: 'Security', level: 'Expert', verified: true },
      { name: 'OWASP', category: 'Security', level: 'Expert', verified: true },
      { name: 'Network Security', category: 'Security', level: 'Expert', verified: false },
      { name: 'Python', category: 'Programming', level: 'Intermediate', verified: true },
    ],
    experiences: [
      { role: 'Senior Security Consultant', company: 'SecureNet', duration: '2020 – Present', description: 'Conducted 80+ penetration tests for banks, hospitals, and tech companies across Asia and Europe.' },
    ],
    recentProjects: [
      { id: 12, title: 'Bank Security Audit', clientName: 'GlobalBank', completionDate: 'Feb 2024', rating: 5.0, thumbnail: 'https://picsum.photos/seed/bs12/600/400' },
    ],
    testimonials: [
      { clientName: 'Andrew Mills', clientInitials: 'AM', clientColor: 'danger', projectName: 'Bank Security Audit', rating: 5, review: 'Priya found 3 critical vulnerabilities our internal team missed. Her detailed report and remediation guidance were invaluable.' },
    ],
    metrics: { totalVisits: 5200, uniqueVisits: 1400, thisWeekVisits: 135, lastWeekVisits: 122, dailyVisits: generateDailyVisits(24, 9) },
  },
  {
    userId: 108, firstName: 'Alex', lastName: 'Thompson', title: 'Content & Copywriter',
    location: 'Toronto, Canada', memberSince: 'November 2021', avatarUrl: undefined,
    rating: 4.8, totalReviews: 112, projectsCompleted: 112, responseRate: 99,
    hourlyRate: 55, yearsExperience: 4,
    bio: 'Strategic content writer who crafts compelling copy that converts. Specialised in SaaS, fintech, and B2B tech content.',
    skills: [
      { name: 'Copywriting', category: 'Content', level: 'Expert', verified: true },
      { name: 'SEO Writing', category: 'Content', level: 'Expert', verified: true },
      { name: 'Content Strategy', category: 'Marketing', level: 'Intermediate', verified: false },
    ],
    experiences: [
      { role: 'Senior Content Strategist', company: 'WordCraft Agency', duration: '2021 – Present', description: 'Managed content strategy for 25+ SaaS clients, driving 3x organic traffic growth on average.' },
    ],
    recentProjects: [
      { id: 13, title: 'SaaS Website Relaunch', clientName: 'CloudSoft', completionDate: 'Jan 2024', rating: 4.9, thumbnail: 'https://picsum.photos/seed/sw13/600/400' },
      { id: 14, title: 'Email Campaign Series', clientName: 'FinanceHub', completionDate: 'Nov 2023', rating: 4.8, thumbnail: 'https://picsum.photos/seed/ec14/600/400' },
    ],
    testimonials: [
      { clientName: 'Sophie Laurent', clientInitials: 'SL', clientColor: 'warning', projectName: 'SaaS Website Relaunch', rating: 5, review: 'Alex completely transformed our website copy. Conversion rates increased 45% in the first month.' },
    ],
    metrics: { totalVisits: 11300, uniqueVisits: 3050, thisWeekVisits: 315, lastWeekVisits: 280, dailyVisits: generateDailyVisits(50, 20) },
  },
];

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class FreelancerService {
  constructor(
    private userSvc: UserService,
    private portfolioSvc: PortfolioService,
    private reviewSvc: ReviewService,
  ) {}

  /** Returns a card-summary list of all freelancers. Falls back to dummy data on error. */
  getAllFreelancers(): Observable<FreelancerCard[]> {
    return this.userSvc.getAll().pipe(
      map((users: User[]) =>
        users
          .filter(u => u.role === 'FREELANCER')
          .map(u => ({
            userId: u.id,
            firstName: u.firstName,
            lastName: u.lastName,
            title: 'Freelancer',
            avatarUrl: u.avatarUrl,
            rating: 4.5,
            totalReviews: 0,
            skills: [],
          }))
      ),
      catchError(() => of(DUMMY_FREELANCERS.map(p => this._profileToCard(p)))),
    );
  }

  /** Returns the full profile for a single freelancer. Falls back to dummy data on error. */
  getFreelancerProfile(userId: number): Observable<FreelancerProfile | null> {
    return this.userSvc.getById(userId).pipe(
      switchMap(user => {
        if (!user || user.role !== 'FREELANCER') {
          return of(DUMMY_FREELANCERS.find(d => d.userId === userId) ?? null);
        }
        return forkJoin({
          skills: this.portfolioSvc.getUserSkills(userId).pipe(catchError(() => of([]))),
          experiences: this.portfolioSvc.getExperiences(userId).pipe(catchError(() => of([]))),
          reviews: this.reviewSvc.getByRevieweeId(userId).pipe(catchError(() => of([]))),
        }).pipe(
          map(({ skills, experiences, reviews }) => {
            const base = DUMMY_FREELANCERS.find(d => d.userId === userId) ?? DUMMY_FREELANCERS[0];
            const avgRating = reviews.length
              ? reviews.reduce((s, r) => s + r.rating, 0) / reviews.length
              : base.rating;
            return {
              ...base,
              userId: user.id,
              firstName: user.firstName,
              lastName: user.lastName,
              avatarUrl: user.avatarUrl ?? undefined,
              rating: Math.round(avgRating * 10) / 10,
              totalReviews: reviews.length || base.totalReviews,
              skills: skills.length ? skills.map(skillFromApi) : base.skills,
              experiences: experiences.length ? experiences.map(expFromApi) : base.experiences,
            } as FreelancerProfile;
          }),
          catchError(() => of(DUMMY_FREELANCERS.find(d => d.userId === userId) ?? null)),
        );
      }),
      catchError(() => of(DUMMY_FREELANCERS.find(d => d.userId === userId) ?? null)),
    );
  }

  private _profileToCard(p: FreelancerProfile): FreelancerCard {
    return {
      userId: p.userId,
      firstName: p.firstName,
      lastName: p.lastName,
      title: p.title,
      avatarUrl: p.avatarUrl,
      rating: p.rating,
      totalReviews: p.totalReviews,
      skills: p.skills.slice(0, 4),
    };
  }
}
