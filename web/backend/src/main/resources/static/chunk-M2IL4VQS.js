import {
  MatChipsModule
} from "./chunk-F3DYJDGJ.js";
import {
  MatFormFieldModule,
  MatInput,
  MatInputModule
} from "./chunk-43RQTZW4.js";
import {
  MatTab,
  MatTabGroup,
  MatTabsModule
} from "./chunk-FJHU3ZRV.js";
import {
  MatFormField,
  MatHint,
  MatLabel
} from "./chunk-RSSZT2MJ.js";
import {
  DefaultValueAccessor,
  FormsModule,
  NgControlStatus,
  NgModel
} from "./chunk-2AQDFUQH.js";
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
  CodeBlockComponent,
  MatSnackBar,
  MatSnackBarModule
} from "./chunk-EFHCE74K.js";
import "./chunk-R2VWAHTD.js";
import "./chunk-SUS3PTUT.js";
import {
  MatButton,
  MatButtonModule
} from "./chunk-BUK7DMBP.js";
import {
  ActivatedRoute,
  Router
} from "./chunk-QTYX35EO.js";
import {
  MatIcon,
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
  ɵɵtextInterpolate3
} from "./chunk-OWEA7TR3.js";

// src/app/pages/analyzer/analyzer.component.ts
var _forTrack0 = ($index, $item) => $item.label;
function AnalyzerComponent_Conditional_14_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "mat-spinner", 6);
  }
}
function AnalyzerComponent_Conditional_15_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " Analyze ");
  }
}
function AnalyzerComponent_Conditional_20_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "section", 8)(1, "mat-icon");
    \u0275\u0275text(2, "error_outline");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "span");
    \u0275\u0275text(4);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext();
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate(ctx_r0.error());
  }
}
function AnalyzerComponent_Conditional_21_For_5_Template(rf, ctx) {
  if (rf & 1) {
    const _r2 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "button", 16);
    \u0275\u0275listener("click", function AnalyzerComponent_Conditional_21_For_5_Template_button_click_0_listener() {
      const ex_r3 = \u0275\u0275restoreView(_r2).$implicit;
      const ctx_r0 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r0.loadExample(ex_r3));
    });
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ex_r3 = ctx.$implicit;
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1(" ", ex_r3.label, " ");
  }
}
function AnalyzerComponent_Conditional_21_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "section", 10)(1, "p", 11);
    \u0275\u0275text(2, "Quick examples:");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "div", 12);
    \u0275\u0275repeaterCreate(4, AnalyzerComponent_Conditional_21_For_5_Template, 2, 1, "button", 13, _forTrack0);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(6, "section", 14)(7, "h3");
    \u0275\u0275text(8, "Session Type Grammar");
    \u0275\u0275elementEnd();
    \u0275\u0275element(9, "app-code-block", 15);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext();
    \u0275\u0275advance(4);
    \u0275\u0275repeater(ctx_r0.quickExamples);
    \u0275\u0275advance(5);
    \u0275\u0275property("code", ctx_r0.grammarRef);
  }
}
function AnalyzerComponent_Conditional_22_Conditional_21_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 22)(1, "div", 23);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "div", 24);
    \u0275\u0275text(4, "Thread Safe");
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275classProp("pass", ctx_r0.result().threadSafe)("fail", !ctx_r0.result().threadSafe);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(ctx_r0.result().threadSafe ? "\u2713" : "\u2717");
  }
}
function AnalyzerComponent_Conditional_22_Conditional_22_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "p", 26);
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1("Counterexample: ", ctx_r0.result().counterexample);
  }
}
function AnalyzerComponent_Conditional_22_Conditional_95_For_4_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "span", 40);
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const m_r4 = ctx.$implicit;
    \u0275\u0275advance();
    \u0275\u0275textInterpolate(m_r4);
  }
}
function AnalyzerComponent_Conditional_22_Conditional_95_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "h3");
    \u0275\u0275text(1, "Methods");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(2, "div", 39);
    \u0275\u0275repeaterCreate(3, AnalyzerComponent_Conditional_22_Conditional_95_For_4_Template, 2, 1, "span", 40, \u0275\u0275repeaterTrackByIdentity);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance(3);
    \u0275\u0275repeater(ctx_r0.result().methods);
  }
}
function AnalyzerComponent_Conditional_22_Conditional_98_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 35);
    \u0275\u0275element(1, "app-hasse-diagram", 41);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance();
    \u0275\u0275property("svgHtml", ctx_r0.result().svgHtml);
  }
}
function AnalyzerComponent_Conditional_22_Conditional_99_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "p", 36);
    \u0275\u0275text(1, "No diagram available for this session type.");
    \u0275\u0275elementEnd();
  }
}
function AnalyzerComponent_Conditional_22_Conditional_102_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "app-code-block", 38);
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275property("code", ctx_r0.result().dotSource);
  }
}
function AnalyzerComponent_Conditional_22_Conditional_103_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "p", 36);
    \u0275\u0275text(1, "No DOT source available.");
    \u0275\u0275elementEnd();
  }
}
function AnalyzerComponent_Conditional_22_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "section", 9);
    \u0275\u0275element(1, "app-code-block", 17);
    \u0275\u0275elementStart(2, "mat-tab-group", 18)(3, "mat-tab", 19)(4, "div", 20)(5, "div", 21)(6, "div", 22)(7, "div", 23);
    \u0275\u0275text(8);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(9, "div", 24);
    \u0275\u0275text(10, "Lattice");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(11, "div", 22)(12, "div", 23);
    \u0275\u0275text(13);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(14, "div", 24);
    \u0275\u0275text(15, "Terminates");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(16, "div", 22)(17, "div", 23);
    \u0275\u0275text(18);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(19, "div", 24);
    \u0275\u0275text(20, "WF-Par");
    \u0275\u0275elementEnd()();
    \u0275\u0275conditionalCreate(21, AnalyzerComponent_Conditional_22_Conditional_21_Template, 5, 5, "div", 25);
    \u0275\u0275elementEnd();
    \u0275\u0275conditionalCreate(22, AnalyzerComponent_Conditional_22_Conditional_22_Template, 2, 1, "p", 26);
    \u0275\u0275elementStart(23, "h3");
    \u0275\u0275text(24, "Summary");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(25, "div", 27)(26, "div", 28)(27, "span", 29);
    \u0275\u0275text(28);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(29, "span", 30);
    \u0275\u0275text(30, "States");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(31, "div", 28)(32, "span", 29);
    \u0275\u0275text(33);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(34, "span", 30);
    \u0275\u0275text(35, "Transitions");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(36, "div", 28)(37, "span", 29);
    \u0275\u0275text(38);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(39, "span", 30);
    \u0275\u0275text(40, "Methods");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(41, "div", 28)(42, "span", 29);
    \u0275\u0275text(43);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(44, "span", 30);
    \u0275\u0275text(45, "Test Paths");
    \u0275\u0275elementEnd()()()()();
    \u0275\u0275elementStart(46, "mat-tab", 31)(47, "div", 20)(48, "h3");
    \u0275\u0275text(49, "Metrics");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(50, "div", 32)(51, "table", 33)(52, "tbody")(53, "tr")(54, "td");
    \u0275\u0275text(55, "States");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(56, "td");
    \u0275\u0275text(57);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(58, "tr")(59, "td");
    \u0275\u0275text(60, "Transitions");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(61, "td");
    \u0275\u0275text(62);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(63, "tr")(64, "td");
    \u0275\u0275text(65, "SCCs");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(66, "td");
    \u0275\u0275text(67);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(68, "tr")(69, "td");
    \u0275\u0275text(70, "Methods");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(71, "td");
    \u0275\u0275text(72);
    \u0275\u0275elementEnd()()()();
    \u0275\u0275elementStart(73, "table", 33)(74, "tbody")(75, "tr")(76, "td");
    \u0275\u0275text(77, "Uses parallel");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(78, "td");
    \u0275\u0275text(79);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(80, "tr")(81, "td");
    \u0275\u0275text(82, "Recursive");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(83, "td");
    \u0275\u0275text(84);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(85, "tr")(86, "td");
    \u0275\u0275text(87, "Test paths");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(88, "td");
    \u0275\u0275text(89);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(90, "tr")(91, "td");
    \u0275\u0275text(92, "Breakdown");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(93, "td");
    \u0275\u0275text(94);
    \u0275\u0275elementEnd()()()()();
    \u0275\u0275conditionalCreate(95, AnalyzerComponent_Conditional_22_Conditional_95_Template, 5, 0);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(96, "mat-tab", 34)(97, "div", 20);
    \u0275\u0275conditionalCreate(98, AnalyzerComponent_Conditional_22_Conditional_98_Template, 2, 1, "div", 35)(99, AnalyzerComponent_Conditional_22_Conditional_99_Template, 2, 0, "p", 36);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(100, "mat-tab", 37)(101, "div", 20);
    \u0275\u0275conditionalCreate(102, AnalyzerComponent_Conditional_22_Conditional_102_Template, 1, 1, "app-code-block", 38)(103, AnalyzerComponent_Conditional_22_Conditional_103_Template, 2, 0, "p", 36);
    \u0275\u0275elementEnd()()()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext();
    \u0275\u0275advance();
    \u0275\u0275property("code", ctx_r0.result().pretty);
    \u0275\u0275advance(5);
    \u0275\u0275classProp("pass", ctx_r0.result().isLattice)("fail", !ctx_r0.result().isLattice);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(ctx_r0.result().isLattice ? "\u2713" : "\u2717");
    \u0275\u0275advance(3);
    \u0275\u0275classProp("pass", ctx_r0.result().terminates)("fail", !ctx_r0.result().terminates);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(ctx_r0.result().terminates ? "\u2713" : "\u2717");
    \u0275\u0275advance(3);
    \u0275\u0275classProp("pass", ctx_r0.result().wfParallel)("fail", !ctx_r0.result().wfParallel);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(ctx_r0.result().wfParallel ? "\u2713" : "\u2717");
    \u0275\u0275advance(3);
    \u0275\u0275conditional(ctx_r0.result().usesParallel ? 21 : -1);
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r0.result().counterexample ? 22 : -1);
    \u0275\u0275advance(6);
    \u0275\u0275textInterpolate(ctx_r0.result().numStates);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numTransitions);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numMethods);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numTests);
    \u0275\u0275advance(14);
    \u0275\u0275textInterpolate(ctx_r0.result().numStates);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numTransitions);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numSccs);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numMethods);
    \u0275\u0275advance(7);
    \u0275\u0275textInterpolate(ctx_r0.result().usesParallel ? "Yes" : "No");
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().isRecursive ? "Yes (depth " + ctx_r0.result().recDepth + ")" : "No");
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.result().numTests);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate3("", ctx_r0.result().numValidPaths, " valid \xB7 ", ctx_r0.result().numViolations, " violations \xB7 ", ctx_r0.result().numIncomplete, " incomplete");
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r0.result().methods && ctx_r0.result().methods.length > 0 ? 95 : -1);
    \u0275\u0275advance(3);
    \u0275\u0275conditional(ctx_r0.result().svgHtml ? 98 : 99);
    \u0275\u0275advance(4);
    \u0275\u0275conditional(ctx_r0.result().dotSource ? 102 : 103);
  }
}
var AnalyzerComponent = class _AnalyzerComponent {
  api;
  route;
  router;
  snackBar;
  typeString = signal("", ...ngDevMode ? [{ debugName: "typeString" }] : []);
  result = signal(null, ...ngDevMode ? [{ debugName: "result" }] : []);
  error = signal("", ...ngDevMode ? [{ debugName: "error" }] : []);
  analyzing = signal(false, ...ngDevMode ? [{ debugName: "analyzing" }] : []);
  quickExamples = [
    { label: "Iterator", typeString: "rec X . &{hasNext: +{true: &{next: X}, false: end}}" },
    { label: "SMTP", typeString: "&{ehlo: &{mail: &{rcpt: &{data: &{send: +{ok: end, error: end}}}}}}" },
    { label: "Two-Buyer", typeString: "&{quote: +{accept: &{deliver: end}, reject: end}}" },
    { label: "File Handle", typeString: "&{open: +{ok: (rec X . &{read: X, done: end} || rec Y . &{write: Y, done: end}) . &{close: end}, error: end}}" },
    { label: "Simple", typeString: "&{a: &{b: end, c: end}}" }
  ];
  grammarRef = `S  ::=  &{ m\u2081 : S\u2081 , ... , m\u2099 : S\u2099 }    -- branch (external choice)
     |  +{ l\u2081 : S\u2081 , ... , l\u2099 : S\u2099 }    -- selection (internal choice)
     |  ( S\u2081 || S\u2082 )                    -- parallel
     |  rec X . S                        -- recursion
     |  X                                -- variable
     |  end                              -- terminated
     |  S\u2081 . S\u2082                          -- sequencing`;
  constructor(api, route, router, snackBar) {
    this.api = api;
    this.route = route;
    this.router = router;
    this.snackBar = snackBar;
  }
  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      if (params["type"]) {
        this.typeString.set(params["type"]);
        setTimeout(() => this.analyze(), 0);
      }
    });
  }
  loadExample(example) {
    this.typeString.set(example.typeString);
    this.analyze();
  }
  analyze() {
    if (!this.typeString().trim())
      return;
    this.analyzing.set(true);
    this.result.set(null);
    this.error.set("");
    this.api.analyze(this.typeString()).subscribe({
      next: (res) => {
        this.result.set(res);
        this.analyzing.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error || err.message || "Analysis failed");
        this.analyzing.set(false);
      }
    });
  }
  copyLink() {
    const url = new URL(window.location.href);
    url.pathname = "/tools/analyzer";
    url.searchParams.set("type", this.typeString());
    navigator.clipboard.writeText(url.toString()).then(() => {
      this.snackBar.open("Link copied to clipboard", "", { duration: 2e3 });
    });
  }
  static \u0275fac = function AnalyzerComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _AnalyzerComponent)(\u0275\u0275directiveInject(ApiService), \u0275\u0275directiveInject(ActivatedRoute), \u0275\u0275directiveInject(Router), \u0275\u0275directiveInject(MatSnackBar));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _AnalyzerComponent, selectors: [["app-analyzer"]], decls: 23, vars: 7, consts: [[1, "page-header"], [1, "form-section"], ["appearance", "outline", 1, "full-width"], ["matInput", "", "rows", "3", "placeholder", "e.g. rec X . &{read: X, done: end}", 3, "ngModelChange", "keydown.control.enter", "ngModel"], [1, "form-row"], ["mat-flat-button", "", "color", "primary", 1, "analyze-btn", 3, "click", "disabled"], ["diameter", "20"], ["mat-stroked-button", "", 1, "copy-link-btn", 3, "click", "disabled"], [1, "error-card"], [1, "results-tabs"], [1, "quick-examples"], [1, "quick-examples-label"], [1, "quick-examples-row"], ["mat-stroked-button", "", 1, "quick-example-btn"], [1, "grammar-section"], ["label", "Grammar", 3, "code"], ["mat-stroked-button", "", 1, "quick-example-btn", 3, "click"], ["label", "Pretty-printed", 3, "code"], ["animationDuration", "200ms", 1, "result-tab-group"], ["label", "Overview"], [1, "tab-content"], [1, "verdict-grid"], [1, "verdict-card"], [1, "verdict-icon"], [1, "verdict-label"], [1, "verdict-card", 3, "pass", "fail"], [1, "counterexample"], [1, "summary-grid"], [1, "summary-item"], [1, "summary-value"], [1, "summary-label"], ["label", "State Space"], [1, "metrics-grid"], [1, "metrics-table"], ["label", "Hasse Diagram"], [1, "hasse-container"], [1, "tab-empty"], ["label", "DOT"], ["label", "DOT", 3, "code"], [1, "methods-list"], [1, "method-chip"], [3, "svgHtml"]], template: function AnalyzerComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "Interactive Analyzer");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4, "Parse a session type, build its state space, check lattice properties, and visualize the Hasse diagram.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(5, "section", 1)(6, "mat-form-field", 2)(7, "mat-label");
      \u0275\u0275text(8, "Session type");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(9, "textarea", 3);
      \u0275\u0275listener("ngModelChange", function AnalyzerComponent_Template_textarea_ngModelChange_9_listener($event) {
        return ctx.typeString.set($event);
      })("keydown.control.enter", function AnalyzerComponent_Template_textarea_keydown_control_enter_9_listener() {
        return ctx.analyze();
      });
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(10, "mat-hint");
      \u0275\u0275text(11, "Press Ctrl+Enter to analyze");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(12, "div", 4)(13, "button", 5);
      \u0275\u0275listener("click", function AnalyzerComponent_Template_button_click_13_listener() {
        return ctx.analyze();
      });
      \u0275\u0275conditionalCreate(14, AnalyzerComponent_Conditional_14_Template, 1, 0, "mat-spinner", 6)(15, AnalyzerComponent_Conditional_15_Template, 1, 0);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(16, "button", 7);
      \u0275\u0275listener("click", function AnalyzerComponent_Template_button_click_16_listener() {
        return ctx.copyLink();
      });
      \u0275\u0275elementStart(17, "mat-icon");
      \u0275\u0275text(18, "link");
      \u0275\u0275elementEnd();
      \u0275\u0275text(19, " Copy link ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275conditionalCreate(20, AnalyzerComponent_Conditional_20_Template, 5, 1, "section", 8);
      \u0275\u0275conditionalCreate(21, AnalyzerComponent_Conditional_21_Template, 10, 1);
      \u0275\u0275conditionalCreate(22, AnalyzerComponent_Conditional_22_Template, 104, 35, "section", 9);
    }
    if (rf & 2) {
      \u0275\u0275advance(9);
      \u0275\u0275property("ngModel", ctx.typeString());
      \u0275\u0275advance(4);
      \u0275\u0275property("disabled", ctx.analyzing() || !ctx.typeString().trim());
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.analyzing() ? 14 : 15);
      \u0275\u0275advance(2);
      \u0275\u0275property("disabled", !ctx.typeString().trim());
      \u0275\u0275advance(4);
      \u0275\u0275conditional(ctx.error() ? 20 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional(!ctx.result() && !ctx.analyzing() ? 21 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.result() ? 22 : -1);
    }
  }, dependencies: [
    FormsModule,
    DefaultValueAccessor,
    NgControlStatus,
    NgModel,
    MatFormFieldModule,
    MatFormField,
    MatLabel,
    MatHint,
    MatInputModule,
    MatInput,
    MatButtonModule,
    MatButton,
    MatProgressSpinnerModule,
    MatProgressSpinner,
    MatIconModule,
    MatIcon,
    MatChipsModule,
    MatSnackBarModule,
    MatTabsModule,
    MatTab,
    MatTabGroup,
    CodeBlockComponent,
    HasseDiagramComponent
  ], styles: ['\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.form-section[_ngcontent-%COMP%] {\n  margin-bottom: 24px;\n}\n.full-width[_ngcontent-%COMP%] {\n  width: 100%;\n}\n.form-row[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 12px;\n  align-items: flex-start;\n  flex-wrap: wrap;\n}\n.analyze-btn[_ngcontent-%COMP%] {\n  height: 56px;\n  min-width: 120px;\n}\n.copy-link-btn[_ngcontent-%COMP%] {\n  height: 56px;\n}\n.error-card[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 12px;\n  padding: 16px;\n  margin: 16px 0;\n  border: 2px solid #d32f2f;\n  border-radius: 8px;\n  background: #fce4ec;\n  color: #b71c1c;\n}\n.quick-examples[_ngcontent-%COMP%] {\n  margin: 24px 0;\n  padding: 20px;\n  border: 1px dashed rgba(0, 0, 0, 0.15);\n  border-radius: 12px;\n  text-align: center;\n}\n.quick-examples-label[_ngcontent-%COMP%] {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.5);\n  margin: 0 0 12px;\n}\n.quick-examples-row[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 8px;\n  justify-content: center;\n  flex-wrap: wrap;\n}\n.quick-example-btn[_ngcontent-%COMP%] {\n  font-size: 13px;\n  border-color: var(--brand-primary, #4338ca);\n  color: var(--brand-primary, #4338ca);\n}\n.grammar-section[_ngcontent-%COMP%] {\n  margin: 24px 0;\n}\n.grammar-section[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 15px;\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.7);\n  margin: 0 0 10px;\n}\n.results-tabs[_ngcontent-%COMP%] {\n  margin: 24px 0;\n}\n.result-tab-group[_ngcontent-%COMP%] {\n  margin-top: 16px;\n}\n.tab-content[_ngcontent-%COMP%] {\n  padding: 20px 0;\n}\n.tab-content[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 15px;\n  font-weight: 600;\n  margin: 20px 0 10px;\n  color: rgba(0, 0, 0, 0.7);\n}\n.tab-content[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%]:first-child {\n  margin-top: 0;\n}\n.tab-empty[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.45);\n  font-size: 14px;\n  text-align: center;\n  padding: 32px 16px;\n}\n.verdict-grid[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 12px;\n  flex-wrap: wrap;\n}\n.verdict-card[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n  padding: 10px 18px;\n  border-radius: 8px;\n  font-weight: 500;\n  font-size: 14px;\n  border: 1px solid;\n}\n.verdict-card.pass[_ngcontent-%COMP%] {\n  background: #ecfdf5;\n  border-color: #a7f3d0;\n  color: #065f46;\n}\n.verdict-card.fail[_ngcontent-%COMP%] {\n  background: #fef2f2;\n  border-color: #fecaca;\n  color: #991b1b;\n}\n.verdict-icon[_ngcontent-%COMP%] {\n  font-size: 18px;\n}\n.verdict-label[_ngcontent-%COMP%] {\n  font-size: 13px;\n}\n.counterexample[_ngcontent-%COMP%] {\n  font-size: 13px;\n  color: #991b1b;\n  background: #fef2f2;\n  padding: 8px 14px;\n  border-radius: 6px;\n  border: 1px solid #fecaca;\n  margin: 12px 0;\n}\n.summary-grid[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 16px;\n  flex-wrap: wrap;\n}\n.summary-item[_ngcontent-%COMP%] {\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  padding: 16px 24px;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  min-width: 100px;\n}\n.summary-value[_ngcontent-%COMP%] {\n  font-size: 28px;\n  font-weight: 600;\n  color: var(--brand-primary, #4338ca);\n}\n.summary-label[_ngcontent-%COMP%] {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  margin-top: 4px;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n}\n.metrics-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 16px;\n}\n@media (max-width: 600px) {\n  .metrics-grid[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n}\n.metrics-table[_ngcontent-%COMP%] {\n  width: 100%;\n  border-collapse: collapse;\n}\n.metrics-table[_ngcontent-%COMP%]   td[_ngcontent-%COMP%] {\n  padding: 7px 14px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n  font-size: 14px;\n}\n.metrics-table[_ngcontent-%COMP%]   td[_ngcontent-%COMP%]:last-child {\n  text-align: right;\n  font-weight: 500;\n}\n.methods-list[_ngcontent-%COMP%] {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 6px;\n}\n.method-chip[_ngcontent-%COMP%] {\n  display: inline-block;\n  padding: 4px 12px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 16px;\n  font-size: 13px;\n  font-family: "JetBrains Mono", monospace;\n  color: rgba(0, 0, 0, 0.7);\n}\n.hasse-container[_ngcontent-%COMP%] {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  background: #fafafa;\n  padding: 8px;\n}\n/*# sourceMappingURL=analyzer.component.css.map */'] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(AnalyzerComponent, [{
    type: Component,
    args: [{ selector: "app-analyzer", standalone: true, imports: [
      FormsModule,
      MatFormFieldModule,
      MatInputModule,
      MatButtonModule,
      MatProgressSpinnerModule,
      MatIconModule,
      MatChipsModule,
      MatSnackBarModule,
      MatTabsModule,
      CodeBlockComponent,
      HasseDiagramComponent
    ], template: `
    <header class="page-header">
      <h1>Interactive Analyzer</h1>
      <p>Parse a session type, build its state space, check lattice properties, and visualize the Hasse diagram.</p>
    </header>

    <!-- Input form (always visible) -->
    <section class="form-section">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Session type</mat-label>
        <textarea matInput
                  [ngModel]="typeString()"
                  (ngModelChange)="typeString.set($event)"
                  rows="3"
                  placeholder="e.g. rec X . &{read: X, done: end}"
                  (keydown.control.enter)="analyze()"></textarea>
        <mat-hint>Press Ctrl+Enter to analyze</mat-hint>
      </mat-form-field>

      <div class="form-row">
        <button mat-flat-button
                color="primary"
                class="analyze-btn"
                [disabled]="analyzing() || !typeString().trim()"
                (click)="analyze()">
          @if (analyzing()) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            Analyze
          }
        </button>

        <button mat-stroked-button
                class="copy-link-btn"
                (click)="copyLink()"
                [disabled]="!typeString().trim()">
          <mat-icon>link</mat-icon>
          Copy link
        </button>
      </div>
    </section>

    <!-- Error -->
    @if (error()) {
      <section class="error-card">
        <mat-icon>error_outline</mat-icon>
        <span>{{ error() }}</span>
      </section>
    }

    <!-- Quick examples (when no result yet) -->
    @if (!result() && !analyzing()) {
      <section class="quick-examples">
        <p class="quick-examples-label">Quick examples:</p>
        <div class="quick-examples-row">
          @for (ex of quickExamples; track ex.label) {
            <button mat-stroked-button
                    class="quick-example-btn"
                    (click)="loadExample(ex)">
              {{ ex.label }}
            </button>
          }
        </div>
      </section>

      <!-- Grammar reference -->
      <section class="grammar-section">
        <h3>Session Type Grammar</h3>
        <app-code-block [code]="grammarRef" label="Grammar"></app-code-block>
      </section>
    }

    <!-- Results tabs -->
    @if (result()) {
      <section class="results-tabs">
        <app-code-block [code]="result()!.pretty" label="Pretty-printed"></app-code-block>

        <mat-tab-group animationDuration="200ms" class="result-tab-group">

          <!-- Overview tab -->
          <mat-tab label="Overview">
            <div class="tab-content">
              <div class="verdict-grid">
                <div class="verdict-card" [class.pass]="result()!.isLattice" [class.fail]="!result()!.isLattice">
                  <div class="verdict-icon">{{ result()!.isLattice ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">Lattice</div>
                </div>
                <div class="verdict-card" [class.pass]="result()!.terminates" [class.fail]="!result()!.terminates">
                  <div class="verdict-icon">{{ result()!.terminates ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">Terminates</div>
                </div>
                <div class="verdict-card" [class.pass]="result()!.wfParallel" [class.fail]="!result()!.wfParallel">
                  <div class="verdict-icon">{{ result()!.wfParallel ? '\u2713' : '\u2717' }}</div>
                  <div class="verdict-label">WF-Par</div>
                </div>
                @if (result()!.usesParallel) {
                  <div class="verdict-card" [class.pass]="result()!.threadSafe" [class.fail]="!result()!.threadSafe">
                    <div class="verdict-icon">{{ result()!.threadSafe ? '\u2713' : '\u2717' }}</div>
                    <div class="verdict-label">Thread Safe</div>
                  </div>
                }
              </div>

              @if (result()!.counterexample) {
                <p class="counterexample">Counterexample: {{ result()!.counterexample }}</p>
              }

              <h3>Summary</h3>
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numStates }}</span>
                  <span class="summary-label">States</span>
                </div>
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numTransitions }}</span>
                  <span class="summary-label">Transitions</span>
                </div>
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numMethods }}</span>
                  <span class="summary-label">Methods</span>
                </div>
                <div class="summary-item">
                  <span class="summary-value">{{ result()!.numTests }}</span>
                  <span class="summary-label">Test Paths</span>
                </div>
              </div>
            </div>
          </mat-tab>

          <!-- State Space tab -->
          <mat-tab label="State Space">
            <div class="tab-content">
              <h3>Metrics</h3>
              <div class="metrics-grid">
                <table class="metrics-table">
                  <tbody>
                    <tr><td>States</td><td>{{ result()!.numStates }}</td></tr>
                    <tr><td>Transitions</td><td>{{ result()!.numTransitions }}</td></tr>
                    <tr><td>SCCs</td><td>{{ result()!.numSccs }}</td></tr>
                    <tr><td>Methods</td><td>{{ result()!.numMethods }}</td></tr>
                  </tbody>
                </table>
                <table class="metrics-table">
                  <tbody>
                    <tr><td>Uses parallel</td><td>{{ result()!.usesParallel ? 'Yes' : 'No' }}</td></tr>
                    <tr><td>Recursive</td><td>{{ result()!.isRecursive ? 'Yes (depth ' + result()!.recDepth + ')' : 'No' }}</td></tr>
                    <tr><td>Test paths</td><td>{{ result()!.numTests }}</td></tr>
                    <tr>
                      <td>Breakdown</td>
                      <td>{{ result()!.numValidPaths }} valid &middot; {{ result()!.numViolations }} violations &middot; {{ result()!.numIncomplete }} incomplete</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              @if (result()!.methods && result()!.methods.length > 0) {
                <h3>Methods</h3>
                <div class="methods-list">
                  @for (m of result()!.methods; track m) {
                    <span class="method-chip">{{ m }}</span>
                  }
                </div>
              }
            </div>
          </mat-tab>

          <!-- Hasse Diagram tab -->
          <mat-tab label="Hasse Diagram">
            <div class="tab-content">
              @if (result()!.svgHtml) {
                <div class="hasse-container">
                  <app-hasse-diagram [svgHtml]="result()!.svgHtml"></app-hasse-diagram>
                </div>
              } @else {
                <p class="tab-empty">No diagram available for this session type.</p>
              }
            </div>
          </mat-tab>

          <!-- DOT tab -->
          <mat-tab label="DOT">
            <div class="tab-content">
              @if (result()!.dotSource) {
                <app-code-block [code]="result()!.dotSource" label="DOT"></app-code-block>
              } @else {
                <p class="tab-empty">No DOT source available.</p>
              }
            </div>
          </mat-tab>

        </mat-tab-group>
      </section>
    }
  `, styles: ['/* angular:styles/component:scss;effbb97b8d4d2fceb8229c71daa6501b549e0d666e63e5894aa347a2954c149d;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/analyzer/analyzer.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.form-section {\n  margin-bottom: 24px;\n}\n.full-width {\n  width: 100%;\n}\n.form-row {\n  display: flex;\n  gap: 12px;\n  align-items: flex-start;\n  flex-wrap: wrap;\n}\n.analyze-btn {\n  height: 56px;\n  min-width: 120px;\n}\n.copy-link-btn {\n  height: 56px;\n}\n.error-card {\n  display: flex;\n  align-items: center;\n  gap: 12px;\n  padding: 16px;\n  margin: 16px 0;\n  border: 2px solid #d32f2f;\n  border-radius: 8px;\n  background: #fce4ec;\n  color: #b71c1c;\n}\n.quick-examples {\n  margin: 24px 0;\n  padding: 20px;\n  border: 1px dashed rgba(0, 0, 0, 0.15);\n  border-radius: 12px;\n  text-align: center;\n}\n.quick-examples-label {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.5);\n  margin: 0 0 12px;\n}\n.quick-examples-row {\n  display: flex;\n  gap: 8px;\n  justify-content: center;\n  flex-wrap: wrap;\n}\n.quick-example-btn {\n  font-size: 13px;\n  border-color: var(--brand-primary, #4338ca);\n  color: var(--brand-primary, #4338ca);\n}\n.grammar-section {\n  margin: 24px 0;\n}\n.grammar-section h3 {\n  font-size: 15px;\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.7);\n  margin: 0 0 10px;\n}\n.results-tabs {\n  margin: 24px 0;\n}\n.result-tab-group {\n  margin-top: 16px;\n}\n.tab-content {\n  padding: 20px 0;\n}\n.tab-content h3 {\n  font-size: 15px;\n  font-weight: 600;\n  margin: 20px 0 10px;\n  color: rgba(0, 0, 0, 0.7);\n}\n.tab-content h3:first-child {\n  margin-top: 0;\n}\n.tab-empty {\n  color: rgba(0, 0, 0, 0.45);\n  font-size: 14px;\n  text-align: center;\n  padding: 32px 16px;\n}\n.verdict-grid {\n  display: flex;\n  gap: 12px;\n  flex-wrap: wrap;\n}\n.verdict-card {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n  padding: 10px 18px;\n  border-radius: 8px;\n  font-weight: 500;\n  font-size: 14px;\n  border: 1px solid;\n}\n.verdict-card.pass {\n  background: #ecfdf5;\n  border-color: #a7f3d0;\n  color: #065f46;\n}\n.verdict-card.fail {\n  background: #fef2f2;\n  border-color: #fecaca;\n  color: #991b1b;\n}\n.verdict-icon {\n  font-size: 18px;\n}\n.verdict-label {\n  font-size: 13px;\n}\n.counterexample {\n  font-size: 13px;\n  color: #991b1b;\n  background: #fef2f2;\n  padding: 8px 14px;\n  border-radius: 6px;\n  border: 1px solid #fecaca;\n  margin: 12px 0;\n}\n.summary-grid {\n  display: flex;\n  gap: 16px;\n  flex-wrap: wrap;\n}\n.summary-item {\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  padding: 16px 24px;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  min-width: 100px;\n}\n.summary-value {\n  font-size: 28px;\n  font-weight: 600;\n  color: var(--brand-primary, #4338ca);\n}\n.summary-label {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  margin-top: 4px;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n}\n.metrics-grid {\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 16px;\n}\n@media (max-width: 600px) {\n  .metrics-grid {\n    grid-template-columns: 1fr;\n  }\n}\n.metrics-table {\n  width: 100%;\n  border-collapse: collapse;\n}\n.metrics-table td {\n  padding: 7px 14px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n  font-size: 14px;\n}\n.metrics-table td:last-child {\n  text-align: right;\n  font-weight: 500;\n}\n.methods-list {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 6px;\n}\n.method-chip {\n  display: inline-block;\n  padding: 4px 12px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 16px;\n  font-size: 13px;\n  font-family: "JetBrains Mono", monospace;\n  color: rgba(0, 0, 0, 0.7);\n}\n.hasse-container {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  background: #fafafa;\n  padding: 8px;\n}\n/*# sourceMappingURL=analyzer.component.css.map */\n'] }]
  }], () => [{ type: ApiService }, { type: ActivatedRoute }, { type: Router }, { type: MatSnackBar }], null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(AnalyzerComponent, { className: "AnalyzerComponent", filePath: "src/app/pages/analyzer/analyzer.component.ts", lineNumber: 448 });
})();
export {
  AnalyzerComponent
};
//# sourceMappingURL=chunk-M2IL4VQS.js.map
