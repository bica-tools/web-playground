import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from './components/navbar/navbar.component';
import { FooterComponent } from './components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent, FormsModule],
  template: `
    @if (authenticated()) {
      <app-navbar (logoutClicked)="logout()" />
      <main class="main-content" role="main">
        <router-outlet />
      </main>
      <app-footer />
    } @else {
      <div class="auth-gate" role="dialog" aria-label="Authentication required">
        <div class="auth-box">
          <h1>BICA Tools</h1>
          <p>Session Types as Algebraic Reticulates</p>
          <form (ngSubmit)="login()" aria-label="Sign in">
            <label for="auth-password" class="visually-hidden">Password</label>
            <input
              id="auth-password"
              type="password"
              [(ngModel)]="password"
              name="password"
              placeholder="Enter password"
              autofocus
              autocomplete="current-password"
              [attr.aria-invalid]="error() ? 'true' : null"
              aria-describedby="auth-error"
            />
            @if (error()) {
              <span id="auth-error" class="auth-error" role="alert">Incorrect password</span>
            }
            <button type="submit">Enter</button>
          </form>
        </div>
      </div>
    }
  `,
  styles: [`
    .main-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px 16px;
      min-height: calc(100vh - 64px - 100px);
    }
    .auth-gate {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, var(--brand-primary-dark, #312e81), var(--brand-primary-light, #6366f1));
      color: #fff;
    }
    .auth-box {
      text-align: center;
      padding: 48px;
    }
    .auth-box h1 {
      font-size: 32px;
      font-weight: 600;
      margin: 0 0 8px;
    }
    .auth-box p {
      opacity: 0.8;
      margin: 0 0 32px;
    }
    .auth-box input {
      display: block;
      width: 260px;
      margin: 0 auto 12px;
      padding: 10px 16px;
      border: 1px solid rgba(255,255,255,0.3);
      border-radius: 6px;
      background: rgba(255,255,255,0.15);
      color: #fff;
      font-size: 16px;
      text-align: center;
    }
    .auth-box input::placeholder {
      color: rgba(255,255,255,0.7);
    }
    .auth-box button {
      padding: 10px 32px;
      border: none;
      border-radius: 6px;
      background: #fff;
      color: var(--brand-primary-dark, #312e81);
      font-size: 16px;
      font-weight: 500;
      cursor: pointer;
    }
    .auth-box button:hover {
      opacity: 0.9;
    }
    .auth-error {
      display: block;
      color: #fecaca;
      font-size: 14px;
      margin-bottom: 12px;
    }
    .visually-hidden {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  `],
})
export class App {
  readonly authenticated = signal(sessionStorage.getItem('auth') === '1');
  readonly error = signal(false);
  password = '';

  login(): void {
    if (this.password === 'reticulate') {
      sessionStorage.setItem('auth', '1');
      this.authenticated.set(true);
    } else {
      this.error.set(true);
    }
  }

  logout(): void {
    sessionStorage.removeItem('auth');
    this.authenticated.set(false);
    this.password = '';
    this.error.set(false);
  }
}
