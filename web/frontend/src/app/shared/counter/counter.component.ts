import { Component, Input, ElementRef, OnInit, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-counter',
  standalone: true,
  template: `<span>{{ displayValue() }}{{ suffix }}</span>`,
  styles: [`:host { display: inline; font: inherit; color: inherit; }`],
})
export class CounterComponent implements OnInit, OnDestroy {
  @Input({ required: true }) target = 0;
  @Input() duration = 1500;
  @Input() suffix = '';

  readonly displayValue = signal(0);

  private readonly el = inject(ElementRef);
  private readonly platformId = inject(PLATFORM_ID);
  private observer?: IntersectionObserver;
  private animationId?: number;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.displayValue.set(this.target);
      return;
    }

    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduced) {
      this.displayValue.set(this.target);
      return;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            this.animate();
            this.observer?.unobserve(this.el.nativeElement);
          }
        }
      },
      { threshold: 0.2 },
    );
    this.observer.observe(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    if (this.animationId != null) cancelAnimationFrame(this.animationId);
  }

  private animate(): void {
    const start = performance.now();
    const from = 0;
    const to = this.target;
    const dur = this.duration;

    const step = (now: number) => {
      const elapsed = now - start;
      const t = Math.min(elapsed / dur, 1);
      const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
      this.displayValue.set(Math.round(from + (to - from) * eased));
      if (t < 1) {
        this.animationId = requestAnimationFrame(step);
      }
    };
    this.animationId = requestAnimationFrame(step);
  }
}
