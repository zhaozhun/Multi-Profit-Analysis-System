import { test, expect } from '@playwright/test';

test.describe('报表中心页面测试', () => {
  test.describe('台账报表', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/report/ledger');
      await page.waitForLoadState('networkidle');
    });

    test('TC-039: 台账报表加载', async ({ page }) => {
      // 验证报表内容显示
      await expect(page.locator('.ant-table, .ant-card')).toBeVisible();
    });
  });

  test.describe('盈利报表', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/report/profit');
      await page.waitForLoadState('networkidle');
    });

    test('TC-040: 盈利报表加载', async ({ page }) => {
      // 验证报表内容显示
      await expect(page.locator('.ant-table, .ant-card')).toBeVisible();
    });
  });

  test.describe('自定义报表', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/report/custom');
      await page.waitForLoadState('networkidle');
    });

    test('TC-041: 自定义报表配置', async ({ page }) => {
      // 验证配置项显示
      await expect(page.locator('.ant-form, .ant-card')).toBeVisible();
    });
  });

  test.describe('AI报表', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/report/ai');
      await page.waitForLoadState('networkidle');
    });

    test('TC-042: AI报表加载', async ({ page }) => {
      // 验证AI分析结果显示
      await expect(page.locator('.ant-card')).toBeVisible();
    });
  });

  test.describe('报表导出', () => {
    test('TC-043: 报表导出功能', async ({ page }) => {
      await page.goto('/report/ledger');
      await page.waitForLoadState('networkidle');

      // 设置下载监听
      const download = page.waitForEvent('download');

      // 点击导出按钮
      const exportButton = page.locator('button').filter({ hasText: '导出' }).first();
      if (await exportButton.isVisible()) {
        await exportButton.click();

        // 验证下载触发
        const downloadObj = await download;
        expect(downloadObj.suggestedFilename()).toContain('.xlsx');
      }
    });
  });
});
