import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const TASK_API = `${environment.apiGatewayUrl}/task/api`;

/** Task entity (matches backend Task). */
export interface Task {
  id: number;
  projectId: number;
  contractId: number | null;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: number | null;
  dueDate: string | null;
  orderIndex: number;
  parentTaskId: number | null;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

/** Task comment (matches backend TaskComment). */
export interface TaskComment {
  id: number;
  taskId: number;
  userId: number;
  message: string;
  createdAt: string;
}

/** Request body for create/update task. */
export interface TaskRequest {
  projectId: number;
  contractId?: number | null;
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: number | null;
  dueDate?: string | null;
  orderIndex?: number | null;
  parentTaskId?: number | null;
  createdBy?: number | null;
}

/** Request body for create/update task comment. */
export interface TaskCommentRequest {
  taskId: number;
  userId: number;
  message: string;
}

/** Filter params for paginated task list. */
export interface TaskFilterParams {
  page?: number;
  size?: number;
  sort?: string;
  projectId?: number | null;
  contractId?: number | null;
  assigneeId?: number | null;
  status?: TaskStatus | null;
  priority?: TaskPriority | null;
  parentId?: number | null;
  search?: string | null;
  dueDateFrom?: string | null;
  dueDateTo?: string | null;
}

/** Spring Data Page response. */
export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

/** Task stats (project or freelancer). */
export interface TaskStatsDto {
  totalTasks: number;
  doneCount: number;
  inProgressCount: number;
  overdueCount: number;
  completionPercentage: number;
}

/** Kanban board: tasks grouped by status. */
export interface TaskBoardDto {
  projectId: number;
  columns?: Record<string, Task[]>;
}

/** Health payload for Task microservice. */
export interface TaskHealthDatabase {
  status: string;
  taskCount?: number;
  error?: string;
}

export interface TaskHealth {
  service: string;
  status: string;
  timestamp: string;
  database?: TaskHealthDatabase;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  constructor(private http: HttpClient) {}

  getFilteredTasks(params: TaskFilterParams): Observable<PageResponse<Task>> {
    const query = new URLSearchParams();
    if (params.page != null) query.set('page', String(params.page));
    if (params.size != null) query.set('size', String(params.size));
    if (params.sort?.trim()) query.set('sort', params.sort.trim());
    if (params.projectId != null) query.set('projectId', String(params.projectId));
    if (params.contractId != null) query.set('contractId', String(params.contractId));
    if (params.assigneeId != null) query.set('assigneeId', String(params.assigneeId));
    if (params.status) query.set('status', params.status);
    if (params.priority) query.set('priority', params.priority);
    if (params.parentId != null) query.set('parentId', String(params.parentId));
    if (params.search?.trim()) query.set('search', params.search.trim());
    if (params.dueDateFrom?.trim()) query.set('dueDateFrom', params.dueDateFrom.trim());
    if (params.dueDateTo?.trim()) query.set('dueDateTo', params.dueDateTo.trim());
    const qs = query.toString();
    const url = qs ? `${TASK_API}/tasks?${qs}` : `${TASK_API}/tasks`;
    return this.http.get<PageResponse<Task>>(url).pipe(
      map((p) => ({
        content: p?.content ?? [],
        totalElements: p?.totalElements ?? 0,
        totalPages: p?.totalPages ?? 0,
        size: p?.size ?? 20,
        number: p?.number ?? 0,
      })),
      catchError((err) => throwError(() => err))
    );
  }

  getTaskById(id: number): Observable<Task | null> {
    return this.http.get<Task>(`${TASK_API}/tasks/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getTasksByProjectId(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${TASK_API}/tasks/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  getTasksByAssigneeId(assigneeId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${TASK_API}/tasks/assignee/${assigneeId}`).pipe(
      catchError(() => of([]))
    );
  }

  getBoardByProject(projectId: number): Observable<TaskBoardDto | null> {
    return this.http.get<TaskBoardDto>(`${TASK_API}/tasks/board/project/${projectId}`).pipe(
      catchError(() => of(null))
    );
  }

  getOverdueTasks(projectId?: number | null, assigneeId?: number | null): Observable<Task[]> {
    const query = new URLSearchParams();
    if (projectId != null) query.set('projectId', String(projectId));
    if (assigneeId != null) query.set('assigneeId', String(assigneeId));
    const qs = query.toString();
    const url = qs ? `${TASK_API}/tasks/overdue?${qs}` : `${TASK_API}/tasks/overdue`;
    return this.http.get<Task[]>(url).pipe(
      catchError(() => of([]))
    );
  }

  getStatsByProject(projectId: number): Observable<TaskStatsDto | null> {
    return this.http.get<TaskStatsDto>(`${TASK_API}/tasks/stats/project/${projectId}`).pipe(
      catchError(() => of(null))
    );
  }

  getStatsByFreelancer(freelancerId: number, from?: string | null, to?: string | null): Observable<TaskStatsDto | null> {
    const query = new URLSearchParams();
    if (from?.trim()) query.set('from', from.trim());
    if (to?.trim()) query.set('to', to.trim());
    const qs = query.toString();
    const url = qs ? `${TASK_API}/tasks/stats/freelancer/${freelancerId}?${qs}` : `${TASK_API}/tasks/stats/freelancer/${freelancerId}`;
    return this.http.get<TaskStatsDto>(url).pipe(
      catchError(() => of(null))
    );
  }

  getStatsDashboard(): Observable<TaskStatsDto | null> {
    return this.http.get<TaskStatsDto>(`${TASK_API}/tasks/stats/dashboard`).pipe(
      catchError(() => of(null))
    );
  }

  createTask(request: TaskRequest): Observable<Task | null> {
    return this.http.post<Task>(`${TASK_API}/tasks`, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  updateTask(id: number, request: TaskRequest): Observable<Task | null> {
    return this.http.put<Task>(`${TASK_API}/tasks/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  patchStatus(id: number, status: TaskStatus): Observable<Task | null> {
    return this.http.patch<Task>(`${TASK_API}/tasks/${id}/status`, null, { params: { status } }).pipe(
      catchError(() => of(null))
    );
  }

  patchAssignee(id: number, assigneeId: number | null): Observable<Task | null> {
    const params: Record<string, string> = {};
    if (assigneeId != null) params['assigneeId'] = String(assigneeId);
    const options = Object.keys(params).length > 0 ? { params } : {};
    return this.http.patch<Task>(`${TASK_API}/tasks/${id}/assignee`, null, options).pipe(
      catchError(() => of(null))
    );
  }

  reorderTasks(taskIds: number[]): Observable<void> {
    return this.http.post<void>(`${TASK_API}/tasks/reorder`, taskIds).pipe(
      catchError(() => of(undefined))
    );
  }

  deleteTask(id: number): Observable<boolean> {
    return this.http.delete(`${TASK_API}/tasks/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getCommentsByTaskId(taskId: number): Observable<TaskComment[]> {
    return this.http.get<TaskComment[]>(`${TASK_API}/task-comments/task/${taskId}`).pipe(
      catchError(() => of([]))
    );
  }

  createComment(request: TaskCommentRequest): Observable<TaskComment | null> {
    return this.http.post<TaskComment>(`${TASK_API}/task-comments`, request).pipe(
      catchError(() => of(null))
    );
  }

  updateComment(id: number, request: TaskCommentRequest): Observable<TaskComment | null> {
    return this.http.put<TaskComment>(`${TASK_API}/task-comments/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  deleteComment(id: number): Observable<boolean> {
    return this.http.delete(`${TASK_API}/task-comments/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getTaskHealth(): Observable<TaskHealth | null> {
    return this.http.get<TaskHealth>(`${TASK_API}/task/health`).pipe(
      catchError(() => of(null))
    );
  }
}
