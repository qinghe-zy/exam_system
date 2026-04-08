import { expect, test } from '@playwright/test'

test('学生可查看答卷回看与错题本', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/review-center')
  await expect(page.getByText('答卷档案')).toBeVisible()
  await expect(page.getByText('总题数')).toBeVisible()
  await expect(page.getByText('待复查标记')).toBeVisible()

  await page.getByRole('tab', { name: '错题本' }).click()
  await expect(page.getByText('累计错')).toBeVisible()
  await page.getByRole('button', { name: '定位到最近答卷' }).first().click()

  await expect(page.getByRole('tab', { name: '答卷回看' })).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByText('答卷档案')).toBeVisible()
})
