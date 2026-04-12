import {
  MatTableModule
} from "./chunk-P4YU6FKE.js";
import "./chunk-CVQJCEWM.js";
import {
  HasseDiagramComponent
} from "./chunk-XXDSCLCV.js";
import {
  MatProgressSpinner,
  MatProgressSpinnerModule
} from "./chunk-KSWLVI2B.js";
import {
  ApiService
} from "./chunk-EOCOQ6DB.js";
import {
  CodeBlockComponent
} from "./chunk-EFHCE74K.js";
import "./chunk-R2VWAHTD.js";
import "./chunk-SUS3PTUT.js";
import {
  MatButtonModule
} from "./chunk-BUK7DMBP.js";
import {
  RouterLink
} from "./chunk-QTYX35EO.js";
import {
  MatIconModule
} from "./chunk-BFW3NWZD.js";
import "./chunk-ZG4TCI7P.js";
import "./chunk-NL2TMNRB.js";
import {
  Component,
  setClassMetadata,
  signal,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵdefineComponent,
  ɵɵdirectiveInject,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵpureFunction0,
  ɵɵpureFunction1,
  ɵɵpureFunction2,
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵrepeaterTrackByIdentity,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1
} from "./chunk-OWEA7TR3.js";

// src/app/pages/benchmarks/benchmarks.component.ts
var _c0 = () => ["/tools/analyzer"];
var _c1 = (a0) => ({ type: a0 });
var _c2 = () => ["/tools/test-generator"];
var _c3 = (a0, a1) => ({ type: a0, class: a1 });
var _forTrack0 = ($index, $item) => $item.name;
function BenchmarksComponent_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 1);
    \u0275\u0275element(1, "mat-spinner", 2);
    \u0275\u0275elementStart(2, "span");
    \u0275\u0275text(3, "Loading benchmarks\u2026");
    \u0275\u0275elementEnd()();
  }
}
function BenchmarksComponent_Conditional_6_For_40_Conditional_14_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " \u2713 ");
  }
}
function BenchmarksComponent_Conditional_6_For_40_Conditional_16_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " \u2713 ");
  }
}
function BenchmarksComponent_Conditional_6_For_40_Conditional_18_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " \u2713 ");
  }
}
function BenchmarksComponent_Conditional_6_For_40_Conditional_50_For_4_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "span", 19);
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const m_r1 = ctx.$implicit;
    \u0275\u0275advance();
    \u0275\u0275textInterpolate(m_r1);
  }
}
function BenchmarksComponent_Conditional_6_For_40_Conditional_50_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 17)(1, "strong");
    \u0275\u0275text(2, "Methods:");
    \u0275\u0275elementEnd();
    \u0275\u0275repeaterCreate(3, BenchmarksComponent_Conditional_6_For_40_Conditional_50_For_4_Template, 2, 1, "span", 19, \u0275\u0275repeaterTrackByIdentity);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const b_r2 = \u0275\u0275nextContext().$implicit;
    \u0275\u0275advance(3);
    \u0275\u0275repeater(b_r2.methods);
  }
}
function BenchmarksComponent_Conditional_6_For_40_Conditional_51_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "app-hasse-diagram", 18);
  }
  if (rf & 2) {
    const b_r2 = \u0275\u0275nextContext().$implicit;
    \u0275\u0275property("svgHtml", b_r2.svgHtml);
  }
}
function BenchmarksComponent_Conditional_6_For_40_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "tr")(1, "td", 7);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "td", 8);
    \u0275\u0275text(4);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(5, "td");
    \u0275\u0275text(6);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "td");
    \u0275\u0275text(8);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(9, "td");
    \u0275\u0275text(10);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(11, "td");
    \u0275\u0275text(12);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(13, "td");
    \u0275\u0275conditionalCreate(14, BenchmarksComponent_Conditional_6_For_40_Conditional_14_Template, 1, 0);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(15, "td");
    \u0275\u0275conditionalCreate(16, BenchmarksComponent_Conditional_6_For_40_Conditional_16_Template, 1, 0);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(17, "td");
    \u0275\u0275conditionalCreate(18, BenchmarksComponent_Conditional_6_For_40_Conditional_18_Template, 1, 0);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(19, "td");
    \u0275\u0275text(20);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(21, "td", 9)(22, "a", 10);
    \u0275\u0275text(23, "analyze");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(24, "a", 10);
    \u0275\u0275text(25, "tests");
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(26, "tr", 11)(27, "td", 12)(28, "details")(29, "summary");
    \u0275\u0275text(30);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(31, "div", 13);
    \u0275\u0275element(32, "app-code-block", 14);
    \u0275\u0275elementStart(33, "div", 15)(34, "span", 16)(35, "strong");
    \u0275\u0275text(36, "Rec depth:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(37);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(38, "span", 16)(39, "strong");
    \u0275\u0275text(40, "Valid paths:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(41);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(42, "span", 16)(43, "strong");
    \u0275\u0275text(44, "Violations:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(45);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(46, "span", 16)(47, "strong");
    \u0275\u0275text(48, "Incomplete:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(49);
    \u0275\u0275elementEnd()();
    \u0275\u0275conditionalCreate(50, BenchmarksComponent_Conditional_6_For_40_Conditional_50_Template, 5, 0, "div", 17);
    \u0275\u0275conditionalCreate(51, BenchmarksComponent_Conditional_6_For_40_Conditional_51_Template, 1, 1, "app-hasse-diagram", 18);
    \u0275\u0275elementEnd()()()();
  }
  if (rf & 2) {
    const b_r2 = ctx.$implicit;
    const \u0275$index_80_r3 = ctx.$index;
    const ctx_r3 = \u0275\u0275nextContext(2);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(\u0275$index_80_r3 + 1);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(b_r2.name);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(b_r2.numStates);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(b_r2.numTransitions);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(b_r2.numSccs);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(b_r2.numMethods);
    \u0275\u0275advance(2);
    \u0275\u0275conditional(b_r2.usesParallel ? 14 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275conditional(b_r2.isRecursive ? 16 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275conditional(b_r2.isLattice ? 18 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(b_r2.numTests);
    \u0275\u0275advance(2);
    \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(22, _c0))("queryParams", \u0275\u0275pureFunction1(23, _c1, b_r2.typeString));
    \u0275\u0275advance(2);
    \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(25, _c2))("queryParams", \u0275\u0275pureFunction2(26, _c3, b_r2.typeString, ctx_r3.toClassName(b_r2.name)));
    \u0275\u0275advance(6);
    \u0275\u0275textInterpolate(b_r2.description);
    \u0275\u0275advance(2);
    \u0275\u0275property("code", b_r2.pretty);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate1(" ", b_r2.recDepth, " ");
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate1(" ", b_r2.numValidPaths, " ");
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate1(" ", b_r2.numViolations, " ");
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate1(" ", b_r2.numIncomplete, " ");
    \u0275\u0275advance();
    \u0275\u0275conditional(b_r2.methods && b_r2.methods.length > 0 ? 50 : -1);
    \u0275\u0275advance();
    \u0275\u0275conditional(b_r2.svgHtml ? 51 : -1);
  }
}
function BenchmarksComponent_Conditional_6_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 3)(1, "div", 4);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "div", 4);
    \u0275\u0275text(4);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(5, "div", 4);
    \u0275\u0275text(6);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "div", 4);
    \u0275\u0275text(8);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(9, "div", 4);
    \u0275\u0275text(10);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(11, "div", 4);
    \u0275\u0275text(12);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(13, "div", 5)(14, "table", 6)(15, "thead")(16, "tr")(17, "th");
    \u0275\u0275text(18, "#");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(19, "th");
    \u0275\u0275text(20, "Protocol");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(21, "th");
    \u0275\u0275text(22, "States");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(23, "th");
    \u0275\u0275text(24, "Trans.");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(25, "th");
    \u0275\u0275text(26, "SCCs");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(27, "th");
    \u0275\u0275text(28, "Methods");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(29, "th");
    \u0275\u0275text(30, "\u2225");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(31, "th");
    \u0275\u0275text(32, "rec");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(33, "th");
    \u0275\u0275text(34, "Lattice");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(35, "th");
    \u0275\u0275text(36, "Tests");
    \u0275\u0275elementEnd();
    \u0275\u0275element(37, "th");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(38, "tbody");
    \u0275\u0275repeaterCreate(39, BenchmarksComponent_Conditional_6_For_40_Template, 52, 29, null, null, _forTrack0);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const ctx_r3 = \u0275\u0275nextContext();
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r3.benchmarks().length, " protocols");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r3.parallelCount(), " use \u2225");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r3.recursiveCount(), " recursive");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r3.latticeCount(), " lattices");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r3.totalStates(), " total states");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r3.totalTests(), " tests generated");
    \u0275\u0275advance(27);
    \u0275\u0275repeater(ctx_r3.benchmarks());
  }
}
var BenchmarksComponent = class _BenchmarksComponent {
  api;
  benchmarks = signal([], ...ngDevMode ? [{ debugName: "benchmarks" }] : []);
  loading = signal(true, ...ngDevMode ? [{ debugName: "loading" }] : []);
  parallelCount = signal(0, ...ngDevMode ? [{ debugName: "parallelCount" }] : []);
  recursiveCount = signal(0, ...ngDevMode ? [{ debugName: "recursiveCount" }] : []);
  latticeCount = signal(0, ...ngDevMode ? [{ debugName: "latticeCount" }] : []);
  totalStates = signal(0, ...ngDevMode ? [{ debugName: "totalStates" }] : []);
  totalTests = signal(0, ...ngDevMode ? [{ debugName: "totalTests" }] : []);
  constructor(api) {
    this.api = api;
  }
  toClassName(name) {
    return name.replace(/[^a-zA-Z0-9]/g, "");
  }
  ngOnInit() {
    this.api.getBenchmarks().subscribe({
      next: (data) => {
        this.benchmarks.set(data);
        this.parallelCount.set(data.filter((b) => b.usesParallel).length);
        this.recursiveCount.set(data.filter((b) => b.isRecursive).length);
        this.latticeCount.set(data.filter((b) => b.isLattice).length);
        this.totalStates.set(data.reduce((s, b) => s + b.numStates, 0));
        this.totalTests.set(data.reduce((s, b) => s + b.numTests, 0));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
  static \u0275fac = function BenchmarksComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _BenchmarksComponent)(\u0275\u0275directiveInject(ApiService));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _BenchmarksComponent, selectors: [["app-benchmarks"]], decls: 7, vars: 2, consts: [[1, "page-header"], [1, "loading"], ["diameter", "32"], [1, "stats-row"], [1, "stat-chip"], [1, "table-container"], [1, "benchmark-table"], [1, "num-col"], [1, "protocol-name"], [1, "action-links"], [3, "routerLink", "queryParams"], [1, "detail-row"], ["colspan", "11"], [1, "detail-content"], ["label", "Session type", 3, "code"], [1, "detail-meta"], [1, "meta-item"], [1, "detail-methods"], [3, "svgHtml"], [1, "method-chip"]], template: function BenchmarksComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "Benchmarks");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4);
      \u0275\u0275elementEnd()();
      \u0275\u0275conditionalCreate(5, BenchmarksComponent_Conditional_5_Template, 4, 0, "div", 1)(6, BenchmarksComponent_Conditional_6_Template, 41, 6);
    }
    if (rf & 2) {
      \u0275\u0275advance(4);
      \u0275\u0275textInterpolate1(" ", ctx.benchmarks().length, " real-world and classic protocols expressed as session types, verified through the full analysis pipeline. ");
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.loading() ? 5 : 6);
    }
  }, dependencies: [
    RouterLink,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatProgressSpinner,
    HasseDiagramComponent,
    CodeBlockComponent
  ], styles: ['\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.loading[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  gap: 12px;\n  padding: 48px 0;\n}\n.stats-row[_ngcontent-%COMP%] {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 10px;\n  justify-content: center;\n  padding: 16px 0 24px;\n}\n.stat-chip[_ngcontent-%COMP%] {\n  display: inline-block;\n  padding: 6px 14px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n  border-radius: 20px;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.7);\n}\n.table-container[_ngcontent-%COMP%] {\n  overflow-x: auto;\n}\n.benchmark-table[_ngcontent-%COMP%] {\n  width: 100%;\n  border-collapse: collapse;\n  font-size: 14px;\n}\n.benchmark-table[_ngcontent-%COMP%]   th[_ngcontent-%COMP%] {\n  text-align: left;\n  padding: 10px 12px;\n  font-weight: 600;\n  font-size: 12px;\n  text-transform: uppercase;\n  letter-spacing: 0.3px;\n  border-bottom: 2px solid rgba(0, 0, 0, 0.12);\n  white-space: nowrap;\n  color: rgba(0, 0, 0, 0.55);\n}\n.benchmark-table[_ngcontent-%COMP%]   td[_ngcontent-%COMP%] {\n  padding: 8px 12px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.04);\n}\n.benchmark-table[_ngcontent-%COMP%]   tbody[_ngcontent-%COMP%]   tr[_ngcontent-%COMP%]:hover   td[_ngcontent-%COMP%] {\n  background: rgba(0, 0, 0, 0.02);\n}\n.num-col[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.35);\n  font-size: 12px;\n}\n.protocol-name[_ngcontent-%COMP%] {\n  font-weight: 500;\n}\n.detail-row[_ngcontent-%COMP%]   td[_ngcontent-%COMP%] {\n  padding: 0 12px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n}\n.detail-row[_ngcontent-%COMP%]   details[_ngcontent-%COMP%] {\n  margin: 4px 0;\n}\n.detail-row[_ngcontent-%COMP%]   summary[_ngcontent-%COMP%] {\n  cursor: pointer;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.45);\n  padding: 4px 0;\n}\n.detail-content[_ngcontent-%COMP%] {\n  padding: 8px 0 16px;\n}\n.detail-meta[_ngcontent-%COMP%] {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 16px;\n  margin: 10px 0;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n}\n.detail-methods[_ngcontent-%COMP%] {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 6px;\n  align-items: center;\n  margin: 8px 0 12px;\n  font-size: 13px;\n}\n.method-chip[_ngcontent-%COMP%] {\n  display: inline-block;\n  padding: 2px 10px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n  border-radius: 12px;\n  font-size: 12px;\n  font-family: "JetBrains Mono", monospace;\n  color: rgba(0, 0, 0, 0.65);\n}\n.action-links[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 12px;\n  white-space: nowrap;\n}\n.benchmark-table[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  font-size: 13px;\n}\n.benchmark-table[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n/*# sourceMappingURL=benchmarks.component.css.map */'] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(BenchmarksComponent, [{
    type: Component,
    args: [{ selector: "app-benchmarks", standalone: true, imports: [
      RouterLink,
      MatTableModule,
      MatIconModule,
      MatButtonModule,
      MatProgressSpinnerModule,
      HasseDiagramComponent,
      CodeBlockComponent
    ], template: `
    <header class="page-header">
      <h1>Benchmarks</h1>
      <p>
        {{ benchmarks().length }} real-world and classic protocols expressed as session types,
        verified through the full analysis pipeline.
      </p>
    </header>

    @if (loading()) {
      <div class="loading">
        <mat-spinner diameter="32"></mat-spinner>
        <span>Loading benchmarks&hellip;</span>
      </div>
    } @else {
      <!-- Summary stats -->
      <div class="stats-row">
        <div class="stat-chip">{{ benchmarks().length }} protocols</div>
        <div class="stat-chip">{{ parallelCount() }} use &#x2225;</div>
        <div class="stat-chip">{{ recursiveCount() }} recursive</div>
        <div class="stat-chip">{{ latticeCount() }} lattices</div>
        <div class="stat-chip">{{ totalStates() }} total states</div>
        <div class="stat-chip">{{ totalTests() }} tests generated</div>
      </div>

      <!-- Benchmark table -->
      <div class="table-container">
        <table class="benchmark-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Protocol</th>
              <th>States</th>
              <th>Trans.</th>
              <th>SCCs</th>
              <th>Methods</th>
              <th>&#x2225;</th>
              <th>rec</th>
              <th>Lattice</th>
              <th>Tests</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (b of benchmarks(); track b.name; let i = $index) {
              <tr>
                <td class="num-col">{{ i + 1 }}</td>
                <td class="protocol-name">{{ b.name }}</td>
                <td>{{ b.numStates }}</td>
                <td>{{ b.numTransitions }}</td>
                <td>{{ b.numSccs }}</td>
                <td>{{ b.numMethods }}</td>
                <td>@if (b.usesParallel) { &#x2713; }</td>
                <td>@if (b.isRecursive) { &#x2713; }</td>
                <td>@if (b.isLattice) { &#x2713; }</td>
                <td>{{ b.numTests }}</td>
                <td class="action-links">
                  <a [routerLink]="['/tools/analyzer']" [queryParams]="{type: b.typeString}">analyze</a>
                  <a [routerLink]="['/tools/test-generator']" [queryParams]="{type: b.typeString, class: toClassName(b.name)}">tests</a>
                </td>
              </tr>
              <tr class="detail-row">
                <td colspan="11">
                  <details>
                    <summary>{{ b.description }}</summary>
                    <div class="detail-content">
                      <app-code-block [code]="b.pretty" label="Session type"></app-code-block>
                      <div class="detail-meta">
                        <span class="meta-item">
                          <strong>Rec depth:</strong> {{ b.recDepth }}
                        </span>
                        <span class="meta-item">
                          <strong>Valid paths:</strong> {{ b.numValidPaths }}
                        </span>
                        <span class="meta-item">
                          <strong>Violations:</strong> {{ b.numViolations }}
                        </span>
                        <span class="meta-item">
                          <strong>Incomplete:</strong> {{ b.numIncomplete }}
                        </span>
                      </div>
                      @if (b.methods && b.methods.length > 0) {
                        <div class="detail-methods">
                          <strong>Methods:</strong>
                          @for (m of b.methods; track m) {
                            <span class="method-chip">{{ m }}</span>
                          }
                        </div>
                      }
                      @if (b.svgHtml) {
                        <app-hasse-diagram [svgHtml]="b.svgHtml"></app-hasse-diagram>
                      }
                    </div>
                  </details>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `, styles: ['/* angular:styles/component:scss;a4dc7ef8a2b91fb9616846b86517c7e9bc3fd8b333fbfc08016972a72ed01a1c;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/benchmarks/benchmarks.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.loading {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  gap: 12px;\n  padding: 48px 0;\n}\n.stats-row {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 10px;\n  justify-content: center;\n  padding: 16px 0 24px;\n}\n.stat-chip {\n  display: inline-block;\n  padding: 6px 14px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n  border-radius: 20px;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.7);\n}\n.table-container {\n  overflow-x: auto;\n}\n.benchmark-table {\n  width: 100%;\n  border-collapse: collapse;\n  font-size: 14px;\n}\n.benchmark-table th {\n  text-align: left;\n  padding: 10px 12px;\n  font-weight: 600;\n  font-size: 12px;\n  text-transform: uppercase;\n  letter-spacing: 0.3px;\n  border-bottom: 2px solid rgba(0, 0, 0, 0.12);\n  white-space: nowrap;\n  color: rgba(0, 0, 0, 0.55);\n}\n.benchmark-table td {\n  padding: 8px 12px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.04);\n}\n.benchmark-table tbody tr:hover td {\n  background: rgba(0, 0, 0, 0.02);\n}\n.num-col {\n  color: rgba(0, 0, 0, 0.35);\n  font-size: 12px;\n}\n.protocol-name {\n  font-weight: 500;\n}\n.detail-row td {\n  padding: 0 12px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n}\n.detail-row details {\n  margin: 4px 0;\n}\n.detail-row summary {\n  cursor: pointer;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.45);\n  padding: 4px 0;\n}\n.detail-content {\n  padding: 8px 0 16px;\n}\n.detail-meta {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 16px;\n  margin: 10px 0;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n}\n.detail-methods {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 6px;\n  align-items: center;\n  margin: 8px 0 12px;\n  font-size: 13px;\n}\n.method-chip {\n  display: inline-block;\n  padding: 2px 10px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n  border-radius: 12px;\n  font-size: 12px;\n  font-family: "JetBrains Mono", monospace;\n  color: rgba(0, 0, 0, 0.65);\n}\n.action-links {\n  display: flex;\n  gap: 12px;\n  white-space: nowrap;\n}\n.benchmark-table a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  font-size: 13px;\n}\n.benchmark-table a:hover {\n  text-decoration: underline;\n}\n/*# sourceMappingURL=benchmarks.component.css.map */\n'] }]
  }], () => [{ type: ApiService }], null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(BenchmarksComponent, { className: "BenchmarksComponent", filePath: "src/app/pages/benchmarks/benchmarks.component.ts", lineNumber: 241 });
})();
export {
  BenchmarksComponent
};
//# sourceMappingURL=chunk-TQJI4YVU.js.map
