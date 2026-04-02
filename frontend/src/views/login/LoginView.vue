<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '900001',
  password: '123456'
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form.username, form.password)
      ElMessage.success('登录成功，正在进入系统')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
      await router.push(redirect)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <main class="login-shell">
    <section class="intro-strip">
      <span class="eyebrow">在线考试平台</span>
      <h1 class="display-title">面向联调、演示与验收的在线考试系统</h1>
      <p class="muted intro-copy">
        当前系统已支持题库、组卷、考试发布、考生答题、阅卷评分、成绩分析与基础监考留痕，可直接用于系统联调、项目演示和验收回归。
      </p>
      <ul class="signal-list">
        <li>题库管理</li>
        <li>试卷组装</li>
        <li>成绩跟踪</li>
      </ul>
    </section>

    <section class="panel-card login-card">
      <div>
        <span class="eyebrow">登录入口</span>
        <h2>登录在线考试系统</h2>
        <p class="muted">当前测试账号统一密码：<strong>123456</strong></p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button class="submit-button" type="primary" :loading="loading" @click="submit">
          进入系统
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 420px);
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

.submit-button {
  width: 100%;
  min-height: 2.95rem;
  font-weight: 700;
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
}
</style>
