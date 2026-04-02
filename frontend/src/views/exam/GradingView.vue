<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchGradingTasks, fetchGradingWorkspace, submitGrading } from '../../api/exam'
import type { GradingTask, GradingWorkspace } from '../../types/exam'
import { labelAnswerSheetStatus, labelQuestionType } from '../../utils/labels'

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
    ElMessage.success('阅卷结果已更新')
    await loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="阅卷中心"
    title="主观题评分与成绩确认"
    description="阅卷中心面向已提交答卷，保留客观题自动评分结果，并支持阅卷老师完成主观题评分与成绩确认。"
  >
    <section class="panel-card section-card">
      <el-table :data="tasks" v-loading="loading">
        <el-table-column prop="examName" label="考试" min-width="220" />
        <el-table-column prop="candidateName" label="考生" min-width="140" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
        <el-table-column prop="objectiveScore" label="客观分" min-width="100" />
        <el-table-column prop="pendingQuestionCount" label="待阅主观题" min-width="140" />
        <el-table-column label="状态" min-width="140"><template #default="{ row }">{{ labelAnswerSheetStatus(row.status) }}</template></el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain @click="openWorkspace(row.answerSheetId)">打开</el-button>
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
              <p class="eyebrow">阅卷工作区</p>
              <h2>{{ workspace.examName }} · {{ workspace.candidateName }}</h2>
              <p class="muted">客观分：{{ workspace.objectiveScore }} · 当前总分：{{ workspace.finalScore }}</p>
            </div>
            <el-button type="primary" :loading="submitting" @click="submitCurrent">提交评分</el-button>
          </header>

          <article v-for="item in workspace.items" :key="item.answerItemId || item.questionId" class="panel-card answer-card">
            <div class="answer-header">
              <span class="eyebrow">{{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</span>
              <strong>{{ item.maxScore }} pts</strong>
            </div>
            <p class="question-stem">{{ item.stem }}</p>
            <div class="answer-block">
              <span class="muted">考生作答</span>
              <p>{{ item.answerContent || '未作答' }}</p>
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
                <el-input v-model="gradeForm[item.answerItemId || item.questionId].reviewComment" type="textarea" :rows="3" placeholder="请输入评分说明" />
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
