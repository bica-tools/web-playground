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
  },
  {
    path: 'map',
    loadComponent: () =>
      import('./pages/concept-graph/concept-graph.component').then(
        (m) => m.ConceptGraphComponent,
      ),
    title: 'Concept Map — BICA Tools',
  },
  {
    path: 'proofs',
    loadComponent: () =>
      import('./pages/proofs/proofs.component').then(
        (m) => m.ProofsComponent,
      ),
    title: 'Proofs — BICA Tools',
  },
  {
    path: 'theory',
    loadComponent: () =>
      import('./pages/theory/theory.component').then(
        (m) => m.TheoryComponent,
      ),
    title: 'Research — BICA Tools',
  },
  {
    path: 'tools',
    loadComponent: () =>
      import('./pages/tools-hub/tools-hub.component').then(
        (m) => m.ToolsHubComponent,
      ),
    pathMatch: 'full',
    title: 'Tools — BICA Tools',
  },
  {
    path: 'tools/analyzer',
    loadComponent: () =>
      import('./pages/analyzer/analyzer.component').then(
        (m) => m.AnalyzerComponent,
      ),
    title: 'Analyzer — BICA Tools',
  },
  {
    path: 'tools/global-analyzer',
    loadComponent: () =>
      import('./pages/global-analyzer/global-analyzer.component').then(
        (m) => m.GlobalAnalyzerComponent,
      ),
    title: 'Global Analyzer — BICA Tools',
  },
  {
    path: 'tools/test-generator',
    loadComponent: () =>
      import('./pages/test-generator/test-generator.component').then(
        (m) => m.TestGeneratorComponent,
      ),
    title: 'Test Generator — BICA Tools',
  },
  {
    path: 'tools/compare',
    loadComponent: () =>
      import('./pages/compare/compare.component').then(
        (m) => m.CompareComponent,
      ),
    title: 'Compare Types — BICA Tools',
  },
  {
    path: 'tools/composition',
    loadComponent: () =>
      import('./pages/composition/composition.component').then(
        (m) => m.CompositionComponent,
      ),
    title: 'Composition — BICA Tools',
  },
  {
    path: 'tools/mcp',
    loadComponent: () =>
      import('./pages/mcp-tools/mcp-tools.component').then(
        (m) => m.McpToolsComponent,
      ),
    title: 'MCP Tools — BICA Tools',
  },
  {
    path: 'games',
    loadComponent: () =>
      import('./pages/games/games.component').then(
        (m) => m.GamesComponent,
      ),
    title: 'Games — BICA Tools',
  },
  {
    path: 'benchmarks',
    loadComponent: () =>
      import('./pages/benchmarks/benchmarks.component').then(
        (m) => m.BenchmarksComponent,
      ),
    title: 'Benchmarks — BICA Tools',
  },
  {
    path: 'pipeline',
    loadComponent: () =>
      import('./pages/pipeline/pipeline.component').then(
        (m) => m.PipelineComponent,
      ),
    canActivate: [authGuard],
    title: 'Pipeline — BICA Tools',
  },
  {
    path: 'publications',
    loadComponent: () =>
      import('./pages/publications/publications.component').then(
        (m) => m.PublicationsComponent,
      ),
    title: 'Publications — BICA Tools',
  },
  {
    path: 'tutorials/:id',
    loadComponent: () =>
      import('./pages/tutorials/tutorial-detail.component').then(
        (m) => m.TutorialDetailComponent,
      ),
    title: 'Tutorial — BICA Tools',
  },
  {
    path: 'tutorials',
    loadComponent: () =>
      import('./pages/tutorials/tutorials-list.component').then(
        (m) => m.TutorialsListComponent,
      ),
    title: 'Tutorials — BICA Tools',
  },
  { path: 'quickstart', redirectTo: 'tutorials/quick-start' },
  {
    path: 'faq',
    loadComponent: () =>
      import('./pages/faq/faq.component').then((m) => m.FaqComponent),
    title: 'FAQ — BICA Tools',
  },
  {
    path: 'documentation',
    loadComponent: () =>
      import('./pages/documentation/documentation.component').then(
        (m) => m.DocumentationComponent,
      ),
    title: 'Documentation — BICA Tools',
  },
  {
    path: 'explore',
    loadComponent: () =>
      import('./pages/explore/explore.component').then(
        (m) => m.ExploreComponent,
      ),
    title: 'Explore — BICA Tools',
  },
  {
    path: 'about',
    loadComponent: () =>
      import('./pages/about/about.component').then((m) => m.AboutComponent),
    title: 'About — BICA Tools',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then(
        (m) => m.LoginComponent,
      ),
    title: 'Login — BICA Tools',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent,
      ),
    canActivate: [authGuard],
    title: 'Dashboard — BICA Tools',
  },
  {
    path: 'tools/sandbox',
    loadComponent: () =>
      import('./pages/sandbox/sandbox.component').then(
        (m) => m.SandboxComponent,
      ),
    title: 'Live Sandbox — BICA Tools',
  },
  {
    path: 'explore/zoo',
    loadComponent: () =>
      import('./pages/zoo/zoo.component').then(
        (m) => m.ZooComponent,
      ),
    title: 'Lattice Zoo — BICA Tools',
  },
  {
    path: 'tools/reverse-search',
    loadComponent: () =>
      import('./pages/reverse-search/reverse-search.component').then(
        (m) => m.ReverseSearchComponent,
      ),
    title: 'Reverse Search — BICA Tools',
  },
  {
    path: 'embed/analyzer',
    loadComponent: () =>
      import('./pages/embed/embed-analyzer.component').then(
        (m) => m.EmbedAnalyzerComponent,
      ),
    title: 'BICA Tools — Embed',
    data: { embed: true },
  },
  {
    path: 'monitoring',
    loadComponent: () =>
      import('./pages/monitoring/monitoring.component').then(
        (m) => m.MonitoringComponent,
      ),
    title: 'Monitoring — BICA Tools',
  },
  {
    path: 'papers',
    loadComponent: () =>
      import('./pages/papers/paper-list.component').then(
        (m) => m.PaperListComponent,
      ),
    title: 'Research Papers — BICA Tools',
  },
  {
    path: 'papers/:slug',
    loadComponent: () =>
      import('./pages/papers/paper-detail.component').then(
        (m) => m.PaperDetailComponent,
      ),
    title: 'Paper — BICA Tools',
  },
  {
    path: 'venue-papers',
    loadComponent: () =>
      import('./pages/papers/venue-papers.component').then(
        (m) => m.VenuePapersComponent,
      ),
    title: 'Publications — BICA Tools',
  },
  {
    path: 'blog',
    loadComponent: () =>
      import('./pages/blog/blog-list.component').then(
        (m) => m.BlogListComponent,
      ),
    title: 'Blog — BICA Tools',
  },
  {
    path: 'blog/:slug',
    loadComponent: () =>
      import('./pages/blog/blog-post.component').then(
        (m) => m.BlogPostComponent,
      ),
    title: 'Blog — BICA Tools',
  },
  { path: '**', redirectTo: '' },
];
