import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ClientHome } from '../client-home/client-home';
import { FreelancerHome } from '../freelancer-home/freelancer-home';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, ClientHome, FreelancerHome],
  template: `
    @if (userRole === 'CLIENT') {
      <app-client-home />
    } @else {
      <app-freelancer-home />
    }
  `,
  styles: [':host { display: block; }'],
})
export class DashboardHome {
  private auth = inject(AuthService);

  get userRole(): string | null {
    return this.auth.getUserRole();
  }
}
