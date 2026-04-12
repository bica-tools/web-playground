import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardModule,
  MatCardSubtitle,
  MatCardTitle
} from "./chunk-SHRTRSL7.js";
import "./chunk-NL2TMNRB.js";
import {
  Component,
  setClassMetadata,
  ɵsetClassDebugInfo,
  ɵɵdefineComponent,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵtext
} from "./chunk-OWEA7TR3.js";

// src/app/pages/about/about.component.ts
var AboutComponent = class _AboutComponent {
  static \u0275fac = function AboutComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _AboutComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _AboutComponent, selectors: [["app-about"]], decls: 89, vars: 0, consts: [[1, "page-header"], [1, "about-section"], [1, "person-links"], ["href", "https://www.zuacaldeira.com", "target", "_blank", "rel", "noopener"], ["href", "https://github.com/zuacaldeira", "target", "_blank", "rel", "noopener"], [1, "pillars-list"], [1, "numbers-grid"], [1, "number-item"], [1, "number-value"], [1, "number-label"], ["href", "https://github.com/zuacaldeira/SessionTypesResearch", "target", "_blank", "rel", "noopener"]], template: function AboutComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "About");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4, "The people and institutions behind the Reticulate project.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(5, "section", 1)(6, "h2");
      \u0275\u0275text(7, "Author");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(8, "mat-card")(9, "mat-card-header")(10, "mat-card-title");
      \u0275\u0275text(11, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(12, "mat-card-subtitle");
      \u0275\u0275text(13, "Independent Researcher");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(14, "mat-card-content")(15, "p");
      \u0275\u0275text(16, " Independent researcher based in Berlin, Germany. Research focus: session types, type theory, and programming language design. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(17, "p");
      \u0275\u0275text(18, " The Reticulate project develops the theory and toolchain for session types as algebraic reticulates. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(19, "div", 2)(20, "a", 3);
      \u0275\u0275text(21, "Website");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(22, "a", 4);
      \u0275\u0275text(23, "GitHub");
      \u0275\u0275elementEnd()()()()();
      \u0275\u0275elementStart(24, "section", 1)(25, "h2");
      \u0275\u0275text(26, "The Project");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(27, "p")(28, "strong");
      \u0275\u0275text(29, "Session Types as Algebraic Reticulates");
      \u0275\u0275elementEnd();
      \u0275\u0275text(30, " is a research project with three pillars: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(31, "ol", 5)(32, "li")(33, "strong");
      \u0275\u0275text(34, "Theory");
      \u0275\u0275elementEnd();
      \u0275\u0275text(35, " \u2014 Proving that session-type state spaces are lattices; developing the morphism hierarchy (isomorphism, embedding, projection, Galois connection); connecting to bisimulation and abstract interpretation. Two key lemmas formally verified in Lean 4 with zero sorry. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(36, "li")(37, "strong");
      \u0275\u0275text(38, "Reticulate");
      \u0275\u0275elementEnd();
      \u0275\u0275text(39, " \u2014 A Python library (9 modules, 789 tests) that constructs state spaces from session type definitions, checks lattice properties, computes morphisms, generates tests, and visualizes Hasse diagrams. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(40, "li")(41, "strong");
      \u0275\u0275text(42, "BICA Reborn");
      \u0275\u0275elementEnd();
      \u0275\u0275text(43, " \u2014 A Java 21 annotation-based session type checker (13 packages, 1,052 tests), successor to the original BICA (2009). Key novelty: the ");
      \u0275\u0275elementStart(44, "code");
      \u0275\u0275text(45, "\u2225");
      \u0275\u0275elementEnd();
      \u0275\u0275text(46, " (parallel) constructor for concurrent access, which forces lattice structure. ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(47, "section", 1)(48, "h2");
      \u0275\u0275text(49, "By the Numbers");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(50, "div", 6)(51, "div", 7)(52, "span", 8);
      \u0275\u0275text(53, "1,841");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(54, "span", 9);
      \u0275\u0275text(55, "Total tests");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(56, "div", 7)(57, "span", 8);
      \u0275\u0275text(58, "34");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(59, "span", 9);
      \u0275\u0275text(60, "Benchmark protocols");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(61, "div", 7)(62, "span", 8);
      \u0275\u0275text(63, "5,183");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(64, "span", 9);
      \u0275\u0275text(65, "Generated JUnit tests");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(66, "div", 7)(67, "span", 8);
      \u0275\u0275text(68, "2");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(69, "span", 9);
      \u0275\u0275text(70, "Lean 4 proofs (0 sorry)");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(71, "div", 7)(72, "span", 8);
      \u0275\u0275text(73, "7");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(74, "span", 9);
      \u0275\u0275text(75, "Pipeline stages");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(76, "div", 7)(77, "span", 8);
      \u0275\u0275text(78, "6");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(79, "span", 9);
      \u0275\u0275text(80, "Papers in progress");
      \u0275\u0275elementEnd()()()();
      \u0275\u0275elementStart(81, "section", 1)(82, "h2");
      \u0275\u0275text(83, "Contact");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(84, "p");
      \u0275\u0275text(85, " For questions about the research or collaboration inquiries, please reach out via the institutional channels listed above or through the ");
      \u0275\u0275elementStart(86, "a", 10);
      \u0275\u0275text(87, "GitHub repository");
      \u0275\u0275elementEnd();
      \u0275\u0275text(88, ". ");
      \u0275\u0275elementEnd()();
    }
  }, dependencies: [MatCardModule, MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle], styles: ["\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.about-section[_ngcontent-%COMP%] {\n  margin: 32px 0;\n}\n.about-section[_ngcontent-%COMP%]   h2[_ngcontent-%COMP%] {\n  font-size: 20px;\n  font-weight: 600;\n  margin-bottom: 16px;\n}\n.about-section[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.7;\n  margin-bottom: 12px;\n}\nmat-card[_ngcontent-%COMP%] {\n  margin-bottom: 16px;\n}\nmat-card-content[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.7;\n}\n.person-links[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 16px;\n  margin-top: 12px;\n}\n.person-links[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  font-size: 14px;\n}\n.person-links[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n.pillars-list[_ngcontent-%COMP%] {\n  padding-left: 24px;\n}\n.pillars-list[_ngcontent-%COMP%]   li[_ngcontent-%COMP%] {\n  margin-bottom: 12px;\n  line-height: 1.7;\n}\n.numbers-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));\n  gap: 16px;\n}\n.number-item[_ngcontent-%COMP%] {\n  text-align: center;\n  padding: 20px 12px;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  background: rgba(0, 0, 0, 0.01);\n}\n.number-value[_ngcontent-%COMP%] {\n  display: block;\n  font-size: 28px;\n  font-weight: 700;\n  color: var(--brand-primary, #4338ca);\n}\n.number-label[_ngcontent-%COMP%] {\n  display: block;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.55);\n  margin-top: 4px;\n}\n/*# sourceMappingURL=about.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(AboutComponent, [{
    type: Component,
    args: [{ selector: "app-about", standalone: true, imports: [MatCardModule], template: `
    <header class="page-header">
      <h1>About</h1>
      <p>The people and institutions behind the Reticulate project.</p>
    </header>

    <!-- Author -->
    <section class="about-section">
      <h2>Author</h2>
      <mat-card>
        <mat-card-header>
          <mat-card-title>Alexandre Zua Caldeira</mat-card-title>
          <mat-card-subtitle>Independent Researcher</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>
            Independent researcher based in Berlin, Germany.
            Research focus: session types, type theory, and programming language design.
          </p>
          <p>
            The Reticulate project develops the theory
            and toolchain for session types as algebraic reticulates.
          </p>
          <div class="person-links">
            <a href="https://www.zuacaldeira.com" target="_blank" rel="noopener">Website</a>
            <a href="https://github.com/zuacaldeira" target="_blank" rel="noopener">GitHub</a>
          </div>
        </mat-card-content>
      </mat-card>
    </section>

    <!-- Project -->
    <section class="about-section">
      <h2>The Project</h2>
      <p>
        <strong>Session Types as Algebraic Reticulates</strong> is a research project with three pillars:
      </p>
      <ol class="pillars-list">
        <li>
          <strong>Theory</strong> &mdash; Proving that session-type state spaces are lattices;
          developing the morphism hierarchy (isomorphism, embedding, projection, Galois connection);
          connecting to bisimulation and abstract interpretation. Two key lemmas formally
          verified in Lean 4 with zero sorry.
        </li>
        <li>
          <strong>Reticulate</strong> &mdash; A Python library (9 modules, 789 tests) that constructs
          state spaces from session type definitions, checks lattice properties, computes
          morphisms, generates tests, and visualizes Hasse diagrams.
        </li>
        <li>
          <strong>BICA Reborn</strong> &mdash; A Java 21 annotation-based session type checker
          (13 packages, 1,052 tests), successor to the original BICA (2009). Key novelty: the
          <code>&parallel;</code> (parallel) constructor for concurrent access, which forces
          lattice structure.
        </li>
      </ol>
    </section>

    <!-- By the numbers -->
    <section class="about-section">
      <h2>By the Numbers</h2>
      <div class="numbers-grid">
        <div class="number-item"><span class="number-value">1,841</span><span class="number-label">Total tests</span></div>
        <div class="number-item"><span class="number-value">34</span><span class="number-label">Benchmark protocols</span></div>
        <div class="number-item"><span class="number-value">5,183</span><span class="number-label">Generated JUnit tests</span></div>
        <div class="number-item"><span class="number-value">2</span><span class="number-label">Lean 4 proofs (0 sorry)</span></div>
        <div class="number-item"><span class="number-value">7</span><span class="number-label">Pipeline stages</span></div>
        <div class="number-item"><span class="number-value">6</span><span class="number-label">Papers in progress</span></div>
      </div>
    </section>

    <!-- Contact -->
    <section class="about-section">
      <h2>Contact</h2>
      <p>
        For questions about the research or collaboration inquiries, please
        reach out via the institutional channels listed above or through the
        <a href="https://github.com/zuacaldeira/SessionTypesResearch" target="_blank" rel="noopener">GitHub repository</a>.
      </p>
    </section>
  `, styles: ["/* angular:styles/component:scss;0a0f3ae5a972b6ac18784ec9118c6787b372aed0eeb9b4c9a3878ab4ce45eb10;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/about/about.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.about-section {\n  margin: 32px 0;\n}\n.about-section h2 {\n  font-size: 20px;\n  font-weight: 600;\n  margin-bottom: 16px;\n}\n.about-section p {\n  line-height: 1.7;\n  margin-bottom: 12px;\n}\nmat-card {\n  margin-bottom: 16px;\n}\nmat-card-content p {\n  line-height: 1.7;\n}\n.person-links {\n  display: flex;\n  gap: 16px;\n  margin-top: 12px;\n}\n.person-links a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n  font-size: 14px;\n}\n.person-links a:hover {\n  text-decoration: underline;\n}\n.pillars-list {\n  padding-left: 24px;\n}\n.pillars-list li {\n  margin-bottom: 12px;\n  line-height: 1.7;\n}\n.numbers-grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));\n  gap: 16px;\n}\n.number-item {\n  text-align: center;\n  padding: 20px 12px;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  background: rgba(0, 0, 0, 0.01);\n}\n.number-value {\n  display: block;\n  font-size: 28px;\n  font-weight: 700;\n  color: var(--brand-primary, #4338ca);\n}\n.number-label {\n  display: block;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.55);\n  margin-top: 4px;\n}\n/*# sourceMappingURL=about.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(AboutComponent, { className: "AboutComponent", filePath: "src/app/pages/about/about.component.ts", lineNumber: 171 });
})();
export {
  AboutComponent
};
//# sourceMappingURL=chunk-TCDXY4GL.js.map
