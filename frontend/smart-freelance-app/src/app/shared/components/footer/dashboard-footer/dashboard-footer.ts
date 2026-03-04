import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-footer',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './dashboard-footer.html',
  styleUrl: './dashboard-footer.scss',
})
export class DashboardFooter {
  private auth = inject(AuthService);
  currentYear = new Date().getFullYear();

  get userRole(): string | null {
    return this.auth.getUserRole();
  }

  get isClient(): boolean {
    return this.userRole === 'CLIENT';
  }

  get isFreelancer(): boolean {
    return this.userRole === 'FREELANCER';
  }
}
