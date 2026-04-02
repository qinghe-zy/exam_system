<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchCandidateWorkspace, fetchMyExams, reportCandidateEvent, saveCandidateAnswers, submitCandidateAnswers } from '../../api/exam'
import type { CandidateAnswerItem, CandidateExam, CandidateExamWorkspace } from '../../types/exam'
import { labelAnswerSheetStatus, labelQuestionType } from '../../utils/labels'

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const exams = ref<CandidateExam[]>([])
const workspaceVisible = ref(false)
const workspace = ref<CandidateExamWorkspace | null>(null)
const answers = ref<Record<number, string | string[]>>({})
const countdownText = ref('--:--:--')
const passwordDialogVisible = ref(false)
const pendingExamPlanId = ref<number | null>(null)
const examPassword = ref('')
let countdownTimer: number | undefined
let autoSaveTimer: number | undefined

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
}

async function saveCurrent() {
  if (!workspace.value) return
  saving.value = true
  try {
    workspace.value = await saveCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    ElMessage.success('答案已保存')
  } finally {
    saving.value = false
  }
}

async function submitCurrent() {
  if (!workspace.value) return
  submitting.value = true
  try {
    workspace.value = await submitCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    ElMessage.success('试卷已提交')
    await loadExams()
  } finally {
    submitting.value = false
  }
}

async function autoSaveCurrent() {
  if (!workspaceVisible.value || !workspace.value || saving.value || submitting.value) return
  try {
    workspace.value = await saveCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
  } catch {
    // Keep the exam flow going and let the user save manually if the network fluctuates.
  }
}

async function reportEvent(eventType: string, severity: string, detailText: string) {
  if (!workspace.value) return
  try {
    await reportCandidateEvent(workspace.value.examPlanId, {
      answerSheetId: workspace.value.answerSheetId,
      eventType,
      severity,
      detailText
    })
  } catch {
    // Avoid blocking the exam flow on telemetry failures.
  }
}

function updateCountdown() {
  if (!workspace.value) {
    countdownText.value = '--:--:--'
    return
  }
  const diff = new Date(workspace.value.endTime).getTime() - Date.now()
  if (diff <= 0) {
    countdownText.value = '00:00:00'
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
  autoSaveTimer = window.setInterval(autoSaveCurrent, 20000)
}

function stopAutoSave() {
  window.clearInterval(autoSaveTimer)
}

async function handleVisibilityChange() {
  if (!workspaceVisible.value || !workspace.value) return
  if (document.hidden) {
    await reportEvent('TAB_SWITCH', 'MEDIUM', '考生在考试过程中切换了页面标签')
  }
}

async function handleWindowBlur() {
  if (!workspaceVisible.value || !workspace.value) return
  await reportEvent('WINDOW_BLUR', 'LOW', '考生当前窗口失去焦点')
}

async function handleFullscreenChange() {
  if (!workspaceVisible.value || !workspace.value) return
  if (!document.fullscreenElement) {
    await reportEvent('FULLSCREEN_EXIT', 'HIGH', '考生在考试过程中退出了全屏模式')
  }
}

async function enterFullscreen() {
  await document.documentElement.requestFullscreen?.()
}

async function jumpToQuestion(questionId: number) {
  await nextTick()
  document.getElementById(`question-${questionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(
  workspaceVisible,
  (visible) => {
    if (visible) {
      startCountdown()
      startAutoSave()
    } else {
      stopCountdown()
      stopAutoSave()
    }
  },
  { immediate: false }
)

onMounted(async () => {
  await loadExams()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  stopCountdown()
  stopAutoSave()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <AppShellSection
    eyebrow="考生中心"
    title="待考列表、作答、保存与交卷"
    description="考生可以查看已分配考试，输入考试口令进入工作区，进行作答、自动保存、手动保存和交卷；切屏、失焦、退出全屏等行为会作为基础监考事件记录。"
  >
    <section class="panel-card section-card">
      <el-table :data="exams" v-loading="loading">
        <el-table-column prop="examName" label="考试名称" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="220" />
        <el-table-column prop="startTime" label="开始时间" min-width="180" />
        <el-table-column prop="endTime" label="结束时间" min-width="180" />
        <el-table-column label="答卷状态" min-width="140"><template #default="{ row }">{{ labelAnswerSheetStatus(row.answerSheetStatus) }}</template></el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain @click="openWorkspace(row.examPlanId)">进入考试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="passwordDialogVisible" title="进入考试" width="min(460px, 92vw)">
      <p class="muted">如该考试配置了考试口令，请输入后进入；未配置口令时可直接进入。</p>
      <el-input v-model="examPassword" type="password" show-password placeholder="请输入考试口令（如有）" />
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmWorkspace">进入考试</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="workspaceVisible" size="70%" :with-header="false">
      <template v-if="workspace">
        <section class="exam-shell">
          <header class="panel-card workspace-hero">
            <div>
              <p class="eyebrow">考试工作区</p>
              <h2>{{ workspace.examName }}</h2>
              <p class="muted">{{ workspace.instructionText }}</p>
            </div>
            <div class="workspace-meta">
              <span class="countdown">{{ countdownText }}</span>
              <el-button @click="enterFullscreen">进入全屏</el-button>
              <el-button :loading="saving" @click="saveCurrent">保存</el-button>
              <el-button type="primary" :loading="submitting" @click="submitCurrent">提交试卷</el-button>
            </div>
          </header>

          <section class="workspace-grid">
            <aside class="panel-card answer-card-nav">
              <div class="answer-card-header">
                <strong>答题卡</strong>
                <span class="muted">每 20 秒自动保存一次</span>
              </div>
              <div class="answer-card-grid">
                <button
                  v-for="item in workspace.items"
                  :key="item.questionId"
                  type="button"
                  class="answer-card-cell"
                  :class="{ filled: !!answers[item.questionId] && String(answers[item.questionId]).length > 0 }"
                  @click="jumpToQuestion(item.questionId)"
                >
                  {{ item.questionOrder }}
                </button>
              </div>
            </aside>

            <article v-for="item in workspace.items" :id="`question-${item.questionId}`" :key="item.questionId" class="panel-card question-card">
              <div class="question-header">
                <span class="eyebrow">{{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</span>
                <strong>{{ item.maxScore }} 分</strong>
              </div>
              <p class="question-stem">{{ item.stem }}</p>

              <template v-if="['SINGLE_CHOICE', 'TRUE_FALSE'].includes(item.questionType) && parseOptions(item).length">
                <el-radio-group v-model="answers[item.questionId]">
                  <el-radio v-for="option in parseOptions(item)" :key="option" :label="option">{{ option }}</el-radio>
                </el-radio-group>
              </template>

              <template v-else-if="item.questionType === 'MULTIPLE_CHOICE' && parseOptions(item).length">
                <el-checkbox-group v-model="answers[item.questionId]">
                  <el-checkbox v-for="option in parseOptions(item)" :key="option" :label="option">{{ option }}</el-checkbox>
                </el-checkbox-group>
              </template>

              <template v-else>
                <el-input v-model="answers[item.questionId]" type="textarea" :rows="4" placeholder="请在此输入答案" />
              </template>
            </article>
          </section>
        </section>
      </template>
    </el-drawer>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}

.exam-shell {
  display: grid;
  gap: 1rem;
}

.workspace-hero {
  padding: 1.2rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.workspace-hero h2 {
  margin: 0.35rem 0;
  font-family: 'Literata', Georgia, serif;
}

.workspace-meta {
  display: grid;
  gap: 0.8rem;
  justify-items: end;
}

.countdown {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--brand-deep);
}

.workspace-grid {
  display: grid;
  gap: 1rem;
}

.answer-card-nav {
  padding: 1rem;
  position: sticky;
  top: 0;
  z-index: 2;
}

.answer-card-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.9rem;
}

.answer-card-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 0.6rem;
}

.answer-card-cell {
  min-height: 2.3rem;
  border-radius: 12px;
  border: 1px solid color-mix(in oklch, var(--line) 85%, white);
  background: color-mix(in oklch, white 88%, var(--panel-soft));
  cursor: pointer;
  font-weight: 700;
}

.answer-card-cell.filled {
  background: color-mix(in oklch, var(--brand) 18%, white);
  color: var(--brand-deep);
}

.question-card {
  padding: 1rem;
  display: grid;
  gap: 0.8rem;
}

.question-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.question-stem {
  margin: 0;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .workspace-hero {
    flex-direction: column;
  }

  .workspace-meta {
    width: 100%;
    justify-items: stretch;
  }

  .answer-card-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
