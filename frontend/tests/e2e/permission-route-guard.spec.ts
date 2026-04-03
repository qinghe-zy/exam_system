import { expect, test } from '@playwright/test'

test('学生直达未授权监考页面时会被路由守卫拦回可访问入口', async ({ page }) => {
  const forbiddenResponses: Array<{ url: string; status: number }> = []
  page.on('response', (response) => {
    if (response.url().includes('/api/') && response.status() === 403) {
      forbiddenResponses.push({ url: response.url(), status: response.status() })
    }
  })

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/proctor')

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: '在线考试系统运行概览' })).toBeVisible()
  expect(forbiddenResponses).toHaveLength(0)
})
