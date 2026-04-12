import {
  MatFormFieldModule,
  MatInput,
  MatInputModule
} from "./chunk-43RQTZW4.js";
import {
  MatTab,
  MatTabGroup,
  MatTabLabel,
  MatTabsModule
} from "./chunk-FJHU3ZRV.js";
import {
  MatFormField,
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
  MatButtonModule,
  MatIconButton
} from "./chunk-BUK7DMBP.js";
import {
  ActivatedRoute
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
  ɵɵattribute,
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
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtemplate,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1,
  ɵɵtextInterpolate3
} from "./chunk-OWEA7TR3.js";

// src/app/pages/test-generator/test-generator.component.ts
function TestGeneratorComponent_Conditional_17_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "mat-spinner", 8);
  }
}
function TestGeneratorComponent_Conditional_18_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " Generate Tests ");
  }
}
function TestGeneratorComponent_Conditional_19_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "section", 9)(1, "mat-icon");
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
function TestGeneratorComponent_Conditional_20_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "section", 10)(1, "h3");
    \u0275\u0275text(2, "How it works");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "div", 12)(4, "div", 13)(5, "div", 14);
    \u0275\u0275text(6, "1");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "div")(8, "strong");
    \u0275\u0275text(9, "Valid paths");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(10, "p");
    \u0275\u0275text(11, "Complete execution traces from initial state to end, exercising all reachable branches.");
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(12, "div", 13)(13, "div", 14);
    \u0275\u0275text(14, "2");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(15, "div")(16, "strong");
    \u0275\u0275text(17, "Violations");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(18, "p");
    \u0275\u0275text(19, "Attempts to call methods that are not enabled in a given state \u2014 tests that the object rejects invalid operations.");
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(20, "div", 13)(21, "div", 14);
    \u0275\u0275text(22, "3");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(23, "div")(24, "strong");
    \u0275\u0275text(25, "Incomplete prefixes");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(26, "p");
    \u0275\u0275text(27, "Partial executions that stop before reaching end \u2014 tests for detecting abandoned sessions.");
    \u0275\u0275elementEnd()()()()();
  }
}
function TestGeneratorComponent_Conditional_21_Conditional_1_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-tab", 15)(1, "section", 17)(2, "div", 19)(3, "h3");
    \u0275\u0275text(4, "JUnit 5 Tests");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(5, "span", 20);
    \u0275\u0275text(6);
    \u0275\u0275elementEnd()();
    \u0275\u0275element(7, "app-code-block", 21);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance(6);
    \u0275\u0275textInterpolate1("Class: ", ctx_r0.className(), "ProtocolTest");
    \u0275\u0275advance();
    \u0275\u0275property("code", ctx_r0.testSource());
  }
}
function TestGeneratorComponent_Conditional_21_ng_template_3_Conditional_1_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "mat-spinner", 22);
  }
}
function TestGeneratorComponent_Conditional_21_ng_template_3_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " Coverage Storyboard ");
    \u0275\u0275conditionalCreate(1, TestGeneratorComponent_Conditional_21_ng_template_3_Conditional_1_Template, 1, 0, "mat-spinner", 22);
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r0.loadingCoverage() ? 1 : -1);
  }
}
function TestGeneratorComponent_Conditional_21_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    const _r2 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "div", 18)(1, "button", 23);
    \u0275\u0275listener("click", function TestGeneratorComponent_Conditional_21_Conditional_5_Template_button_click_1_listener() {
      \u0275\u0275restoreView(_r2);
      const ctx_r0 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r0.loadCoverage());
    });
    \u0275\u0275text(2, " Generate Coverage Storyboard ");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "p");
    \u0275\u0275text(4, "Visualise how each test covers the state space \u2014 green edges/states are exercised, gray are not.");
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance();
    \u0275\u0275property("disabled", ctx_r0.loadingCoverage() || !ctx_r0.typeString().trim());
  }
}
function TestGeneratorComponent_Conditional_21_Conditional_6_Template(rf, ctx) {
  if (rf & 1) {
    const _r3 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "div", 24)(1, "span", 25);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "span", 25);
    \u0275\u0275text(4);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(5, "span", 25);
    \u0275\u0275text(6);
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(7, "div", 26)(8, "button", 27);
    \u0275\u0275listener("click", function TestGeneratorComponent_Conditional_21_Conditional_6_Template_button_click_8_listener() {
      \u0275\u0275restoreView(_r3);
      const ctx_r0 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r0.prevFrame());
    });
    \u0275\u0275elementStart(9, "mat-icon");
    \u0275\u0275text(10, "chevron_left");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(11, "span", 28);
    \u0275\u0275text(12);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(13, "button", 27);
    \u0275\u0275listener("click", function TestGeneratorComponent_Conditional_21_Conditional_6_Template_button_click_13_listener() {
      \u0275\u0275restoreView(_r3);
      const ctx_r0 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r0.nextFrame());
    });
    \u0275\u0275elementStart(14, "mat-icon");
    \u0275\u0275text(15, "chevron_right");
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(16, "div", 29)(17, "span", 30);
    \u0275\u0275text(18);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(19, "span");
    \u0275\u0275text(20);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(21, "span");
    \u0275\u0275text(22);
    \u0275\u0275elementEnd()();
    \u0275\u0275element(23, "app-hasse-diagram", 31);
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext(2);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r0.coverageTotalTransitions(), " transitions");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r0.coverageTotalStates(), " states");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", ctx_r0.coverageFrames().length, " frames");
    \u0275\u0275advance(2);
    \u0275\u0275property("disabled", ctx_r0.currentFrame() === 0);
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate3(" ", ctx_r0.currentFrame() + 1, " / ", ctx_r0.coverageFrames().length, " \u2014 ", ctx_r0.coverageFrames()[ctx_r0.currentFrame()].testName, " ");
    \u0275\u0275advance();
    \u0275\u0275property("disabled", ctx_r0.currentFrame() >= ctx_r0.coverageFrames().length - 1);
    \u0275\u0275advance(4);
    \u0275\u0275attribute("data-kind", ctx_r0.coverageFrames()[ctx_r0.currentFrame()].testKind);
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1(" ", ctx_r0.coverageFrames()[ctx_r0.currentFrame()].testKind, " ");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("Transition coverage: ", (ctx_r0.coverageFrames()[ctx_r0.currentFrame()].transitionCoverage * 100).toFixed(1), "%");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("State coverage: ", (ctx_r0.coverageFrames()[ctx_r0.currentFrame()].stateCoverage * 100).toFixed(1), "%");
    \u0275\u0275advance();
    \u0275\u0275property("svgHtml", ctx_r0.coverageFrames()[ctx_r0.currentFrame()].svgHtml);
  }
}
function TestGeneratorComponent_Conditional_21_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-tab-group", 11);
    \u0275\u0275conditionalCreate(1, TestGeneratorComponent_Conditional_21_Conditional_1_Template, 8, 2, "mat-tab", 15);
    \u0275\u0275elementStart(2, "mat-tab");
    \u0275\u0275template(3, TestGeneratorComponent_Conditional_21_ng_template_3_Template, 2, 1, "ng-template", 16);
    \u0275\u0275elementStart(4, "section", 17);
    \u0275\u0275conditionalCreate(5, TestGeneratorComponent_Conditional_21_Conditional_5_Template, 5, 1, "div", 18);
    \u0275\u0275conditionalCreate(6, TestGeneratorComponent_Conditional_21_Conditional_6_Template, 24, 13);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext();
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r0.testSource() ? 1 : -1);
    \u0275\u0275advance(4);
    \u0275\u0275conditional(ctx_r0.coverageFrames().length === 0 && !ctx_r0.loadingCoverage() ? 5 : -1);
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r0.coverageFrames().length > 0 ? 6 : -1);
  }
}
var TestGeneratorComponent = class _TestGeneratorComponent {
  api;
  route;
  snackBar;
  typeString = signal("", ...ngDevMode ? [{ debugName: "typeString" }] : []);
  className = signal("", ...ngDevMode ? [{ debugName: "className" }] : []);
  testSource = signal("", ...ngDevMode ? [{ debugName: "testSource" }] : []);
  error = signal("", ...ngDevMode ? [{ debugName: "error" }] : []);
  generating = signal(false, ...ngDevMode ? [{ debugName: "generating" }] : []);
  // Coverage storyboard
  coverageFrames = signal([], ...ngDevMode ? [{ debugName: "coverageFrames" }] : []);
  coverageTotalTransitions = signal(0, ...ngDevMode ? [{ debugName: "coverageTotalTransitions" }] : []);
  coverageTotalStates = signal(0, ...ngDevMode ? [{ debugName: "coverageTotalStates" }] : []);
  loadingCoverage = signal(false, ...ngDevMode ? [{ debugName: "loadingCoverage" }] : []);
  currentFrame = signal(0, ...ngDevMode ? [{ debugName: "currentFrame" }] : []);
  constructor(api, route, snackBar) {
    this.api = api;
    this.route = route;
    this.snackBar = snackBar;
  }
  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      if (params["type"]) {
        this.typeString.set(params["type"]);
      }
      if (params["class"]) {
        this.className.set(params["class"]);
      }
    });
  }
  generate() {
    if (!this.typeString().trim() || !this.className().trim())
      return;
    this.generating.set(true);
    this.testSource.set("");
    this.error.set("");
    this.coverageFrames.set([]);
    const request = {
      typeString: this.typeString(),
      className: this.className()
    };
    this.api.generateTests(request).subscribe({
      next: (res) => {
        this.testSource.set(res.testSource);
        this.generating.set(false);
        this.loadCoverage();
      },
      error: (err) => {
        this.error.set(err.error?.error || err.message || "Test generation failed");
        this.generating.set(false);
      }
    });
  }
  loadCoverage() {
    if (!this.typeString().trim())
      return;
    this.loadingCoverage.set(true);
    this.currentFrame.set(0);
    this.api.coverageStoryboard(this.typeString()).subscribe({
      next: (res) => {
        this.coverageFrames.set(res.frames);
        this.coverageTotalTransitions.set(res.totalTransitions);
        this.coverageTotalStates.set(res.totalStates);
        this.loadingCoverage.set(false);
      },
      error: (err) => {
        this.snackBar.open(err.error?.error || "Coverage storyboard failed", "Close", { duration: 5e3 });
        this.loadingCoverage.set(false);
      }
    });
  }
  prevFrame() {
    if (this.currentFrame() > 0) {
      this.currentFrame.set(this.currentFrame() - 1);
    }
  }
  nextFrame() {
    if (this.currentFrame() < this.coverageFrames().length - 1) {
      this.currentFrame.set(this.currentFrame() + 1);
    }
  }
  static \u0275fac = function TestGeneratorComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _TestGeneratorComponent)(\u0275\u0275directiveInject(ApiService), \u0275\u0275directiveInject(ActivatedRoute), \u0275\u0275directiveInject(MatSnackBar));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _TestGeneratorComponent, selectors: [["app-test-generator"]], decls: 22, vars: 7, consts: [[1, "page-header"], [1, "form-section"], ["appearance", "outline", 1, "full-width"], ["matInput", "", "rows", "3", "placeholder", "e.g. rec X . &{read: X, done: end}", 3, "ngModelChange", "ngModel"], [1, "form-row"], ["appearance", "outline", 1, "class-name-input"], ["matInput", "", "placeholder", "e.g. FileHandle", 3, "ngModelChange", "ngModel"], ["mat-flat-button", "", "color", "primary", 1, "generate-btn", 3, "click", "disabled"], ["diameter", "20"], [1, "error-card"], [1, "help-section"], ["animationDuration", "200ms", 1, "result-tabs"], [1, "help-grid"], [1, "help-card"], [1, "help-number"], ["label", "Generated Tests"], ["mat-tab-label", ""], [1, "result-section"], [1, "coverage-prompt"], [1, "result-header"], [1, "result-meta"], ["label", "JUnit 5", 3, "code"], ["diameter", "16", 1, "tab-spinner"], ["mat-flat-button", "", "color", "primary", 3, "click", "disabled"], [1, "coverage-stats"], [1, "stat-chip"], [1, "coverage-controls"], ["mat-icon-button", "", 3, "click", "disabled"], [1, "frame-label"], [1, "frame-meta"], [1, "kind-chip"], [3, "svgHtml"]], template: function TestGeneratorComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "Test Generator");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4, "Generate JUnit 5 tests from session type definitions: valid paths, protocol violations, and incomplete prefixes.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(5, "section", 1)(6, "mat-form-field", 2)(7, "mat-label");
      \u0275\u0275text(8, "Session type");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(9, "textarea", 3);
      \u0275\u0275listener("ngModelChange", function TestGeneratorComponent_Template_textarea_ngModelChange_9_listener($event) {
        return ctx.typeString.set($event);
      });
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(10, "div", 4)(11, "mat-form-field", 5)(12, "mat-label");
      \u0275\u0275text(13, "Class name");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(14, "input", 6);
      \u0275\u0275listener("ngModelChange", function TestGeneratorComponent_Template_input_ngModelChange_14_listener($event) {
        return ctx.className.set($event);
      });
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(15, "div", 4)(16, "button", 7);
      \u0275\u0275listener("click", function TestGeneratorComponent_Template_button_click_16_listener() {
        return ctx.generate();
      });
      \u0275\u0275conditionalCreate(17, TestGeneratorComponent_Conditional_17_Template, 1, 0, "mat-spinner", 8)(18, TestGeneratorComponent_Conditional_18_Template, 1, 0);
      \u0275\u0275elementEnd()()();
      \u0275\u0275conditionalCreate(19, TestGeneratorComponent_Conditional_19_Template, 5, 1, "section", 9);
      \u0275\u0275conditionalCreate(20, TestGeneratorComponent_Conditional_20_Template, 28, 0, "section", 10);
      \u0275\u0275conditionalCreate(21, TestGeneratorComponent_Conditional_21_Template, 7, 3, "mat-tab-group", 11);
    }
    if (rf & 2) {
      \u0275\u0275advance(9);
      \u0275\u0275property("ngModel", ctx.typeString());
      \u0275\u0275advance(5);
      \u0275\u0275property("ngModel", ctx.className());
      \u0275\u0275advance(2);
      \u0275\u0275property("disabled", ctx.generating() || !ctx.typeString().trim() || !ctx.className().trim());
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.generating() ? 17 : 18);
      \u0275\u0275advance(2);
      \u0275\u0275conditional(ctx.error() ? 19 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional(!ctx.testSource() && !ctx.generating() && !ctx.error() ? 20 : -1);
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.testSource() || ctx.coverageFrames().length > 0 ? 21 : -1);
    }
  }, dependencies: [
    FormsModule,
    DefaultValueAccessor,
    NgControlStatus,
    NgModel,
    MatFormFieldModule,
    MatFormField,
    MatLabel,
    MatInputModule,
    MatInput,
    MatButtonModule,
    MatButton,
    MatIconButton,
    MatProgressSpinnerModule,
    MatProgressSpinner,
    MatIconModule,
    MatIcon,
    MatTabsModule,
    MatTabLabel,
    MatTab,
    MatTabGroup,
    MatSnackBarModule,
    CodeBlockComponent,
    HasseDiagramComponent
  ], styles: ['\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.form-section[_ngcontent-%COMP%] {\n  margin-bottom: 24px;\n}\n.full-width[_ngcontent-%COMP%] {\n  width: 100%;\n}\n.form-row[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 12px;\n  align-items: flex-start;\n  flex-wrap: wrap;\n}\n.class-name-input[_ngcontent-%COMP%] {\n  flex: 1;\n  min-width: 180px;\n}\n.generate-btn[_ngcontent-%COMP%] {\n  height: 56px;\n  min-width: 160px;\n}\n.error-card[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 12px;\n  padding: 16px;\n  margin: 16px 0;\n  border: 2px solid #d32f2f;\n  border-radius: 8px;\n  background: #fce4ec;\n  color: #b71c1c;\n}\n.help-section[_ngcontent-%COMP%] {\n  margin: 24px 0;\n}\n.help-section[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 15px;\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.7);\n  margin: 0 0 16px;\n}\n.help-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));\n  gap: 16px;\n}\n.help-card[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 14px;\n  padding: 18px;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 10px;\n  background: #fafafa;\n}\n.help-number[_ngcontent-%COMP%] {\n  flex-shrink: 0;\n  width: 32px;\n  height: 32px;\n  border-radius: 50%;\n  background: var(--brand-primary, #4338ca);\n  color: white;\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  font-weight: 600;\n  font-size: 14px;\n}\n.help-card[_ngcontent-%COMP%]   strong[_ngcontent-%COMP%] {\n  display: block;\n  margin-bottom: 4px;\n  font-size: 14px;\n}\n.help-card[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  margin: 0;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n  line-height: 1.5;\n}\n.result-tabs[_ngcontent-%COMP%] {\n  margin-top: 16px;\n}\n.result-section[_ngcontent-%COMP%] {\n  padding: 16px 0;\n}\n.result-header[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: baseline;\n  gap: 16px;\n  margin-bottom: 12px;\n}\n.result-header[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 15px;\n  font-weight: 600;\n  margin: 0;\n  color: rgba(0, 0, 0, 0.7);\n}\n.result-meta[_ngcontent-%COMP%] {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.45);\n  font-family: "JetBrains Mono", monospace;\n}\n.tab-spinner[_ngcontent-%COMP%] {\n  display: inline-block;\n  margin-left: 8px;\n}\n.coverage-prompt[_ngcontent-%COMP%] {\n  text-align: center;\n  padding: 32px 0;\n}\n.coverage-prompt[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  margin: 12px 0 0;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.5);\n}\n.coverage-stats[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 10px;\n  justify-content: center;\n  padding: 8px 0 16px;\n}\n.stat-chip[_ngcontent-%COMP%] {\n  display: inline-block;\n  padding: 4px 12px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n  border-radius: 16px;\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.7);\n}\n.coverage-controls[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  gap: 8px;\n  margin-bottom: 8px;\n}\n.frame-label[_ngcontent-%COMP%] {\n  font-size: 13px;\n  font-weight: 500;\n  min-width: 200px;\n  text-align: center;\n}\n.frame-meta[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 16px;\n  justify-content: center;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n  margin-bottom: 8px;\n}\n.kind-chip[_ngcontent-%COMP%] {\n  padding: 2px 10px;\n  border-radius: 10px;\n  font-size: 11px;\n  font-weight: 600;\n  text-transform: uppercase;\n  letter-spacing: 0.3px;\n}\n.kind-chip[data-kind=valid][_ngcontent-%COMP%] {\n  background: #dcfce7;\n  color: #166534;\n}\n.kind-chip[data-kind=violation][_ngcontent-%COMP%] {\n  background: #fee2e2;\n  color: #991b1b;\n}\n.kind-chip[data-kind=incomplete][_ngcontent-%COMP%] {\n  background: #fef3c7;\n  color: #92400e;\n}\n/*# sourceMappingURL=test-generator.component.css.map */'] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TestGeneratorComponent, [{
    type: Component,
    args: [{ selector: "app-test-generator", standalone: true, imports: [
      FormsModule,
      MatFormFieldModule,
      MatInputModule,
      MatButtonModule,
      MatProgressSpinnerModule,
      MatIconModule,
      MatTabsModule,
      MatSnackBarModule,
      CodeBlockComponent,
      HasseDiagramComponent
    ], template: `
    <header class="page-header">
      <h1>Test Generator</h1>
      <p>Generate JUnit 5 tests from session type definitions: valid paths, protocol violations, and incomplete prefixes.</p>
    </header>

    <!-- Input form -->
    <section class="form-section">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Session type</mat-label>
        <textarea matInput
                  [ngModel]="typeString()"
                  (ngModelChange)="typeString.set($event)"
                  rows="3"
                  placeholder="e.g. rec X . &{read: X, done: end}"></textarea>
      </mat-form-field>

      <div class="form-row">
        <mat-form-field appearance="outline" class="class-name-input">
          <mat-label>Class name</mat-label>
          <input matInput
                 [ngModel]="className()"
                 (ngModelChange)="className.set($event)"
                 placeholder="e.g. FileHandle">
        </mat-form-field>
      </div>

      <div class="form-row">
        <button mat-flat-button
                color="primary"
                class="generate-btn"
                [disabled]="generating() || !typeString().trim() || !className().trim()"
                (click)="generate()">
          @if (generating()) {
            <mat-spinner diameter="20"></mat-spinner>
          } @else {
            Generate Tests
          }
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

    <!-- Help text (when no result yet) -->
    @if (!testSource() && !generating() && !error()) {
      <section class="help-section">
        <h3>How it works</h3>
        <div class="help-grid">
          <div class="help-card">
            <div class="help-number">1</div>
            <div>
              <strong>Valid paths</strong>
              <p>Complete execution traces from initial state to end, exercising all reachable branches.</p>
            </div>
          </div>
          <div class="help-card">
            <div class="help-number">2</div>
            <div>
              <strong>Violations</strong>
              <p>Attempts to call methods that are not enabled in a given state &mdash; tests that the object rejects invalid operations.</p>
            </div>
          </div>
          <div class="help-card">
            <div class="help-number">3</div>
            <div>
              <strong>Incomplete prefixes</strong>
              <p>Partial executions that stop before reaching end &mdash; tests for detecting abandoned sessions.</p>
            </div>
          </div>
        </div>
      </section>
    }

    <!-- Result tabs -->
    @if (testSource() || coverageFrames().length > 0) {
      <mat-tab-group class="result-tabs" animationDuration="200ms">
        @if (testSource()) {
          <mat-tab label="Generated Tests">
            <section class="result-section">
              <div class="result-header">
                <h3>JUnit 5 Tests</h3>
                <span class="result-meta">Class: {{ className() }}ProtocolTest</span>
              </div>
              <app-code-block [code]="testSource()" label="JUnit 5"></app-code-block>
            </section>
          </mat-tab>
        }
        <mat-tab>
          <ng-template mat-tab-label>
            Coverage Storyboard
            @if (loadingCoverage()) {
              <mat-spinner diameter="16" class="tab-spinner"></mat-spinner>
            }
          </ng-template>
          <section class="result-section">
            @if (coverageFrames().length === 0 && !loadingCoverage()) {
              <div class="coverage-prompt">
                <button mat-flat-button color="primary"
                        [disabled]="loadingCoverage() || !typeString().trim()"
                        (click)="loadCoverage()">
                  Generate Coverage Storyboard
                </button>
                <p>Visualise how each test covers the state space \u2014 green edges/states are exercised, gray are not.</p>
              </div>
            }
            @if (coverageFrames().length > 0) {
              <div class="coverage-stats">
                <span class="stat-chip">{{ coverageTotalTransitions() }} transitions</span>
                <span class="stat-chip">{{ coverageTotalStates() }} states</span>
                <span class="stat-chip">{{ coverageFrames().length }} frames</span>
              </div>
              <div class="coverage-controls">
                <button mat-icon-button [disabled]="currentFrame() === 0" (click)="prevFrame()">
                  <mat-icon>chevron_left</mat-icon>
                </button>
                <span class="frame-label">
                  {{ currentFrame() + 1 }} / {{ coverageFrames().length }}
                  &mdash; {{ coverageFrames()[currentFrame()].testName }}
                </span>
                <button mat-icon-button [disabled]="currentFrame() >= coverageFrames().length - 1" (click)="nextFrame()">
                  <mat-icon>chevron_right</mat-icon>
                </button>
              </div>
              <div class="frame-meta">
                <span class="kind-chip" [attr.data-kind]="coverageFrames()[currentFrame()].testKind">
                  {{ coverageFrames()[currentFrame()].testKind }}
                </span>
                <span>Transition coverage: {{ (coverageFrames()[currentFrame()].transitionCoverage * 100).toFixed(1) }}%</span>
                <span>State coverage: {{ (coverageFrames()[currentFrame()].stateCoverage * 100).toFixed(1) }}%</span>
              </div>
              <app-hasse-diagram [svgHtml]="coverageFrames()[currentFrame()].svgHtml"></app-hasse-diagram>
            }
          </section>
        </mat-tab>
      </mat-tab-group>
    }
  `, styles: ['/* angular:styles/component:scss;e5f39bea612c9c8091b0358e08f8f98032fff1a0349b541184bceddccc1c3a04;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/test-generator/test-generator.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.form-section {\n  margin-bottom: 24px;\n}\n.full-width {\n  width: 100%;\n}\n.form-row {\n  display: flex;\n  gap: 12px;\n  align-items: flex-start;\n  flex-wrap: wrap;\n}\n.class-name-input {\n  flex: 1;\n  min-width: 180px;\n}\n.generate-btn {\n  height: 56px;\n  min-width: 160px;\n}\n.error-card {\n  display: flex;\n  align-items: center;\n  gap: 12px;\n  padding: 16px;\n  margin: 16px 0;\n  border: 2px solid #d32f2f;\n  border-radius: 8px;\n  background: #fce4ec;\n  color: #b71c1c;\n}\n.help-section {\n  margin: 24px 0;\n}\n.help-section h3 {\n  font-size: 15px;\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.7);\n  margin: 0 0 16px;\n}\n.help-grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));\n  gap: 16px;\n}\n.help-card {\n  display: flex;\n  gap: 14px;\n  padding: 18px;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 10px;\n  background: #fafafa;\n}\n.help-number {\n  flex-shrink: 0;\n  width: 32px;\n  height: 32px;\n  border-radius: 50%;\n  background: var(--brand-primary, #4338ca);\n  color: white;\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  font-weight: 600;\n  font-size: 14px;\n}\n.help-card strong {\n  display: block;\n  margin-bottom: 4px;\n  font-size: 14px;\n}\n.help-card p {\n  margin: 0;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n  line-height: 1.5;\n}\n.result-tabs {\n  margin-top: 16px;\n}\n.result-section {\n  padding: 16px 0;\n}\n.result-header {\n  display: flex;\n  align-items: baseline;\n  gap: 16px;\n  margin-bottom: 12px;\n}\n.result-header h3 {\n  font-size: 15px;\n  font-weight: 600;\n  margin: 0;\n  color: rgba(0, 0, 0, 0.7);\n}\n.result-meta {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.45);\n  font-family: "JetBrains Mono", monospace;\n}\n.tab-spinner {\n  display: inline-block;\n  margin-left: 8px;\n}\n.coverage-prompt {\n  text-align: center;\n  padding: 32px 0;\n}\n.coverage-prompt p {\n  margin: 12px 0 0;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.5);\n}\n.coverage-stats {\n  display: flex;\n  gap: 10px;\n  justify-content: center;\n  padding: 8px 0 16px;\n}\n.stat-chip {\n  display: inline-block;\n  padding: 4px 12px;\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n  border-radius: 16px;\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.7);\n}\n.coverage-controls {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  gap: 8px;\n  margin-bottom: 8px;\n}\n.frame-label {\n  font-size: 13px;\n  font-weight: 500;\n  min-width: 200px;\n  text-align: center;\n}\n.frame-meta {\n  display: flex;\n  gap: 16px;\n  justify-content: center;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n  margin-bottom: 8px;\n}\n.kind-chip {\n  padding: 2px 10px;\n  border-radius: 10px;\n  font-size: 11px;\n  font-weight: 600;\n  text-transform: uppercase;\n  letter-spacing: 0.3px;\n}\n.kind-chip[data-kind=valid] {\n  background: #dcfce7;\n  color: #166534;\n}\n.kind-chip[data-kind=violation] {\n  background: #fee2e2;\n  color: #991b1b;\n}\n.kind-chip[data-kind=incomplete] {\n  background: #fef3c7;\n  color: #92400e;\n}\n/*# sourceMappingURL=test-generator.component.css.map */\n'] }]
  }], () => [{ type: ApiService }, { type: ActivatedRoute }, { type: MatSnackBar }], null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(TestGeneratorComponent, { className: "TestGeneratorComponent", filePath: "src/app/pages/test-generator/test-generator.component.ts", lineNumber: 341 });
})();
export {
  TestGeneratorComponent
};
//# sourceMappingURL=chunk-WEEN5E44.js.map
