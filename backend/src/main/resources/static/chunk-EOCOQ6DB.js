import {
  HttpClient
} from "./chunk-ZG4TCI7P.js";
import {
  Injectable,
  setClassMetadata,
  ɵɵdefineInjectable,
  ɵɵinject
} from "./chunk-OWEA7TR3.js";

// src/app/services/api.service.ts
var ApiService = class _ApiService {
  http;
  baseUrl = "/api";
  constructor(http) {
    this.http = http;
  }
  analyze(typeString) {
    return this.http.post(`${this.baseUrl}/analyze`, {
      typeString
    });
  }
  generateTests(request) {
    return this.http.post(`${this.baseUrl}/test-gen`, request);
  }
  coverageStoryboard(typeString) {
    return this.http.post(`${this.baseUrl}/coverage-storyboard`, {
      typeString
    });
  }
  getBenchmarks() {
    return this.http.get(`${this.baseUrl}/benchmarks`);
  }
  getTutorials() {
    return this.http.get(`${this.baseUrl}/tutorials`);
  }
  getTutorial(id) {
    return this.http.get(`${this.baseUrl}/tutorials/${id}`);
  }
  analyzeGlobal(typeString) {
    return this.http.post(`${this.baseUrl}/analyze-global`, {
      typeString
    });
  }
  static \u0275fac = function ApiService_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ApiService)(\u0275\u0275inject(HttpClient));
  };
  static \u0275prov = /* @__PURE__ */ \u0275\u0275defineInjectable({ token: _ApiService, factory: _ApiService.\u0275fac, providedIn: "root" });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ApiService, [{
    type: Injectable,
    args: [{ providedIn: "root" }]
  }], () => [{ type: HttpClient }], null);
})();

export {
  ApiService
};
//# sourceMappingURL=chunk-EOCOQ6DB.js.map
