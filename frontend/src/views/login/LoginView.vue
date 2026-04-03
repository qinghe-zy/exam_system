<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { fetchRegisterOptions, registerAccount, resetPassword, sendVerificationCode, type RegisterOption } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

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
const mockHint = ref('')

const loginForm = reactive({
  username: '900001',
  password: '123456'
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
  organizationId: [{ required: true, message: '请选择班级/部门', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const resetRules: FormRules<typeof resetForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  verificationCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const registerTarget = computed(() =>
  registerForm.verificationChannel === 'EMAIL' ? registerForm.email.trim() : registerForm.phone.trim()
)
const resetTargetHint = computed(() =>
  resetForm.verificationChannel === 'EMAIL' ? '向账号绑定邮箱发送验证码' : '向账号绑定手机号发送验证码'
)

async function loadRegisterOptions() {
  registerOptions.value = await fetchRegisterOptions()
  if (!registerForm.organizationId && registerOptions.value.length > 0) {
    registerForm.organizationId = registerOptions.value[0].organizationId
  }
}

async function submitLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loginLoading.value = true
    try {
      await authStore.login(loginForm.username, loginForm.password)
      ElMessage.success('登录成功，正在进入系统')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
      await router.push(redirect)
    } finally {
      loginLoading.value = false
    }
  })
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
    mockHint.value = result.mockCode ? `Mock 验证码：${result.mockCode}` : result.deliveryTrace
    ElMessage.success('验证码已发送，请查收提示信息')
  } finally {
    codeSending.value = false
  }
}

async function submitRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
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
      ElMessage.success('注册成功，请使用新账号登录')
      activeTab.value = 'login'
      loginForm.username = registerForm.username
      loginForm.password = registerForm.password
    } finally {
      registerLoading.value = false
    }
  })
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
    mockHint.value = result.mockCode ? `Mock 验证码：${result.mockCode}` : result.deliveryTrace
    ElMessage.success('验证码已发送，请查收提示信息')
  } finally {
    codeSending.value = false
  }
}

async function submitReset() {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
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
    } finally {
      resetLoading.value = false
    }
  })
}

onMounted(loadRegisterOptions)
</script>

<template>
  <main class="login-shell">
    <section class="intro-strip">
      <span class="eyebrow">在线考试平台</span>
      <h1 class="display-title">面向正式考试项目持续补全的在线考试系统</h1>
      <p class="muted intro-copy">
        当前入口除了登录，还提供学生自注册和找回密码基础版。验证码先通过可运行 mock 通道下发，用于联调和验收环境验证完整账号流程。
      </p>
      <ul class="signal-list">
        <li>注册与找回密码</li>
        <li>题库与组卷</li>
        <li>考试与成绩闭环</li>
      </ul>
    </section>

    <section class="panel-card login-card">
      <div>
        <span class="eyebrow">账号入口</span>
        <h2>进入在线考试系统</h2>
        <p class="muted">当前测试账号统一密码：<strong>123456</strong></p>
      </div>

      <el-alert
        v-if="mockHint"
        type="info"
        :closable="false"
        show-icon
        :title="mockHint"
      />

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @submit.prevent="submitLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button class="submit-button" type="primary" :loading="loginLoading" @click="submitLogin">
              进入系统
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="学生注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top">
            <div class="grid-two">
              <el-form-item label="用户名" prop="username"><el-input v-model="registerForm.username" /></el-form-item>
              <el-form-item label="姓名" prop="fullName"><el-input v-model="registerForm.fullName" /></el-form-item>
              <el-form-item label="班级/部门" prop="organizationId">
                <el-select v-model="registerForm.organizationId" filterable>
                  <el-option
                    v-for="option in registerOptions"
                    :key="option.organizationId"
                    :label="option.organizationName"
                    :value="option.organizationId"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="班级备注"><el-input v-model="registerForm.departmentName" placeholder="可留空，默认沿用组织名称" /></el-form-item>
              <el-form-item label="验证码通道">
                <el-radio-group v-model="registerForm.verificationChannel">
                  <el-radio-button label="EMAIL">邮箱</el-radio-button>
                  <el-radio-button label="SMS">短信</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item :label="registerForm.verificationChannel === 'EMAIL' ? '邮箱' : '手机号'">
                <el-input v-if="registerForm.verificationChannel === 'EMAIL'" v-model="registerForm.email" placeholder="请输入邮箱" />
                <el-input v-else v-model="registerForm.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="登录密码" prop="password"><el-input v-model="registerForm.password" type="password" show-password /></el-form-item>
              <el-form-item label="验证码" prop="verificationCode">
                <div class="code-row">
                  <el-input v-model="registerForm.verificationCode" />
                  <el-button :loading="codeSending" @click="sendRegisterCode">发送验证码</el-button>
                </div>
              </el-form-item>
            </div>
            <el-button class="submit-button" type="primary" :loading="registerLoading" @click="submitRegister">
              完成注册
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="找回密码" name="reset">
          <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top">
            <el-form-item label="用户名" prop="username"><el-input v-model="resetForm.username" placeholder="请输入已有账号用户名" /></el-form-item>
            <el-form-item label="验证码通道">
              <el-radio-group v-model="resetForm.verificationChannel">
                <el-radio-button label="EMAIL">邮箱</el-radio-button>
                <el-radio-button label="SMS">短信</el-radio-button>
              </el-radio-group>
              <p class="muted helper-text">{{ resetTargetHint }}</p>
            </el-form-item>
            <el-form-item label="验证码" prop="verificationCode">
              <div class="code-row">
                <el-input v-model="resetForm.verificationCode" />
                <el-button :loading="codeSending" @click="sendResetCode">发送验证码</el-button>
              </div>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="resetForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-button class="submit-button" type="primary" :loading="resetLoading" @click="submitReset">
              重置密码
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </main>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 480px);
  gap: clamp(1rem, 3vw, 2rem);
  padding: clamp(1rem, 4vw, 2.5rem);
  align-items: stretch;
}

.intro-strip,
.login-card {
  padding: clamp(1.5rem, 3vw, 2.5rem);
}

.intro-strip {
  display: grid;
  align-content: end;
  background:
    linear-gradient(135deg, color-mix(in oklch, var(--panel-soft) 78%, white), rgba(255, 255, 255, 0.52)),
    linear-gradient(180deg, color-mix(in oklch, var(--brand) 5%, transparent), transparent);
  border: 1px solid color-mix(in oklch, var(--line) 85%, white);
  border-radius: 30px;
}

.intro-copy {
  max-width: 52rem;
  line-height: 1.75;
}

.signal-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  list-style: none;
  padding: 0;
  margin: 1.25rem 0 0;
}

.signal-list li {
  padding: 0.75rem 0.95rem;
  border-radius: 999px;
  background: color-mix(in oklch, var(--accent) 16%, white);
  color: color-mix(in oklch, var(--text) 82%, var(--brand-deep));
  font-weight: 700;
}

.login-card {
  display: grid;
  align-content: center;
  gap: 1.2rem;
}

.login-card h2 {
  margin: 0.35rem 0 0.4rem;
  font-family: 'Literata', Georgia, serif;
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
}

.helper-text {
  margin: 0.45rem 0 0;
}

.submit-button {
  width: 100%;
  min-height: 2.95rem;
  font-weight: 700;
}

@media (max-width: 980px) {
  .login-shell,
  .grid-two {
    grid-template-columns: 1fr;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
