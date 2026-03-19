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

  explain(typeString: string): Observable<{ explanation: string }> {
    return this.http.get<{ explanation: string }>(`${this.baseUrl}/explain`, {
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
}
