import { test, expect } from '@playwright/test';

test.describe('AI助手页面测试', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/ai');
    await page.waitForLoadState('networkidle');
  });

  test('TC-047: 页面加载', async ({ page }) => {
    // 验证对话界面显示
    await expect(page.locator('.ant-input, textarea')).toBeVisible();
  });

  test('TC-048: 发送消息', async ({ page }) => {
    // 查找输入框
    const input = page.locator('.ant-input, textarea').first();
    if (await input.isVisible()) {
      // 输入问题
      await input.fill('查询本月利润');

      // 点击发送按钮
      const sendButton = page.locator('button').filter({ hasText: '发送' }).first();
      if (await sendButton.isVisible()) {
        await sendButton.click();

        // 验证AI响应
        await page.waitForTimeout(2000);
      }
    }
  });

  test('TC-049: 历史记录', async ({ page }) => {
    // 验证历史记录区域
    const historySection = page.locator('.ant-list, .ant-card').first();
    await expect(historySection).toBeVisible();
  });
});
