import { Component, inject, computed } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { NavbarComponent } from './components/navbar/navbar.component';
import { FooterComponent } from './components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  template: `
    @if (!isEmbed()) { <app-navbar /> }
    <main [class]="isEmbed() ? 'embed-content' : 'main-content'" role="main">
      <router-outlet />
    </main>
    @if (!isEmbed()) { <app-footer /> }
  `,
  styles: [`
    .main-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px 16px;
      min-height: calc(100vh - 64px - 100px);
    }
    .embed-content {
      padding: 0;
      margin: 0;
    }
  `],
})
export class App {
  private router = inject(Router);
  private url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: '' },
  );
  isEmbed = computed(() => this.url().startsWith('/embed'));
}
