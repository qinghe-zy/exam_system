import { expect, test } from '@playwright/test'

test('教师可导出成绩单与分析报表', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/records')
  const scoreDownload = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出成绩单' }).click()
  const scoreArtifact = await scoreDownload
  expect(scoreArtifact.suggestedFilename()).toContain('score-records')

  await page.goto('/exam/analytics')
  const analysisDownload = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出分析报表' }).click()
  const analysisArtifact = await analysisDownload
  expect(analysisArtifact.suggestedFilename()).toContain('analysis-overview')
})
