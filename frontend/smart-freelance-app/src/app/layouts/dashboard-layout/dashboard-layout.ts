import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from '../../shared/components/header/header';
import { DashboardFooter } from '../../shared/components/footer/dashboard-footer/dashboard-footer';

@Component({
  selector: 'app-dashboard-layout',
  imports: [RouterOutlet, Header, DashboardFooter],
  templateUrl: './dashboard-layout.html',
  styleUrl: './dashboard-layout.scss',
  standalone: true,
})
export class DashboardLayout {}
