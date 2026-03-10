export interface AnalyzeRequest {
  typeString: string;
}

export interface AnalyzeResponse {
  pretty: string;
  numStates: number;
  numTransitions: number;
  numSccs: number;
  isLattice: boolean;
  counterexample: string | null;
  terminates: boolean;
  wfParallel: boolean;
  usesParallel: boolean;
  isRecursive: boolean;
  recDepth: number;
  methods: string[];
  numMethods: number;
  threadSafe: boolean;
  numTests: number;
  numValidPaths: number;
  numViolations: number;
  numIncomplete: number;
  svgHtml: string;
  dotSource: string;
}

export interface TestGenRequest {
  typeString: string;
  className: string;
  packageName?: string;
  maxRevisits?: number;
}

export interface TestGenResponse {
  testSource: string;
}

export interface HomeStats {
  numBenchmarks: number;
  totalStates: number;
  totalTests: number;
  allLattice: boolean;
}

export interface TutorialSummaryDto {
  id: string;
  number: number;
  title: string;
  subtitle: string;
}

export interface TutorialStepDto {
  title: string;
  prose: string;
  code: string | null;
  codeLabel: string | null;
}

export interface TutorialDto {
  id: string;
  number: number;
  title: string;
  subtitle: string;
  steps: TutorialStepDto[];
}

export interface CoverageFrameDto {
  testName: string;
  testKind: string;
  transitionCoverage: number;
  stateCoverage: number;
  svgHtml: string;
}

export interface CoverageStoryboardResponse {
  totalTransitions: number;
  totalStates: number;
  frames: CoverageFrameDto[];
}

export interface BenchmarkDto {
  name: string;
  description: string;
  typeString: string;
  pretty: string;
  numStates: number;
  numTransitions: number;
  numSccs: number;
  isLattice: boolean;
  usesParallel: boolean;
  isRecursive: boolean;
  recDepth: number;
  numMethods: number;
  methods: string[];
  threadSafe: boolean;
  numValidPaths: number;
  numViolations: number;
  numIncomplete: number;
  svgHtml: string;
  toolUrl: string;
  numTests: number;
}
