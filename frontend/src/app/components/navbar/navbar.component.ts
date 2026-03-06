import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
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
    <mat-toolbar color="primary" class="navbar">
      <a routerLink="/" class="brand">
        <svg class="brand-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="28" height="28">
          <line x1="16" y1="4" x2="6" y2="16" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="16" y1="4" x2="26" y2="16" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="6" y1="16" x2="16" y2="28" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="26" y1="16" x2="16" y2="28" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <circle cx="16" cy="4" r="3" fill="white"/>
          <circle cx="6" cy="16" r="3" fill="white"/>
          <circle cx="26" cy="16" r="3" fill="white"/>
          <circle cx="16" cy="28" r="3" fill="white"/>
        </svg>
        <span class="brand-text">BICA Reborn</span>
      </a>

      <span class="spacer"></span>

      <!-- Desktop nav -->
      <nav class="nav-links desktop-nav">
        <a mat-button routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Home</a>
        <a mat-button routerLink="/tools/analyzer" routerLinkActive="active">Analyzer</a>
        <a mat-button routerLink="/benchmarks" routerLinkActive="active">Benchmarks</a>
        <a mat-button routerLink="/pipeline" routerLinkActive="active">Pipeline</a>
        <a mat-button routerLink="/publications" routerLinkActive="active">Publications</a>
        <a mat-button routerLink="/tutorials" routerLinkActive="active">Tutorials</a>
        <a mat-button routerLink="/documentation" routerLinkActive="active">Docs</a>
        <a mat-button routerLink="/faq" routerLinkActive="active">FAQ</a>
        <a mat-button routerLink="/about" routerLinkActive="active">About</a>
        <a mat-icon-button routerLink="/dashboard" routerLinkActive="active" class="dashboard-btn" title="Dashboard">
          <mat-icon>dashboard</mat-icon>
        </a>
        <button mat-icon-button class="logout-btn" title="Logout" (click)="logoutClicked.emit()">
          <mat-icon>logout</mat-icon>
        </button>
      </nav>

      <!-- Mobile hamburger -->
      <button mat-icon-button class="mobile-menu-btn" (click)="toggleMenu()">
        <mat-icon>{{ isMenuOpen ? 'close' : 'menu' }}</mat-icon>
      </button>
    </mat-toolbar>

    <!-- Mobile nav overlay -->
    @if (isMenuOpen) {
      <nav class="mobile-nav">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" (click)="closeMenu()">Home</a>
        <a routerLink="/tools/analyzer" routerLinkActive="active" (click)="closeMenu()">Analyzer</a>
        <a routerLink="/benchmarks" routerLinkActive="active" (click)="closeMenu()">Benchmarks</a>
        <a routerLink="/pipeline" routerLinkActive="active" (click)="closeMenu()">Pipeline</a>
        <a routerLink="/publications" routerLinkActive="active" (click)="closeMenu()">Publications</a>
        <a routerLink="/tutorials" routerLinkActive="active" (click)="closeMenu()">Tutorials</a>
        <a routerLink="/documentation" routerLinkActive="active" (click)="closeMenu()">Documentation</a>
        <a routerLink="/faq" routerLinkActive="active" (click)="closeMenu()">FAQ</a>
        <a routerLink="/about" routerLinkActive="active" (click)="closeMenu()">About</a>
        <a routerLink="/dashboard" routerLinkActive="active" (click)="closeMenu()">Dashboard</a>
        <a (click)="closeMenu(); logoutClicked.emit()" style="cursor:pointer">Logout</a>
      </nav>
    }
  `,
  styles: [`
    .navbar {
      position: sticky;
      top: 0;
      z-index: 1000;
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
    .nav-links a {
      color: inherit;
    }
    .nav-links a.active {
      border-bottom: 2px solid white;
    }
    .dashboard-btn {
      opacity: 0.7;
      margin-left: 4px;
    }
    .dashboard-btn:hover, .dashboard-btn.active {
      opacity: 1;
    }
    .logout-btn {
      opacity: 0.7;
      margin-left: 4px;
    }
    .logout-btn:hover {
      opacity: 1;
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
  @Output() logoutClicked = new EventEmitter<void>();
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
