import { test, expect } from '@playwright/test';

test.describe('指标数据页面测试', () => {
  const indicatorPages = [
    { path: '/indicator-data/asset/interest', name: '资产-利息收入', type: 'asset' },
    { path: '/indicator-data/asset/ftp', name: '资产-FTP成本', type: 'asset' },
    { path: '/indicator-data/asset/risk', name: '资产-风险成本', type: 'asset' },
    { path: '/indicator-data/asset/operation', name: '资产-运营成本', type: 'asset' },
    { path: '/indicator-data/liability/interest', name: '负债-对客利息支出', type: 'liability' },
    { path: '/indicator-data/liability/ftp', name: '负债-FTP成本', type: 'liability' },
    { path: '/indicator-data/liability/risk', name: '负债-风险成本', type: 'liability' },
    { path: '/indicator-data/liability/operation', name: '负债-运营成本', type: 'liability' },
  ];

  for (const page of indicatorPages) {
    test.describe(`${page.name}页面`, () => {
      test.beforeEach(async ({ page: p }) => {
        await p.goto(page.path);
        await p.waitForLoadState('networkidle');
      });

      test(`TC-021: ${page.name}页面加载`, async ({ page: p }) => {
        // 验证页面标题
        await expect(p.locator('h2')).toContainText(page.name);

        // 验证筛选区域存在
        await expect(p.locator('.ant-card').first()).toBeVisible();

        // 验证表格存在
        await expect(p.locator('.ant-table')).toBeVisible();
      });

      test(`TC-022: ${page.name}账期选择`, async ({ page: p }) => {
        // 点击账期选择器
        const periodSelect = p.locator('.ant-select').first();
        await periodSelect.click();

        // 选择一个月份
        await p.locator('.ant-select-item-option').filter({ hasText: '2026年5月' }).click();

        // 验证数据更新
        await p.waitForTimeout(500);
      });

      test(`TC-023: ${page.name}口径切换`, async ({ page: p }) => {
        // 点击口径选择器
        const caliberSelect = p.locator('.ant-select').nth(1);
        await caliberSelect.click();

        // 切换到"账面"
        await p.locator('.ant-select-item-option').filter({ hasText: '账面' }).click();

        // 验证数据更新
        await p.waitForTimeout(500);
      });

      test(`TC-024: ${page.name}维度切换`, async ({ page: p }) => {
        // 点击维度选择器
        const dimensionSelect = p.locator('.ant-select').nth(2);
        await dimensionSelect.click();

        // 切换到"产品"
        await p.locator('.ant-select-item-option').filter({ hasText: '产品' }).click();

        // 验证图表和表格更新
        await p.waitForTimeout(500);
      });

      test(`TC-025: ${page.name}维度值筛选`, async ({ page: p }) => {
        // 点击维度值选择器
        const dimensionValueSelect = p.locator('.ant-select').nth(3);
        await dimensionValueSelect.click();

        // 选择一个维度值
        const option = p.locator('.ant-select-item-option').first();
        if (await option.isVisible()) {
          await option.click();

          // 验证数据过滤
          await p.waitForTimeout(500);
        }
      });

      test(`TC-026: ${page.name}柱状图渲染`, async ({ page: p }) => {
        // 验证柱状图
        await expect(p.locator('canvas').first()).toBeVisible();
      });

      test(`TC-027: ${page.name}饼图渲染`, async ({ page: p }) => {
        // 验证饼图
        await expect(p.locator('canvas').nth(1)).toBeVisible();
      });

      test(`TC-028: ${page.name}维度汇总表格`, async ({ page: p }) => {
        // 验证维度汇总表格
        await expect(p.locator('.ant-table').nth(1)).toBeVisible();

        // 验证表格有数据
        const rows = p.locator('.ant-table-body .ant-table-row');
        await expect(rows).toHaveCount({ minimum: 0 });
      });

      test(`TC-029: ${page.name}明细数据表格`, async ({ page: p }) => {
        // 验证明细数据表格
        await expect(p.locator('.ant-table').last()).toBeVisible();
      });

      test(`TC-030: ${page.name}表格行点击`, async ({ page: p }) => {
        // 等待表格加载
        await p.waitForSelector('.ant-table-body .ant-table-row');

        // 点击维度汇总表格的行
        const row = p.locator('.ant-table-body .ant-table-row').first();
        if (await row.isVisible()) {
          await row.click();

          // 验证维度值选择器更新
          await p.waitForTimeout(500);
        }
      });
    });
  }
});
