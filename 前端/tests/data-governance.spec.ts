import { test, expect } from '@playwright/test';

test.describe('数据治理页面测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/data-governance');
    await page.waitForLoadState('networkidle');
  });

  test('TC-044: 页面加载', async ({ page }) => {
    // 验证页面内容显示
    await expect(page.locator('.ant-card, .ant-table')).toBeVisible();
  });

  test('TC-045: 告警规则', async ({ page }) => {
    // 验证告警规则区域
    const ruleSection = page.locator('text=告警规则').or(page.locator('text=规则配置'));
    await expect(ruleSection).toBeVisible();
  });

  test('TC-046: 数据校验', async ({ page }) => {
    // 验证数据校验结果
    const validationSection = page.locator('text=数据校验').or(page.locator('text=校验结果'));
    await expect(validationSection).toBeVisible();
  });
});
