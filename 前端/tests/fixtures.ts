import { test as base, expect } from '@playwright/test';

// 自定义测试夹具
export const test = base.extend({
  // 页面导航辅助
  navigateTo: async ({ page }, use) => {
    const navigate = async (path: string) => {
      await page.goto(path);
      await page.waitForLoadState('networkidle');
    };
    await use(navigate);
  },

  // 等待加载完成
  waitForLoading: async ({ page }, use) => {
    const wait = async () => {
      // 等待loading spinner消失
      await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 10000 }).catch(() => {});
    };
    await use(wait);
  },
});

export { expect };
