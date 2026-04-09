import { expect, test } from '@playwright/test'

test('学生可通过注册和找回密码基础版完成账号流程', async ({ page }) => {
  const suffix = Date.now().toString().slice(-6)
  const username = `reg${suffix}`
  const initialPassword = 'Reg123456!'
  const resetPasswordValue = 'Reset123456!'

  await page.goto('/login')
  await page.getByRole('tab', { name: '学生注册' }).click()
  const registerPane = page.getByRole('tabpanel', { name: '学生注册' })
  await registerPane.getByLabel('用户名').fill(username)
  await registerPane.getByLabel('姓名').fill(`注册学生${suffix}`)
  await registerPane.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item').first().click()
  await page.locator('body').click({ position: { x: 20, y: 20 } })
  await registerPane.getByLabel('邮箱').fill(`${username}@example.local`)
  await registerPane.getByLabel('登录密码').fill(initialPassword)
  await registerPane.getByRole('button', { name: '发送验证码' }).click()
  const registerHint = page.getByText(/验证码：\d{6}/)
  await expect(registerHint).toBeVisible()
  const registerCode = (await registerHint.textContent())?.match(/\d{6}/)?.[0]
  expect(registerCode).toBeTruthy()
  await registerPane.getByLabel('验证码').fill(registerCode!)
  await registerPane.getByRole('button', { name: '完成注册' }).click()
  await expect(page.getByText('注册成功，请直接登录')).toBeVisible()

  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')
  await page.locator('.account-pill').click()
  await page.getByText('退出登录').click()
  await page.waitForURL('**/login')

  await page.getByRole('tab', { name: '找回密码' }).click()
  const resetPane = page.getByRole('tabpanel', { name: '找回密码' })
  await resetPane.getByLabel('用户名').fill(username)
  await resetPane.getByRole('button', { name: '发送验证码' }).click()
  const resetHint = page.getByText(/验证码：\d{6}/)
  await expect(resetHint).toBeVisible()
  const resetCode = (await resetHint.textContent())?.match(/\d{6}/)?.[0]
  expect(resetCode).toBeTruthy()
  await resetPane.getByLabel('验证码').fill(resetCode!)
  await resetPane.getByLabel('新密码').fill(resetPasswordValue)
  await resetPane.getByRole('button', { name: '重置密码' }).click()
  await expect(page.getByText('密码已重置，请重新登录')).toBeVisible()

  await page.locator('input.el-input__inner').nth(0).fill(username)
  await page.locator('input.el-input__inner').nth(1).fill(resetPasswordValue)
  await page.getByRole('button', { name: '进入系统' }).click()
  await page.waitForURL('**/dashboard')
})
