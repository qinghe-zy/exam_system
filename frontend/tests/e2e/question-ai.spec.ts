import { expect, test } from '@playwright/test'

test('题库页提供 AI 草稿入口，并在未配置密钥时给出明确中文提示', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/questions')
  await page.locator('.hero-actions button').first().click()
  const items = page.locator('.el-dialog .el-form-item')
  await items.nth(0).locator('input').fill('数学')
  await items.nth(3).locator('input').fill('函数概念')
  await items.nth(5).locator('textarea').fill('生成一道偏重理解题的函数基础题')
  await page.locator('.el-dialog__footer button').last().click()

  if (process.env.AI_API_KEY) {
    await expect(page.locator('.el-message')).toContainText('AI 辅助生成草稿')
  } else {
    await expect(page.locator('.el-message')).toContainText('AI 功能尚未配置')
  }
})
