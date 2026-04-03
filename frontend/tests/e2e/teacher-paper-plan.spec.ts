import { expect, test } from '@playwright/test'

test('教师可在试卷管理中取题并正常进入考试发布页', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/papers')
  await expect(page.getByRole('button', { name: '新建试卷' })).toBeVisible()
  await page.getByRole('button', { name: '新建试卷' }).click()
  await page.waitForURL('**/exam/papers/create**')

  await expect(page.getByText(/当前学科\s*语文\s*共\s*\d+\s*道题/)).toBeVisible()
  await page.locator('.question-chip').first().locator('button').click()
  await expect(page.getByText('已选题量')).toBeVisible()
  await expect(page.getByText('卷面预览')).toBeVisible()
  await page.getByRole('button', { name: '返回试卷列表' }).click()

  await page.getByRole('button', { name: '新建策略组卷' }).click()
  await page.waitForURL('**/exam/papers/create**')
  await expect(page.getByRole('button', { name: '执行策略组卷' })).toBeVisible()
  await page.getByRole('button', { name: '执行策略组卷' }).click()
  await page.getByRole('button', { name: '卷面预览' }).click()
  await expect(page.locator('.composition-item')).toHaveCount(10)

  await page.goto('/exam/plans')
  await expect(page.getByRole('button', { name: '新建考试' })).toBeVisible()
  await expect(page.getByText('Access Denied')).toHaveCount(0)
})
