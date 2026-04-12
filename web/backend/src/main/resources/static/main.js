import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardModule,
  MatCardTitle
} from "./chunk-SHRTRSL7.js";
import {
  DefaultValueAccessor,
  FormsModule,
  NgControlStatus,
  NgControlStatusGroup,
  NgForm,
  NgModel,
  ɵNgNoValidate
} from "./chunk-2AQDFUQH.js";
import {
  MatProgressSpinner,
  MatProgressSpinnerModule
} from "./chunk-KSWLVI2B.js";
import {
  ApiService
} from "./chunk-EOCOQ6DB.js";
import {
  MatButton,
  MatButtonModule,
  MatIconButton
} from "./chunk-BUK7DMBP.js";
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
  provideRouter
} from "./chunk-QTYX35EO.js";
import {
  MatIcon,
  MatIconModule,
  Platform
} from "./chunk-BFW3NWZD.js";
import {
  DomRendererFactory2,
  bootstrapApplication,
  provideHttpClient
} from "./chunk-ZG4TCI7P.js";
import {
  BidiModule
} from "./chunk-NL2TMNRB.js";
import {
  ANIMATION_MODULE_TYPE,
  ChangeDetectionScheduler,
  ChangeDetectionStrategy,
  Component,
  ContentChildren,
  DOCUMENT,
  Directive,
  ElementRef,
  EventEmitter,
  Injectable,
  InjectionToken,
  Injector,
  Input,
  NgModule,
  NgZone,
  Output,
  RendererFactory2,
  RuntimeError,
  ViewEncapsulation,
  filter,
  inject,
  makeEnvironmentProviders,
  performanceMarkFeature,
  provideBrowserGlobalErrorListeners,
  setClassMetadata,
  signal,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵclassMap,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵcontentQuery,
  ɵɵdefineComponent,
  ɵɵdefineDirective,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵdomElementEnd,
  ɵɵdomElementStart,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵinvalidFactory,
  ɵɵlistener,
  ɵɵloadQuery,
  ɵɵnamespaceHTML,
  ɵɵnamespaceSVG,
  ɵɵnextContext,
  ɵɵprojection,
  ɵɵprojectionDef,
  ɵɵproperty,
  ɵɵpureFunction0,
  ɵɵqueryRefresh,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtwoWayBindingSet,
  ɵɵtwoWayListener,
  ɵɵtwoWayProperty
} from "./chunk-OWEA7TR3.js";

// node_modules/@angular/platform-browser/fesm2022/animations-async.mjs
var ANIMATION_PREFIX = "@";
var AsyncAnimationRendererFactory = class _AsyncAnimationRendererFactory {
  doc;
  delegate;
  zone;
  animationType;
  moduleImpl;
  _rendererFactoryPromise = null;
  scheduler = null;
  injector = inject(Injector);
  loadingSchedulerFn = inject(\u0275ASYNC_ANIMATION_LOADING_SCHEDULER_FN, {
    optional: true
  });
  _engine;
  constructor(doc, delegate, zone, animationType, moduleImpl) {
    this.doc = doc;
    this.delegate = delegate;
    this.zone = zone;
    this.animationType = animationType;
    this.moduleImpl = moduleImpl;
  }
  ngOnDestroy() {
    this._engine?.flush();
  }
  loadImpl() {
    const loadFn = () => this.moduleImpl ?? import("./chunk-JGTBOWKO.js").then((m) => m);
    let moduleImplPromise;
    if (this.loadingSchedulerFn) {
      moduleImplPromise = this.loadingSchedulerFn(loadFn);
    } else {
      moduleImplPromise = loadFn();
    }
    return moduleImplPromise.catch((e) => {
      throw new RuntimeError(5300, (typeof ngDevMode === "undefined" || ngDevMode) && "Async loading for animations package was enabled, but loading failed. Angular falls back to using regular rendering. No animations will be displayed and their styles won't be applied.");
    }).then(({
      \u0275createEngine,
      \u0275AnimationRendererFactory
    }) => {
      this._engine = \u0275createEngine(this.animationType, this.doc);
      const rendererFactory = new \u0275AnimationRendererFactory(this.delegate, this._engine, this.zone);
      this.delegate = rendererFactory;
      return rendererFactory;
    });
  }
  createRenderer(hostElement, rendererType) {
    const renderer = this.delegate.createRenderer(hostElement, rendererType);
    if (renderer.\u0275type === 0) {
      return renderer;
    }
    if (typeof renderer.throwOnSyntheticProps === "boolean") {
      renderer.throwOnSyntheticProps = false;
    }
    const dynamicRenderer = new DynamicDelegationRenderer(renderer);
    if (rendererType?.data?.["animation"] && !this._rendererFactoryPromise) {
      this._rendererFactoryPromise = this.loadImpl();
    }
    this._rendererFactoryPromise?.then((animationRendererFactory) => {
      const animationRenderer = animationRendererFactory.createRenderer(hostElement, rendererType);
      dynamicRenderer.use(animationRenderer);
      this.scheduler ??= this.injector.get(ChangeDetectionScheduler, null, {
        optional: true
      });
      this.scheduler?.notify(10);
    }).catch((e) => {
      dynamicRenderer.use(renderer);
    });
    return dynamicRenderer;
  }
  begin() {
    this.delegate.begin?.();
  }
  end() {
    this.delegate.end?.();
  }
  whenRenderingDone() {
    return this.delegate.whenRenderingDone?.() ?? Promise.resolve();
  }
  componentReplaced(componentId) {
    this._engine?.flush();
    this.delegate.componentReplaced?.(componentId);
  }
  static \u0275fac = function AsyncAnimationRendererFactory_Factory(__ngFactoryType__) {
    \u0275\u0275invalidFactory();
  };
  static \u0275prov = /* @__PURE__ */ \u0275\u0275defineInjectable({
    token: _AsyncAnimationRendererFactory,
    factory: _AsyncAnimationRendererFactory.\u0275fac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(AsyncAnimationRendererFactory, [{
    type: Injectable
  }], () => [{
    type: Document
  }, {
    type: RendererFactory2
  }, {
    type: NgZone
  }, {
    type: void 0
  }, {
    type: Promise
  }], null);
})();
var DynamicDelegationRenderer = class {
  delegate;
  replay = [];
  \u0275type = 1;
  constructor(delegate) {
    this.delegate = delegate;
  }
  use(impl) {
    this.delegate = impl;
    if (this.replay !== null) {
      for (const fn of this.replay) {
        fn(impl);
      }
      this.replay = null;
    }
  }
  get data() {
    return this.delegate.data;
  }
  destroy() {
    this.replay = null;
    this.delegate.destroy();
  }
  createElement(name, namespace) {
    return this.delegate.createElement(name, namespace);
  }
  createComment(value) {
    return this.delegate.createComment(value);
  }
  createText(value) {
    return this.delegate.createText(value);
  }
  get destroyNode() {
    return this.delegate.destroyNode;
  }
  appendChild(parent, newChild) {
    this.delegate.appendChild(parent, newChild);
  }
  insertBefore(parent, newChild, refChild, isMove) {
    this.delegate.insertBefore(parent, newChild, refChild, isMove);
  }
  removeChild(parent, oldChild, isHostElement, requireSynchronousElementRemoval) {
    this.delegate.removeChild(parent, oldChild, isHostElement, requireSynchronousElementRemoval);
  }
  selectRootElement(selectorOrNode, preserveContent) {
    return this.delegate.selectRootElement(selectorOrNode, preserveContent);
  }
  parentNode(node) {
    return this.delegate.parentNode(node);
  }
  nextSibling(node) {
    return this.delegate.nextSibling(node);
  }
  setAttribute(el, name, value, namespace) {
    this.delegate.setAttribute(el, name, value, namespace);
  }
  removeAttribute(el, name, namespace) {
    this.delegate.removeAttribute(el, name, namespace);
  }
  addClass(el, name) {
    this.delegate.addClass(el, name);
  }
  removeClass(el, name) {
    this.delegate.removeClass(el, name);
  }
  setStyle(el, style, value, flags) {
    this.delegate.setStyle(el, style, value, flags);
  }
  removeStyle(el, style, flags) {
    this.delegate.removeStyle(el, style, flags);
  }
  setProperty(el, name, value) {
    if (this.shouldReplay(name)) {
      this.replay.push((renderer) => renderer.setProperty(el, name, value));
    }
    this.delegate.setProperty(el, name, value);
  }
  setValue(node, value) {
    this.delegate.setValue(node, value);
  }
  listen(target, eventName, callback, options) {
    if (this.shouldReplay(eventName)) {
      this.replay.push((renderer) => renderer.listen(target, eventName, callback, options));
    }
    return this.delegate.listen(target, eventName, callback, options);
  }
  shouldReplay(propOrEventName) {
    return this.replay !== null && propOrEventName.startsWith(ANIMATION_PREFIX);
  }
};
var \u0275ASYNC_ANIMATION_LOADING_SCHEDULER_FN = new InjectionToken(typeof ngDevMode !== "undefined" && ngDevMode ? "async_animation_loading_scheduler_fn" : "");
function provideAnimationsAsync(type = "animations") {
  performanceMarkFeature("NgAsyncAnimations");
  if (false) {
    type = "noop";
  }
  return makeEnvironmentProviders([{
    provide: RendererFactory2,
    useFactory: () => {
      return new AsyncAnimationRendererFactory(inject(DOCUMENT), inject(DomRendererFactory2), inject(NgZone), type);
    }
  }, {
    provide: ANIMATION_MODULE_TYPE,
    useValue: type === "noop" ? "NoopAnimations" : "BrowserAnimations"
  }]);
}

// src/app/pages/home/home.component.ts
var _c0 = () => ["/tools/analyzer"];
var _c1 = () => ({ type: "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}" });
var _c2 = () => ({ type: "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}" });
var _c3 = () => ({ type: "initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})" });
var _c4 = () => ({ type: "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})" });
function HomeComponent_Conditional_93_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275element(0, "mat-spinner", 60);
  }
}
function HomeComponent_Conditional_94_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 91)(1, "span", 92);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "span", 93);
    \u0275\u0275text(4, "Benchmarks");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(5, "div", 91)(6, "span", 92);
    \u0275\u0275text(7);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(8, "span", 93);
    \u0275\u0275text(9, "States analyzed");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(10, "div", 91)(11, "span", 92);
    \u0275\u0275text(12);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(13, "span", 93);
    \u0275\u0275text(14, "Tests generated");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(15, "div", 91)(16, "span", 92);
    \u0275\u0275text(17, "19");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(18, "span", 93);
    \u0275\u0275text(19, "Analysis modules");
    \u0275\u0275elementEnd()();
    \u0275\u0275elementStart(20, "div", 91)(21, "span", 92);
    \u0275\u0275text(22, "2,458");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(23, "span", 93);
    \u0275\u0275text(24, "Total tests");
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const ctx_r0 = \u0275\u0275nextContext();
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(ctx_r0.stats().numBenchmarks);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.stats().totalStates);
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(ctx_r0.stats().totalTests);
  }
}
var HomeComponent = class _HomeComponent {
  api;
  stats = signal(null, ...ngDevMode ? [{ debugName: "stats" }] : []);
  loading = signal(true, ...ngDevMode ? [{ debugName: "loading" }] : []);
  statsError = signal(false, ...ngDevMode ? [{ debugName: "statsError" }] : []);
  constructor(api) {
    this.api = api;
  }
  ngOnInit() {
    this.api.getBenchmarks().subscribe({
      next: (benchmarks) => {
        this.stats.set({
          numBenchmarks: benchmarks.length,
          totalStates: benchmarks.reduce((sum, b) => sum + b.numStates, 0),
          totalTests: benchmarks.reduce((sum, b) => sum + b.numTests, 0),
          allLattice: benchmarks.every((b) => b.isLattice)
        });
        this.loading.set(false);
      },
      error: () => {
        this.statsError.set(true);
        this.loading.set(false);
      }
    });
  }
  static \u0275fac = function HomeComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _HomeComponent)(\u0275\u0275directiveInject(ApiService));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _HomeComponent, selectors: [["app-home"]], decls: 259, vars: 17, consts: [[1, "hero"], [1, "hero-inner"], [1, "hero-text"], [1, "hero-sub"], [1, "hero-desc"], [1, "hero-cta"], ["mat-flat-button", "", "routerLink", "/tools/analyzer", 1, "hero-btn-primary"], ["mat-stroked-button", "", "routerLink", "/publications", 1, "hero-btn-outline"], [1, "hero-author"], [1, "hero-diagram"], ["xmlns", "http://www.w3.org/2000/svg", "viewBox", "0 0 340 440", 1, "hero-lattice-svg"], ["id", "ah", "markerWidth", "8", "markerHeight", "6", "refX", "8", "refY", "3", "orient", "auto"], ["points", "0 0, 8 3, 0 6", "fill", "rgba(255,255,255,0.5)"], ["stroke", "rgba(255,255,255,0.35)", "stroke-width", "1.2", "fill", "none", "marker-end", "url(#ah)", 1, "edges"], ["x1", "157", "y1", "36", "x2", "83", "y2", "102"], ["x1", "183", "y1", "36", "x2", "257", "y2", "102"], ["x1", "61", "y1", "130", "x2", "29", "y2", "188"], ["x1", "83", "y1", "126", "x2", "157", "y2", "192"], ["x1", "257", "y1", "126", "x2", "183", "y2", "192"], ["x1", "279", "y1", "130", "x2", "311", "y2", "188"], ["x1", "29", "y1", "220", "x2", "61", "y2", "278"], ["x1", "157", "y1", "216", "x2", "83", "y2", "282"], ["x1", "183", "y1", "216", "x2", "257", "y2", "282"], ["x1", "311", "y1", "220", "x2", "279", "y2", "278"], ["x1", "83", "y1", "306", "x2", "157", "y2", "372"], ["x1", "257", "y1", "306", "x2", "183", "y2", "372"], ["fill", "rgba(255,255,255,0.55)", "font-family", "Inter,sans-serif", "font-size", "11", "font-style", "italic"], ["x", "112", "y", "63"], ["x", "222", "y", "63"], ["x", "38", "y", "156"], ["x", "128", "y", "156"], ["x", "212", "y", "156"], ["x", "298", "y", "156"], ["x", "30", "y", "252"], ["x", "108", "y", "252"], ["x", "230", "y", "252"], ["x", "306", "y", "252"], ["x", "108", "y", "342"], ["x", "228", "y", "342"], ["font-family", "Inter,sans-serif", "font-size", "7", "text-anchor", "middle", "dominant-baseline", "central"], ["cx", "170", "cy", "24", "r", "18", "fill", "rgba(147,197,253,0.3)", "stroke", "rgba(191,219,254,0.8)", "stroke-width", "1.5"], ["x", "170", "y", "24", "fill", "rgba(255,255,255,0.95)"], ["cx", "70", "cy", "114", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "70", "y", "114", "fill", "rgba(255,255,255,0.9)"], ["cx", "270", "cy", "114", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "270", "y", "114", "fill", "rgba(255,255,255,0.9)"], ["cx", "20", "cy", "204", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "20", "y", "204", "fill", "rgba(255,255,255,0.9)"], ["cx", "170", "cy", "204", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "170", "y", "204", "fill", "rgba(255,255,255,0.9)"], ["cx", "320", "cy", "204", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "320", "y", "204", "fill", "rgba(255,255,255,0.9)"], ["cx", "70", "cy", "294", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "70", "y", "294", "fill", "rgba(255,255,255,0.9)"], ["cx", "270", "cy", "294", "r", "18", "fill", "rgba(255,255,255,0.1)", "stroke", "rgba(255,255,255,0.5)", "stroke-width", "1.2"], ["x", "270", "y", "294", "fill", "rgba(255,255,255,0.9)"], ["cx", "170", "cy", "384", "r", "18", "fill", "rgba(134,239,172,0.25)", "stroke", "rgba(187,247,208,0.8)", "stroke-width", "1.5"], ["x", "170", "y", "384", "fill", "rgba(255,255,255,0.95)"], ["x", "170", "y", "438", "text-anchor", "middle", "fill", "rgba(255,255,255,0.6)", "font-family", "Inter,sans-serif", "font-size", "10"], [1, "stats-bar"], ["diameter", "16"], [1, "concept"], [1, "concept-content"], [1, "concept-text"], [1, "concept-example"], [1, "code-example"], [1, "code-label"], [1, "arrow-down"], [1, "code-example", "result"], [1, "capabilities"], [1, "cap-grid"], ["routerLink", "/tools/analyzer", 1, "cap-card"], [1, "cap-icon"], ["routerLink", "/benchmarks", 1, "cap-card"], [1, "live-example"], [1, "live-desc"], [1, "example-buttons"], ["mat-stroked-button", "", 3, "routerLink", "queryParams"], [1, "publications"], [1, "pub-list"], [1, "pub-item"], [1, "pub-venue"], [1, "pub-title"], [1, "pub-authors"], ["mat-button", "", "routerLink", "/publications"], [1, "cite"], [1, "cite-box"], [1, "tools-section"], [1, "tools-grid"], ["mat-button", "", "routerLink", "/tools/analyzer"], ["mat-button", "", "routerLink", "/documentation"], [1, "stat-item"], [1, "stat-value"], [1, "stat-label"]], template: function HomeComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "section", 0)(1, "div", 1)(2, "div", 2)(3, "h1");
      \u0275\u0275text(4, "Session Types as Lattices");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(5, "p", 3);
      \u0275\u0275text(6, "Verify object protocols. Visualize state spaces. Generate tests.");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(7, "p", 4);
      \u0275\u0275text(8, " A theory and toolchain proving that session-type state spaces form ");
      \u0275\u0275elementStart(9, "strong");
      \u0275\u0275text(10, "lattices");
      \u0275\u0275elementEnd();
      \u0275\u0275text(11, " \u2014 with the parallel constructor forcing product lattice structure for concurrent access. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(12, "div", 5)(13, "a", 6);
      \u0275\u0275text(14, " Try the Analyzer ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(15, "a", 7);
      \u0275\u0275text(16, " Read the Paper ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(17, "p", 8);
      \u0275\u0275text(18, "Alexandre Zua Caldeira \u2014 Independent Researcher");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(19, "div", 9);
      \u0275\u0275namespaceSVG();
      \u0275\u0275elementStart(20, "svg", 10)(21, "defs")(22, "marker", 11);
      \u0275\u0275element(23, "polygon", 12);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(24, "g", 13);
      \u0275\u0275element(25, "line", 14)(26, "line", 15)(27, "line", 16)(28, "line", 17)(29, "line", 18)(30, "line", 19)(31, "line", 20)(32, "line", 21)(33, "line", 22)(34, "line", 23)(35, "line", 24)(36, "line", 25);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(37, "g", 26)(38, "text", 27);
      \u0275\u0275text(39, "a");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(40, "text", 28);
      \u0275\u0275text(41, "c");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(42, "text", 29);
      \u0275\u0275text(43, "b");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(44, "text", 30);
      \u0275\u0275text(45, "c");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(46, "text", 31);
      \u0275\u0275text(47, "a");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(48, "text", 32);
      \u0275\u0275text(49, "d");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(50, "text", 33);
      \u0275\u0275text(51, "c");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(52, "text", 34);
      \u0275\u0275text(53, "b");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(54, "text", 35);
      \u0275\u0275text(55, "d");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(56, "text", 36);
      \u0275\u0275text(57, "a");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(58, "text", 37);
      \u0275\u0275text(59, "d");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(60, "text", 38);
      \u0275\u0275text(61, "b");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(62, "g", 39);
      \u0275\u0275element(63, "circle", 40);
      \u0275\u0275elementStart(64, "text", 41);
      \u0275\u0275text(65, "(\u22A4\u2081, \u22A4\u2082)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(66, "circle", 42);
      \u0275\u0275elementStart(67, "text", 43);
      \u0275\u0275text(68, "(s\u2090, \u22A4\u2082)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(69, "circle", 44);
      \u0275\u0275elementStart(70, "text", 45);
      \u0275\u0275text(71, "(\u22A4\u2081, s\u1D9C)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(72, "circle", 46);
      \u0275\u0275elementStart(73, "text", 47);
      \u0275\u0275text(74, "(\u22A5\u2081, \u22A4\u2082)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(75, "circle", 48);
      \u0275\u0275elementStart(76, "text", 49);
      \u0275\u0275text(77, "(s\u2090, s\u1D9C)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(78, "circle", 50);
      \u0275\u0275elementStart(79, "text", 51);
      \u0275\u0275text(80, "(\u22A4\u2081, \u22A5\u2082)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(81, "circle", 52);
      \u0275\u0275elementStart(82, "text", 53);
      \u0275\u0275text(83, "(\u22A5\u2081, s\u1D9C)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(84, "circle", 54);
      \u0275\u0275elementStart(85, "text", 55);
      \u0275\u0275text(86, "(s\u2090, \u22A5\u2082)");
      \u0275\u0275elementEnd();
      \u0275\u0275element(87, "circle", 56);
      \u0275\u0275elementStart(88, "text", 57);
      \u0275\u0275text(89, "(\u22A5\u2081, \u22A5\u2082)");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(90, "text", 58);
      \u0275\u0275text(91, " a.b.end \u2225 c.d.end \u2014 3\xD73 product lattice ");
      \u0275\u0275elementEnd()()()()();
      \u0275\u0275namespaceHTML();
      \u0275\u0275elementStart(92, "section", 59);
      \u0275\u0275conditionalCreate(93, HomeComponent_Conditional_93_Template, 1, 0, "mat-spinner", 60)(94, HomeComponent_Conditional_94_Template, 25, 3);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(95, "section", 61)(96, "h2");
      \u0275\u0275text(97, "The Key Insight");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(98, "div", 62)(99, "div", 63)(100, "p")(101, "strong");
      \u0275\u0275text(102, "Session types");
      \u0275\u0275elementEnd();
      \u0275\u0275text(103, " describe communication protocols on objects \u2014 the legal sequences of method calls, branches, and selections. We prove that the state space of any well-formed session type, ordered by reachability, forms a ");
      \u0275\u0275elementStart(104, "strong");
      \u0275\u0275text(105, "lattice");
      \u0275\u0275elementEnd();
      \u0275\u0275text(106, " (which we call a ");
      \u0275\u0275elementStart(107, "em");
      \u0275\u0275text(108, "reticulate");
      \u0275\u0275elementEnd();
      \u0275\u0275text(109, "). ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(110, "p");
      \u0275\u0275text(111, " The ");
      \u0275\u0275elementStart(112, "strong");
      \u0275\u0275text(113, "parallel constructor");
      \u0275\u0275elementEnd();
      \u0275\u0275text(114, " (");
      \u0275\u0275elementStart(115, "code");
      \u0275\u0275text(116, "\u2225");
      \u0275\u0275elementEnd();
      \u0275\u0275text(117, ") models concurrent access to a shared object. When two execution paths run in parallel, their combined state space is the ");
      \u0275\u0275elementStart(118, "strong");
      \u0275\u0275text(119, "product lattice");
      \u0275\u0275elementEnd();
      \u0275\u0275text(120, " \u2014 and products of lattices are lattices. This makes lattice structure ");
      \u0275\u0275elementStart(121, "em");
      \u0275\u0275text(122, "necessary");
      \u0275\u0275elementEnd();
      \u0275\u0275text(123, " rather than merely nice. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(124, "p");
      \u0275\u0275text(125, " Building on this foundation, we develop a ");
      \u0275\u0275elementStart(126, "strong");
      \u0275\u0275text(127, "morphism hierarchy");
      \u0275\u0275elementEnd();
      \u0275\u0275text(128, " (isomorphism, embedding, projection, Galois connection) between session-type state spaces, connecting to bisimulation, abstract interpretation, and multiparty session types. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(129, "div", 64)(130, "div", 65)(131, "div", 66);
      \u0275\u0275text(132, "Session type");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(133, "code");
      \u0275\u0275text(134, "rec X . &{read: X, close: end}");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(135, "div", 67);
      \u0275\u0275text(136, "\u2193");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(137, "div", 65)(138, "div", 66);
      \u0275\u0275text(139, "State space (lattice)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(140, "code");
      \u0275\u0275text(141, "\u22A4 \u2192 read \u2192 \u22A4 | close \u2192 \u22A5");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(142, "div", 67);
      \u0275\u0275text(143, "\u2193");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(144, "div", 68)(145, "div", 66);
      \u0275\u0275text(146, "Verdict");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(147, "code");
      \u0275\u0275text(148, "\u2713 Is a lattice \xB7 2 states \xB7 2 transitions");
      \u0275\u0275elementEnd()()()()();
      \u0275\u0275elementStart(149, "section", 69)(150, "h2");
      \u0275\u0275text(151, "Capabilities");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(152, "div", 70)(153, "div", 71)(154, "div", 72);
      \u0275\u0275text(155, "\u2713");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(156, "h3");
      \u0275\u0275text(157, "Lattice Verification");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(158, "p");
      \u0275\u0275text(159, "Parse session types, build state spaces, check lattice properties. Detect counterexamples, SCCs, and distributivity.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(160, "div", 73)(161, "div", 72);
      \u0275\u0275text(162, "\u25A6");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(163, "h3");
      \u0275\u0275text(164, "34 Protocol Benchmarks");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(165, "p");
      \u0275\u0275text(166, "SMTP, OAuth 2.0, MCP, A2A, Raft, Saga, 2PC, WebSocket, Kafka, gRPC, and more \u2014 all verified as lattices.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(167, "div", 71)(168, "div", 72);
      \u0275\u0275text(169, "\u2699");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(170, "h3");
      \u0275\u0275text(171, "Test Generation");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(172, "p");
      \u0275\u0275text(173, "Generate JUnit 5 tests from session types: valid paths, protocol violations, and incomplete prefixes.");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(174, "div", 71)(175, "div", 72);
      \u0275\u0275text(176, "\u25C7");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(177, "h3");
      \u0275\u0275text(178, "Hasse Diagrams");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(179, "p");
      \u0275\u0275text(180, "Interactive visualization of state-space lattices with counterexample highlighting and role-colored edges.");
      \u0275\u0275elementEnd()()()();
      \u0275\u0275elementStart(181, "section", 74)(182, "h2");
      \u0275\u0275text(183, "Try an Example");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(184, "p", 75);
      \u0275\u0275text(185, " See the analyzer in action with a real protocol benchmark. ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(186, "div", 76)(187, "a", 77);
      \u0275\u0275text(188, " Java Iterator ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(189, "a", 77);
      \u0275\u0275text(190, " SMTP ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(191, "a", 77);
      \u0275\u0275text(192, " MCP Protocol ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(193, "a", 77);
      \u0275\u0275text(194, " Two-Buyer ");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(195, "section", 78)(196, "h2");
      \u0275\u0275text(197, "Publications");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(198, "div", 79)(199, "div", 80)(200, "div", 81);
      \u0275\u0275text(201, "ICE 2026 \u2014 DisCoTec Workshop");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(202, "div", 82);
      \u0275\u0275text(203, "Session Type State Spaces Form Lattices");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(204, "div", 83);
      \u0275\u0275text(205, "A. Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(206, "a", 84);
      \u0275\u0275text(207, "View paper");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(208, "div", 80)(209, "div", 81);
      \u0275\u0275text(210, "Tool paper (in preparation)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(211, "div", 82);
      \u0275\u0275text(212, "Reticulate: A Lattice Checker for Session Types");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(213, "div", 83);
      \u0275\u0275text(214, "A. Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(215, "a", 84);
      \u0275\u0275text(216, "View paper");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(217, "div", 80)(218, "div", 81);
      \u0275\u0275text(219, "Tool paper (in preparation)");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(220, "div", 82);
      \u0275\u0275text(221, "BICA Reborn: Annotation-Based Session Types for Java Objects");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(222, "div", 83);
      \u0275\u0275text(223, "A. Zua Caldeira");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(224, "a", 84);
      \u0275\u0275text(225, "View paper");
      \u0275\u0275elementEnd()()()();
      \u0275\u0275elementStart(226, "section", 85)(227, "h2");
      \u0275\u0275text(228, "How to Cite");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(229, "div", 86)(230, "code");
      \u0275\u0275text(231, '\nA. Zua Caldeira. "Session Type State Spaces Form Lattices."\nIn: ICE 2026 \u2014 Interaction and Concurrency Experience,\nDisCoTec Satellite Workshop, Urbino, Italy, June 2026. ');
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(232, "section", 87)(233, "h2");
      \u0275\u0275text(234, "Two Implementations");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(235, "div", 88)(236, "mat-card")(237, "mat-card-header")(238, "mat-card-title");
      \u0275\u0275text(239, "Reticulate (Python)");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(240, "mat-card-content")(241, "p");
      \u0275\u0275text(242, " 19 modules, 1,406 tests. Parser, state space, lattice checker, morphisms, test generation, multiparty projection, recursion analysis, Chomsky classification, coverage, visualization. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(243, "mat-card-actions")(244, "a", 89);
      \u0275\u0275text(245, "Online analyzer");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(246, "mat-card")(247, "mat-card-header")(248, "mat-card-title");
      \u0275\u0275text(249, "BICA Reborn (Java)");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(250, "mat-card-content")(251, "p");
      \u0275\u0275text(252, " 7 phases, 1,052 tests. Annotation-based session type checker with concurrency analysis, typestate checking, and JUnit 5 test generation from ");
      \u0275\u0275elementStart(253, "code");
      \u0275\u0275text(254, "@Session");
      \u0275\u0275elementEnd();
      \u0275\u0275text(255, " annotations. ");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(256, "mat-card-actions")(257, "a", 90);
      \u0275\u0275text(258, "Documentation");
      \u0275\u0275elementEnd()()()()();
    }
    if (rf & 2) {
      \u0275\u0275advance(93);
      \u0275\u0275conditional(ctx.loading() ? 93 : ctx.stats() ? 94 : -1);
      \u0275\u0275advance(94);
      \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(9, _c0))("queryParams", \u0275\u0275pureFunction0(10, _c1));
      \u0275\u0275advance(2);
      \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(11, _c0))("queryParams", \u0275\u0275pureFunction0(12, _c2));
      \u0275\u0275advance(2);
      \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(13, _c0))("queryParams", \u0275\u0275pureFunction0(14, _c3));
      \u0275\u0275advance(2);
      \u0275\u0275property("routerLink", \u0275\u0275pureFunction0(15, _c0))("queryParams", \u0275\u0275pureFunction0(16, _c4));
    }
  }, dependencies: [RouterLink, MatCardModule, MatCard, MatCardActions, MatCardContent, MatCardHeader, MatCardTitle, MatButtonModule, MatButton, MatIconModule, MatProgressSpinnerModule, MatProgressSpinner], styles: ['@charset "UTF-8";\n\n\n\n.hero[_ngcontent-%COMP%] {\n  width: 100vw;\n  margin-left: calc(-50vw + 50%);\n  margin-top: -24px;\n  background:\n    linear-gradient(\n      135deg,\n      var(--brand-primary-dark),\n      var(--brand-primary-light));\n  color: #fff;\n  padding: 64px 24px 48px;\n}\n.hero-inner[_ngcontent-%COMP%] {\n  max-width: 1200px;\n  margin: 0 auto;\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 48px;\n  align-items: center;\n}\n.hero-text[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 40px;\n  font-weight: 700;\n  margin: 0 0 12px;\n  line-height: 1.15;\n  letter-spacing: -0.5px;\n}\n.hero-sub[_ngcontent-%COMP%] {\n  font-size: 18px;\n  font-weight: 400;\n  margin: 0 0 16px;\n  opacity: 0.95;\n  line-height: 1.5;\n}\n.hero-desc[_ngcontent-%COMP%] {\n  font-size: 15px;\n  line-height: 1.6;\n  margin: 0 0 24px;\n  opacity: 0.85;\n}\n.hero-cta[_ngcontent-%COMP%] {\n  display: flex;\n  gap: 12px;\n  flex-wrap: wrap;\n}\n.hero-btn-primary[_ngcontent-%COMP%] {\n  background: #fff !important;\n  color: var(--brand-primary-dark) !important;\n  font-weight: 500;\n  font-size: 15px;\n  padding: 8px 24px;\n}\n.hero-btn-outline[_ngcontent-%COMP%] {\n  border-color: rgba(255, 255, 255, 0.6) !important;\n  color: #fff !important;\n}\n.hero-author[_ngcontent-%COMP%] {\n  font-size: 13px;\n  opacity: 0.65;\n  margin: 16px 0 0;\n}\n.hero-diagram[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n}\n.hero-lattice-svg[_ngcontent-%COMP%] {\n  width: 100%;\n  max-width: 340px;\n  height: auto;\n}\n@media (max-width: 768px) {\n  .hero[_ngcontent-%COMP%] {\n    padding: 40px 16px 32px;\n  }\n  .hero-inner[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n    gap: 32px;\n  }\n  .hero-text[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n    font-size: 28px;\n  }\n  .hero-diagram[_ngcontent-%COMP%] {\n    max-height: 300px;\n    overflow: auto;\n  }\n}\n.stats-bar[_ngcontent-%COMP%] {\n  width: 100vw;\n  margin-left: calc(-50vw + 50%);\n  background: #f8fafc;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n  display: flex;\n  justify-content: center;\n  gap: 48px;\n  padding: 20px 16px;\n  flex-wrap: wrap;\n}\n.stat-item[_ngcontent-%COMP%] {\n  text-align: center;\n}\n.stat-value[_ngcontent-%COMP%] {\n  display: block;\n  font-size: 22px;\n  font-weight: 700;\n  color: var(--brand-primary);\n}\n.stat-label[_ngcontent-%COMP%] {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n}\nsection[_ngcontent-%COMP%]:not(.hero):not(.stats-bar) {\n  max-width: 960px;\n  margin: 0 auto;\n  padding: 48px 16px;\n}\nh2[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 24px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.85);\n}\n.concept-content[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: 1.4fr 1fr;\n  gap: 40px;\n  align-items: flex-start;\n}\n.concept-text[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.7;\n  margin: 0 0 14px;\n  color: rgba(0, 0, 0, 0.75);\n}\n.concept-example[_ngcontent-%COMP%] {\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  gap: 8px;\n}\n.code-example[_ngcontent-%COMP%] {\n  width: 100%;\n  background: #f1f5f9;\n  border-radius: 8px;\n  padding: 14px 16px;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n}\n.code-example.result[_ngcontent-%COMP%] {\n  background: #ecfdf5;\n  border-color: #a7f3d0;\n}\n.code-label[_ngcontent-%COMP%] {\n  font-size: 11px;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: rgba(0, 0, 0, 0.45);\n  margin-bottom: 6px;\n}\n.code-example[_ngcontent-%COMP%]   code[_ngcontent-%COMP%] {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.8);\n}\n.arrow-down[_ngcontent-%COMP%] {\n  font-size: 18px;\n  color: rgba(0, 0, 0, 0.25);\n}\n@media (max-width: 768px) {\n  .concept-content[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n}\n.cap-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 20px;\n}\n.cap-card[_ngcontent-%COMP%] {\n  background: #fff;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 12px;\n  padding: 24px 20px;\n  cursor: pointer;\n  transition: box-shadow 0.2s, border-color 0.2s;\n}\n.cap-card[_ngcontent-%COMP%]:hover {\n  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);\n  border-color: var(--brand-primary-light);\n}\n.cap-icon[_ngcontent-%COMP%] {\n  font-size: 28px;\n  margin-bottom: 12px;\n  color: var(--brand-primary);\n}\n.cap-card[_ngcontent-%COMP%]   h3[_ngcontent-%COMP%] {\n  font-size: 15px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.cap-card[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  font-size: 13px;\n  line-height: 1.6;\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n@media (max-width: 768px) {\n  .cap-grid[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr 1fr;\n  }\n}\n@media (max-width: 480px) {\n  .cap-grid[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n}\n.live-example[_ngcontent-%COMP%] {\n  text-align: center;\n  background: #f8fafc;\n  border-radius: 12px;\n  padding: 40px 24px !important;\n}\n.live-desc[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0 0 20px;\n}\n.example-buttons[_ngcontent-%COMP%] {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 12px;\n  justify-content: center;\n}\n.pub-list[_ngcontent-%COMP%] {\n  display: flex;\n  flex-direction: column;\n  gap: 16px;\n}\n.pub-item[_ngcontent-%COMP%] {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px 20px;\n  background: #fff;\n}\n.pub-venue[_ngcontent-%COMP%] {\n  font-size: 12px;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: var(--brand-primary);\n  font-weight: 500;\n  margin-bottom: 4px;\n}\n.pub-title[_ngcontent-%COMP%] {\n  font-size: 16px;\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.85);\n  margin-bottom: 2px;\n}\n.pub-authors[_ngcontent-%COMP%] {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.55);\n  margin-bottom: 8px;\n}\n.cite-box[_ngcontent-%COMP%] {\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 20px 24px;\n  max-width: 640px;\n  margin: 0 auto;\n}\n.cite-box[_ngcontent-%COMP%]   code[_ngcontent-%COMP%] {\n  white-space: pre-line;\n  font-size: 13px;\n  line-height: 1.6;\n  color: rgba(0, 0, 0, 0.75);\n}\n.tools-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 24px;\n}\n.tools-grid[_ngcontent-%COMP%]   mat-card[_ngcontent-%COMP%] {\n  height: 100%;\n  display: flex;\n  flex-direction: column;\n}\n.tools-grid[_ngcontent-%COMP%]   mat-card-content[_ngcontent-%COMP%] {\n  flex: 1;\n}\n.tools-grid[_ngcontent-%COMP%]   mat-card-content[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  line-height: 1.6;\n  color: rgba(0, 0, 0, 0.7);\n}\n@media (max-width: 768px) {\n  .tools-grid[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n}\n/*# sourceMappingURL=home.component.css.map */'] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(HomeComponent, [{
    type: Component,
    args: [{ selector: "app-home", standalone: true, imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule], template: `
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-text">
          <h1>Session Types as Lattices</h1>
          <p class="hero-sub">Verify object protocols. Visualize state spaces. Generate tests.</p>
          <p class="hero-desc">
            A theory and toolchain proving that session-type state spaces form
            <strong>lattices</strong> &mdash; with the parallel constructor forcing
            product lattice structure for concurrent access.
          </p>
          <div class="hero-cta">
            <a mat-flat-button routerLink="/tools/analyzer" class="hero-btn-primary">
              Try the Analyzer
            </a>
            <a mat-stroked-button routerLink="/publications" class="hero-btn-outline">
              Read the Paper
            </a>
          </div>
          <p class="hero-author">Alexandre Zua Caldeira &mdash; Independent Researcher</p>
        </div>
        <div class="hero-diagram">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 340 440" class="hero-lattice-svg">
            <defs>
              <marker id="ah" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
                <polygon points="0 0, 8 3, 0 6" fill="rgba(255,255,255,0.5)"/>
              </marker>
            </defs>
            <g class="edges" stroke="rgba(255,255,255,0.35)" stroke-width="1.2" fill="none" marker-end="url(#ah)">
              <line x1="157" y1="36" x2="83" y2="102"/>
              <line x1="183" y1="36" x2="257" y2="102"/>
              <line x1="61" y1="130" x2="29" y2="188"/>
              <line x1="83" y1="126" x2="157" y2="192"/>
              <line x1="257" y1="126" x2="183" y2="192"/>
              <line x1="279" y1="130" x2="311" y2="188"/>
              <line x1="29" y1="220" x2="61" y2="278"/>
              <line x1="157" y1="216" x2="83" y2="282"/>
              <line x1="183" y1="216" x2="257" y2="282"/>
              <line x1="311" y1="220" x2="279" y2="278"/>
              <line x1="83" y1="306" x2="157" y2="372"/>
              <line x1="257" y1="306" x2="183" y2="372"/>
            </g>
            <g fill="rgba(255,255,255,0.55)" font-family="Inter,sans-serif" font-size="11" font-style="italic">
              <text x="112" y="63">a</text>
              <text x="222" y="63">c</text>
              <text x="38" y="156">b</text>
              <text x="128" y="156">c</text>
              <text x="212" y="156">a</text>
              <text x="298" y="156">d</text>
              <text x="30" y="252">c</text>
              <text x="108" y="252">b</text>
              <text x="230" y="252">d</text>
              <text x="306" y="252">a</text>
              <text x="108" y="342">d</text>
              <text x="228" y="342">b</text>
            </g>
            <g font-family="Inter,sans-serif" font-size="7" text-anchor="middle" dominant-baseline="central">
              <circle cx="170" cy="24" r="18" fill="rgba(147,197,253,0.3)" stroke="rgba(191,219,254,0.8)" stroke-width="1.5"/>
              <text x="170" y="24" fill="rgba(255,255,255,0.95)">(&#x22A4;&#x2081;, &#x22A4;&#x2082;)</text>
              <circle cx="70" cy="114" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="70" y="114" fill="rgba(255,255,255,0.9)">(s&#x2090;, &#x22A4;&#x2082;)</text>
              <circle cx="270" cy="114" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="270" y="114" fill="rgba(255,255,255,0.9)">(&#x22A4;&#x2081;, s&#x1D9C;)</text>
              <circle cx="20" cy="204" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="20" y="204" fill="rgba(255,255,255,0.9)">(&#x22A5;&#x2081;, &#x22A4;&#x2082;)</text>
              <circle cx="170" cy="204" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="170" y="204" fill="rgba(255,255,255,0.9)">(s&#x2090;, s&#x1D9C;)</text>
              <circle cx="320" cy="204" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="320" y="204" fill="rgba(255,255,255,0.9)">(&#x22A4;&#x2081;, &#x22A5;&#x2082;)</text>
              <circle cx="70" cy="294" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="70" y="294" fill="rgba(255,255,255,0.9)">(&#x22A5;&#x2081;, s&#x1D9C;)</text>
              <circle cx="270" cy="294" r="18" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.5)" stroke-width="1.2"/>
              <text x="270" y="294" fill="rgba(255,255,255,0.9)">(s&#x2090;, &#x22A5;&#x2082;)</text>
              <circle cx="170" cy="384" r="18" fill="rgba(134,239,172,0.25)" stroke="rgba(187,247,208,0.8)" stroke-width="1.5"/>
              <text x="170" y="384" fill="rgba(255,255,255,0.95)">(&#x22A5;&#x2081;, &#x22A5;&#x2082;)</text>
            </g>
            <text x="170" y="438" text-anchor="middle" fill="rgba(255,255,255,0.6)" font-family="Inter,sans-serif" font-size="10">
              a.b.end &#x2225; c.d.end &mdash; 3&#xD7;3 product lattice
            </text>
          </svg>
        </div>
      </div>
    </section>

    <!-- Stats bar -->
    <section class="stats-bar">
      @if (loading()) {
        <mat-spinner diameter="16"></mat-spinner>
      } @else if (stats()) {
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.numBenchmarks }}</span>
          <span class="stat-label">Benchmarks</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.totalStates }}</span>
          <span class="stat-label">States analyzed</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ stats()!.totalTests }}</span>
          <span class="stat-label">Tests generated</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">19</span>
          <span class="stat-label">Analysis modules</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">2,458</span>
          <span class="stat-label">Total tests</span>
        </div>
      }
    </section>

    <!-- Core concept -->
    <section class="concept">
      <h2>The Key Insight</h2>
      <div class="concept-content">
        <div class="concept-text">
          <p>
            <strong>Session types</strong> describe communication protocols on objects &mdash;
            the legal sequences of method calls, branches, and selections.
            We prove that the state space of any well-formed session type,
            ordered by reachability, forms a <strong>lattice</strong>
            (which we call a <em>reticulate</em>).
          </p>
          <p>
            The <strong>parallel constructor</strong> (<code>&#x2225;</code>) models concurrent
            access to a shared object. When two execution paths run in parallel, their
            combined state space is the <strong>product lattice</strong> &mdash; and products
            of lattices are lattices. This makes lattice structure <em>necessary</em>
            rather than merely nice.
          </p>
          <p>
            Building on this foundation, we develop a <strong>morphism hierarchy</strong>
            (isomorphism, embedding, projection, Galois connection) between session-type
            state spaces, connecting to bisimulation, abstract interpretation, and
            multiparty session types.
          </p>
        </div>
        <div class="concept-example">
          <div class="code-example">
            <div class="code-label">Session type</div>
            <code>rec X . &amp;&#123;read: X, close: end&#125;</code>
          </div>
          <div class="arrow-down">&#x2193;</div>
          <div class="code-example">
            <div class="code-label">State space (lattice)</div>
            <code>&#x22A4; &rarr; read &rarr; &#x22A4; | close &rarr; &#x22A5;</code>
          </div>
          <div class="arrow-down">&#x2193;</div>
          <div class="code-example result">
            <div class="code-label">Verdict</div>
            <code>&#x2713; Is a lattice &middot; 2 states &middot; 2 transitions</code>
          </div>
        </div>
      </div>
    </section>

    <!-- Capabilities -->
    <section class="capabilities">
      <h2>Capabilities</h2>
      <div class="cap-grid">
        <div class="cap-card" routerLink="/tools/analyzer">
          <div class="cap-icon">&#x2713;</div>
          <h3>Lattice Verification</h3>
          <p>Parse session types, build state spaces, check lattice properties.
             Detect counterexamples, SCCs, and distributivity.</p>
        </div>
        <div class="cap-card" routerLink="/benchmarks">
          <div class="cap-icon">&#x25A6;</div>
          <h3>34 Protocol Benchmarks</h3>
          <p>SMTP, OAuth 2.0, MCP, A2A, Raft, Saga, 2PC, WebSocket, Kafka,
             gRPC, and more &mdash; all verified as lattices.</p>
        </div>
        <div class="cap-card" routerLink="/tools/analyzer">
          <div class="cap-icon">&#x2699;</div>
          <h3>Test Generation</h3>
          <p>Generate JUnit 5 tests from session types: valid paths,
             protocol violations, and incomplete prefixes.</p>
        </div>
        <div class="cap-card" routerLink="/tools/analyzer">
          <div class="cap-icon">&#x25C7;</div>
          <h3>Hasse Diagrams</h3>
          <p>Interactive visualization of state-space lattices with
             counterexample highlighting and role-colored edges.</p>
        </div>
      </div>
    </section>

    <!-- Live example -->
    <section class="live-example">
      <h2>Try an Example</h2>
      <p class="live-desc">
        See the analyzer in action with a real protocol benchmark.
      </p>
      <div class="example-buttons">
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}'}">
          Java Iterator
        </a>
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}'}">
          SMTP
        </a>
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})'}">
          MCP Protocol
        </a>
        <a mat-stroked-button
           [routerLink]="['/tools/analyzer']"
           [queryParams]="{type: 'lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})'}">
          Two-Buyer
        </a>
      </div>
    </section>

    <!-- Publications -->
    <section class="publications">
      <h2>Publications</h2>
      <div class="pub-list">
        <div class="pub-item">
          <div class="pub-venue">ICE 2026 &mdash; DisCoTec Workshop</div>
          <div class="pub-title">Session Type State Spaces Form Lattices</div>
          <div class="pub-authors">A. Zua Caldeira</div>
          <a mat-button routerLink="/publications">View paper</a>
        </div>
        <div class="pub-item">
          <div class="pub-venue">Tool paper (in preparation)</div>
          <div class="pub-title">Reticulate: A Lattice Checker for Session Types</div>
          <div class="pub-authors">A. Zua Caldeira</div>
          <a mat-button routerLink="/publications">View paper</a>
        </div>
        <div class="pub-item">
          <div class="pub-venue">Tool paper (in preparation)</div>
          <div class="pub-title">BICA Reborn: Annotation-Based Session Types for Java Objects</div>
          <div class="pub-authors">A. Zua Caldeira</div>
          <a mat-button routerLink="/publications">View paper</a>
        </div>
      </div>
    </section>

    <!-- How to cite -->
    <section class="cite">
      <h2>How to Cite</h2>
      <div class="cite-box">
        <code>
A. Zua Caldeira. "Session Type State Spaces Form Lattices."
In: ICE 2026 &mdash; Interaction and Concurrency Experience,
DisCoTec Satellite Workshop, Urbino, Italy, June 2026.
        </code>
      </div>
    </section>

    <!-- Tools -->
    <section class="tools-section">
      <h2>Two Implementations</h2>
      <div class="tools-grid">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Reticulate (Python)</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              19 modules, 1,406 tests. Parser, state space, lattice checker,
              morphisms, test generation, multiparty projection, recursion analysis,
              Chomsky classification, coverage, visualization.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/tools/analyzer">Online analyzer</a>
          </mat-card-actions>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>BICA Reborn (Java)</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>
              7 phases, 1,052 tests. Annotation-based session type checker
              with concurrency analysis, typestate checking, and JUnit 5
              test generation from <code>&#64;Session</code> annotations.
            </p>
          </mat-card-content>
          <mat-card-actions>
            <a mat-button routerLink="/documentation">Documentation</a>
          </mat-card-actions>
        </mat-card>
      </div>
    </section>
  `, styles: ['@charset "UTF-8";\n\n/* angular:styles/component:scss;aebf31bbd86d425f4b5d2e662871ed74191351ad1a69c4fc68b9dec331876b96;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/home/home.component.ts */\n.hero {\n  width: 100vw;\n  margin-left: calc(-50vw + 50%);\n  margin-top: -24px;\n  background:\n    linear-gradient(\n      135deg,\n      var(--brand-primary-dark),\n      var(--brand-primary-light));\n  color: #fff;\n  padding: 64px 24px 48px;\n}\n.hero-inner {\n  max-width: 1200px;\n  margin: 0 auto;\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 48px;\n  align-items: center;\n}\n.hero-text h1 {\n  font-size: 40px;\n  font-weight: 700;\n  margin: 0 0 12px;\n  line-height: 1.15;\n  letter-spacing: -0.5px;\n}\n.hero-sub {\n  font-size: 18px;\n  font-weight: 400;\n  margin: 0 0 16px;\n  opacity: 0.95;\n  line-height: 1.5;\n}\n.hero-desc {\n  font-size: 15px;\n  line-height: 1.6;\n  margin: 0 0 24px;\n  opacity: 0.85;\n}\n.hero-cta {\n  display: flex;\n  gap: 12px;\n  flex-wrap: wrap;\n}\n.hero-btn-primary {\n  background: #fff !important;\n  color: var(--brand-primary-dark) !important;\n  font-weight: 500;\n  font-size: 15px;\n  padding: 8px 24px;\n}\n.hero-btn-outline {\n  border-color: rgba(255, 255, 255, 0.6) !important;\n  color: #fff !important;\n}\n.hero-author {\n  font-size: 13px;\n  opacity: 0.65;\n  margin: 16px 0 0;\n}\n.hero-diagram {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n}\n.hero-lattice-svg {\n  width: 100%;\n  max-width: 340px;\n  height: auto;\n}\n@media (max-width: 768px) {\n  .hero {\n    padding: 40px 16px 32px;\n  }\n  .hero-inner {\n    grid-template-columns: 1fr;\n    gap: 32px;\n  }\n  .hero-text h1 {\n    font-size: 28px;\n  }\n  .hero-diagram {\n    max-height: 300px;\n    overflow: auto;\n  }\n}\n.stats-bar {\n  width: 100vw;\n  margin-left: calc(-50vw + 50%);\n  background: #f8fafc;\n  border-bottom: 1px solid rgba(0, 0, 0, 0.06);\n  display: flex;\n  justify-content: center;\n  gap: 48px;\n  padding: 20px 16px;\n  flex-wrap: wrap;\n}\n.stat-item {\n  text-align: center;\n}\n.stat-value {\n  display: block;\n  font-size: 22px;\n  font-weight: 700;\n  color: var(--brand-primary);\n}\n.stat-label {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n}\nsection:not(.hero):not(.stats-bar) {\n  max-width: 960px;\n  margin: 0 auto;\n  padding: 48px 16px;\n}\nh2 {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 24px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.85);\n}\n.concept-content {\n  display: grid;\n  grid-template-columns: 1.4fr 1fr;\n  gap: 40px;\n  align-items: flex-start;\n}\n.concept-text p {\n  line-height: 1.7;\n  margin: 0 0 14px;\n  color: rgba(0, 0, 0, 0.75);\n}\n.concept-example {\n  display: flex;\n  flex-direction: column;\n  align-items: center;\n  gap: 8px;\n}\n.code-example {\n  width: 100%;\n  background: #f1f5f9;\n  border-radius: 8px;\n  padding: 14px 16px;\n  border: 1px solid rgba(0, 0, 0, 0.06);\n}\n.code-example.result {\n  background: #ecfdf5;\n  border-color: #a7f3d0;\n}\n.code-label {\n  font-size: 11px;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: rgba(0, 0, 0, 0.45);\n  margin-bottom: 6px;\n}\n.code-example code {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.8);\n}\n.arrow-down {\n  font-size: 18px;\n  color: rgba(0, 0, 0, 0.25);\n}\n@media (max-width: 768px) {\n  .concept-content {\n    grid-template-columns: 1fr;\n  }\n}\n.cap-grid {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 20px;\n}\n.cap-card {\n  background: #fff;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 12px;\n  padding: 24px 20px;\n  cursor: pointer;\n  transition: box-shadow 0.2s, border-color 0.2s;\n}\n.cap-card:hover {\n  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);\n  border-color: var(--brand-primary-light);\n}\n.cap-icon {\n  font-size: 28px;\n  margin-bottom: 12px;\n  color: var(--brand-primary);\n}\n.cap-card h3 {\n  font-size: 15px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.cap-card p {\n  font-size: 13px;\n  line-height: 1.6;\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0;\n}\n@media (max-width: 768px) {\n  .cap-grid {\n    grid-template-columns: 1fr 1fr;\n  }\n}\n@media (max-width: 480px) {\n  .cap-grid {\n    grid-template-columns: 1fr;\n  }\n}\n.live-example {\n  text-align: center;\n  background: #f8fafc;\n  border-radius: 12px;\n  padding: 40px 24px !important;\n}\n.live-desc {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0 0 20px;\n}\n.example-buttons {\n  display: flex;\n  flex-wrap: wrap;\n  gap: 12px;\n  justify-content: center;\n}\n.pub-list {\n  display: flex;\n  flex-direction: column;\n  gap: 16px;\n}\n.pub-item {\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 16px 20px;\n  background: #fff;\n}\n.pub-venue {\n  font-size: 12px;\n  text-transform: uppercase;\n  letter-spacing: 0.5px;\n  color: var(--brand-primary);\n  font-weight: 500;\n  margin-bottom: 4px;\n}\n.pub-title {\n  font-size: 16px;\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.85);\n  margin-bottom: 2px;\n}\n.pub-authors {\n  font-size: 14px;\n  color: rgba(0, 0, 0, 0.55);\n  margin-bottom: 8px;\n}\n.cite-box {\n  background: #f1f5f9;\n  border: 1px solid rgba(0, 0, 0, 0.08);\n  border-radius: 8px;\n  padding: 20px 24px;\n  max-width: 640px;\n  margin: 0 auto;\n}\n.cite-box code {\n  white-space: pre-line;\n  font-size: 13px;\n  line-height: 1.6;\n  color: rgba(0, 0, 0, 0.75);\n}\n.tools-grid {\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 24px;\n}\n.tools-grid mat-card {\n  height: 100%;\n  display: flex;\n  flex-direction: column;\n}\n.tools-grid mat-card-content {\n  flex: 1;\n}\n.tools-grid mat-card-content p {\n  line-height: 1.6;\n  color: rgba(0, 0, 0, 0.7);\n}\n@media (max-width: 768px) {\n  .tools-grid {\n    grid-template-columns: 1fr;\n  }\n}\n/*# sourceMappingURL=home.component.css.map */\n'] }]
  }], () => [{ type: ApiService }], null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(HomeComponent, { className: "HomeComponent", filePath: "src/app/pages/home/home.component.ts", lineNumber: 661 });
})();

// src/app/app.routes.ts
var routes = [
  { path: "", component: HomeComponent },
  {
    path: "tools/analyzer",
    loadComponent: () => import("./chunk-M2IL4VQS.js").then((m) => m.AnalyzerComponent)
  },
  {
    path: "tools/global-analyzer",
    loadComponent: () => import("./chunk-LB7FUTCO.js").then((m) => m.GlobalAnalyzerComponent)
  },
  {
    path: "tools/test-generator",
    loadComponent: () => import("./chunk-WEEN5E44.js").then((m) => m.TestGeneratorComponent)
  },
  {
    path: "benchmarks",
    loadComponent: () => import("./chunk-TQJI4YVU.js").then((m) => m.BenchmarksComponent)
  },
  {
    path: "pipeline",
    loadComponent: () => import("./chunk-UHLWZBE7.js").then((m) => m.PipelineComponent)
  },
  {
    path: "publications",
    loadComponent: () => import("./chunk-DUFPOB2B.js").then((m) => m.PublicationsComponent)
  },
  {
    path: "tutorials/:id",
    loadComponent: () => import("./chunk-TL4XWDTA.js").then((m) => m.TutorialDetailComponent)
  },
  {
    path: "tutorials",
    loadComponent: () => import("./chunk-WMQIZF2F.js").then((m) => m.TutorialsListComponent)
  },
  { path: "quickstart", redirectTo: "tutorials/quick-start" },
  {
    path: "faq",
    loadComponent: () => import("./chunk-N7U6I4XV.js").then((m) => m.FaqComponent)
  },
  {
    path: "documentation",
    loadComponent: () => import("./chunk-BDRXLE3T.js").then((m) => m.DocumentationComponent)
  },
  {
    path: "about",
    loadComponent: () => import("./chunk-TCDXY4GL.js").then((m) => m.AboutComponent)
  },
  {
    path: "dashboard",
    loadComponent: () => import("./chunk-XFA4NJV3.js").then((m) => m.DashboardComponent)
  },
  { path: "**", redirectTo: "" }
];

// src/app/app.config.ts
var appConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    provideAnimationsAsync()
  ]
};

// node_modules/@angular/material/fesm2022/toolbar.mjs
var _c02 = ["*", [["mat-toolbar-row"]]];
var _c12 = ["*", "mat-toolbar-row"];
var MatToolbarRow = class _MatToolbarRow {
  static \u0275fac = function MatToolbarRow_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MatToolbarRow)();
  };
  static \u0275dir = /* @__PURE__ */ \u0275\u0275defineDirective({
    type: _MatToolbarRow,
    selectors: [["mat-toolbar-row"]],
    hostAttrs: [1, "mat-toolbar-row"],
    exportAs: ["matToolbarRow"]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MatToolbarRow, [{
    type: Directive,
    args: [{
      selector: "mat-toolbar-row",
      exportAs: "matToolbarRow",
      host: {
        "class": "mat-toolbar-row"
      }
    }]
  }], null, null);
})();
var MatToolbar = class _MatToolbar {
  _elementRef = inject(ElementRef);
  _platform = inject(Platform);
  _document = inject(DOCUMENT);
  color;
  _toolbarRows;
  constructor() {
  }
  ngAfterViewInit() {
    if (this._platform.isBrowser) {
      this._checkToolbarMixedModes();
      this._toolbarRows.changes.subscribe(() => this._checkToolbarMixedModes());
    }
  }
  _checkToolbarMixedModes() {
    if (this._toolbarRows.length && (typeof ngDevMode === "undefined" || ngDevMode)) {
      const isCombinedUsage = Array.from(this._elementRef.nativeElement.childNodes).filter((node) => !(node.classList && node.classList.contains("mat-toolbar-row"))).filter((node) => node.nodeType !== (this._document ? this._document.COMMENT_NODE : 8)).some((node) => !!(node.textContent && node.textContent.trim()));
      if (isCombinedUsage) {
        throwToolbarMixedModesError();
      }
    }
  }
  static \u0275fac = function MatToolbar_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MatToolbar)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({
    type: _MatToolbar,
    selectors: [["mat-toolbar"]],
    contentQueries: function MatToolbar_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        \u0275\u0275contentQuery(dirIndex, MatToolbarRow, 5);
      }
      if (rf & 2) {
        let _t;
        \u0275\u0275queryRefresh(_t = \u0275\u0275loadQuery()) && (ctx._toolbarRows = _t);
      }
    },
    hostAttrs: [1, "mat-toolbar"],
    hostVars: 6,
    hostBindings: function MatToolbar_HostBindings(rf, ctx) {
      if (rf & 2) {
        \u0275\u0275classMap(ctx.color ? "mat-" + ctx.color : "");
        \u0275\u0275classProp("mat-toolbar-multiple-rows", ctx._toolbarRows.length > 0)("mat-toolbar-single-row", ctx._toolbarRows.length === 0);
      }
    },
    inputs: {
      color: "color"
    },
    exportAs: ["matToolbar"],
    ngContentSelectors: _c12,
    decls: 2,
    vars: 0,
    template: function MatToolbar_Template(rf, ctx) {
      if (rf & 1) {
        \u0275\u0275projectionDef(_c02);
        \u0275\u0275projection(0);
        \u0275\u0275projection(1, 1);
      }
    },
    styles: [".mat-toolbar{background:var(--mat-toolbar-container-background-color, var(--mat-sys-surface));color:var(--mat-toolbar-container-text-color, var(--mat-sys-on-surface))}.mat-toolbar,.mat-toolbar h1,.mat-toolbar h2,.mat-toolbar h3,.mat-toolbar h4,.mat-toolbar h5,.mat-toolbar h6{font-family:var(--mat-toolbar-title-text-font, var(--mat-sys-title-large-font));font-size:var(--mat-toolbar-title-text-size, var(--mat-sys-title-large-size));line-height:var(--mat-toolbar-title-text-line-height, var(--mat-sys-title-large-line-height));font-weight:var(--mat-toolbar-title-text-weight, var(--mat-sys-title-large-weight));letter-spacing:var(--mat-toolbar-title-text-tracking, var(--mat-sys-title-large-tracking));margin:0}@media(forced-colors: active){.mat-toolbar{outline:solid 1px}}.mat-toolbar .mat-form-field-underline,.mat-toolbar .mat-form-field-ripple,.mat-toolbar .mat-focused .mat-form-field-ripple{background-color:currentColor}.mat-toolbar .mat-form-field-label,.mat-toolbar .mat-focused .mat-form-field-label,.mat-toolbar .mat-select-value,.mat-toolbar .mat-select-arrow,.mat-toolbar .mat-form-field.mat-focused .mat-select-arrow{color:inherit}.mat-toolbar .mat-input-element{caret-color:currentColor}.mat-toolbar .mat-mdc-button-base.mat-mdc-button-base.mat-unthemed{--mat-button-text-label-text-color: var(--mat-toolbar-container-text-color, var(--mat-sys-on-surface));--mat-button-outlined-label-text-color: var(--mat-toolbar-container-text-color, var(--mat-sys-on-surface))}.mat-toolbar-row,.mat-toolbar-single-row{display:flex;box-sizing:border-box;padding:0 16px;width:100%;flex-direction:row;align-items:center;white-space:nowrap;height:var(--mat-toolbar-standard-height, 64px)}@media(max-width: 599px){.mat-toolbar-row,.mat-toolbar-single-row{height:var(--mat-toolbar-mobile-height, 56px)}}.mat-toolbar-multiple-rows{display:flex;box-sizing:border-box;flex-direction:column;width:100%;min-height:var(--mat-toolbar-standard-height, 64px)}@media(max-width: 599px){.mat-toolbar-multiple-rows{min-height:var(--mat-toolbar-mobile-height, 56px)}}\n"],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MatToolbar, [{
    type: Component,
    args: [{
      selector: "mat-toolbar",
      exportAs: "matToolbar",
      host: {
        "class": "mat-toolbar",
        "[class]": 'color ? "mat-" + color : ""',
        "[class.mat-toolbar-multiple-rows]": "_toolbarRows.length > 0",
        "[class.mat-toolbar-single-row]": "_toolbarRows.length === 0"
      },
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      template: '<ng-content></ng-content>\n<ng-content select="mat-toolbar-row"></ng-content>\n',
      styles: [".mat-toolbar{background:var(--mat-toolbar-container-background-color, var(--mat-sys-surface));color:var(--mat-toolbar-container-text-color, var(--mat-sys-on-surface))}.mat-toolbar,.mat-toolbar h1,.mat-toolbar h2,.mat-toolbar h3,.mat-toolbar h4,.mat-toolbar h5,.mat-toolbar h6{font-family:var(--mat-toolbar-title-text-font, var(--mat-sys-title-large-font));font-size:var(--mat-toolbar-title-text-size, var(--mat-sys-title-large-size));line-height:var(--mat-toolbar-title-text-line-height, var(--mat-sys-title-large-line-height));font-weight:var(--mat-toolbar-title-text-weight, var(--mat-sys-title-large-weight));letter-spacing:var(--mat-toolbar-title-text-tracking, var(--mat-sys-title-large-tracking));margin:0}@media(forced-colors: active){.mat-toolbar{outline:solid 1px}}.mat-toolbar .mat-form-field-underline,.mat-toolbar .mat-form-field-ripple,.mat-toolbar .mat-focused .mat-form-field-ripple{background-color:currentColor}.mat-toolbar .mat-form-field-label,.mat-toolbar .mat-focused .mat-form-field-label,.mat-toolbar .mat-select-value,.mat-toolbar .mat-select-arrow,.mat-toolbar .mat-form-field.mat-focused .mat-select-arrow{color:inherit}.mat-toolbar .mat-input-element{caret-color:currentColor}.mat-toolbar .mat-mdc-button-base.mat-mdc-button-base.mat-unthemed{--mat-button-text-label-text-color: var(--mat-toolbar-container-text-color, var(--mat-sys-on-surface));--mat-button-outlined-label-text-color: var(--mat-toolbar-container-text-color, var(--mat-sys-on-surface))}.mat-toolbar-row,.mat-toolbar-single-row{display:flex;box-sizing:border-box;padding:0 16px;width:100%;flex-direction:row;align-items:center;white-space:nowrap;height:var(--mat-toolbar-standard-height, 64px)}@media(max-width: 599px){.mat-toolbar-row,.mat-toolbar-single-row{height:var(--mat-toolbar-mobile-height, 56px)}}.mat-toolbar-multiple-rows{display:flex;box-sizing:border-box;flex-direction:column;width:100%;min-height:var(--mat-toolbar-standard-height, 64px)}@media(max-width: 599px){.mat-toolbar-multiple-rows{min-height:var(--mat-toolbar-mobile-height, 56px)}}\n"]
    }]
  }], () => [], {
    color: [{
      type: Input
    }],
    _toolbarRows: [{
      type: ContentChildren,
      args: [MatToolbarRow, {
        descendants: true
      }]
    }]
  });
})();
function throwToolbarMixedModesError() {
  throw Error("MatToolbar: Attempting to combine different toolbar modes. Either specify multiple `<mat-toolbar-row>` elements explicitly or just place content inside of a `<mat-toolbar>` for a single row.");
}
var MatToolbarModule = class _MatToolbarModule {
  static \u0275fac = function MatToolbarModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MatToolbarModule)();
  };
  static \u0275mod = /* @__PURE__ */ \u0275\u0275defineNgModule({
    type: _MatToolbarModule,
    imports: [MatToolbar, MatToolbarRow],
    exports: [MatToolbar, MatToolbarRow, BidiModule]
  });
  static \u0275inj = /* @__PURE__ */ \u0275\u0275defineInjector({
    imports: [BidiModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MatToolbarModule, [{
    type: NgModule,
    args: [{
      imports: [MatToolbar, MatToolbarRow],
      exports: [MatToolbar, MatToolbarRow, BidiModule]
    }]
  }], null, null);
})();

// src/app/components/navbar/navbar.component.ts
var _c03 = () => ({ exact: true });
function NavbarComponent_Conditional_56_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "nav", 31)(1, "a", 32);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_1_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(2, "Home");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "span", 33);
    \u0275\u0275text(4, "Tools");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(5, "a", 34);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_5_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(6, "Analyzer");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "a", 35);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_7_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(8, "Global Types");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(9, "a", 36);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_9_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(10, "Test Generator");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(11, "a", 37);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_11_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(12, "Benchmarks");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(13, "a", 38);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_13_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(14, "Publications");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(15, "span", 33);
    \u0275\u0275text(16, "Learn");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(17, "a", 39);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_17_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(18, "Tutorials");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(19, "a", 40);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_19_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(20, "Documentation");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(21, "a", 41);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_21_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(22, "FAQ");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(23, "a", 42);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_23_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(24, "Pipeline");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(25, "a", 43);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_25_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(26, "About");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(27, "a", 44);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_27_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.closeMenu());
    });
    \u0275\u0275text(28, "Dashboard");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(29, "a", 45);
    \u0275\u0275listener("click", function NavbarComponent_Conditional_56_Template_a_click_29_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      ctx_r1.closeMenu();
      return \u0275\u0275resetView(ctx_r1.logoutClicked.emit());
    });
    \u0275\u0275text(30, "Logout");
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    \u0275\u0275advance();
    \u0275\u0275property("routerLinkActiveOptions", \u0275\u0275pureFunction0(1, _c03));
  }
}
var NavbarComponent = class _NavbarComponent {
  router;
  logoutClicked = new EventEmitter();
  isMenuOpen = false;
  toolsOpen = false;
  learnOpen = false;
  routerSub;
  constructor(router) {
    this.router = router;
  }
  get isToolsActive() {
    return this.router.url.startsWith("/tools/");
  }
  get isLearnActive() {
    const url = this.router.url;
    return url.startsWith("/tutorials") || url.startsWith("/documentation") || url.startsWith("/faq") || url.startsWith("/pipeline");
  }
  ngOnInit() {
    this.routerSub = this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => this.closeMenu());
  }
  ngOnDestroy() {
    this.routerSub?.unsubscribe();
  }
  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }
  closeMenu() {
    this.isMenuOpen = false;
  }
  static \u0275fac = function NavbarComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _NavbarComponent)(\u0275\u0275directiveInject(Router));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _NavbarComponent, selectors: [["app-navbar"]], outputs: { logoutClicked: "logoutClicked" }, decls: 57, vars: 10, consts: [["color", "primary", 1, "navbar"], ["routerLink", "/", 1, "brand"], ["xmlns", "http://www.w3.org/2000/svg", "viewBox", "0 0 32 32", "width", "28", "height", "28", 1, "brand-icon"], ["x1", "16", "y1", "4", "x2", "6", "y2", "16", "stroke", "white", "stroke-width", "2", "stroke-linecap", "round"], ["x1", "16", "y1", "4", "x2", "26", "y2", "16", "stroke", "white", "stroke-width", "2", "stroke-linecap", "round"], ["x1", "6", "y1", "16", "x2", "16", "y2", "28", "stroke", "white", "stroke-width", "2", "stroke-linecap", "round"], ["x1", "26", "y1", "16", "x2", "16", "y2", "28", "stroke", "white", "stroke-width", "2", "stroke-linecap", "round"], ["cx", "16", "cy", "4", "r", "3", "fill", "white"], ["cx", "6", "cy", "16", "r", "3", "fill", "white"], ["cx", "26", "cy", "16", "r", "3", "fill", "white"], ["cx", "16", "cy", "28", "r", "3", "fill", "white"], [1, "brand-text"], [1, "spacer"], [1, "nav-links", "desktop-nav"], [1, "nav-dropdown"], ["mat-button", "", 1, "dropdown-trigger", 3, "mouseenter", "mouseleave"], [1, "dropdown-arrow"], [1, "dropdown-menu", 3, "mouseenter", "mouseleave"], ["routerLink", "/tools/analyzer", "routerLinkActive", "active", 3, "click"], ["routerLink", "/tools/global-analyzer", "routerLinkActive", "active", 3, "click"], ["routerLink", "/tools/test-generator", "routerLinkActive", "active", 3, "click"], ["mat-button", "", "routerLink", "/benchmarks", "routerLinkActive", "active"], ["mat-button", "", "routerLink", "/publications", "routerLinkActive", "active"], ["routerLink", "/tutorials", "routerLinkActive", "active", 3, "click"], ["routerLink", "/documentation", "routerLinkActive", "active", 3, "click"], ["routerLink", "/faq", "routerLinkActive", "active", 3, "click"], ["routerLink", "/pipeline", "routerLinkActive", "active", 3, "click"], ["mat-button", "", "routerLink", "/about", "routerLinkActive", "active"], ["mat-icon-button", "", "routerLink", "/dashboard", "routerLinkActive", "active", "title", "Dashboard", 1, "dashboard-btn"], ["mat-icon-button", "", "title", "Logout", 1, "logout-btn", 3, "click"], ["mat-icon-button", "", 1, "mobile-menu-btn", 3, "click"], [1, "mobile-nav"], ["routerLink", "/", "routerLinkActive", "active", 3, "click", "routerLinkActiveOptions"], [1, "mobile-section-label"], ["routerLink", "/tools/analyzer", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/tools/global-analyzer", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/tools/test-generator", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/benchmarks", "routerLinkActive", "active", 3, "click"], ["routerLink", "/publications", "routerLinkActive", "active", 3, "click"], ["routerLink", "/tutorials", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/documentation", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/faq", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/pipeline", "routerLinkActive", "active", 1, "mobile-indent", 3, "click"], ["routerLink", "/about", "routerLinkActive", "active", 3, "click"], ["routerLink", "/dashboard", "routerLinkActive", "active", 3, "click"], [2, "cursor", "pointer", 3, "click"]], template: function NavbarComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "mat-toolbar", 0)(1, "a", 1);
      \u0275\u0275namespaceSVG();
      \u0275\u0275elementStart(2, "svg", 2);
      \u0275\u0275element(3, "line", 3)(4, "line", 4)(5, "line", 5)(6, "line", 6)(7, "circle", 7)(8, "circle", 8)(9, "circle", 9)(10, "circle", 10);
      \u0275\u0275elementEnd();
      \u0275\u0275namespaceHTML();
      \u0275\u0275elementStart(11, "span", 11);
      \u0275\u0275text(12, "BICA Reborn");
      \u0275\u0275elementEnd()();
      \u0275\u0275element(13, "span", 12);
      \u0275\u0275elementStart(14, "nav", 13)(15, "div", 14)(16, "button", 15);
      \u0275\u0275listener("mouseenter", function NavbarComponent_Template_button_mouseenter_16_listener() {
        return ctx.toolsOpen = true;
      })("mouseleave", function NavbarComponent_Template_button_mouseleave_16_listener() {
        return ctx.toolsOpen = false;
      });
      \u0275\u0275text(17, " Tools ");
      \u0275\u0275elementStart(18, "mat-icon", 16);
      \u0275\u0275text(19, "expand_more");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(20, "div", 17);
      \u0275\u0275listener("mouseenter", function NavbarComponent_Template_div_mouseenter_20_listener() {
        return ctx.toolsOpen = true;
      })("mouseleave", function NavbarComponent_Template_div_mouseleave_20_listener() {
        return ctx.toolsOpen = false;
      });
      \u0275\u0275elementStart(21, "a", 18);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_21_listener() {
        return ctx.toolsOpen = false;
      });
      \u0275\u0275text(22, "Analyzer");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(23, "a", 19);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_23_listener() {
        return ctx.toolsOpen = false;
      });
      \u0275\u0275text(24, "Global Types");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(25, "a", 20);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_25_listener() {
        return ctx.toolsOpen = false;
      });
      \u0275\u0275text(26, "Test Generator");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(27, "a", 21);
      \u0275\u0275text(28, "Benchmarks");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(29, "a", 22);
      \u0275\u0275text(30, "Publications");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(31, "div", 14)(32, "button", 15);
      \u0275\u0275listener("mouseenter", function NavbarComponent_Template_button_mouseenter_32_listener() {
        return ctx.learnOpen = true;
      })("mouseleave", function NavbarComponent_Template_button_mouseleave_32_listener() {
        return ctx.learnOpen = false;
      });
      \u0275\u0275text(33, " Learn ");
      \u0275\u0275elementStart(34, "mat-icon", 16);
      \u0275\u0275text(35, "expand_more");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(36, "div", 17);
      \u0275\u0275listener("mouseenter", function NavbarComponent_Template_div_mouseenter_36_listener() {
        return ctx.learnOpen = true;
      })("mouseleave", function NavbarComponent_Template_div_mouseleave_36_listener() {
        return ctx.learnOpen = false;
      });
      \u0275\u0275elementStart(37, "a", 23);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_37_listener() {
        return ctx.learnOpen = false;
      });
      \u0275\u0275text(38, "Tutorials");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(39, "a", 24);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_39_listener() {
        return ctx.learnOpen = false;
      });
      \u0275\u0275text(40, "Documentation");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(41, "a", 25);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_41_listener() {
        return ctx.learnOpen = false;
      });
      \u0275\u0275text(42, "FAQ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(43, "a", 26);
      \u0275\u0275listener("click", function NavbarComponent_Template_a_click_43_listener() {
        return ctx.learnOpen = false;
      });
      \u0275\u0275text(44, "Pipeline");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(45, "a", 27);
      \u0275\u0275text(46, "About");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(47, "a", 28)(48, "mat-icon");
      \u0275\u0275text(49, "dashboard");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(50, "button", 29);
      \u0275\u0275listener("click", function NavbarComponent_Template_button_click_50_listener() {
        return ctx.logoutClicked.emit();
      });
      \u0275\u0275elementStart(51, "mat-icon");
      \u0275\u0275text(52, "logout");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(53, "button", 30);
      \u0275\u0275listener("click", function NavbarComponent_Template_button_click_53_listener() {
        return ctx.toggleMenu();
      });
      \u0275\u0275elementStart(54, "mat-icon");
      \u0275\u0275text(55);
      \u0275\u0275elementEnd()()();
      \u0275\u0275conditionalCreate(56, NavbarComponent_Conditional_56_Template, 31, 2, "nav", 31);
    }
    if (rf & 2) {
      \u0275\u0275advance(16);
      \u0275\u0275classProp("active", ctx.isToolsActive);
      \u0275\u0275advance(4);
      \u0275\u0275classProp("open", ctx.toolsOpen);
      \u0275\u0275advance(12);
      \u0275\u0275classProp("active", ctx.isLearnActive);
      \u0275\u0275advance(4);
      \u0275\u0275classProp("open", ctx.learnOpen);
      \u0275\u0275advance(19);
      \u0275\u0275textInterpolate(ctx.isMenuOpen ? "close" : "menu");
      \u0275\u0275advance();
      \u0275\u0275conditional(ctx.isMenuOpen ? 56 : -1);
    }
  }, dependencies: [
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatToolbar,
    MatButtonModule,
    MatButton,
    MatIconButton,
    MatIconModule,
    MatIcon
  ], styles: ["\n\n.navbar[_ngcontent-%COMP%] {\n  position: sticky;\n  top: 0;\n  z-index: 1000;\n  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);\n}\n.brand[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  text-decoration: none;\n  color: inherit;\n  gap: 8px;\n}\n.brand-icon[_ngcontent-%COMP%] {\n  flex-shrink: 0;\n}\n.brand-text[_ngcontent-%COMP%] {\n  font-size: 18px;\n  font-weight: 500;\n}\n.spacer[_ngcontent-%COMP%] {\n  flex: 1;\n}\n.nav-links[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: inherit;\n}\n.nav-links[_ngcontent-%COMP%]   a.active[_ngcontent-%COMP%] {\n  border-bottom: 2px solid white;\n}\n.nav-dropdown[_ngcontent-%COMP%] {\n  position: relative;\n  display: inline-block;\n}\n.dropdown-trigger[_ngcontent-%COMP%] {\n  color: inherit;\n  display: inline-flex;\n  align-items: center;\n}\n.dropdown-trigger.active[_ngcontent-%COMP%] {\n  border-bottom: 2px solid white;\n}\n.dropdown-arrow[_ngcontent-%COMP%] {\n  font-size: 18px;\n  width: 18px;\n  height: 18px;\n  margin-left: -2px;\n  transition: transform 0.2s;\n}\n.dropdown-menu[_ngcontent-%COMP%] {\n  display: none;\n  position: absolute;\n  top: 100%;\n  left: 0;\n  min-width: 180px;\n  background: #fff;\n  border-radius: 8px;\n  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);\n  padding: 6px 0;\n  z-index: 1001;\n}\n.dropdown-menu.open[_ngcontent-%COMP%] {\n  display: flex;\n  flex-direction: column;\n}\n.dropdown-menu[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  padding: 10px 20px;\n  color: rgba(0, 0, 0, 0.8);\n  text-decoration: none;\n  font-size: 14px;\n  transition: background 0.15s;\n}\n.dropdown-menu[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  background: rgba(0, 0, 0, 0.04);\n}\n.dropdown-menu[_ngcontent-%COMP%]   a.active[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  font-weight: 500;\n}\n.dashboard-btn[_ngcontent-%COMP%] {\n  opacity: 0.7;\n  margin-left: 4px;\n}\n.dashboard-btn[_ngcontent-%COMP%]:hover, \n.dashboard-btn.active[_ngcontent-%COMP%] {\n  opacity: 1;\n}\n.logout-btn[_ngcontent-%COMP%] {\n  opacity: 0.7;\n  margin-left: 4px;\n}\n.logout-btn[_ngcontent-%COMP%]:hover {\n  opacity: 1;\n}\n.mobile-menu-btn[_ngcontent-%COMP%] {\n  display: none;\n  color: inherit;\n}\n.mobile-nav[_ngcontent-%COMP%] {\n  display: none;\n  flex-direction: column;\n  background: var(--mat-sys-primary);\n  position: sticky;\n  top: 64px;\n  z-index: 999;\n}\n.mobile-nav[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  padding: 12px 24px;\n  color: white;\n  text-decoration: none;\n  font-size: 16px;\n  border-bottom: 1px solid rgba(255, 255, 255, 0.1);\n}\n.mobile-nav[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover, \n.mobile-nav[_ngcontent-%COMP%]   a.active[_ngcontent-%COMP%] {\n  background: rgba(255, 255, 255, 0.1);\n}\n.mobile-section-label[_ngcontent-%COMP%] {\n  padding: 10px 24px 4px;\n  font-size: 11px;\n  text-transform: uppercase;\n  letter-spacing: 1px;\n  opacity: 0.5;\n  color: white;\n}\n.mobile-indent[_ngcontent-%COMP%] {\n  padding-left: 40px !important;\n}\n@media (max-width: 768px) {\n  .desktop-nav[_ngcontent-%COMP%] {\n    display: none;\n  }\n  .mobile-menu-btn[_ngcontent-%COMP%] {\n    display: inline-flex;\n  }\n  .mobile-nav[_ngcontent-%COMP%] {\n    display: flex;\n  }\n}\n/*# sourceMappingURL=navbar.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NavbarComponent, [{
    type: Component,
    args: [{ selector: "app-navbar", standalone: true, imports: [
      RouterLink,
      RouterLinkActive,
      MatToolbarModule,
      MatButtonModule,
      MatIconModule
    ], template: `
    <mat-toolbar color="primary" class="navbar">
      <a routerLink="/" class="brand">
        <svg class="brand-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" width="28" height="28">
          <line x1="16" y1="4" x2="6" y2="16" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="16" y1="4" x2="26" y2="16" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="6" y1="16" x2="16" y2="28" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <line x1="26" y1="16" x2="16" y2="28" stroke="white" stroke-width="2" stroke-linecap="round"/>
          <circle cx="16" cy="4" r="3" fill="white"/>
          <circle cx="6" cy="16" r="3" fill="white"/>
          <circle cx="26" cy="16" r="3" fill="white"/>
          <circle cx="16" cy="28" r="3" fill="white"/>
        </svg>
        <span class="brand-text">BICA Reborn</span>
      </a>

      <span class="spacer"></span>

      <!-- Desktop nav -->
      <nav class="nav-links desktop-nav">
        <!-- Tools dropdown -->
        <div class="nav-dropdown">
          <button mat-button class="dropdown-trigger"
                  [class.active]="isToolsActive"
                  (mouseenter)="toolsOpen = true"
                  (mouseleave)="toolsOpen = false">
            Tools <mat-icon class="dropdown-arrow">expand_more</mat-icon>
          </button>
          <div class="dropdown-menu"
               [class.open]="toolsOpen"
               (mouseenter)="toolsOpen = true"
               (mouseleave)="toolsOpen = false">
            <a routerLink="/tools/analyzer" routerLinkActive="active" (click)="toolsOpen = false">Analyzer</a>
            <a routerLink="/tools/global-analyzer" routerLinkActive="active" (click)="toolsOpen = false">Global Types</a>
            <a routerLink="/tools/test-generator" routerLinkActive="active" (click)="toolsOpen = false">Test Generator</a>
          </div>
        </div>
        <a mat-button routerLink="/benchmarks" routerLinkActive="active">Benchmarks</a>
        <a mat-button routerLink="/publications" routerLinkActive="active">Publications</a>

        <!-- Learn dropdown -->
        <div class="nav-dropdown">
          <button mat-button class="dropdown-trigger"
                  [class.active]="isLearnActive"
                  (mouseenter)="learnOpen = true"
                  (mouseleave)="learnOpen = false">
            Learn <mat-icon class="dropdown-arrow">expand_more</mat-icon>
          </button>
          <div class="dropdown-menu"
               [class.open]="learnOpen"
               (mouseenter)="learnOpen = true"
               (mouseleave)="learnOpen = false">
            <a routerLink="/tutorials" routerLinkActive="active" (click)="learnOpen = false">Tutorials</a>
            <a routerLink="/documentation" routerLinkActive="active" (click)="learnOpen = false">Documentation</a>
            <a routerLink="/faq" routerLinkActive="active" (click)="learnOpen = false">FAQ</a>
            <a routerLink="/pipeline" routerLinkActive="active" (click)="learnOpen = false">Pipeline</a>
          </div>
        </div>

        <a mat-button routerLink="/about" routerLinkActive="active">About</a>

        <a mat-icon-button routerLink="/dashboard" routerLinkActive="active" class="dashboard-btn" title="Dashboard">
          <mat-icon>dashboard</mat-icon>
        </a>
        <button mat-icon-button class="logout-btn" title="Logout" (click)="logoutClicked.emit()">
          <mat-icon>logout</mat-icon>
        </button>
      </nav>

      <!-- Mobile hamburger -->
      <button mat-icon-button class="mobile-menu-btn" (click)="toggleMenu()">
        <mat-icon>{{ isMenuOpen ? 'close' : 'menu' }}</mat-icon>
      </button>
    </mat-toolbar>

    <!-- Mobile nav overlay -->
    @if (isMenuOpen) {
      <nav class="mobile-nav">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" (click)="closeMenu()">Home</a>
        <span class="mobile-section-label">Tools</span>
        <a routerLink="/tools/analyzer" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Analyzer</a>
        <a routerLink="/tools/global-analyzer" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Global Types</a>
        <a routerLink="/tools/test-generator" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Test Generator</a>
        <a routerLink="/benchmarks" routerLinkActive="active" (click)="closeMenu()">Benchmarks</a>
        <a routerLink="/publications" routerLinkActive="active" (click)="closeMenu()">Publications</a>
        <span class="mobile-section-label">Learn</span>
        <a routerLink="/tutorials" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Tutorials</a>
        <a routerLink="/documentation" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Documentation</a>
        <a routerLink="/faq" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">FAQ</a>
        <a routerLink="/pipeline" routerLinkActive="active" (click)="closeMenu()" class="mobile-indent">Pipeline</a>
        <a routerLink="/about" routerLinkActive="active" (click)="closeMenu()">About</a>
        <a routerLink="/dashboard" routerLinkActive="active" (click)="closeMenu()">Dashboard</a>
        <a (click)="closeMenu(); logoutClicked.emit()" style="cursor:pointer">Logout</a>
      </nav>
    }
  `, styles: ["/* angular:styles/component:scss;85b4e0e09e2602ae07a7a7d9f1a74eab8c552790b19d25946f2f673adcf1b21c;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/components/navbar/navbar.component.ts */\n.navbar {\n  position: sticky;\n  top: 0;\n  z-index: 1000;\n  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);\n}\n.brand {\n  display: flex;\n  align-items: center;\n  text-decoration: none;\n  color: inherit;\n  gap: 8px;\n}\n.brand-icon {\n  flex-shrink: 0;\n}\n.brand-text {\n  font-size: 18px;\n  font-weight: 500;\n}\n.spacer {\n  flex: 1;\n}\n.nav-links a {\n  color: inherit;\n}\n.nav-links a.active {\n  border-bottom: 2px solid white;\n}\n.nav-dropdown {\n  position: relative;\n  display: inline-block;\n}\n.dropdown-trigger {\n  color: inherit;\n  display: inline-flex;\n  align-items: center;\n}\n.dropdown-trigger.active {\n  border-bottom: 2px solid white;\n}\n.dropdown-arrow {\n  font-size: 18px;\n  width: 18px;\n  height: 18px;\n  margin-left: -2px;\n  transition: transform 0.2s;\n}\n.dropdown-menu {\n  display: none;\n  position: absolute;\n  top: 100%;\n  left: 0;\n  min-width: 180px;\n  background: #fff;\n  border-radius: 8px;\n  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);\n  padding: 6px 0;\n  z-index: 1001;\n}\n.dropdown-menu.open {\n  display: flex;\n  flex-direction: column;\n}\n.dropdown-menu a {\n  padding: 10px 20px;\n  color: rgba(0, 0, 0, 0.8);\n  text-decoration: none;\n  font-size: 14px;\n  transition: background 0.15s;\n}\n.dropdown-menu a:hover {\n  background: rgba(0, 0, 0, 0.04);\n}\n.dropdown-menu a.active {\n  color: var(--brand-primary, #4338ca);\n  font-weight: 500;\n}\n.dashboard-btn {\n  opacity: 0.7;\n  margin-left: 4px;\n}\n.dashboard-btn:hover,\n.dashboard-btn.active {\n  opacity: 1;\n}\n.logout-btn {\n  opacity: 0.7;\n  margin-left: 4px;\n}\n.logout-btn:hover {\n  opacity: 1;\n}\n.mobile-menu-btn {\n  display: none;\n  color: inherit;\n}\n.mobile-nav {\n  display: none;\n  flex-direction: column;\n  background: var(--mat-sys-primary);\n  position: sticky;\n  top: 64px;\n  z-index: 999;\n}\n.mobile-nav a {\n  padding: 12px 24px;\n  color: white;\n  text-decoration: none;\n  font-size: 16px;\n  border-bottom: 1px solid rgba(255, 255, 255, 0.1);\n}\n.mobile-nav a:hover,\n.mobile-nav a.active {\n  background: rgba(255, 255, 255, 0.1);\n}\n.mobile-section-label {\n  padding: 10px 24px 4px;\n  font-size: 11px;\n  text-transform: uppercase;\n  letter-spacing: 1px;\n  opacity: 0.5;\n  color: white;\n}\n.mobile-indent {\n  padding-left: 40px !important;\n}\n@media (max-width: 768px) {\n  .desktop-nav {\n    display: none;\n  }\n  .mobile-menu-btn {\n    display: inline-flex;\n  }\n  .mobile-nav {\n    display: flex;\n  }\n}\n/*# sourceMappingURL=navbar.component.css.map */\n"] }]
  }], () => [{ type: Router }], { logoutClicked: [{
    type: Output
  }] });
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(NavbarComponent, { className: "NavbarComponent", filePath: "src/app/components/navbar/navbar.component.ts", lineNumber: 257 });
})();

// src/app/components/footer/footer.component.ts
var FooterComponent = class _FooterComponent {
  static \u0275fac = function FooterComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _FooterComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _FooterComponent, selectors: [["app-footer"]], decls: 20, vars: 0, consts: [[1, "footer"], [1, "footer-content"], [1, "footer-main"], [1, "footer-brand"], [1, "footer-sep"], [1, "footer-meta"], ["href", "https://github.com/alcides/bica", "target", "_blank", "rel", "noopener"]], template: function FooterComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275domElementStart(0, "footer", 0)(1, "div", 1)(2, "div", 2)(3, "span", 3);
      \u0275\u0275text(4, "BICA Reborn");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(5, "span", 4);
      \u0275\u0275text(6, "\u2014");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(7, "span");
      \u0275\u0275text(8, "Session Types as Algebraic Reticulates");
      \u0275\u0275domElementEnd()();
      \u0275\u0275domElementStart(9, "div", 5)(10, "span");
      \u0275\u0275text(11, "Alexandre Zua Caldeira \xB7 Independent Researcher");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(12, "span", 4);
      \u0275\u0275text(13, "\xB7");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(14, "a", 6);
      \u0275\u0275text(15, "GitHub");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(16, "span", 4);
      \u0275\u0275text(17, "\xB7");
      \u0275\u0275domElementEnd();
      \u0275\u0275domElementStart(18, "span");
      \u0275\u0275text(19, "Last updated: March 2026");
      \u0275\u0275domElementEnd()()()();
    }
  }, styles: ["\n\n.footer[_ngcontent-%COMP%] {\n  padding: 24px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n  font-size: 13px;\n  border-top: 1px solid rgba(0, 0, 0, 0.08);\n  margin-top: 48px;\n  line-height: 1.8;\n}\n.footer-content[_ngcontent-%COMP%] {\n  max-width: 960px;\n  margin: 0 auto;\n}\n.footer-brand[_ngcontent-%COMP%] {\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.65);\n}\n.footer-sep[_ngcontent-%COMP%] {\n  margin: 0 6px;\n}\n.footer-meta[_ngcontent-%COMP%] {\n  margin-top: 4px;\n}\n.footer-meta[_ngcontent-%COMP%]   a[_ngcontent-%COMP%] {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.footer-meta[_ngcontent-%COMP%]   a[_ngcontent-%COMP%]:hover {\n  text-decoration: underline;\n}\n/*# sourceMappingURL=footer.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(FooterComponent, [{
    type: Component,
    args: [{ selector: "app-footer", standalone: true, template: `
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-main">
          <span class="footer-brand">BICA Reborn</span>
          <span class="footer-sep">&mdash;</span>
          <span>Session Types as Algebraic Reticulates</span>
        </div>
        <div class="footer-meta">
          <span>Alexandre Zua Caldeira &middot; Independent Researcher</span>
          <span class="footer-sep">&middot;</span>
          <a href="https://github.com/alcides/bica" target="_blank" rel="noopener">GitHub</a>
          <span class="footer-sep">&middot;</span>
          <span>Last updated: March 2026</span>
        </div>
      </div>
    </footer>
  `, styles: ["/* angular:styles/component:scss;748aff53710312e554894c7455ae1bc501a19fae1e254f22d744dc0a77e5bf72;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/components/footer/footer.component.ts */\n.footer {\n  padding: 24px 16px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.5);\n  font-size: 13px;\n  border-top: 1px solid rgba(0, 0, 0, 0.08);\n  margin-top: 48px;\n  line-height: 1.8;\n}\n.footer-content {\n  max-width: 960px;\n  margin: 0 auto;\n}\n.footer-brand {\n  font-weight: 600;\n  color: rgba(0, 0, 0, 0.65);\n}\n.footer-sep {\n  margin: 0 6px;\n}\n.footer-meta {\n  margin-top: 4px;\n}\n.footer-meta a {\n  color: var(--brand-primary, #4338ca);\n  text-decoration: none;\n}\n.footer-meta a:hover {\n  text-decoration: underline;\n}\n/*# sourceMappingURL=footer.component.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(FooterComponent, { className: "FooterComponent", filePath: "src/app/components/footer/footer.component.ts", lineNumber: 57 });
})();

// src/app/app.ts
function App_Conditional_0_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "app-navbar", 1);
    \u0275\u0275listener("logoutClicked", function App_Conditional_0_Template_app_navbar_logoutClicked_0_listener() {
      \u0275\u0275restoreView(_r1);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.logout());
    });
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(1, "main", 2);
    \u0275\u0275element(2, "router-outlet");
    \u0275\u0275elementEnd();
    \u0275\u0275element(3, "app-footer");
  }
}
function App_Conditional_1_Conditional_8_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "span", 6);
    \u0275\u0275text(1, "Incorrect password");
    \u0275\u0275elementEnd();
  }
}
function App_Conditional_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r3 = \u0275\u0275getCurrentView();
    \u0275\u0275elementStart(0, "div", 0)(1, "div", 3)(2, "h1");
    \u0275\u0275text(3, "BICA Reborn");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "p");
    \u0275\u0275text(5, "Session Types as Algebraic Reticulates");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(6, "form", 4);
    \u0275\u0275listener("ngSubmit", function App_Conditional_1_Template_form_ngSubmit_6_listener() {
      \u0275\u0275restoreView(_r3);
      const ctx_r1 = \u0275\u0275nextContext();
      return \u0275\u0275resetView(ctx_r1.login());
    });
    \u0275\u0275elementStart(7, "input", 5);
    \u0275\u0275twoWayListener("ngModelChange", function App_Conditional_1_Template_input_ngModelChange_7_listener($event) {
      \u0275\u0275restoreView(_r3);
      const ctx_r1 = \u0275\u0275nextContext();
      \u0275\u0275twoWayBindingSet(ctx_r1.password, $event) || (ctx_r1.password = $event);
      return \u0275\u0275resetView($event);
    });
    \u0275\u0275elementEnd();
    \u0275\u0275conditionalCreate(8, App_Conditional_1_Conditional_8_Template, 2, 0, "span", 6);
    \u0275\u0275elementStart(9, "button", 7);
    \u0275\u0275text(10, "Enter");
    \u0275\u0275elementEnd()()()();
  }
  if (rf & 2) {
    const ctx_r1 = \u0275\u0275nextContext();
    \u0275\u0275advance(7);
    \u0275\u0275twoWayProperty("ngModel", ctx_r1.password);
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r1.error() ? 8 : -1);
  }
}
var App = class _App {
  authenticated = signal(sessionStorage.getItem("auth") === "1", ...ngDevMode ? [{ debugName: "authenticated" }] : []);
  error = signal(false, ...ngDevMode ? [{ debugName: "error" }] : []);
  password = "";
  login() {
    if (this.password === "reticulate") {
      sessionStorage.setItem("auth", "1");
      this.authenticated.set(true);
    } else {
      this.error.set(true);
    }
  }
  logout() {
    sessionStorage.removeItem("auth");
    this.authenticated.set(false);
    this.password = "";
    this.error.set(false);
  }
  static \u0275fac = function App_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _App)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _App, selectors: [["app-root"]], decls: 2, vars: 1, consts: [[1, "auth-gate"], [3, "logoutClicked"], [1, "main-content"], [1, "auth-box"], [3, "ngSubmit"], ["type", "password", "name", "password", "placeholder", "Enter password", "autofocus", "", 3, "ngModelChange", "ngModel"], [1, "auth-error"], ["type", "submit"]], template: function App_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275conditionalCreate(0, App_Conditional_0_Template, 4, 0)(1, App_Conditional_1_Template, 11, 2, "div", 0);
    }
    if (rf & 2) {
      \u0275\u0275conditional(ctx.authenticated() ? 0 : 1);
    }
  }, dependencies: [RouterOutlet, NavbarComponent, FooterComponent, FormsModule, \u0275NgNoValidate, DefaultValueAccessor, NgControlStatus, NgControlStatusGroup, NgModel, NgForm], styles: ["\n\n.main-content[_ngcontent-%COMP%] {\n  max-width: 1200px;\n  margin: 0 auto;\n  padding: 24px 16px;\n  min-height: calc(100vh - 64px - 100px);\n}\n.auth-gate[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  min-height: 100vh;\n  background:\n    linear-gradient(\n      135deg,\n      var(--brand-primary-dark, #312e81),\n      var(--brand-primary-light, #6366f1));\n  color: #fff;\n}\n.auth-box[_ngcontent-%COMP%] {\n  text-align: center;\n  padding: 48px;\n}\n.auth-box[_ngcontent-%COMP%]   h1[_ngcontent-%COMP%] {\n  font-size: 32px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.auth-box[_ngcontent-%COMP%]   p[_ngcontent-%COMP%] {\n  opacity: 0.8;\n  margin: 0 0 32px;\n}\n.auth-box[_ngcontent-%COMP%]   input[_ngcontent-%COMP%] {\n  display: block;\n  width: 260px;\n  margin: 0 auto 12px;\n  padding: 10px 16px;\n  border: 1px solid rgba(255, 255, 255, 0.3);\n  border-radius: 6px;\n  background: rgba(255, 255, 255, 0.15);\n  color: #fff;\n  font-size: 16px;\n  text-align: center;\n}\n.auth-box[_ngcontent-%COMP%]   input[_ngcontent-%COMP%]::placeholder {\n  color: rgba(255, 255, 255, 0.5);\n}\n.auth-box[_ngcontent-%COMP%]   button[_ngcontent-%COMP%] {\n  padding: 10px 32px;\n  border: none;\n  border-radius: 6px;\n  background: #fff;\n  color: var(--brand-primary-dark, #312e81);\n  font-size: 16px;\n  font-weight: 500;\n  cursor: pointer;\n}\n.auth-box[_ngcontent-%COMP%]   button[_ngcontent-%COMP%]:hover {\n  opacity: 0.9;\n}\n.auth-error[_ngcontent-%COMP%] {\n  display: block;\n  color: #fca5a5;\n  font-size: 14px;\n  margin-bottom: 12px;\n}\n/*# sourceMappingURL=app.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(App, [{
    type: Component,
    args: [{ selector: "app-root", standalone: true, imports: [RouterOutlet, NavbarComponent, FooterComponent, FormsModule], template: `
    @if (authenticated()) {
      <app-navbar (logoutClicked)="logout()" />
      <main class="main-content">
        <router-outlet />
      </main>
      <app-footer />
    } @else {
      <div class="auth-gate">
        <div class="auth-box">
          <h1>BICA Reborn</h1>
          <p>Session Types as Algebraic Reticulates</p>
          <form (ngSubmit)="login()">
            <input
              type="password"
              [(ngModel)]="password"
              name="password"
              placeholder="Enter password"
              autofocus
            />
            @if (error()) {
              <span class="auth-error">Incorrect password</span>
            }
            <button type="submit">Enter</button>
          </form>
        </div>
      </div>
    }
  `, styles: ["/* angular:styles/component:scss;e56e16515b0c2e3861c5275f22d7e4e2c74de97eec774aad6c71ab684cbd0e68;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/app.ts */\n.main-content {\n  max-width: 1200px;\n  margin: 0 auto;\n  padding: 24px 16px;\n  min-height: calc(100vh - 64px - 100px);\n}\n.auth-gate {\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  min-height: 100vh;\n  background:\n    linear-gradient(\n      135deg,\n      var(--brand-primary-dark, #312e81),\n      var(--brand-primary-light, #6366f1));\n  color: #fff;\n}\n.auth-box {\n  text-align: center;\n  padding: 48px;\n}\n.auth-box h1 {\n  font-size: 32px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.auth-box p {\n  opacity: 0.8;\n  margin: 0 0 32px;\n}\n.auth-box input {\n  display: block;\n  width: 260px;\n  margin: 0 auto 12px;\n  padding: 10px 16px;\n  border: 1px solid rgba(255, 255, 255, 0.3);\n  border-radius: 6px;\n  background: rgba(255, 255, 255, 0.15);\n  color: #fff;\n  font-size: 16px;\n  text-align: center;\n}\n.auth-box input::placeholder {\n  color: rgba(255, 255, 255, 0.5);\n}\n.auth-box button {\n  padding: 10px 32px;\n  border: none;\n  border-radius: 6px;\n  background: #fff;\n  color: var(--brand-primary-dark, #312e81);\n  font-size: 16px;\n  font-weight: 500;\n  cursor: pointer;\n}\n.auth-box button:hover {\n  opacity: 0.9;\n}\n.auth-error {\n  display: block;\n  color: #fca5a5;\n  font-size: 14px;\n  margin-bottom: 12px;\n}\n/*# sourceMappingURL=app.css.map */\n"] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(App, { className: "App", filePath: "src/app/app.ts", lineNumber: 104 });
})();

// src/main.ts
bootstrapApplication(App, appConfig).catch((err) => console.error(err));
/*! Bundled license information:

@angular/platform-browser/fesm2022/animations-async.mjs:
  (**
   * @license Angular v21.2.0
   * (c) 2010-2026 Google LLC. https://angular.dev/
   * License: MIT
   *)
*/
//# sourceMappingURL=main.js.map
