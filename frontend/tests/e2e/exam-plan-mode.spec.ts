import { expect, test } from '@playwright/test'

import { buildActiveExamWindow } from './helpers'

test('补考/重考考试计划可在教师端和学生端展示考试类型与批次', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-6)
  const { startTime, endTime } = buildActiveExamWindow()

  const teacherLoginResponse = await request.post('http://127.0.0.1:8083/api/auth/login', {
    data: { username: '800001', password: '123456' }
  })
  const teacherLoginPayload = await teacherLoginResponse.json()
  const teacherToken = teacherLoginPayload.data.token

  const createPlanResponse = await request.post('http://127.0.0.1:8083/api/exam/plans', {
    headers: {
      Authorization: `Bearer ${teacherToken}`,
      'Content-Type': 'application/json'
    },
    data: {
      examCode: `RETAKE-E2E-${suffix}`,
      examName: `自动化重考考试-${suffix}`,
      examMode: 'RETAKE',
      batchLabel: '重考批次 B',
      sourceExamPlanId: 1,
      paperId: 1,
      startTime,
      endTime,
      durationMinutes: 90,
      passScore: 60,
      candidateScope: 'ASSIGNED',
      attemptLimit: 2,
      examPassword: 'RETAKE2026',
      lateEntryMinutes: 180,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'BASIC',
      instructionText: '用于验证补考 / 重考展示。',
      status: 1,
      publishStatus: 1,
      candidateUserIds: [15]
    }
  })
  const createPlanPayload = await createPlanResponse.json()
  expect(createPlanPayload.code).toBe(0)

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/plans')
  const teacherRow = page.getByRole('row', { name: new RegExp(`自动化重考考试-${suffix}`) })
  await expect(teacherRow).toContainText('重考')
  await expect(teacherRow).toContainText('重考批次 B')
  await expect(teacherRow).toContainText('2026级语文阶段测验')

  await page.evaluate(() => localStorage.clear())
  await page.context().clearCookies()

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/exams')
  const studentRow = page.getByRole('row', { name: new RegExp(`自动化重考考试-${suffix}`) })
  await expect(studentRow).toContainText('重考')
  await expect(studentRow).toContainText('重考批次 B')
  await expect(studentRow).toContainText('2026级语文阶段测验')
})
