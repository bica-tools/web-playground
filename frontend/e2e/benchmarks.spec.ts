import { test, expect } from '@playwright/test';

// Authenticate before each test
test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await page.evaluate(() => sessionStorage.setItem('auth', '1'));
});

test.describe('Benchmarks Page', () => {
  test('page loads with benchmark cards', async ({ page }) => {
    await page.goto('/benchmarks');

    // Hero title
    await expect(page.locator('.bench-hero h1')).toHaveText('Benchmark Observatory');

    // Wait for loading to complete and cards to appear
    await expect(page.locator('.bench-grid')).toBeVisible({ timeout: 15_000 });

    // Should have multiple benchmark cards
    const cards = page.locator('.bench-card');
    await expect(cards.first()).toBeVisible();
    expect(await cards.count()).toBeGreaterThan(0);
  });

  test('filter chips work - clicking Parallel shows only parallel benchmarks', async ({ page }) => {
    await page.goto('/benchmarks');

    // Wait for cards to load
    await expect(page.locator('.bench-grid')).toBeVisible({ timeout: 15_000 });

    // Count all cards
    const allCount = await page.locator('.bench-card').count();

    // Click Parallel filter
    await page.click('.f-chip:has-text("Parallel")');

    // The filter chip should be active
    await expect(page.locator('.f-chip:has-text("Parallel")')).toHaveClass(/active/);

    // Filtered cards should be <= total cards
    const filteredCount = await page.locator('.bench-card').count();
    expect(filteredCount).toBeLessThanOrEqual(allCount);

    // Every visible card should have the parallel badge
    const parallelBadges = page.locator('.bench-card .badge-par');
    if (filteredCount > 0) {
      expect(await parallelBadges.count()).toBe(filteredCount);
    }
  });

  test('clicking a benchmark card opens detail panel', async ({ page }) => {
    await page.goto('/benchmarks');

    // Wait for cards to load
    await expect(page.locator('.bench-grid')).toBeVisible({ timeout: 15_000 });

    // Click the first benchmark card
    await page.click('.bench-card:first-child');

    // Detail overlay should appear
    await expect(page.locator('.detail-overlay')).toBeVisible();

    // Detail panel should have a title and close button
    await expect(page.locator('.dp-title')).toBeVisible();
    await expect(page.locator('.dp-close')).toBeVisible();

    // Close it
    await page.click('.dp-close');
    await expect(page.locator('.detail-overlay')).not.toBeVisible();
  });
});
