import { expect, test } from '@playwright/test'

test('学生可从消息中心进入我的成绩详情页', async ({ page, request }) => {
  const loginResponse = await request.post('http://127.0.0.1:8083/api/auth/login', {
    data: { username: '20260001', password: '123456' }
  })
  const loginPayload = await loginResponse.json()
  const token = loginPayload.data.token
  const messagesResponse = await request.get('http://127.0.0.1:8083/api/messages/my', {
    headers: { Authorization: `Bearer ${token}` }
  })
  const messagesPayload = await messagesResponse.json()
  const scoreMessage = messagesPayload.data.find((item: any) => item.messageType === 'SCORE_PUBLISH' && item.relatedId)

  expect(scoreMessage).toBeTruthy()

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/messages')
  const scoreRow = page.getByRole('row', { name: /成绩发布提醒/ }).first()
  await expect(scoreRow).toBeVisible()
  await scoreRow.getByRole('button', { name: '查看详情' }).click()

  await expect(page).toHaveURL(new RegExp(`/candidate/scores\\?recordId=${scoreMessage.relatedId}`))
  await expect(page.getByText('成绩详情')).toBeVisible()
  await expect(page.getByText('逐题结果')).toBeVisible()
})
