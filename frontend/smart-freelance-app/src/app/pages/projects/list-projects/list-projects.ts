import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { ProjectService, Project } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { HttpErrorResponse } from '@angular/common/http';
import { interval, Subscription, forkJoin, of } from 'rxjs';
import { catchError, map, filter } from 'rxjs/operators';

const AUTO_REFRESH_INTERVAL_MS = 60_000;

/** Normalize API response to Project[] (handles raw array or wrapped { data/content }). */
function toProjectList(res: unknown): Project[] {
  if (Array.isArray(res)) return res as Project[];
  if (res && typeof res === 'object') {
    const o = res as Record<string, unknown>;
    if (Array.isArray(o['data'])) return o['data'] as Project[];
    if (Array.isArray(o['content'])) return o['content'] as Project[];
  }
  return [];
}

@Component({
  selector: 'app-list-projects',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './list-projects.html',
  styleUrl: './list-projects.scss',
})
export class ListProjects implements OnInit, OnDestroy {
  projects: Project[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  projectToDelete: Project | null = null;
  deleting = false;
  private autoRefreshSub: Subscription | null = null;
  private routerSub: Subscription | null = null;
  private previousUrl = '';

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.previousUrl = this.router.url;
    this.loadProjects();
    this.autoRefreshSub = interval(AUTO_REFRESH_INTERVAL_MS).subscribe(() => {
      if (!this.isLoading) this.loadProjects();
    });
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        if (this.previousUrl.includes('edit') && e.urlAfterRedirects.includes('my-projects')) {
          this.loadProjects();
        }
        this.previousUrl = e.urlAfterRedirects;
      });
  }

  ngOnDestroy(): void {
    this.autoRefreshSub?.unsubscribe();
    this.routerSub?.unsubscribe();
  }

  openDeleteModal(project: Project): void {
    this.projectToDelete = project;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.projectToDelete = null;
  }

  doDelete(): void {
    const project = this.projectToDelete;
    if (!project?.id) return;
    this.deleting = true;
    this.projectService.deleteProject(project.id).subscribe({
      next: () => {
        this.projectToDelete = null;
        this.deleting = false;
        this.loadProjects();
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error('Delete failed', err);
        this.deleting = false;
        this.errorMessage = 'Failed to delete project.';
        this.cdr.detectChanges();
      },
    });
  }

  private isTimeoutError(err: unknown): boolean {
    if (err && typeof err === 'object') {
      const o = err as { name?: string; message?: string };
      return o.name === 'TimeoutError' || (typeof o.message === 'string' && o.message.includes('Timeout'));
    }
    return false;
  }

  loadProjects(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    const email = this.authService.getPreferredUsername();
    if (!email) {
      this.errorMessage = 'You must be signed in to view projects.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    const user$ = this.userService.getByEmail(email).pipe(catchError(() => of(null)));

    forkJoin({ user: user$ }).subscribe({
      next: ({ user }) => {
        if (!user?.id) {
          this.projects = [];
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }
        let projects$;
        if (user.role === 'CLIENT') {
          projects$ = this.projectService.getByClientId(user.id).pipe(map((res) => toProjectList(res)));
        } else if (user.role === 'FREELANCER') {
          projects$ = this.projectService.getByFreelancerId(user.id).pipe(map((res) => toProjectList(res)));
        } else {
          projects$ = this.projectService.getAllProjects().pipe(map((res) => toProjectList(res)));
        }
        projects$.subscribe({
          next: (list) => {
            this.projects = Array.isArray(list) ? list : toProjectList(list);
            this.isLoading = false;
            this.cdr.detectChanges();
          },
          error: (err: HttpErrorResponse) => {
            this.errorMessage = this.isTimeoutError(err)
              ? 'Request timed out. Use Refresh to try again.'
              : 'Failed to load projects.';
            this.isLoading = false;
            this.cdr.detectChanges();
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage = this.isTimeoutError(err)
          ? 'Request timed out. Use Refresh to try again.'
          : 'Failed to load projects.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getSkills(project: Project): string[] {
    const s = project.skillsRequiered;
    if (s == null || s === '') return [];
    if (Array.isArray(s)) return s.map((x) => String(x).trim()).filter(Boolean);
    return String(s)
      .split(',')
      .map((x) => x.trim())
      .filter(Boolean);
  }

}
