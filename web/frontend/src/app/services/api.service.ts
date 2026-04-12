import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import {
  AnalyzeRequest,
  AnalyzeResponse,
  TestGenRequest,
  TestGenResponse,
  CoverageStoryboardResponse,
  BenchmarkDto,
  TutorialSummaryDto,
  TutorialDto,
  GlobalAnalyzeRequest,
  GlobalAnalyzeResponse,
  CompareRequest,
  CompareResponse,
  CompositionRequest,
  CompositionResponse,
  GameDataResponse,
  GamePlaysResponse,
  SubtypeRequest,
  SubtypeResponse,
  DualRequest,
  DualResponse,
  TraceRequest,
  TraceResponse,
  AgentTypeDto,
  ProgrammeStatusDto,
  PublicationDto,
  StepEvaluationDto,
  BlogPostSummary,
  BlogPost,
  StepPaperSummary,
  StepPaper,
  VenuePaper,
  PaperComment,
  PhaseStats,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = '/api';

  private benchmarksCache$?: Observable<BenchmarkDto[]>;
  private tutorialsCache$?: Observable<TutorialSummaryDto[]>;
  private tutorialCache = new Map<string, Observable<TutorialDto>>();

  constructor(private http: HttpClient) {}

  analyze(typeString: string): Observable<AnalyzeResponse> {
    return this.http.post<AnalyzeResponse>(`${this.baseUrl}/analyze`, {
      typeString,
    } as AnalyzeRequest);
  }

  generateTests(request: TestGenRequest): Observable<TestGenResponse> {
    return this.http.post<TestGenResponse>(`${this.baseUrl}/test-gen`, request);
  }

  coverageStoryboard(typeString: string): Observable<CoverageStoryboardResponse> {
    return this.http.post<CoverageStoryboardResponse>(`${this.baseUrl}/coverage-storyboard`, {
      typeString,
    } as AnalyzeRequest);
  }

  getBenchmarks(): Observable<BenchmarkDto[]> {
    if (!this.benchmarksCache$) {
      this.benchmarksCache$ = this.http
        .get<BenchmarkDto[]>(`${this.baseUrl}/benchmarks`)
        .pipe(shareReplay(1));
    }
    return this.benchmarksCache$;
  }

  getTutorials(): Observable<TutorialSummaryDto[]> {
    if (!this.tutorialsCache$) {
      this.tutorialsCache$ = this.http
        .get<TutorialSummaryDto[]>(`${this.baseUrl}/tutorials`)
        .pipe(shareReplay(1));
    }
    return this.tutorialsCache$;
  }

  getTutorial(id: string): Observable<TutorialDto> {
    if (!this.tutorialCache.has(id)) {
      this.tutorialCache.set(
        id,
        this.http.get<TutorialDto>(`${this.baseUrl}/tutorials/${id}`).pipe(shareReplay(1)),
      );
    }
    return this.tutorialCache.get(id)!;
  }

  compareTypes(type1: string, type2: string): Observable<CompareResponse> {
    return this.http.post<CompareResponse>(`${this.baseUrl}/compare`, {
      type1,
      type2,
    } as CompareRequest);
  }

  compose(request: CompositionRequest): Observable<CompositionResponse> {
    return this.http.post<CompositionResponse>(`${this.baseUrl}/compose`, request);
  }

  analyzeGlobal(typeString: string): Observable<GlobalAnalyzeResponse> {
    return this.http.post<GlobalAnalyzeResponse>(`${this.baseUrl}/analyze-global`, {
      typeString,
    } as GlobalAnalyzeRequest);
  }

  extractSession(file: File): Observable<{ found: boolean; message?: string; annotations: { className: string; typeString: string }[] }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.baseUrl}/extract-session`, formData);
  }

  explain(typeString: string): Observable<{ explanation: string }> {
    return this.http.get<{ explanation: string }>(`${this.baseUrl}/explain`, {
      params: { type: typeString },
    });
  }

  story(typeString: string): Observable<{ story: string }> {
    return this.http.get<{ story: string }>(`${this.baseUrl}/story`, {
      params: { type: typeString },
    });
  }

  gameData(typeString: string): Observable<GameDataResponse> {
    return this.http.post<GameDataResponse>(`${this.baseUrl}/game-data`, {
      typeString,
    } as AnalyzeRequest);
  }

  gamePlays(typeString: string): Observable<GamePlaysResponse> {
    return this.http.post<GamePlaysResponse>(`${this.baseUrl}/game-plays`, {
      typeString,
    } as AnalyzeRequest);
  }

  checkSubtype(subtype: string, supertype: string): Observable<SubtypeResponse> {
    return this.http.post<SubtypeResponse>(`${this.baseUrl}/subtype`, {
      subtype,
      supertype,
    } as SubtypeRequest);
  }

  computeDual(type: string): Observable<DualResponse> {
    return this.http.post<DualResponse>(`${this.baseUrl}/dual`, {
      type,
    } as DualRequest);
  }

  validateTrace(type: string, trace: string): Observable<TraceResponse> {
    return this.http.post<TraceResponse>(`${this.baseUrl}/trace`, {
      type,
      trace,
    } as TraceRequest);
  }

  getPublications(): Observable<PublicationDto[]> {
    return this.http.get<PublicationDto[]>(`${this.baseUrl}/monitor/publications`);
  }

  getAgentTypes(): Observable<AgentTypeDto[]> {
    return this.http.get<AgentTypeDto[]>(`${this.baseUrl}/monitor/agents`);
  }

  getProgrammeStatus(): Observable<ProgrammeStatusDto> {
    return this.http.get<ProgrammeStatusDto>(`${this.baseUrl}/monitor/status`);
  }

  evaluateStep(step: string): Observable<StepEvaluationDto> {
    return this.http.post<StepEvaluationDto>(`${this.baseUrl}/monitor/evaluate`, { step });
  }

  // --- Blog ---

  getBlogPosts(arc?: number): Observable<BlogPostSummary[]> {
    if (arc !== undefined) {
      return this.http.get<BlogPostSummary[]>(`${this.baseUrl}/blog`, {
        params: { arc: arc.toString() },
      });
    }
    return this.http.get<BlogPostSummary[]>(`${this.baseUrl}/blog`);
  }

  getBlogPost(slug: string): Observable<BlogPost> {
    return this.http.get<BlogPost>(`${this.baseUrl}/blog/${slug}`);
  }

  // --- Papers ---

  getPapers(params?: Record<string, string>): Observable<StepPaperSummary[]> {
    return this.http.get<StepPaperSummary[]>(`${this.baseUrl}/papers`, {
      params: params || {},
    });
  }

  getPaper(slug: string): Observable<StepPaper> {
    return this.http.get<StepPaper>(`${this.baseUrl}/papers/${slug}`);
  }

  getPaperBibtex(slug: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/papers/${slug}/bibtex`, {
      responseType: 'text',
    });
  }

  getPhaseStats(): Observable<PhaseStats[]> {
    return this.http.get<PhaseStats[]>(`${this.baseUrl}/papers/phases`);
  }

  getVenuePapers(): Observable<VenuePaper[]> {
    return this.http.get<VenuePaper[]>(`${this.baseUrl}/venue-papers`);
  }

  addReaction(slug: string, type: string): Observable<Record<string, number>> {
    return this.http.post<Record<string, number>>(
      `${this.baseUrl}/papers/${slug}/reactions`,
      { type },
    );
  }

  getComments(slug: string): Observable<PaperComment[]> {
    return this.http.get<PaperComment[]>(`${this.baseUrl}/papers/${slug}/comments`);
  }

  addComment(slug: string, request: { authorName: string; content: string; parentId?: number }): Observable<PaperComment> {
    return this.http.post<PaperComment>(`${this.baseUrl}/papers/${slug}/comments`, request);
  }
}
