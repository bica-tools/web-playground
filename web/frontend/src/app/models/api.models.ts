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
  interactiveType: string | null;
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
  sharedMethods: string[];
  uniqueMethods1: string[];
  uniqueMethods2: string[];
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

// --- Game Data ---

export interface GameNode {
  id: number;
  x: number;
  y: number;
  label: string;
  kind: 'top' | 'branch' | 'select' | 'end';
  isTop: boolean;
  isBottom: boolean;
}

export interface GameEdge {
  src: number;
  tgt: number;
  label: string;
  isSelection: boolean;
}

export interface GameDataResponse {
  nodes: GameNode[];
  edges: GameEdge[];
  top: number;
  bottom: number;
  numStates: number;
  numTransitions: number;
  pretty: string;
}

// --- Game Plays (tests as game replays) ---

export interface GameStep {
  label: string;
  from: number;
  to: number;
  isSelection: boolean;
}

export interface GamePlay {
  name: string;
  kind: 'valid' | 'violation' | 'incomplete';
  steps: GameStep[];
  violationMethod: string | null;
  enabledMethods: string[] | null;
  remainingMethods: string[] | null;
  transitionCoverage: number;
  stateCoverage: number;
}

export interface GamePlaysResponse {
  plays: GamePlay[];
  totalTransitions: number;
  totalStates: number;
  board: GameDataResponse;
}

// --- MCP Tools ---

export interface SubtypeRequest {
  subtype: string;
  supertype: string;
}

export interface SubtypeResponse {
  prettySubtype: string;
  prettySupertype: string;
  isSubtype: boolean;
  isEquivalent: boolean;
  relation: string;
  subtypeStates: number;
  supertypeStates: number;
  svgSubtype: string;
  svgSupertype: string;
}

export interface DualRequest {
  type: string;
}

export interface DualResponse {
  prettyOriginal: string;
  prettyDual: string;
  isInvolution: boolean;
  isIsomorphic: boolean;
  selectionFlipped: boolean;
  originalStates: number;
  dualStates: number;
  svgOriginal: string;
  svgDual: string;
}

export interface TraceRequest {
  type: string;
  trace: string;
}

export interface TraceStepDto {
  method: string;
  from: number;
  to: number;
  valid: boolean;
  enabled?: string[];
}

export interface TraceResponse {
  valid: boolean;
  complete: boolean;
  currentState: number;
  path: TraceStepDto[];
  violationAt: string;
  atEnd: boolean;
  enabledAtEnd: string[];
  prettyType: string;
}

// --- Monitoring ---

export interface AgentTypeDto {
  name: string;
  protocol: string;
  sessionType: string;
  description: string;
  transport: string;
}

export interface StepEvaluationDto {
  stepNumber: string;
  title: string;
  grade: string;
  score: number;
  accepted: boolean;
  fixes: string[];
}

export interface ProgrammeStatusDto {
  totalSteps: number;
  acceptedSteps: number;
  totalModules: number;
  totalTests: number;
  steps: StepEvaluationDto[];
}

export interface PublicationDto {
  id: string;
  title: string;
  layer: string;
  venues: string[];
  priority: string;
  deadline: string;
  status: string;
}

// --- Blog ---

export interface BlogPostSummary {
  id: number;
  slug: string;
  title: string;
  summary: string;
  arc: number;
  sequence: number;
  tags: string;
  author: string;
  publishedAt: string;
}

export interface BlogPost {
  id: number;
  slug: string;
  title: string;
  summary: string;
  content: string;
  arc: number;
  sequence: number;
  tags: string;
  author: string;
  published: boolean;
  createdAt: string;
  publishedAt: string;
  updatedAt: string;
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
  tags: string[];
}

// --- Papers ---

export interface StepPaperSummary {
  id: number;
  stepNumber: string;
  slug: string;
  title: string;
  phase: string;
  status: string;
  proofBacking: string;
  grade: string;
  wordCount: number;
  tags: string;
  reactionCounts: Record<string, number>;
}

export interface StepPaper extends StepPaperSummary {
  abstract: string;
  domain: string;
  pdfPath: string;
  hasProofsTex: boolean;
  leanFiles: string;
  reticulateModule: string;
  bicaPackage: string;
  dependsOn: string;
  relatedSteps: string;
  supersededBy: string;
  version: number;
  revisionNotes: string;
  blogSlugs: string;
  venuePaperSlug: string;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface VenuePaper {
  id: number;
  slug: string;
  title: string;
  abstract: string;
  venue: string;
  status: string;
  pdfPath: string;
  submissionDate: string;
  decisionDate: string;
  stepsCovered: string;
  doi: string;
  arxivId: string;
  createdAt: string;
}

export interface PaperComment {
  id: number;
  authorName: string;
  content: string;
  createdAt: string;
  replies: PaperComment[];
}

export interface PhaseStats {
  phase: string;
  totalPapers: number;
  completedPapers: number;
  provedPapers: number;
}
