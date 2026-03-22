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
  HostListener,
  setClassMetadata,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵclassProp,
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
  ɵɵresetView,
  ɵɵresolveWindow,
  ɵɵrestoreView,
  ɵɵtext,
  ɵɵtextInterpolate
} from "./chunk-OWEA7TR3.js";

// src/app/pages/documentation/documentation.component.ts
var _forTrack0 = ($index, $item) => $item.id;
function DocumentationComponent_For_18_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "li")(1, "a", 22);
    \u0275\u0275listener("click", function DocumentationComponent_For_18_Template_a_click_1_listener() {
      const entry_r2 = \u0275\u0275restoreView(_r1).$implicit;
      const ctx_r2 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r2.scrollTo(entry_r2.id));
    });
    \u0275\u0275text(2);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const entry_r2 = ctx.$implicit;
    const ctx_r2 = \u0275\u0275nextContext();
    \u0275\u0275classProp("sub", entry_r2.level === 2)("active", ctx_r2.activeSection === entry_r2.id);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(entry_r2.label);
  }
}
var DocumentationComponent = class _DocumentationComponent {
  activeSection = "theory";
  sectionIds = [];
  tocEntries = [
    { id: "theory", label: "Theory", level: 1 },
    { id: "session-types", label: "Session Types", level: 2 },
    { id: "grammar", label: "Grammar", level: 2 },
    { id: "constructors", label: "Constructors", level: 2 },
    { id: "state-spaces", label: "State Spaces", level: 2 },
    { id: "lattice-properties", label: "Lattice Properties", level: 2 },
    { id: "parallel-constructor", label: "Parallel Constructor", level: 2 },
    { id: "morphisms", label: "Morphism Hierarchy", level: 2 }
  ];
  ngOnInit() {
    this.sectionIds = this.tocEntries.map((e) => e.id);
  }
  ngOnDestroy() {
  }
  onScroll() {
    const offset = 120;
    for (let i = this.sectionIds.length - 1; i >= 0; i--) {
      const el = document.getElementById(this.sectionIds[i]);
      if (el && el.getBoundingClientRect().top <= offset) {
        this.activeSection = this.sectionIds[i];
        return;
      }
    }
    this.activeSection = this.sectionIds[0];
  }
  scrollTo(id) {
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
  }
  grammarCode = `S  ::=  &{ m\u2081 : S\u2081 , \u2026 , m\u2099 : S\u2099 }    \u2014 branch (external choice)
     |  +{ l\u2081 : S\u2081 , \u2026 , l\u2099 : S\u2099 }    \u2014 selection (internal choice)
     |  ( S\u2081 || S\u2082 )                    \u2014 parallel
     |  rec X . S                        \u2014 recursion
     |  X                                \u2014 variable
     |  end                              \u2014 terminated
     |  S\u2081 . S\u2082                          \u2014 sequencing`;
  static \u0275fac = function DocumentationComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _DocumentationComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _DocumentationComponent, selectors: [["app-documentation"]], hostBindings: function DocumentationComponent_HostBindings(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275listener("scroll", function DocumentationComponent_scroll_HostBindingHandler() {
        return ctx.onScroll();
      }, \u0275\u0275resolveWindow);
    }
  }, decls: 244, vars: 1, consts: [[1, "page-header"], ["routerLink", "/tutorials"], ["routerLink", "/faq"], [1, "doc-layout"], [1, "doc-sidebar"], [1, "sidebar-nav"], [3, "sub", "active"], [1, "doc-content"], ["id", "theory", 1, "doc-section"], ["id", "session-types", 1, "theory-section"], [1, "example-card"], ["code", "open . rec X . &{read: +{data: X, eof: close . end}}", "label", "Session type"], ["id", "grammar", 1, "theory-section"], ["label", "Grammar", 3, "code"], ["id", "constructors", 1, "theory-section"], [1, "constructors-grid"], [1, "constructor-card"], ["id", "state-spaces", 1, "theory-section"], ["id", "lattice-properties", 1, "theory-section"], ["id", "parallel-constructor", 1, "theory-section"], [1, "theory-highlight"], ["id", "morphisms", 1, "theory-section"], [3, "click"]], template: function DocumentationComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "Documentation");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p");
      \u0275\u0275text(4, "Theory reference. Looking for hands-on guides? See the ");
      \u0275\u0275elementStart(5, "a", 1);
      \u0275\u0275text(6, "tutorials");
      \u0275\u0275elementEnd();
      \u0275\u0275text(7, ". Have questions? Check the ");
      \u0275\u0275elementStart(8, "a", 2);
      \u0275\u0275text(9, "FAQ");
      \u0275\u0275elementEnd();
      \u0275\u0275text(10, ".");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(11, "div", 3)(12, "aside", 4)(13, "nav", 5)(14, "h3");
      \u0275\u0275text(15, "Contents");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(16, "ul");
      \u0275\u0275repeaterCreate(17, DocumentationComponent_For_18_Template, 3, 5, "li", 6, _forTrack0);
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(19, "div", 7)(20, "section", 8)(21, "h2");
      \u0275\u0275text(22, "Theory");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(23, "div", 9)(24, "h3");
      \u0275\u0275text(25, "Session Types");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(26, "p");
      \u0275\u0275text(27, " A ");
      \u0275\u0275elementStart(28, "strong");
      \u0275\u0275text(29, "session type");
      \u0275\u0275elementEnd();
      \u0275\u0275text(30, " describes a communication protocol on an object \u2014 the legal sequences of method calls, branches, and selections that a client may perform. Instead of a flat interface, an object's type evolves as methods are called, enforcing protocol compliance at compile time. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(31, "div", 10)(32, "h4");
      \u0275\u0275text(33, "Example: File Object");
      \u0275\u0275elementEnd();
      \u0275\u0275element(34, "app-code-block", 11);
      \u0275\u0275elementStart(35, "p");
      \u0275\u0275text(36, " Open the file, then repeatedly read: on ");
      \u0275\u0275elementStart(37, "code");
      \u0275\u0275text(38, "data");
      \u0275\u0275elementEnd();
      \u0275\u0275text(39, ", loop back; on ");
      \u0275\u0275elementStart(40, "code");
      \u0275\u0275text(41, "eof");
      \u0275\u0275elementEnd();
      \u0275\u0275text(42, ", close and terminate. The type ensures ");
      \u0275\u0275elementStart(43, "code");
      \u0275\u0275text(44, "close");
      \u0275\u0275elementEnd();
      \u0275\u0275text(45, " is always called exactly once. ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(46, "div", 12)(47, "h3");
      \u0275\u0275text(48, "Grammar");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(49, "p");
      \u0275\u0275text(50, "Session types are defined by the following grammar:");
      \u0275\u0275elementEnd();
      \u0275\u0275element(51, "app-code-block", 13);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(52, "div", 14)(53, "h3");
      \u0275\u0275text(54, "Constructors");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(55, "div", 15)(56, "article", 16)(57, "h4")(58, "code");
      \u0275\u0275text(59, "&{...}");
      \u0275\u0275elementEnd();
      \u0275\u0275text(60, " \u2014 Branch");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(61, "p")(62, "strong");
      \u0275\u0275text(63, "External choice.");
      \u0275\u0275elementEnd();
      \u0275\u0275text(64, " The environment (client) chooses which method to call. Each branch leads to a different continuation type. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(65, "article", 16)(66, "h4")(67, "code");
      \u0275\u0275text(68, "+{...}");
      \u0275\u0275elementEnd();
      \u0275\u0275text(69, " \u2014 Selection");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(70, "p")(71, "strong");
      \u0275\u0275text(72, "Internal choice.");
      \u0275\u0275elementEnd();
      \u0275\u0275text(73, " The object (server) decides the outcome. The client must handle all possibilities. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(74, "article", 16)(75, "h4")(76, "code");
      \u0275\u0275text(77, "( S1 || S2 )");
      \u0275\u0275elementEnd();
      \u0275\u0275text(78, " \u2014 Parallel");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(79, "p")(80, "strong");
      \u0275\u0275text(81, "Concurrent access.");
      \u0275\u0275elementEnd();
      \u0275\u0275text(82, " Two execution paths run simultaneously on a shared object. The combined state space is the product lattice. This is the ");
      \u0275\u0275elementStart(83, "strong");
      \u0275\u0275text(84, "key novelty");
      \u0275\u0275elementEnd();
      \u0275\u0275text(85, " of this work. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(86, "article", 16)(87, "h4")(88, "code");
      \u0275\u0275text(89, "rec X . S");
      \u0275\u0275elementEnd();
      \u0275\u0275text(90, " \u2014 Recursion");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(91, "p")(92, "strong");
      \u0275\u0275text(93, "Looping protocols.");
      \u0275\u0275elementEnd();
      \u0275\u0275text(94, " The variable ");
      \u0275\u0275elementStart(95, "code");
      \u0275\u0275text(96, "X");
      \u0275\u0275elementEnd();
      \u0275\u0275text(97, " marks the loop point. Well-formed recursive types must have an exit path. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(98, "article", 16)(99, "h4")(100, "code");
      \u0275\u0275text(101, "S1 . S2");
      \u0275\u0275elementEnd();
      \u0275\u0275text(102, " \u2014 Sequencing");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(103, "p")(104, "strong");
      \u0275\u0275text(105, "Sequential composition.");
      \u0275\u0275elementEnd();
      \u0275\u0275text(106, " Syntactic sugar for a single-method branch: ");
      \u0275\u0275elementStart(107, "code");
      \u0275\u0275text(108, "m . S");
      \u0275\u0275elementEnd();
      \u0275\u0275text(109, " is equivalent to ");
      \u0275\u0275elementStart(110, "code");
      \u0275\u0275text(111, "&{m: S}");
      \u0275\u0275elementEnd();
      \u0275\u0275text(112, ". ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(113, "article", 16)(114, "h4")(115, "code");
      \u0275\u0275text(116, "end");
      \u0275\u0275elementEnd();
      \u0275\u0275text(117, " \u2014 Terminated");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(118, "p")(119, "strong");
      \u0275\u0275text(120, "Protocol end.");
      \u0275\u0275elementEnd();
      \u0275\u0275text(121, " No further operations are allowed. Every well-formed session type must eventually reach ");
      \u0275\u0275elementStart(122, "code");
      \u0275\u0275text(123, "end");
      \u0275\u0275elementEnd();
      \u0275\u0275text(124, ". ");
      \u0275\u0275elementEnd()()()();
      \u0275\u0275elementStart(125, "div", 17)(126, "h3");
      \u0275\u0275text(127, "State Spaces");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(128, "p");
      \u0275\u0275text(129, " Given a session type ");
      \u0275\u0275elementStart(130, "code");
      \u0275\u0275text(131, "S");
      \u0275\u0275elementEnd();
      \u0275\u0275text(132, ", we construct its ");
      \u0275\u0275elementStart(133, "strong");
      \u0275\u0275text(134, "state space");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(135, "code");
      \u0275\u0275text(136, "L(S)");
      \u0275\u0275elementEnd();
      \u0275\u0275text(137, " \u2014 a directed graph where: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(138, "ul")(139, "li")(140, "strong");
      \u0275\u0275text(141, "States");
      \u0275\u0275elementEnd();
      \u0275\u0275text(142, " are the reachable configurations of the protocol");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(143, "li")(144, "strong");
      \u0275\u0275text(145, "Transitions");
      \u0275\u0275elementEnd();
      \u0275\u0275text(146, " are labeled edges (method calls, selections)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(147, "li");
      \u0275\u0275text(148, "The ");
      \u0275\u0275elementStart(149, "strong");
      \u0275\u0275text(150, "initial state");
      \u0275\u0275elementEnd();
      \u0275\u0275text(151, " (top) is the protocol's entry point");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(152, "li");
      \u0275\u0275text(153, "The ");
      \u0275\u0275elementStart(154, "strong");
      \u0275\u0275text(155, "terminal state");
      \u0275\u0275elementEnd();
      \u0275\u0275text(156, " (bottom) corresponds to ");
      \u0275\u0275elementStart(157, "code");
      \u0275\u0275text(158, "end");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(159, "p");
      \u0275\u0275text(160, " The ");
      \u0275\u0275elementStart(161, "strong");
      \u0275\u0275text(162, "reachability ordering");
      \u0275\u0275elementEnd();
      \u0275\u0275text(163, " defines a partial order on states: s1 \u2265 s2 iff there is a path from s1 to s2. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(164, "div", 18)(165, "h3");
      \u0275\u0275text(166, "Lattice Properties");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(167, "p");
      \u0275\u0275text(168, " A state space is a ");
      \u0275\u0275elementStart(169, "strong");
      \u0275\u0275text(170, "lattice");
      \u0275\u0275elementEnd();
      \u0275\u0275text(171, " (a ");
      \u0275\u0275elementStart(172, "em");
      \u0275\u0275text(173, "reticulate");
      \u0275\u0275elementEnd();
      \u0275\u0275text(174, ") if and only if: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(175, "ol")(176, "li");
      \u0275\u0275text(177, "There is a ");
      \u0275\u0275elementStart(178, "strong");
      \u0275\u0275text(179, "top element");
      \u0275\u0275elementEnd();
      \u0275\u0275text(180, " (initial state)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(181, "li");
      \u0275\u0275text(182, "There is a ");
      \u0275\u0275elementStart(183, "strong");
      \u0275\u0275text(184, "bottom element");
      \u0275\u0275elementEnd();
      \u0275\u0275text(185, " (terminal state)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(186, "li");
      \u0275\u0275text(187, "Every pair of states has a ");
      \u0275\u0275elementStart(188, "strong");
      \u0275\u0275text(189, "meet");
      \u0275\u0275elementEnd();
      \u0275\u0275text(190, " (greatest lower bound)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(191, "li");
      \u0275\u0275text(192, "Every pair of states has a ");
      \u0275\u0275elementStart(193, "strong");
      \u0275\u0275text(194, "join");
      \u0275\u0275elementEnd();
      \u0275\u0275text(195, " (least upper bound)");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(196, "p");
      \u0275\u0275text(197, " For cyclic state spaces (from recursion), we first ");
      \u0275\u0275elementStart(198, "strong");
      \u0275\u0275text(199, "quotient by SCCs");
      \u0275\u0275elementEnd();
      \u0275\u0275text(200, " to obtain an acyclic DAG, then check lattice properties on the quotient. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(201, "div", 19)(202, "h3");
      \u0275\u0275text(203, "The Parallel Constructor");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(204, "p");
      \u0275\u0275text(205, " The ");
      \u0275\u0275elementStart(206, "code");
      \u0275\u0275text(207, "\u2225");
      \u0275\u0275elementEnd();
      \u0275\u0275text(208, " constructor is the key novelty of this work. When two branches execute in parallel on a shared object: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(209, "div", 20)(210, "code");
      \u0275\u0275text(211, "L(S1 \u2225 S2) = L(S1) \xD7 L(S2)");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(212, "p");
      \u0275\u0275text(213, " The product construction orders states componentwise. ");
      \u0275\u0275elementStart(214, "strong");
      \u0275\u0275text(215, "Crucially");
      \u0275\u0275elementEnd();
      \u0275\u0275text(216, ", the product of two lattices is always a lattice. This means that any well-formed session type using ");
      \u0275\u0275elementStart(217, "code");
      \u0275\u0275text(218, "\u2225");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(219, "em");
      \u0275\u0275text(220, "necessarily");
      \u0275\u0275elementEnd();
      \u0275\u0275text(221, " has a lattice state space. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(222, "div", 21)(223, "h3");
      \u0275\u0275text(224, "Morphism Hierarchy");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(225, "p");
      \u0275\u0275text(226, " Between session-type state spaces, we define a hierarchy of structure-preserving maps: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(227, "ol")(228, "li")(229, "strong");
      \u0275\u0275text(230, "Isomorphism");
      \u0275\u0275elementEnd();
      \u0275\u0275text(231, " \u2014 bijective, order-preserving and reflecting.");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(232, "li")(233, "strong");
      \u0275\u0275text(234, "Embedding");
      \u0275\u0275elementEnd();
      \u0275\u0275text(235, " \u2014 injective, order-preserving and reflecting.");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(236, "li")(237, "strong");
      \u0275\u0275text(238, "Projection");
      \u0275\u0275elementEnd();
      \u0275\u0275text(239, " \u2014 surjective, order-preserving.");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(240, "li")(241, "strong");
      \u0275\u0275text(242, "Galois connection");
      \u0275\u0275elementEnd();
      \u0275\u0275text(243, " \u2014 an adjunction \u03B1(x) \u2264 y \u21D4 x \u2264 \u03B3(y).");
      \u0275\u0275elementEnd()()()()()();
    }
    if (rf & 2) {
      \u0275\u0275advance(17);
      \u0275\u0275repeater(ctx.tocEntries);
      \u0275\u0275advance(34);
      \u0275\u0275property("code", ctx.grammarCode);
    }
  }, dependencies: [RouterLink, CodeBlockComponent], styles: ["\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.doc-layout[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 32px;\n  align-items: flex-start;\n}\n.doc-sidebar[_ngcontent-%COMP%] {\n  position: sticky;\n  top: 80px;\n  width: 240px;\n  flex-shrink: 0;\n  max-height: calc(100vh - 100px);\n  overflow-y: auto;\n}\n.sidebar-nav[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 13px;\n  font-weight: 600;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: rgba(0, 0, 0, 0.5);\n  margin: 0 0 12px;\n  padding: 0 12px;\n}\n.sidebar-nav[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%] {\n  list-style: none;\n  margin: 0;\n  padding: 0;\n}\n.sidebar-nav[_ngcontent-%COMP%]   li[_ngcontent-%COMP%] {\n  margin: 0;\n}\n.sidebar-nav[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  display: block;\n  padding: 6px 12px;\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.7);\n  text-decoration: none;\n  border-left: 3px solid transparent;\n  cursor: pointer;\n  transition: all 0.15s;\n}\n.sidebar-nav[_ngcontent-%COMP%]   li.sub[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  padding-left: 24px;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.55);\n}\n.sidebar-nav[_ngcontent-%COMP%]   li[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  color: var(--brand-primary, #4338ca);\n  background: rgba(67, 56, 202, 0.04);\n}\n.sidebar-nav[_ngcontent-%COMP%]   li.active[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  border-left-color: var(--brand-primary, #4338ca);\n  font-weight: 500;\n  background: rgba(67, 56, 202, 0.06);\n}\n.sidebar-nav[_ngcontent-%COMP%]   li.active.sub[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  font-weight: 500;\n}\n.doc-content[_ngcontent-%COMP%] {\n  flex: 1;\n  min-width: 0;\n}\n@media (max-width: 900px) {\n  .doc-layout[_ngcontent-%COMP%] {\n    flex-direction: column;\n  }\n  .doc-sidebar[_ngcontent-%COMP%] {\n    position: static;\n    width: 100%;\n    max-height: none;\n    border: 1px solid rgba(0, 0, 0, 0.08);\n    border-radius: 8px;\n    padding: 12px 0;\n    background: rgba(0, 0, 0, 0.01);\n  }\n}\n.doc-section[_ngcontent-%COMP%] {\n  margin: 40px 0;\n}\n.doc-section[_ngcontent-%COMP%]   h2[_ngcontent-%COMP%] {\n  font-size: 22px;\n  font-weight: 600;\n  margin-bottom: 16px;\n}\n.doc-section[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 18px;\n  font-weight: 500;\n  margin: 24px 0 12px;\n}\n.doc-section[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.7;\n  margin-bottom: 12px;\n}\n.doc-section[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%], \n.doc-section[_ngcontent-%COMP%]   ol[_ngcontent-%COMP%] {\n  line-height: 1.8;\n  margin-bottom: 12px;\n}\n.theory-section[_ngcontent-%COMP%] {\n  margin: 24px 0;\n}\n.example-card[_ngcontent-%COMP%] {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px;\n  margin: 12px 0;\n  background: rgba(0, 0, 0, 0.01);\n}\n.example-card[_ngcontent-%COMP%]   h4[_ngcontent-%COMP%] {\n  font-size: 14px;\n  font-weight: 500;\n  margin: 0 0 8px;\n}\n.example-card[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  margin: 8px 0 0;\n  font-size: 14px;\n}\n.constructors-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));\n  gap: 16px;\n  margin: 16px 0;\n}\n.constructor-card[_ngcontent-%COMP%] {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px;\n  background: rgba(0, 0, 0, 0.01);\n}\n.constructor-card[_ngcontent-%COMP%]   h4[_ngcontent-%COMP%] {\n  font-size: 14px;\n  font-weight: 500;\n  margin: 0 0 8px;\n}\n.constructor-card[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  font-size: 14px;\n  line-height: 1.6;\n  margin: 0;\n}\n.theory-highlight[_ngcontent-%COMP%] {\n  text-align: center;\n  font-size: 18px;\n  padding: 16px;\n  margin: 16px 0;\n  background: rgba(67, 56, 202, 0.05);\n  border-radius: 8px;\n  border: 1px solid rgba(67, 56, 202, 0.15);\n}\n/*# sourceMappingURL=documentation.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(DocumentationComponent, [{
    type: Component,
    args: [{ selector: "app-documentation", standalone: true, imports: [RouterLink, CodeBlockComponent], template: `
    <header class="page-header">
      <h1>Documentation</h1>
      <p>Theory reference. Looking for hands-on guides? See the <a routerLink="/tutorials">tutorials</a>. Have questions? Check the <a routerLink="/faq">FAQ</a>.</p>
    </header>

    <div class="doc-layout">
      <!-- Sticky sidebar -->
      <aside class="doc-sidebar">
        <nav class="sidebar-nav">
          <h3>Contents</h3>
          <ul>
            @for (entry of tocEntries; track entry.id) {
              <li [class.sub]="entry.level === 2"
                  [class.active]="activeSection === entry.id">
                <a (click)="scrollTo(entry.id)">{{ entry.label }}</a>
              </li>
            }
          </ul>
        </nav>
      </aside>

      <!-- Main content -->
      <div class="doc-content">

        <!-- ================================================================ -->
        <!-- THEORY                                                           -->
        <!-- ================================================================ -->
        <section class="doc-section" id="theory">
          <h2>Theory</h2>

          <div class="theory-section" id="session-types">
            <h3>Session Types</h3>
            <p>
              A <strong>session type</strong> describes a communication protocol on an object &mdash;
              the legal sequences of method calls, branches, and selections that a client may perform.
              Instead of a flat interface, an object's type evolves as methods are called, enforcing
              protocol compliance at compile time.
            </p>
            <div class="example-card">
              <h4>Example: File Object</h4>
              <app-code-block code="open . rec X . &{read: +{data: X, eof: close . end}}" label="Session type"></app-code-block>
              <p>
                Open the file, then repeatedly read: on <code>data</code>, loop back;
                on <code>eof</code>, close and terminate. The type ensures <code>close</code>
                is always called exactly once.
              </p>
            </div>
          </div>

          <div class="theory-section" id="grammar">
            <h3>Grammar</h3>
            <p>Session types are defined by the following grammar:</p>
            <app-code-block [code]="grammarCode" label="Grammar"></app-code-block>
          </div>

          <div class="theory-section" id="constructors">
            <h3>Constructors</h3>
            <div class="constructors-grid">
              <article class="constructor-card">
                <h4><code>&amp;&#123;...&#125;</code> &mdash; Branch</h4>
                <p>
                  <strong>External choice.</strong> The environment (client) chooses which method
                  to call. Each branch leads to a different continuation type.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>+&#123;...&#125;</code> &mdash; Selection</h4>
                <p>
                  <strong>Internal choice.</strong> The object (server) decides the outcome.
                  The client must handle all possibilities.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>( S1 || S2 )</code> &mdash; Parallel</h4>
                <p>
                  <strong>Concurrent access.</strong> Two execution paths run simultaneously
                  on a shared object. The combined state space is the product lattice.
                  This is the <strong>key novelty</strong> of this work.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>rec X . S</code> &mdash; Recursion</h4>
                <p>
                  <strong>Looping protocols.</strong> The variable <code>X</code> marks the
                  loop point. Well-formed recursive types must have an exit path.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>S1 . S2</code> &mdash; Sequencing</h4>
                <p>
                  <strong>Sequential composition.</strong> Syntactic sugar for a single-method
                  branch: <code>m . S</code> is equivalent to <code>&amp;&#123;m: S&#125;</code>.
                </p>
              </article>
              <article class="constructor-card">
                <h4><code>end</code> &mdash; Terminated</h4>
                <p>
                  <strong>Protocol end.</strong> No further operations are allowed. Every
                  well-formed session type must eventually reach <code>end</code>.
                </p>
              </article>
            </div>
          </div>

          <div class="theory-section" id="state-spaces">
            <h3>State Spaces</h3>
            <p>
              Given a session type <code>S</code>, we construct its <strong>state space</strong>
              <code>L(S)</code> &mdash; a directed graph where:
            </p>
            <ul>
              <li><strong>States</strong> are the reachable configurations of the protocol</li>
              <li><strong>Transitions</strong> are labeled edges (method calls, selections)</li>
              <li>The <strong>initial state</strong> (top) is the protocol's entry point</li>
              <li>The <strong>terminal state</strong> (bottom) corresponds to <code>end</code></li>
            </ul>
            <p>
              The <strong>reachability ordering</strong> defines a partial order on states:
              s1 &ge; s2 iff there is a path from s1 to s2.
            </p>
          </div>

          <div class="theory-section" id="lattice-properties">
            <h3>Lattice Properties</h3>
            <p>
              A state space is a <strong>lattice</strong> (a <em>reticulate</em>) if and only if:
            </p>
            <ol>
              <li>There is a <strong>top element</strong> (initial state)</li>
              <li>There is a <strong>bottom element</strong> (terminal state)</li>
              <li>Every pair of states has a <strong>meet</strong> (greatest lower bound)</li>
              <li>Every pair of states has a <strong>join</strong> (least upper bound)</li>
            </ol>
            <p>
              For cyclic state spaces (from recursion), we first <strong>quotient by SCCs</strong>
              to obtain an acyclic DAG, then check lattice properties on the quotient.
            </p>
          </div>

          <div class="theory-section" id="parallel-constructor">
            <h3>The Parallel Constructor</h3>
            <p>
              The <code>&parallel;</code> constructor is the key novelty of this work. When two
              branches execute in parallel on a shared object:
            </p>
            <div class="theory-highlight">
              <code>L(S1 &parallel; S2) = L(S1) &times; L(S2)</code>
            </div>
            <p>
              The product construction orders states componentwise.
              <strong>Crucially</strong>, the product of two lattices is always a lattice.
              This means that any well-formed session type using <code>&parallel;</code>
              <em>necessarily</em> has a lattice state space.
            </p>
          </div>

          <div class="theory-section" id="morphisms">
            <h3>Morphism Hierarchy</h3>
            <p>
              Between session-type state spaces, we define a hierarchy of structure-preserving maps:
            </p>
            <ol>
              <li><strong>Isomorphism</strong> &mdash; bijective, order-preserving and reflecting.</li>
              <li><strong>Embedding</strong> &mdash; injective, order-preserving and reflecting.</li>
              <li><strong>Projection</strong> &mdash; surjective, order-preserving.</li>
              <li><strong>Galois connection</strong> &mdash; an adjunction &alpha;(x) &le; y &hArr; x &le; &gamma;(y).</li>
            </ol>
          </div>
        </section>

      </div>
    </div>
  `, styles: ["/* angular:styles/component:scss;461c1a3b7af755f3ca65e850b70319b1af44f39d4d7ba198dc30708ad82e1d92;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/documentation/documentation.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-header p {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n.doc-layout {\n  display: flex;\n  gap: 32px;\n  align-items: flex-start;\n}\n.doc-sidebar {\n  position: sticky;\n  top: 80px;\n  width: 240px;\n  flex-shrink: 0;\n  max-height: calc(100vh - 100px);\n  overflow-y: auto;\n}\n.sidebar-nav h3 {\n  font-size: 13px;\n  font-weight: 600;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: rgba(0, 0, 0, 0.5);\n  margin: 0 0 12px;\n  padding: 0 12px;\n}\n.sidebar-nav ul {\n  list-style: none;\n  margin: 0;\n  padding: 0;\n}\n.sidebar-nav li {\n  margin: 0;\n}\n.sidebar-nav li a {\n  display: block;\n  padding: 6px 12px;\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.7);\n  text-decoration: none;\n  border-left: 3px solid transparent;\n  cursor: pointer;\n  transition: all 0.15s;\n}\n.sidebar-nav li.sub a {\n  padding-left: 24px;\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.55);\n}\n.sidebar-nav li a:hover {\n  color: var(--brand-primary, #4338ca);\n  background: rgba(67, 56, 202, 0.04);\n}\n.sidebar-nav li.active a {\n  color: var(--brand-primary, #4338ca);\n  border-left-color: var(--brand-primary, #4338ca);\n  font-weight: 500;\n  background: rgba(67, 56, 202, 0.06);\n}\n.sidebar-nav li.active.sub a {\n  font-weight: 500;\n}\n.doc-content {\n  flex: 1;\n  min-width: 0;\n}\n@media (max-width: 900px) {\n  .doc-layout {\n    flex-direction: column;\n  }\n  .doc-sidebar {\n    position: static;\n    width: 100%;\n    max-height: none;\n    border: 1px solid rgba(0, 0, 0, 0.08);\n    border-radius: 8px;\n    padding: 12px 0;\n    background: rgba(0, 0, 0, 0.01);\n  }\n}\n.doc-section {\n  margin: 40px 0;\n}\n.doc-section h2 {\n  font-size: 22px;\n  font-weight: 600;\n  margin-bottom: 16px;\n}\n.doc-section h3 {\n  font-size: 18px;\n  font-weight: 500;\n  margin: 24px 0 12px;\n}\n.doc-section p {\n  line-height: 1.7;\n  margin-bottom: 12px;\n}\n.doc-section ul,\n.doc-section ol {\n  line-height: 1.8;\n  margin-bottom: 12px;\n}\n.theory-section {\n  margin: 24px 0;\n}\n.example-card {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px;\n  margin: 12px 0;\n  background: rgba(0, 0, 0, 0.01);\n}\n.example-card h4 {\n  font-size: 14px;\n  font-weight: 500;\n  margin: 0 0 8px;\n}\n.example-card p {\n  margin: 8px 0 0;\n  font-size: 14px;\n}\n.constructors-grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));\n  gap: 16px;\n  margin: 16px 0;\n}\n.constructor-card {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px;\n  background: rgba(0, 0, 0, 0.01);\n}\n.constructor-card h4 {\n  font-size: 14px;\n  font-weight: 500;\n  margin: 0 0 8px;\n}\n.constructor-card p {\n  font-size: 14px;\n  line-height: 1.6;\n  margin: 0;\n}\n.theory-highlight {\n  text-align: center;\n  font-size: 18px;\n  padding: 16px;\n  margin: 16px 0;\n  background: rgba(67, 56, 202, 0.05);\n  border-radius: 8px;\n  border: 1px solid rgba(67, 56, 202, 0.15);\n}\n/*# sourceMappingURL=documentation.component.css.map */\n"] }]
  }], null, { onScroll: [{
    type: HostListener,
    args: ["window:scroll"]
  }] });
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(DocumentationComponent, { className: "DocumentationComponent", filePath: "src/app/pages/documentation/documentation.component.ts", lineNumber: 364 });
})();
export {
  DocumentationComponent
};
//# sourceMappingURL=chunk-BDRXLE3T.js.map
