import {
  CodeBlockComponent
} from "./chunk-EFHCE74K.js";
import "./chunk-R2VWAHTD.js";
import "./chunk-SUS3PTUT.js";
import "./chunk-BUK7DMBP.js";
import {
  RouterLink
} from "./chunk-QTYX35EO.js";
import "./chunk-BFW3NWZD.js";
import "./chunk-ZG4TCI7P.js";
import "./chunk-NL2TMNRB.js";
import {
  Component,
  setClassMetadata,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵdefineComponent,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵproperty,
  ɵɵtext
} from "./chunk-OWEA7TR3.js";

// src/app/pages/publications/publications.component.ts
var PublicationsComponent = class _PublicationsComponent {
  bibtex = `@inproceedings{caldeira2026reticulate,
  author       = {Caldeira, Alexandre Zua},
  title        = {Session Type State Spaces Form Lattices},
  year         = {2026},
  note         = {Draft --- session types as algebraic reticulates}
}`;
  static \u0275fac = function PublicationsComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _PublicationsComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _PublicationsComponent, selectors: [["app-publications"]], decls: 163, vars: 1, consts: [[1, "page-header"], [1, "pub-list"], [1, "pub-title"], [1, "badge-draft"], [1, "pub-authors"], [1, "pub-venue"], [1, "pub-links"], ["href", "/papers/step5-lattice.pdf", "target", "_blank", "rel", "noopener"], ["routerLink", "/tools/analyzer"], ["href", "/papers/ice-2026.pdf", "target", "_blank", "rel", "noopener"], ["href", "/papers/ice-2026-oral.pdf", "target", "_blank", "rel", "noopener"], ["href", "/papers/reticulate-tool.pdf", "target", "_blank", "rel", "noopener"], ["href", "/papers/bica-reborn.pdf", "target", "_blank", "rel", "noopener"], ["href", "/papers/pipeline-article.pdf", "target", "_blank", "rel", "noopener"], ["routerLink", "/pipeline"], ["href", "/papers/slides.pdf", "target", "_blank", "rel", "noopener"], ["href", "/papers/definitions.pdf", "target", "_blank", "rel", "noopener"], [1, "bibtex-section"], ["label", "BibTeX", 3, "code"]], template: function PublicationsComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "Publications");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4, "Papers, slides, and formal definitions from the Reticulate project.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(5, "h2");
      \u0275\u0275text(6, "Research Papers");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(7, "ol", 1)(8, "li")(9, "span", 2);
      \u0275\u0275text(10, "Session Type State Spaces Form Lattices");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(11, "span", 3);
      \u0275\u0275text(12, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(13, "br");
      \u0275\u0275elementStart(14, "span", 4);
      \u0275\u0275text(15, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(16, "br");
      \u0275\u0275elementStart(17, "em", 5);
      \u0275\u0275text(18, "Target: CONCUR 2026, Liverpool, Sep 1\u20134");
      \u0275\u0275elementEnd();
      \u0275\u0275element(19, "br");
      \u0275\u0275elementStart(20, "span", 6);
      \u0275\u0275text(21, " [");
      \u0275\u0275elementStart(22, "a", 7);
      \u0275\u0275text(23, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(24, "] [");
      \u0275\u0275elementStart(25, "a", 8);
      \u0275\u0275text(26, "Live Demo");
      \u0275\u0275elementEnd();
      \u0275\u0275text(27, "] ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(28, "li")(29, "span", 2);
      \u0275\u0275text(30, "Session Type State Spaces Form Lattices (Extended Abstract)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(31, "span", 3);
      \u0275\u0275text(32, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(33, "br");
      \u0275\u0275elementStart(34, "span", 4);
      \u0275\u0275text(35, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(36, "br");
      \u0275\u0275elementStart(37, "em", 5);
      \u0275\u0275text(38, "ICE 2026, Urbino, Jun 12 (DisCoTec satellite workshop)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(39, "br");
      \u0275\u0275elementStart(40, "span", 6);
      \u0275\u0275text(41, " [");
      \u0275\u0275elementStart(42, "a", 9);
      \u0275\u0275text(43, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(44, "] ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(45, "li")(46, "span", 2);
      \u0275\u0275text(47, "Reticulate: A Tool for Lattice Analysis of Session Type State Spaces");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(48, "span", 3);
      \u0275\u0275text(49, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(50, "br");
      \u0275\u0275elementStart(51, "span", 4);
      \u0275\u0275text(52, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(53, "br");
      \u0275\u0275elementStart(54, "em", 5);
      \u0275\u0275text(55, "ICE 2026 Oral Communication, Urbino, Jun 12");
      \u0275\u0275elementEnd();
      \u0275\u0275element(56, "br");
      \u0275\u0275elementStart(57, "span", 6);
      \u0275\u0275text(58, " [");
      \u0275\u0275elementStart(59, "a", 10);
      \u0275\u0275text(60, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(61, "] [");
      \u0275\u0275elementStart(62, "a", 8);
      \u0275\u0275text(63, "Live Demo");
      \u0275\u0275elementEnd();
      \u0275\u0275text(64, "] ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(65, "h2");
      \u0275\u0275text(66, "Tool Papers");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(67, "ol", 1)(68, "li")(69, "span", 2);
      \u0275\u0275text(70, "Reticulate: A Lattice-Theoretic Toolkit for Session Types");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(71, "span", 3);
      \u0275\u0275text(72, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(73, "br");
      \u0275\u0275elementStart(74, "span", 4);
      \u0275\u0275text(75, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(76, "br");
      \u0275\u0275elementStart(77, "em", 5);
      \u0275\u0275text(78, "Target: TACAS 2027 (~Oct 2026) \u2014 Python library, 9 modules, 789 tests");
      \u0275\u0275elementEnd();
      \u0275\u0275element(79, "br");
      \u0275\u0275elementStart(80, "span", 6);
      \u0275\u0275text(81, " [");
      \u0275\u0275elementStart(82, "a", 11);
      \u0275\u0275text(83, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(84, "] ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(85, "li")(86, "span", 2);
      \u0275\u0275text(87, "BICA Reborn: Annotation-Based Session Type Checking for Java");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(88, "span", 3);
      \u0275\u0275text(89, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(90, "br");
      \u0275\u0275elementStart(91, "span", 4);
      \u0275\u0275text(92, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(93, "br");
      \u0275\u0275elementStart(94, "em", 5);
      \u0275\u0275text(95, "Target: OOPSLA 2027 R1 (~Oct 2026) \u2014 Java 21, 13 packages, 1,052 tests");
      \u0275\u0275elementEnd();
      \u0275\u0275element(96, "br");
      \u0275\u0275elementStart(97, "span", 6);
      \u0275\u0275text(98, " [");
      \u0275\u0275elementStart(99, "a", 12);
      \u0275\u0275text(100, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(101, "] ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(102, "h2");
      \u0275\u0275text(103, "Supplementary Materials");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(104, "ol", 1)(105, "li")(106, "span", 2);
      \u0275\u0275text(107, "The Session Type Verification Pipeline");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(108, "span", 3);
      \u0275\u0275text(109, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(110, "br");
      \u0275\u0275elementStart(111, "span", 4);
      \u0275\u0275text(112, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(113, "br");
      \u0275\u0275elementStart(114, "em", 5);
      \u0275\u0275text(115, "Pipeline walkthrough \u2014 7 stages, error catalogue, examples");
      \u0275\u0275elementEnd();
      \u0275\u0275element(116, "br");
      \u0275\u0275elementStart(117, "span", 6);
      \u0275\u0275text(118, " [");
      \u0275\u0275elementStart(119, "a", 13);
      \u0275\u0275text(120, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(121, "] [");
      \u0275\u0275elementStart(122, "a", 14);
      \u0275\u0275text(123, "Web version");
      \u0275\u0275elementEnd();
      \u0275\u0275text(124, "] ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(125, "li")(126, "span", 2);
      \u0275\u0275text(127, "Session Types as Algebraic Reticulates \u2014 Presentation");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(128, "span", 3);
      \u0275\u0275text(129, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(130, "br");
      \u0275\u0275elementStart(131, "span", 4);
      \u0275\u0275text(132, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(133, "br");
      \u0275\u0275elementStart(134, "em", 5);
      \u0275\u0275text(135, "Research presentation \u2014 Beamer, 18 slides");
      \u0275\u0275elementEnd();
      \u0275\u0275element(136, "br");
      \u0275\u0275elementStart(137, "span", 6);
      \u0275\u0275text(138, " [");
      \u0275\u0275elementStart(139, "a", 15);
      \u0275\u0275text(140, "Slides (PDF)");
      \u0275\u0275elementEnd();
      \u0275\u0275text(141, "] ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(142, "li")(143, "span", 2);
      \u0275\u0275text(144, "Formal Definitions");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(145, "span", 3);
      \u0275\u0275text(146, "Draft");
      \u0275\u0275elementEnd();
      \u0275\u0275element(147, "br");
      \u0275\u0275elementStart(148, "span", 4);
      \u0275\u0275text(149, "Alexandre Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275element(150, "br");
      \u0275\u0275elementStart(151, "em", 5);
      \u0275\u0275text(152, "Glossary \u2014 40+ definitions, internal reference document");
      \u0275\u0275elementEnd();
      \u0275\u0275element(153, "br");
      \u0275\u0275elementStart(154, "span", 6);
      \u0275\u0275text(155, " [");
      \u0275\u0275elementStart(156, "a", 16);
      \u0275\u0275text(157, "PDF");
      \u0275\u0275elementEnd();
      \u0275\u0275text(158, "] ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(159, "section", 17)(160, "h2");
      \u0275\u0275text(161, "BibTeX");
      \u0275\u0275elementEnd();
      \u0275\u0275element(162, "app-code-block", 18);
      \u0275\u0275elementEnd();
    }
    if (rf & 2) {
      \u0275\u0275advance(162);
      \u0275\u0275property("code", ctx.bibtex);
    }
  }, dependencies: [RouterLink, CodeBlockComponent], styles: ["\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\nh2[_ngcontent-%COMP%] {\n  font-size: 20px;\n  font-weight: 600;\n  margin: 32px 0 16px;\n}\n.pub-list[_ngcontent-%COMP%] {\n  list-style: decimal;\n  padding-left: 24px;\n}\n.pub-list[_ngcontent-%COMP%]   li[_ngcontent-%COMP%] {\n  margin-bottom: 24px;\n  line-height: 1.6;\n}\n.pub-title[_ngcontent-%COMP%] {\n  font-weight: 500;\n  font-size: 16px;\n}\n.badge-draft[_ngcontent-%COMP%] {\n  display: inline-block;\n  padding: 2px 8px;\n  font-size: 11px;\n  font-weight: 600;\n  text-transform: uppercase;\n  background: #fff3e0;\n  color: #e65100;\n  border-radius: 4px;\n  margin-left: 8px;\n  vertical-align: middle;\n}\n.pub-authors[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.7);\n  font-size: 14px;\n}\n.pub-venue[_ngcontent-%COMP%] {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.6);\n}\n.pub-links[_ngcontent-%COMP%] {\n  font-size: 14px;\n}\n.pub-links[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.pub-links[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n.bibtex-section[_ngcontent-%COMP%] {\n  margin: 32px 0;\n}\n/*# sourceMappingURL=publications.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(PublicationsComponent, [{
    type: Component,
    args: [{ selector: "app-publications", standalone: true, imports: [RouterLink, CodeBlockComponent], template: `
    <header class="page-header">
      <h1>Publications</h1>
      <p>Papers, slides, and formal definitions from the Reticulate project.</p>
    </header>

    <h2>Research Papers</h2>
    <ol class="pub-list">
      <li>
        <span class="pub-title">Session Type State Spaces Form Lattices</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Target: CONCUR 2026, Liverpool, Sep 1&ndash;4</em><br>
        <span class="pub-links">
          [<a href="/papers/step5-lattice.pdf" target="_blank" rel="noopener">PDF</a>]
          [<a routerLink="/tools/analyzer">Live Demo</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Session Type State Spaces Form Lattices (Extended Abstract)</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">ICE 2026, Urbino, Jun 12 (DisCoTec satellite workshop)</em><br>
        <span class="pub-links">
          [<a href="/papers/ice-2026.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Reticulate: A Tool for Lattice Analysis of Session Type State Spaces</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">ICE 2026 Oral Communication, Urbino, Jun 12</em><br>
        <span class="pub-links">
          [<a href="/papers/ice-2026-oral.pdf" target="_blank" rel="noopener">PDF</a>]
          [<a routerLink="/tools/analyzer">Live Demo</a>]
        </span>
      </li>
    </ol>

    <h2>Tool Papers</h2>
    <ol class="pub-list">
      <li>
        <span class="pub-title">Reticulate: A Lattice-Theoretic Toolkit for Session Types</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Target: TACAS 2027 (~Oct 2026) &mdash; Python library, 9 modules, 789 tests</em><br>
        <span class="pub-links">
          [<a href="/papers/reticulate-tool.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">BICA Reborn: Annotation-Based Session Type Checking for Java</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Target: OOPSLA 2027 R1 (~Oct 2026) &mdash; Java 21, 13 packages, 1,052 tests</em><br>
        <span class="pub-links">
          [<a href="/papers/bica-reborn.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
    </ol>

    <h2>Supplementary Materials</h2>
    <ol class="pub-list">
      <li>
        <span class="pub-title">The Session Type Verification Pipeline</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Pipeline walkthrough &mdash; 7 stages, error catalogue, examples</em><br>
        <span class="pub-links">
          [<a href="/papers/pipeline-article.pdf" target="_blank" rel="noopener">PDF</a>]
          [<a routerLink="/pipeline">Web version</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Session Types as Algebraic Reticulates &mdash; Presentation</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Research presentation &mdash; Beamer, 18 slides</em><br>
        <span class="pub-links">
          [<a href="/papers/slides.pdf" target="_blank" rel="noopener">Slides (PDF)</a>]
        </span>
      </li>
      <li>
        <span class="pub-title">Formal Definitions</span>
        <span class="badge-draft">Draft</span><br>
        <span class="pub-authors">Alexandre Zua Caldeira</span><br>
        <em class="pub-venue">Glossary &mdash; 40+ definitions, internal reference document</em><br>
        <span class="pub-links">
          [<a href="/papers/definitions.pdf" target="_blank" rel="noopener">PDF</a>]
        </span>
      </li>
    </ol>

    <section class="bibtex-section">
      <h2>BibTeX</h2>
      <app-code-block [code]="bibtex" label="BibTeX"></app-code-block>
    </section>
  `, styles: ["/* angular:styles/component:scss;b9c9da90401f50c92d6df28c2f46962c9ede258e2bb80f8bfa4b3ad8c6e14ed3;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/publications/publications.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\nh2 {\n  font-size: 20px;\n  font-weight: 600;\n  margin: 32px 0 16px;\n}\n.pub-list {\n  list-style: decimal;\n  padding-left: 24px;\n}\n.pub-list li {\n  margin-bottom: 24px;\n  line-height: 1.6;\n}\n.pub-title {\n  font-weight: 500;\n  font-size: 16px;\n}\n.badge-draft {\n  display: inline-block;\n  padding: 2px 8px;\n  font-size: 11px;\n  font-weight: 600;\n  text-transform: uppercase;\n  background: #fff3e0;\n  color: #e65100;\n  border-radius: 4px;\n  margin-left: 8px;\n  vertical-align: middle;\n}\n.pub-authors {\n  color: rgba(0, 0, 0, 0.7);\n  font-size: 14px;\n}\n.pub-venue {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.6);\n}\n.pub-links {\n  font-size: 14px;\n}\n.pub-links a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.pub-links a:hover {\n  text-decoration: underline;\n}\n.bibtex-section {\n  margin: 32px 0;\n}\n/*# sourceMappingURL=publications.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(PublicationsComponent, { className: "PublicationsComponent", filePath: "src/app/pages/publications/publications.component.ts", lineNumber: 175 });
})();
export {
  PublicationsComponent
};
//# sourceMappingURL=chunk-DUFPOB2B.js.map
