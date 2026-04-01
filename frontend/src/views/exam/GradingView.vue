<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchGradingTasks, fetchGradingWorkspace, submitGrading } from '../../api/exam'
import type { GradingTask, GradingWorkspace } from '../../types/exam'

const loading = ref(false)
const submitting = ref(false)
const tasks = ref<GradingTask[]>([])
const drawerVisible = ref(false)
const workspace = ref<GradingWorkspace | null>(null)
const gradeForm = reactive<Record<number, { scoreAwarded: number; reviewComment: string }>>({})

async function loadData() {
  loading.value = true
  try {
    tasks.value = await fetchGradingTasks()
  } finally {
    loading.value = false
  }
}

async function openWorkspace(answerSheetId: number) {
  workspace.value = await fetchGradingWorkspace(answerSheetId)
  Object.keys(gradeForm).forEach((key) => {
    delete gradeForm[Number(key)]
  })
  workspace.value.items.forEach((item) => {
    gradeForm[item.answerItemId || item.questionId] = {
      scoreAwarded: item.scoreAwarded || 0,
      reviewComment: item.reviewComment || ''
    }
  })
  drawerVisible.value = true
}

async function submitCurrent() {
  if (!workspace.value) return
  submitting.value = true
  try {
    workspace.value = await submitGrading(
      workspace.value.answerSheetId,
      workspace.value.items
        .filter((item) => !['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(item.questionType))
        .map((item) => ({
          answerItemId: item.answerItemId!,
          scoreAwarded: gradeForm[item.answerItemId || item.questionId]?.scoreAwarded || 0,
          reviewComment: gradeForm[item.answerItemId || item.questionId]?.reviewComment || ''
        }))
    )
    ElMessage.success('Grading updated')
    await loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="Grading Center"
    title="Review subjective answers and finalize scores"
    description="The grading center works from submitted answer sheets, preserving objective auto-score results while letting graders evaluate subjective responses and publish the final score record."
  >
    <section class="panel-card section-card">
      <el-table :data="tasks" v-loading="loading">
        <el-table-column prop="examName" label="Exam" min-width="220" />
        <el-table-column prop="candidateName" label="Candidate" min-width="140" />
        <el-table-column prop="submittedAt" label="Submitted At" min-width="180" />
        <el-table-column prop="objectiveScore" label="Objective" min-width="100" />
        <el-table-column prop="pendingQuestionCount" label="Pending Subjective" min-width="140" />
        <el-table-column prop="status" label="Status" min-width="140" />
        <el-table-column label="Action" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain @click="openWorkspace(row.answerSheetId)">Open</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="drawerVisible" size="72%" :with-header="false">
      <template v-if="workspace">
        <section class="grading-shell">
          <header class="panel-card workspace-hero">
            <div>
              <p class="eyebrow">Grading Workspace</p>
              <h2>{{ workspace.examName }} · {{ workspace.candidateName }}</h2>
              <p class="muted">Objective Score: {{ workspace.objectiveScore }} · Current Final: {{ workspace.finalScore }}</p>
            </div>
            <el-button type="primary" :loading="submitting" @click="submitCurrent">Submit Grades</el-button>
          </header>

          <article v-for="item in workspace.items" :key="item.answerItemId || item.questionId" class="panel-card answer-card">
            <div class="answer-header">
              <span class="eyebrow">{{ item.questionCode }} · {{ item.questionType }}</span>
              <strong>{{ item.maxScore }} pts</strong>
            </div>
            <p class="question-stem">{{ item.stem }}</p>
            <div class="answer-block">
              <span class="muted">Candidate Answer</span>
              <p>{{ item.answerContent || 'No answer submitted' }}</p>
            </div>

            <template v-if="['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(item.questionType)">
              <div class="objective-result">
                <el-tag type="success">{{ item.status }}</el-tag>
                <span>{{ item.scoreAwarded }} / {{ item.maxScore }}</span>
              </div>
            </template>
            <template v-else>
              <div class="grading-form">
                <el-input-number v-model="gradeForm[item.answerItemId || item.questionId].scoreAwarded" :min="0" :max="item.maxScore" />
                <el-input v-model="gradeForm[item.answerItemId || item.questionId].reviewComment" type="textarea" :rows="3" placeholder="Feedback or marking comment" />
              </div>
            </template>
          </article>
        </section>
      </template>
    </el-drawer>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}

.grading-shell {
  display: grid;
  gap: 1rem;
}

.workspace-hero,
.answer-card {
  padding: 1rem;
}

.workspace-hero {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.workspace-hero h2 {
  margin: 0.35rem 0;
  font-family: 'Literata', Georgia, serif;
}

.answer-card {
  display: grid;
  gap: 0.8rem;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-stem,
.answer-block p {
  margin: 0;
  line-height: 1.7;
}

.grading-form {
  display: grid;
  gap: 0.8rem;
}

.objective-result {
  display: flex;
  gap: 0.8rem;
  align-items: center;
}

@media (max-width: 980px) {
  .workspace-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
