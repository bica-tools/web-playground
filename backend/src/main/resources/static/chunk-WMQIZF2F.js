import {
  ApiService
} from "./chunk-EOCOQ6DB.js";
import {
  Router
} from "./chunk-QTYX35EO.js";
import "./chunk-ZG4TCI7P.js";
import {
  Component,
  inject,
  setClassMetadata,
  signal,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵdefineComponent,
  ɵɵdomElementEnd,
  ɵɵdomElementStart,
  ɵɵdomListener,
  ɵɵgetCurrentView,
  ɵɵnextContext,
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtext,
  ɵɵtextInterpolate
} from "./chunk-OWEA7TR3.js";

// src/app/pages/tutorials/tutorials-list.component.ts
var _forTrack0 = ($index, $item) => $item.id;
function TutorialsListComponent_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275domElementStart(0, "div", 1);
    \u0275\u0275text(1, "Loading tutorials...");
    \u0275\u0275domElementEnd();
  }
}
function TutorialsListComponent_Conditional_6_For_2_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275domElementStart(0, "div", 4);
    \u0275\u0275domListener("click", function TutorialsListComponent_Conditional_6_For_2_Template_div_click_0_listener() {
      const tut_r2 = \u0275\u0275restoreView(_r1).$implicit;
      const ctx_r2 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r2.openTutorial(tut_r2.id));
    })("keydown.enter", function TutorialsListComponent_Conditional_6_For_2_Template_div_keydown_enter_0_listener() {
      const tut_r2 = \u0275\u0275restoreView(_r1).$implicit;
      const ctx_r2 = \u0275\u0275nextContext(2);
      return \u0275\u0275resetView(ctx_r2.openTutorial(tut_r2.id));
    });
    \u0275\u0275domElementStart(1, "span", 5);
    \u0275\u0275text(2);
    \u0275\u0275domElementEnd();
    \u0275\u0275domElementStart(3, "h3", 6);
    \u0275\u0275text(4);
    \u0275\u0275domElementEnd();
    \u0275\u0275domElementStart(5, "p", 7);
    \u0275\u0275text(6);
    \u0275\u0275domElementEnd()();
  }
  if (rf & 2) {
    const tut_r2 = ctx.$implicit;
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(tut_r2.number);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(tut_r2.title);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(tut_r2.subtitle);
  }
}
function TutorialsListComponent_Conditional_6_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275domElementStart(0, "div", 2);
    \u0275\u0275repeaterCreate(1, TutorialsListComponent_Conditional_6_For_2_Template, 7, 3, "div", 3, _forTrack0);
    \u0275\u0275domElementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275advance();
    \u0275\u0275repeater(ctx_r2.tutorials());
  }
}
var TutorialsListComponent = class _TutorialsListComponent {
  api = inject(ApiService);
  router = inject(Router);
  tutorials = signal([], ...ngDevMode ? [{ debugName: "tutorials" }] : []);
  ngOnInit() {
    this.api.getTutorials().subscribe((list) => {
      this.tutorials.set(list);
    });
  }
  openTutorial(id) {
    this.router.navigate(["/tutorials", id]);
  }
  static \u0275fac = function TutorialsListComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _TutorialsListComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _TutorialsListComponent, selectors: [["app-tutorials-list"]], decls: 7, vars: 1, consts: [[1, "page-header"], [1, "loading"], [1, "card-grid"], ["tabindex", "0", 1, "tutorial-card"], ["tabindex", "0", 1, "tutorial-card", 3, "click", "keydown.enter"], [1, "card-number"], [1, "card-title"], [1, "card-subtitle"]], template: function TutorialsListComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275domElementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "Tutorials");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(3, "p");
      \u0275\u0275text(4, "Step-by-step guides to session types, reticulates, and tooling.");
      \u0275\u0275domElementEnd()();
      \u0275\u0275conditionalCreate(5, TutorialsListComponent_Conditional_5_Template, 2, 0, "div", 1)(6, TutorialsListComponent_Conditional_6_Template, 3, 0, "div", 2);
    }
    if (rf & 2) {
      \u0275\u0275advance(5);
      \u0275\u0275conditional(ctx.tutorials().length === 0 ? 5 : 6);
    }
  }, styles: ["\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.card-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));\n  gap: 20px;\n  padding: 16px 0 40px;\n}\n.tutorial-card[_ngcontent-%COMP%] {\n  position: relative;\n  padding: 24px;\n  border: 1px solid rgba(0, 0, 0, 0.1);\n  border-radius: 12px;\n  cursor: pointer;\n  transition: all 0.2s ease;\n  background: white;\n  overflow: hidden;\n}\n.tutorial-card[_ngcontent-%COMP%]:hover {\n  border-color: var(--brand-primary, #4338ca);\n  box-shadow: 0 4px 12px rgba(67, 56, 202, 0.12);\n  transform: translateY(-2px);\n}\n.tutorial-card[_ngcontent-%COMP%]:focus-visible {\n  outline: 2px solid var(--brand-primary, #4338ca);\n  outline-offset: 2px;\n}\n.card-number[_ngcontent-%COMP%] {\n  position: absolute;\n  top: -8px;\n  right: 12px;\n  font-size: 72px;\n  font-weight: 700;\n  color: rgba(67, 56, 202, 0.07);\n  line-height: 1;\n  pointer-events: none;\n  -webkit-user-select: none;\n  user-select: none;\n}\n.card-title[_ngcontent-%COMP%] {\n  font-size: 17px;\n  font-weight: 500;\n  margin: 0 0 8px;\n  color: rgba(0, 0, 0, 0.87);\n}\n.card-subtitle[_ngcontent-%COMP%] {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.55);\n  margin: 0;\n  line-height: 1.5;\n}\n.loading[_ngcontent-%COMP%] {\n  padding: 48px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n  font-size: 16px;\n}\n@media (max-width: 640px) {\n  .card-grid[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n}\n/*# sourceMappingURL=tutorials-list.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TutorialsListComponent, [{
    type: Component,
    args: [{ selector: "app-tutorials-list", standalone: true, imports: [], template: `
    <header class="page-header">
      <h1>Tutorials</h1>
      <p>Step-by-step guides to session types, reticulates, and tooling.</p>
    </header>

    @if (tutorials().length === 0) {
      <div class="loading">Loading tutorials...</div>
    } @else {
      <div class="card-grid">
        @for (tut of tutorials(); track tut.id) {
          <div class="tutorial-card" (click)="openTutorial(tut.id)" tabindex="0" (keydown.enter)="openTutorial(tut.id)">
            <span class="card-number">{{ tut.number }}</span>
            <h3 class="card-title">{{ tut.title }}</h3>
            <p class="card-subtitle">{{ tut.subtitle }}</p>
          </div>
        }
      </div>
    }
  `, styles: ["/* angular:styles/component:scss;c6d35093ae76ca990e0041e04360bdb663403a588dcc7e3a02047747eea38d40;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/tutorials/tutorials-list.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.card-grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));\n  gap: 20px;\n  padding: 16px 0 40px;\n}\n.tutorial-card {\n  position: relative;\n  padding: 24px;\n  border: 1px solid rgba(0, 0, 0, 0.1);\n  border-radius: 12px;\n  cursor: pointer;\n  transition: all 0.2s ease;\n  background: white;\n  overflow: hidden;\n}\n.tutorial-card:hover {\n  border-color: var(--brand-primary, #4338ca);\n  box-shadow: 0 4px 12px rgba(67, 56, 202, 0.12);\n  transform: translateY(-2px);\n}\n.tutorial-card:focus-visible {\n  outline: 2px solid var(--brand-primary, #4338ca);\n  outline-offset: 2px;\n}\n.card-number {\n  position: absolute;\n  top: -8px;\n  right: 12px;\n  font-size: 72px;\n  font-weight: 700;\n  color: rgba(67, 56, 202, 0.07);\n  line-height: 1;\n  pointer-events: none;\n  -webkit-user-select: none;\n  user-select: none;\n}\n.card-title {\n  font-size: 17px;\n  font-weight: 500;\n  margin: 0 0 8px;\n  color: rgba(0, 0, 0, 0.87);\n}\n.card-subtitle {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.55);\n  margin: 0;\n  line-height: 1.5;\n}\n.loading {\n  padding: 48px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n  font-size: 16px;\n}\n@media (max-width: 640px) {\n  .card-grid {\n    grid-template-columns: 1fr;\n  }\n}\n/*# sourceMappingURL=tutorials-list.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(TutorialsListComponent, { className: "TutorialsListComponent", filePath: "src/app/pages/tutorials/tutorials-list.component.ts", lineNumber: 111 });
})();
export {
  TutorialsListComponent
};
//# sourceMappingURL=chunk-WMQIZF2F.js.map
