import {
  DomSanitizer
} from "./chunk-ZG4TCI7P.js";
import {
  Component,
  Input,
  setClassMetadata,
  ɵsetClassDebugInfo,
  ɵɵdefineComponent,
  ɵɵdirectiveInject,
  ɵɵdomElement,
  ɵɵdomProperty,
  ɵɵsanitizeHtml
} from "./chunk-OWEA7TR3.js";

// src/app/components/hasse-diagram/hasse-diagram.component.ts
var HasseDiagramComponent = class _HasseDiagramComponent {
  sanitizer;
  safeSvg = "";
  constructor(sanitizer) {
    this.sanitizer = sanitizer;
  }
  set svgHtml(value) {
    this.safeSvg = this.sanitizer.bypassSecurityTrustHtml(value || "");
  }
  static \u0275fac = function HasseDiagramComponent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _HasseDiagramComponent)(\u0275\u0275directiveInject(DomSanitizer));
  };
  static \u0275cmp = /* @__PURE__ */ \u0275\u0275defineComponent({ type: _HasseDiagramComponent, selectors: [["app-hasse-diagram"]], inputs: { svgHtml: "svgHtml" }, decls: 1, vars: 1, consts: [[1, "hasse-container", 3, "innerHTML"]], template: function HasseDiagramComponent_Template(rf, ctx) {
    if (rf & 1) {
      \u0275\u0275domElement(0, "div", 0);
    }
    if (rf & 2) {
      \u0275\u0275domProperty("innerHTML", ctx.safeSvg, \u0275\u0275sanitizeHtml);
    }
  }, styles: ["\n\n.hasse-container[_ngcontent-%COMP%] {\n  display: flex;\n  justify-content: center;\n  padding: 16px;\n  overflow: auto;\n}\n[_nghost-%COMP%]     svg {\n  max-width: 100%;\n  height: auto;\n}\n/*# sourceMappingURL=hasse-diagram.component.css.map */"] });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(HasseDiagramComponent, [{
    type: Component,
    args: [{ selector: "app-hasse-diagram", standalone: true, template: `
    <div class="hasse-container" [innerHTML]="safeSvg"></div>
  `, styles: ["/* angular:styles/component:scss;66e36864066b91ac92a8581ed33194560d36d064c0c64c125ff471de96dd7a14;/home/zuacaldeira/Development/SessionTypesResearch/web/frontend/src/app/components/hasse-diagram/hasse-diagram.component.ts */\n.hasse-container {\n  display: flex;\n  justify-content: center;\n  padding: 16px;\n  overflow: auto;\n}\n:host ::ng-deep svg {\n  max-width: 100%;\n  height: auto;\n}\n/*# sourceMappingURL=hasse-diagram.component.css.map */\n"] }]
  }], () => [{ type: DomSanitizer }], { svgHtml: [{
    type: Input
  }] });
})();
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && \u0275setClassDebugInfo(HasseDiagramComponent, { className: "HasseDiagramComponent", filePath: "src/app/components/hasse-diagram/hasse-diagram.component.ts", lineNumber: 23 });
})();

export {
  HasseDiagramComponent
};
//# sourceMappingURL=chunk-XXDSCLCV.js.map
