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

export interface GlobalAnalyzeRequest {
  typeString: string;
}

export interface ProjectionDto {
  role: string;
  localType: string;
  localStates: number;
  localTransitions: number;
  localIsLattice: boolean;
  localSvgHtml: string;
}

export interface GlobalAnalyzeResponse {
  pretty: string;
  numStates: number;
  numTransitions: number;
  numSccs: number;
  isLattice: boolean;
  counterexample: string | null;
  roles: string[];
  numRoles: number;
  usesParallel: boolean;
  isRecursive: boolean;
  svgHtml: string;
  dotSource: string;
  projections: { [role: string]: ProjectionDto };
}

export interface CompareRequest {
  type1: string;
  type2: string;
}

export interface CompareResponse {
  pretty1: string;
  pretty2: string;
  type1SubtypeOfType2: boolean;
  type2SubtypeOfType1: boolean;
  subtypingRelation: string;
  dual1: string;
  dual2: string;
  areDuals: boolean;
  states1: number;
  transitions1: number;
  isLattice1: boolean;
  svgHtml1: string;
  states2: number;
  transitions2: number;
  isLattice2: boolean;
  svgHtml2: string;
  chomsky1: string;
  chomsky2: string;
  isRecursive1: boolean;
  isGuarded1: boolean;
  isContractive1: boolean;
  isTailRecursive1: boolean;
  isRecursive2: boolean;
  isGuarded2: boolean;
  isContractive2: boolean;
  isTailRecursive2: boolean;
}

// --- Composition ---

export interface ParticipantEntry {
  name: string;
  typeString: string;
}

export interface CompositionRequest {
  participants: ParticipantEntry[];
  globalType?: string;
}

export interface ParticipantDto {
  name: string;
  pretty: string;
  states: number;
  transitions: number;
  isLattice: boolean;
  svgHtml: string;
}

export interface CompatibilityEntry {
  first: string;
  second: string;
  compatible: boolean;
}

export interface SharedLabelsEntry {
  first: string;
  second: string;
  labels: string[];
}

export interface GlobalComparisonDto {
  globalStates: number;
  globalIsLattice: boolean;
  embeddingExists: boolean;
  overApproximationRatio: number;
  roleTypeMatches: { [role: string]: boolean };
}

export interface CompositionResponse {
  participantCount: number;
  participants: ParticipantDto[];
  freeStates: number;
  freeTransitions: number;
  freeIsLattice: boolean;
  freeSvgHtml: string;
  syncedStates: number;
  syncedTransitions: number;
  syncedIsLattice: boolean;
  syncedSvgHtml: string;
  reductionRatio: number;
  compatibility: CompatibilityEntry[];
  sharedLabels: SharedLabelsEntry[];
  globalComparison: GlobalComparisonDto | null;
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
