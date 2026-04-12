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
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵdefineComponent,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵproperty,
  ɵɵpureFunction0,
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵtext,
  ɵɵtextInterpolate
} from "./chunk-OWEA7TR3.js";

// src/app/pages/pipeline/pipeline.component.ts
var _c0 = () => ["/tools/analyzer"];
var _c1 = () => ({ type: "open . &{read: close . end, write: close . end}" });
var _forTrack0 = ($index, $item) => $item.num;
function PipelineComponent_For_7_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "span", 22);
    \u0275\u0275text(1, "\u2192");
    \u0275\u0275elementEnd();
  }
}
function PipelineComponent_For_7_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 19)(1, "span", 20);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "span", 21);
    \u0275\u0275text(4);
    \u0275\u0275elementEnd()();
    \u0275\u0275conditionalCreate(5, PipelineComponent_For_7_Conditional_5_Template, 2, 0, "span", 22);
  }
  if (rf & 2) {
    const stage_r1 = ctx.$implicit;
    const \u0275$index_11_r2 = ctx.$index;
    const \u0275$count_11_r3 = ctx.$count;
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(stage_r1.num);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(stage_r1.name);
    \u0275\u0275advance();
    \u0275\u0275conditional(!(\u0275$index_11_r2 === \u0275$count_11_r3 - 1) ? 5 : -1);
  }
}
var PipelineComponent = class _PipelineComponent {
  stages = [
    { num: 1, name: "Parse" },
    { num: 2, name: "Termination" },
    { num: 3, name: "WF-Par" },
    { num: 4, name: "State Space" },
    { num: 5, name: "Conformance" },
    { num: 6, name: "Lattice" },
    { num: 7, name: "Thread Safety" }
  ];
  grammarCode = `S  ::=  &{ m\u2081 : S\u2081 , ... , m\u2099 : S\u2099 }     -- branch (external choice)
     |  +{ l\u2081 : S\u2081 , ... , l\u2099 : S\u2099 }     -- selection (internal choice)
     |  ( S\u2081 || S\u2082 )                      -- parallel
     |  rec X . S                          -- recursion
     |  X                                  -- variable
     |  end                                -- terminated
     |  S\u2081 . S\u2082                            -- sequencing (sugar)`;
  conformanceValidCode = `@Session("authenticate . +{OK: dashboard . end, DENIED: end}")
class Auth {
    enum Result { OK, DENIED }
    Result authenticate() { ... }  // Correct: covers {OK, DENIED}
}`;
  conformanceErrorCode = `@Session("authenticate . +{OK: dashboard . end, DENIED: end}")
class Auth {
    enum Result { OK }  // Error: missing DENIED
    Result authenticate() { ... }
}`;
  conformanceBoolCode = `@Session("hasNext . +{TRUE: next . end, FALSE: end}")
class Iterator {
    boolean hasNext() { ... }  // Correct: boolean covers {TRUE, FALSE}
}`;
  threadSafetyCode = `@Session("(write . end || read . end)")
class File {
    @Exclusive void write() { ... }
    @ReadOnly String read() { ... }  // Error: read || write not safe
}`;
  static \u0275fac = function PipelineComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _PipelineComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _PipelineComponent, selectors: [["app-pipeline"]], decls: 284, vars: 9, consts: [[1, "page-header"], [1, "stats-line"], [1, "pipeline-diagram"], [1, "intro"], ["id", "parse", 1, "doc-section"], ["label", "Grammar", 3, "code"], [1, "table-container"], [1, "error-table"], ["id", "termination", 1, "doc-section"], [1, "example-card"], ["id", "wf-par", 1, "doc-section"], ["id", "statespace", 1, "doc-section"], [3, "routerLink", "queryParams"], ["id", "conformance", 1, "doc-section"], ["label", "Java", 3, "code"], ["id", "lattice", 1, "doc-section"], ["id", "thread-safety", 1, "doc-section"], [1, "analyzer-link"], ["routerLink", "/tools/analyzer"], [1, "pipeline-stage"], [1, "stage-num"], [1, "stage-name"], [1, "pipeline-arrow"]], template: function PipelineComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "header", 0)(1, "h1");
      \u0275\u0275text(2, "The Verification Pipeline");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p", 1);
      \u0275\u0275text(4, " 7 stages \xB7 Parse \u2192 Termination \u2192 WF-Par \u2192 State Space \u2192 Conformance \u2192 Lattice \u2192 Thread Safety ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(5, "div", 2);
      \u0275\u0275repeaterCreate(6, PipelineComponent_For_7_Template, 6, 3, null, null, _forTrack0);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(8, "p", 3);
      \u0275\u0275text(9, " Every ");
      \u0275\u0275elementStart(10, "code");
      \u0275\u0275text(11, "@Session");
      \u0275\u0275elementEnd();
      \u0275\u0275text(12, " annotation goes through a 7-stage verification pipeline at compile time. Each stage can reject the protocol with a specific error, and the pipeline stops at the first failure. This fail-fast design means errors are reported at the earliest possible stage, with the most relevant context. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(13, "section", 4)(14, "h2");
      \u0275\u0275text(15, "Stage 1: Parse");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(16, "p");
      \u0275\u0275text(17, " The parser transforms a session type string into an abstract syntax tree (AST). It uses recursive descent with a tokenizer that supports both ASCII and Unicode notation. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(18, "h3");
      \u0275\u0275text(19, "Grammar");
      \u0275\u0275elementEnd();
      \u0275\u0275element(20, "app-code-block", 5);
      \u0275\u0275elementStart(21, "h3");
      \u0275\u0275text(22, "Parse Error Catalogue");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(23, "div", 6)(24, "table", 7)(25, "thead")(26, "tr")(27, "th");
      \u0275\u0275text(28, "Category");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(29, "th");
      \u0275\u0275text(30, "Example Input");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(31, "th");
      \u0275\u0275text(32, "Error");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(33, "tbody")(34, "tr")(35, "td");
      \u0275\u0275text(36, "Empty input");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(37, "td")(38, "code");
      \u0275\u0275text(39, '""');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(40, "td");
      \u0275\u0275text(41, "Unexpected end of input");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(42, "tr")(43, "td");
      \u0275\u0275text(44, "Unknown token");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(45, "td")(46, "code");
      \u0275\u0275text(47, '"@#$"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(48, "td");
      \u0275\u0275text(49, "Unexpected character '@'");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(50, "tr")(51, "td");
      \u0275\u0275text(52, "Missing brace (branch)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(53, "td")(54, "code");
      \u0275\u0275text(55, '"&{read: end"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(56, "td");
      \u0275\u0275text(57, "Expected '}' to close branch");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(58, "tr")(59, "td");
      \u0275\u0275text(60, "Missing brace (select)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(61, "td")(62, "code");
      \u0275\u0275text(63, '"+{OK: end"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(64, "td");
      \u0275\u0275text(65, "Expected '}' to close selection");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(66, "tr")(67, "td");
      \u0275\u0275text(68, "Missing colon");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(69, "td")(70, "code");
      \u0275\u0275text(71, '"&{read end}"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(72, "td");
      \u0275\u0275text(73, "Expected ':' after label");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(74, "tr")(75, "td");
      \u0275\u0275text(76, "Empty branch/select");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(77, "td")(78, "code");
      \u0275\u0275text(79, '"&{}"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(80, "td");
      \u0275\u0275text(81, "Expected label in branch");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(82, "tr")(83, "td");
      \u0275\u0275text(84, "Missing rec variable");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(85, "td")(86, "code");
      \u0275\u0275text(87, '"rec . end"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(88, "td");
      \u0275\u0275text(89, "Expected variable after 'rec'");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(90, "tr")(91, "td");
      \u0275\u0275text(92, "Missing rec dot");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(93, "td")(94, "code");
      \u0275\u0275text(95, '"rec X end"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(96, "td");
      \u0275\u0275text(97, "Expected '.' after rec variable");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(98, "tr")(99, "td");
      \u0275\u0275text(100, "Missing rec body");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(101, "td")(102, "code");
      \u0275\u0275text(103, '"rec X ."');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(104, "td");
      \u0275\u0275text(105, "Unexpected end of input in rec body");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(106, "tr")(107, "td");
      \u0275\u0275text(108, "Unclosed parallel");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(109, "td")(110, "code");
      \u0275\u0275text(111, '"(a.end || b.end"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(112, "td");
      \u0275\u0275text(113, "Expected ')' to close parallel");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(114, "tr")(115, "td");
      \u0275\u0275text(116, "Missing parallel RHS");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(117, "td")(118, "code");
      \u0275\u0275text(119, '"(a.end ||)"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(120, "td");
      \u0275\u0275text(121, "Expected type after '||'");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(122, "tr")(123, "td");
      \u0275\u0275text(124, "Trailing tokens");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(125, "td")(126, "code");
      \u0275\u0275text(127, '"end end"');
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(128, "td");
      \u0275\u0275text(129, "Unexpected token after complete type");
      \u0275\u0275elementEnd()()()()()();
      \u0275\u0275elementStart(130, "section", 8)(131, "h2");
      \u0275\u0275text(132, "Stage 2: Termination Check");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(133, "p");
      \u0275\u0275text(134, " Every recursive type must have an exit path that does not pass through the bound variable. This ensures all protocols eventually terminate. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(135, "div", 9)(136, "h4");
      \u0275\u0275text(137, "Terminating (valid)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(138, "code");
      \u0275\u0275text(139, "rec X . &{next: X, stop: end}");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(140, "p");
      \u0275\u0275text(141, "The ");
      \u0275\u0275elementStart(142, "code");
      \u0275\u0275text(143, "stop");
      \u0275\u0275elementEnd();
      \u0275\u0275text(144, " branch reaches ");
      \u0275\u0275elementStart(145, "code");
      \u0275\u0275text(146, "end");
      \u0275\u0275elementEnd();
      \u0275\u0275text(147, " without going through ");
      \u0275\u0275elementStart(148, "code");
      \u0275\u0275text(149, "X");
      \u0275\u0275elementEnd();
      \u0275\u0275text(150, ".");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(151, "div", 9)(152, "h4");
      \u0275\u0275text(153, "Non-terminating (rejected)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(154, "code");
      \u0275\u0275text(155, "rec X . X");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(156, "p");
      \u0275\u0275text(157, "No exit path \u2014 ");
      \u0275\u0275elementStart(158, "code");
      \u0275\u0275text(159, "X");
      \u0275\u0275elementEnd();
      \u0275\u0275text(160, " is the only continuation.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(161, "div", 9)(162, "h4");
      \u0275\u0275text(163, "Non-terminating through branch");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(164, "code");
      \u0275\u0275text(165, "rec X . &{loop: X}");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(166, "p");
      \u0275\u0275text(167, "Every branch leads back to ");
      \u0275\u0275elementStart(168, "code");
      \u0275\u0275text(169, "X");
      \u0275\u0275elementEnd();
      \u0275\u0275text(170, " \u2014 no escape.");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(171, "section", 10)(172, "h2");
      \u0275\u0275text(173, "Stage 3: WF-Par (Well-Formed Parallel)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(174, "p");
      \u0275\u0275text(175, " The parallel constructor ");
      \u0275\u0275elementStart(176, "code");
      \u0275\u0275text(177, "(S1 || S2)");
      \u0275\u0275elementEnd();
      \u0275\u0275text(178, " has three well-formedness rules that ensure the product construction is sound: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(179, "div", 9)(180, "h4");
      \u0275\u0275text(181, "Rule 1: Both branches must terminate");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(182, "code");
      \u0275\u0275text(183, "(rec X . X || a.end)");
      \u0275\u0275elementEnd();
      \u0275\u0275text(184, " \u2014 ");
      \u0275\u0275elementStart(185, "strong");
      \u0275\u0275text(186, "rejected");
      \u0275\u0275elementEnd();
      \u0275\u0275text(187, ": left branch does not terminate. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(188, "div", 9)(189, "h4");
      \u0275\u0275text(190, "Rule 2: No shared recursion variables");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(191, "code");
      \u0275\u0275text(192, "rec X . (X || a.end)");
      \u0275\u0275elementEnd();
      \u0275\u0275text(193, " \u2014 ");
      \u0275\u0275elementStart(194, "strong");
      \u0275\u0275text(195, "rejected");
      \u0275\u0275elementEnd();
      \u0275\u0275text(196, ": ");
      \u0275\u0275elementStart(197, "code");
      \u0275\u0275text(198, "X");
      \u0275\u0275elementEnd();
      \u0275\u0275text(199, " is free in the left branch but bound outside. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(200, "div", 9)(201, "h4");
      \u0275\u0275text(202, "Rule 3: No nested parallel");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(203, "code");
      \u0275\u0275text(204, "((a.end || b.end) || c.end)");
      \u0275\u0275elementEnd();
      \u0275\u0275text(205, " \u2014 ");
      \u0275\u0275elementStart(206, "strong");
      \u0275\u0275text(207, "rejected");
      \u0275\u0275elementEnd();
      \u0275\u0275text(208, ": parallel inside parallel. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(209, "section", 11)(210, "h2");
      \u0275\u0275text(211, "Stage 4: State Space Construction");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(212, "p");
      \u0275\u0275text(213, " The AST is compiled into a finite state machine. States are integer IDs, transitions are labeled edges. The construction handles recursion (via placeholder + merge), sequencing (bottom-to-top chaining), and parallel composition (via product construction). ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(214, "p")(215, "strong");
      \u0275\u0275text(216, "Example:");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(217, "code");
      \u0275\u0275text(218, "open . &{read: close . end, write: close . end}");
      \u0275\u0275elementEnd();
      \u0275\u0275text(219, " produces 4 states and 4 transitions. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(220, "p")(221, "a", 12);
      \u0275\u0275text(222, " Try it in the analyzer \u2192 ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(223, "section", 13)(224, "h2");
      \u0275\u0275text(225, "Stage 5: Object Conformance");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(226, "p");
      \u0275\u0275text(227, " When a method ");
      \u0275\u0275elementStart(228, "code");
      \u0275\u0275text(229, "m");
      \u0275\u0275elementEnd();
      \u0275\u0275text(230, " is followed by a selection ");
      \u0275\u0275elementStart(231, "code");
      \u0275\u0275text(232, "+{OP1: S1, OP2: S2}");
      \u0275\u0275elementEnd();
      \u0275\u0275text(233, ", the method must return a type whose values cover all selection labels. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(234, "div", 9)(235, "h4");
      \u0275\u0275text(236, "Valid: enum covers all labels");
      \u0275\u0275elementEnd();
      \u0275\u0275element(237, "app-code-block", 14);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(238, "div", 9)(239, "h4");
      \u0275\u0275text(240, "Error: missing label");
      \u0275\u0275elementEnd();
      \u0275\u0275element(241, "app-code-block", 14);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(242, "div", 9)(243, "h4");
      \u0275\u0275text(244, "Valid: boolean for two labels");
      \u0275\u0275elementEnd();
      \u0275\u0275element(245, "app-code-block", 14);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(246, "section", 15)(247, "h2");
      \u0275\u0275text(248, "Stage 6: Lattice Check");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(249, "p");
      \u0275\u0275text(250, " The state space, ordered by reachability, must form a lattice. This means every pair of states has a unique least upper bound (join) and greatest lower bound (meet). The lattice structure is ");
      \u0275\u0275elementStart(251, "em");
      \u0275\u0275text(252, "necessary");
      \u0275\u0275elementEnd();
      \u0275\u0275text(253, " for the parallel constructor: products of lattices are lattices. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(254, "p");
      \u0275\u0275text(255, " The checker quotients by strongly connected components (from recursive types), then checks all pairwise meets and joins on the quotient DAG. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(256, "section", 16)(257, "h2");
      \u0275\u0275text(258, "Stage 7: Thread Safety");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(259, "p");
      \u0275\u0275text(260, " When the session type uses ");
      \u0275\u0275elementStart(261, "code");
      \u0275\u0275text(262, "||");
      \u0275\u0275elementEnd();
      \u0275\u0275text(263, ", methods from different parallel branches may execute concurrently. Each method is classified on the concurrency lattice: ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(264, "ul")(265, "li")(266, "strong");
      \u0275\u0275text(267, "@Shared");
      \u0275\u0275elementEnd();
      \u0275\u0275text(268, " \u2014 safe for any concurrent access");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(269, "li")(270, "strong");
      \u0275\u0275text(271, "@ReadOnly");
      \u0275\u0275elementEnd();
      \u0275\u0275text(272, " \u2014 safe with other reads, not with writes");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(273, "li")(274, "strong");
      \u0275\u0275text(275, "@Exclusive");
      \u0275\u0275elementEnd();
      \u0275\u0275text(276, " \u2014 must not be concurrent with anything");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(277, "div", 9)(278, "h4");
      \u0275\u0275text(279, "Error: exclusive methods in parallel");
      \u0275\u0275elementEnd();
      \u0275\u0275element(280, "app-code-block", 14);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(281, "p", 17)(282, "a", 18);
      \u0275\u0275text(283, "Try the interactive analyzer \u2192");
      \u0275\u0275elementEnd()();
    }
    if (rf & 2) {
      \u0275\u0275advance(6);
      \u0275\u0275repeater(ctx.stages);
      \u0275\u0275advance(14);
      \u0275\u0275property("code", ctx.grammarCode);
      \u0275\u0275advance(201);
      \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(7, _c0))("queryParams", \u0275\u0275pureFunction0(8, _c1));
      \u0275\u0275advance(16);
      \u0275\u0275property("code", ctx.conformanceValidCode);
      \u0275\u0275advance(4);
      \u0275\u0275property("code", ctx.conformanceErrorCode);
      \u0275\u0275advance(4);
      \u0275\u0275property("code", ctx.conformanceBoolCode);
      \u0275\u0275advance(35);
      \u0275\u0275property("code", ctx.threadSafetyCode);
    }
  }, dependencies: [RouterLink, CodeBlockComponent], styles: ["\n\n.page-header[_ngcontent-%COMP%] {\n  padding: 24px 0 16px;\n}\n.page-header[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.stats-line[_ngcontent-%COMP%] {\n  text-align: center;\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.6);\n  padding: 8px 0 16px;\n}\n.intro[_ngcontent-%COMP%] {\n  line-height: 1.7;\n  margin-bottom: 32px;\n}\n.pipeline-diagram[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  gap: 8px;\n  flex-wrap: wrap;\n  padding: 24px 0;\n  margin-bottom: 24px;\n}\n.pipeline-stage[_ngcontent-%COMP%] {\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  padding: 12px 16px;\n  border: 2px solid var(--brand-primary, #4338ca);\n  border-radius: 8px;\n  background: rgba(67, 56, 202, 0.05);\n  min-width: 80px;\n}\n.stage-num[_ngcontent-%COMP%] {\n  font-size: 12px;\n  font-weight: 600;\n  color: var(--brand-primary, #4338ca);\n}\n.stage-name[_ngcontent-%COMP%] {\n  font-size: 14px;\n  font-weight: 500;\n}\n.pipeline-arrow[_ngcontent-%COMP%] {\n  font-size: 20px;\n  color: rgba(0, 0, 0, 0.4);\n}\n.doc-section[_ngcontent-%COMP%] {\n  margin: 32px 0;\n}\n.doc-section[_ngcontent-%COMP%]   h2[_ngcontent-%COMP%] {\n  font-size: 20px;\n  font-weight: 600;\n  margin-bottom: 12px;\n}\n.doc-section[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 16px;\n  font-weight: 500;\n  margin: 16px 0 8px;\n}\n.doc-section[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.7;\n  margin-bottom: 12px;\n}\n.doc-section[_ngcontent-%COMP%]   ul[_ngcontent-%COMP%] {\n  line-height: 1.8;\n}\n.example-card[_ngcontent-%COMP%] {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px;\n  margin: 12px 0;\n  background: rgba(0, 0, 0, 0.01);\n}\n.example-card[_ngcontent-%COMP%]   h4[_ngcontent-%COMP%] {\n  font-size: 14px;\n  font-weight: 500;\n  margin: 0 0 8px;\n}\n.example-card[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  margin: 8px 0 0;\n  font-size: 14px;\n}\n.table-container[_ngcontent-%COMP%] {\n  overflow-x: auto;\n}\n.error-table[_ngcontent-%COMP%] {\n  width: 100%;\n  border-collapse: collapse;\n  font-size: 14px;\n}\n.error-table[_ngcontent-%COMP%]   th[_ngcontent-%COMP%] {\n  text-align: left;\n  padding: 10px 16px;\n  font-weight: 500;\n  border-bottom: 2px solid rgba(0, 0, 0, 0.12);\n}\n.error-table[_ngcontent-%COMP%]   td[_ngcontent-%COMP%] {\n  padding: 8px 16px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n}\n.analyzer-link[_ngcontent-%COMP%] {\n  text-align: center;\n  padding: 32px 0;\n  font-size: 16px;\n}\n.analyzer-link[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.analyzer-link[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n/*# sourceMappingURL=pipeline.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(PipelineComponent, [{
    type: Component,
    args: [{ selector: "app-pipeline", standalone: true, imports: [RouterLink, CodeBlockComponent], template: `
    <header class="page-header">
      <h1>The Verification Pipeline</h1>
      <p class="stats-line">
        7 stages &middot; Parse &rarr; Termination &rarr; WF-Par &rarr; State Space &rarr; Conformance &rarr; Lattice &rarr; Thread Safety
      </p>
    </header>

    <!-- Pipeline diagram -->
    <div class="pipeline-diagram">
      @for (stage of stages; track stage.num; let last = $last) {
        <div class="pipeline-stage">
          <span class="stage-num">{{ stage.num }}</span>
          <span class="stage-name">{{ stage.name }}</span>
        </div>
        @if (!last) {
          <span class="pipeline-arrow">&rarr;</span>
        }
      }
    </div>

    <p class="intro">
      Every <code>&#64;Session</code> annotation goes through a 7-stage verification pipeline
      at compile time. Each stage can reject the protocol with a specific error, and the
      pipeline stops at the first failure. This fail-fast design means errors are reported
      at the earliest possible stage, with the most relevant context.
    </p>

    <!-- Stage 1: Parse -->
    <section class="doc-section" id="parse">
      <h2>Stage 1: Parse</h2>
      <p>
        The parser transforms a session type string into an abstract syntax tree (AST).
        It uses recursive descent with a tokenizer that supports both ASCII and Unicode
        notation.
      </p>
      <h3>Grammar</h3>
      <app-code-block [code]="grammarCode" label="Grammar"></app-code-block>

      <h3>Parse Error Catalogue</h3>
      <div class="table-container">
        <table class="error-table">
          <thead>
            <tr><th>Category</th><th>Example Input</th><th>Error</th></tr>
          </thead>
          <tbody>
            <tr><td>Empty input</td><td><code>""</code></td><td>Unexpected end of input</td></tr>
            <tr><td>Unknown token</td><td><code>"&#64;#$"</code></td><td>Unexpected character '&#64;'</td></tr>
            <tr><td>Missing brace (branch)</td><td><code>"&amp;&#123;read: end"</code></td><td>Expected '&#125;' to close branch</td></tr>
            <tr><td>Missing brace (select)</td><td><code>"+&#123;OK: end"</code></td><td>Expected '&#125;' to close selection</td></tr>
            <tr><td>Missing colon</td><td><code>"&amp;&#123;read end&#125;"</code></td><td>Expected ':' after label</td></tr>
            <tr><td>Empty branch/select</td><td><code>"&amp;&#123;&#125;"</code></td><td>Expected label in branch</td></tr>
            <tr><td>Missing rec variable</td><td><code>"rec . end"</code></td><td>Expected variable after 'rec'</td></tr>
            <tr><td>Missing rec dot</td><td><code>"rec X end"</code></td><td>Expected '.' after rec variable</td></tr>
            <tr><td>Missing rec body</td><td><code>"rec X ."</code></td><td>Unexpected end of input in rec body</td></tr>
            <tr><td>Unclosed parallel</td><td><code>"(a.end || b.end"</code></td><td>Expected ')' to close parallel</td></tr>
            <tr><td>Missing parallel RHS</td><td><code>"(a.end ||)"</code></td><td>Expected type after '||'</td></tr>
            <tr><td>Trailing tokens</td><td><code>"end end"</code></td><td>Unexpected token after complete type</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Stage 2: Termination -->
    <section class="doc-section" id="termination">
      <h2>Stage 2: Termination Check</h2>
      <p>
        Every recursive type must have an exit path that does not pass through the
        bound variable. This ensures all protocols eventually terminate.
      </p>
      <div class="example-card">
        <h4>Terminating (valid)</h4>
        <code>rec X . &amp;&#123;next: X, stop: end&#125;</code>
        <p>The <code>stop</code> branch reaches <code>end</code> without going through <code>X</code>.</p>
      </div>
      <div class="example-card">
        <h4>Non-terminating (rejected)</h4>
        <code>rec X . X</code>
        <p>No exit path &mdash; <code>X</code> is the only continuation.</p>
      </div>
      <div class="example-card">
        <h4>Non-terminating through branch</h4>
        <code>rec X . &amp;&#123;loop: X&#125;</code>
        <p>Every branch leads back to <code>X</code> &mdash; no escape.</p>
      </div>
    </section>

    <!-- Stage 3: WF-Par -->
    <section class="doc-section" id="wf-par">
      <h2>Stage 3: WF-Par (Well-Formed Parallel)</h2>
      <p>
        The parallel constructor <code>(S1 || S2)</code> has three well-formedness rules
        that ensure the product construction is sound:
      </p>
      <div class="example-card">
        <h4>Rule 1: Both branches must terminate</h4>
        <code>(rec X . X || a.end)</code> &mdash; <strong>rejected</strong>: left branch does not terminate.
      </div>
      <div class="example-card">
        <h4>Rule 2: No shared recursion variables</h4>
        <code>rec X . (X || a.end)</code> &mdash; <strong>rejected</strong>: <code>X</code> is free in the left branch but bound outside.
      </div>
      <div class="example-card">
        <h4>Rule 3: No nested parallel</h4>
        <code>((a.end || b.end) || c.end)</code> &mdash; <strong>rejected</strong>: parallel inside parallel.
      </div>
    </section>

    <!-- Stage 4: State Space -->
    <section class="doc-section" id="statespace">
      <h2>Stage 4: State Space Construction</h2>
      <p>
        The AST is compiled into a finite state machine. States are integer IDs,
        transitions are labeled edges. The construction handles recursion (via
        placeholder + merge), sequencing (bottom-to-top chaining), and parallel
        composition (via product construction).
      </p>
      <p>
        <strong>Example:</strong> <code>open . &amp;&#123;read: close . end, write: close . end&#125;</code>
        produces 4 states and 4 transitions.
      </p>
      <p>
        <a [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'open . &{read: close . end, write: close . end}'}">
          Try it in the analyzer &rarr;
        </a>
      </p>
    </section>

    <!-- Stage 5: Conformance -->
    <section class="doc-section" id="conformance">
      <h2>Stage 5: Object Conformance</h2>
      <p>
        When a method <code>m</code> is followed by a selection <code>+&#123;OP1: S1, OP2: S2&#125;</code>,
        the method must return a type whose values cover all selection labels.
      </p>
      <div class="example-card">
        <h4>Valid: enum covers all labels</h4>
        <app-code-block [code]="conformanceValidCode" label="Java"></app-code-block>
      </div>
      <div class="example-card">
        <h4>Error: missing label</h4>
        <app-code-block [code]="conformanceErrorCode" label="Java"></app-code-block>
      </div>
      <div class="example-card">
        <h4>Valid: boolean for two labels</h4>
        <app-code-block [code]="conformanceBoolCode" label="Java"></app-code-block>
      </div>
    </section>

    <!-- Stage 6: Lattice -->
    <section class="doc-section" id="lattice">
      <h2>Stage 6: Lattice Check</h2>
      <p>
        The state space, ordered by reachability, must form a lattice. This means
        every pair of states has a unique least upper bound (join) and greatest lower
        bound (meet). The lattice structure is <em>necessary</em> for the parallel
        constructor: products of lattices are lattices.
      </p>
      <p>
        The checker quotients by strongly connected components (from recursive types),
        then checks all pairwise meets and joins on the quotient DAG.
      </p>
    </section>

    <!-- Stage 7: Thread Safety -->
    <section class="doc-section" id="thread-safety">
      <h2>Stage 7: Thread Safety</h2>
      <p>
        When the session type uses <code>||</code>, methods from different parallel
        branches may execute concurrently. Each method is classified on the concurrency
        lattice:
      </p>
      <ul>
        <li><strong>&#64;Shared</strong> &mdash; safe for any concurrent access</li>
        <li><strong>&#64;ReadOnly</strong> &mdash; safe with other reads, not with writes</li>
        <li><strong>&#64;Exclusive</strong> &mdash; must not be concurrent with anything</li>
      </ul>
      <div class="example-card">
        <h4>Error: exclusive methods in parallel</h4>
        <app-code-block [code]="threadSafetyCode" label="Java"></app-code-block>
      </div>
    </section>

    <p class="analyzer-link">
      <a routerLink="/tools/analyzer">Try the interactive analyzer &rarr;</a>
    </p>
  `, styles: ["/* angular:styles/component:scss;53a348337fccfbd0038fa98a6397737839b47bd616bc3ab56e5a1c96df2fe763;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/pipeline/pipeline.component.ts */\n.page-header {\n  padding: 24px 0 16px;\n}\n.page-header h1 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.stats-line {\n  text-align: center;\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.6);\n  padding: 8px 0 16px;\n}\n.intro {\n  line-height: 1.7;\n  margin-bottom: 32px;\n}\n.pipeline-diagram {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  gap: 8px;\n  flex-wrap: wrap;\n  padding: 24px 0;\n  margin-bottom: 24px;\n}\n.pipeline-stage {\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  padding: 12px 16px;\n  border: 2px solid var(--brand-primary, #4338ca);\n  border-radius: 8px;\n  background: rgba(67, 56, 202, 0.05);\n  min-width: 80px;\n}\n.stage-num {\n  font-size: 12px;\n  font-weight: 600;\n  color: var(--brand-primary, #4338ca);\n}\n.stage-name {\n  font-size: 14px;\n  font-weight: 500;\n}\n.pipeline-arrow {\n  font-size: 20px;\n  color: rgba(0, 0, 0, 0.4);\n}\n.doc-section {\n  margin: 32px 0;\n}\n.doc-section h2 {\n  font-size: 20px;\n  font-weight: 600;\n  margin-bottom: 12px;\n}\n.doc-section h3 {\n  font-size: 16px;\n  font-weight: 500;\n  margin: 16px 0 8px;\n}\n.doc-section p {\n  line-height: 1.7;\n  margin-bottom: 12px;\n}\n.doc-section ul {\n  line-height: 1.8;\n}\n.example-card {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px;\n  margin: 12px 0;\n  background: rgba(0, 0, 0, 0.01);\n}\n.example-card h4 {\n  font-size: 14px;\n  font-weight: 500;\n  margin: 0 0 8px;\n}\n.example-card p {\n  margin: 8px 0 0;\n  font-size: 14px;\n}\n.table-container {\n  overflow-x: auto;\n}\n.error-table {\n  width: 100%;\n  border-collapse: collapse;\n  font-size: 14px;\n}\n.error-table th {\n  text-align: left;\n  padding: 10px 16px;\n  font-weight: 500;\n  border-bottom: 2px solid rgba(0, 0, 0, 0.12);\n}\n.error-table td {\n  padding: 8px 16px;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n}\n.analyzer-link {\n  text-align: center;\n  padding: 32px 0;\n  font-size: 16px;\n}\n.analyzer-link a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.analyzer-link a:hover {\n  text-decoration: underline;\n}\n/*# sourceMappingURL=pipeline.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(PipelineComponent, { className: "PipelineComponent", filePath: "src/app/pages/pipeline/pipeline.component.ts", lineNumber: 321 });
})();
export {
  PipelineComponent
};
//# sourceMappingURL=chunk-UHLWZBE7.js.map
