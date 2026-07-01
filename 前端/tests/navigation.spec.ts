import { test, expect } from '@playwright/test';

test.describe('导航和布局测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('TC-050: 菜单导航', async ({ page }) => {
    // 验证侧边栏菜单存在
    await expect(page.locator('.ant-layout-sider')).toBeVisible();

    // 点击"维度分析"菜单
    await page.locator('.ant-menu-item').filter({ hasText: '维度分析' }).click();

    // 验证子菜单展开
    await expect(page.locator('.ant-menu-submenu')).toBeVisible();

    // 点击"机构分析"
    await page.locator('.ant-menu-item').filter({ hasText: '机构分析' }).click();

    // 验证页面跳转
    await expect(page).toHaveURL(/.*\/analysis\/ORG/);
  });

  test('TC-051: 侧边栏折叠', async ({ page }) => {
    // 查找折叠按钮
    const collapseButton = page.locator('.ant-layout-sider-trigger');

    // 点击折叠
    await collapseButton.click();

    // 验证侧边栏折叠
    await expect(page.locator('.ant-layout-sider')).toHaveClass(/ant-layout-sider-collapsed/);

    // 再次点击展开
    await collapseButton.click();

    // 验证侧边栏展开
    await expect(page.locator('.ant-layout-sider')).not.toHaveClass(/ant-layout-sider-collapsed/);
  });

  test('TC-052: 响应式布局', async ({ page }) => {
    // 测试不同屏幕尺寸
    await page.setViewportSize({ width: 1200, height: 800 });
    await expect(page.locator('.ant-layout')).toBeVisible();

    await page.setViewportSize({ width: 768, height: 1024 });
    await expect(page.locator('.ant-layout')).toBeVisible();

    await page.setViewportSize({ width: 375, height: 667 });
    await expect(page.locator('.ant-layout')).toBeVisible();
  });
});
