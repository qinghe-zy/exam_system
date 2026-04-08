import { expect, test } from '@playwright/test'

test('管理员可查看登录风险记录页', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('900001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/system/login-risks')
  await expect(page.getByRole('heading', { name: '登录风险记录' })).toBeVisible()
  await expect(page.getByText('中高风险')).toBeVisible()
  await expect(page.getByRole('cell', { name: '20260001' }).first()).toBeVisible()
})
