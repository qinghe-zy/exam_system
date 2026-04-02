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

  await expect(page.getByText('共 40 道')).toBeVisible()
  await page.locator('.question-chip').first().click()
  await expect(page.getByText('已选 1 题，共 5 分')).toBeVisible()
  await page.getByRole('button', { name: '取消' }).click()

  await page.getByRole('button', { name: '随机/策略组卷' }).click()
  await expect(page.getByText('随机 / 策略组卷')).toBeVisible()
  await page.getByRole('button', { name: '随机组卷' }).click()
  await expect(page.locator('.composition-item')).toHaveCount(4)

  await page.goto('/exam/plans')
  await expect(page.getByRole('button', { name: '新建考试计划' })).toBeVisible()
  await expect(page.getByText('Access Denied')).toHaveCount(0)
})
