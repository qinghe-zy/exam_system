<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { exportExamRecordsCsv, fetchExamRecords, fetchScoreAppeals, processScoreAppeal } from '../../api/exam'
import type { ExamRecord, ScoreAppeal } from '../../types/exam'
import { formatDateTime } from '../../utils/datetime'
import { labelAnswerSheetStatus, labelAppealStatus, labelGradingReviewStatus } from '../../utils/labels'

const loading = ref(false)
const records = ref<ExamRecord[]>([])
const appealVisible = ref(false)
const appealLoading = ref(false)
const currentRecord = ref<ExamRecord | null>(null)
const appeals = ref<ScoreAppeal[]>([])

async function loadData() {
  loading.value = true
  try {
    records.value = await fetchExamRecords()
  } finally {
    loading.value = false
  }
}

async function exportCsv() {
  const csv = await exportExamRecordsCsv()
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'score-records.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('成绩单导出已开始')
}

async function openAppeals(record: ExamRecord) {
  currentRecord.value = record
  appealVisible.value = true
  appealLoading.value = true
  try {
    appeals.value = await fetchScoreAppeals(record.id)
  } finally {
    appealLoading.value = false
  }
}

async function processAppealItem(appealId: number, action: 'REJECT' | 'REJUDGE') {
  await processScoreAppeal(appealId, {
    action,
    processComment: action === 'REJECT' ? '复核后维持原成绩。' : '复核通过，已进入重判流程。'
  })
  ElMessage.success(action === 'REJECT' ? '申诉已驳回' : '申诉已转入重判')
  if (currentRecord.value) {
    await openAppeals(currentRecord.value)
  }
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="成绩中心"
    title="成绩记录、复核与申诉状态"
    description="在这里可以查看成绩记录、复核状态和申诉状态，并对学生申诉执行驳回或转入重判。"
  >
    <template #actions>
      <el-button type="primary" plain @click="exportCsv">导出成绩单</el-button>
    </template>
    <section class="panel-card section-card">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="candidateName" label="考生" min-width="140" />
        <el-table-column prop="examName" label="考试" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="220" />
        <el-table-column label="提交时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观分" min-width="100" />
        <el-table-column prop="subjectiveScore" label="主观分" min-width="100" />
        <el-table-column prop="finalScore" label="总分" min-width="90" />
        <el-table-column label="复核状态" min-width="130">
          <template #default="{ row }">
            <el-tag :type="row.reviewStatus === 'APPROVED' ? 'success' : row.reviewStatus === 'REJUDGE_REQUIRED' ? 'warning' : 'info'">
              {{ labelGradingReviewStatus(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申诉状态" min-width="150">
          <template #default="{ row }">
            <el-tag :type="row.appealStatus === 'SUBMITTED' ? 'warning' : row.appealStatus === 'APPROVED_REJUDGE' ? 'danger' : 'info'">
              {{ labelAppealStatus(row.appealStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否及格" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.passedFlag === 1 ? 'success' : 'danger'">{{ row.passedFlag === 1 ? '及格' : '不及格' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="140">
          <template #default="{ row }">
            <el-tag :type="row.publishedFlag === 1 ? 'success' : 'warning'">{{ labelAnswerSheetStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button plain type="warning" @click="openAppeals(row)">处理申诉</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="appealVisible" size="min(720px, 100vw)" :with-header="false">
      <section class="appeal-shell" v-loading="appealLoading">
        <header class="panel-card section-card" v-if="currentRecord">
          <p class="eyebrow">成绩申诉治理</p>
          <h2>{{ currentRecord.examName }} · {{ currentRecord.candidateName }}</h2>
          <p class="muted">
            当前总分：{{ currentRecord.finalScore }} ·
            复核：{{ labelGradingReviewStatus(currentRecord.reviewStatus) }} ·
            申诉：{{ labelAppealStatus(currentRecord.appealStatus) }}
          </p>
        </header>

        <section class="panel-card section-card">
          <div class="appeal-header">
            <strong>申诉记录</strong>
            <span class="muted">当前支持“驳回”或“转入重判”两种治理动作。</span>
          </div>
          <el-empty v-if="!appeals.length" description="当前成绩暂无申诉记录" />
            <article v-for="appeal in appeals" :key="appeal.id" class="appeal-card">
              <div class="appeal-card-head">
                <div>
                  <p class="eyebrow">提交时间：{{ formatDateTime(appeal.submittedAt) }}</p>
                  <h3>{{ labelAppealStatus(appeal.status) }}</h3>
                </div>
              <div class="appeal-actions" v-if="appeal.status === 'SUBMITTED'">
                <el-button type="warning" plain @click="processAppealItem(appeal.id, 'REJECT')">驳回</el-button>
                <el-button type="danger" @click="processAppealItem(appeal.id, 'REJUDGE')">转入重判</el-button>
              </div>
            </div>
            <p><strong>申诉原因：</strong>{{ appeal.appealReason }}</p>
            <p><strong>期望结果：</strong>{{ appeal.expectedOutcome || '未填写' }}</p>
            <p><strong>处理意见：</strong>{{ appeal.processComment || '待处理' }}</p>
            <p class="muted">处理人：{{ appeal.processedByName || '待处理' }} · 处理时间：{{ formatDateTime(appeal.processedAt) }}</p>
          </article>
        </section>
      </section>
    </el-drawer>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}

.table-actions,
.appeal-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.appeal-shell {
  display: grid;
  gap: 1rem;
}

.appeal-header,
.appeal-card-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.appeal-card + .appeal-card {
  margin-top: 1rem;
}

.appeal-card {
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
  border-radius: 18px;
  padding: 1rem;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
}

.appeal-card h3 {
  margin: 0.3rem 0 0;
  font-family: 'Literata', Georgia, serif;
}
</style>
