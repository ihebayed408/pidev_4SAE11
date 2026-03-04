import { Component, input, output } from '@angular/core';
import { ProjectFeedCard } from './project-feed-card/project-feed-card.component';
import { ProjectFeed } from '../../models/project-feed';

@Component({
  selector: 'app-projects-feed',
  standalone: true,
  imports: [ProjectFeedCard],
  templateUrl: './projects-feed.component.html',
  styleUrl: './projects-feed.component.scss',
})
export class ProjectsFeed {
  projects   = input.required<ProjectFeed[]>();
  totalCount = input<number>(0);
  currentPage = input<number>(1);
  totalPages  = input<number>(1);
  pageSize    = input<number>(6);
  startIndex  = input<number>(0);
  endIndex    = input<number>(0);

  pageChange = output<number>();

  getAnimDelay(index: number): string {
    const delay = (index % this.pageSize()) * 100;
    return `${Math.min(delay, 300)}ms`;
  }

  goToPage(page: number): void {
    this.pageChange.emit(page);
  }

  /** Page numbers to show (with ellipsis for long ranges) */
  get pageNumbers(): number[] {
    const total = this.totalPages();
    const curr = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    const pages: number[] = [];
    if (curr <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (curr >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, curr - 1, curr, curr + 1, -1, total);
    }
    return pages;
  }
}
