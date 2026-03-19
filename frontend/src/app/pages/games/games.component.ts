import {
  Component,
  signal,
  computed,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
} from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FadeInDirective } from '../../shared/fade-in.directive';
import { ApiService } from '../../services/api.service';
import { GameDataResponse, GameNode, GameEdge } from '../../models/api.models';

/* ═══════════════════════════════════════════════════════════════ */
/* Protocol catalogue — small protocols perfect for a quick game  */
/* ═══════════════════════════════════════════════════════════════ */
interface ProtocolOption {
  name: string;
  typeString: string;
  difficulty: string;
}

const PROTOCOLS: ProtocolOption[] = [
  { name: 'The Fork', typeString: '&{a: end, b: end}', difficulty: 'Easy' },
  {
    name: 'The Oracle',
    typeString: '&{ask: +{YES: end, NO: end}}',
    difficulty: 'Easy',
  },
  {
    name: 'The Vault',
    typeString: '&{auth: +{OK: &{balance: end, withdraw: +{OK: end, DENIED: end}}, FAIL: end}}',
    difficulty: 'Medium',
  },
  {
    name: 'Java Iterator',
    typeString: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}',
    difficulty: 'Medium',
  },
  {
    name: 'File Object',
    typeString: '&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}',
    difficulty: 'Medium',
  },
  {
    name: 'SMTP',
    typeString:
      '&{connect: &{ehlo: rec X . &{mail: &{rcpt: &{data: +{OK: X, ERR: X}}}, quit: end}}}',
    difficulty: 'Hard',
  },
];

/* ═══════════════════════════════════════════════════════════════ */
/* Move history entry                                             */
/* ═══════════════════════════════════════════════════════════════ */
interface MoveEntry {
  label: string;
  type: 'client' | 'server' | 'auto';
}

@Component({
  selector: 'app-games',
  standalone: true,
  imports: [NgClass, FormsModule, FadeInDirective],
  templateUrl: './games.component.html',
  styleUrl: './games.component.scss',
})
export class GamesComponent implements AfterViewInit, OnDestroy {
  @ViewChild('boardSvg') boardSvgRef!: ElementRef<SVGSVGElement>;

  /* ── Protocol selector ─────────────────────────────── */
  readonly protocols = PROTOCOLS;
  selectedProtocol = signal(0);
  customType = signal('');

  /* ── Game data from API ────────────────────────────── */
  gameData = signal<GameDataResponse | null>(null);
  loading = signal(false);
  error = signal('');

  /* ── Game state ────────────────────────────────────── */
  current = signal(-1);
  moveHistory = signal<MoveEntry[]>([]);
  turnCount = signal(0);
  traversedEdges = signal<Set<string>>(new Set());
  isFinished = signal(false);
  mode = signal<'cooperative' | 'adversarial'>('cooperative');

  /* ── Computed ──────────────────────────────────────── */
  nodeMap = computed(() => {
    const map: Record<number, GameNode> = {};
    for (const n of this.gameData()?.nodes ?? []) map[n.id] = n;
    return map;
  });

  availableMoves = computed(() => {
    const data = this.gameData();
    if (!data) return [];
    return data.edges.filter((e) => e.src === this.current());
  });

  whoseTurn = computed<'client' | 'server' | 'auto' | 'none'>(() => {
    const moves = this.availableMoves();
    if (moves.length === 0) return 'none';
    if (moves.length === 1) return 'auto';
    if (moves.some((e) => e.isSelection)) return 'server';
    return 'client';
  });

  boardHeight = computed(() => {
    const data = this.gameData();
    if (!data || data.nodes.length === 0) return 400;
    return Math.max(400, Math.max(...data.nodes.map((n) => n.y)) + 80);
  });

  victoryEfficiency = computed(() => {
    const tc = this.turnCount();
    return Math.max(0, 100 - (tc - 1) * 10);
  });

  victoryPath = computed(() => {
    return this.moveHistory()
      .map((m) => m.label)
      .join(' \u2192 ');
  });

  private autoTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private api: ApiService) {}

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    if (this.autoTimer) clearTimeout(this.autoTimer);
  }

  /* ── Actions ───────────────────────────────────────── */

  startGame(): void {
    const idx = this.selectedProtocol();
    const proto = this.protocols[idx];
    const typeStr = this.customType() || proto.typeString;
    this.loadGame(typeStr);
  }

  startWithType(typeString: string): void {
    this.customType.set(typeString);
    this.loadGame(typeString);
  }

  private loadGame(typeString: string): void {
    this.loading.set(true);
    this.error.set('');
    this.isFinished.set(false);
    this.moveHistory.set([]);
    this.turnCount.set(0);
    this.traversedEdges.set(new Set());

    this.api.gameData(typeString).subscribe({
      next: (data) => {
        this.gameData.set(data);
        this.current.set(data.top);
        this.loading.set(false);
        this.scheduleAutoMove();
      },
      error: (err) => {
        this.error.set(err?.error?.error ?? 'Failed to load game data');
        this.loading.set(false);
      },
    });
  }

  makeMove(edge: GameEdge): void {
    if (this.autoTimer) {
      clearTimeout(this.autoTimer);
      this.autoTimer = null;
    }

    const turn = this.whoseTurn();
    const moveType: 'client' | 'server' | 'auto' =
      turn === 'auto' ? 'auto' : edge.isSelection ? 'server' : 'client';

    const newTraversed = new Set(this.traversedEdges());
    newTraversed.add(`${edge.src}-${edge.label}-${edge.tgt}`);
    this.traversedEdges.set(newTraversed);

    this.moveHistory.update((h) => [...h, { label: edge.label, type: moveType }]);
    this.current.set(edge.tgt);
    this.turnCount.update((n) => n + 1);

    if (edge.tgt === this.gameData()!.bottom) {
      this.isFinished.set(true);
      return;
    }

    this.scheduleAutoMove();
  }

  restart(): void {
    if (this.autoTimer) clearTimeout(this.autoTimer);
    const data = this.gameData();
    if (!data) return;
    this.current.set(data.top);
    this.moveHistory.set([]);
    this.turnCount.set(0);
    this.traversedEdges.set(new Set());
    this.isFinished.set(false);
    this.scheduleAutoMove();
  }

  /* ── Auto / AI moves ───────────────────────────────── */

  private scheduleAutoMove(): void {
    this.autoTimer = setTimeout(() => {
      const moves = this.availableMoves();
      const turn = this.whoseTurn();

      if (turn === 'auto' && moves.length === 1) {
        this.makeMove(moves[0]);
      } else if (moves.length > 1 && turn === 'server') {
        // AI picks for server
        const pick =
          this.mode() === 'adversarial' ? this.aiLongestPath(moves) : this.aiShortestPath(moves);
        this.makeMove(pick);
      }
    }, 700);
  }

  private aiShortestPath(moves: GameEdge[]): GameEdge {
    let best = moves[0];
    let bestDist = Infinity;
    for (const e of moves) {
      const d = this.bfsDistance(e.tgt, this.gameData()!.bottom);
      if (d < bestDist) {
        bestDist = d;
        best = e;
      }
    }
    return best;
  }

  private aiLongestPath(moves: GameEdge[]): GameEdge {
    let best = moves[0];
    let bestDist = -1;
    for (const e of moves) {
      const d = this.bfsDistance(e.tgt, this.gameData()!.bottom);
      if (d > bestDist) {
        bestDist = d;
        best = e;
      }
    }
    return best;
  }

  private bfsDistance(from: number, to: number): number {
    if (from === to) return 0;
    const data = this.gameData()!;
    const visited = new Set([from]);
    const queue: [number, number][] = [[from, 0]];
    while (queue.length > 0) {
      const [node, dist] = queue.shift()!;
      for (const e of data.edges.filter((e) => e.src === node)) {
        if (e.tgt === to) return dist + 1;
        if (!visited.has(e.tgt)) {
          visited.add(e.tgt);
          queue.push([e.tgt, dist + 1]);
        }
      }
    }
    return 999;
  }

  /* ── SVG helpers (used from template) ──────────────── */

  edgePath(edge: GameEdge): string {
    const nodes = this.nodeMap();
    const src = nodes[edge.src];
    const tgt = nodes[edge.tgt];
    if (!src || !tgt) return '';

    const dx = tgt.x - src.x;
    const dy = tgt.y - src.y;
    const len = Math.sqrt(dx * dx + dy * dy);
    if (len === 0) return '';

    const r = 20;
    const x1 = src.x + (dx / len) * r;
    const y1 = src.y + (dy / len) * r;
    const x2 = src.x + (dx / len) * (len - r - 6);
    const y2 = src.y + (dy / len) * (len - r - 6);

    // Check parallel edges
    const data = this.gameData()!;
    const sameDir = data.edges.filter((e) => e.src === edge.src && e.tgt === edge.tgt);
    const idx = sameDir.indexOf(edge);
    const total = sameDir.length;

    if (total > 1) {
      const offset = (idx - (total - 1) / 2) * 30;
      const mx = (src.x + tgt.x) / 2 - (dy / len) * offset;
      const my = (src.y + tgt.y) / 2 + (dx / len) * offset;
      return `M ${x1} ${y1} Q ${mx} ${my} ${x2} ${y2}`;
    }
    return `M ${x1} ${y1} L ${x2} ${y2}`;
  }

  edgeLabelPos(edge: GameEdge): { x: number; y: number } {
    const nodes = this.nodeMap();
    const src = nodes[edge.src];
    const tgt = nodes[edge.tgt];
    if (!src || !tgt) return { x: 0, y: 0 };

    const dx = tgt.x - src.x;
    const dy = tgt.y - src.y;
    const len = Math.sqrt(dx * dx + dy * dy) || 1;

    const data = this.gameData()!;
    const sameDir = data.edges.filter((e) => e.src === edge.src && e.tgt === edge.tgt);
    const idx = sameDir.indexOf(edge);
    const total = sameDir.length;

    if (total > 1) {
      const offset = (idx - (total - 1) / 2) * 30;
      return {
        x: (src.x + tgt.x) / 2 - (dy / len) * offset,
        y: (src.y + tgt.y) / 2 + (dx / len) * offset - 6,
      };
    }
    return {
      x: (src.x + tgt.x) / 2 - (dy / len) * 12,
      y: (src.y + tgt.y) / 2 + (dx / len) * 12 - 4,
    };
  }

  isEdgeAvailable(edge: GameEdge): boolean {
    return edge.src === this.current() && this.availableMoves().length > 1 && !this.isFinished();
  }

  isEdgeTraversed(edge: GameEdge): boolean {
    return this.traversedEdges().has(`${edge.src}-${edge.label}-${edge.tgt}`);
  }

  nodeGradient(node: GameNode): string {
    if (node.kind === 'top') return 'url(#grad-top)';
    if (node.kind === 'end') return 'url(#grad-end)';
    if (node.kind === 'select') return 'url(#grad-select)';
    return 'url(#grad-branch)';
  }

  nodeDisplayLabel(node: GameNode): string {
    if (node.isTop) return '\u22a4 ' + this.truncateLabel(node.label);
    if (node.isBottom) return '\u22a5 end';
    return this.truncateLabel(node.label);
  }

  private truncateLabel(s: string): string {
    return s.length > 16 ? s.substring(0, 14) + '\u2026' : s;
  }

  isNodeTarget(node: GameNode): boolean {
    return (
      this.availableMoves().some((e) => e.tgt === node.id) &&
      this.availableMoves().length > 1 &&
      !this.isFinished()
    );
  }

  clickNode(node: GameNode): void {
    const edge = this.availableMoves().find((e) => e.tgt === node.id);
    if (edge) this.makeMove(edge);
  }

  glowColor(node: GameNode): string {
    return node.isBottom ? '#22c55e' : '#6366f1';
  }

  turnLabel(): string {
    const turn = this.whoseTurn();
    if (this.isFinished()) return 'Protocol Complete!';
    if (turn === 'auto') return 'Automatic move...';
    if (turn === 'client') return "Client's turn (branch)";
    if (turn === 'server') return "Server's turn (select)";
    return 'Select a protocol to begin';
  }

  turnClass(): string {
    if (this.isFinished()) return 'turn-victory';
    if (!this.gameData()) return 'turn-waiting';
    const turn = this.whoseTurn();
    if (turn === 'auto') return 'turn-auto';
    if (turn === 'client') return 'turn-client';
    if (turn === 'server') return 'turn-server';
    return 'turn-waiting';
  }
}
