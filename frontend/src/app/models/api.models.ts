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
  svgHtml: string;
  toolUrl: string;
  numTests: number;
}
