import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PlanningService, ProgressUpdate, ProgressUpdateRequest } from '../../../core/services/planning.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-planning-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './planning-management.html',
  styleUrl: './planning-management.scss',
})
export class PlanningManagement implements OnInit {
  updates: ProgressUpdate[] = [];
  loading = true;
  errorMessage = '';
  updateToDelete: ProgressUpdate | null = null;
  deleting = false;
  addForm: FormGroup;
  editForm: FormGroup;
  addModalOpen = false;
  editingUpdate: ProgressUpdate | null = null;
  saving = false;
  adding = false;

  constructor(
    private planningService: PlanningService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', Validators.required],
      description: [''],
      progressPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
    this.editForm = this.fb.group({
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', Validators.required],
      description: [''],
      progressPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
  }

  ngOnInit(): void {
    this.loadUpdates();
  }

  loadUpdates(): void {
    this.loading = true;
    this.errorMessage = '';
    this.planningService.getAllProgressUpdates().subscribe({
      next: (list) => {
        this.updates = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load progress updates.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openAdd(): void {
    this.errorMessage = '';
    this.addForm.reset({
      projectId: null,
      freelancerId: null,
      title: '',
      description: '',
      progressPercentage: 0,
    });
    this.addModalOpen = true;
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  buildRequest(v: { projectId: number | null; freelancerId: number | null; title: string; description: string; progressPercentage: number }): ProgressUpdateRequest {
    return {
      projectId: Number(v.projectId),
      contractId: null,
      freelancerId: Number(v.freelancerId),
      title: v.title.trim(),
      description: v.description?.trim() || null,
      progressPercentage: Number(v.progressPercentage),
    };
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const request = this.buildRequest(v);
    this.adding = true;
    this.planningService.createProgressUpdate(request).subscribe({
      next: () => {
        this.adding = false;
        this.addModalOpen = false;
        this.loadUpdates();
        this.cdr.detectChanges();
      },
      error: () => {
        this.adding = false;
        this.errorMessage = 'Failed to create progress update.';
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(update: ProgressUpdate): void {
    this.editingUpdate = update;
    this.updateToDelete = null;
    this.editForm.patchValue({
      projectId: update.projectId,
      freelancerId: update.freelancerId,
      title: update.title,
      description: update.description ?? '',
      progressPercentage: update.progressPercentage,
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingUpdate = null;
  }

  saveEdit(): void {
    if (!this.editingUpdate?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const request = this.buildRequest(v);
    this.saving = true;
    this.planningService.updateProgressUpdate(this.editingUpdate.id, request).subscribe({
      next: (updated) => {
        this.saving = false;
        if (updated) {
          const idx = this.updates.findIndex((u) => u.id === this.editingUpdate!.id);
          if (idx !== -1) this.updates[idx] = { ...this.updates[idx], ...updated };
          this.editingUpdate = null;
        } else this.errorMessage = 'Failed to update progress update.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update progress update.';
        this.cdr.detectChanges();
      },
    });
  }

  openDeleteModal(update: ProgressUpdate): void {
    this.updateToDelete = update;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.updateToDelete = null;
  }

  doDelete(): void {
    if (!this.updateToDelete?.id) return;
    this.deleting = true;
    this.planningService.deleteProgressUpdate(this.updateToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.updateToDelete = null;
        if (ok) this.loadUpdates();
        else this.errorMessage = 'Failed to delete progress update.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete progress update.';
        this.cdr.detectChanges();
      },
    });
  }
}
