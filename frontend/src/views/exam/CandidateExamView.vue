<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchCandidateWorkspace, fetchMyExams, reportCandidateEvent, saveCandidateAnswers, submitCandidateAnswers } from '../../api/exam'
import type { CandidateAnswerItem, CandidateExam, CandidateExamWorkspace } from '../../types/exam'
import { labelAnswerSheetStatus, labelQuestionType } from '../../utils/labels'

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const autoSubmitting = ref(false)
const exams = ref<CandidateExam[]>([])
const workspaceVisible = ref(false)
const workspace = ref<CandidateExamWorkspace | null>(null)
const answers = ref<Record<number, string | string[]>>({})
const countdownText = ref('--:--:--')
const passwordDialogVisible = ref(false)
const pendingExamPlanId = ref<number | null>(null)
const examPassword = ref('')
const currentQuestionId = ref<number | null>(null)
const lastSavedText = ref('尚未保存')
const questionObserver = ref<IntersectionObserver | null>(null)
const violationStats = reactive({
  tabSwitch: 0,
  blur: 0,
  fullscreenExit: 0
})
let countdownTimer: number | undefined
let autoSaveTimer: number | undefined

const totalViolationCount = computed(() => violationStats.tabSwitch + violationStats.blur + violationStats.fullscreenExit)
const entryWindowText = computed(() => {
  if (!workspace.value) return '--'
  return `${formatDateTime(workspace.value.startTime)} 至 ${formatDateTime(workspace.value.entryDeadlineAt)}`
})
const autoSubmitText = computed(() => {
  if (!workspace.value) return '--'
  return workspace.value.autoSubmitEnabled === 1
    ? `当剩余时间归零或达到 ${formatDateTime(workspace.value.answerDeadlineAt)} 时自动交卷`
    : '未开启自动交卷，到时后系统会禁止继续保存答案'
})
const answeredCount = computed(() => {
  if (!workspace.value) return 0
  return workspace.value.items.filter((item) => hasAnswer(item.questionId)).length
})

function parseOptions(item: CandidateAnswerItem) {
  try {
    return item.optionsJson ? (JSON.parse(item.optionsJson) as string[]) : []
  } catch {
    return []
  }
}

function hydrateAnswers(items: CandidateAnswerItem[]) {
  const next: Record<number, string | string[]> = {}
  items.forEach((item) => {
    if (item.questionType === 'MULTIPLE_CHOICE') {
      next[item.questionId] = item.answerContent ? item.answerContent.split('|').filter(Boolean) : []
    } else {
      next[item.questionId] = item.answerContent || ''
    }
  })
  answers.value = next
}

function collectAnswers() {
  if (!workspace.value) return []
  return workspace.value.items.map((item) => ({
    questionId: item.questionId,
    answerContent:
      item.questionType === 'MULTIPLE_CHOICE'
        ? ((answers.value[item.questionId] as string[] | undefined) || []).join('|')
        : String(answers.value[item.questionId] || '')
  }))
}

function hasAnswer(questionId: number) {
  const value = answers.value[questionId]
  if (Array.isArray(value)) return value.length > 0
  return String(value || '').trim().length > 0
}

async function loadExams() {
  loading.value = true
  try {
    exams.value = await fetchMyExams()
  } finally {
    loading.value = false
  }
}

async function openWorkspace(examPlanId: number) {
  pendingExamPlanId.value = examPlanId
  examPassword.value = ''
  passwordDialogVisible.value = true
}

async function confirmWorkspace() {
  if (!pendingExamPlanId.value) return
  workspace.value = await fetchCandidateWorkspace(pendingExamPlanId.value, examPassword.value || undefined)
  hydrateAnswers(workspace.value.items)
  workspaceVisible.value = true
  passwordDialogVisible.value = false
  currentQuestionId.value = workspace.value.items[0]?.questionId || null
  resetViolationStats()
  lastSavedText.value = workspace.value.saveVersion > 0 ? `已保存 ${workspace.value.saveVersion} 次` : '进入考试后将自动保存答案'
  await nextTick()
  bindQuestionObserver()
}

function resetViolationStats() {
  violationStats.tabSwitch = 0
  violationStats.blur = 0
  violationStats.fullscreenExit = 0
}

async function saveCurrent(showMessage = true, triggerSource: 'manual' | 'timer' | 'event' = 'manual') {
  if (!workspace.value || saving.value || submitting.value) return false
  saving.value = true
  try {
    workspace.value = await saveCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    lastSavedText.value = `${triggerSource === 'event' ? '异常行为触发' : '系统'}保存成功，当前版本 ${workspace.value.saveVersion}`
    if (showMessage) {
      ElMessage.success(triggerSource === 'manual' ? '答案已保存' : '系统已自动保存当前答案')
    }
    return true
  } catch {
    if (showMessage) {
      ElMessage.error('保存失败，请检查网络后重试')
    }
    lastSavedText.value = '最近一次保存失败，请尽快手动保存'
    return false
  } finally {
    saving.value = false
  }
}

async function submitCurrent(auto = false) {
  if (!workspace.value || submitting.value) return
  if (!auto) {
    try {
      await ElMessageBox.confirm('确认提交试卷？提交后将无法继续作答。', '提交确认', { type: 'warning' })
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    workspace.value = await submitCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    ElMessage.success(auto ? '作答时间已到，系统已自动交卷' : '试卷已提交')
    workspaceVisible.value = false
    await loadExams()
  } finally {
    submitting.value = false
    autoSubmitting.value = false
  }
}

async function autoSaveCurrent(triggerSource: 'timer' | 'event' = 'timer') {
  if (!workspaceVisible.value || !workspace.value) return false
  return saveCurrent(false, triggerSource)
}

async function reportSuspiciousEvent(eventType: string, severity: string, detailText: string, violationCount: number) {
  if (!workspace.value) return
  const autoSaved = await autoSaveCurrent('event')
  try {
    await reportCandidateEvent(workspace.value.examPlanId, {
      answerSheetId: workspace.value.answerSheetId,
      eventType,
      severity,
      leaveCount: violationCount,
      triggeredAutoSave: autoSaved ? 1 : 0,
      saveVersion: workspace.value.saveVersion,
      detailText
    })
  } catch {
    // 不阻塞考试主流程
  }
}

function updateCountdown() {
  if (!workspace.value) {
    countdownText.value = '--:--:--'
    return
  }
  const diff = new Date(workspace.value.answerDeadlineAt).getTime() - Date.now()
  if (diff <= 0) {
    countdownText.value = '00:00:00'
    if (!autoSubmitting.value && workspace.value.autoSubmitEnabled === 1 && workspace.value.answerSheetStatus !== 'SUBMITTED') {
      autoSubmitting.value = true
      submitCurrent(true)
    }
    return
  }
  const hours = Math.floor(diff / 3_600_000)
  const minutes = Math.floor((diff % 3_600_000) / 60_000)
  const seconds = Math.floor((diff % 60_000) / 1000)
  countdownText.value = [hours, minutes, seconds].map((item) => String(item).padStart(2, '0')).join(':')
}

function startCountdown() {
  window.clearInterval(countdownTimer)
  updateCountdown()
  countdownTimer = window.setInterval(updateCountdown, 1000)
}

function stopCountdown() {
  window.clearInterval(countdownTimer)
}

function startAutoSave() {
  window.clearInterval(autoSaveTimer)
  autoSaveTimer = window.setInterval(() => autoSaveCurrent('timer'), 20_000)
}

function stopAutoSave() {
  window.clearInterval(autoSaveTimer)
}

async function handleVisibilityChange() {
  if (!workspaceVisible.value || !workspace.value || !document.hidden) return
  violationStats.tabSwitch += 1
  await reportSuspiciousEvent('TAB_SWITCH', 'MEDIUM', `考生第 ${violationStats.tabSwitch} 次切换页面标签，系统已联动自动保存当前答案。`, violationStats.tabSwitch)
}

async function handleWindowBlur() {
  if (!workspaceVisible.value || !workspace.value) return
  violationStats.blur += 1
  await reportSuspiciousEvent('WINDOW_BLUR', violationStats.blur >= 3 ? 'MEDIUM' : 'LOW', `考生第 ${violationStats.blur} 次离开当前考试窗口，系统已联动自动保存当前答案。`, violationStats.blur)
}

async function handleFullscreenChange() {
  if (!workspaceVisible.value || !workspace.value || document.fullscreenElement) return
  violationStats.fullscreenExit += 1
  await reportSuspiciousEvent('FULLSCREEN_EXIT', 'HIGH', `考生第 ${violationStats.fullscreenExit} 次退出全屏考试态，建议监考端重点关注。`, violationStats.fullscreenExit)
}

async function enterFullscreen() {
  try {
    await document.documentElement.requestFullscreen?.()
  } catch {
    ElMessage.warning('当前浏览器未允许全屏，请手动切换到全屏模式')
  }
}

async function jumpToQuestion(questionId: number) {
  currentQuestionId.value = questionId
  await nextTick()
  document.getElementById(`question-${questionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function bindQuestionObserver() {
  questionObserver.value?.disconnect()
  if (!workspace.value) return
  questionObserver.value = new IntersectionObserver(
    (entries) => {
      const visibleEntry = entries
        .filter((entry) => entry.isIntersecting)
        .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0]
      if (!visibleEntry) return
      const questionId = Number(visibleEntry.target.getAttribute('data-question-id'))
      if (!Number.isNaN(questionId)) {
        currentQuestionId.value = questionId
      }
    },
    { rootMargin: '-20% 0px -55% 0px', threshold: [0.15, 0.4, 0.75] }
  )
  workspace.value.items.forEach((item) => {
    const element = document.getElementById(`question-${item.questionId}`)
    if (element) {
      questionObserver.value?.observe(element)
    }
  })
}

watch(
  workspaceVisible,
  (visible) => {
    document.body.style.overflow = visible ? 'hidden' : ''
    if (visible) {
      startCountdown()
      startAutoSave()
    } else {
      stopCountdown()
      stopAutoSave()
      questionObserver.value?.disconnect()
      workspace.value = null
    }
  },
  { immediate: false }
)

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  await loadExams()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  stopCountdown()
  stopAutoSave()
  questionObserver.value?.disconnect()
  document.body.style.overflow = ''
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <AppShellSection
    eyebrow="考生中心"
    title="我的考试：先看入场窗口，再进入沉浸式考试态"
    description="开始时间与结束时间用于控制学生允许进入考试的时间窗口。进入后页面会展示本场实际剩余作答时间，并在异常行为发生时自动保存当前答案。"
  >
    <section class="panel-card section-card">
      <el-table :data="exams" v-loading="loading">
        <el-table-column prop="examName" label="考试名称" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="200" />
        <el-table-column prop="startTime" label="开始时间" min-width="180" />
        <el-table-column prop="entryDeadlineAt" label="最晚进入时间" min-width="180" />
        <el-table-column prop="durationMinutes" label="作答时长" min-width="110" />
        <el-table-column label="答卷状态" min-width="120">
          <template #default="{ row }">{{ labelAnswerSheetStatus(row.answerSheetStatus) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain @click="openWorkspace(row.examPlanId)">进入考试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="passwordDialogVisible" title="进入考试" width="min(460px, 92vw)">
      <p class="muted">如果考试配置了口令，请输入后进入。进入后会按照“考试时长”和“窗口剩余时间”共同计算本场实际倒计时。</p>
      <el-input v-model="examPassword" type="password" show-password placeholder="请输入考试口令（如有）" />
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmWorkspace">进入考试</el-button>
      </template>
    </el-dialog>

    <section v-if="workspaceVisible && workspace" class="exam-overlay">
      <div class="exam-stage">
        <header class="exam-header">
          <div class="exam-title-block">
            <p class="eyebrow">考试工作区</p>
            <h2>{{ workspace.examName }}</h2>
            <p class="muted">试卷：{{ workspace.paperName }}{{ workspace.paperVersion ? ` · ${workspace.paperVersion}` : '' }}</p>
          </div>
          <div class="exam-metrics">
            <div class="metric-card metric-card--timer">
              <span>本场剩余作答时间</span>
              <strong>{{ countdownText }}</strong>
            </div>
            <div class="metric-card">
              <span>允许进入窗口</span>
              <strong>{{ entryWindowText }}</strong>
            </div>
            <div class="metric-card">
              <span>自动交卷规则</span>
              <strong>{{ autoSubmitText }}</strong>
            </div>
          </div>
          <div class="exam-toolbar">
            <div class="toolbar-note">
              <span>已答 {{ answeredCount }}/{{ workspace.items.length }}</span>
              <span>异常 {{ totalViolationCount }} 次</span>
              <span>{{ lastSavedText }}</span>
            </div>
            <div class="toolbar-actions">
              <el-button @click="enterFullscreen">进入全屏</el-button>
              <el-button :loading="saving" @click="saveCurrent(true, 'manual')">保存答案</el-button>
              <el-button type="primary" :loading="submitting || autoSubmitting" @click="submitCurrent()">提交试卷</el-button>
            </div>
          </div>
        </header>

        <div class="exam-content">
          <main class="question-column">
            <section class="exam-notice">
              <div class="notice-card">
                <strong>考前提示</strong>
                <p>{{ workspace.instructionText || '请专注作答，保持页面可见，避免切屏、退出全屏或长期离开当前考试窗口。' }}</p>
              </div>
              <div class="notice-card">
                <strong>监考说明</strong>
                <p>页面切换、窗口失焦、退出全屏等异常行为会实时记录，并联动自动保存当前作答内容。</p>
              </div>
            </section>

            <article
              v-for="item in workspace.items"
              :id="`question-${item.questionId}`"
              :key="item.questionId"
              :data-question-id="item.questionId"
              class="question-card"
              :class="{ active: currentQuestionId === item.questionId }"
            >
              <div class="question-head">
                <div>
                  <p class="eyebrow">第 {{ item.questionOrder }} 题 · {{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</p>
                  <h3>{{ item.stem }}</h3>
                </div>
                <el-tag type="warning">{{ item.maxScore }} 分</el-tag>
              </div>

              <template v-if="['SINGLE_CHOICE', 'TRUE_FALSE'].includes(item.questionType) && parseOptions(item).length">
                <el-radio-group v-model="answers[item.questionId]" class="choice-group">
                  <el-radio v-for="option in parseOptions(item)" :key="option" :label="option">{{ option }}</el-radio>
                </el-radio-group>
              </template>

              <template v-else-if="item.questionType === 'MULTIPLE_CHOICE' && parseOptions(item).length">
                <el-checkbox-group v-model="answers[item.questionId]" class="choice-group">
                  <el-checkbox v-for="option in parseOptions(item)" :key="option" :label="option">{{ option }}</el-checkbox>
                </el-checkbox-group>
              </template>

              <template v-else>
                <el-input v-model="answers[item.questionId]" type="textarea" :rows="6" placeholder="请在此输入答案，系统会自动保存" />
              </template>
            </article>
          </main>

          <aside class="aside-column">
            <section class="answer-card-panel">
              <div class="aside-head">
                <strong>答题卡</strong>
                <span class="muted">聚焦当前作答节奏</span>
              </div>
              <div class="answer-card-grid">
                <button
                  v-for="item in workspace.items"
                  :key="item.questionId"
                  type="button"
                  class="answer-card-cell"
                  :class="{ answered: hasAnswer(item.questionId), active: currentQuestionId === item.questionId }"
                  @click="jumpToQuestion(item.questionId)"
                >
                  {{ item.questionOrder }}
                </button>
              </div>
            </section>

            <section class="aside-panel">
              <div class="aside-head">
                <strong>考试状态</strong>
                <span class="muted">{{ labelAnswerSheetStatus(workspace.answerSheetStatus) }}</span>
              </div>
              <ul class="status-list">
                <li>允许进入：{{ entryWindowText }}</li>
                <li>作答截止：{{ formatDateTime(workspace.answerDeadlineAt) }}</li>
                <li>自动交卷：{{ workspace.autoSubmitEnabled === 1 ? '开启' : '关闭' }}</li>
                <li>异常次数：{{ totalViolationCount }}</li>
              </ul>
            </section>
          </aside>
        </div>
      </div>
    </section>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}

.exam-overlay {
  position: fixed;
  inset: 0;
  z-index: 2200;
  background:
    radial-gradient(circle at top, rgba(207, 190, 162, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(244, 241, 236, 0.98), rgba(237, 233, 224, 0.99));
  overflow: auto;
}

.exam-stage {
  min-height: 100vh;
  padding: 1.4rem;
  display: grid;
  gap: 1.1rem;
}

.exam-header,
.metric-card,
.notice-card,
.question-card,
.answer-card-panel,
.aside-panel {
  border: 1px solid color-mix(in oklch, var(--line) 70%, white);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
}

.exam-header,
.notice-card,
.question-card,
.answer-card-panel,
.aside-panel {
  padding: 1.1rem 1.2rem;
}

.exam-header {
  position: sticky;
  top: 0;
  z-index: 3;
  display: grid;
  gap: 1rem;
  box-shadow: 0 18px 40px rgba(112, 97, 71, 0.08);
}

.exam-title-block h2,
.question-head h3 {
  margin: 0.3rem 0 0;
  font-family: 'Literata', Georgia, serif;
}

.exam-title-block p,
.notice-card p,
.toolbar-note {
  margin: 0;
}

.exam-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.metric-card {
  padding: 0.9rem 1rem;
  display: grid;
  gap: 0.35rem;
}

.metric-card span {
  color: var(--muted);
  font-size: 0.92rem;
}

.metric-card strong {
  color: var(--brand-deep);
  line-height: 1.4;
}

.metric-card--timer strong {
  font-size: 1.8rem;
}

.exam-toolbar,
.toolbar-actions,
.exam-notice,
.exam-content,
.question-head,
.aside-head {
  display: flex;
  gap: 0.8rem;
}

.exam-toolbar,
.question-head,
.aside-head {
  justify-content: space-between;
  align-items: center;
}

.toolbar-actions {
  flex-wrap: wrap;
}

.toolbar-note {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  color: var(--muted);
}

.exam-content {
  align-items: flex-start;
}

.question-column {
  flex: 1;
  display: grid;
  gap: 1rem;
}

.aside-column {
  width: min(22rem, 30vw);
  display: grid;
  gap: 1rem;
  position: sticky;
  top: 12rem;
}

.exam-notice {
  flex-wrap: wrap;
}

.notice-card {
  flex: 1 1 18rem;
}

.question-card {
  display: grid;
  gap: 1rem;
  box-shadow: 0 10px 30px rgba(112, 97, 71, 0.06);
}

.question-card.active {
  border-color: color-mix(in oklch, var(--brand) 45%, white);
  box-shadow: 0 16px 36px rgba(92, 119, 144, 0.12);
}

.choice-group {
  display: grid;
  gap: 0.8rem;
}

.answer-card-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.6rem;
  margin-top: 0.9rem;
}

.answer-card-cell {
  min-height: 2.9rem;
  border-radius: 16px;
  border: 1px solid color-mix(in oklch, var(--line) 72%, white);
  background: color-mix(in oklch, white 92%, var(--panel-soft));
  cursor: pointer;
  font-weight: 700;
  transition: all 0.2s ease;
}

.answer-card-cell.answered {
  background: color-mix(in oklch, var(--brand) 18%, white);
  color: var(--brand-deep);
}

.answer-card-cell.active {
  border-color: color-mix(in oklch, var(--brand) 50%, white);
  transform: translateY(-2px);
}

.status-list {
  margin: 0.9rem 0 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.55rem;
  color: var(--muted);
}

@media (max-width: 1180px) {
  .exam-metrics,
  .exam-content {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .aside-column {
    width: 100%;
    position: static;
  }

  .answer-card-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .exam-stage {
    padding: 1rem;
  }

  .answer-card-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
