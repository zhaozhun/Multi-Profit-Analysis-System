import { test, expect } from '@playwright/test';

// 导航 DOM 取证(2026-07-02)关键结论:
// 1. 菜单结构:3个叶子menu-item(驾驶舱/数据治理/AI助手)+4个submenu(维度分析/指标数据/主数据管理/报表中心)
// 2. "维度分析"是submenu-title非menu-item,点击展开子菜单(6项:机构/条线/产品/渠道/部门/客户经理分析)
// 3. 折叠按钮有效,点击后sider获得ant-layout-sider-collapsed class(antd inline折叠仅CSS隐藏文本,DOM文本仍在)
// 4. .ant-layout有2个匹配(根has-sider+内容区)→strict violation,改用.ant-layout-has-sider(唯一)
// 5. 移动端375未自动折叠(本项目未配置breakpoint,sider保持200px,非bug,仅未做移动响应式)

test.describe('导航和布局测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('TC-050: 菜单导航', async ({ page }) => {
    // 验证侧边栏菜单存在
    await expect(page.locator('.ant-layout-sider')).toBeVisible();

    // "维度分析"是submenu-title,点击展开子菜单
    await page.locator('.ant-menu-submenu-title').filter({ hasText: '维度分析' }).click();

    // 验证子菜单展开(出现ant-menu-submenu-open)
    const submenu = page.locator('.ant-menu-submenu').filter({ hasText: '维度分析' });
    await expect(submenu).toHaveClass(/ant-menu-submenu-open/);

    // 验证6个子菜单项存在
    const subItems = submenu.locator('.ant-menu-item');
    expect(await subItems.count()).toBeGreaterThanOrEqual(6);
    await expect(subItems.filter({ hasText: '机构分析' })).toBeVisible();

    // 点击"机构分析"
    await subItems.filter({ hasText: '机构分析' }).click();

    // 验证页面跳转
    await expect(page).toHaveURL(/\/analysis\/ORG$/);
  });

  test('TC-051: 侧边栏折叠', async ({ page }) => {
    // 查找折叠按钮
    const collapseButton = page.locator('.ant-layout-sider-trigger');
    await expect(collapseButton).toBeVisible();

    const sider = page.locator('.ant-layout-sider').first();

    // 点击折叠
    await collapseButton.click();

    // 验证侧边栏折叠(获得collapsed class)
    await expect(sider).toHaveClass(/ant-layout-sider-collapsed/);

    // 再次点击展开
    await collapseButton.click();

    // 验证侧边栏展开(失去collapsed class)
    await expect(sider).not.toHaveClass(/ant-layout-sider-collapsed/);
  });

  test('TC-052: 响应式布局', async ({ page }) => {
    // .ant-layout有2个匹配会触发strict violation,改用唯一的.ant-layout-has-sider
    const layout = page.locator('.ant-layout-has-sider');

    // 测试不同屏幕尺寸(本项目未做移动端自动折叠,sider保持可见,非bug)
    await page.setViewportSize({ width: 1200, height: 800 });
    await expect(layout).toBeVisible();
    await expect(page.locator('.ant-layout-sider')).toBeVisible();

    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(layout).toBeVisible();

    await page.setViewportSize({ width: 375, height: 667 });
    await expect(layout).toBeVisible();
    // 移动端sider仍可见(未配置响应式折叠,记录为已知现状)
    await expect(page.locator('.ant-layout-sider')).toBeVisible();
  });
});
