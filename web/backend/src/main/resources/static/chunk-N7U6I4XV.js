import {
  MatAccordion,
  MatExpansionModule,
  MatExpansionPanel,
  MatExpansionPanelDescription,
  MatExpansionPanelHeader,
  MatExpansionPanelTitle
} from "./chunk-OPVQJPP7.js";
import {
  MatChip,
  MatChipSet,
  MatChipsModule
} from "./chunk-F3DYJDGJ.js";
import {
  MatFormFieldModule,
  MatInput,
  MatInputModule
} from "./chunk-43RQTZW4.js";
import {
  MatFormField,
  MatLabel,
  MatSuffix
} from "./chunk-RSSZT2MJ.js";
import "./chunk-2AQDFUQH.js";
import "./chunk-CVQJCEWM.js";
import "./chunk-R2VWAHTD.js";
import {
  CdkFixedSizeVirtualScroll,
  CdkVirtualScrollViewport,
  ScrollingModule
} from "./chunk-SUS3PTUT.js";
import {
  MatIcon,
  MatIconModule
} from "./chunk-BFW3NWZD.js";
import "./chunk-ZG4TCI7P.js";
import "./chunk-NL2TMNRB.js";
import {
  Component,
  computed,
  setClassMetadata,
  signal,
  ɵsetClassDebugInfo,
  ɵɵadvance,
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
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵrepeaterTrackByIdentity,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵsanitizeHtml,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1
} from "./chunk-OWEA7TR3.js";

// src/app/pages/faq/faq-data.ts
var FAQ_DATA = [
  {
    question: "What is a session type?",
    answer: "A <strong>session type</strong> describes the communication protocol governing interaction with an object \u2014 the legal sequences of method calls, branches, and selections a client may perform. Unlike a flat interface, a session type makes the object's type <em>evolve</em> as methods are called, enforcing protocol compliance <strong>statically</strong>.",
    category: "Fundamentals"
  },
  {
    question: "What is a state space?",
    answer: 'A <strong>state space</strong> <code>L(S)</code> is the labeled transition system obtained by "executing" a session type. Each state represents a protocol stage, each transition a permitted action. It has a unique initial state (top) and terminal state (bottom). The reachability ordering gives it the structure of a bounded lattice (after SCC quotient).',
    category: "Fundamentals"
  },
  {
    question: "What is a bounded lattice?",
    answer: "A <strong>bounded lattice</strong> is a partially ordered set where every pair has a meet (greatest lower bound) and join (least upper bound), plus top and bottom elements. In session types: top = initial state, bottom = terminal state, meet = convergence point, join = divergence point.",
    category: "Theory"
  },
  {
    question: "What is the parallel constructor?",
    answer: "The <strong>parallel constructor</strong> <code>S\u2081 \u2225 S\u2082</code> models two sub-protocols executing concurrently on a shared object. Its state space is the product of the two components. This is the <strong>key novelty</strong> \u2014 the product of two lattices is always a lattice, making lattice structure <em>necessary</em>.",
    category: "Constructs"
  },
  {
    question: "What is a reticulate?",
    answer: "A <strong>reticulate</strong> is the bounded lattice formed by the state space of a well-formed session type, after quotienting by SCCs. The Reticulate Theorem proves every well-formed session type produces one \u2014 it is a guaranteed structural property.",
    category: "Theory"
  },
  {
    question: "What is an SCC quotient?",
    answer: "An <strong>SCC quotient</strong> collapses every strongly connected component into a single node, turning a cyclic graph into a DAG. In session types, cycles from recursion are collapsed, restoring antisymmetry for the partial order.",
    category: "Theory"
  },
  {
    question: "What is top absorption?",
    answer: "<strong>Top absorption</strong> is the key lemma for recursion. It states: if you collapse an upward-closed set containing top into a single element, the result is still a bounded lattice. This has been mechanically verified in Lean 4.",
    category: "Theory"
  },
  {
    question: "What is WF-Par?",
    answer: "<strong>WF-Par</strong> is the well-formedness condition for the parallel constructor. It requires both branches to be terminating, well-formed, variable-disjoint, and free of nested <code>\u2225</code>.",
    category: "Constructs"
  },
  {
    question: "What is a morphism between session types?",
    answer: "A <strong>morphism</strong> is a structure-preserving map between state spaces. The hierarchy: homomorphism (order-preserving), projection (surjective), embedding (injective + reflecting), isomorphism (bijective embedding). Additionally, Galois connections capture approximation relationships.",
    category: "Theory"
  },
  {
    question: "Why do session type state spaces form lattices?",
    answer: "Because every constructor preserves lattice structure, and the proof goes by structural induction: <code>end</code> is trivial; sequencing adds a maximum; branch/selection create joins and meets; recursion is absorbed by SCC quotient; parallel takes the product. Since every constructor preserves the property and the base case has it, every well-formed session type has it.",
    category: "Fundamentals"
  }
];

// src/app/pages/faq/faq.component.ts
var _forTrack0 = ($index, $item) => $item.question;
function FaqComponent_For_17_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "mat-chip", 6);
    \u0275\u0275listener("click", function FaqComponent_For_17_Template_mat_chip_click_0_listener() {
      const cat_r2 = \u0275\u0275restoreView(_r1).$implicit;
      const ctx_r2 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r2.selectCategory(cat_r2));
    });
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const cat_r2 = ctx.$implicit;
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275property("highlighted", ctx_r2.selectedCategory() === cat_r2);
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1(" ", cat_r2, " ");
  }
}
function FaqComponent_Conditional_18_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 8)(1, "mat-icon");
    \u0275\u0275text(2, "search_off");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "p");
    \u0275\u0275text(4, "No matching questions found.");
    \u0275\u0275elementEnd()();
  }
}
function FaqComponent_Conditional_19_For_3_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-expansion-panel")(1, "mat-expansion-panel-header")(2, "mat-panel-title");
    \u0275\u0275text(3);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "mat-panel-description");
    \u0275\u0275text(5);
    \u0275\u0275elementEnd()();
    \u0275\u0275element(6, "p", 11);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const item_r4 = ctx.$implicit;
    \u0275\u0275advance(3);
    \u0275\u0275textInterpolate(item_r4.question);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(item_r4.category);
    \u0275\u0275advance();
    \u0275\u0275property("innerHTML", item_r4.answer, \u0275\u0275sanitizeHtml);
  }
}
function FaqComponent_Conditional_19_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "cdk-virtual-scroll-viewport", 9)(1, "mat-accordion", 10);
    \u0275\u0275repeaterCreate(2, FaqComponent_Conditional_19_For_3_Template, 7, 3, "mat-expansion-panel", null, _forTrack0);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275advance(2);
    \u0275\u0275repeater(ctx_r2.filteredFaqs());
  }
}
var FaqComponent = class _FaqComponent {
  searchTerm = signal("", ...ngDevMode ? [{ debugName: "searchTerm" }] : []);
  selectedCategory = signal(null, ...ngDevMode ? [{ debugName: "selectedCategory" }] : []);
  categories = [...new Set(FAQ_DATA.map((f) => f.category))];
  filteredFaqs = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const cat = this.selectedCategory();
    return FAQ_DATA.filter((item) => {
      if (cat && item.category !== cat)
        return false;
      if (term) {
        const haystack = (item.question + " " + item.answer).toLowerCase();
        if (!haystack.includes(term))
          return false;
      }
      return true;
    });
  }, ...ngDevMode ? [{ debugName: "filteredFaqs" }] : []);
  onSearch(event) {
    const value = event.target.value;
    this.searchTerm.set(value);
  }
  selectCategory(cat) {
    this.selectedCategory.set(cat);
  }
  static \u0275fac = function FaqComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _FaqComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _FaqComponent, selectors: [["app-faq"]], decls: 20, vars: 4, consts: [[1, "page-header"], [1, "faq-controls"], ["appearance", "outline", 1, "search-field"], ["matInput", "", "placeholder", "Type to filter...", 3, "input", "value"], ["matSuffix", ""], [1, "category-chips"], [3, "click", "highlighted"], [3, "highlighted"], [1, "no-results"], ["itemSize", "64", 1, "faq-viewport"], ["multi", ""], [3, "innerHTML"]], template: function FaqComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "FAQ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(5, "div", 1)(6, "mat-form-field", 2)(7, "mat-label");
      \u0275\u0275text(8, "Search FAQ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(9, "input", 3);
      \u0275\u0275listener("input", function FaqComponent_Template_input_input_9_listener($event) {
        return ctx.onSearch($event);
      });
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(10, "mat-icon", 4);
      \u0275\u0275text(11, "search");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(12, "div", 5)(13, "mat-chip-set")(14, "mat-chip", 6);
      \u0275\u0275listener("click", function FaqComponent_Template_mat_chip_click_14_listener() {
        return ctx.selectCategory(null);
      });
      \u0275\u0275text(15, " All ");
      \u0275\u0275elementEnd();
      \u0275\u0275repeaterCreate(16, FaqComponent_For_17_Template, 2, 2, "mat-chip", 7, \u0275\u0275repeaterTrackByIdentity);
      \u0275\u0275elementEnd()()();
      \u0275\u0275conditionalCreate(18, FaqComponent_Conditional_18_Template, 5, 0, "div", 8)(19, FaqComponent_Conditional_19_Template, 4, 0, "cdk-virtual-scroll-viewport", 9);
    }
    if (rf & 2) {
      \u0275\u0275advance(4);
      \u0275\u0275textInterpolate1("Frequently asked questions about session types, reticulates, and tooling (", ctx.filteredFaqs().length, " items)");
      \u0275\u0275advance(5);
      \u0275\u0275property("value", ctx.searchTerm());
      \u0275\u0275advance(5);
      \u0275\u0275property("highlighted", ctx.selectedCategory() === null);
      \u0275\u0275advance(2);
      \u0275\u0275repeater(ctx.categories);
      \u0275\u0275advance(2);
      \u0275\u0275conditional(ctx.filteredFaqs().length === 0 ? 18 : 19);
    }
  }, dependencies: [ScrollingModule, CdkFixedSizeVirtualScroll, CdkVirtualScrollViewport, MatExpansionModule, MatAccordion, MatExpansionPanel, MatExpansionPanelHeader, MatExpansionPanelTitle, MatExpansionPanelDescription, MatFormFieldModule, MatFormField, MatLabel, MatSuffix, MatInputModule, MatInput, MatChipsModule, MatChip, MatChipSet, MatIconModule, MatIcon], styles: ["\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.faq-controls[_ngcontent-%COMP%] {\n  margin: 16px 0;\n}\n.search-field[_ngcontent-%COMP%] {\n  width: 100%;\n}\n.category-chips[_ngcontent-%COMP%] {\n  margin: 8px 0 16px;\n}\nmat-chip[_ngcontent-%COMP%] {\n  cursor: pointer;\n}\n.faq-viewport[_ngcontent-%COMP%] {\n  height: calc(100vh - 320px);\n  min-height: 400px;\n}\n.no-results[_ngcontent-%COMP%] {\n  text-align: center;\n  padding: 48px 16px;\n  color: rgba(0, 0, 0, 0.5);\n}\n.no-results[_ngcontent-%COMP%]   mat-icon[_ngcontent-%COMP%] {\n  font-size: 48px;\n  width: 48px;\n  height: 48px;\n  margin-bottom: 12px;\n}\n.no-results[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  font-size: 16px;\n  margin: 0;\n}\n/*# sourceMappingURL=faq.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(FaqComponent, [{
    type: Component,
    args: [{ selector: "app-faq", standalone: true, imports: [
      ScrollingModule,
      MatExpansionModule,
      MatFormFieldModule,
      MatInputModule,
      MatChipsModule,
      MatIconModule
    ], template: `
    <header class="page-header">
      <h1>FAQ</h1>
      <p>Frequently asked questions about session types, reticulates, and tooling ({{ filteredFaqs().length }} items)</p>
    </header>

    <div class="faq-controls">
      <mat-form-field appearance="outline" class="search-field">
        <mat-label>Search FAQ</mat-label>
        <input matInput
               [value]="searchTerm()"
               (input)="onSearch($event)"
               placeholder="Type to filter..." />
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      <div class="category-chips">
        <mat-chip-set>
          <mat-chip [highlighted]="selectedCategory() === null"
                    (click)="selectCategory(null)">
            All
          </mat-chip>
          @for (cat of categories; track cat) {
            <mat-chip [highlighted]="selectedCategory() === cat"
                      (click)="selectCategory(cat)">
              {{ cat }}
            </mat-chip>
          }
        </mat-chip-set>
      </div>
    </div>

    @if (filteredFaqs().length === 0) {
      <div class="no-results">
        <mat-icon>search_off</mat-icon>
        <p>No matching questions found.</p>
      </div>
    } @else {
      <cdk-virtual-scroll-viewport itemSize="64" class="faq-viewport">
        <mat-accordion multi>
          @for (item of filteredFaqs(); track item.question) {
            <mat-expansion-panel>
              <mat-expansion-panel-header>
                <mat-panel-title>{{ item.question }}</mat-panel-title>
                <mat-panel-description>{{ item.category }}</mat-panel-description>
              </mat-expansion-panel-header>
              <p [innerHTML]="item.answer"></p>
            </mat-expansion-panel>
          }
        </mat-accordion>
      </cdk-virtual-scroll-viewport>
    }
  `, styles: ["/* angular:styles/component:scss;4c236c2d7f21d15905a73094d014f61aa212ec8c4c3d40f920b43cd00c98f7f4;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/faq/faq.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.faq-controls {\n  margin: 16px 0;\n}\n.search-field {\n  width: 100%;\n}\n.category-chips {\n  margin: 8px 0 16px;\n}\nmat-chip {\n  cursor: pointer;\n}\n.faq-viewport {\n  height: calc(100vh - 320px);\n  min-height: 400px;\n}\n.no-results {\n  text-align: center;\n  padding: 48px 16px;\n  color: rgba(0, 0, 0, 0.5);\n}\n.no-results mat-icon {\n  font-size: 48px;\n  width: 48px;\n  height: 48px;\n  margin-bottom: 12px;\n}\n.no-results p {\n  font-size: 16px;\n  margin: 0;\n}\n/*# sourceMappingURL=faq.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(FaqComponent, { className: "FaqComponent", filePath: "src/app/pages/faq/faq.component.ts", lineNumber: 123 });
})();
export {
  FaqComponent
};
//# sourceMappingURL=chunk-N7U6I4XV.js.map
