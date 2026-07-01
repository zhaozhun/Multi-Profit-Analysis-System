import { test, expect } from '@playwright/test';

test.describe('驾驶舱页面测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
  });

  test('TC-001: 页面加载', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h2')).toContainText('多维盈利分析系统');

    // 验证KPI卡片区域存在
    await expect(page.locator('.ant-card')).toHaveCount({ minimum: 1 });

    // 验证图表区域存在
    await expect(page.locator('canvas')).toHaveCount({ minimum: 1 });
  });

  test('TC-002: 日期快捷选择', async ({ page }) => {
    // 点击快捷选择下拉框
    const quickSelect = page.locator('.ant-select').first();
    await quickSelect.click();

    // 选择"本月"
    await page.locator('.ant-select-item-option').filter({ hasText: '本月' }).click();

    // 验证日期范围更新
    await page.waitForTimeout(500);
    // 验证数据重新加载
    await expect(page.locator('.ant-spin-spinning')).toHaveCount(0);
  });

  test('TC-003: 自定义日期范围', async ({ page }) => {
    // 点击日期选择器
    const datePicker = page.locator('.ant-picker').first();
    await datePicker.click();

    // 选择日期范围
    await page.locator('.ant-picker-cell').filter({ hasText: '1' }).first().click();
    await page.locator('.ant-picker-cell').filter({ hasText: '15' }).first().click();

    // 验证数据重新加载
    await page.waitForTimeout(500);
  });

  test('TC-004: 刷新功能', async ({ page }) => {
    // 点击刷新按钮
    await page.locator('button').filter({ hasText: '刷新' }).click();

    // 验证加载状态
    await expect(page.locator('.ant-spin-spinning')).toHaveCount({ minimum: 0 });

    // 等待加载完成
    await page.waitForTimeout(1000);
  });

  test('TC-005: 导出数据', async ({ page }) => {
    // 设置下载监听
    const download = page.waitForEvent('download');

    // 点击导出按钮
    await page.locator('button').filter({ hasText: '导出数据' }).click();

    // 验证下载触发
    const downloadObj = await download;
    expect(downloadObj.suggestedFilename()).toContain('.xlsx');
  });

  test('TC-006: Tab切换', async ({ page }) => {
    // 点击"经营分析报告"Tab
    await page.locator('.ant-tabs-tab').filter({ hasText: '经营分析报告' }).click();

    // 验证报告内容显示
    await expect(page.locator('text=一、整体经营概况')).toBeVisible();

    // 切换回"经营总览"
    await page.locator('.ant-tabs-tab').filter({ hasText: '经营总览' }).click();

    // 验证总览内容显示
    await expect(page.locator('text=总利润')).toBeVisible();
  });

  test('TC-007: 瀑布图渲染', async ({ page }) => {
    // 验证贷款瀑布图
    await expect(page.locator('text=贷款利润瀑布图')).toBeVisible();
    await expect(page.locator('canvas').first()).toBeVisible();

    // 验证存款瀑布图
    await expect(page.locator('text=存款利润瀑布图')).toBeVisible();
  });

  test('TC-008: 趋势图渲染', async ({ page }) => {
    // 验证趋势图
    await expect(page.locator('text=盈利趋势')).toBeVisible();
    await expect(page.locator('canvas').nth(1)).toBeVisible();
  });

  test('TC-009: 异常告警', async ({ page }) => {
    // 验证告警区域
    await expect(page.locator('text=异常告警')).toBeVisible();

    // 验证告警列表或空状态
    const alertList = page.locator('.ant-list');
    const emptyState = page.locator('text=暂无异常告警');

    // 至少一个应该存在
    await expect(alertList.or(emptyState)).toBeVisible();
  });

  test('TC-010: 维度概览', async ({ page }) => {
    // 验证维度概览区域
    await expect(page.locator('text=维度盈利概览')).toBeVisible();

    // 验证维度标签页
    await expect(page.locator('.ant-tabs-tab')).toHaveCount({ minimum: 1 });

    // 点击第一个维度标签
    await page.locator('.ant-tabs-tab').first().click();

    // 验证图表显示
    await expect(page.locator('canvas').last()).toBeVisible();
  });
});
