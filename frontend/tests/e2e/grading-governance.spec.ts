import { expect, test } from '@playwright/test'

import { buildActiveExamWindow } from './helpers'

test('阅卷复核、学生申诉与管理端处理可形成基础治理闭环', async ({ page, request }) => {
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
      examCode: `GOV-${suffix}`,
      examName: `治理验证-${suffix}`,
      paperId: paper.id,
      startTime,
      endTime,
      durationMinutes: 90,
      passScore: Math.min(60, paper.totalScore),
      candidateScope: 'ASSIGNED',
      attemptLimit: 1,
      examPassword: 'GOV2026',
      lateEntryMinutes: 240,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'STRICT',
      instructionText: '用于验证阅卷治理基础版。',
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
  const workspaceResponse = await request.get(`http://127.0.0.1:8083/api/exam/candidate/exams/${examPlanId}?examPassword=GOV2026`, {
    headers: { Authorization: `Bearer ${studentToken}` }
  })
  const workspacePayload = await workspaceResponse.json()
  const answers = workspacePayload.data.items.slice(0, 4).map((item: any) => {
    if (item.questionType === 'MULTIPLE_CHOICE') {
      return { questionId: item.questionId, answerContent: '说法一：符合教材结论|说法三：符合题意要求' }
    }
    if (item.questionType === 'SINGLE_CHOICE') {
      return { questionId: item.questionId, answerContent: '选项一：符合教材结论' }
    }
    if (item.questionType === 'TRUE_FALSE') {
      return { questionId: item.questionId, answerContent: '正确' }
    }
    return { questionId: item.questionId, answerContent: '这是用于申诉验证的主观题答案。' }
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
  const gradingRow = page.getByRole('row', { name: new RegExp(`治理验证-${suffix}`) }).first()
  await expect(gradingRow).toBeVisible()
  await gradingRow.getByRole('button', { name: '打开' }).click()
  const spinbuttons = await page.getByRole('spinbutton').all()
  for (const input of spinbuttons) {
    await input.fill('8')
  }
  const commentBoxes = await page.getByPlaceholder('请输入评分说明').all()
  for (const item of commentBoxes) {
    await item.fill('阅卷治理 UI 验证评分。')
  }
  await page.getByRole('button', { name: '提交评分' }).click()
  await expect(page.getByText('阅卷结果已更新')).toBeVisible()
  await page.getByRole('button', { name: '复核通过并发布' }).click()
  await expect(page.getByText('复核已通过，成绩已发布')).toBeVisible()

  await page.evaluate(() => localStorage.clear())
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('20260001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/candidate/scores')
  const scoreRow = page.getByRole('row', { name: new RegExp(`治理验证-${suffix}`) }).first()
  await expect(scoreRow).toBeVisible()
  await scoreRow.getByRole('button', { name: '查看详情' }).click()
  await expect(page.getByText('成绩申诉')).toBeVisible()
  await page.getByPlaceholder('请说明你认为分数需要复核的原因').fill('我认为主观题评分需要进一步复核。')
  await page.getByPlaceholder(/可选：希望复核后如何处理/).fill('希望重新核对主观题评分。')
  await page.getByRole('button', { name: '提交申诉' }).click()
  await expect(page.getByText('成绩申诉已提交')).toBeVisible()
  await expect(page.getByRole('heading', { name: '申诉待处理' })).toBeVisible()

  await page.evaluate(() => localStorage.clear())
  await page.goto('/login')
  await page.locator('input.el-input__inner').nth(0).fill('900001')
  await page.locator('input.el-input__inner').nth(1).fill('123456')
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')

  await page.goto('/exam/records')
  const targetRow = page.getByRole('row', { name: new RegExp(`治理验证-${suffix}`) }).first()
  await expect(targetRow).toBeVisible()
  await targetRow.getByRole('button', { name: '处理申诉' }).click()
  await expect(page.getByText('申诉记录', { exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '申诉待处理' })).toBeVisible()
})
