import { expect, test } from '@playwright/test'

test('学生登录后进入考试、作答并提交', async ({ page, request }) => {
  const suffix = Date.now().toString().slice(-6)
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
      examCode: `STD-${suffix}`,
      examName: `学生端验证-${suffix}`,
      paperId: paper.id,
      startTime: '2026-04-03T11:00:00',
      endTime: '2026-04-03T16:00:00',
      durationMinutes: 90,
      passScore: Math.min(60, paper.totalScore),
      candidateScope: 'ASSIGNED',
      attemptLimit: 1,
      examPassword: 'STD2026',
      lateEntryMinutes: 240,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'STRICT',
      instructionText: '学生端验证，请专注作答。',
      status: 1,
      publishStatus: 1,
      candidateUserIds: [15]
    }
  })
  const planPayload = await createPlan.json()
  const createdPlanId = planPayload.data.id

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/exams')
  const examRow = page.getByRole('row', { name: new RegExp(`学生端验证-${suffix}`) })
  await expect(examRow.getByRole('button')).toBeVisible()
  await examRow.getByRole('button').click()
  await page.getByPlaceholder('请输入考试口令（如有）').fill('STD2026')
  await page.getByRole('dialog').getByRole('button', { name: '进入考试' }).click()

  await expect(page.locator('.metric-card--timer strong')).toBeVisible()
  await page.evaluate(() => {
    let full = false
    Object.defineProperty(document, 'fullscreenElement', {
      configurable: true,
      get: () => (full ? document.documentElement : null)
    })
    document.documentElement.requestFullscreen = async () => {
      full = true
      document.dispatchEvent(new Event('fullscreenchange'))
    }
    document.exitFullscreen = async () => {
      full = false
      document.dispatchEvent(new Event('fullscreenchange'))
    }
  })
  const before = await page.locator('.aside-column').boundingBox()
  await page.locator('.exam-overlay').evaluate((el) => {
    el.scrollTop = 1000
  })
  await page.waitForTimeout(300)
  const after = await page.locator('.aside-column').boundingBox()
  expect(Math.round(after?.x || 0)).toBe(Math.round(before?.x || 0))
  await page.locator('.toolbar-actions button').first().click()
  await expect(page.locator('.toolbar-actions button').first()).toHaveText('退出全屏')
  await page.locator('.toolbar-actions button').first().click()
  await page.waitForTimeout(300)
  await expect(page.locator('.toolbar-actions button').first()).toHaveText('进入全屏')
  await page.locator('.toolbar-actions button').nth(1).click()
  await expect(page.locator('.el-message')).toContainText('答案已保存')

  const answerBoxes = page.getByPlaceholder('请在此输入答案')
  await answerBoxes.nth(0).fill('@Controller')
  await answerBoxes.nth(1).fill('Atomicity|Consistency|Isolation|Durability')
  await answerBoxes.nth(2).fill('True')
  await answerBoxes.nth(3).fill('清晰的模块边界可以降低变更影响，便于扩展与维护。')
  await page.locator('.toolbar-actions button').nth(2).click()
  await expect(page.locator('.el-dialog').last()).toContainText('提交确认')
  await page.getByRole('button', { name: '再检查一下' }).click()
  const submitResult = await page.evaluate(async (createdPlanId) => {
    const token = localStorage.getItem('exam-system-template-token')
    const response = await fetch(`http://127.0.0.1:8083/api/exam/candidate/exams/${createdPlanId}/submit`, {
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
  }, createdPlanId)
  expect(submitResult.code).toBe(0)
})
