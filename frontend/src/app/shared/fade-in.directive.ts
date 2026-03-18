import { Directive, ElementRef, Input, OnInit, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Directive({
  selector: '[appFadeIn]',
  standalone: true,
})
export class FadeInDirective implements OnInit, OnDestroy {
  @Input('appFadeIn') delay: number | '' = 0;

  private readonly el = inject(ElementRef);
  private readonly platformId = inject(PLATFORM_ID);
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduced) return;

    const nativeEl: HTMLElement = this.el.nativeElement;
    nativeEl.classList.add('fade-in-hidden');

    const delayMs = typeof this.delay === 'number' ? this.delay : 0;
    if (delayMs > 0) {
      nativeEl.style.transitionDelay = `${delayMs}ms`;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            nativeEl.classList.remove('fade-in-hidden');
            nativeEl.classList.add('fade-in-visible');
            this.observer?.unobserve(nativeEl);
          }
        }
      },
      { threshold: 0.2 },
    );
    this.observer.observe(nativeEl);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
