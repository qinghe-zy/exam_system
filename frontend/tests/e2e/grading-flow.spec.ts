import { expect, test } from '@playwright/test'

test('阅卷老师登录后完成主观题评分', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('810001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/grading')
  await expect(page.getByRole('button', { name: '打开' }).first()).toBeVisible()
  await page.getByRole('button', { name: '打开' }).first().click()
  await expect(page.getByRole('button', { name: '提交评分' })).toBeVisible()
  await page.getByRole('spinbutton').fill('40')
  await page.locator('textarea').last().fill('答案覆盖了模块边界和可维护性的关键点。')
  await page.getByRole('button', { name: '提交评分' }).click()
  await expect(page.getByText('阅卷结果已更新')).toBeVisible()
})
