import { expect, test } from '@playwright/test'

import { buildActiveExamWindow } from './helpers'

test('严格考试态下，设备检测不通过会阻止进入考试工作区', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-6)
  const { startTime, endTime } = buildActiveExamWindow()
  const teacherLogin = await request.post('http://127.0.0.1:8083/api/auth/login', {
    data: { username: '800001', password: '123456' }
  })
  const teacherPayload = await teacherLogin.json()
  const teacherToken = teacherPayload.data.token
  const papersResponse = await request.get('http://127.0.0.1:8083/api/exam/papers', {
    headers: { Authorization: `Bearer ${teacherToken}` }
  })
  const papersPayload = await papersResponse.json()
  const paper = papersPayload.data.find((item: any) => item.publishStatus === 1) || papersPayload.data[0]

  const createPlan = await request.post('http://127.0.0.1:8083/api/exam/plans', {
    headers: { Authorization: `Bearer ${teacherToken}` },
    data: {
      examCode: `DEV-${suffix}`,
      examName: `设备检测验证-${suffix}`,
      paperId: paper.id,
      startTime,
      endTime,
      durationMinutes: 90,
      passScore: Math.min(60, paper.totalScore),
      candidateScope: 'ASSIGNED',
      attemptLimit: 1,
      examPassword: 'DEV2026',
      lateEntryMinutes: 240,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'STRICT',
      instructionText: '设备检测验证。',
      status: 1,
      publishStatus: 1,
      candidateUserIds: [15]
    }
  })
  const planPayload = await createPlan.json()
  expect(planPayload.code).toBe(0)

  await page.setViewportSize({ width: 1024, height: 640 })
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/exams')
  const examRow = page.getByRole('row', { name: new RegExp(`设备检测验证-${suffix}`) })
  await expect(examRow.getByRole('button')).toBeVisible()
  await examRow.getByRole('button').click()
  await page.getByPlaceholder('请输入考试口令（如有）').fill('DEV2026')
  await page.getByRole('dialog').getByRole('button', { name: '进入考试' }).click()

  await expect(page.getByText('设备检测结果')).toBeVisible()
  await expect(page.getByText('当前设备检测未通过，系统已阻止进入考试。')).toBeVisible()
  await expect(page.getByText(/当前窗口 1024x640/)).toBeVisible()
  await expect(page.locator('.exam-overlay')).toHaveCount(0)
})
