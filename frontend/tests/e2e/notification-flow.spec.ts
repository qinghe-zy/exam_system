import { expect, test } from '@playwright/test'

test('教师可查看通知模板与通知投递日志页面', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/notifications/templates')
  await expect(page.getByText('通知模板与变量管理', { exact: true })).toBeVisible()
  await expect(page.getByText('考试发布站内提醒模板')).toBeVisible()
  await expect(page.getByText('开考前短信提醒模板')).toBeVisible()

  await page.goto('/notifications/delivery-logs')
  await expect(page.getByText('站内消息与 Mock 短信投递留痕', { exact: true })).toBeVisible()
  await expect(page.locator('.el-table').getByText('Mock 短信', { exact: true }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: '立即扫描开考前提醒' })).toBeVisible()
})
