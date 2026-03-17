import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'BICA Tools — Session Types as Algebraic Reticulates' },
  {
    path: 'intro',
    loadComponent: () =>
      import('./pages/intro/intro.component').then(
        (m) => m.IntroComponent,
      ),
    title: 'Introduction — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'map',
    loadComponent: () =>
      import('./pages/concept-graph/concept-graph.component').then(
        (m) => m.ConceptGraphComponent,
      ),
    title: 'Concept Map — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'proofs',
    loadComponent: () =>
      import('./pages/proofs/proofs.component').then(
        (m) => m.ProofsComponent,
      ),
    title: 'Proofs — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'theory',
    loadComponent: () =>
      import('./pages/theory/theory.component').then(
        (m) => m.TheoryComponent,
      ),
    title: 'Research — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tools',
    loadComponent: () =>
      import('./pages/tools-hub/tools-hub.component').then(
        (m) => m.ToolsHubComponent,
      ),
    pathMatch: 'full',
    title: 'Tools — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tools/analyzer',
    loadComponent: () =>
      import('./pages/analyzer/analyzer.component').then(
        (m) => m.AnalyzerComponent,
      ),
    title: 'Analyzer — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tools/global-analyzer',
    loadComponent: () =>
      import('./pages/global-analyzer/global-analyzer.component').then(
        (m) => m.GlobalAnalyzerComponent,
      ),
    title: 'Global Analyzer — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tools/test-generator',
    loadComponent: () =>
      import('./pages/test-generator/test-generator.component').then(
        (m) => m.TestGeneratorComponent,
      ),
    title: 'Test Generator — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tools/compare',
    loadComponent: () =>
      import('./pages/compare/compare.component').then(
        (m) => m.CompareComponent,
      ),
    title: 'Compare Types — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tools/composition',
    loadComponent: () =>
      import('./pages/composition/composition.component').then(
        (m) => m.CompositionComponent,
      ),
    title: 'Composition — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'benchmarks',
    loadComponent: () =>
      import('./pages/benchmarks/benchmarks.component').then(
        (m) => m.BenchmarksComponent,
      ),
    title: 'Benchmarks — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'pipeline',
    loadComponent: () =>
      import('./pages/pipeline/pipeline.component').then(
        (m) => m.PipelineComponent,
      ),
    title: 'Pipeline — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'publications',
    loadComponent: () =>
      import('./pages/publications/publications.component').then(
        (m) => m.PublicationsComponent,
      ),
    title: 'Publications — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tutorials/:id',
    loadComponent: () =>
      import('./pages/tutorials/tutorial-detail.component').then(
        (m) => m.TutorialDetailComponent,
      ),
    title: 'Tutorial — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'tutorials',
    loadComponent: () =>
      import('./pages/tutorials/tutorials-list.component').then(
        (m) => m.TutorialsListComponent,
      ),
    title: 'Tutorials — BICA Tools',
    canActivate: [authGuard],
  },
  { path: 'quickstart', redirectTo: 'tutorials/quick-start' },
  {
    path: 'faq',
    loadComponent: () =>
      import('./pages/faq/faq.component').then((m) => m.FaqComponent),
    title: 'FAQ — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'documentation',
    loadComponent: () =>
      import('./pages/documentation/documentation.component').then(
        (m) => m.DocumentationComponent,
      ),
    title: 'Documentation — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'explore',
    loadComponent: () =>
      import('./pages/explore/explore.component').then(
        (m) => m.ExploreComponent,
      ),
    title: 'Explore — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'about',
    loadComponent: () =>
      import('./pages/about/about.component').then((m) => m.AboutComponent),
    title: 'About — BICA Tools',
    canActivate: [authGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent,
      ),
    title: 'Dashboard — BICA Tools',
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: '' },
];
