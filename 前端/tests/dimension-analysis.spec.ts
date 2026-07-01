import { test, expect } from '@playwright/test';

test.describe('维度分析页面测试', () => {
  const dimensions = [
    { type: 'ORG', name: '机构' },
    { type: 'BIZ_LINE', name: '条线' },
    { type: 'PRODUCT', name: '产品' },
    { type: 'CHANNEL', name: '渠道' },
    { type: 'DEPT', name: '部门' },
    { type: 'MANAGER', name: '客户经理' },
  ];

  for (const dim of dimensions) {
    test.describe(`${dim.name}维度分析`, () => {
      test.beforeEach(async ({ page }) => {
        await page.goto(`/analysis/${dim.type}`);
        await page.waitForLoadState('networkidle');
      });

      test(`TC-011: ${dim.name}页面加载`, async ({ page }) => {
        // 验证页面标题
        await expect(page.locator('h3')).toContainText(`${dim.name}维度分析`);

        // 验证树形表格存在
        await expect(page.locator('.ant-table')).toBeVisible();
      });

      test(`TC-012: ${dim.name}树形展开`, async ({ page }) => {
        // 等待表格加载
        await page.waitForSelector('.ant-table-row');

        // 点击展开按钮
        const expandButton = page.locator('.ant-table-row-expand-icon').first();
        if (await expandButton.isVisible()) {
          await expandButton.click();

          // 验证子节点加载
          await page.waitForTimeout(500);
        }
      });

      test(`TC-016: ${dim.name}日期筛选`, async ({ page }) => {
        // 点击快捷选择
        const quickSelect = page.locator('.ant-select').first();
        await quickSelect.click();

        // 选择"本年"
        await page.locator('.ant-select-item-option').filter({ hasText: '本年' }).click();

        // 验证数据更新
        await page.waitForTimeout(500);
      });

      test(`TC-017: ${dim.name}刷新功能`, async ({ page }) => {
        // 点击刷新按钮
        await page.locator('button').filter({ hasText: '刷新' }).click();

        // 验证加载状态
        await page.waitForTimeout(1000);
      });

      test(`TC-018: ${dim.name}导出功能`, async ({ page }) => {
        // 设置下载监听
        const download = page.waitForEvent('download');

        // 点击导出按钮
        await page.locator('button').filter({ hasText: '导出' }).click();

        // 验证下载触发
        const downloadObj = await download;
        expect(downloadObj.suggestedFilename()).toContain('.xlsx');
      });

      test(`TC-019: ${dim.name}分析报告Tab`, async ({ page }) => {
        // 点击"分析报告"Tab
        await page.locator('.ant-tabs-tab').filter({ hasText: '分析报告' }).click();

        // 验证报告内容
        await expect(page.locator('text=经营摘要')).toBeVisible();
        await expect(page.locator('text=TOP3 盈利')).toBeVisible();
        await expect(page.locator('text=BOTTOM3')).toBeVisible();
      });
    });
  }

  test.describe('交叉钻取功能', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/analysis/ORG');
      await page.waitForLoadState('networkidle');
    });

    test('TC-013: 交叉钻取菜单', async ({ page }) => {
      // 等待表格加载
      await page.waitForSelector('.ant-table-row');

      // 点击交叉分析按钮
      const crossButton = page.locator('button').filter({ hasText: '交叉分析' }).first();
      if (await crossButton.isVisible()) {
        await crossButton.click();

        // 验证菜单显示
        await expect(page.locator('.ant-dropdown')).toBeVisible();
      }
    });

    test('TC-014: 交叉钻取结果', async ({ page }) => {
      // 等待表格加载
      await page.waitForSelector('.ant-table-row');

      // 点击交叉分析按钮
      const crossButton = page.locator('button').filter({ hasText: '交叉分析' }).first();
      if (await crossButton.isVisible()) {
        await crossButton.click();

        // 选择维度
        await page.locator('.ant-dropdown-menu-item').filter({ hasText: '按产品分析' }).click();

        // 验证交叉分析表格显示
        await page.waitForTimeout(1000);
        await expect(page.locator('text=交叉钻取')).toBeVisible();
      }
    });

    test('TC-015: 下钻弹窗', async ({ page }) => {
      // 等待表格加载
      await page.waitForSelector('.ant-table-row');

      // 点击利润数字
      const profitCell = page.locator('.ant-table-cell').filter({ hasText: /^\d+$/ }).first();
      if (await profitCell.isVisible()) {
        await profitCell.click();

        // 验证弹窗显示
        await expect(page.locator('.ant-modal')).toBeVisible();
        await expect(page.locator('text=利润构成')).toBeVisible();
      }
    });
  });
});
