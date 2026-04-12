import { Component, signal, QueryList, ViewChildren, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-intro',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  templateUrl: './intro.component.html',
  styleUrl: './intro.component.scss',
})
export class IntroComponent implements AfterViewInit, OnDestroy {
  @ViewChildren('sceneEl') sceneElements!: QueryList<ElementRef>;

  readonly activeScene = signal(0);
  private observer?: IntersectionObserver;

  readonly scenes = [
    { id: 'title', label: 'Introduction', dark: true },
    { id: 'protocol', label: 'Protocols', dark: false },
    { id: 'statespace', label: 'State Spaces', dark: false },
    { id: 'lattice', label: 'Lattices', dark: false },
    { id: 'parallel', label: 'Parallel', dark: false },
    { id: 'theorem', label: 'The Theorem', dark: true },
  ];

  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && entry.intersectionRatio > 0.4) {
            const idx = this.sceneElements.toArray().findIndex(
              (el) => el.nativeElement === entry.target,
            );
            if (idx >= 0) this.activeScene.set(idx);
          }
        }
      },
      { threshold: [0.4] },
    );

    this.sceneElements.forEach((el) => {
      this.observer!.observe(el.nativeElement);
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  scrollToScene(index: number): void {
    const el = this.sceneElements.toArray()[index];
    if (el) {
      el.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
