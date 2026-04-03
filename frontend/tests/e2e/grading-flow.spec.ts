import { expect, test } from '@playwright/test'

import { buildActiveExamWindow } from './helpers'

test('阅卷老师登录后完成主观题评分', async ({ page, request }) => {
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
      examCode: `GRD-${suffix}`,
      examName: `阅卷验证-${suffix}`,
      paperId: paper.id,
      startTime,
      endTime,
      durationMinutes: 90,
      passScore: Math.min(60, paper.totalScore),
      candidateScope: 'ASSIGNED',
      attemptLimit: 1,
      examPassword: 'GRD2026',
      lateEntryMinutes: 240,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'STRICT',
      instructionText: '阅卷验证考试。',
      status: 1,
      publishStatus: 1,
      candidateUserIds: [15]
    }
  })
  const planPayload = await createPlan.json()
  const examPlanId = planPayload.data.id

  const studentLogin = await request.post('http://127.0.0.1:8083/api/auth/login', {
    data: { username: '20260001', password: '123456' }
  })
  const studentPayload = await studentLogin.json()
  const studentToken = studentPayload.data.token
  const workspaceResponse = await request.get(`http://127.0.0.1:8083/api/exam/candidate/exams/${examPlanId}?examPassword=GRD2026`, {
    headers: { Authorization: `Bearer ${studentToken}` }
  })
  const workspacePayload = await workspaceResponse.json()
  const answers = workspacePayload.data.items.slice(0, 4).map((item: any) => {
    if (item.questionType === 'MULTIPLE_CHOICE') {
      return { questionId: item.questionId, answerContent: item.answerContent || '说法一：符合教材结论|说法三：符合题意要求' }
    }
    if (item.questionType === 'SINGLE_CHOICE') {
      return { questionId: item.questionId, answerContent: item.answerContent || '选项一：符合教材结论' }
    }
    if (item.questionType === 'TRUE_FALSE') {
      return { questionId: item.questionId, answerContent: item.answerContent || '正确' }
    }
    return { questionId: item.questionId, answerContent: '这是用于阅卷验证的主观题答案。' }
  })
  await request.post(`http://127.0.0.1:8083/api/exam/candidate/exams/${examPlanId}/submit`, {
    headers: {
      Authorization: `Bearer ${studentToken}`,
      'Content-Type': 'application/json'
    },
    data: { answers }
  })

  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('810001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/grading')
  await expect(page.getByRole('button', { name: '打开' }).first()).toBeVisible()
  await page.getByRole('button', { name: '打开' }).first().click()
  await expect(page.getByRole('button', { name: '提交评分' })).toBeVisible()
  await expect(page.getByRole('spinbutton').first()).toBeVisible()
  await page.getByRole('spinbutton').first().fill('12')
  await page.getByPlaceholder('请输入评分说明').first().fill('答案覆盖了模块边界和可维护性的关键点。')
  await page.getByRole('button', { name: '提交评分' }).click()
  await expect(page.getByText('阅卷结果已更新')).toBeVisible()
})
