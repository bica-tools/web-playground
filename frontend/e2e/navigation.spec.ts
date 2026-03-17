import { test, expect } from '@playwright/test';

// Authenticate before each test
test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await page.evaluate(() => sessionStorage.setItem('auth', '1'));
});

test.describe('Navigation', () => {
  test('navbar links work - Playground, Research, Benchmarks', async ({ page }) => {
    await page.goto('/');

    // Navbar should be visible
    await expect(page.locator('app-navbar')).toBeVisible();

    // Click Playground link
    await page.click('nav.desktop-nav a:has-text("Playground")');
    await expect(page).toHaveURL(/\/tools\/analyzer/);
    await expect(page.locator('.pane-title')).toHaveText('Analyzer');

    // Click Research link
    await page.click('nav.desktop-nav a:has-text("Research")');
    await expect(page).toHaveURL(/\/theory/);

    // Click Benchmarks link
    await page.click('nav.desktop-nav a:has-text("Benchmarks")');
    await expect(page).toHaveURL(/\/benchmarks/);
    await expect(page.locator('.bench-hero h1')).toHaveText('Benchmark Observatory');
  });

  test('mobile menu toggle works at 375px width', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/');

    // Desktop nav should be hidden (via CSS media query)
    await expect(page.locator('nav.desktop-nav')).not.toBeVisible();

    // Mobile menu button should be visible
    const menuBtn = page.locator('.mobile-menu-btn');
    await expect(menuBtn).toBeVisible();

    // Mobile nav should NOT be visible initially
    await expect(page.locator('#mobile-nav')).not.toBeVisible();

    // Click hamburger to open
    await menuBtn.click();

    // Mobile nav should appear
    await expect(page.locator('#mobile-nav')).toBeVisible();

    // Should have navigation links
    await expect(page.locator('#mobile-nav a:has-text("Playground")')).toBeVisible();
    await expect(page.locator('#mobile-nav a:has-text("Research")')).toBeVisible();
    await expect(page.locator('#mobile-nav a:has-text("Benchmarks")')).toBeVisible();

    // Click close (the same button toggles)
    await menuBtn.click();
    await expect(page.locator('#mobile-nav')).not.toBeVisible();
  });
});
