import {
  MatCard,
  MatCardModule
} from "./chunk-SHRTRSL7.js";
import {
  MatChip,
  MatChipSet,
  MatChipsModule
} from "./chunk-F3DYJDGJ.js";
import "./chunk-RSSZT2MJ.js";
import "./chunk-2AQDFUQH.js";
import {
  MatTableModule
} from "./chunk-P4YU6FKE.js";
import "./chunk-CVQJCEWM.js";
import "./chunk-SUS3PTUT.js";
import {
  MatIcon,
  MatIconModule,
  _getAnimationsState
} from "./chunk-BFW3NWZD.js";
import "./chunk-ZG4TCI7P.js";
import {
  BidiModule
} from "./chunk-NL2TMNRB.js";
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DOCUMENT,
  ElementRef,
  EventEmitter,
  InjectionToken,
  Input,
  NgModule,
  NgZone,
  Output,
  Renderer2,
  ViewEncapsulation,
  inject,
  numberAttribute,
  setClassMetadata,
  ɵsetClassDebugInfo,
  ɵɵadvance,
  ɵɵattribute,
  ɵɵclassMap,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵconditionalCreate,
  ɵɵdefineComponent,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdomElement,
  ɵɵdomElementEnd,
  ɵɵdomElementStart,
  ɵɵelement,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵrepeater,
  ɵɵrepeaterCreate,
  ɵɵstyleProp,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1,
  ɵɵtextInterpolate2
} from "./chunk-OWEA7TR3.js";

// node_modules/@angular/material/fesm2022/progress-bar.mjs
function MatProgressBar_Conditional_2_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275domElement(0, "div", 2);
  }
}
var MAT_PROGRESS_BAR_DEFAULT_OPTIONS = new InjectionToken("MAT_PROGRESS_BAR_DEFAULT_OPTIONS");
var MAT_PROGRESS_BAR_LOCATION = new InjectionToken("mat-progress-bar-location", {
  providedIn: "root",
  factory: () => {
    const _document = inject(DOCUMENT);
    const _location = _document ? _document.location : null;
    return {
      getPathname: () => _location ? _location.pathname + _location.search : ""
    };
  }
});
var MatProgressBar = class _MatProgressBar {
  _elementRef = inject(ElementRef);
  _ngZone = inject(NgZone);
  _changeDetectorRef = inject(ChangeDetectorRef);
  _renderer = inject(Renderer2);
  _cleanupTransitionEnd;
  constructor() {
    const animationsState = _getAnimationsState();
    const defaults = inject(MAT_PROGRESS_BAR_DEFAULT_OPTIONS, {
      optional: true
    });
    this._isNoopAnimation = animationsState === "di-disabled";
    if (animationsState === "reduced-motion") {
      this._elementRef.nativeElement.classList.add("mat-progress-bar-reduced-motion");
    }
    if (defaults) {
      if (defaults.color) {
        this.color = this._defaultColor = defaults.color;
      }
      this.mode = defaults.mode || this.mode;
    }
  }
  _isNoopAnimation;
  get color() {
    return this._color || this._defaultColor;
  }
  set color(value) {
    this._color = value;
  }
  _color;
  _defaultColor = "primary";
  get value() {
    return this._value;
  }
  set value(v) {
    this._value = clamp(v || 0);
    this._changeDetectorRef.markForCheck();
  }
  _value = 0;
  get bufferValue() {
    return this._bufferValue || 0;
  }
  set bufferValue(v) {
    this._bufferValue = clamp(v || 0);
    this._changeDetectorRef.markForCheck();
  }
  _bufferValue = 0;
  animationEnd = new EventEmitter();
  get mode() {
    return this._mode;
  }
  set mode(value) {
    this._mode = value;
    this._changeDetectorRef.markForCheck();
  }
  _mode = "determinate";
  ngAfterViewInit() {
    this._ngZone.runOutsideAngular(() => {
      this._cleanupTransitionEnd = this._renderer.listen(this._elementRef.nativeElement, "transitionend", this._transitionendHandler);
    });
  }
  ngOnDestroy() {
    this._cleanupTransitionEnd?.();
  }
  _getPrimaryBarTransform() {
    return `scaleX(${this._isIndeterminate() ? 1 : this.value / 100})`;
  }
  _getBufferBarFlexBasis() {
    return `${this.mode === "buffer" ? this.bufferValue : 100}%`;
  }
  _isIndeterminate() {
    return this.mode === "indeterminate" || this.mode === "query";
  }
  _transitionendHandler = (event) => {
    if (this.animationEnd.observers.length === 0 || !event.target || !event.target.classList.contains("mdc-linear-progress__primary-bar")) {
      return;
    }
    if (this.mode === "determinate" || this.mode === "buffer") {
      this._ngZone.run(() => this.animationEnd.next({
        value: this.value
      }));
    }
  };
  static \u0275fac = function MatProgressBar_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MatProgressBar)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({
    type: _MatProgressBar,
    selectors: [["mat-progress-bar"]],
    hostAttrs: ["role", "progressbar", "aria-valuemin", "0", "aria-valuemax", "100", "tabindex", "-1", 1, "mat-mdc-progress-bar", "mdc-linear-progress"],
    hostVars: 10,
    hostBindings: function MatProgressBar_HostBindings(rf, ctx) {
      if (rf & 2) {
        \u0275\u0275attribute("aria-valuenow", ctx._isIndeterminate() ? null : ctx.value)("mode", ctx.mode);
        \u0275\u0275classMap("mat-" + ctx.color);
        \u0275\u0275classProp("_mat-animation-noopable", ctx._isNoopAnimation)("mdc-linear-progress--animation-ready", !ctx._isNoopAnimation)("mdc-linear-progress--indeterminate", ctx._isIndeterminate());
      }
    },
    inputs: {
      color: "color",
      value: [2, "value", "value", numberAttribute],
      bufferValue: [2, "bufferValue", "bufferValue", numberAttribute],
      mode: "mode"
    },
    outputs: {
      animationEnd: "animationEnd"
    },
    exportAs: ["matProgressBar"],
    decls: 7,
    vars: 5,
    consts: [["aria-hidden", "true", 1, "mdc-linear-progress__buffer"], [1, "mdc-linear-progress__buffer-bar"], [1, "mdc-linear-progress__buffer-dots"], ["aria-hidden", "true", 1, "mdc-linear-progress__bar", "mdc-linear-progress__primary-bar"], [1, "mdc-linear-progress__bar-inner"], ["aria-hidden", "true", 1, "mdc-linear-progress__bar", "mdc-linear-progress__secondary-bar"]],
    template: function MatProgressBar_Template(rf, ctx) {
      if (rf & 1) {
        \u0275\u0275domElementStart(0, "div", 0);
        \u0275\u0275domElement(1, "div", 1);
        \u0275\u0275conditionalCreate(2, MatProgressBar_Conditional_2_Template, 1, 0, "div", 2);
        \u0275\u0275domElementEnd();
        \u0275\u0275domElementStart(3, "div", 3);
        \u0275\u0275domElement(4, "span", 4);
        \u0275\u0275domElementEnd();
        \u0275\u0275domElementStart(5, "div", 5);
        \u0275\u0275domElement(6, "span", 4);
        \u0275\u0275domElementEnd();
      }
      if (rf & 2) {
        \u0275\u0275advance();
        \u0275\u0275styleProp("flex-basis", ctx._getBufferBarFlexBasis());
        \u0275\u0275advance();
        \u0275\u0275conditional(ctx.mode === "buffer" ? 2 : -1);
        \u0275\u0275advance();
        \u0275\u0275styleProp("transform", ctx._getPrimaryBarTransform());
      }
    },
    styles: [".mat-mdc-progress-bar{--mat-progress-bar-animation-multiplier: 1;display:block;text-align:start}.mat-mdc-progress-bar[mode=query]{transform:scaleX(-1)}.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__buffer-dots,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__primary-bar,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__secondary-bar,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__bar-inner.mdc-linear-progress__bar-inner{animation:none}.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__primary-bar,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__buffer-bar{transition:transform 1ms}.mat-progress-bar-reduced-motion{--mat-progress-bar-animation-multiplier: 2}.mdc-linear-progress{position:relative;width:100%;transform:translateZ(0);outline:1px solid rgba(0,0,0,0);overflow-x:hidden;transition:opacity 250ms 0ms cubic-bezier(0.4, 0, 0.6, 1);height:max(var(--mat-progress-bar-track-height, 4px),var(--mat-progress-bar-active-indicator-height, 4px))}@media(forced-colors: active){.mdc-linear-progress{outline-color:CanvasText}}.mdc-linear-progress__bar{position:absolute;top:0;bottom:0;margin:auto 0;width:100%;animation:none;transform-origin:top left;transition:transform 250ms 0ms cubic-bezier(0.4, 0, 0.6, 1);height:var(--mat-progress-bar-active-indicator-height, 4px)}.mdc-linear-progress--indeterminate .mdc-linear-progress__bar{transition:none}[dir=rtl] .mdc-linear-progress__bar{right:0;transform-origin:center right}.mdc-linear-progress__bar-inner{display:inline-block;position:absolute;width:100%;animation:none;border-top-style:solid;border-color:var(--mat-progress-bar-active-indicator-color, var(--mat-sys-primary));border-top-width:var(--mat-progress-bar-active-indicator-height, 4px)}.mdc-linear-progress__buffer{display:flex;position:absolute;top:0;bottom:0;margin:auto 0;width:100%;overflow:hidden;height:var(--mat-progress-bar-track-height, 4px);border-radius:var(--mat-progress-bar-track-shape, var(--mat-sys-corner-none))}.mdc-linear-progress__buffer-dots{background-image:radial-gradient(circle, var(--mat-progress-bar-track-color, var(--mat-sys-surface-variant)) calc(var(--mat-progress-bar-track-height, 4px) / 2), transparent 0);background-repeat:repeat-x;background-size:calc(calc(var(--mat-progress-bar-track-height, 4px) / 2)*5);background-position:left;flex:auto;transform:rotate(180deg);animation:mdc-linear-progress-buffering calc(250ms*var(--mat-progress-bar-animation-multiplier)) infinite linear}@media(forced-colors: active){.mdc-linear-progress__buffer-dots{background-color:ButtonBorder}}[dir=rtl] .mdc-linear-progress__buffer-dots{animation:mdc-linear-progress-buffering-reverse calc(250ms*var(--mat-progress-bar-animation-multiplier)) infinite linear;transform:rotate(0)}.mdc-linear-progress__buffer-bar{flex:0 1 100%;transition:flex-basis 250ms 0ms cubic-bezier(0.4, 0, 0.6, 1);background-color:var(--mat-progress-bar-track-color, var(--mat-sys-surface-variant))}.mdc-linear-progress__primary-bar{transform:scaleX(0)}.mdc-linear-progress--indeterminate .mdc-linear-progress__primary-bar{left:-145.166611%}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__primary-bar{animation:mdc-linear-progress-primary-indeterminate-translate calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__primary-bar>.mdc-linear-progress__bar-inner{animation:mdc-linear-progress-primary-indeterminate-scale calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--animation-ready .mdc-linear-progress__primary-bar{animation-name:mdc-linear-progress-primary-indeterminate-translate-reverse}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--indeterminate .mdc-linear-progress__primary-bar{right:-145.166611%;left:auto}.mdc-linear-progress__secondary-bar{display:none}.mdc-linear-progress--indeterminate .mdc-linear-progress__secondary-bar{left:-54.888891%;display:block}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__secondary-bar{animation:mdc-linear-progress-secondary-indeterminate-translate calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__secondary-bar>.mdc-linear-progress__bar-inner{animation:mdc-linear-progress-secondary-indeterminate-scale calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--animation-ready .mdc-linear-progress__secondary-bar{animation-name:mdc-linear-progress-secondary-indeterminate-translate-reverse}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--indeterminate .mdc-linear-progress__secondary-bar{right:-54.888891%;left:auto}@keyframes mdc-linear-progress-buffering{from{transform:rotate(180deg) translateX(calc(var(--mat-progress-bar-track-height, 4px) * -2.5))}}@keyframes mdc-linear-progress-primary-indeterminate-translate{0%{transform:translateX(0)}20%{animation-timing-function:cubic-bezier(0.5, 0, 0.701732, 0.495819);transform:translateX(0)}59.15%{animation-timing-function:cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);transform:translateX(83.67142%)}100%{transform:translateX(200.611057%)}}@keyframes mdc-linear-progress-primary-indeterminate-scale{0%{transform:scaleX(0.08)}36.65%{animation-timing-function:cubic-bezier(0.334731, 0.12482, 0.785844, 1);transform:scaleX(0.08)}69.15%{animation-timing-function:cubic-bezier(0.06, 0.11, 0.6, 1);transform:scaleX(0.661479)}100%{transform:scaleX(0.08)}}@keyframes mdc-linear-progress-secondary-indeterminate-translate{0%{animation-timing-function:cubic-bezier(0.15, 0, 0.515058, 0.409685);transform:translateX(0)}25%{animation-timing-function:cubic-bezier(0.31033, 0.284058, 0.8, 0.733712);transform:translateX(37.651913%)}48.35%{animation-timing-function:cubic-bezier(0.4, 0.627035, 0.6, 0.902026);transform:translateX(84.386165%)}100%{transform:translateX(160.277782%)}}@keyframes mdc-linear-progress-secondary-indeterminate-scale{0%{animation-timing-function:cubic-bezier(0.205028, 0.057051, 0.57661, 0.453971);transform:scaleX(0.08)}19.15%{animation-timing-function:cubic-bezier(0.152313, 0.196432, 0.648374, 1.004315);transform:scaleX(0.457104)}44.15%{animation-timing-function:cubic-bezier(0.257759, -0.003163, 0.211762, 1.38179);transform:scaleX(0.72796)}100%{transform:scaleX(0.08)}}@keyframes mdc-linear-progress-primary-indeterminate-translate-reverse{0%{transform:translateX(0)}20%{animation-timing-function:cubic-bezier(0.5, 0, 0.701732, 0.495819);transform:translateX(0)}59.15%{animation-timing-function:cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);transform:translateX(-83.67142%)}100%{transform:translateX(-200.611057%)}}@keyframes mdc-linear-progress-secondary-indeterminate-translate-reverse{0%{animation-timing-function:cubic-bezier(0.15, 0, 0.515058, 0.409685);transform:translateX(0)}25%{animation-timing-function:cubic-bezier(0.31033, 0.284058, 0.8, 0.733712);transform:translateX(-37.651913%)}48.35%{animation-timing-function:cubic-bezier(0.4, 0.627035, 0.6, 0.902026);transform:translateX(-84.386165%)}100%{transform:translateX(-160.277782%)}}@keyframes mdc-linear-progress-buffering-reverse{from{transform:translateX(-10px)}}\n"],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MatProgressBar, [{
    type: Component,
    args: [{
      selector: "mat-progress-bar",
      exportAs: "matProgressBar",
      host: {
        "role": "progressbar",
        "aria-valuemin": "0",
        "aria-valuemax": "100",
        "tabindex": "-1",
        "[attr.aria-valuenow]": "_isIndeterminate() ? null : value",
        "[attr.mode]": "mode",
        "class": "mat-mdc-progress-bar mdc-linear-progress",
        "[class]": '"mat-" + color',
        "[class._mat-animation-noopable]": "_isNoopAnimation",
        "[class.mdc-linear-progress--animation-ready]": "!_isNoopAnimation",
        "[class.mdc-linear-progress--indeterminate]": "_isIndeterminate()"
      },
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      template: `<!--
  All children need to be hidden for screen readers in order to support ChromeVox.
  More context in the issue: https://github.com/angular/components/issues/22165.
-->
<div class="mdc-linear-progress__buffer" aria-hidden="true">
  <div
    class="mdc-linear-progress__buffer-bar"
    [style.flex-basis]="_getBufferBarFlexBasis()"></div>
  <!-- Remove the dots outside of buffer mode since they can cause CSP issues (see #28938) -->
  @if (mode === 'buffer') {
    <div class="mdc-linear-progress__buffer-dots"></div>
  }
</div>
<div
  class="mdc-linear-progress__bar mdc-linear-progress__primary-bar"
  aria-hidden="true"
  [style.transform]="_getPrimaryBarTransform()">
  <span class="mdc-linear-progress__bar-inner"></span>
</div>
<div class="mdc-linear-progress__bar mdc-linear-progress__secondary-bar" aria-hidden="true">
  <span class="mdc-linear-progress__bar-inner"></span>
</div>
`,
      styles: [".mat-mdc-progress-bar{--mat-progress-bar-animation-multiplier: 1;display:block;text-align:start}.mat-mdc-progress-bar[mode=query]{transform:scaleX(-1)}.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__buffer-dots,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__primary-bar,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__secondary-bar,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__bar-inner.mdc-linear-progress__bar-inner{animation:none}.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__primary-bar,.mat-mdc-progress-bar._mat-animation-noopable .mdc-linear-progress__buffer-bar{transition:transform 1ms}.mat-progress-bar-reduced-motion{--mat-progress-bar-animation-multiplier: 2}.mdc-linear-progress{position:relative;width:100%;transform:translateZ(0);outline:1px solid rgba(0,0,0,0);overflow-x:hidden;transition:opacity 250ms 0ms cubic-bezier(0.4, 0, 0.6, 1);height:max(var(--mat-progress-bar-track-height, 4px),var(--mat-progress-bar-active-indicator-height, 4px))}@media(forced-colors: active){.mdc-linear-progress{outline-color:CanvasText}}.mdc-linear-progress__bar{position:absolute;top:0;bottom:0;margin:auto 0;width:100%;animation:none;transform-origin:top left;transition:transform 250ms 0ms cubic-bezier(0.4, 0, 0.6, 1);height:var(--mat-progress-bar-active-indicator-height, 4px)}.mdc-linear-progress--indeterminate .mdc-linear-progress__bar{transition:none}[dir=rtl] .mdc-linear-progress__bar{right:0;transform-origin:center right}.mdc-linear-progress__bar-inner{display:inline-block;position:absolute;width:100%;animation:none;border-top-style:solid;border-color:var(--mat-progress-bar-active-indicator-color, var(--mat-sys-primary));border-top-width:var(--mat-progress-bar-active-indicator-height, 4px)}.mdc-linear-progress__buffer{display:flex;position:absolute;top:0;bottom:0;margin:auto 0;width:100%;overflow:hidden;height:var(--mat-progress-bar-track-height, 4px);border-radius:var(--mat-progress-bar-track-shape, var(--mat-sys-corner-none))}.mdc-linear-progress__buffer-dots{background-image:radial-gradient(circle, var(--mat-progress-bar-track-color, var(--mat-sys-surface-variant)) calc(var(--mat-progress-bar-track-height, 4px) / 2), transparent 0);background-repeat:repeat-x;background-size:calc(calc(var(--mat-progress-bar-track-height, 4px) / 2)*5);background-position:left;flex:auto;transform:rotate(180deg);animation:mdc-linear-progress-buffering calc(250ms*var(--mat-progress-bar-animation-multiplier)) infinite linear}@media(forced-colors: active){.mdc-linear-progress__buffer-dots{background-color:ButtonBorder}}[dir=rtl] .mdc-linear-progress__buffer-dots{animation:mdc-linear-progress-buffering-reverse calc(250ms*var(--mat-progress-bar-animation-multiplier)) infinite linear;transform:rotate(0)}.mdc-linear-progress__buffer-bar{flex:0 1 100%;transition:flex-basis 250ms 0ms cubic-bezier(0.4, 0, 0.6, 1);background-color:var(--mat-progress-bar-track-color, var(--mat-sys-surface-variant))}.mdc-linear-progress__primary-bar{transform:scaleX(0)}.mdc-linear-progress--indeterminate .mdc-linear-progress__primary-bar{left:-145.166611%}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__primary-bar{animation:mdc-linear-progress-primary-indeterminate-translate calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__primary-bar>.mdc-linear-progress__bar-inner{animation:mdc-linear-progress-primary-indeterminate-scale calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--animation-ready .mdc-linear-progress__primary-bar{animation-name:mdc-linear-progress-primary-indeterminate-translate-reverse}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--indeterminate .mdc-linear-progress__primary-bar{right:-145.166611%;left:auto}.mdc-linear-progress__secondary-bar{display:none}.mdc-linear-progress--indeterminate .mdc-linear-progress__secondary-bar{left:-54.888891%;display:block}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__secondary-bar{animation:mdc-linear-progress-secondary-indeterminate-translate calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}.mdc-linear-progress--indeterminate.mdc-linear-progress--animation-ready .mdc-linear-progress__secondary-bar>.mdc-linear-progress__bar-inner{animation:mdc-linear-progress-secondary-indeterminate-scale calc(2s*var(--mat-progress-bar-animation-multiplier)) infinite linear}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--animation-ready .mdc-linear-progress__secondary-bar{animation-name:mdc-linear-progress-secondary-indeterminate-translate-reverse}[dir=rtl] .mdc-linear-progress.mdc-linear-progress--indeterminate .mdc-linear-progress__secondary-bar{right:-54.888891%;left:auto}@keyframes mdc-linear-progress-buffering{from{transform:rotate(180deg) translateX(calc(var(--mat-progress-bar-track-height, 4px) * -2.5))}}@keyframes mdc-linear-progress-primary-indeterminate-translate{0%{transform:translateX(0)}20%{animation-timing-function:cubic-bezier(0.5, 0, 0.701732, 0.495819);transform:translateX(0)}59.15%{animation-timing-function:cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);transform:translateX(83.67142%)}100%{transform:translateX(200.611057%)}}@keyframes mdc-linear-progress-primary-indeterminate-scale{0%{transform:scaleX(0.08)}36.65%{animation-timing-function:cubic-bezier(0.334731, 0.12482, 0.785844, 1);transform:scaleX(0.08)}69.15%{animation-timing-function:cubic-bezier(0.06, 0.11, 0.6, 1);transform:scaleX(0.661479)}100%{transform:scaleX(0.08)}}@keyframes mdc-linear-progress-secondary-indeterminate-translate{0%{animation-timing-function:cubic-bezier(0.15, 0, 0.515058, 0.409685);transform:translateX(0)}25%{animation-timing-function:cubic-bezier(0.31033, 0.284058, 0.8, 0.733712);transform:translateX(37.651913%)}48.35%{animation-timing-function:cubic-bezier(0.4, 0.627035, 0.6, 0.902026);transform:translateX(84.386165%)}100%{transform:translateX(160.277782%)}}@keyframes mdc-linear-progress-secondary-indeterminate-scale{0%{animation-timing-function:cubic-bezier(0.205028, 0.057051, 0.57661, 0.453971);transform:scaleX(0.08)}19.15%{animation-timing-function:cubic-bezier(0.152313, 0.196432, 0.648374, 1.004315);transform:scaleX(0.457104)}44.15%{animation-timing-function:cubic-bezier(0.257759, -0.003163, 0.211762, 1.38179);transform:scaleX(0.72796)}100%{transform:scaleX(0.08)}}@keyframes mdc-linear-progress-primary-indeterminate-translate-reverse{0%{transform:translateX(0)}20%{animation-timing-function:cubic-bezier(0.5, 0, 0.701732, 0.495819);transform:translateX(0)}59.15%{animation-timing-function:cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);transform:translateX(-83.67142%)}100%{transform:translateX(-200.611057%)}}@keyframes mdc-linear-progress-secondary-indeterminate-translate-reverse{0%{animation-timing-function:cubic-bezier(0.15, 0, 0.515058, 0.409685);transform:translateX(0)}25%{animation-timing-function:cubic-bezier(0.31033, 0.284058, 0.8, 0.733712);transform:translateX(-37.651913%)}48.35%{animation-timing-function:cubic-bezier(0.4, 0.627035, 0.6, 0.902026);transform:translateX(-84.386165%)}100%{transform:translateX(-160.277782%)}}@keyframes mdc-linear-progress-buffering-reverse{from{transform:translateX(-10px)}}\n"]
    }]
  }], () => [], {
    color: [{
      type: Input
    }],
    value: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    bufferValue: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    animationEnd: [{
      type: Output
    }],
    mode: [{
      type: Input
    }]
  });
})();
function clamp(v, min = 0, max = 100) {
  return Math.max(min, Math.min(max, v));
}
var MatProgressBarModule = class _MatProgressBarModule {
  static \u0275fac = function MatProgressBarModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MatProgressBarModule)();
  };
  static \u0275mod = /* @__PURE__ */ \u0275\u0275defineNgModule({
    type: _MatProgressBarModule,
    imports: [MatProgressBar],
    exports: [MatProgressBar, BidiModule]
  });
  static \u0275inj = /* @__PURE__ */ \u0275\u0275defineInjector({
    imports: [BidiModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MatProgressBarModule, [{
    type: NgModule,
    args: [{
      imports: [MatProgressBar],
      exports: [MatProgressBar, BidiModule]
    }]
  }], null, null);
})();

// src/app/pages/dashboard/project-status.ts
var RETICULATE_MODULES = [
  {
    "name": "parser",
    "description": "AST nodes, tokenizer, recursive-descent parser",
    "tests": 93,
    "status": "complete"
  },
  {
    "name": "statespace",
    "description": "State-space construction from session types",
    "tests": 50,
    "status": "complete"
  },
  {
    "name": "product",
    "description": "Product construction for parallel (\u2225)",
    "tests": 0,
    "status": "complete"
  },
  {
    "name": "lattice",
    "description": "Lattice property checking (meets, joins)",
    "tests": 53,
    "status": "complete"
  },
  {
    "name": "termination",
    "description": "Termination checking + WF-Par",
    "tests": 56,
    "status": "complete"
  },
  {
    "name": "morphism",
    "description": "Morphism hierarchy (iso, embedding, projection, Galois)",
    "tests": 38,
    "status": "complete"
  },
  {
    "name": "visualize",
    "description": "Hasse diagram generation (DOT/graphviz)",
    "tests": 26,
    "status": "complete"
  },
  {
    "name": "testgen",
    "description": "Test generation from state spaces",
    "tests": 62,
    "status": "complete"
  },
  {
    "name": "cli",
    "description": "CLI entry point (argparse)",
    "tests": 27,
    "status": "complete"
  }
];
var BICA_PHASES = [
  {
    "name": "Phase 1: AST + Parser",
    "description": "Sealed AST, tokenizer, parser, pretty-printer",
    "tests": 118,
    "status": "complete"
  },
  {
    "name": "Phase 2: StateSpace + Lattice",
    "description": "State space builder, lattice checker, termination",
    "tests": 170,
    "status": "complete"
  },
  {
    "name": "Phase 3: Concurrency + Annotations",
    "description": "Thread safety, @Session/@Shared/@ReadOnly/@Exclusive",
    "tests": 173,
    "status": "complete"
  },
  {
    "name": "Phase 4: Morphisms",
    "description": "Morphism hierarchy between state spaces",
    "tests": 88,
    "status": "complete"
  },
  {
    "name": "Phase 5: CLI",
    "description": "Standalone CLI tool with DOT output",
    "tests": 75,
    "status": "complete"
  },
  {
    "name": "Phase 6: Typestate",
    "description": "Typestate checking with selection auto-advance",
    "tests": 48,
    "status": "complete"
  },
  {
    "name": "Phase 7: Test Generation",
    "description": "JUnit 5 test generation from @Session",
    "tests": 90,
    "status": "complete"
  }
];
var LEAN_PROOFS = [
  {
    "name": "Bottom Absorption (Lemma 5)",
    "description": "L'(D) is a lattice when D absorbs bottom",
    "sorryCount": 0,
    "status": "complete"
  },
  {
    "name": "Recursion Lemma (Lemma 6)",
    "description": "SCC quotient preserves lattice structure",
    "sorryCount": 0,
    "status": "complete"
  },
  {
    "name": "Main Theorem",
    "description": "Session type state spaces form lattices",
    "sorryCount": -1,
    "status": "planned"
  }
];
var PAPERS = [
  {
    "title": "State-Space Construction for Finite Binary Session Types",
    "shortName": "Step 1",
    "target": "arXiv",
    "deadline": null,
    "status": "draft",
    "pages": 15
  },
  {
    "title": "Session Type State Spaces Form Lattices",
    "shortName": "Step 5",
    "target": "CONCUR 2026",
    "deadline": "2026-04-27",
    "status": "draft",
    "pages": 23
  },
  {
    "title": "Reticulate: Lattice Analysis of Session Type State Spaces",
    "shortName": "ICE oral",
    "target": "ICE 2026",
    "deadline": "2026-04-02",
    "status": "draft",
    "pages": 2
  },
  {
    "title": "Reticulate: A Lattice Verification Tool for Session Types",
    "shortName": "Tool paper",
    "target": "TACAS 2027",
    "deadline": "2026-10-15",
    "status": "draft",
    "pages": 13
  },
  {
    "title": "BICA Reborn: Annotation-Based Session Type Checker for Java",
    "shortName": "BICA paper",
    "target": "OOPSLA 2027",
    "deadline": "2026-10-15",
    "status": "draft",
    "pages": 20
  },
  {
    "title": "Definitions and Glossary",
    "shortName": "Glossary",
    "target": "Internal",
    "deadline": null,
    "status": "draft",
    "pages": 17
  }
];
var MILESTONES = [
  {
    "label": "Reticulate Python library complete",
    "date": "2026-02",
    "done": true
  },
  {
    "label": "BICA Reborn Java complete (7 phases)",
    "date": "2026-02",
    "done": true
  },
  {
    "label": "Lean 4: Bottom Absorption proved",
    "date": "2026-02",
    "done": true
  },
  {
    "label": "Lean 4: Recursion Lemma proved",
    "date": "2026-03",
    "done": true
  },
  {
    "label": "34 benchmarks validated",
    "date": "2026-03",
    "done": true
  },
  {
    "label": "Website live (bica.zuacaldeira.com)",
    "date": "2026-03",
    "done": true
  },
  {
    "label": "ICE 2026 submission",
    "date": "2026-04-02",
    "done": false
  },
  {
    "label": "CONCUR 2026 abstract",
    "date": "2026-04-20",
    "done": false
  },
  {
    "label": "CONCUR 2026 full paper",
    "date": "2026-04-27",
    "done": false
  },
  {
    "label": "LICS/FLOC 2026 attend (Lisbon)",
    "date": "2026-07-20",
    "done": false
  },
  {
    "label": "Lean 4: Main Theorem",
    "date": "2026-06",
    "done": false
  },
  {
    "label": "TACAS 2027 tool paper",
    "date": "2026-10",
    "done": false
  },
  {
    "label": "OOPSLA 2027 BICA paper",
    "date": "2026-10",
    "done": false
  }
];
var SUMMARY = {
  "totalPythonTests": 789,
  "totalJavaTests": 1052,
  "totalBenchmarks": 34,
  "benchmarksWithParallel": 13,
  "leanSorryCount": 0,
  "pythonModules": 9,
  "javaPackages": 13,
  "javaSourceFiles": 52,
  "javaSourceLines": 4800,
  "javaTestLines": 8500,
  "generatedTests": 5183
};

// src/app/pages/dashboard/dashboard.component.ts
var _forTrack0 = ($index, $item) => $item.label;
var _forTrack1 = ($index, $item) => $item.name;
var _forTrack2 = ($index, $item) => $item.shortName;
function DashboardComponent_For_48_Conditional_2_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0);
  }
  if (rf & 2) {
    const m_r1 = \u0275\u0275nextContext().$implicit;
    const ctx_r1 = \u0275\u0275nextContext();
    \u0275\u0275textInterpolate1(" ", ctx_r1.daysUntil(m_r1.date), "d ");
  }
}
function DashboardComponent_For_48_Conditional_3_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275text(0, " overdue ");
  }
}
function DashboardComponent_For_48_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 18)(1, "div", 19);
    \u0275\u0275conditionalCreate(2, DashboardComponent_For_48_Conditional_2_Template, 1, 1)(3, DashboardComponent_For_48_Conditional_3_Template, 1, 0);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "div", 20)(5, "div", 21);
    \u0275\u0275text(6);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "div", 22);
    \u0275\u0275text(8);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const m_r1 = ctx.$implicit;
    const ctx_r1 = \u0275\u0275nextContext();
    \u0275\u0275classProp("overdue", ctx_r1.daysUntil(m_r1.date) < 0);
    \u0275\u0275advance();
    \u0275\u0275classProp("urgent", ctx_r1.daysUntil(m_r1.date) <= 14 && ctx_r1.daysUntil(m_r1.date) >= 0);
    \u0275\u0275advance();
    \u0275\u0275conditional(ctx_r1.daysUntil(m_r1.date) >= 0 ? 2 : 3);
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate(m_r1.label);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(m_r1.date);
  }
}
function DashboardComponent_For_57_Conditional_6_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-chip-set")(1, "mat-chip");
    \u0275\u0275text(2);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const mod_r3 = \u0275\u0275nextContext().$implicit;
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", mod_r3.tests, " tests");
  }
}
function DashboardComponent_For_57_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 13)(1, "div", 23)(2, "mat-icon", 24);
    \u0275\u0275text(3, "check_circle");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "span", 25);
    \u0275\u0275text(5);
    \u0275\u0275elementEnd();
    \u0275\u0275conditionalCreate(6, DashboardComponent_For_57_Conditional_6_Template, 3, 1, "mat-chip-set");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(7, "div", 26);
    \u0275\u0275text(8);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const mod_r3 = ctx.$implicit;
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(mod_r3.name);
    \u0275\u0275advance();
    \u0275\u0275conditional(mod_r3.tests > 0 ? 6 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(mod_r3.description);
  }
}
function DashboardComponent_For_66_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 13)(1, "div", 23)(2, "mat-icon", 24);
    \u0275\u0275text(3, "check_circle");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(4, "span", 25);
    \u0275\u0275text(5);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(6, "mat-chip-set")(7, "mat-chip");
    \u0275\u0275text(8);
    \u0275\u0275elementEnd()()();
    \u0275\u0275elementStart(9, "div", 26);
    \u0275\u0275text(10);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const phase_r4 = ctx.$implicit;
    \u0275\u0275advance(5);
    \u0275\u0275textInterpolate(phase_r4.name);
    \u0275\u0275advance(3);
    \u0275\u0275textInterpolate1("", phase_r4.tests, " tests");
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(phase_r4.description);
  }
}
function DashboardComponent_For_75_Conditional_2_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-icon", 24);
    \u0275\u0275text(1, "check_circle");
    \u0275\u0275elementEnd();
  }
}
function DashboardComponent_For_75_Conditional_3_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-icon", 27);
    \u0275\u0275text(1, "pending");
    \u0275\u0275elementEnd();
  }
}
function DashboardComponent_For_75_Conditional_4_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-icon", 28);
    \u0275\u0275text(1, "radio_button_unchecked");
    \u0275\u0275elementEnd();
  }
}
function DashboardComponent_For_75_Conditional_7_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-chip-set")(1, "mat-chip");
    \u0275\u0275text(2);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const proof_r5 = \u0275\u0275nextContext().$implicit;
    \u0275\u0275advance();
    \u0275\u0275classProp("sorry-zero", proof_r5.sorryCount === 0);
    \u0275\u0275advance();
    \u0275\u0275textInterpolate1(" ", proof_r5.sorryCount, " sorry ");
  }
}
function DashboardComponent_For_75_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 13)(1, "div", 23);
    \u0275\u0275conditionalCreate(2, DashboardComponent_For_75_Conditional_2_Template, 2, 0, "mat-icon", 24)(3, DashboardComponent_For_75_Conditional_3_Template, 2, 0, "mat-icon", 27)(4, DashboardComponent_For_75_Conditional_4_Template, 2, 0, "mat-icon", 28);
    \u0275\u0275elementStart(5, "span", 25);
    \u0275\u0275text(6);
    \u0275\u0275elementEnd();
    \u0275\u0275conditionalCreate(7, DashboardComponent_For_75_Conditional_7_Template, 3, 3, "mat-chip-set");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(8, "div", 26);
    \u0275\u0275text(9);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const proof_r5 = ctx.$implicit;
    \u0275\u0275advance(2);
    \u0275\u0275conditional(proof_r5.status === "complete" ? 2 : proof_r5.status === "in-progress" ? 3 : 4);
    \u0275\u0275advance(4);
    \u0275\u0275textInterpolate(proof_r5.name);
    \u0275\u0275advance();
    \u0275\u0275conditional(proof_r5.sorryCount >= 0 ? 7 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(proof_r5.description);
  }
}
function DashboardComponent_For_83_For_4_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 36);
    \u0275\u0275text(1);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const paper_r6 = \u0275\u0275nextContext().$implicit;
    const ctx_r1 = \u0275\u0275nextContext(2);
    \u0275\u0275classProp("urgent", ctx_r1.daysUntil(paper_r6.deadline) <= 30 && ctx_r1.daysUntil(paper_r6.deadline) >= 0);
    \u0275\u0275advance();
    \u0275\u0275textInterpolate2(" ", paper_r6.deadline, " (", ctx_r1.daysUntil(paper_r6.deadline), "d) ");
  }
}
function DashboardComponent_For_83_For_4_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-card", 30)(1, "div", 32);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "div", 33);
    \u0275\u0275text(4);
    \u0275\u0275elementEnd();
    \u0275\u0275conditionalCreate(5, DashboardComponent_For_83_For_4_Conditional_5_Template, 2, 4, "div", 34);
    \u0275\u0275elementStart(6, "div", 35);
    \u0275\u0275text(7);
    \u0275\u0275elementEnd()();
  }
  if (rf & 2) {
    const paper_r6 = ctx.$implicit;
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(paper_r6.shortName);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(paper_r6.target);
    \u0275\u0275advance();
    \u0275\u0275conditional(paper_r6.deadline ? 5 : -1);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate1("", paper_r6.pages, " pp");
  }
}
function DashboardComponent_For_83_Conditional_5_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 31);
    \u0275\u0275text(1, "\u2014");
    \u0275\u0275elementEnd();
  }
}
function DashboardComponent_For_83_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 15)(1, "div", 29);
    \u0275\u0275text(2);
    \u0275\u0275elementEnd();
    \u0275\u0275repeaterCreate(3, DashboardComponent_For_83_For_4_Template, 8, 4, "mat-card", 30, _forTrack2);
    \u0275\u0275conditionalCreate(5, DashboardComponent_For_83_Conditional_5_Template, 2, 0, "div", 31);
    \u0275\u0275elementEnd();
  }
  if (rf & 2) {
    const col_r7 = ctx.$implicit;
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(col_r7.label);
    \u0275\u0275advance();
    \u0275\u0275repeater(col_r7.papers);
    \u0275\u0275advance(2);
    \u0275\u0275conditional(col_r7.papers.length === 0 ? 5 : -1);
  }
}
function DashboardComponent_For_91_Conditional_2_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "mat-icon");
    \u0275\u0275text(1, "check");
    \u0275\u0275elementEnd();
  }
}
function DashboardComponent_For_91_Template(rf, ctx) {
  if (rf & 1) {
    \u0275\u0275elementStart(0, "div", 37)(1, "div", 38);
    \u0275\u0275conditionalCreate(2, DashboardComponent_For_91_Conditional_2_Template, 2, 0, "mat-icon");
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(3, "div", 39)(4, "span", 40);
    \u0275\u0275text(5);
    \u0275\u0275elementEnd();
    \u0275\u0275elementStart(6, "span", 41);
    \u0275\u0275text(7);
    \u0275\u0275elementEnd()()();
  }
  if (rf & 2) {
    const m_r8 = ctx.$implicit;
    \u0275\u0275classProp("done", m_r8.done);
    \u0275\u0275advance(2);
    \u0275\u0275conditional(m_r8.done ? 2 : -1);
    \u0275\u0275advance(3);
    \u0275\u0275textInterpolate(m_r8.label);
    \u0275\u0275advance(2);
    \u0275\u0275textInterpolate(m_r8.date);
  }
}
var DashboardComponent = class _DashboardComponent {
  reticulateModules = RETICULATE_MODULES;
  bicaPhases = BICA_PHASES;
  leanProofs = LEAN_PROOFS;
  papers = PAPERS;
  milestones = MILESTONES;
  summary = SUMMARY;
  reticulateTests = RETICULATE_MODULES.reduce((s, m) => s + m.tests, 0);
  bicaTests = BICA_PHASES.reduce((s, m) => s + m.tests, 0);
  pctParallel = Math.round(SUMMARY.benchmarksWithParallel / SUMMARY.totalBenchmarks * 100);
  reticulateProgress = RETICULATE_MODULES.filter((m) => m.status === "complete").length / RETICULATE_MODULES.length * 100;
  bicaProgress = BICA_PHASES.filter((m) => m.status === "complete").length / BICA_PHASES.length * 100;
  leanProgress = LEAN_PROOFS.filter((m) => m.status === "complete").length / LEAN_PROOFS.length * 100;
  draftCount = PAPERS.filter((p) => p.status === "draft").length;
  submittedCount = PAPERS.filter((p) => p.status === "submitted").length;
  upcomingMilestones = MILESTONES.filter((m) => !m.done).slice(0, 5);
  paperColumns = [
    { label: "Draft", papers: PAPERS.filter((p) => p.status === "draft") },
    { label: "Submitted", papers: PAPERS.filter((p) => p.status === "submitted") },
    { label: "Under Review", papers: PAPERS.filter((p) => p.status === "under-review") },
    { label: "Accepted", papers: PAPERS.filter((p) => p.status === "accepted" || p.status === "published") }
  ];
  daysUntil(dateStr) {
    const target = new Date(dateStr);
    const now = /* @__PURE__ */ new Date();
    return Math.ceil((target.getTime() - now.getTime()) / (1e3 * 60 * 60 * 24));
  }
  static \u0275fac = function DashboardComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _DashboardComponent)();
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _DashboardComponent, selectors: [["app-dashboard"]], decls: 92, vars: 16, consts: [[1, "dashboard"], [1, "page-title"], [1, "page-subtitle"], [1, "summary-row"], [1, "stat-card"], [1, "stat-value"], [1, "stat-label"], [1, "stat-detail"], [1, "section"], [1, "deadline-cards"], [1, "deadline-card", 3, "overdue"], ["mode", "determinate", 3, "value"], [1, "module-grid"], [1, "module-card"], [1, "papers-kanban"], [1, "kanban-column"], [1, "timeline"], [1, "timeline-item", 3, "done"], [1, "deadline-card"], [1, "deadline-days"], [1, "deadline-info"], [1, "deadline-label"], [1, "deadline-date"], [1, "module-header"], [1, "status-icon", "complete"], [1, "module-name"], [1, "module-desc"], [1, "status-icon", "in-progress"], [1, "status-icon", "planned"], [1, "kanban-header"], [1, "paper-card"], [1, "kanban-empty"], [1, "paper-title"], [1, "paper-target"], [1, "paper-deadline", 3, "urgent"], [1, "paper-pages"], [1, "paper-deadline"], [1, "timeline-item"], [1, "timeline-dot"], [1, "timeline-content"], [1, "timeline-label"], [1, "timeline-date"]], template: function DashboardComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275elementStart(0, "div", 0)(1, "h1", 1);
      \u0275\u0275text(2, "Project Dashboard");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(3, "p", 2);
      \u0275\u0275text(4, "Session Types as Algebraic Reticulates \u2014 Research Overview");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(5, "section", 3)(6, "mat-card", 4)(7, "div", 5);
      \u0275\u0275text(8);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(9, "div", 6);
      \u0275\u0275text(10, "Total Tests");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(11, "div", 7);
      \u0275\u0275text(12);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(13, "mat-card", 4)(14, "div", 5);
      \u0275\u0275text(15);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(16, "div", 6);
      \u0275\u0275text(17, "Benchmarks");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(18, "div", 7);
      \u0275\u0275text(19);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(20, "mat-card", 4)(21, "div", 5);
      \u0275\u0275text(22);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(23, "div", 6);
      \u0275\u0275text(24, "Lean sorry");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(25, "div", 7);
      \u0275\u0275text(26, "2 lemmas fully proved");
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(27, "mat-card", 4)(28, "div", 5);
      \u0275\u0275text(29);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(30, "div", 6);
      \u0275\u0275text(31, "Papers");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(32, "div", 7);
      \u0275\u0275text(33);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(34, "mat-card", 4)(35, "div", 5);
      \u0275\u0275text(36);
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(37, "div", 6);
      \u0275\u0275text(38, "Generated Tests");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(39, "div", 7);
      \u0275\u0275text(40, "JUnit 5 from 34 protocols");
      \u0275\u0275elementEnd()()();
      \u0275\u0275elementStart(41, "section", 8)(42, "h2")(43, "mat-icon");
      \u0275\u0275text(44, "schedule");
      \u0275\u0275elementEnd();
      \u0275\u0275text(45, " Upcoming Deadlines ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(46, "div", 9);
      \u0275\u0275repeaterCreate(47, DashboardComponent_For_48_Template, 9, 7, "mat-card", 10, _forTrack0);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(49, "section", 8)(50, "h2")(51, "mat-icon");
      \u0275\u0275text(52, "code");
      \u0275\u0275elementEnd();
      \u0275\u0275text(53);
      \u0275\u0275elementEnd();
      \u0275\u0275element(54, "mat-progress-bar", 11);
      \u0275\u0275elementStart(55, "div", 12);
      \u0275\u0275repeaterCreate(56, DashboardComponent_For_57_Template, 9, 3, "mat-card", 13, _forTrack1);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(58, "section", 8)(59, "h2")(60, "mat-icon");
      \u0275\u0275text(61, "integration_instructions");
      \u0275\u0275elementEnd();
      \u0275\u0275text(62);
      \u0275\u0275elementEnd();
      \u0275\u0275element(63, "mat-progress-bar", 11);
      \u0275\u0275elementStart(64, "div", 12);
      \u0275\u0275repeaterCreate(65, DashboardComponent_For_66_Template, 11, 3, "mat-card", 13, _forTrack1);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(67, "section", 8)(68, "h2")(69, "mat-icon");
      \u0275\u0275text(70, "functions");
      \u0275\u0275elementEnd();
      \u0275\u0275text(71, " Lean 4 Formalization ");
      \u0275\u0275elementEnd();
      \u0275\u0275element(72, "mat-progress-bar", 11);
      \u0275\u0275elementStart(73, "div", 12);
      \u0275\u0275repeaterCreate(74, DashboardComponent_For_75_Template, 10, 4, "mat-card", 13, _forTrack1);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(76, "section", 8)(77, "h2")(78, "mat-icon");
      \u0275\u0275text(79, "article");
      \u0275\u0275elementEnd();
      \u0275\u0275text(80, " Papers Pipeline ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(81, "div", 14);
      \u0275\u0275repeaterCreate(82, DashboardComponent_For_83_Template, 6, 2, "div", 15, _forTrack0);
      \u0275\u0275elementEnd()();
      \u0275\u0275elementStart(84, "section", 8)(85, "h2")(86, "mat-icon");
      \u0275\u0275text(87, "flag");
      \u0275\u0275elementEnd();
      \u0275\u0275text(88, " Milestones ");
      \u0275\u0275elementEnd();
      \u0275\u0275elementStart(89, "div", 16);
      \u0275\u0275repeaterCreate(90, DashboardComponent_For_91_Template, 8, 5, "div", 17, _forTrack0);
      \u0275\u0275elementEnd()()();
    }
    if (rf & 2) {
      \u0275\u0275advance(8);
      \u0275\u0275textInterpolate(ctx.summary.totalPythonTests + ctx.summary.totalJavaTests);
      \u0275\u0275advance(4);
      \u0275\u0275textInterpolate2("", ctx.summary.totalPythonTests, " Python \xB7 ", ctx.summary.totalJavaTests, " Java");
      \u0275\u0275advance(3);
      \u0275\u0275textInterpolate(ctx.summary.totalBenchmarks);
      \u0275\u0275advance(4);
      \u0275\u0275textInterpolate2("", ctx.summary.benchmarksWithParallel, " with \u2225 (", ctx.pctParallel, "%)");
      \u0275\u0275advance(3);
      \u0275\u0275textInterpolate(ctx.summary.leanSorryCount);
      \u0275\u0275advance(7);
      \u0275\u0275textInterpolate(ctx.papers.length);
      \u0275\u0275advance(4);
      \u0275\u0275textInterpolate2("", ctx.draftCount, " draft \xB7 ", ctx.submittedCount, " submitted");
      \u0275\u0275advance(3);
      \u0275\u0275textInterpolate(ctx.summary.generatedTests);
      \u0275\u0275advance(11);
      \u0275\u0275repeater(ctx.upcomingMilestones);
      \u0275\u0275advance(6);
      \u0275\u0275textInterpolate1(" Reticulate (Python) \u2014 ", ctx.reticulateTests, " tests ");
      \u0275\u0275advance();
      \u0275\u0275property("value", ctx.reticulateProgress);
      \u0275\u0275advance(2);
      \u0275\u0275repeater(ctx.reticulateModules);
      \u0275\u0275advance(6);
      \u0275\u0275textInterpolate1(" BICA Reborn (Java) \u2014 ", ctx.bicaTests, " tests ");
      \u0275\u0275advance();
      \u0275\u0275property("value", ctx.bicaProgress);
      \u0275\u0275advance(2);
      \u0275\u0275repeater(ctx.bicaPhases);
      \u0275\u0275advance(7);
      \u0275\u0275property("value", ctx.leanProgress);
      \u0275\u0275advance(2);
      \u0275\u0275repeater(ctx.leanProofs);
      \u0275\u0275advance(8);
      \u0275\u0275repeater(ctx.paperColumns);
      \u0275\u0275advance(8);
      \u0275\u0275repeater(ctx.milestones);
    }
  }, dependencies: [MatCardModule, MatCard, MatIconModule, MatIcon, MatChipsModule, MatChip, MatChipSet, MatProgressBarModule, MatProgressBar, MatTableModule], styles: ['\n\n.dashboard[_ngcontent-%COMP%] {\n  max-width: 1100px;\n  margin: 0 auto;\n  padding: 32px 24px;\n}\n.page-title[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-subtitle[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0 0 32px;\n}\n.summary-row[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));\n  gap: 16px;\n  margin-bottom: 40px;\n}\n.stat-card[_ngcontent-%COMP%] {\n  padding: 20px;\n  text-align: center;\n}\n.stat-value[_ngcontent-%COMP%] {\n  font-size: 36px;\n  font-weight: 700;\n  color: var(--mat-sys-primary, #4338ca);\n}\n.stat-label[_ngcontent-%COMP%] {\n  font-size: 14px;\n  font-weight: 500;\n  margin-top: 4px;\n}\n.stat-detail[_ngcontent-%COMP%] {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  margin-top: 4px;\n}\n.section[_ngcontent-%COMP%] {\n  margin-bottom: 40px;\n}\n.section[_ngcontent-%COMP%]   h2[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n  font-size: 20px;\n  font-weight: 600;\n  margin-bottom: 16px;\n}\nmat-progress-bar[_ngcontent-%COMP%] {\n  margin-bottom: 16px;\n  border-radius: 4px;\n}\n.module-grid[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));\n  gap: 12px;\n}\n.module-card[_ngcontent-%COMP%] {\n  padding: 16px;\n}\n.module-header[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n}\n.module-name[_ngcontent-%COMP%] {\n  font-weight: 600;\n  flex: 1;\n}\n.module-desc[_ngcontent-%COMP%] {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n  margin-top: 8px;\n}\n.status-icon.complete[_ngcontent-%COMP%] {\n  color: #16a34a;\n}\n.status-icon.in-progress[_ngcontent-%COMP%] {\n  color: #d97706;\n}\n.status-icon.planned[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.3);\n}\n.sorry-zero[_ngcontent-%COMP%] {\n  background-color: #dcfce7 !important;\n  color: #16a34a !important;\n}\n.deadline-cards[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));\n  gap: 12px;\n}\n.deadline-card[_ngcontent-%COMP%] {\n  padding: 16px;\n  display: flex;\n  align-items: center;\n  gap: 16px;\n}\n.deadline-card.overdue[_ngcontent-%COMP%] {\n  border-left: 4px solid #dc2626;\n}\n.deadline-days[_ngcontent-%COMP%] {\n  font-size: 24px;\n  font-weight: 700;\n  min-width: 60px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.7);\n}\n.deadline-days.urgent[_ngcontent-%COMP%] {\n  color: #dc2626;\n}\n.deadline-info[_ngcontent-%COMP%] {\n  flex: 1;\n}\n.deadline-label[_ngcontent-%COMP%] {\n  font-weight: 500;\n  font-size: 14px;\n}\n.deadline-date[_ngcontent-%COMP%] {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n}\n.papers-kanban[_ngcontent-%COMP%] {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 12px;\n}\n.kanban-column[_ngcontent-%COMP%] {\n  background: rgba(0, 0, 0, 0.03);\n  border-radius: 8px;\n  padding: 12px;\n  min-height: 120px;\n}\n.kanban-header[_ngcontent-%COMP%] {\n  font-weight: 600;\n  font-size: 13px;\n  text-transform: uppercase;\n  letter-spacing: 0.05em;\n  color: rgba(0, 0, 0, 0.5);\n  margin-bottom: 12px;\n  text-align: center;\n}\n.kanban-empty[_ngcontent-%COMP%] {\n  text-align: center;\n  color: rgba(0, 0, 0, 0.2);\n  padding: 20px;\n}\n.paper-card[_ngcontent-%COMP%] {\n  padding: 12px;\n  margin-bottom: 8px;\n}\n.paper-title[_ngcontent-%COMP%] {\n  font-weight: 600;\n  font-size: 14px;\n}\n.paper-target[_ngcontent-%COMP%] {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  margin-top: 2px;\n}\n.paper-deadline[_ngcontent-%COMP%] {\n  font-size: 12px;\n  margin-top: 4px;\n  color: rgba(0, 0, 0, 0.6);\n}\n.paper-deadline.urgent[_ngcontent-%COMP%] {\n  color: #dc2626;\n  font-weight: 600;\n}\n.paper-pages[_ngcontent-%COMP%] {\n  font-size: 11px;\n  color: rgba(0, 0, 0, 0.4);\n  margin-top: 2px;\n}\n.timeline[_ngcontent-%COMP%] {\n  position: relative;\n  padding-left: 32px;\n}\n.timeline[_ngcontent-%COMP%]::before {\n  content: "";\n  position: absolute;\n  left: 11px;\n  top: 0;\n  bottom: 0;\n  width: 2px;\n  background: rgba(0, 0, 0, 0.12);\n}\n.timeline-item[_ngcontent-%COMP%] {\n  display: flex;\n  align-items: flex-start;\n  gap: 16px;\n  margin-bottom: 16px;\n  position: relative;\n}\n.timeline-dot[_ngcontent-%COMP%] {\n  width: 24px;\n  height: 24px;\n  border-radius: 50%;\n  background: rgba(0, 0, 0, 0.08);\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  position: absolute;\n  left: -32px;\n  z-index: 1;\n}\n.timeline-item.done[_ngcontent-%COMP%]   .timeline-dot[_ngcontent-%COMP%] {\n  background: #16a34a;\n  color: white;\n}\n.timeline-dot[_ngcontent-%COMP%]   mat-icon[_ngcontent-%COMP%] {\n  font-size: 16px;\n  width: 16px;\n  height: 16px;\n}\n.timeline-content[_ngcontent-%COMP%] {\n  display: flex;\n  justify-content: space-between;\n  width: 100%;\n  padding: 2px 0;\n}\n.timeline-label[_ngcontent-%COMP%] {\n  font-size: 14px;\n}\n.timeline-item.done[_ngcontent-%COMP%]   .timeline-label[_ngcontent-%COMP%] {\n  color: rgba(0, 0, 0, 0.5);\n}\n.timeline-date[_ngcontent-%COMP%] {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.4);\n  white-space: nowrap;\n  margin-left: 16px;\n}\n@media (max-width: 768px) {\n  .papers-kanban[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n  .summary-row[_ngcontent-%COMP%] {\n    grid-template-columns: repeat(2, 1fr);\n  }\n  .module-grid[_ngcontent-%COMP%] {\n    grid-template-columns: 1fr;\n  }\n}\n/*# sourceMappingURL=dashboard.component.css.map */'] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(DashboardComponent, [{
    type: Component,
    args: [{ selector: "app-dashboard", standalone: true, imports: [MatCardModule, MatIconModule, MatChipsModule, MatProgressBarModule, MatTableModule], template: `
    <div class="dashboard">
      <h1 class="page-title">Project Dashboard</h1>
      <p class="page-subtitle">Session Types as Algebraic Reticulates \u2014 Research Overview</p>

      <!-- Summary cards -->
      <section class="summary-row">
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.totalPythonTests + summary.totalJavaTests }}</div>
          <div class="stat-label">Total Tests</div>
          <div class="stat-detail">{{ summary.totalPythonTests }} Python \xB7 {{ summary.totalJavaTests }} Java</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.totalBenchmarks }}</div>
          <div class="stat-label">Benchmarks</div>
          <div class="stat-detail">{{ summary.benchmarksWithParallel }} with \u2225 ({{ pctParallel }}%)</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.leanSorryCount }}</div>
          <div class="stat-label">Lean sorry</div>
          <div class="stat-detail">2 lemmas fully proved</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ papers.length }}</div>
          <div class="stat-label">Papers</div>
          <div class="stat-detail">{{ draftCount }} draft \xB7 {{ submittedCount }} submitted</div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-value">{{ summary.generatedTests }}</div>
          <div class="stat-label">Generated Tests</div>
          <div class="stat-detail">JUnit 5 from 34 protocols</div>
        </mat-card>
      </section>

      <!-- Upcoming deadlines -->
      <section class="section">
        <h2>
          <mat-icon>schedule</mat-icon>
          Upcoming Deadlines
        </h2>
        <div class="deadline-cards">
          @for (m of upcomingMilestones; track m.label) {
            <mat-card class="deadline-card" [class.overdue]="daysUntil(m.date) < 0">
              <div class="deadline-days" [class.urgent]="daysUntil(m.date) <= 14 && daysUntil(m.date) >= 0">
                @if (daysUntil(m.date) >= 0) {
                  {{ daysUntil(m.date) }}d
                } @else {
                  overdue
                }
              </div>
              <div class="deadline-info">
                <div class="deadline-label">{{ m.label }}</div>
                <div class="deadline-date">{{ m.date }}</div>
              </div>
            </mat-card>
          }
        </div>
      </section>

      <!-- Reticulate Python -->
      <section class="section">
        <h2>
          <mat-icon>code</mat-icon>
          Reticulate (Python) \u2014 {{ reticulateTests }} tests
        </h2>
        <mat-progress-bar mode="determinate" [value]="reticulateProgress"></mat-progress-bar>
        <div class="module-grid">
          @for (mod of reticulateModules; track mod.name) {
            <mat-card class="module-card">
              <div class="module-header">
                <mat-icon class="status-icon complete">check_circle</mat-icon>
                <span class="module-name">{{ mod.name }}</span>
                @if (mod.tests > 0) {
                  <mat-chip-set>
                    <mat-chip>{{ mod.tests }} tests</mat-chip>
                  </mat-chip-set>
                }
              </div>
              <div class="module-desc">{{ mod.description }}</div>
            </mat-card>
          }
        </div>
      </section>

      <!-- BICA Reborn Java -->
      <section class="section">
        <h2>
          <mat-icon>integration_instructions</mat-icon>
          BICA Reborn (Java) \u2014 {{ bicaTests }} tests
        </h2>
        <mat-progress-bar mode="determinate" [value]="bicaProgress"></mat-progress-bar>
        <div class="module-grid">
          @for (phase of bicaPhases; track phase.name) {
            <mat-card class="module-card">
              <div class="module-header">
                <mat-icon class="status-icon complete">check_circle</mat-icon>
                <span class="module-name">{{ phase.name }}</span>
                <mat-chip-set>
                  <mat-chip>{{ phase.tests }} tests</mat-chip>
                </mat-chip-set>
              </div>
              <div class="module-desc">{{ phase.description }}</div>
            </mat-card>
          }
        </div>
      </section>

      <!-- Lean 4 -->
      <section class="section">
        <h2>
          <mat-icon>functions</mat-icon>
          Lean 4 Formalization
        </h2>
        <mat-progress-bar mode="determinate" [value]="leanProgress"></mat-progress-bar>
        <div class="module-grid">
          @for (proof of leanProofs; track proof.name) {
            <mat-card class="module-card">
              <div class="module-header">
                @if (proof.status === 'complete') {
                  <mat-icon class="status-icon complete">check_circle</mat-icon>
                } @else if (proof.status === 'in-progress') {
                  <mat-icon class="status-icon in-progress">pending</mat-icon>
                } @else {
                  <mat-icon class="status-icon planned">radio_button_unchecked</mat-icon>
                }
                <span class="module-name">{{ proof.name }}</span>
                @if (proof.sorryCount >= 0) {
                  <mat-chip-set>
                    <mat-chip [class.sorry-zero]="proof.sorryCount === 0">
                      {{ proof.sorryCount }} sorry
                    </mat-chip>
                  </mat-chip-set>
                }
              </div>
              <div class="module-desc">{{ proof.description }}</div>
            </mat-card>
          }
        </div>
      </section>

      <!-- Papers pipeline -->
      <section class="section">
        <h2>
          <mat-icon>article</mat-icon>
          Papers Pipeline
        </h2>
        <div class="papers-kanban">
          @for (col of paperColumns; track col.label) {
            <div class="kanban-column">
              <div class="kanban-header">{{ col.label }}</div>
              @for (paper of col.papers; track paper.shortName) {
                <mat-card class="paper-card">
                  <div class="paper-title">{{ paper.shortName }}</div>
                  <div class="paper-target">{{ paper.target }}</div>
                  @if (paper.deadline) {
                    <div class="paper-deadline"
                         [class.urgent]="daysUntil(paper.deadline) <= 30 && daysUntil(paper.deadline) >= 0">
                      {{ paper.deadline }} ({{ daysUntil(paper.deadline) }}d)
                    </div>
                  }
                  <div class="paper-pages">{{ paper.pages }} pp</div>
                </mat-card>
              }
              @if (col.papers.length === 0) {
                <div class="kanban-empty">\u2014</div>
              }
            </div>
          }
        </div>
      </section>

      <!-- Milestones timeline -->
      <section class="section">
        <h2>
          <mat-icon>flag</mat-icon>
          Milestones
        </h2>
        <div class="timeline">
          @for (m of milestones; track m.label) {
            <div class="timeline-item" [class.done]="m.done">
              <div class="timeline-dot">
                @if (m.done) {
                  <mat-icon>check</mat-icon>
                }
              </div>
              <div class="timeline-content">
                <span class="timeline-label">{{ m.label }}</span>
                <span class="timeline-date">{{ m.date }}</span>
              </div>
            </div>
          }
        </div>
      </section>
    </div>
  `, styles: ['/* angular:styles/component:scss;839c01429166859d19dbc458778c36a748fe5bb910fc37d93499466c1cc31941;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/pages/dashboard/dashboard.component.ts */\n.dashboard {\n  max-width: 1100px;\n  margin: 0 auto;\n  padding: 32px 24px;\n}\n.page-title {\n  font-size: 24px;\n  font-weight: 600;\n  margin: 0 0 8px;\n}\n.page-subtitle {\n  color: rgba(0, 0, 0, 0.6);\n  margin: 0 0 32px;\n}\n.summary-row {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));\n  gap: 16px;\n  margin-bottom: 40px;\n}\n.stat-card {\n  padding: 20px;\n  text-align: center;\n}\n.stat-value {\n  font-size: 36px;\n  font-weight: 700;\n  color: var(--mat-sys-primary, #4338ca);\n}\n.stat-label {\n  font-size: 14px;\n  font-weight: 500;\n  margin-top: 4px;\n}\n.stat-detail {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  margin-top: 4px;\n}\n.section {\n  margin-bottom: 40px;\n}\n.section h2 {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n  font-size: 20px;\n  font-weight: 600;\n  margin-bottom: 16px;\n}\nmat-progress-bar {\n  margin-bottom: 16px;\n  border-radius: 4px;\n}\n.module-grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));\n  gap: 12px;\n}\n.module-card {\n  padding: 16px;\n}\n.module-header {\n  display: flex;\n  align-items: center;\n  gap: 8px;\n}\n.module-name {\n  font-weight: 600;\n  flex: 1;\n}\n.module-desc {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.6);\n  margin-top: 8px;\n}\n.status-icon.complete {\n  color: #16a34a;\n}\n.status-icon.in-progress {\n  color: #d97706;\n}\n.status-icon.planned {\n  color: rgba(0, 0, 0, 0.3);\n}\n.sorry-zero {\n  background-color: #dcfce7 !important;\n  color: #16a34a !important;\n}\n.deadline-cards {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));\n  gap: 12px;\n}\n.deadline-card {\n  padding: 16px;\n  display: flex;\n  align-items: center;\n  gap: 16px;\n}\n.deadline-card.overdue {\n  border-left: 4px solid #dc2626;\n}\n.deadline-days {\n  font-size: 24px;\n  font-weight: 700;\n  min-width: 60px;\n  text-align: center;\n  color: rgba(0, 0, 0, 0.7);\n}\n.deadline-days.urgent {\n  color: #dc2626;\n}\n.deadline-info {\n  flex: 1;\n}\n.deadline-label {\n  font-weight: 500;\n  font-size: 14px;\n}\n.deadline-date {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n}\n.papers-kanban {\n  display: grid;\n  grid-template-columns: repeat(4, 1fr);\n  gap: 12px;\n}\n.kanban-column {\n  background: rgba(0, 0, 0, 0.03);\n  border-radius: 8px;\n  padding: 12px;\n  min-height: 120px;\n}\n.kanban-header {\n  font-weight: 600;\n  font-size: 13px;\n  text-transform: uppercase;\n  letter-spacing: 0.05em;\n  color: rgba(0, 0, 0, 0.5);\n  margin-bottom: 12px;\n  text-align: center;\n}\n.kanban-empty {\n  text-align: center;\n  color: rgba(0, 0, 0, 0.2);\n  padding: 20px;\n}\n.paper-card {\n  padding: 12px;\n  margin-bottom: 8px;\n}\n.paper-title {\n  font-weight: 600;\n  font-size: 14px;\n}\n.paper-target {\n  font-size: 12px;\n  color: rgba(0, 0, 0, 0.5);\n  margin-top: 2px;\n}\n.paper-deadline {\n  font-size: 12px;\n  margin-top: 4px;\n  color: rgba(0, 0, 0, 0.6);\n}\n.paper-deadline.urgent {\n  color: #dc2626;\n  font-weight: 600;\n}\n.paper-pages {\n  font-size: 11px;\n  color: rgba(0, 0, 0, 0.4);\n  margin-top: 2px;\n}\n.timeline {\n  position: relative;\n  padding-left: 32px;\n}\n.timeline::before {\n  content: "";\n  position: absolute;\n  left: 11px;\n  top: 0;\n  bottom: 0;\n  width: 2px;\n  background: rgba(0, 0, 0, 0.12);\n}\n.timeline-item {\n  display: flex;\n  align-items: flex-start;\n  gap: 16px;\n  margin-bottom: 16px;\n  position: relative;\n}\n.timeline-dot {\n  width: 24px;\n  height: 24px;\n  border-radius: 50%;\n  background: rgba(0, 0, 0, 0.08);\n  display: flex;\n  align-items: center;\n  justify-content: center;\n  position: absolute;\n  left: -32px;\n  z-index: 1;\n}\n.timeline-item.done .timeline-dot {\n  background: #16a34a;\n  color: white;\n}\n.timeline-dot mat-icon {\n  font-size: 16px;\n  width: 16px;\n  height: 16px;\n}\n.timeline-content {\n  display: flex;\n  justify-content: space-between;\n  width: 100%;\n  padding: 2px 0;\n}\n.timeline-label {\n  font-size: 14px;\n}\n.timeline-item.done .timeline-label {\n  color: rgba(0, 0, 0, 0.5);\n}\n.timeline-date {\n  font-size: 13px;\n  color: rgba(0, 0, 0, 0.4);\n  white-space: nowrap;\n  margin-left: 16px;\n}\n@media (max-width: 768px) {\n  .papers-kanban {\n    grid-template-columns: 1fr;\n  }\n  .summary-row {\n    grid-template-columns: repeat(2, 1fr);\n  }\n  .module-grid {\n    grid-template-columns: 1fr;\n  }\n}\n/*# sourceMappingURL=dashboard.component.css.map */\n'] }]
  }], null, null);
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(DashboardComponent, { className: "DashboardComponent", filePath: "src/app/pages/dashboard/dashboard.component.ts", lineNumber: 468 });
})();
export {
  DashboardComponent
};
//# sourceMappingURL=chunk-XFA4NJV3.js.map
