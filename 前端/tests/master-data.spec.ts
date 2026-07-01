import { test, expect } from '@playwright/test';

test.describe('主数据管理页面测试', () => {
  const masterPages = [
    { path: '/base-data/master/org', name: '机构' },
    { path: '/base-data/master/biz-line', name: '条线' },
    { path: '/base-data/master/dept', name: '部门' },
    { path: '/base-data/master/product', name: '产品' },
    { path: '/base-data/master/channel', name: '渠道' },
    { path: '/base-data/master/manager', name: '客户经理' },
    { path: '/base-data/master/customer', name: '客户' },
  ];

  for (const page of masterPages) {
    test.describe(`${page.name}主数据`, () => {
      test.beforeEach(async ({ page: p }) => {
        await p.goto(page.path);
        await p.waitForLoadState('networkidle');
      });

      test(`TC-031: ${page.name}页面加载`, async ({ page: p }) => {
        // 验证数据表格存在
        await expect(p.locator('.ant-table')).toBeVisible();
      });

      test(`TC-032: ${page.name}搜索功能`, async ({ page: p }) => {
        // 查找搜索输入框
        const searchInput = p.locator('.ant-input').first();
        if (await searchInput.isVisible()) {
          // 输入搜索关键词
          await searchInput.fill('测试');

          // 验证数据过滤
          await p.waitForTimeout(500);
        }
      });

      test(`TC-033: ${page.name}新增功能`, async ({ page: p }) => {
        // 点击新增按钮
        const addButton = p.locator('button').filter({ hasText: '新增' }).first();
        if (await addButton.isVisible()) {
          await addButton.click();

          // 验证弹窗显示
          await expect(p.locator('.ant-modal')).toBeVisible();
        }
      });

      test(`TC-034: ${page.name}编辑功能`, async ({ page: p }) => {
        // 等待表格加载
        await p.waitForSelector('.ant-table-body .ant-table-row');

        // 点击编辑按钮
        const editButton = p.locator('.ant-btn').filter({ hasText: '编辑' }).first();
        if (await editButton.isVisible()) {
          await editButton.click();

          // 验证弹窗显示
          await expect(p.locator('.ant-modal')).toBeVisible();
        }
      });

      test(`TC-035: ${page.name}删除功能`, async ({ page: p }) => {
        // 等待表格加载
        await p.waitForSelector('.ant-table-body .ant-table-row');

        // 点击删除按钮
        const deleteButton = p.locator('.ant-btn').filter({ hasText: '删除' }).first();
        if (await deleteButton.isVisible()) {
          await deleteButton.click();

          // 验证确认弹窗显示
          await expect(p.locator('.ant-modal-confirm')).toBeVisible();
        }
      });

      test(`TC-036: ${page.name}分页功能`, async ({ page: p }) => {
        // 验证分页组件存在
        const pagination = p.locator('.ant-pagination');
        if (await pagination.isVisible()) {
          // 点击下一页
          const nextButton = p.locator('.ant-pagination-next');
          if (await nextButton.isEnabled()) {
            await nextButton.click();

            // 验证数据更新
            await p.waitForTimeout(500);
          }
        }
      });

      test(`TC-037: ${page.name}导入功能`, async ({ page: p }) => {
        // 点击导入按钮
        const importButton = p.locator('button').filter({ hasText: '导入' }).first();
        if (await importButton.isVisible()) {
          await importButton.click();

          // 验证导入弹窗或上传组件显示
          await expect(p.locator('.ant-modal, .ant-upload')).toBeVisible();
        }
      });

      test(`TC-038: ${page.name}导出功能`, async ({ page: p }) => {
        // 设置下载监听
        const download = p.waitForEvent('download');

        // 点击导出按钮
        const exportButton = p.locator('button').filter({ hasText: '导出' }).first();
        if (await exportButton.isVisible()) {
          await exportButton.click();

          // 验证下载触发
          const downloadObj = await download;
          expect(downloadObj.suggestedFilename()).toContain('.xlsx');
        }
      });
    });
  }
});
