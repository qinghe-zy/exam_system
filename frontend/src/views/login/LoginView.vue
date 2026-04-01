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
  username: 'admin',
  password: 'admin123'
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: 'Please enter the username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter the password', trigger: 'blur' }]
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form.username, form.password)
      ElMessage.success('Signed in to the exam-system workspace')
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
      <span class="eyebrow">Academic Template Factory</span>
      <h1 class="display-title">A reusable exam-system starter for question, paper, and result workflows.</h1>
      <p class="muted intro-copy">
        This derived starter keeps the shared authentication and system baseline, then adds the first exam-focused business flows that often recur in graduation-project systems.
      </p>
      <ul class="signal-list">
        <li>Question bank</li>
        <li>Paper management</li>
        <li>Exam result tracking</li>
      </ul>
    </section>

    <section class="panel-card login-card">
      <div>
        <span class="eyebrow">Current Access</span>
        <h2>Sign in to the exam-system example</h2>
        <p class="muted">Default starter account: <strong>admin / admin123</strong></p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="admin123" />
        </el-form-item>
        <el-button class="submit-button" type="primary" :loading="loading" @click="submit">
          Enter workspace
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
