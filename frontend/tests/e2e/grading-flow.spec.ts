import { expect, test } from '@playwright/test'

test('阅卷老师登录后完成主观题评分', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('grader')
  await page.locator('input.el-input__inner').nth(1).fill('grader123')
  await page.getByRole('button', { name: 'Enter workspace' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/grading')
  await expect(page.getByRole('button', { name: 'Open' })).toBeVisible()
  await page.getByRole('button', { name: 'Open' }).click()
  await expect(page.getByRole('button', { name: 'Submit Grades' })).toBeVisible()
  await page.getByRole('spinbutton').fill('40')
  await page.locator('textarea').last().fill('答案覆盖了模块边界和可维护性的关键点。')
  await page.getByRole('button', { name: 'Submit Grades' }).click()
  await expect(page.getByText('Grading updated')).toBeVisible()
})
