import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  TaskService,
  Task,
  TaskRequest,
  TaskFilterParams,
  TaskStatsDto,
  TaskHealth,
  PageResponse,
} from '../../../core/services/task.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-task-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './task-management.html',
  styleUrl: './task-management.scss',
})
export class TaskManagement implements OnInit {
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  taskToDelete: Task | null = null;
  deleting = false;
  addModalOpen = false;
  editingTask: Task | null = null;
  saving = false;
  adding = false;
  addForm: FormGroup;
  editForm: FormGroup;
  filterForm: FormGroup;

  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  taskHealth: TaskHealth | null = null;
  healthLoading = false;
  dashboardStats: TaskStatsDto | null = null;
  projectIdToTitle: Record<number, string> = {};
  assigneeIdToName: Record<number, string> = {};
  projectsForAdd: Project[] = [];
  freelancersForAdd: User[] = [];

  constructor(
    private taskService: TaskService,
    private userService: UserService,
    private projectService: ProjectService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', Validators.required],
      description: [''],
      status: ['TODO'],
      priority: ['MEDIUM'],
      assigneeId: [null as number | null],
      dueDate: [null as string | null],
    });
    this.editForm = this.fb.group({
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', Validators.required],
      description: [''],
      status: ['TODO'],
      priority: ['MEDIUM'],
      assigneeId: [null as number | null],
      dueDate: [null as string | null],
    });
    this.filterForm = this.fb.group({
      projectId: [null as number | null],
      assigneeId: [null as number | null],
      status: [null as string | null],
      search: [''],
    });
  }

  ngOnInit(): void {
    this.loadHealth();
    this.loadStats();
    this.loadTasks();
    this.userService.getAll().subscribe((users) => {
      users.forEach((u) => {
        this.assigneeIdToName[u.id] = `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || `User ${u.id}`;
      });
      this.freelancersForAdd = users.filter((u) => u.role === 'FREELANCER');
    });
    this.projectService.getAllProjects().subscribe((projects) => {
      projects.forEach((p) => {
        if (p.id != null) this.projectIdToTitle[p.id] = p.title?.trim() || `Project ${p.id}`;
      });
      this.projectsForAdd = projects.filter((p) => p.id != null);
    });
  }

  loadHealth(): void {
    this.healthLoading = true;
    this.taskService.getTaskHealth().subscribe({
      next: (h) => {
        this.taskHealth = h ?? null;
        this.healthLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.healthLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadStats(): void {
    this.taskService.getStatsDashboard().subscribe({
      next: (stats) => {
        this.dashboardStats = stats ?? null;
        this.cdr.detectChanges();
      },
    });
  }

  loadTasks(): void {
    this.loading = true;
    const v = this.filterForm.getRawValue();
    const params: TaskFilterParams = {
      page: this.page,
      size: this.size,
      sort: 'createdAt,desc',
      projectId: v.projectId ?? null,
      assigneeId: v.assigneeId ?? null,
      status: v.status ? (v.status as any) : null,
      search: v.search?.trim() || null,
    };
    this.taskService.getFilteredTasks(params).subscribe({
      next: (p: PageResponse<Task>) => {
        this.tasks = p.content ?? [];
        this.totalElements = p.totalElements ?? 0;
        this.totalPages = p.totalPages ?? 0;
        this.loading = false;
        this.errorMessage = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load tasks. Ensure the Task microservice is running (port 8091) and the API Gateway (8078) is up.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onFilterChange(): void {
    this.page = 0;
    this.loadTasks();
  }

  clearFilters(): void {
    this.filterForm.reset({ projectId: null, assigneeId: null, status: null, search: '' });
    this.page = 0;
    this.loadTasks();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadTasks();
  }

  getProjectTitle(id: number): string {
    return this.projectIdToTitle[id] ?? `Project ${id}`;
  }

  getAssigneeName(id: number | null): string {
    return id != null ? (this.assigneeIdToName[id] ?? `User ${id}`) : '—';
  }

  getFreelancerLabel(u: User): string {
    const name = `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || `User ${u.id}`;
    return `${name} (#${u.id})`;
  }

  getProjectLabel(p: Project): string {
    return p.title?.trim() || `Project ${p.id}`;
  }

  formatDueDate(d: string | null): string {
    return d ? new Date(d).toLocaleDateString() : '—';
  }

  openAdd(): void {
    this.addForm.reset({
      projectId: null,
      title: '',
      description: '',
      status: 'TODO',
      priority: 'MEDIUM',
      assigneeId: null,
      dueDate: null,
    });
    this.addModalOpen = true;
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const request: TaskRequest = {
      projectId: Number(v.projectId),
      title: v.title.trim(),
      description: v.description?.trim() || null,
      status: v.status as any,
      priority: v.priority as any,
      assigneeId: v.assigneeId,
      dueDate: v.dueDate ? v.dueDate : null,
    };
    this.adding = true;
    this.taskService.createTask(request).subscribe({
      next: () => {
        this.adding = false;
        this.addModalOpen = false;
        this.loadTasks();
        this.loadStats();
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create task.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(t: Task): void {
    this.editingTask = t;
    this.editForm.patchValue({
      projectId: t.projectId,
      title: t.title,
      description: t.description ?? '',
      status: t.status,
      priority: t.priority,
      assigneeId: t.assigneeId,
      dueDate: t.dueDate ? t.dueDate.split('T')[0] : null,
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingTask = null;
  }

  saveEdit(): void {
    if (!this.editingTask?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const request: TaskRequest = {
      projectId: Number(v.projectId),
      title: v.title.trim(),
      description: v.description?.trim() || null,
      status: v.status as any,
      priority: v.priority as any,
      assigneeId: v.assigneeId,
      dueDate: v.dueDate ? v.dueDate : null,
    };
    this.saving = true;
    this.taskService.updateTask(this.editingTask.id, request).subscribe({
      next: () => {
        this.saving = false;
        this.editingTask = null;
        this.loadTasks();
        this.loadStats();
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update task.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(t: Task): void {
    this.taskToDelete = t;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.taskToDelete = null;
  }

  doDelete(): void {
    if (!this.taskToDelete?.id) return;
    this.deleting = true;
    this.taskService.deleteTask(this.taskToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.taskToDelete = null;
        if (ok) this.loadTasks();
        else this.errorMessage = 'Failed to delete task.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete task.';
        this.cdr.detectChanges();
      },
    });
  }
}
