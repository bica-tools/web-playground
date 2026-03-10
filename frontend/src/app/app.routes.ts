import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  {
    path: 'tools/analyzer',
    loadComponent: () =>
      import('./pages/analyzer/analyzer.component').then(
        (m) => m.AnalyzerComponent,
      ),
  },
  {
    path: 'tools/test-generator',
    loadComponent: () =>
      import('./pages/test-generator/test-generator.component').then(
        (m) => m.TestGeneratorComponent,
      ),
  },
  {
    path: 'benchmarks',
    loadComponent: () =>
      import('./pages/benchmarks/benchmarks.component').then(
        (m) => m.BenchmarksComponent,
      ),
  },
  {
    path: 'pipeline',
    loadComponent: () =>
      import('./pages/pipeline/pipeline.component').then(
        (m) => m.PipelineComponent,
      ),
  },
  {
    path: 'publications',
    loadComponent: () =>
      import('./pages/publications/publications.component').then(
        (m) => m.PublicationsComponent,
      ),
  },
  {
    path: 'tutorials/:id',
    loadComponent: () =>
      import('./pages/tutorials/tutorial-detail.component').then(
        (m) => m.TutorialDetailComponent,
      ),
  },
  {
    path: 'tutorials',
    loadComponent: () =>
      import('./pages/tutorials/tutorials-list.component').then(
        (m) => m.TutorialsListComponent,
      ),
  },
  { path: 'quickstart', redirectTo: 'tutorials/quick-start' },
  {
    path: 'faq',
    loadComponent: () =>
      import('./pages/faq/faq.component').then((m) => m.FaqComponent),
  },
  {
    path: 'documentation',
    loadComponent: () =>
      import('./pages/documentation/documentation.component').then(
        (m) => m.DocumentationComponent,
      ),
  },
  {
    path: 'about',
    loadComponent: () =>
      import('./pages/about/about.component').then((m) => m.AboutComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
