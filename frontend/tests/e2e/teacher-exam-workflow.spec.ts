import { expect, test } from '@playwright/test'

import { buildActiveExamWindow } from './helpers'

test('教师策略组卷并发布考试后，学生倒计时与监考事件链路可用', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-6)
  const paperCode = `AUTO-${suffix}`
  const paperName = `自动化策略卷-${suffix}`
  const examCode = `AUTO-KS-${suffix}`
  const examName = `自动化回归考试-${suffix}`
  const { startTime, endTime } = buildActiveExamWindow()
  const startDisplay = startTime.slice(5, 16).replace('T', ' ').replace('-', '/')

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('800001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/papers')
  await page.locator('.hero-actions button').nth(1).click()
  await page.waitForURL('**/exam/papers/create**')
  await page.locator('.field-grid--four .el-input__inner').nth(0).fill(paperCode)
  await page.locator('.field-grid--four .el-input__inner').nth(1).fill(paperName)
  await page.locator('.panel-actions button').last().click()
  await page.getByRole('button', { name: '保存试卷' }).click()

  const teacherToken = await page.evaluate(() => localStorage.getItem('exam-system-template-token'))
  await expect
    .poll(async () => {
      const response = await request.get('http://127.0.0.1:8083/api/exam/papers', {
        headers: { Authorization: `Bearer ${teacherToken}` }
      })
      const payload = await response.json()
      return payload.data.some((item: any) => item.paperCode === paperCode)
    }, { timeout: 30000 })
    .toBeTruthy()
  const papersResponse = await request.get('http://127.0.0.1:8083/api/exam/papers', {
    headers: { Authorization: `Bearer ${teacherToken}` }
  })
  const papersPayload = await papersResponse.json()
  const createdPaper = papersPayload.data.find((item: any) => item.paperCode === paperCode)

  expect(createdPaper).toBeTruthy()
  expect(createdPaper.assemblyMode).toBe('STRATEGY')
  expect(createdPaper.questionCount).toBe(10)
  expect(createdPaper.questionTypeConfigs[0].count).toBeGreaterThan(0)

  const createPlanResponse = await request.post('http://127.0.0.1:8083/api/exam/plans', {
    headers: {
      Authorization: `Bearer ${teacherToken}`,
      'Content-Type': 'application/json'
    },
    data: {
      examCode,
      examName,
      paperId: createdPaper.id,
      startTime,
      endTime,
      durationMinutes: 90,
      passScore: 60,
      candidateScope: 'ASSIGNED',
      attemptLimit: 1,
      examPassword: 'AUTO2026',
      lateEntryMinutes: 180,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'STRICT',
      instructionText: '自动化回归考试，请保持页面可见并专注作答。',
      status: 1,
      publishStatus: 1,
      candidateUserIds: [15]
    }
  })
  const createPlanPayload = await createPlanResponse.json()

  expect(createPlanPayload.code).toBe(0)
  const createdPlan = createPlanPayload.data

  await page.evaluate(() => localStorage.clear())
  await page.context().clearCookies()
  await page.goto('/login')
  await page.waitForLoadState('domcontentloaded')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/exams')
  const examRow = page.getByRole('row', { name: new RegExp(`自动化回归考试-${suffix}`) })
  await expect(examRow.getByRole('button', { name: '进入考试' })).toBeVisible()
  await examRow.getByRole('button', { name: '进入考试' }).click()
  await page.locator('.el-dialog input').fill('AUTO2026')
  await page.locator('.el-dialog__footer button').last().click()
  await page.waitForSelector('.exam-overlay')

  await expect(page.locator('.metric-card--timer strong')).not.toHaveText('00:00:00')
  await expect(page.locator('.metric-card').nth(1)).toContainText(startDisplay)
  await expect(page.locator('.metric-card').nth(2)).toContainText('自动交卷')

  await page.evaluate(() => {
    Object.defineProperty(document, 'hidden', { configurable: true, get: () => true })
    document.dispatchEvent(new Event('visibilitychange'))
    window.dispatchEvent(new Event('blur'))
    document.dispatchEvent(new Event('fullscreenchange'))
  })
  await page.evaluate(() => {
    document.dispatchEvent(new ClipboardEvent('copy', { bubbles: true, cancelable: true }))
    document.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true }))
    document.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true }))
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'F5', bubbles: true, cancelable: true }))
  })
  await page.waitForTimeout(1500)

  const adminLoginResponse = await request.post('http://127.0.0.1:8083/api/auth/login', {
    data: { username: '900001', password: '123456' }
  })
  const adminPayload = await adminLoginResponse.json()
  const adminToken = adminPayload.data.token
  const eventsResponse = await request.get('http://127.0.0.1:8083/api/exam/proctor/events', {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  const eventsPayload = await eventsResponse.json()
  const createdPlanEvents = eventsPayload.data.filter((item: any) => item.examPlanId === createdPlan.id)

  expect(createdPlanEvents.length).toBeGreaterThanOrEqual(3)
  expect(createdPlanEvents.some((item: any) => item.eventType === 'TAB_SWITCH' && item.triggeredAutoSave === 1)).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'WINDOW_BLUR')).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'FULLSCREEN_EXIT')).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'COPY_ATTEMPT')).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'PASTE_ATTEMPT')).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'CONTEXT_MENU_BLOCKED')).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'SHORTCUT_BLOCKED')).toBeTruthy()
  expect(createdPlanEvents.some((item: any) => item.eventType === 'DEVICE_CONTEXT' && item.deviceFingerprint && item.deviceInfo)).toBeTruthy()
})
