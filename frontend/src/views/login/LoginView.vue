<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { fetchRegisterOptions, registerAccount, resetPassword, sendVerificationCode, type RegisterOption } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const REMEMBERED_USERNAME_KEY = 'exam-system-remembered-username'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<'login' | 'register' | 'reset'>('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()

const loginLoading = ref(false)
const registerLoading = ref(false)
const resetLoading = ref(false)
const codeSending = ref(false)
const registerOptions = ref<RegisterOption[]>([])
const codeHint = ref('')
const rememberUsername = ref(true)
const panelShake = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  fullName: '',
  organizationId: 0,
  departmentName: '',
  email: '',
  phone: '',
  password: '',
  verificationChannel: 'EMAIL',
  verificationCode: ''
})

const resetForm = reactive({
  username: '',
  verificationChannel: 'EMAIL',
  verificationCode: '',
  newPassword: ''
})

const loginRules: FormRules<typeof loginForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules: FormRules<typeof registerForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  organizationId: [{ required: true, message: '请选择班级或部门', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const resetRules: FormRules<typeof resetForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const tabs = [
  { key: 'login', label: '登录' },
  { key: 'register', label: '学生注册' },
  { key: 'reset', label: '找回密码' }
] as const

const tabIndex = computed(() => tabs.findIndex((item) => item.key === activeTab.value))
const tabThumbStyle = computed(() => ({
  transform: `translateX(calc(${tabIndex.value} * 100%))`
}))

const registerTarget = computed(() =>
  registerForm.verificationChannel === 'EMAIL' ? registerForm.email.trim() : registerForm.phone.trim()
)
const resetTargetHint = computed(() =>
  resetForm.verificationChannel === 'EMAIL' ? '向账号绑定邮箱发送验证码' : '向账号绑定手机号发送验证码'
)
const rememberedUsername = computed(() => localStorage.getItem(REMEMBERED_USERNAME_KEY) || '')
const panelTitle = computed(() => {
  if (activeTab.value === 'register') return '创建学生账号'
  if (activeTab.value === 'reset') return '重置账号密码'
  return '进入在线考试系统'
})
const panelSubtitle = computed(() => {
  if (activeTab.value === 'register') return '完成基础信息与验证码校验'
  if (activeTab.value === 'reset') return '通过验证码重设密码后返回登录'
  return '输入账号与密码后进入系统'
})

function triggerPanelShake() {
  panelShake.value = false
  requestAnimationFrame(() => {
    panelShake.value = true
    window.setTimeout(() => {
      panelShake.value = false
    }, 420)
  })
}

function persistRememberedUsername(username: string) {
  if (rememberUsername.value) {
    localStorage.setItem(REMEMBERED_USERNAME_KEY, username)
    return
  }
  localStorage.removeItem(REMEMBERED_USERNAME_KEY)
}

function useRememberedUsername() {
  if (!rememberedUsername.value) return
  activeTab.value = 'login'
  loginForm.username = rememberedUsername.value
}

async function loadRegisterOptions() {
  registerOptions.value = await fetchRegisterOptions()
  if (!registerForm.organizationId && registerOptions.value.length > 0) {
    registerForm.organizationId = registerOptions.value[0].organizationId
  }
}

async function submitLogin() {
  if (!loginFormRef.value) return
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loginLoading.value = true
  try {
    await authStore.login(loginForm.username, loginForm.password)
    persistRememberedUsername(loginForm.username)
    ElMessage.success('正在进入系统')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } catch {
    triggerPanelShake()
    throw new Error('login-failed')
  } finally {
    loginLoading.value = false
  }
}

async function sendRegisterCode() {
  if (!registerTarget.value) {
    ElMessage.warning(registerForm.verificationChannel === 'EMAIL' ? '请先填写邮箱' : '请先填写手机号')
    return
  }
  codeSending.value = true
  try {
    const result = await sendVerificationCode({
      purpose: 'REGISTER',
      channel: registerForm.verificationChannel,
      targetValue: registerTarget.value,
      organizationId: registerForm.organizationId || undefined
    })
    codeHint.value = result.mockCode ? `验证码：${result.mockCode}` : result.deliveryTrace
    ElMessage.success('验证码已发送')
  } finally {
    codeSending.value = false
  }
}

async function submitRegister() {
  if (!registerFormRef.value) return
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!registerTarget.value) {
    ElMessage.warning(registerForm.verificationChannel === 'EMAIL' ? '请填写邮箱' : '请填写手机号')
    return
  }
  registerLoading.value = true
  try {
    await registerAccount({
      username: registerForm.username,
      fullName: registerForm.fullName,
      organizationId: registerForm.organizationId,
      departmentName: registerForm.departmentName,
      email: registerForm.email || undefined,
      phone: registerForm.phone || undefined,
      password: registerForm.password,
      verificationCode: registerForm.verificationCode,
      verificationChannel: registerForm.verificationChannel
    })
    ElMessage.success('注册成功，请直接登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
  } catch {
    triggerPanelShake()
    throw new Error('register-failed')
  } finally {
    registerLoading.value = false
  }
}

async function sendResetCode() {
  if (!resetForm.username.trim()) {
    ElMessage.warning('请先填写用户名')
    return
  }
  codeSending.value = true
  try {
    const result = await sendVerificationCode({
      purpose: 'RESET_PASSWORD',
      channel: resetForm.verificationChannel,
      targetValue: resetForm.username.trim(),
      username: resetForm.username.trim()
    })
    codeHint.value = result.mockCode ? `验证码：${result.mockCode}` : result.deliveryTrace
    ElMessage.success('验证码已发送')
  } finally {
    codeSending.value = false
  }
}

async function submitReset() {
  if (!resetFormRef.value) return
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return
  resetLoading.value = true
  try {
    await resetPassword({
      username: resetForm.username,
      verificationCode: resetForm.verificationCode,
      verificationChannel: resetForm.verificationChannel,
      newPassword: resetForm.newPassword
    })
    ElMessage.success('密码已重置，请重新登录')
    activeTab.value = 'login'
    loginForm.username = resetForm.username
    loginForm.password = resetForm.newPassword
  } catch {
    triggerPanelShake()
    throw new Error('reset-failed')
  } finally {
    resetLoading.value = false
  }
}

watch(activeTab, () => {
  codeHint.value = ''
})

onMounted(async () => {
  await loadRegisterOptions()
  if (rememberedUsername.value) {
    loginForm.username = rememberedUsername.value
  }
})
</script>

<template>
  <main class="login-shell">
    <div class="glow glow--cyan"></div>
    <div class="glow glow--gold"></div>

    <section class="login-stage panel-card">
      <aside class="visual-stage">
        <div class="visual-canvas">
          <div class="visual-surface"></div>
          <div class="visual-orbit visual-orbit--outer"></div>
          <div class="visual-orbit visual-orbit--inner"></div>
          <div class="visual-core">
            <div class="visual-core__plate"></div>
            <div class="visual-core__plate visual-core__plate--middle"></div>
            <div class="visual-core__plate visual-core__plate--top"></div>
          </div>
          <div class="visual-card visual-card--top-left">
            <span></span><span></span><span></span>
          </div>
          <div class="visual-card visual-card--bottom-left">
            <span></span><span></span>
          </div>
          <div class="visual-card visual-card--right">
            <span></span><span></span><span></span><span></span>
          </div>
          <div class="visual-beam visual-beam--a"></div>
          <div class="visual-beam visual-beam--b"></div>
          <div class="visual-grid"></div>
        </div>
      </aside>

      <section class="auth-stage" :class="{ shaking: panelShake }">
        <header class="auth-head">
          <div>
            <span class="eyebrow">账号入口</span>
            <h1 class="auth-title">{{ panelTitle }}</h1>
            <p class="auth-subtitle">{{ panelSubtitle }}</p>
          </div>
          <el-checkbox v-model="rememberUsername">记住账号</el-checkbox>
        </header>

        <div class="segmented" role="tablist" aria-label="账号入口切换">
          <div class="segmented__thumb" :style="tabThumbStyle"></div>
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="segmented__item"
            :class="{ active: activeTab === tab.key }"
            role="tab"
            :aria-selected="activeTab === tab.key"
            :tabindex="activeTab === tab.key ? 0 : -1"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <button
          v-if="rememberedUsername"
          type="button"
          class="remembered-chip"
          @click="useRememberedUsername"
        >
          最近使用账号：{{ rememberedUsername }}
        </button>

        <el-alert
          v-if="codeHint"
          class="code-alert"
          type="info"
          :closable="false"
          show-icon
          :title="codeHint"
        />

        <div class="form-stage">
          <Transition name="tab-fade" mode="out-in">
            <section v-if="activeTab === 'login'" key="login" role="tabpanel" aria-label="登录">
              <el-form
                ref="loginFormRef"
                :model="loginForm"
                :rules="loginRules"
                label-position="top"
                class="auth-form"
                @keyup.enter="submitLogin"
              >
                <div class="field-shell">
                  <el-form-item label="用户名" prop="username">
                    <el-input v-model="loginForm.username" placeholder="输入账号" clearable />
                  </el-form-item>
                </div>
                <div class="field-shell">
                  <el-form-item label="密码" prop="password">
                    <el-input v-model="loginForm.password" type="password" show-password placeholder="输入密码" />
                  </el-form-item>
                </div>
                <button
                  type="button"
                  class="submit-button"
                  :class="{ loading: loginLoading }"
                  :disabled="loginLoading"
                  @click="submitLogin"
                >
                  {{ loginLoading ? '正在进入系统' : '进入系统' }}
                </button>
              </el-form>
            </section>

            <section v-else-if="activeTab === 'register'" key="register" role="tabpanel" aria-label="学生注册">
              <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" class="auth-form">
                <div class="compact-grid">
                  <div class="field-shell">
                    <el-form-item label="用户名" prop="username"><el-input v-model="registerForm.username" clearable /></el-form-item>
                  </div>
                  <div class="field-shell">
                    <el-form-item label="姓名" prop="fullName"><el-input v-model="registerForm.fullName" clearable /></el-form-item>
                  </div>
                  <div class="field-shell">
                    <el-form-item label="班级 / 部门" prop="organizationId">
                      <el-select v-model="registerForm.organizationId" filterable>
                        <el-option
                          v-for="option in registerOptions"
                          :key="option.organizationId"
                          :label="option.organizationName"
                          :value="option.organizationId"
                        />
                      </el-select>
                    </el-form-item>
                  </div>
                  <div class="field-shell">
                    <el-form-item label="备注"><el-input v-model="registerForm.departmentName" placeholder="选填" clearable /></el-form-item>
                  </div>
                </div>

                <div class="channel-switch">
                  <button
                    type="button"
                    class="channel-switch__item"
                    :class="{ active: registerForm.verificationChannel === 'EMAIL' }"
                    @click="registerForm.verificationChannel = 'EMAIL'"
                  >
                    邮箱
                  </button>
                  <button
                    type="button"
                    class="channel-switch__item"
                    :class="{ active: registerForm.verificationChannel === 'SMS' }"
                    @click="registerForm.verificationChannel = 'SMS'"
                  >
                    短信
                  </button>
                </div>

                <div class="compact-grid">
                  <div class="field-shell">
                    <el-form-item :label="registerForm.verificationChannel === 'EMAIL' ? '邮箱' : '手机号'">
                      <el-input v-if="registerForm.verificationChannel === 'EMAIL'" v-model="registerForm.email" clearable />
                      <el-input v-else v-model="registerForm.phone" clearable />
                    </el-form-item>
                  </div>
                  <div class="field-shell">
                    <el-form-item label="登录密码" prop="password">
                      <el-input v-model="registerForm.password" type="password" show-password />
                    </el-form-item>
                  </div>
                </div>

                <div class="field-shell">
                  <el-form-item label="验证码" prop="verificationCode">
                    <div class="code-row">
                      <el-input v-model="registerForm.verificationCode" clearable />
                      <el-button :loading="codeSending" @click="sendRegisterCode">发送验证码</el-button>
                    </div>
                  </el-form-item>
                </div>

                <button type="button" class="submit-button" :class="{ loading: registerLoading }" :disabled="registerLoading" @click="submitRegister">
                  {{ registerLoading ? '正在提交' : '完成注册' }}
                </button>
              </el-form>
            </section>

            <section v-else key="reset" role="tabpanel" aria-label="找回密码">
              <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top" class="auth-form">
                <div class="field-shell">
                  <el-form-item label="用户名" prop="username"><el-input v-model="resetForm.username" clearable /></el-form-item>
                </div>

                <div class="channel-switch">
                  <button
                    type="button"
                    class="channel-switch__item"
                    :class="{ active: resetForm.verificationChannel === 'EMAIL' }"
                    @click="resetForm.verificationChannel = 'EMAIL'"
                  >
                    邮箱
                  </button>
                  <button
                    type="button"
                    class="channel-switch__item"
                    :class="{ active: resetForm.verificationChannel === 'SMS' }"
                    @click="resetForm.verificationChannel = 'SMS'"
                  >
                    短信
                  </button>
                </div>

                <div class="inline-tip">{{ resetTargetHint }}</div>

                <div class="field-shell">
                  <el-form-item label="验证码" prop="verificationCode">
                    <div class="code-row">
                      <el-input v-model="resetForm.verificationCode" clearable />
                      <el-button :loading="codeSending" @click="sendResetCode">发送验证码</el-button>
                    </div>
                  </el-form-item>
                </div>

                <div class="field-shell">
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="resetForm.newPassword" type="password" show-password />
                  </el-form-item>
                </div>

                <button type="button" class="submit-button" :class="{ loading: resetLoading }" :disabled="resetLoading" @click="submitReset">
                  {{ resetLoading ? '正在重置' : '重置密码' }}
                </button>
              </el-form>
            </section>
          </Transition>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: clamp(1rem, 2.8vw, 2rem);
  background:
    radial-gradient(circle at 14% 20%, rgba(170, 211, 222, 0.24), transparent 24%),
    radial-gradient(circle at 86% 10%, rgba(229, 205, 157, 0.24), transparent 20%),
    linear-gradient(135deg, rgba(250, 248, 242, 0.98), rgba(244, 248, 249, 0.98));
}

.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(64px);
  opacity: 0.72;
  pointer-events: none;
  animation: drift 16s ease-in-out infinite alternate;
}

.glow--cyan {
  width: 24rem;
  height: 24rem;
  left: -5rem;
  top: -4rem;
  background: radial-gradient(circle, rgba(115, 186, 204, 0.2), transparent 72%);
}

.glow--gold {
  width: 22rem;
  height: 22rem;
  right: -4rem;
  bottom: -3rem;
  background: radial-gradient(circle, rgba(220, 189, 135, 0.2), transparent 74%);
}

.login-stage {
  min-height: calc(100vh - 4rem);
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(24rem, 34rem);
  gap: 0;
  overflow: hidden;
}

.visual-stage,
.auth-stage {
  position: relative;
  min-height: 100%;
}

.visual-stage {
  background:
    radial-gradient(circle at 18% 20%, rgba(115, 183, 201, 0.18), transparent 28%),
    linear-gradient(145deg, rgba(20, 34, 43, 0.96), rgba(31, 47, 57, 0.94) 45%, rgba(235, 241, 243, 0.26) 100%);
}

.visual-canvas {
  position: absolute;
  inset: 1.75rem;
  border-radius: 30px;
  overflow: hidden;
  background:
    radial-gradient(circle at center, rgba(255, 255, 255, 0.05), transparent 62%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0));
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.visual-surface,
.visual-orbit,
.visual-core,
.visual-card,
.visual-beam,
.visual-grid {
  position: absolute;
}

.visual-surface {
  inset: 0;
  background:
    radial-gradient(circle at 50% 42%, rgba(173, 224, 235, 0.22), transparent 32%),
    radial-gradient(circle at 46% 58%, rgba(255, 236, 203, 0.14), transparent 28%);
}

.visual-orbit {
  border-radius: 999px;
}

.visual-orbit--outer {
  width: 72%;
  aspect-ratio: 1;
  left: 14%;
  top: 10%;
  border: 1px solid rgba(196, 233, 241, 0.24);
  animation: spin 24s linear infinite;
}

.visual-orbit--inner {
  width: 48%;
  aspect-ratio: 1;
  left: 26%;
  top: 22%;
  border: 1px dashed rgba(240, 216, 173, 0.34);
  animation: spin 18s linear infinite reverse;
}

.visual-core {
  width: 46%;
  aspect-ratio: 1;
  left: 27%;
  top: 27%;
  display: grid;
  place-items: center;
}

.visual-core__plate {
  position: absolute;
  inset: 0;
  border-radius: 34px;
  background:
    linear-gradient(160deg, rgba(25, 37, 46, 0.96), rgba(16, 27, 35, 0.94)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.08), transparent);
  box-shadow:
    0 32px 60px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  transform: rotate(-8deg);
}

.visual-core__plate--middle {
  inset: 13%;
  border-radius: 28px;
  background:
    linear-gradient(160deg, rgba(228, 244, 247, 0.92), rgba(168, 214, 224, 0.56)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.6), transparent);
  transform: rotate(7deg);
  box-shadow: 0 16px 34px rgba(27, 51, 61, 0.18);
}

.visual-core__plate--top {
  inset: 30%;
  border-radius: 22px;
  background:
    linear-gradient(160deg, rgba(253, 248, 241, 0.94), rgba(244, 222, 183, 0.78)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.7), transparent);
  transform: rotate(-6deg);
  box-shadow: 0 12px 28px rgba(27, 51, 61, 0.14);
}

.visual-card {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.48);
  backdrop-filter: blur(12px);
  box-shadow: 0 18px 36px rgba(16, 28, 36, 0.18);
  overflow: hidden;
}

.visual-card span {
  display: block;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(60, 94, 107, 0.16), rgba(60, 94, 107, 0.05));
}

.visual-card--top-left {
  width: 14rem;
  height: 8.6rem;
  top: 12%;
  left: 9%;
  transform: rotate(-8deg);
  padding: 1rem;
  animation: float-a 8s ease-in-out infinite;
}

.visual-card--top-left span:nth-child(1) {
  width: 86%;
  height: 0.82rem;
}

.visual-card--top-left span:nth-child(2) {
  width: 68%;
  height: 0.82rem;
  margin-top: 0.75rem;
}

.visual-card--top-left span:nth-child(3) {
  width: 54%;
  height: 3.2rem;
  margin-top: 1rem;
  border-radius: 20px;
}

.visual-card--bottom-left {
  width: 11rem;
  height: 7rem;
  left: 15%;
  bottom: 14%;
  transform: rotate(7deg);
  padding: 1rem;
  animation: float-b 7s ease-in-out infinite;
}

.visual-card--bottom-left span:nth-child(1) {
  width: 100%;
  height: 3.2rem;
  border-radius: 20px;
}

.visual-card--bottom-left span:nth-child(2) {
  width: 62%;
  height: 0.78rem;
  margin-top: 0.9rem;
}

.visual-card--right {
  width: 13rem;
  height: 8rem;
  right: 12%;
  top: 18%;
  transform: rotate(10deg);
  padding: 1rem;
  animation: float-c 9s ease-in-out infinite;
}

.visual-card--right span:nth-child(1),
.visual-card--right span:nth-child(2) {
  width: 100%;
  height: 0.78rem;
  margin-bottom: 0.7rem;
}

.visual-card--right span:nth-child(3) {
  width: 72%;
  height: 2.6rem;
  border-radius: 18px;
  margin-top: 0.2rem;
}

.visual-card--right span:nth-child(4) {
  width: 46%;
  height: 0.78rem;
  margin-top: 0.75rem;
}

.visual-beam {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(218, 239, 244, 0.3), transparent);
}

.visual-beam--a {
  width: 56%;
  left: 22%;
  top: 36%;
}

.visual-beam--b {
  width: 42%;
  left: 30%;
  bottom: 30%;
}

.visual-grid {
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 2.8rem 2.8rem;
  mask-image: radial-gradient(circle at center, black 36%, transparent 84%);
}

.auth-stage {
  padding: clamp(1.3rem, 2.4vw, 2.1rem);
  display: grid;
  gap: 1rem;
  align-content: start;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(252, 252, 250, 0.92)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.54), rgba(235, 243, 246, 0.2));
  border-left: 1px solid color-mix(in oklch, var(--line) 74%, white);
}

.auth-stage.shaking {
  animation: shake 0.38s ease;
}

.auth-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.auth-title {
  margin: 0.42rem 0 0.4rem;
  font-family: 'Literata', Georgia, serif;
  font-size: clamp(2rem, 4vw, 2.9rem);
  line-height: 1.04;
}

.auth-subtitle {
  margin: 0;
  color: var(--muted);
}

.segmented {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.3rem;
  padding: 0.3rem;
  border-radius: 20px;
  background: color-mix(in oklch, white 88%, var(--panel-soft));
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
}

.segmented__thumb {
  position: absolute;
  top: 0.3rem;
  left: 0.3rem;
  width: calc((100% - 0.6rem) / 3);
  height: calc(100% - 0.6rem);
  border-radius: 16px;
  background: linear-gradient(135deg, color-mix(in oklch, var(--brand) 16%, white), rgba(255, 255, 255, 0.98));
  box-shadow: 0 12px 24px rgba(46, 86, 101, 0.11);
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.segmented__item,
.channel-switch__item,
.remembered-chip,
.submit-button {
  appearance: none;
  border: 0;
  font: inherit;
  cursor: pointer;
}

.segmented__item {
  position: relative;
  z-index: 1;
  min-height: 3rem;
  border-radius: 16px;
  background: transparent;
  color: var(--muted);
  font-weight: 800;
  transition: color 0.24s ease;
}

.segmented__item.active {
  color: var(--brand-deep);
}

.remembered-chip {
  align-self: flex-start;
  padding: 0.72rem 0.95rem;
  border-radius: 16px;
  background: color-mix(in oklch, var(--accent) 10%, white);
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
  color: color-mix(in oklch, var(--brand-deep) 90%, var(--text));
  font-weight: 700;
}

.code-alert {
  border-radius: 18px;
}

.form-stage {
  min-height: 31rem;
  display: grid;
}

.auth-form {
  display: grid;
  gap: 0.85rem;
}

.compact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.field-shell {
  border-radius: 20px;
  padding: 0.1rem 0.2rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.62), rgba(248, 250, 251, 0.74));
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.field-shell:focus-within {
  transform: translateY(-1px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 250, 0.94));
  box-shadow: 0 12px 28px rgba(51, 94, 111, 0.09);
}

.channel-switch {
  display: inline-flex;
  gap: 0.55rem;
  padding: 0.28rem;
  border-radius: 18px;
  background: color-mix(in oklch, white 88%, var(--panel-soft));
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
}

.channel-switch__item {
  min-height: 2.55rem;
  padding: 0 1rem;
  border-radius: 14px;
  background: transparent;
  color: var(--muted);
  font-weight: 800;
  transition: all 0.24s ease;
}

.channel-switch__item.active {
  background: color-mix(in oklch, var(--brand) 15%, white);
  color: var(--brand-deep);
}

.inline-tip {
  color: var(--muted);
  font-size: 0.92rem;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
}

.submit-button {
  width: 100%;
  min-height: 3.25rem;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--brand), color-mix(in oklch, var(--brand-deep) 84%, var(--brand)));
  color: white;
  font-weight: 800;
  letter-spacing: 0.02em;
  box-shadow: 0 16px 32px rgba(47, 103, 122, 0.22);
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.submit-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 36px rgba(47, 103, 122, 0.26);
}

.submit-button:active {
  transform: translateY(1px) scale(0.995);
}

.submit-button.loading {
  filter: saturate(0.92);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.el-form-item__label) {
  padding-bottom: 0.45rem;
  font-size: 0.86rem;
  font-weight: 700;
  color: color-mix(in oklch, var(--text) 78%, var(--brand-deep));
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner),
:deep(.el-checkbox__label) {
  box-shadow: none !important;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 3.2rem;
  border-radius: 16px;
  border: 1px solid color-mix(in oklch, var(--line) 80%, white);
  background: rgba(255, 255, 255, 0.88);
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

:deep(.el-textarea__inner) {
  min-height: 7.5rem;
  border-radius: 16px;
  border: 1px solid color-mix(in oklch, var(--line) 80%, white);
  background: rgba(255, 255, 255, 0.88);
  padding-top: 0.95rem;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
  border-color: color-mix(in oklch, var(--brand) 58%, white) !important;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 0 0 4px rgba(85, 170, 191, 0.12) !important;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  font-size: 1rem;
  color: var(--text);
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: color-mix(in oklch, var(--muted) 86%, white);
}

:deep(.el-form-item__error) {
  padding-top: 0.4rem;
  font-size: 0.8rem;
}

.tab-fade-enter-active,
.tab-fade-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.tab-fade-enter-from,
.tab-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes fade-rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes drift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(1.2rem, 0.8rem, 0) scale(1.05);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes halo-breathe {
  0%, 100% {
    transform: translate(-50%, -50%) scale(0.82);
    opacity: 0.82;
  }
  50% {
    transform: translate(-50%, -50%) scale(0.9);
    opacity: 1;
  }
}

@keyframes shake {
  10%, 90% { transform: translateX(-2px); }
  20%, 80% { transform: translateX(4px); }
  30%, 50%, 70% { transform: translateX(-6px); }
  40%, 60% { transform: translateX(6px); }
}

@keyframes float-a {
  0%, 100% { transform: rotate(-8deg) translateY(0); }
  50% { transform: rotate(-10deg) translateY(-10px); }
}

@keyframes float-b {
  0%, 100% { transform: rotate(7deg) translateY(0); }
  50% { transform: rotate(9deg) translateY(8px); }
}

@keyframes float-c {
  0%, 100% { transform: rotate(-5deg) translateY(0); }
  50% { transform: rotate(-7deg) translateY(-8px); }
}

@media (max-width: 1100px) {
  .login-stage {
    grid-template-columns: 1fr;
  }

  .visual-stage {
    min-height: 24rem;
  }
}

@media (max-width: 780px) {
  .login-shell {
    padding: 0.9rem;
  }

  .compact-grid {
    grid-template-columns: 1fr;
  }

  .auth-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
