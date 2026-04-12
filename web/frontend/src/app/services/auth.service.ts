import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface LoginResponse {
  token: string;
  expiresAt: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'bica_token';
  private readonly EXPIRY_KEY = 'bica_token_expiry';

  private _isLoggedIn = signal(this.checkStoredToken());
  readonly isLoggedIn = this._isLoggedIn.asReadonly();

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        localStorage.setItem(this.EXPIRY_KEY, String(res.expiresAt));
        this._isLoggedIn.set(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EXPIRY_KEY);
    this._isLoggedIn.set(false);
  }

  getToken(): string | null {
    if (!this.checkStoredToken()) {
      this.logout();
      return null;
    }
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private checkStoredToken(): boolean {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const expiry = localStorage.getItem(this.EXPIRY_KEY);
    if (!token || !expiry) return false;
    return Date.now() < Number(expiry);
  }
}
