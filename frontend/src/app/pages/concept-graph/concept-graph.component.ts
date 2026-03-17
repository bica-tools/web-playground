import { Component, AfterViewInit, OnDestroy, ElementRef, ViewChild, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

interface GraphNode {
  id: string;
  step: string;
  label: string;
  description: string;
  category: string;
  status: string;
  lean: boolean;
  reticulate: boolean;
  bica: boolean;
  x?: number;
  y?: number;
  vx?: number;
  vy?: number;
  fx?: number | null;
  fy?: number | null;
}

interface GraphEdge {
  source: string;
  target: string;
  label?: string;
}

const CATEGORY_COLORS: Record<string, string> = {
  'ground-truth': '#059669',
  'morphism': '#6366f1',
  'multiparty': '#0284c7',
  'structural': '#d97706',
  'advanced': '#7c3aed',
  'categorical': '#ec4899',
  'metatheory': '#6b7280',
};

@Component({
  selector: 'app-concept-graph',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="graph-hero">
      <h1>Research Map</h1>
      <p>How the 28 results connect — click a node to explore</p>
    </section>

    <div class="graph-layout">
      <div class="graph-canvas">
        <svg #graphSvg class="graph-svg"></svg>
        <div class="graph-legend">
          @for (cat of categories; track cat.id) {
            <div class="legend-item">
              <div class="legend-dot" [style.background]="catColor(cat.id)"></div>
              <span class="legend-label">{{ cat.label }}</span>
            </div>
          }
        </div>
      </div>

      <div class="side-panel">
        @if (!selectedNode()) {
          <div class="panel-empty">
            <div class="empty-icon">&#x25C9;</div>
            <p>Click a node to see details</p>
            <p class="empty-hint">Drag nodes to rearrange</p>
          </div>
        } @else {
          <h2 class="panel-title">{{ selectedNode()!.label }}</h2>
          <div class="panel-step">Step {{ selectedNode()!.step }}</div>
          <div class="panel-desc">{{ selectedNode()!.description }}</div>

          <div class="panel-section-label">Status</div>
          <div class="panel-badges">
            <span class="p-badge" [class.p-proved]="selectedNode()!.status === 'mechanised'"
                  [class.p-mechanised]="selectedNode()!.status === 'mechanised'"
                  [class.p-implemented]="selectedNode()!.status === 'implemented'"
                  [class.p-partial]="selectedNode()!.status === 'partial'">
              {{ selectedNode()!.status }}
            </span>
            @if (selectedNode()!.lean) { <span class="p-badge p-mechanised">Lean 4</span> }
            @if (selectedNode()!.reticulate) { <span class="p-badge p-implemented">Python</span> }
            @if (selectedNode()!.bica) { <span class="p-badge p-implemented">Java</span> }
          </div>

          @if (getDeps(selectedNode()!.id).length > 0) {
            <div class="panel-section-label">Depends on</div>
            <div class="panel-deps">
              @for (dep of getDeps(selectedNode()!.id); track dep) {
                <div class="dep-item" (click)="selectById(dep)">
                  <span class="dep-arrow">&larr;</span>
                  {{ getLabel(dep) }}
                </div>
              }
            </div>
          }

          @if (getDependents(selectedNode()!.id).length > 0) {
            <div class="panel-section-label">Required by</div>
            <div class="panel-deps">
              @for (dep of getDependents(selectedNode()!.id); track dep) {
                <div class="dep-item" (click)="selectById(dep)">
                  <span class="dep-arrow">&rarr;</span>
                  {{ getLabel(dep) }}
                </div>
              }
            </div>
          }

          <div class="panel-links">
            <a class="panel-link" routerLink="/theory">View in Theorem Registry &rarr;</a>
            @if (selectedNode()!.lean) {
              <a class="panel-link" routerLink="/proofs">View in Proof Gallery &rarr;</a>
            }
          </div>
        }
      </div>
    </div>
  `,
  styleUrl: './concept-graph.component.scss',
})
export class ConceptGraphComponent implements AfterViewInit, OnDestroy {
  @ViewChild('graphSvg') svgRef!: ElementRef<SVGSVGElement>;

  readonly selectedNode = signal<GraphNode | null>(null);
  private animationId?: number;

  readonly categories = [
    { id: 'ground-truth', label: 'Ground Truth' },
    { id: 'morphism', label: 'Morphisms' },
    { id: 'multiparty', label: 'Multiparty' },
    { id: 'structural', label: 'Structural' },
    { id: 'advanced', label: 'Advanced' },
    { id: 'categorical', label: 'Categorical' },
    { id: 'metatheory', label: 'Metatheory' },
  ];

  readonly nodes: GraphNode[] = [
    { id: 'ss', step: '1', label: 'State Spaces', description: 'Reachability on state spaces is a preorder; SCC quotient yields a partial order.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'end', step: '2a', label: 'End Lemma', description: 'L(end) is a singleton bounded lattice.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'branch', step: '2b', label: 'Branch Lemma', description: 'Adding new bottom preserves bounded lattice.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'par', step: '2c', label: 'Parallel Lemma', description: 'L(S\u2081 \u2225 S\u2082) = L(S\u2081) \u00D7 L(S\u2082) is a bounded lattice.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'abs', step: '2d', label: 'Bottom Absorption', description: 'Collapsing downward-closed set preserves lattice.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: false, bica: false },
    { id: 'rec', step: '2e', label: 'Recursion Lemma', description: 'SCC quotient of recursive type is bounded lattice.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'rt', step: '2', label: 'Reticulate Theorem', description: 'Every well-formed session type\u2019s state space is a bounded lattice.', category: 'ground-truth', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'univ', step: '6', label: 'Universality', description: 'All terminating types at depth 3 form lattices.', category: 'ground-truth', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'sub', step: '7', label: 'Subtyping \u21D4 Embedding', description: 'Gay\u2013Hole subtyping iff lattice embedding. Faithful but not full.', category: 'morphism', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'dual', step: '8', label: 'Duality Involution', description: 'dual(dual(S)) = S. Preserves isomorphism, reverses subtyping.', category: 'morphism', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'ret', step: '9', label: 'Reticular Form', description: 'Necessary and sufficient conditions for realizability.', category: 'morphism', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'endo', step: '10', label: 'Endomorphisms', description: '97% of transition labels are lattice endomorphisms.', category: 'morphism', status: 'mechanised', lean: true, reticulate: true, bica: true },
    { id: 'gt', step: '11', label: 'Global Types', description: 'Multiparty global types form lattices.', category: 'multiparty', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'proj', step: '12', label: 'Projection', description: 'Projection onto roles yields surjective order-preserving maps.', category: 'multiparty', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'comp', step: '15', label: 'Composition', description: 'Free and synchronized products with compatibility checking.', category: 'multiparty', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'recurse', step: '13', label: 'Recursion Analysis', description: 'Guardedness, contractiveness, tail recursion, SCC analysis.', category: 'structural', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'chomsky', step: '14', label: 'Chomsky Classification', description: 'All 34 benchmarks have regular trace languages.', category: 'structural', status: 'implemented', lean: false, reticulate: true, bica: true },
    { id: 'pol', step: '155b', label: 'Polarity / FCA', description: 'Galois connection between state and label closures.', category: 'advanced', status: 'mechanised', lean: true, reticulate: true, bica: false },
    { id: 'real', step: '156', label: 'Realizability', description: 'Bounded lattice + reticular form \u21D4 realizable.', category: 'advanced', status: 'mechanised', lean: true, reticulate: true, bica: false },
    { id: 'ch', step: '157a', label: 'Channel Duality', description: 'Ch(S) = L(S) \u00D7 L(dual(S)) is bounded lattice.', category: 'advanced', status: 'mechanised', lean: true, reticulate: true, bica: false },
    { id: 'cat', step: '163', label: 'SessLat Category', description: 'Session-type lattices and morphisms form a category.', category: 'categorical', status: 'implemented', lean: false, reticulate: true, bica: false },
    { id: 'coprod', step: '164', label: 'Coproducts', description: 'Coproduct construction in SessLat.', category: 'categorical', status: 'implemented', lean: false, reticulate: true, bica: false },
    { id: 'eq', step: '165', label: 'Equalizers', description: 'Equalizer construction in SessLat.', category: 'categorical', status: 'implemented', lean: false, reticulate: true, bica: false },
    { id: 'coeq', step: '166', label: 'Coequalizers', description: 'Coequalizer construction in SessLat.', category: 'categorical', status: 'implemented', lean: false, reticulate: true, bica: false },
    { id: 'lam0', step: '200b', label: '\u03BB_S\u2070 Soundness', description: 'Progress + preservation for basic typestate checking.', category: 'metatheory', status: 'partial', lean: true, reticulate: false, bica: false },
  ];

  readonly edges: GraphEdge[] = [
    { source: 'ss', target: 'end' }, { source: 'ss', target: 'branch' },
    { source: 'ss', target: 'par' }, { source: 'ss', target: 'rec' },
    { source: 'end', target: 'rt' }, { source: 'branch', target: 'rt' },
    { source: 'par', target: 'rt' }, { source: 'abs', target: 'rec' },
    { source: 'rec', target: 'rt' },
    { source: 'rt', target: 'sub' }, { source: 'rt', target: 'dual' },
    { source: 'rt', target: 'ret' }, { source: 'rt', target: 'endo' },
    { source: 'rt', target: 'univ' }, { source: 'rt', target: 'gt' },
    { source: 'rt', target: 'pol' }, { source: 'rt', target: 'real' },
    { source: 'rt', target: 'recurse' }, { source: 'rt', target: 'chomsky' },
    { source: 'rt', target: 'cat' }, { source: 'rt', target: 'lam0' },
    { source: 'dual', target: 'ch' }, { source: 'par', target: 'ch' },
    { source: 'gt', target: 'proj' }, { source: 'proj', target: 'comp' },
    { source: 'sub', target: 'cat' }, { source: 'cat', target: 'coprod' },
    { source: 'cat', target: 'eq' }, { source: 'cat', target: 'coeq' },
    { source: 'ret', target: 'real' },
  ];

  catColor(id: string): string {
    return CATEGORY_COLORS[id] || '#999';
  }

  getLabel(id: string): string {
    return this.nodes.find(n => n.id === id)?.label || id;
  }

  getDeps(id: string): string[] {
    return this.edges.filter(e => e.target === id).map(e => e.source);
  }

  getDependents(id: string): string[] {
    return this.edges.filter(e => e.source === id).map(e => e.target);
  }

  selectById(id: string): void {
    const node = this.nodes.find(n => n.id === id);
    if (node) this.selectedNode.set(node);
  }

  ngAfterViewInit(): void {
    this.renderGraph();
  }

  ngOnDestroy(): void {
    if (this.animationId) cancelAnimationFrame(this.animationId);
  }

  private renderGraph(): void {
    const svg = this.svgRef.nativeElement;
    const rect = svg.getBoundingClientRect();
    const W = rect.width || 800;
    const H = rect.height || 600;

    svg.setAttribute('viewBox', `0 0 ${W} ${H}`);

    // Initialize positions
    const nodeMap = new Map<string, GraphNode>();
    this.nodes.forEach((n, i) => {
      n.x = W / 2 + (Math.random() - 0.5) * W * 0.6;
      n.y = H / 2 + (Math.random() - 0.5) * H * 0.6;
      n.vx = 0; n.vy = 0;
      nodeMap.set(n.id, n);
    });

    // Create SVG elements
    const ns = 'http://www.w3.org/2000/svg';

    // Defs for arrow marker
    const defs = document.createElementNS(ns, 'defs');
    const marker = document.createElementNS(ns, 'marker');
    marker.setAttribute('id', 'arrowhead');
    marker.setAttribute('markerWidth', '8');
    marker.setAttribute('markerHeight', '6');
    marker.setAttribute('refX', '20');
    marker.setAttribute('refY', '3');
    marker.setAttribute('orient', 'auto');
    const poly = document.createElementNS(ns, 'polygon');
    poly.setAttribute('points', '0 0, 8 3, 0 6');
    poly.setAttribute('class', 'edge-arrow');
    marker.appendChild(poly);
    defs.appendChild(marker);
    svg.appendChild(defs);

    // Edges
    const edgeGroup = document.createElementNS(ns, 'g');
    const edgeLines: SVGLineElement[] = [];
    this.edges.forEach(e => {
      const line = document.createElementNS(ns, 'line');
      line.setAttribute('class', 'edge-line');
      line.setAttribute('marker-end', 'url(#arrowhead)');
      edgeGroup.appendChild(line);
      edgeLines.push(line);
    });
    svg.appendChild(edgeGroup);

    // Nodes
    const nodeGroup = document.createElementNS(ns, 'g');
    const circles: SVGCircleElement[] = [];
    const labels: SVGTextElement[] = [];

    this.nodes.forEach((n, i) => {
      const g = document.createElementNS(ns, 'g');

      const circle = document.createElementNS(ns, 'circle');
      circle.setAttribute('r', n.id === 'rt' ? '16' : '12');
      circle.setAttribute('class', 'node-circle');
      circle.setAttribute('fill', CATEGORY_COLORS[n.category] || '#999');
      circle.setAttribute('opacity', '0.8');
      circle.setAttribute('stroke', '#fff');
      circle.setAttribute('stroke-width', '2');
      circles.push(circle);

      const text = document.createElementNS(ns, 'text');
      text.setAttribute('class', 'node-label');
      text.setAttribute('dy', '24');
      text.textContent = n.label.length > 16 ? n.label.substring(0, 14) + '..' : n.label;
      labels.push(text);

      g.appendChild(circle);
      g.appendChild(text);
      nodeGroup.appendChild(g);

      // Click handler
      circle.addEventListener('click', () => this.selectedNode.set(n));
      text.addEventListener('click', () => this.selectedNode.set(n));

      // Drag
      let dragging = false;
      circle.addEventListener('mousedown', (ev) => {
        dragging = true;
        n.fx = n.x; n.fy = n.y;
        ev.preventDefault();
      });
      const onMove = (ev: MouseEvent) => {
        if (!dragging) return;
        const svgRect = svg.getBoundingClientRect();
        n.fx = (ev.clientX - svgRect.left) * (W / svgRect.width);
        n.fy = (ev.clientY - svgRect.top) * (H / svgRect.height);
        n.x = n.fx; n.y = n.fy;
      };
      const onUp = () => {
        if (dragging) { dragging = false; n.fx = null; n.fy = null; }
      };
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
    svg.appendChild(nodeGroup);

    // Force simulation (simple)
    const alpha = { value: 1 };
    const simulate = () => {
      alpha.value *= 0.995;
      if (alpha.value < 0.001) { alpha.value = 0.001; }

      // Repulsion
      for (let i = 0; i < this.nodes.length; i++) {
        for (let j = i + 1; j < this.nodes.length; j++) {
          const a = this.nodes[i], b = this.nodes[j];
          let dx = (b.x ?? 0) - (a.x ?? 0);
          let dy = (b.y ?? 0) - (a.y ?? 0);
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          const force = 2000 / (dist * dist) * alpha.value;
          dx *= force / dist; dy *= force / dist;
          if (a.fx == null) { a.vx! -= dx; a.vy! -= dy; }
          if (b.fx == null) { b.vx! += dx; b.vy! += dy; }
        }
      }

      // Edge attraction
      this.edges.forEach(e => {
        const s = nodeMap.get(e.source)!, t = nodeMap.get(e.target)!;
        let dx = (t.x ?? 0) - (s.x ?? 0);
        let dy = (t.y ?? 0) - (s.y ?? 0);
        const dist = Math.sqrt(dx * dx + dy * dy) || 1;
        const force = (dist - 80) * 0.01 * alpha.value;
        dx *= force / dist; dy *= force / dist;
        if (s.fx == null) { s.vx! += dx; s.vy! += dy; }
        if (t.fx == null) { t.vx! -= dx; t.vy! -= dy; }
      });

      // Center gravity
      this.nodes.forEach(n => {
        if (n.fx != null) return;
        n.vx! += (W / 2 - (n.x ?? 0)) * 0.002 * alpha.value;
        n.vy! += (H / 2 - (n.y ?? 0)) * 0.002 * alpha.value;
      });

      // Velocity + position update
      this.nodes.forEach(n => {
        if (n.fx != null) { n.x = n.fx; n.y = n.fy!; n.vx = 0; n.vy = 0; return; }
        n.vx! *= 0.6; n.vy! *= 0.6;
        n.x = Math.max(20, Math.min(W - 20, (n.x ?? 0) + n.vx!));
        n.y = Math.max(20, Math.min(H - 20, (n.y ?? 0) + n.vy!));
      });

      // Update SVG
      this.edges.forEach((e, i) => {
        const s = nodeMap.get(e.source)!, t = nodeMap.get(e.target)!;
        edgeLines[i].setAttribute('x1', String(s.x));
        edgeLines[i].setAttribute('y1', String(s.y));
        edgeLines[i].setAttribute('x2', String(t.x));
        edgeLines[i].setAttribute('y2', String(t.y));
      });

      this.nodes.forEach((n, i) => {
        circles[i].setAttribute('cx', String(n.x));
        circles[i].setAttribute('cy', String(n.y));
        labels[i].setAttribute('x', String(n.x));
        labels[i].setAttribute('y', String(n.y));
      });

      this.animationId = requestAnimationFrame(simulate);
    };

    simulate();
  }
}
