import {
  MatAccordion,
  MatExpansionModule,
  MatExpansionPanel,
  MatExpansionPanelDescription,
  MatExpansionPanelHeader,
  MatExpansionPanelTitle
} from "./chunk-OPVQJPP7.js";
import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardModule,
  MatCardTitle
} from "./chunk-SHRTRSL7.js";
import {
  MatChip,
  MatChipSet,
  MatChipsModule
} from "./chunk-F3DYJDGJ.js";
import {
  MatTab,
  MatTabGroup,
  MatTabsModule
} from "./chunk-FJHU3ZRV.js";
import "./chunk-RSSZT2MJ.js";
import {
  DefaultValueAccessor,
  FormsModule,
  NgControlStatus,
  NgModel
} from "./chunk-2AQDFUQH.js";
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
  MatButton,
  MatButtonModule
} from "./chunk-BUK7DMBP.js";
import {
  ActivatedRoute
} from "./chunk-QTYX35EO.js";
import {
  MatIcon,
  MatIconModule
} from "./chunk-BFW3NWZD.js";
import {
  CommonModule
} from "./chunk-ZG4TCI7P.js";
import "./chunk-NL2TMNRB.js";
import {
  Component,
  setClassMetadata,
  signal,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵdefineComponent,
  ɵɵdirectiveInject,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵlistener,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵrepeaterTrackByIdentity,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1,
  ɵɵtextInterpolate2,
  ɵɵtwoWayBindingSet,
  ɵɵtwoWayListener,
  ɵɵtwoWayProperty
} from "./chunk-OWEA7TR3.js";

// src/app/pages/global-analyzer/global-analyzer.component.ts
var _forTrack0 = ($index, $item) => $item.name;
function GlobalAnalyzerComponent_For_20_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "button", 15);
    \u0275\u0275listener("click", function GlobalAnalyzerComponent_For_20_Template_button_click_0_listener() {
      const ex_r2 = \u0275\u0275restoreView(_r1).$implicit;
      const ctx_r2 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r2.loadExample(ex_r2.type));
    });
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ex_r2 = ctx.$implicit;
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1(" ", ex_r2.name, " ");
  }
}
function GlobalAnalyzerComponent_Conditional_21_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 11)(1, "mat-card-header")(2, "mat-card-title");
    \u0275\u0275text(3, "Grammar");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(4, "mat-card-content")(5, "pre", 16);
    \u0275\u0275text(6);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    \u0275\u0275advance(6);
    \u0275\u0275textInterpolate2("G  ::=  sender -> receiver : ", "{", " m\u2081 : G\u2081 , \u2026 , m\u2099 : G\u2099 ", "}", "\n     |  G\u2081 || G\u2082\n     |  rec X . G\n     |  X\n     |  end");
  }
}
function GlobalAnalyzerComponent_Conditional_22_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 12);
    \u0275\u0275element(1, "mat-spinner", 17);
    \u0275\u0275elementStart(2, "span");
    \u0275\u0275text(3, "Analyzing global type...");
    \u0275\u0275elementEnd()();
  }
}
function GlobalAnalyzerComponent_Conditional_23_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 13)(1, "mat-card-content")(2, "mat-icon");
    \u0275\u0275text(3, "error");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "span");
    \u0275\u0275text(5);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r2.error());
  }
}
function GlobalAnalyzerComponent_Conditional_24_Conditional_45_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 13)(1, "mat-card-content")(2, "mat-icon");
    \u0275\u0275text(3, "warning");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "span");
    \u0275\u0275text(5);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const r_r4 = \u0275\u0275nextContext();
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(r_r4.counterexample);
  }
}
function GlobalAnalyzerComponent_Conditional_24_For_50_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-chip");
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const role_r5 = ctx.$implicit;
    \u0275\u0275advance();
    \u0275\u0275textInterpolate(role_r5);
  }
}
function GlobalAnalyzerComponent_Conditional_24_For_66_Conditional_0_Conditional_4_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-icon", 33);
    \u0275\u0275text(1, "check_circle");
    \u0275\u0275elementEnd();
  }
}
function GlobalAnalyzerComponent_Conditional_24_For_66_Conditional_0_Conditional_25_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "h4");
    \u0275\u0275text(1, "Local Hasse Diagram");
    \u0275\u0275elementEnd();
    \u0275\u0275element(2, "app-hasse-diagram", 29);
  }
  if (rf & 2) {
    const proj_r6 = \u0275\u0275nextContext();
    \u0275\u0275advance(2);
    \u0275\u0275property("svgHtml", proj_r6.localSvgHtml);
  }
}
function GlobalAnalyzerComponent_Conditional_24_For_66_Conditional_0_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-expansion-panel")(1, "mat-expansion-panel-header")(2, "mat-panel-title");
    \u0275\u0275text(3);
    \u0275\u0275conditionalCreate(4, GlobalAnalyzerComponent_Conditional_24_For_66_Conditional_0_Conditional_4_Template, 2, 0, "mat-icon", 33);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(5, "mat-panel-description");
    \u0275\u0275text(6);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(7, "div", 34)(8, "h4");
    \u0275\u0275text(9, "Local type");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(10, "pre", 27);
    \u0275\u0275text(11);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(12, "div", 35)(13, "span")(14, "strong");
    \u0275\u0275text(15, "States:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(16);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(17, "span")(18, "strong");
    \u0275\u0275text(19, "Transitions:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(20);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(21, "span")(22, "strong");
    \u0275\u0275text(23, "Lattice:");
    \u0275\u0275elementEnd();
    \u0275\u0275text(24);
    \u0275\u0275elementEnd()();
    \u0275\u0275conditionalCreate(25, GlobalAnalyzerComponent_Conditional_24_For_66_Conditional_0_Conditional_25_Template, 3, 1);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const proj_r6 = ctx;
    const role_r7 = \u0275\u0275nextContext().$implicit;
    \u0275\u0275advance(3);
    \u0275\u0275textInterpolate1(" ", role_r7, " ");
    \u0275\u0275advance();
    \u0275\u0275conditional(proj_r6.localIsLattice ? 4 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate2(" ", proj_r6.localStates, " states, ", proj_r6.localTransitions, " transitions ");
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(proj_r6.localType);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate1(" ", proj_r6.localStates);
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate1(" ", proj_r6.localTransitions);
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate1(" ", proj_r6.localIsLattice ? "Yes" : "No");
    \u0275\u0275advance();
    \u0275\u0275conditional(proj_r6.localSvgHtml ? 25 : -1);
  }
}
function GlobalAnalyzerComponent_Conditional_24_For_66_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275conditionalCreate(0, GlobalAnalyzerComponent_Conditional_24_For_66_Conditional_0_Template, 26, 9, "mat-expansion-panel");
  }
  if (rf & 2) {
    let tmp_12_0;
    const role_r7 = ctx.$implicit;
    const r_r4 = \u0275\u0275nextContext();
    \u0275\u0275conditional((tmp_12_0 = r_r4.projections[role_r7]) ? 0 : -1, tmp_12_0);
  }
}
function GlobalAnalyzerComponent_Conditional_24_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-tab-group", 14)(1, "mat-tab", 18)(2, "div", 19)(3, "div", 20)(4, "div", 21)(5, "mat-icon");
    \u0275\u0275text(6);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "span");
    \u0275\u0275text(8, "Lattice");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(9, "div", 22)(10, "mat-icon");
    \u0275\u0275text(11, "check_circle");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(12, "span");
    \u0275\u0275text(13);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(14, "div", 21)(15, "mat-icon");
    \u0275\u0275text(16);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(17, "span");
    \u0275\u0275text(18);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(19, "div", 21)(20, "mat-icon");
    \u0275\u0275text(21);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(22, "span");
    \u0275\u0275text(23);
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(24, "div", 23)(25, "div", 24)(26, "span", 25);
    \u0275\u0275text(27);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(28, "span", 26);
    \u0275\u0275text(29, "States");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(30, "div", 24)(31, "span", 25);
    \u0275\u0275text(32);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(33, "span", 26);
    \u0275\u0275text(34, "Transitions");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(35, "div", 24)(36, "span", 25);
    \u0275\u0275text(37);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(38, "span", 26);
    \u0275\u0275text(39, "SCCs");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(40, "div", 24)(41, "span", 25);
    \u0275\u0275text(42);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(43, "span", 26);
    \u0275\u0275text(44, "Roles");
    \u0275\u0275elementEnd()()();
    \u0275\u0275conditionalCreate(45, GlobalAnalyzerComponent_Conditional_24_Conditional_45_Template, 6, 1, "mat-card", 13);
    \u0275\u0275elementStart(46, "h3");
    \u0275\u0275text(47, "Roles");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(48, "mat-chip-set");
    \u0275\u0275repeaterCreate(49, GlobalAnalyzerComponent_Conditional_24_For_50_Template, 2, 1, "mat-chip", null, \u0275\u0275repeaterTrackByIdentity);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(51, "h3");
    \u0275\u0275text(52, "Pretty-printed");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(53, "pre", 27);
    \u0275\u0275text(54);
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(55, "mat-tab", 28)(56, "div", 19)(57, "h3");
    \u0275\u0275text(58, "Global State Space");
    \u0275\u0275elementEnd();
    \u0275\u0275element(59, "app-hasse-diagram", 29);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(60, "mat-tab", 30)(61, "div", 19)(62, "p");
    \u0275\u0275text(63, "Each role's local view of the protocol, obtained by projection.");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(64, "mat-accordion");
    \u0275\u0275repeaterCreate(65, GlobalAnalyzerComponent_Conditional_24_For_66_Template, 1, 1, null, null, \u0275\u0275repeaterTrackByIdentity);
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(67, "mat-tab", 31)(68, "div", 19);
    \u0275\u0275element(69, "app-code-block", 32);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const r_r4 = ctx;
    \u0275\u0275advance(4);
    \u0275\u0275classProp("pass", r_r4.isLattice)("fail", !r_r4.isLattice);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(r_r4.isLattice ? "check_circle" : "cancel");
    \u0275\u0275advance(7);
    \u0275\u0275textInterpolate1("", r_r4.numRoles, " roles");
    \u0275\u0275advance();
    \u0275\u0275classProp("pass", !r_r4.usesParallel)("info", r_r4.usesParallel);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(r_r4.usesParallel ? "call_split" : "linear_scale");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(r_r4.usesParallel ? "Parallel" : "Sequential");
    \u0275\u0275advance();
    \u0275\u0275classProp("pass", !r_r4.isRecursive)("info", r_r4.isRecursive);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(r_r4.isRecursive ? "loop" : "trending_flat");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(r_r4.isRecursive ? "Recursive" : "Finite");
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate(r_r4.numStates);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(r_r4.numTransitions);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(r_r4.numSccs);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(r_r4.numRoles);
    \u0275\u0275advance(3);
    \u0275\u0275conditional(r_r4.counterexample ? 45 : -1);
    \u0275\u0275advance(4);
    \u0275\u0275repeater(r_r4.roles);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(r_r4.pretty);
    \u0275\u0275advance(5);
    \u0275\u0275property("svgHtml", r_r4.svgHtml);
    \u0275\u0275advance(6);
    \u0275\u0275repeater(r_r4.roles);
    \u0275\u0275advance(4);
    \u0275\u0275property("code", r_r4.dotSource);
  }
}
var GlobalAnalyzerComponent = class _GlobalAnalyzerComponent {
  api;
  route;
  typeString = "";
  loading = signal(false, ...ngDevMode ? [{ debugName: "loading" }] : []);
  error = signal(null, ...ngDevMode ? [{ debugName: "error" }] : []);
  result = signal(null, ...ngDevMode ? [{ debugName: "result" }] : []);
  examples = [
    {
      name: "Request-Response",
      type: "Client -> Server : {request: Server -> Client : {response: end}}"
    },
    {
      name: "Two-Buyer",
      type: "Buyer1 -> Seller : {lookup: Seller -> Buyer1 : {price: Seller -> Buyer2 : {price: Buyer1 -> Buyer2 : {share: Buyer2 -> Seller : {accept: Seller -> Buyer2 : {deliver: end}, reject: end}}}}}"
    },
    {
      name: "Streaming",
      type: "rec X . Producer -> Consumer : {data: X, done: end}"
    },
    {
      name: "Two-Phase Commit",
      type: "Coord -> P : {prepare: P -> Coord : {yes: Coord -> P : {commit: end}, no: Coord -> P : {abort: end}}}"
    },
    {
      name: "Delegation",
      type: "Client -> Master : {task: Master -> Worker : {delegate: Worker -> Master : {result: Master -> Client : {response: end}}}}"
    },
    {
      name: "Auth-Service",
      type: "Client -> Server : {login: Server -> Client : {granted: rec X . Client -> Server : {request: Server -> Client : {response: X}, logout: end}, denied: end}}"
    }
  ];
  constructor(api, route) {
    this.api = api;
    this.route = route;
    this.route.queryParams.subscribe((params) => {
      if (params["type"]) {
        this.typeString = params["type"];
        this.analyze();
      }
    });
  }
  loadExample(type) {
    this.typeString = type;
    this.analyze();
  }
  analyze() {
    if (!this.typeString.trim())
      return;
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    this.api.analyzeGlobal(this.typeString.trim()).subscribe({
      next: (response) => {
        this.result.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || "Analysis failed");
        this.loading.set(false);
      }
    });
  }
  static \u0275fac = function GlobalAnalyzerComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _GlobalAnalyzerComponent)(\u0275\u0275directiveInject(ApiService), \u0275\u0275directiveInject(ActivatedRoute));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _GlobalAnalyzerComponent, selectors: [["app-global-analyzer"]], decls: 25, vars: 6, consts: [[1, "page-container"], [1, "subtitle"], [1, "input-section"], ["for", "global-type-input"], ["id", "global-type-input", "placeholder", "e.g. Buyer1 -> Seller : {lookup: Seller -> Buyer1 : {price: end}}", "rows", "4", 3, "ngModelChange", "keydown.control.enter", "keydown.meta.enter", "ngModel"], [1, "input-actions"], ["mat-raised-button", "", "color", "primary", 3, "click", "disabled"], [1, "hint"], [1, "examples"], [1, "examples-label"], ["mat-stroked-button", "", 1, "example-btn"], [1, "grammar-card"], [1, "loading"], [1, "error-card"], ["animationDuration", "0", 1, "results-tabs"], ["mat-stroked-button", "", 1, "example-btn", 3, "click"], [1, "grammar"], ["diameter", "40"], ["label", "Overview"], [1, "tab-content"], [1, "verdict-grid"], [1, "verdict-item"], [1, "verdict-item", "pass"], [1, "metrics-grid"], [1, "metric"], [1, "metric-value"], [1, "metric-label"], [1, "pretty-type"], ["label", "Hasse Diagram"], [3, "svgHtml"], ["label", "Projections"], ["label", "DOT"], ["label", "DOT source", 3, "code"], [1, "lattice-icon", "pass-icon"], [1, "projection-content"], [1, "proj-metrics"]], template: function GlobalAnalyzerComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "div", 0)(1, "h1");
      \u0275\u0275text(2, "Global Type Analyzer");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p", 1);
      \u0275\u0275text(4, " Analyze multiparty global session types: parse, build state space, check lattice properties, and project onto individual roles. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(5, "div", 2)(6, "label", 3);
      \u0275\u0275text(7, "Global type");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(8, "textarea", 4);
      \u0275\u0275twoWayListener("ngModelChange", function GlobalAnalyzerComponent_Template_textarea_ngModelChange_8_listener($event) {
        \u0275\u0275twoWayBindingSet(ctx.typeString, $event) || (ctx.typeString = $event);
        return $event;
      });
      \u0275\u0275listener("keydown.control.enter", function GlobalAnalyzerComponent_Template_textarea_keydown_control_enter_8_listener() {
        return ctx.analyze();
      })("keydown.meta.enter", function GlobalAnalyzerComponent_Template_textarea_keydown_meta_enter_8_listener() {
        return ctx.analyze();
      });
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(9, "div", 5)(10, "button", 6);
      \u0275\u0275listener("click", function GlobalAnalyzerComponent_Template_button_click_10_listener() {
        return ctx.analyze();
      });
      \u0275\u0275elementStart(11, "mat-icon");
      \u0275\u0275text(12, "play_arrow");
      \u0275\u0275elementEnd();
      \u0275\u0275text(13, " Analyze ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(14, "span", 7);
      \u0275\u0275text(15, "Ctrl+Enter");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(16, "div", 8)(17, "span", 9);
      \u0275\u0275text(18, "Examples:");
      \u0275\u0275elementEnd();
      \u0275\u0275repeaterCreate(19, GlobalAnalyzerComponent_For_20_Template, 2, 1, "button", 10, _forTrack0);
      \u0275\u0275elementEnd();
      \u0275\u0275conditionalCreate(21, GlobalAnalyzerComponent_Conditional_21_Template, 7, 2, "mat-card", 11);
      \u0275\u0275conditionalCreate(22, GlobalAnalyzerComponent_Conditional_22_Template, 4, 0, "div", 12);
      \u0275\u0275conditionalCreate(23, GlobalAnalyzerComponent_Conditional_23_Template, 6, 1, "mat-card", 13);
      \u0275\u0275conditionalCreate(24, GlobalAnalyzerComponent_Conditional_24_Template, 70, 26, "mat-tab-group", 14);
      \u0275\u0275elementEnd();
    }
    if (rf & 2) {
      let tmp_6_0;
      \u0275\u0275advance(8);
      \u0275\u0275twoWayProperty("ngModel", ctx.typeString);
      \u0275\u0275advance(2);
      \u0275\u0275property("disabled", ctx.loading());
      \u0275\u0275advance(9);
      \u0275\u0275repeater(ctx.examples);
      \u0275\u0275advance(2);
      \u0275\u0275conditional(!ctx.result() ? 21 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.loading() ? 22 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.error() ? 23 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional((tmp_6_0 = ctx.result()) ? 24 : -1, tmp_6_0);
    }
  }, dependencies: [
    CommonModule,
    FormsModule,
    DefaultValueAccessor,
    NgControlStatus,
    NgModel,
    MatTabsModule,
    MatTab,
    MatTabGroup,
    MatButtonModule,
    MatButton,
    MatIconModule,
    MatIcon,
    MatCardModule,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardTitle,
    MatChipsModule,
    MatChip,
    MatChipSet,
    MatProgressSpinnerModule,
    MatProgressSpinner,
    MatExpansionModule,
    MatAccordion,
    MatExpansionPanel,
    MatExpansionPanelHeader,
    MatExpansionPanelTitle,
    MatExpansionPanelDescription,
    HasseDiagramComponent,
    CodeBlockComponent
  ], styles: ['\n\n.page-container[_ngcontent-%COMP%] {\n  max-width: 900px;\n  margin: 0 auto;\n  padding: 2rem;\n}\nh1[_ngcontent-%COMP%] {\n  margin-bottom: 0.25rem;\n}\n.subtitle[_ngcontent-%COMP%] {\n  color: #64748b;\n  margin-bottom: 1.5rem;\n}\n.input-section[_ngcontent-%COMP%] {\n  margin-bottom: 1rem;\n}\n.input-section[_ngcontent-%COMP%]   label[_ngcontent-%COMP%] {\n  display: block;\n  font-weight: 600;\n  margin-bottom: 0.5rem;\n}\n.input-section[_ngcontent-%COMP%]   textarea[_ngcontent-%COMP%] {\n  width: 100%;\n  font-family: "JetBrains Mono", monospace;\n  font-size: 0.9rem;\n  padding: 0.75rem;\n  border: 1px solid #cbd5e1;\n  border-radius: 8px;\n  resize: vertical;\n  box-sizing: border-box;\n}\n.input-actions[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 1rem;\n  margin-top: 0.5rem;\n}\n.hint[_ngcontent-%COMP%] {\n  color: #94a3b8;\n  font-size: 0.8rem;\n}\n.examples[_ngcontent-%COMP%] {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 0.5rem;\n  align-items: center;\n  margin-bottom: 1.5rem;\n}\n.examples-label[_ngcontent-%COMP%] {\n  font-weight: 600;\n  color: #475569;\n}\n.example-btn[_ngcontent-%COMP%] {\n  font-size: 0.8rem;\n}\n.grammar-card[_ngcontent-%COMP%] {\n  margin-bottom: 1.5rem;\n}\n.grammar[_ngcontent-%COMP%] {\n  font-family: "JetBrains Mono", monospace;\n  font-size: 0.85rem;\n  margin: 0;\n}\n.loading[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 1rem;\n  padding: 2rem 0;\n}\n.error-card[_ngcontent-%COMP%] {\n  background: #fef2f2;\n  border: 1px solid #fecaca;\n  margin: 1rem 0;\n}\n.error-card[_ngcontent-%COMP%]   mat-card-content[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 0.5rem;\n  color: #dc2626;\n}\n.results-tabs[_ngcontent-%COMP%] {\n  margin-top: 1.5rem;\n}\n.tab-content[_ngcontent-%COMP%] {\n  padding: 1.5rem 0;\n}\n.verdict-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 1rem;\n  margin-bottom: 1.5rem;\n}\n.verdict-item[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 0.5rem;\n  padding: 0.75rem;\n  border-radius: 8px;\n  background: #f8fafc;\n  border: 1px solid #e2e8f0;\n}\n.verdict-item.pass[_ngcontent-%COMP%] {\n  background: #f0fdf4;\n  border-color: #bbf7d0;\n  color: #16a34a;\n}\n.verdict-item.fail[_ngcontent-%COMP%] {\n  background: #fef2f2;\n  border-color: #fecaca;\n  color: #dc2626;\n}\n.verdict-item.info[_ngcontent-%COMP%] {\n  background: #eff6ff;\n  border-color: #bfdbfe;\n  color: #2563eb;\n}\n.metrics-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 1rem;\n  margin-bottom: 1.5rem;\n}\n.metric[_ngcontent-%COMP%] {\n  text-align: center;\n  padding: 1rem;\n  background: #f8fafc;\n  border-radius: 8px;\n  border: 1px solid #e2e8f0;\n}\n.metric-value[_ngcontent-%COMP%] {\n  display: block;\n  font-size: 1.5rem;\n  font-weight: 700;\n  color: #1e293b;\n}\n.metric-label[_ngcontent-%COMP%] {\n  display: block;\n  font-size: 0.8rem;\n  color: #64748b;\n}\n.pretty-type[_ngcontent-%COMP%] {\n  font-family: "JetBrains Mono", monospace;\n  font-size: 0.85rem;\n  background: #f8fafc;\n  padding: 1rem;\n  border-radius: 8px;\n  border: 1px solid #e2e8f0;\n  overflow-x: auto;\n  white-space: pre-wrap;\n}\n.projection-content[_ngcontent-%COMP%] {\n  padding: 0.5rem 0;\n}\n.proj-metrics[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 2rem;\n  margin: 0.75rem 0;\n  color: #475569;\n}\n.lattice-icon[_ngcontent-%COMP%] {\n  margin-left: 0.5rem;\n  font-size: 18px;\n  height: 18px;\n  width: 18px;\n}\n.pass-icon[_ngcontent-%COMP%] {\n  color: #16a34a;\n}\nh3[_ngcontent-%COMP%] {\n  margin-top: 1.5rem;\n  margin-bottom: 0.75rem;\n}\nh4[_ngcontent-%COMP%] {\n  margin-top: 1rem;\n  margin-bottom: 0.5rem;\n}\n@media (max-width: 640px) {\n  .verdict-grid[_ngcontent-%COMP%], \n   .metrics-grid[_ngcontent-%COMP%] {\n    grid-template-columns: repeat(2, 1fr);\n  }\n}\n/*# sourceMappingURL=global-analyzer.component.css.map */'] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(GlobalAnalyzerComponent, [{
    type: Component,
    args: [{ selector: "app-global-analyzer", standalone: true, imports: [
      CommonModule,
      FormsModule,
      MatTabsModule,
      MatButtonModule,
      MatIconModule,
      MatCardModule,
      MatChipsModule,
      MatProgressSpinnerModule,
      MatExpansionModule,
      HasseDiagramComponent,
      CodeBlockComponent
    ], template: `
    <div class="page-container">
      <h1>Global Type Analyzer</h1>
      <p class="subtitle">
        Analyze multiparty global session types: parse, build state space,
        check lattice properties, and project onto individual roles.
      </p>

      <!-- Input -->
      <div class="input-section">
        <label for="global-type-input">Global type</label>
        <textarea
          id="global-type-input"
          [(ngModel)]="typeString"
          placeholder="e.g. Buyer1 -> Seller : {lookup: Seller -> Buyer1 : {price: end}}"
          rows="4"
          (keydown.control.enter)="analyze()"
          (keydown.meta.enter)="analyze()"
        ></textarea>
        <div class="input-actions">
          <button mat-raised-button color="primary" (click)="analyze()" [disabled]="loading()">
            <mat-icon>play_arrow</mat-icon>
            Analyze
          </button>
          <span class="hint">Ctrl+Enter</span>
        </div>
      </div>

      <!-- Quick examples -->
      <div class="examples">
        <span class="examples-label">Examples:</span>
        @for (ex of examples; track ex.name) {
          <button mat-stroked-button (click)="loadExample(ex.type)" class="example-btn">
            {{ ex.name }}
          </button>
        }
      </div>

      <!-- Grammar reference -->
      @if (!result()) {
        <mat-card class="grammar-card">
          <mat-card-header>
            <mat-card-title>Grammar</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <pre class="grammar">G  ::=  sender -> receiver : {{ '{' }} m\u2081 : G\u2081 , \u2026 , m\u2099 : G\u2099 {{ '}' }}
     |  G\u2081 || G\u2082
     |  rec X . G
     |  X
     |  end</pre>
          </mat-card-content>
        </mat-card>
      }

      <!-- Loading -->
      @if (loading()) {
        <div class="loading">
          <mat-spinner diameter="40"></mat-spinner>
          <span>Analyzing global type...</span>
        </div>
      }

      <!-- Error -->
      @if (error()) {
        <mat-card class="error-card">
          <mat-card-content>
            <mat-icon>error</mat-icon>
            <span>{{ error() }}</span>
          </mat-card-content>
        </mat-card>
      }

      <!-- Results -->
      @if (result(); as r) {
        <mat-tab-group class="results-tabs" animationDuration="0">

          <!-- Overview -->
          <mat-tab label="Overview">
            <div class="tab-content">
              <div class="verdict-grid">
                <div class="verdict-item" [class.pass]="r.isLattice" [class.fail]="!r.isLattice">
                  <mat-icon>{{ r.isLattice ? 'check_circle' : 'cancel' }}</mat-icon>
                  <span>Lattice</span>
                </div>
                <div class="verdict-item pass">
                  <mat-icon>check_circle</mat-icon>
                  <span>{{ r.numRoles }} roles</span>
                </div>
                <div class="verdict-item" [class.pass]="!r.usesParallel" [class.info]="r.usesParallel">
                  <mat-icon>{{ r.usesParallel ? 'call_split' : 'linear_scale' }}</mat-icon>
                  <span>{{ r.usesParallel ? 'Parallel' : 'Sequential' }}</span>
                </div>
                <div class="verdict-item" [class.pass]="!r.isRecursive" [class.info]="r.isRecursive">
                  <mat-icon>{{ r.isRecursive ? 'loop' : 'trending_flat' }}</mat-icon>
                  <span>{{ r.isRecursive ? 'Recursive' : 'Finite' }}</span>
                </div>
              </div>

              <div class="metrics-grid">
                <div class="metric"><span class="metric-value">{{ r.numStates }}</span><span class="metric-label">States</span></div>
                <div class="metric"><span class="metric-value">{{ r.numTransitions }}</span><span class="metric-label">Transitions</span></div>
                <div class="metric"><span class="metric-value">{{ r.numSccs }}</span><span class="metric-label">SCCs</span></div>
                <div class="metric"><span class="metric-value">{{ r.numRoles }}</span><span class="metric-label">Roles</span></div>
              </div>

              @if (r.counterexample) {
                <mat-card class="error-card">
                  <mat-card-content>
                    <mat-icon>warning</mat-icon>
                    <span>{{ r.counterexample }}</span>
                  </mat-card-content>
                </mat-card>
              }

              <h3>Roles</h3>
              <mat-chip-set>
                @for (role of r.roles; track role) {
                  <mat-chip>{{ role }}</mat-chip>
                }
              </mat-chip-set>

              <h3>Pretty-printed</h3>
              <pre class="pretty-type">{{ r.pretty }}</pre>
            </div>
          </mat-tab>

          <!-- Global Hasse Diagram -->
          <mat-tab label="Hasse Diagram">
            <div class="tab-content">
              <h3>Global State Space</h3>
              <app-hasse-diagram [svgHtml]="r.svgHtml"></app-hasse-diagram>
            </div>
          </mat-tab>

          <!-- Projections -->
          <mat-tab label="Projections">
            <div class="tab-content">
              <p>Each role's local view of the protocol, obtained by projection.</p>
              <mat-accordion>
                @for (role of r.roles; track role) {
                  @if (r.projections[role]; as proj) {
                    <mat-expansion-panel>
                      <mat-expansion-panel-header>
                        <mat-panel-title>
                          {{ role }}
                          @if (proj.localIsLattice) {
                            <mat-icon class="lattice-icon pass-icon">check_circle</mat-icon>
                          }
                        </mat-panel-title>
                        <mat-panel-description>
                          {{ proj.localStates }} states, {{ proj.localTransitions }} transitions
                        </mat-panel-description>
                      </mat-expansion-panel-header>

                      <div class="projection-content">
                        <h4>Local type</h4>
                        <pre class="pretty-type">{{ proj.localType }}</pre>

                        <div class="proj-metrics">
                          <span><strong>States:</strong> {{ proj.localStates }}</span>
                          <span><strong>Transitions:</strong> {{ proj.localTransitions }}</span>
                          <span><strong>Lattice:</strong> {{ proj.localIsLattice ? 'Yes' : 'No' }}</span>
                        </div>

                        @if (proj.localSvgHtml) {
                          <h4>Local Hasse Diagram</h4>
                          <app-hasse-diagram [svgHtml]="proj.localSvgHtml"></app-hasse-diagram>
                        }
                      </div>
                    </mat-expansion-panel>
                  }
                }
              </mat-accordion>
            </div>
          </mat-tab>

          <!-- DOT -->
          <mat-tab label="DOT">
            <div class="tab-content">
              <app-code-block [code]="r.dotSource" label="DOT source"></app-code-block>
            </div>
          </mat-tab>

        </mat-tab-group>
      }
    </div>
  `, styles: ['/* angular:styles/component:scss;09f743ba6d9edc832afa5bd94939114926b1809194b6531982883e8e1a4cdfe9;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/global-analyzer/global-analyzer.component.ts */\n.page-container {\n  max-width: 900px;\n  margin: 0 auto;\n  padding: 2rem;\n}\nh1 {\n  margin-bottom: 0.25rem;\n}\n.subtitle {\n  color: #64748b;\n  margin-bottom: 1.5rem;\n}\n.input-section {\n  margin-bottom: 1rem;\n}\n.input-section label {\n  display: block;\n  font-weight: 600;\n  margin-bottom: 0.5rem;\n}\n.input-section textarea {\n  width: 100%;\n  font-family: "JetBrains Mono", monospace;\n  font-size: 0.9rem;\n  padding: 0.75rem;\n  border: 1px solid #cbd5e1;\n  border-radius: 8px;\n  resize: vertical;\n  box-sizing: border-box;\n}\n.input-actions {\n  display: flex;\n  align-items: center;\n  gap: 1rem;\n  margin-top: 0.5rem;\n}\n.hint {\n  color: #94a3b8;\n  font-size: 0.8rem;\n}\n.examples {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 0.5rem;\n  align-items: center;\n  margin-bottom: 1.5rem;\n}\n.examples-label {\n  font-weight: 600;\n  color: #475569;\n}\n.example-btn {\n  font-size: 0.8rem;\n}\n.grammar-card {\n  margin-bottom: 1.5rem;\n}\n.grammar {\n  font-family: "JetBrains Mono", monospace;\n  font-size: 0.85rem;\n  margin: 0;\n}\n.loading {\n  display: flex;\n  align-items: center;\n  gap: 1rem;\n  padding: 2rem 0;\n}\n.error-card {\n  background: #fef2f2;\n  border: 1px solid #fecaca;\n  margin: 1rem 0;\n}\n.error-card mat-card-content {\n  display: flex;\n  align-items: center;\n  gap: 0.5rem;\n  color: #dc2626;\n}\n.results-tabs {\n  margin-top: 1.5rem;\n}\n.tab-content {\n  padding: 1.5rem 0;\n}\n.verdict-grid {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 1rem;\n  margin-bottom: 1.5rem;\n}\n.verdict-item {\n  display: flex;\n  align-items: center;\n  gap: 0.5rem;\n  padding: 0.75rem;\n  border-radius: 8px;\n  background: #f8fafc;\n  border: 1px solid #e2e8f0;\n}\n.verdict-item.pass {\n  background: #f0fdf4;\n  border-color: #bbf7d0;\n  color: #16a34a;\n}\n.verdict-item.fail {\n  background: #fef2f2;\n  border-color: #fecaca;\n  color: #dc2626;\n}\n.verdict-item.info {\n  background: #eff6ff;\n  border-color: #bfdbfe;\n  color: #2563eb;\n}\n.metrics-grid {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 1rem;\n  margin-bottom: 1.5rem;\n}\n.metric {\n  text-align: center;\n  padding: 1rem;\n  background: #f8fafc;\n  border-radius: 8px;\n  border: 1px solid #e2e8f0;\n}\n.metric-value {\n  display: block;\n  font-size: 1.5rem;\n  font-weight: 700;\n  color: #1e293b;\n}\n.metric-label {\n  display: block;\n  font-size: 0.8rem;\n  color: #64748b;\n}\n.pretty-type {\n  font-family: "JetBrains Mono", monospace;\n  font-size: 0.85rem;\n  background: #f8fafc;\n  padding: 1rem;\n  border-radius: 8px;\n  border: 1px solid #e2e8f0;\n  overflow-x: auto;\n  white-space: pre-wrap;\n}\n.projection-content {\n  padding: 0.5rem 0;\n}\n.proj-metrics {\n  display: flex;\n  gap: 2rem;\n  margin: 0.75rem 0;\n  color: #475569;\n}\n.lattice-icon {\n  margin-left: 0.5rem;\n  font-size: 18px;\n  height: 18px;\n  width: 18px;\n}\n.pass-icon {\n  color: #16a34a;\n}\nh3 {\n  margin-top: 1.5rem;\n  margin-bottom: 0.75rem;\n}\nh4 {\n  margin-top: 1rem;\n  margin-bottom: 0.5rem;\n}\n@media (max-width: 640px) {\n  .verdict-grid,\n  .metrics-grid {\n    grid-template-columns: repeat(2, 1fr);\n  }\n}\n/*# sourceMappingURL=global-analyzer.component.css.map */\n'] }]
  }], () => [{ type: ApiService }, { type: ActivatedRoute }], null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(GlobalAnalyzerComponent, { className: "GlobalAnalyzerComponent", filePath: "src/app/pages/global-analyzer/global-analyzer.component.ts", lineNumber: 290 });
})();
export {
  GlobalAnalyzerComponent
};
//# sourceMappingURL=chunk-LB7FUTCO.js.map
