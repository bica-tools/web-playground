import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('wrong password shows error', async ({ page }) => {
    await page.goto('/');
    // The auth gate should be visible
    await expect(page.locator('.auth-gate')).toBeVisible();

    // Enter wrong password and submit
    await page.fill('#auth-password', 'wrong-password');
    await page.click('.auth-box button[type="submit"]');

    // Error message should appear
    await expect(page.locator('.auth-error')).toBeVisible();
    await expect(page.locator('.auth-error')).toHaveText('Incorrect password');
  });

  test('correct password grants access', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.auth-gate')).toBeVisible();

    // Enter correct password
    await page.fill('#auth-password', 'reticulate');
    await page.click('.auth-box button[type="submit"]');

    // Auth gate should disappear; navbar should appear
    await expect(page.locator('.auth-gate')).not.toBeVisible();
    await expect(page.locator('app-navbar')).toBeVisible();
  });

  test('protected routes redirect to home when not authenticated', async ({ page }) => {
    // Try navigating directly to a protected route without auth
    await page.goto('/tools/analyzer');

    // Should be redirected to home (auth gate)
    await expect(page.locator('.auth-gate')).toBeVisible();
  });
});
