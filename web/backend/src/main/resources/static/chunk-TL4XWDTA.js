import {
  ApiService
} from "./chunk-EOCOQ6DB.js";
import {
  CodeBlockComponent
} from "./chunk-EFHCE74K.js";
import "./chunk-R2VWAHTD.js";
import "./chunk-SUS3PTUT.js";
import "./chunk-BUK7DMBP.js";
import {
  ActivatedRoute,
  RouterLink
} from "./chunk-QTYX35EO.js";
import "./chunk-BFW3NWZD.js";
import "./chunk-ZG4TCI7P.js";
import "./chunk-NL2TMNRB.js";
import {
  ChangeDetectorRef,
  Component,
  forkJoin,
  inject,
  setClassMetadata,
  signal,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵdefineComponent,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵlistener,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵpureFunction1,
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵsanitizeHtml,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1,
  ɵɵtextInterpolate2
} from "./chunk-OWEA7TR3.js";

// src/app/pages/tutorials/tutorial-detail.component.ts
var _c0 = (a0) => ["/tutorials", a0];
var _forTrack0 = ($index, $item) => $item.title;
function TutorialDetailComponent_Conditional_5_For_4_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "li")(1, "a", 9);
    \u0275\u0275listener("click", function TutorialDetailComponent_Conditional_5_For_4_Template_a_click_1_listener() {
      const \u0275$index_16_r2 = \u0275\u0275restoreView(_r1).$index;
      const ctx_r2 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r2.scrollToStep(\u0275$index_16_r2));
    });
    \u0275\u0275text(2);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const step_r4 = ctx.$implicit;
    const \u0275$index_16_r2 = ctx.$index;
    const ctx_r2 = \u0275\u0275nextContext(2);
    \u0275\u0275classProp("active", ctx_r2.activeStepIndex() === \u0275$index_16_r2);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(step_r4.title);
  }
}
function TutorialDetailComponent_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "h3");
    \u0275\u0275text(1, "Steps");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(2, "ul");
    \u0275\u0275repeaterCreate(3, TutorialDetailComponent_Conditional_5_For_4_Template, 3, 3, "li", 8, _forTrack0);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275advance(3);
    \u0275\u0275repeater(ctx_r2.tutorial().steps);
  }
}
function TutorialDetailComponent_Conditional_7_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 5);
    \u0275\u0275text(1, "Loading...");
    \u0275\u0275elementEnd();
  }
}
function TutorialDetailComponent_Conditional_8_For_6_Conditional_4_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "app-code-block", 17);
  }
  if (rf & 2) {
    const step_r5 = \u0275\u0275nextContext().$implicit;
    \u0275\u0275property("code", step_r5.code)("label", step_r5.codeLabel || "");
  }
}
function TutorialDetailComponent_Conditional_8_For_6_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 11)(1, "h3");
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275element(3, "p", 16);
    \u0275\u0275conditionalCreate(4, TutorialDetailComponent_Conditional_8_For_6_Conditional_4_Template, 1, 2, "app-code-block", 17);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const step_r5 = ctx.$implicit;
    const \u0275$index_37_r6 = ctx.$index;
    \u0275\u0275property("id", "step-" + \u0275$index_37_r6);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(step_r5.title);
    \u0275\u0275advance();
    \u0275\u0275property("innerHTML", step_r5.prose, \u0275\u0275sanitizeHtml);
    \u0275\u0275advance();
    \u0275\u0275conditional(step_r5.code ? 4 : -1);
  }
}
function TutorialDetailComponent_Conditional_8_Conditional_8_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "a", 13);
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext(2);
    \u0275\u0275property("routerLink", \u0275\u0275pureFunction1(2, _c0, ctx_r2.prevTutorial().id));
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1("\u2190 ", ctx_r2.prevTutorial().title);
  }
}
function TutorialDetailComponent_Conditional_8_Conditional_10_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "a", 15);
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext(2);
    \u0275\u0275property("routerLink", \u0275\u0275pureFunction1(2, _c0, ctx_r2.nextTutorial().id));
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1("", ctx_r2.nextTutorial().title, " \u2192");
  }
}
function TutorialDetailComponent_Conditional_8_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "section", 6)(1, "h2");
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "p", 10);
    \u0275\u0275text(4);
    \u0275\u0275elementEnd();
    \u0275\u0275repeaterCreate(5, TutorialDetailComponent_Conditional_8_For_6_Template, 5, 4, "div", 11, _forTrack0);
    \u0275\u0275elementStart(7, "div", 12);
    \u0275\u0275conditionalCreate(8, TutorialDetailComponent_Conditional_8_Conditional_8_Template, 2, 4, "a", 13);
    \u0275\u0275element(9, "span", 14);
    \u0275\u0275conditionalCreate(10, TutorialDetailComponent_Conditional_8_Conditional_10_Template, 2, 4, "a", 15);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate2("Tutorial ", ctx_r2.tutorial().number, ": ", ctx_r2.tutorial().title);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(ctx_r2.tutorial().subtitle);
    \u0275\u0275advance();
    \u0275\u0275repeater(ctx_r2.tutorial().steps);
    \u0275\u0275advance(3);
    \u0275\u0275conditional(ctx_r2.prevTutorial() ? 8 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275conditional(ctx_r2.nextTutorial() ? 10 : -1);
  }
}
function TutorialDetailComponent_Conditional_9_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 7)(1, "p");
    \u0275\u0275text(2, "Tutorial not found.");
    \u0275\u0275elementEnd()();
  }
}
var TutorialDetailComponent = class _TutorialDetailComponent {
  api = inject(ApiService);
  route = inject(ActivatedRoute);
  cdr = inject(ChangeDetectorRef);
  sub = null;
  tutorial = signal(null, ...ngDevMode ? [{ debugName: "tutorial" }] : []);
  allTutorials = signal([], ...ngDevMode ? [{ debugName: "allTutorials" }] : []);
  prevTutorial = signal(null, ...ngDevMode ? [{ debugName: "prevTutorial" }] : []);
  nextTutorial = signal(null, ...ngDevMode ? [{ debugName: "nextTutorial" }] : []);
  activeStepIndex = signal(-1, ...ngDevMode ? [{ debugName: "activeStepIndex" }] : []);
  loading = signal(false, ...ngDevMode ? [{ debugName: "loading" }] : []);
  ngOnInit() {
    this.sub = this.route.paramMap.subscribe((params) => {
      const id = params.get("id");
      if (id) {
        this.loadTutorial(id);
      }
    });
  }
  ngOnDestroy() {
    this.sub?.unsubscribe();
  }
  loadTutorial(id) {
    this.loading.set(true);
    this.tutorial.set(null);
    this.activeStepIndex.set(-1);
    forkJoin({
      tutorial: this.api.getTutorial(id),
      list: this.api.getTutorials()
    }).subscribe({
      next: ({ tutorial, list }) => {
        this.tutorial.set(tutorial);
        this.allTutorials.set(list);
        this.computePrevNext(tutorial, list);
        this.loading.set(false);
        this.cdr.markForCheck();
        window.scrollTo({ top: 0, behavior: "smooth" });
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      }
    });
  }
  computePrevNext(tutorial, list) {
    const idx = list.findIndex((t) => t.id === tutorial.id);
    this.prevTutorial.set(idx > 0 ? list[idx - 1] : null);
    this.nextTutorial.set(idx < list.length - 1 ? list[idx + 1] : null);
  }
  scrollToStep(index) {
    this.activeStepIndex.set(index);
    const el = document.getElementById("step-" + index);
    el?.scrollIntoView({ behavior: "smooth" });
  }
  static \u0275fac = function TutorialDetailComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _TutorialDetailComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _TutorialDetailComponent, selectors: [["app-tutorial-detail"]], decls: 10, vars: 2, consts: [[1, "tut-layout"], [1, "tut-sidebar"], [1, "sidebar-nav"], ["routerLink", "/tutorials", 1, "back-link"], [1, "tut-content"], [1, "loading"], [1, "tutorial-section"], [1, "empty-state"], [3, "active"], [3, "click"], [1, "subtitle"], [1, "tutorial-step", 3, "id"], [1, "tutorial-nav"], [1, "nav-prev", 3, "routerLink"], [1, "nav-spacer"], [1, "nav-next", 3, "routerLink"], [3, "innerHTML"], [3, "code", "label"]], template: function TutorialDetailComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "div", 0)(1, "aside", 1)(2, "nav", 2)(3, "a", 3);
      \u0275\u0275text(4, "\u2190 All Tutorials");
      \u0275\u0275elementEnd();
      \u0275\u0275conditionalCreate(5, TutorialDetailComponent_Conditional_5_Template, 5, 0);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(6, "div", 4);
      \u0275\u0275conditionalCreate(7, TutorialDetailComponent_Conditional_7_Template, 2, 0, "div", 5)(8, TutorialDetailComponent_Conditional_8_Template, 11, 5, "section", 6)(9, TutorialDetailComponent_Conditional_9_Template, 3, 0, "div", 7);
      \u0275\u0275elementEnd()();
    }
    if (rf & 2) {
      \u0275\u0275advance(5);
      \u0275\u0275conditional(ctx.tutorial() ? 5 : -1);
      \u0275\u0275advance(2);
      \u0275\u0275conditional(ctx.loading() ? 7 : ctx.tutorial() ? 8 : 9);
    }
  }, dependencies: [CodeBlockComponent, RouterLink], styles: ["\n\n.tut-layout[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 32px;\n  align-items: flex-start;\n  padding-top: 16px;\n}\n.tut-sidebar[_ngcontent-%COMP%] {\n  position: sticky;\n  top: 80px;\n  width: 260px;\n  flex-shrink: 0;\n  max-height: calc(100vh - 100px);\n  overflow-y: auto;\n}\n.back-link[_ngcontent-%COMP%] {\n  display: block;\n  padding: 8px 12px;\n  font-size: 14px;\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  margin-bottom: 16px;\n}\n.back-link[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n.sidebar-nav[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 13px;\n  font-weight: 600;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: rgba(0, 0, 0, 0.5);\n  margin: 0 0 12px;\n  padding: 0 12px;\n}\n.sidebar-nav[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%] {\n  list-style: none;\n  margin: 0;\n  padding: 0;\n}\n.sidebar-nav[_ngcontent-%COMP%]   li[_ngcontent-%COMP%] {\n  margin: 0;\n}\n.sidebar-nav[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  display: block;\n  padding: 6px 12px;\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.7);\n  text-decoration: none;\n  border-left: 3px solid transparent;\n  cursor: pointer;\n  transition: all 0.15s;\n}\n.sidebar-nav[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  color: var(--brand-primary, #4338ca);\n  background: rgba(67, 56, 202, 0.04);\n}\n.sidebar-nav[_ngcontent-%COMP%]   li.active[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  border-left-color: var(--brand-primary, #4338ca);\n  font-weight: 500;\n  background: rgba(67, 56, 202, 0.06);\n}\n.tut-content[_ngcontent-%COMP%] {\n  flex: 1;\n  min-width: 0;\n}\n@media (max-width: 900px) {\n  .tut-layout[_ngcontent-%COMP%] {\n    flex-direction: column;\n  }\n  .tut-sidebar[_ngcontent-%COMP%] {\n    position: static;\n    width: 100%;\n    max-height: none;\n    border: 1px solid rgba(0, 0, 0, 0.08);\n    border-radius: 8px;\n    padding: 12px 0;\n    background: rgba(0, 0, 0, 0.01);\n  }\n}\n.tutorial-section[_ngcontent-%COMP%] {\n  margin: 0 0 40px;\n}\n.tutorial-section[_ngcontent-%COMP%]   h2[_ngcontent-%COMP%] {\n  font-size: 22px;\n  font-weight: 500;\n  margin-bottom: 8px;\n}\n.subtitle[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  line-height: 1.7;\n  margin-bottom: 24px;\n}\n.tutorial-step[_ngcontent-%COMP%] {\n  margin-bottom: 24px;\n}\n.tutorial-step[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 16px;\n  font-weight: 500;\n  margin: 0 0 12px;\n}\n.tutorial-step[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.7;\n  margin: 8px 0;\n}\n.tutorial-step[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.tutorial-step[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n.tutorial-nav[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  padding: 24px 0;\n  border-top: 1px solid rgba(0, 0, 0, 0.08);\n  margin-top: 32px;\n  gap: 16px;\n}\n.tutorial-nav[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  font-size: 14px;\n}\n.tutorial-nav[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n.nav-prev[_ngcontent-%COMP%] {\n  max-width: 45%;\n}\n.nav-next[_ngcontent-%COMP%] {\n  max-width: 45%;\n  text-align: right;\n}\n.nav-spacer[_ngcontent-%COMP%] {\n  flex: 1;\n}\n.loading[_ngcontent-%COMP%] {\n  padding: 48px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n  font-size: 16px;\n}\n.empty-state[_ngcontent-%COMP%] {\n  padding: 48px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n}\n.empty-state[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  font-size: 16px;\n  margin: 0;\n}\n/*# sourceMappingURL=tutorial-detail.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TutorialDetailComponent, [{
    type: Component,
    args: [{ selector: "app-tutorial-detail", standalone: true, imports: [CodeBlockComponent, RouterLink], template: `
    <div class="tut-layout">
      <!-- Sticky sidebar -->
      <aside class="tut-sidebar">
        <nav class="sidebar-nav">
          <a class="back-link" routerLink="/tutorials">&larr; All Tutorials</a>

          @if (tutorial()) {
            <h3>Steps</h3>
            <ul>
              @for (step of tutorial()!.steps; track step.title; let i = $index) {
                <li [class.active]="activeStepIndex() === i">
                  <a (click)="scrollToStep(i)">{{ step.title }}</a>
                </li>
              }
            </ul>
          }
        </nav>
      </aside>

      <!-- Main content -->
      <div class="tut-content">
        @if (loading()) {
          <div class="loading">Loading...</div>
        } @else if (tutorial()) {
          <section class="tutorial-section">
            <h2>Tutorial {{ tutorial()!.number }}: {{ tutorial()!.title }}</h2>
            <p class="subtitle">{{ tutorial()!.subtitle }}</p>

            @for (step of tutorial()!.steps; track step.title; let i = $index) {
              <div class="tutorial-step" [id]="'step-' + i">
                <h3>{{ step.title }}</h3>
                <p [innerHTML]="step.prose"></p>
                @if (step.code) {
                  <app-code-block [code]="step.code" [label]="step.codeLabel || ''"></app-code-block>
                }
              </div>
            }

            <div class="tutorial-nav">
              @if (prevTutorial()) {
                <a class="nav-prev" [routerLink]="['/tutorials', prevTutorial()!.id]">&larr; {{ prevTutorial()!.title }}</a>
              }
              <span class="nav-spacer"></span>
              @if (nextTutorial()) {
                <a class="nav-next" [routerLink]="['/tutorials', nextTutorial()!.id]">{{ nextTutorial()!.title }} &rarr;</a>
              }
            </div>
          </section>
        } @else {
          <div class="empty-state">
            <p>Tutorial not found.</p>
          </div>
        }
      </div>
    </div>
  `, styles: ["/* angular:styles/component:scss;e502d438d1aa2a5ec9f4983de1e593c941018d97e30c1f98df067f46c14368b8;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/tutorials/tutorial-detail.component.ts */\n.tut-layout {\n  display: flex;\n  gap: 32px;\n  align-items: flex-start;\n  padding-top: 16px;\n}\n.tut-sidebar {\n  position: sticky;\n  top: 80px;\n  width: 260px;\n  flex-shrink: 0;\n  max-height: calc(100vh - 100px);\n  overflow-y: auto;\n}\n.back-link {\n  display: block;\n  padding: 8px 12px;\n  font-size: 14px;\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  margin-bottom: 16px;\n}\n.back-link:hover {\n  text-decoration: underline;\n}\n.sidebar-nav h3 {\n  font-size: 13px;\n  font-weight: 600;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: rgba(0, 0, 0, 0.5);\n  margin: 0 0 12px;\n  padding: 0 12px;\n}\n.sidebar-nav ul {\n  list-style: none;\n  margin: 0;\n  padding: 0;\n}\n.sidebar-nav li {\n  margin: 0;\n}\n.sidebar-nav li a {\n  display: block;\n  padding: 6px 12px;\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.7);\n  text-decoration: none;\n  border-left: 3px solid transparent;\n  cursor: pointer;\n  transition: all 0.15s;\n}\n.sidebar-nav li a:hover {\n  color: var(--brand-primary, #4338ca);\n  background: rgba(67, 56, 202, 0.04);\n}\n.sidebar-nav li.active a {\n  color: var(--brand-primary, #4338ca);\n  border-left-color: var(--brand-primary, #4338ca);\n  font-weight: 500;\n  background: rgba(67, 56, 202, 0.06);\n}\n.tut-content {\n  flex: 1;\n  min-width: 0;\n}\n@media (max-width: 900px) {\n  .tut-layout {\n    flex-direction: column;\n  }\n  .tut-sidebar {\n    position: static;\n    width: 100%;\n    max-height: none;\n    border: 1px solid rgba(0, 0, 0, 0.08);\n    border-radius: 8px;\n    padding: 12px 0;\n    background: rgba(0, 0, 0, 0.01);\n  }\n}\n.tutorial-section {\n  margin: 0 0 40px;\n}\n.tutorial-section h2 {\n  font-size: 22px;\n  font-weight: 500;\n  margin-bottom: 8px;\n}\n.subtitle {\n  color: rgba(0, 0, 0, 0.6);\n  line-height: 1.7;\n  margin-bottom: 24px;\n}\n.tutorial-step {\n  margin-bottom: 24px;\n}\n.tutorial-step h3 {\n  font-size: 16px;\n  font-weight: 500;\n  margin: 0 0 12px;\n}\n.tutorial-step p {\n  line-height: 1.7;\n  margin: 8px 0;\n}\n.tutorial-step a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.tutorial-step a:hover {\n  text-decoration: underline;\n}\n.tutorial-nav {\n  display: flex;\n  align-items: center;\n  padding: 24px 0;\n  border-top: 1px solid rgba(0, 0, 0, 0.08);\n  margin-top: 32px;\n  gap: 16px;\n}\n.tutorial-nav a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  font-size: 14px;\n}\n.tutorial-nav a:hover {\n  text-decoration: underline;\n}\n.nav-prev {\n  max-width: 45%;\n}\n.nav-next {\n  max-width: 45%;\n  text-align: right;\n}\n.nav-spacer {\n  flex: 1;\n}\n.loading {\n  padding: 48px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n  font-size: 16px;\n}\n.empty-state {\n  padding: 48px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n}\n.empty-state p {\n  font-size: 16px;\n  margin: 0;\n}\n/*# sourceMappingURL=tutorial-detail.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(TutorialDetailComponent, { className: "TutorialDetailComponent", filePath: "src/app/pages/tutorials/tutorial-detail.component.ts", lineNumber: 237 });
})();
export {
  TutorialDetailComponent
};
//# sourceMappingURL=chunk-TL4XWDTA.js.map
