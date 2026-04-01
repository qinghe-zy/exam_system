<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchCandidateWorkspace, fetchMyExams, reportCandidateEvent, saveCandidateAnswers, submitCandidateAnswers } from '../../api/exam'
import type { CandidateAnswerItem, CandidateExam, CandidateExamWorkspace } from '../../types/exam'

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const exams = ref<CandidateExam[]>([])
const workspaceVisible = ref(false)
const workspace = ref<CandidateExamWorkspace | null>(null)
const answers = ref<Record<number, string | string[]>>({})
const countdownText = ref('--:--:--')
let countdownTimer: number | undefined

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
  workspace.value = await fetchCandidateWorkspace(examPlanId)
  hydrateAnswers(workspace.value.items)
  workspaceVisible.value = true
}

async function saveCurrent() {
  if (!workspace.value) return
  saving.value = true
  try {
    workspace.value = await saveCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    ElMessage.success('Answers saved')
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
    ElMessage.success('Exam submitted')
    await loadExams()
  } finally {
    submitting.value = false
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

async function handleVisibilityChange() {
  if (!workspaceVisible.value || !workspace.value) return
  if (document.hidden) {
    await reportEvent('TAB_SWITCH', 'MEDIUM', 'Candidate switched away from the exam tab')
  }
}

async function handleWindowBlur() {
  if (!workspaceVisible.value || !workspace.value) return
  await reportEvent('WINDOW_BLUR', 'LOW', 'Candidate window lost focus')
}

async function handleFullscreenChange() {
  if (!workspaceVisible.value || !workspace.value) return
  if (!document.fullscreenElement) {
    await reportEvent('FULLSCREEN_EXIT', 'HIGH', 'Candidate exited fullscreen during the exam')
  }
}

async function enterFullscreen() {
  await document.documentElement.requestFullscreen?.()
}

watch(
  workspaceVisible,
  (visible) => {
    if (visible) startCountdown()
    else stopCountdown()
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
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <AppShellSection
    eyebrow="Candidate Center"
    title="Take assigned exams with timed save-and-submit flow"
    description="Candidates can review assigned exams, open the active workspace, save progress, and submit papers. Visibility and fullscreen exits are recorded as baseline anti-cheat telemetry."
  >
    <section class="panel-card section-card">
      <el-table :data="exams" v-loading="loading">
        <el-table-column prop="examName" label="Exam" min-width="220" />
        <el-table-column prop="paperName" label="Paper" min-width="220" />
        <el-table-column prop="startTime" label="Start" min-width="180" />
        <el-table-column prop="endTime" label="End" min-width="180" />
        <el-table-column prop="answerSheetStatus" label="Sheet Status" min-width="140" />
        <el-table-column label="Action" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain @click="openWorkspace(row.examPlanId)">Enter</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="workspaceVisible" size="70%" :with-header="false">
      <template v-if="workspace">
        <section class="exam-shell">
          <header class="panel-card workspace-hero">
            <div>
              <p class="eyebrow">Live Exam Workspace</p>
              <h2>{{ workspace.examName }}</h2>
              <p class="muted">{{ workspace.instructionText }}</p>
            </div>
            <div class="workspace-meta">
              <span class="countdown">{{ countdownText }}</span>
              <el-button @click="enterFullscreen">Enter Fullscreen</el-button>
              <el-button :loading="saving" @click="saveCurrent">Save</el-button>
              <el-button type="primary" :loading="submitting" @click="submitCurrent">Submit</el-button>
            </div>
          </header>

          <section class="workspace-grid">
            <article v-for="item in workspace.items" :key="item.questionId" class="panel-card question-card">
              <div class="question-header">
                <span class="eyebrow">{{ item.questionCode }} · {{ item.questionType }}</span>
                <strong>{{ item.maxScore }} pts</strong>
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
                <el-input v-model="answers[item.questionId]" type="textarea" :rows="4" placeholder="Write your answer here" />
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
}
</style>
