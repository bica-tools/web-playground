import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AnalyzeRequest,
  AnalyzeResponse,
  TestGenRequest,
  TestGenResponse,
  BenchmarkDto,
  TutorialSummaryDto,
  TutorialDto,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = '/api';

  constructor(private http: HttpClient) {}

  analyze(typeString: string): Observable<AnalyzeResponse> {
    return this.http.post<AnalyzeResponse>(`${this.baseUrl}/analyze`, {
      typeString,
    } as AnalyzeRequest);
  }

  generateTests(request: TestGenRequest): Observable<TestGenResponse> {
    return this.http.post<TestGenResponse>(`${this.baseUrl}/test-gen`, request);
  }

  getBenchmarks(): Observable<BenchmarkDto[]> {
    return this.http.get<BenchmarkDto[]>(`${this.baseUrl}/benchmarks`);
  }

  getTutorials(): Observable<TutorialSummaryDto[]> {
    return this.http.get<TutorialSummaryDto[]>(`${this.baseUrl}/tutorials`);
  }

  getTutorial(id: string): Observable<TutorialDto> {
    return this.http.get<TutorialDto>(`${this.baseUrl}/tutorials/${id}`);
  }
}
