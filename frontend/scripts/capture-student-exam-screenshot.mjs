import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { chromium, request } from 'playwright'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..', '..')
const shotDir = path.join(repoRoot, 'docs', 'assets', 'screenshots')
const baseUrl = 'http://127.0.0.1:5173'
const apiUrl = 'http://127.0.0.1:8083'

async function login(page, username, password) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
  await page.locator('input.el-input__inner').nth(0).fill(username)
  await page.locator('input.el-input__inner').nth(1).fill(password)
  await page.locator('.submit-button').click()
  await page.waitForURL('**/dashboard')
  await page.waitForLoadState('networkidle')
}

async function createScreenshotExam(apiContext, suffix) {
  const teacherLogin = await apiContext.post(`${apiUrl}/api/auth/login`, {
    data: { username: '800001', password: '123456' }
  })
  const teacherPayload = await teacherLogin.json()
  const teacherToken = teacherPayload.data.token
  const papersResp = await apiContext.get(`${apiUrl}/api/exam/papers`, {
    headers: { Authorization: `Bearer ${teacherToken}` }
  })
  const papersPayload = await papersResp.json()
  const paper = papersPayload.data.find((item) => item.publishStatus === 1) || papersPayload.data[0]
  const now = new Date()
  const start = new Date(now.getTime() - 10 * 60 * 1000).toISOString().slice(0, 19)
  const end = new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString().slice(0, 19)
  const examName = `学生考试截图-${suffix}`

  await apiContext.post(`${apiUrl}/api/exam/plans`, {
    headers: { Authorization: `Bearer ${teacherToken}` },
    data: {
      examCode: `SHOT-STU-${suffix}`,
      examName,
      paperId: paper.id,
      startTime: start,
      endTime: end,
      durationMinutes: 90,
      passScore: Math.min(60, paper.totalScore),
      candidateScope: 'ASSIGNED',
      attemptLimit: 1,
      examPassword: 'SHOT2026',
      lateEntryMinutes: 180,
      earlySubmitMinutes: 0,
      autoSubmitEnabled: 1,
      antiCheatLevel: 'STRICT',
      instructionText: '用于交付截图采集。',
      status: 1,
      publishStatus: 1,
      candidateUserIds: [15]
    }
  })

  return examName
}

async function main() {
  const suffix = String(Date.now()).slice(-6)
  const apiContext = await request.newContext()
  const examName = await createScreenshotExam(apiContext, suffix)
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    deviceScaleFactor: 1.5
  })
  const page = await context.newPage()

  await login(page, '20260001', '123456')
  await page.goto(`${baseUrl}/candidate/exams`, { waitUntil: 'networkidle' })
  const examRow = page.getByRole('row', { name: new RegExp(examName) })
  await examRow.getByRole('button', { name: '进入考试' }).click()
  await page.getByPlaceholder('请输入考试口令（如有）').fill('SHOT2026')
  await page.getByRole('dialog').getByRole('button', { name: '进入考试' }).click()
  await page.waitForSelector('.exam-overlay')
  await page.screenshot({
    path: path.join(shotDir, '10-student-exam.png'),
    fullPage: true
  })

  await browser.close()
  await apiContext.dispose()
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
