import { expect, test } from '@playwright/test'

test('学生登录后进入考试、作答并提交', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/exams')
  await expect(page.getByRole('cell', { name: '进入考试' }).getByRole('button')).toBeVisible()
  await page.getByRole('cell', { name: '进入考试' }).getByRole('button').click()
  await page.getByPlaceholder('请输入考试口令（如有）').fill('YW2026')
  await page.getByRole('dialog').getByRole('button', { name: '进入考试' }).click()

  await expect(page.getByRole('button', { name: '提交试卷' })).toBeVisible()
  const answerBoxes = page.getByPlaceholder('请在此输入答案')
  await answerBoxes.nth(0).fill('@Controller')
  await answerBoxes.nth(1).fill('Atomicity|Consistency|Isolation|Durability')
  await answerBoxes.nth(2).fill('True')
  await answerBoxes.nth(3).fill('清晰的模块边界可以降低变更影响，便于扩展与维护。')
  const submitResult = await page.evaluate(async () => {
    const token = localStorage.getItem('exam-system-template-token')
    const response = await fetch('http://127.0.0.1:8083/api/exam/candidate/exams/1/submit', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        answers: [
          { questionId: 1, answerContent: '@Controller' },
          { questionId: 2, answerContent: 'Atomicity|Consistency|Isolation|Durability' },
          { questionId: 3, answerContent: 'True' },
          { questionId: 4, answerContent: '清晰的模块边界可以降低变更影响，便于扩展与维护。' }
        ]
      })
    })
    return response.json()
  })
  expect(submitResult.code).toBe(0)
})
