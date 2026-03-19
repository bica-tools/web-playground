import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <mat-toolbar color="primary" class="navbar" role="banner">
      <a routerLink="/" class="brand" aria-label="BICA Tools home">
        <svg class="brand-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="28" height="28" aria-hidden="true">
          <line x1="16" y1="4" x2="6" y2="16" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="16" y1="4" x2="26" y2="16" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="6" y1="16" x2="16" y2="28" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="26" y1="16" x2="16" y2="28" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <circle cx="16" cy="4" r="3" fill="white"/>
          <circle cx="6" cy="16" r="3" fill="white"/>
          <circle cx="26" cy="16" r="3" fill="white"/>
          <circle cx="16" cy="28" r="3" fill="white"/>
        </svg>
        <span class="brand-text">BICA Tools</span>
      </a>

      <span class="spacer"></span>

      <!-- Desktop nav -->
      <nav class="nav-links desktop-nav" aria-label="Main navigation">
        <a mat-button routerLink="/tools/analyzer" routerLinkActive="active">Playground</a>
        <a mat-button routerLink="/games" routerLinkActive="active">Games</a>
        <a mat-button routerLink="/theory" routerLinkActive="active">Research</a>
        <a mat-button routerLink="/benchmarks" routerLinkActive="active">Benchmarks</a>

      </nav>

      <!-- Mobile hamburger -->
      <button mat-icon-button class="mobile-menu-btn"
              (click)="toggleMenu()"
              [attr.aria-expanded]="isMenuOpen"
              aria-controls="mobile-nav"
              aria-label="Toggle navigation menu">
        <mat-icon>{{ isMenuOpen ? 'close' : 'menu' }}</mat-icon>
      </button>
    </mat-toolbar>

    <!-- Mobile nav overlay -->
    @if (isMenuOpen) {
      <nav id="mobile-nav" class="mobile-nav" aria-label="Mobile navigation">
        <a routerLink="/tools/analyzer" routerLinkActive="active" (click)="closeMenu()">Playground</a>
        <a routerLink="/games" routerLinkActive="active" (click)="closeMenu()">Games</a>
        <a routerLink="/theory" routerLinkActive="active" (click)="closeMenu()">Research</a>
        <a routerLink="/benchmarks" routerLinkActive="active" (click)="closeMenu()">Benchmarks</a>
      </nav>
    }
  `,
  styles: [`
    .navbar {
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }
    .brand {
      display: flex;
      align-items: center;
      text-decoration: none;
      color: inherit;
      gap: 8px;
    }
    .brand-icon {
      flex-shrink: 0;
    }
    .brand-text {
      font-size: 18px;
      font-weight: 500;
    }
    .spacer {
      flex: 1;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .nav-links a {
      color: inherit;
    }
    .nav-links a.active {
      border-bottom: 2px solid white;
    }
    .mobile-menu-btn {
      display: none;
      color: inherit;
    }
    .mobile-nav {
      display: none;
      flex-direction: column;
      background: var(--mat-sys-primary);
      position: sticky;
      top: 64px;
      z-index: 999;
    }
    .mobile-nav a {
      padding: 12px 24px;
      color: white;
      text-decoration: none;
      font-size: 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }
    .mobile-nav a:hover, .mobile-nav a.active {
      background: rgba(255, 255, 255, 0.1);
    }

    @media (max-width: 768px) {
      .desktop-nav {
        display: none;
      }
      .mobile-menu-btn {
        display: inline-flex;
      }
      .mobile-nav {
        display: flex;
      }
    }
  `],
})
export class NavbarComponent implements OnInit, OnDestroy {
  isMenuOpen = false;
  private routerSub?: Subscription;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.routerSub = this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.closeMenu());
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(): void {
    this.isMenuOpen = false;
  }
}
