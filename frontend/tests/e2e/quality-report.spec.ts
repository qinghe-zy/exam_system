import { expect, test } from '@playwright/test'

test('教师可查看并导出考试质量报告', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/analytics')
  await expect(page.getByText('组织对比')).toBeVisible()
  await expect(page.getByText('历次考试趋势')).toBeVisible()
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出质量报告' }).click()
  const artifact = await download
  expect(artifact.suggestedFilename()).toContain('exam-quality-report')

  await page.getByRole('button', { name: '查看质量报告' }).click()
  await expect(page.getByText('考试质量报告', { exact: true })).toBeVisible()
  await expect(page.getByText('维度评分')).toBeVisible()
  await expect(page.getByText('建议动作')).toBeVisible()
})
