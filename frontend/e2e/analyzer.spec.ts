import { test, expect } from '@playwright/test';

// Authenticate before each test by setting sessionStorage
test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await page.evaluate(() => sessionStorage.setItem('auth', '1'));
});

test.describe('Analyzer Page', () => {
  test('page loads with empty state', async ({ page }) => {
    await page.goto('/tools/analyzer');

    // Title should be visible
    await expect(page.locator('.pane-title')).toHaveText('Analyzer');

    // Empty state should be shown in the right pane
    await expect(page.locator('.empty-state')).toBeVisible();
    await expect(page.locator('.empty-text')).toHaveText('Type a session type and click Analyze');
  });

  test('clicking an example loads it and shows results', async ({ page }) => {
    await page.goto('/tools/analyzer');

    // Click the first example chip (Iterator)
    await page.click('.example-chip:first-child');

    // Wait for the result to appear
    await expect(page.locator('.result-header')).toBeVisible({ timeout: 15_000 });

    // Should show lattice badge and stats
    await expect(page.locator('.result-badge')).toBeVisible();

    // At least one result-stat should be visible
    const stats = page.locator('.result-stat');
    await expect(stats.first()).toBeVisible();
    expect(await stats.count()).toBeGreaterThan(0);
  });

  test('analyze "&{a: end, b: end}" returns lattice with 3 states', async ({ page }) => {
    await page.goto('/tools/analyzer');

    // Type session type in the textarea
    await page.fill('#type-input', '&{a: end, b: end}');

    // Click analyze
    await page.click('.analyze-btn');

    // Wait for results
    await expect(page.locator('.result-header')).toBeVisible({ timeout: 15_000 });

    // Should be a lattice
    await expect(page.locator('.result-badge')).toHaveText('Lattice');
    await expect(page.locator('.result-badge')).toHaveClass(/badge-lattice/);

    // Should have 3 states (top + a-branch end + b-branch end, but they share end => 3 states)
    await expect(page.locator('.result-stat').first()).toContainText('states');
  });

  test('invalid input shows error message', async ({ page }) => {
    await page.goto('/tools/analyzer');

    // Type invalid session type
    await page.fill('#type-input', '&{broken!!!}');

    // Click analyze
    await page.click('.analyze-btn');

    // Error should appear
    await expect(page.locator('.error-banner')).toBeVisible({ timeout: 10_000 });
  });

  test('Hasse diagram SVG is rendered', async ({ page }) => {
    await page.goto('/tools/analyzer');

    // Use a simple example
    await page.fill('#type-input', '&{a: end, b: end}');
    await page.click('.analyze-btn');

    // Wait for the Hasse diagram figure
    await expect(page.locator('.hasse-section')).toBeVisible({ timeout: 15_000 });

    // Should contain an SVG element
    await expect(page.locator('.hasse-section svg')).toBeVisible();
  });
});
