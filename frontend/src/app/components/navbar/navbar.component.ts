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
        <span class="brand-text">BICA Tools</span>
      </a>

      <span class="spacer"></span>

      <!-- Desktop nav -->
      <nav class="nav-links desktop-nav">
        <!-- Explore dropdown -->
        <div class="nav-dropdown">
          <button mat-button class="dropdown-trigger"
                  [class.active]="isExploreActive"
                  (mouseenter)="exploreOpen = true"
                  (mouseleave)="exploreOpen = false">
            Explore <mat-icon class="dropdown-arrow">expand_more</mat-icon>
          </button>
          <div class="dropdown-menu"
               [class.open]="exploreOpen"
               (mouseenter)="exploreOpen = true"
               (mouseleave)="exploreOpen = false">
            <a routerLink="/intro" routerLinkActive="active" (click)="exploreOpen = false">Visual Intro</a>
            <a routerLink="/map" routerLinkActive="active" (click)="exploreOpen = false">Research Map</a>
            <a routerLink="/documentation" routerLinkActive="active" (click)="exploreOpen = false">Documentation</a>
            <a routerLink="/tutorials" routerLinkActive="active" (click)="exploreOpen = false">Tutorials</a>
            <a routerLink="/faq" routerLinkActive="active" (click)="exploreOpen = false">FAQ</a>
            <a routerLink="/pipeline" routerLinkActive="active" (click)="exploreOpen = false">Pipeline</a>
          </div>
        </div>

        <!-- Tools dropdown -->
        <div class="nav-dropdown">
          <button mat-button class="dropdown-trigger"
                  [class.active]="isToolsActive"
                  (mouseenter)="toolsOpen = true"
                  (mouseleave)="toolsOpen = false">
            Tools <mat-icon class="dropdown-arrow">expand_more</mat-icon>
          </button>
          <div class="dropdown-menu"
               [class.open]="toolsOpen"
               (mouseenter)="toolsOpen = true"
               (mouseleave)="toolsOpen = false">
            <a routerLink="/tools/analyzer" routerLinkActive="active" (click)="toolsOpen = false">Analyzer</a>
            <a routerLink="/tools/global-analyzer" routerLinkActive="active" (click)="toolsOpen = false">Global Types</a>
            <a routerLink="/tools/test-generator" routerLinkActive="active" (click)="toolsOpen = false">Test Generator</a>
            <a routerLink="/tools/compare" routerLinkActive="active" (click)="toolsOpen = false">Compare</a>
            <a routerLink="/tools/composition" routerLinkActive="active" (click)="toolsOpen = false">Composition</a>
          </div>
        </div>

        <a mat-button routerLink="/theory" routerLinkActive="active">Research</a>
        <a mat-button routerLink="/benchmarks" routerLinkActive="active">Benchmarks</a>

        <!-- About dropdown -->
        <div class="nav-dropdown">
          <button mat-button class="dropdown-trigger"
                  [class.active]="isAboutActive"
                  (mouseenter)="aboutOpen = true"
                  (mouseleave)="aboutOpen = false">
            About <mat-icon class="dropdown-arrow">expand_more</mat-icon>
          </button>
          <div class="dropdown-menu"
               [class.open]="aboutOpen"
               (mouseenter)="aboutOpen = true"
               (mouseleave)="aboutOpen = false">
            <a routerLink="/publications" routerLinkActive="active" (click)="aboutOpen = false">Publications</a>
            <a routerLink="/about" routerLinkActive="active" (click)="aboutOpen = false">About</a>
            <a routerLink="/dashboard" routerLinkActive="active" (click)="aboutOpen = false">Dashboard</a>
          </div>
        </div>

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
        <span class="mobile-section-label">Explore</span>
        <a routerLink="/intro" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Visual Intro</a>
        <a routerLink="/map" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Research Map</a>
        <a routerLink="/documentation" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Documentation</a>
        <a routerLink="/tutorials" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Tutorials</a>
        <a routerLink="/faq" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">FAQ</a>
        <a routerLink="/pipeline" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Pipeline</a>
        <span class="mobile-section-label">Tools</span>
        <a routerLink="/tools/analyzer" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Analyzer</a>
        <a routerLink="/tools/global-analyzer" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Global Types</a>
        <a routerLink="/tools/test-generator" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Test Generator</a>
        <a routerLink="/tools/compare" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Compare</a>
        <a routerLink="/tools/composition" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Composition</a>
        <a routerLink="/theory" routerLinkActive="active" (click)="closeMenu()">Research</a>
        <a routerLink="/benchmarks" routerLinkActive="active" (click)="closeMenu()">Benchmarks</a>
        <span class="mobile-section-label">About</span>
        <a routerLink="/publications" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Publications</a>
        <a routerLink="/about" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">About</a>
        <a routerLink="/dashboard" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Dashboard</a>
        <a (click)="closeMenu(); logoutClicked.emit()" style="cursor:pointer">Logout</a>
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
    .nav-links a {
      color: inherit;
    }
    .nav-links a.active {
      border-bottom: 2px solid white;
    }
    /* Dropdown */
    .nav-dropdown {
      position: relative;
      display: inline-block;
    }
    .dropdown-trigger {
      color: inherit;
      display: inline-flex;
      align-items: center;
    }
    .dropdown-trigger.active {
      border-bottom: 2px solid white;
    }
    .dropdown-arrow {
      font-size: 18px;
      width: 18px;
      height: 18px;
      margin-left: -2px;
      transition: transform 0.2s;
    }
    .dropdown-menu {
      display: none;
      position: absolute;
      top: 100%;
      left: 0;
      min-width: 180px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.15);
      padding: 6px 0;
      z-index: 1001;
    }
    .dropdown-menu.open {
      display: flex;
      flex-direction: column;
    }
    .dropdown-menu a {
      padding: 10px 20px;
      color: rgba(0,0,0,0.8);
      text-decoration: none;
      font-size: 14px;
      transition: background 0.15s;
    }
    .dropdown-menu a:hover {
      background: rgba(0,0,0,0.04);
    }
    .dropdown-menu a.active {
      color: var(--brand-primary, #4338ca);
      font-weight: 500;
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
    .mobile-section-label {
      padding: 10px 24px 4px;
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 1px;
      opacity: 0.5;
      color: white;
    }
    .mobile-indent {
      padding-left: 40px !important;
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
  exploreOpen = false;
  toolsOpen = false;
  aboutOpen = false;
  private routerSub?: Subscription;

  constructor(private router: Router) {}

  get isExploreActive(): boolean {
    const url = this.router.url;
    return url.startsWith('/intro') || url.startsWith('/map')
        || url.startsWith('/documentation') || url.startsWith('/tutorials')
        || url.startsWith('/faq') || url.startsWith('/pipeline');
  }

  get isToolsActive(): boolean {
    return this.router.url.startsWith('/tools/');
  }

  get isAboutActive(): boolean {
    const url = this.router.url;
    return url.startsWith('/publications') || url.startsWith('/about')
        || url.startsWith('/dashboard');
  }

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
