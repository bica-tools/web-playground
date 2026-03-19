import { Injectable, signal, computed } from '@angular/core';

export interface UserProgress {
  xp: number;
  tutorialsCompleted: string[];
  gamesWon: number;
  analysesRun: number;
  counterexamplesFixed: number;
}

export interface Level {
  name: string;
  minXp: number;
}

const LEVELS: Level[] = [
  { name: 'Novice', minXp: 0 },
  { name: 'Apprentice', minXp: 200 },
  { name: 'Practitioner', minXp: 500 },
  { name: 'Expert', minXp: 1000 },
  { name: 'Master', minXp: 2000 },
];

const STORAGE_KEY = 'bica-progress';

@Injectable({ providedIn: 'root' })
export class ProgressService {
  private _progress = signal<UserProgress>(this.load());

  readonly progress = this._progress.asReadonly();
  readonly xp = computed(() => this._progress().xp);
  readonly level = computed(() => {
    const xp = this._progress().xp;
    let current = LEVELS[0];
    for (const l of LEVELS) {
      if (xp >= l.minXp) current = l;
    }
    return current;
  });
  readonly nextLevel = computed(() => {
    const xp = this._progress().xp;
    for (const l of LEVELS) {
      if (xp < l.minXp) return l;
    }
    return null;
  });
  readonly progressPercent = computed(() => {
    const curr = this.level();
    const next = this.nextLevel();
    if (!next) return 100;
    const xp = this._progress().xp;
    return Math.round(((xp - curr.minXp) / (next.minXp - curr.minXp)) * 100);
  });

  addXp(amount: number): void {
    this.update(p => ({ ...p, xp: p.xp + amount }));
  }

  completeTutorial(id: string): void {
    const p = this._progress();
    if (p.tutorialsCompleted.includes(id)) return;
    this.update(curr => ({
      ...curr,
      tutorialsCompleted: [...curr.tutorialsCompleted, id],
      xp: curr.xp + 50,
    }));
  }

  recordGameWin(): void {
    this.update(p => ({ ...p, gamesWon: p.gamesWon + 1, xp: p.xp + 25 }));
  }

  recordAnalysis(): void {
    this.update(p => ({ ...p, analysesRun: p.analysesRun + 1, xp: p.xp + 5 }));
  }

  recordCounterexampleFix(): void {
    this.update(p => ({ ...p, counterexamplesFixed: p.counterexamplesFixed + 1, xp: p.xp + 100 }));
  }

  private update(fn: (p: UserProgress) => UserProgress): void {
    const updated = fn(this._progress());
    this._progress.set(updated);
    this.save(updated);
  }

  private load(): UserProgress {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) return JSON.parse(stored);
    } catch {}
    return { xp: 0, tutorialsCompleted: [], gamesWon: 0, analysesRun: 0, counterexamplesFixed: 0 };
  }

  private save(p: UserProgress): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(p));
    } catch {}
  }
}
