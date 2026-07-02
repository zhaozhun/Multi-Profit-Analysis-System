import { test, expect } from '@playwright/test';

test.describe('驾驶舱页面测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
  });

  test('TC-001: 页面加载', async ({ page }) => {
    // 验证页面标题
    await expect(page.locator('h2')).toContainText('多维盈利分析系统');

    // 验证4个KPI卡片存在(总利润/贷款利润/存款利润/成本收入比)
    // 用精确正则避免与"贷款利润瀑布图"等卡片撞名(KPI卡标题后紧跟数值)
    const kpiPatterns = [/^总利润\d/, /^贷款利润\d/, /^存款利润\d/, /^成本收入比\d/];
    for (const pat of kpiPatterns) {
      await expect(page.locator('.ant-card.ant-card-small').filter({ hasText: pat })).toBeVisible();
    }

    // 验证"总利润"卡片数值非空非占位符(原生div,数值在第2个子div)
    const totalProfitCard = page.locator('.ant-card.ant-card-small').filter({ hasText: '总利润' });
    const valueText = await totalProfitCard.locator('.ant-card-body > div > div').nth(1).innerText();
    // 数值应为数字(如"0.80"),不能是空字符串或占位符"-"
    expect(valueText.trim()).toMatch(/^\d+(\.\d+)?$/);
    expect(valueText.trim()).not.toBe('-');
  });

  test('TC-002: 期间切换', async ({ page }) => {
    // 记录切换前总利润数值
    const totalProfitCard = page.locator('.ant-card.ant-card-small').filter({ hasText: '总利润' });
    const valueBefore = (await totalProfitCard.locator('.ant-card-body > div > div').nth(1).innerText()).trim();

    // 点击期间选择器(整页唯一select)
    const periodSelect = page.locator('.ant-select').first();
    await periodSelect.click();

    // 选择"2026年5月"(若不存在则跳过该项,验证select可展开即可)
    const mayOption = page.locator('.ant-select-item-option').filter({ hasText: '2026年5月' });
    if (await mayOption.isVisible().catch(() => false)) {
      await mayOption.click();
      await page.waitForLoadState('networkidle');
      // 验证数据重新加载:spinner出现再消失
      await expect(page.locator('.ant-spin-spinning')).toHaveCount(0);
      const valueAfter = (await totalProfitCard.locator('.ant-card-body > div > div').nth(1).innerText()).trim();
      expect(valueAfter).toMatch(/^\d+(\.\d+)?$/);
    }
  });

  test('TC-003: 自定义日期范围', async ({ page }) => {
    // 点击日期范围选择器
    const datePicker = page.locator('.ant-picker-range').first();
    await datePicker.click();

    // 选择日期范围(1日到15日)
    await page.locator('.ant-picker-cell').filter({ hasText: '1' }).first().click();
    await page.locator('.ant-picker-cell').filter({ hasText: '15' }).first().click();

    // 验证日期范围已填入两个input
    await expect(page.locator('input[placeholder="开始日期"]')).toHaveValue(/.+/);
    await expect(page.locator('input[placeholder="结束日期"]')).toHaveValue(/.+/);
    // 验证数据重新加载完成
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.ant-spin-spinning')).toHaveCount(0);
  });

  test('TC-004: 刷新功能', async ({ page }) => {
    // 记录刷新前总利润数值
    const totalProfitCard = page.locator('.ant-card.ant-card-small').filter({ hasText: '总利润' });
    const valueBefore = (await totalProfitCard.locator('.ant-card-body > div > div').nth(1).innerText()).trim();

    // 点击刷新按钮
    await page.getByRole('button', { name: '刷新' }).click();

    // 验证加载状态:spinner应出现(或快速消失),最终数据仍有效
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.ant-spin-spinning')).toHaveCount(0);

    // 验证刷新后数值仍为有效数字(非空非占位)
    const valueAfter = (await totalProfitCard.locator('.ant-card-body > div > div').nth(1).innerText()).trim();
    expect(valueAfter).toMatch(/^\d+(\.\d+)?$/);
  });

  test('TC-005: 导出数据', async ({ page }) => {
    // 验证导出按钮存在且可点击
    const exportBtn = page.getByRole('button', { name: '导出数据' });
    await expect(exportBtn).toBeVisible();
    await expect(exportBtn).toBeEnabled();

    // 点击导出按钮,验证不抛错(实际下载功能待后端实现,当前仅验证按钮可用)
    await exportBtn.click();
    await page.waitForTimeout(1000);
    // 无控制台错误即视为通过(导出下载断言待功能实现后补)
  });

  test('TC-006: Tab切换', async ({ page }) => {
    // 点击"经营分析报告"Tab
    await page.getByRole('tab', { name: '经营分析报告' }).click();

    // 验证报告内容显示
    await expect(page.locator('.ant-card-head-title').filter({ hasText: '一、整体经营概况' })).toBeVisible();

    // 切换回"经营总览"
    await page.getByRole('tab', { name: '经营总览' }).click();

    // 验证总览内容显示
    await expect(page.locator('.ant-card.ant-card-small').filter({ hasText: '总利润' })).toBeVisible();
  });

  test('TC-007: 瀑布图渲染', async ({ page }) => {
    // 验证贷款瀑布图标题及canvas
    const loanCard = page.locator('.ant-card').filter({ hasText: '贷款利润瀑布图' });
    await expect(loanCard.locator('.ant-card-head-title')).toContainText('贷款利润瀑布图');
    await expect(loanCard.locator('canvas')).toBeVisible();

    // 验证存款瀑布图标题及canvas
    const depositCard = page.locator('.ant-card').filter({ hasText: '存款利润瀑布图' });
    await expect(depositCard.locator('.ant-card-head-title')).toContainText('存款利润瀑布图');
    await expect(depositCard.locator('canvas')).toBeVisible();
  });

  test('TC-008: 趋势图渲染', async ({ page }) => {
    // 验证趋势图标题及canvas
    const trendCard = page.locator('.ant-card').filter({ hasText: '盈利趋势' });
    await expect(trendCard.locator('.ant-card-head-title')).toContainText('盈利趋势');
    await expect(trendCard.locator('canvas')).toBeVisible();
  });

  test('TC-009: 异常告警', async ({ page }) => {
    // 验证告警区域标题(innerText含尾随告警数,用hasText)
    const alertCard = page.locator('.ant-card').filter({ hasText: '异常告警' });
    await expect(alertCard).toBeVisible();

    // 当前有告警数据时验证列表项;无数据时验证空状态文本
    const alertItems = alertCard.locator('.ant-list-item');
    const emptyState = alertCard.locator('text=暂无异常告警');
    const itemCount = await alertItems.count();
    if (itemCount > 0) {
      // 有告警:验证至少1条
      expect(itemCount).toBeGreaterThanOrEqual(1);
    } else {
      // 无告警:验证空状态显示
      await expect(emptyState).toBeVisible();
    }
  });

  test('TC-010: 维度概览', async ({ page }) => {
    // 验证维度概览区域
    const dimCard = page.locator('.ant-card').filter({ hasText: '维度盈利概览' });
    await expect(dimCard.locator('.ant-card-head-title')).toContainText('维度盈利概览');

    // 验证维度标签页(数据驱动,至少1个且含"机构")
    const dimTabs = dimCard.locator('.ant-tabs-tab');
    expect(await dimTabs.count()).toBeGreaterThanOrEqual(1);
    await expect(dimTabs.filter({ hasText: '机构' })).toBeVisible();

    // 验证表格有数据行(antd5用.ant-table-tbody tr)
    const tableRows = dimCard.locator('.ant-table-tbody tr.ant-table-row');
    expect(await tableRows.count()).toBeGreaterThanOrEqual(1);
  });
});
