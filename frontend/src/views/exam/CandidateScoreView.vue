<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchMyExamRecordDetail, fetchMyExamRecords, fetchMyScoreAppeals, submitScoreAppeal } from '../../api/exam'
import type { CandidateScoreDetail, CandidateScoreItem, ExamRecord, ScoreAppeal } from '../../types/exam'
import { formatDateTime } from '../../utils/datetime'
import { labelAnswerSheetStatus, labelAppealStatus, labelGradingReviewStatus, labelQuestionType } from '../../utils/labels'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const appealSubmitting = ref(false)
const records = ref<ExamRecord[]>([])
const detailVisible = ref(false)
const detail = ref<CandidateScoreDetail | null>(null)
const appeals = ref<ScoreAppeal[]>([])
const appealForm = reactive({
  appealReason: '',
  expectedOutcome: ''
})

const passedText = computed(() => {
  if (!detail.value) return ''
  return detail.value.passedFlag === 1 ? '已及格' : '未及格'
})

const canSubmitAppeal = computed(() => {
  if (!detail.value || detail.value.publishedFlag !== 1) return false
  return !appeals.value.some((item) => ['SUBMITTED', 'APPROVED_REJUDGE'].includes(item.status))
})

function parseOptions(item: CandidateScoreItem) {
  try {
    return item.optionsJson ? (JSON.parse(item.optionsJson) as string[]) : []
  } catch {
    return []
  }
}

async function loadRecords() {
  loading.value = true
  try {
    records.value = await fetchMyExamRecords()
  } finally {
    loading.value = false
  }
}

async function loadAppeals(recordId: number) {
  appeals.value = await fetchMyScoreAppeals(recordId)
}

async function openDetail(recordId: number) {
  detailLoading.value = true
  try {
    detail.value = await fetchMyExamRecordDetail(recordId)
    await loadAppeals(recordId)
    detailVisible.value = true
    const nextQuery = { ...route.query, recordId: String(recordId) }
    await router.replace({ path: '/candidate/scores', query: nextQuery })
  } catch {
    ElMessage.error('成绩详情加载失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

async function submitAppeal() {
  if (!detail.value) return
  if (!appealForm.appealReason.trim()) {
    ElMessage.warning('请先填写申诉原因')
    return
  }
  appealSubmitting.value = true
  try {
    await submitScoreAppeal(detail.value.id, {
      appealReason: appealForm.appealReason.trim(),
      expectedOutcome: appealForm.expectedOutcome.trim() || undefined
    })
    appealForm.appealReason = ''
    appealForm.expectedOutcome = ''
    await loadAppeals(detail.value.id)
    await loadRecords()
    ElMessage.success('成绩申诉已提交')
  } finally {
    appealSubmitting.value = false
  }
}

async function openReviewCenter(recordId: number) {
  await router.push({ path: '/candidate/review-center', query: { recordId: String(recordId) } })
}

async function closeDetail() {
  detailVisible.value = false
  detail.value = null
  appeals.value = []
  appealForm.appealReason = ''
  appealForm.expectedOutcome = ''
  const nextQuery = { ...route.query }
  delete nextQuery.recordId
  await router.replace({ path: '/candidate/scores', query: nextQuery })
}

onMounted(async () => {
  await loadRecords()
  const recordId = Number(route.query.recordId || 0)
  if (recordId > 0) {
    await openDetail(recordId)
  }
})
</script>

<template>
  <AppShellSection
    eyebrow="我的成绩"
    title="已发布成绩、申诉与逐题解析"
    description="学生只能查看已发布的成绩记录，并可在成绩详情页提交申诉、查看处理进度和逐题解析。"
  >
    <section class="panel-card section-card">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="examName" label="考试名称" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="220" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观分" min-width="90" />
        <el-table-column prop="subjectiveScore" label="主观分" min-width="90" />
        <el-table-column prop="finalScore" label="总分" min-width="90" />
        <el-table-column label="复核状态" min-width="120">
          <template #default="{ row }">{{ labelGradingReviewStatus(row.reviewStatus) }}</template>
        </el-table-column>
        <el-table-column label="申诉状态" min-width="140">
          <template #default="{ row }">{{ labelAppealStatus(row.appealStatus) }}</template>
        </el-table-column>
        <el-table-column label="结果" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.passedFlag === 1 ? 'success' : 'danger'">{{ row.passedFlag === 1 ? '及格' : '未及格' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ labelAnswerSheetStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openDetail(row.id)">查看详情</el-button>
              <el-button link type="success" @click="openReviewCenter(row.id)">答卷回看</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer
      v-model="detailVisible"
      size="min(920px, 100vw)"
      :with-header="false"
      :destroy-on-close="false"
      @close="closeDetail"
    >
      <div v-loading="detailLoading" class="detail-shell">
        <section class="panel-card detail-head" v-if="detail">
          <div>
            <p class="eyebrow">成绩详情</p>
            <h2>{{ detail.examName }}</h2>
            <p class="muted">
              试卷：{{ detail.paperName }} · 提交时间：{{ formatDateTime(detail.submittedAt) }} ·
              复核：{{ labelGradingReviewStatus(detail.reviewStatus) }} ·
              申诉：{{ labelAppealStatus(detail.appealStatus) }}
            </p>
          </div>
          <div class="score-summary">
            <div class="summary-card"><span>客观分</span><strong>{{ detail.objectiveScore }}</strong></div>
            <div class="summary-card"><span>主观分</span><strong>{{ detail.subjectiveScore }}</strong></div>
            <div class="summary-card"><span>总分</span><strong>{{ detail.finalScore }}</strong></div>
            <div class="summary-card"><span>结果</span><strong>{{ passedText }}</strong></div>
          </div>
        </section>

        <section class="panel-card detail-panel" v-if="detail">
          <div class="detail-panel-head">
            <strong>成绩申诉</strong>
            <span class="muted">当前仅支持提交一条处理中申诉；若已在处理中，则需等待结果。</span>
          </div>
          <div class="appeal-form">
            <el-input
              v-model="appealForm.appealReason"
              type="textarea"
              :rows="3"
              placeholder="请说明你认为分数需要复核的原因"
              :disabled="!canSubmitAppeal"
            />
            <el-input
              v-model="appealForm.expectedOutcome"
              placeholder="可选：希望复核后如何处理，例如重新核对主观题评分"
              :disabled="!canSubmitAppeal"
            />
            <el-button type="warning" :disabled="!canSubmitAppeal" :loading="appealSubmitting" @click="submitAppeal">
              提交申诉
            </el-button>
          </div>

          <el-empty v-if="!appeals.length" description="当前成绩暂无申诉记录" />
          <article v-for="appeal in appeals" :key="appeal.id" class="appeal-card">
            <div class="appeal-card-head">
              <div>
                <p class="eyebrow">提交时间：{{ formatDateTime(appeal.submittedAt) }}</p>
                <h3>{{ labelAppealStatus(appeal.status) }}</h3>
              </div>
              <el-tag :type="appeal.status === 'SUBMITTED' ? 'warning' : appeal.status === 'REJECTED' ? 'danger' : 'success'">
                {{ labelAppealStatus(appeal.status) }}
              </el-tag>
            </div>
            <p><strong>申诉原因：</strong>{{ appeal.appealReason }}</p>
            <p><strong>期望结果：</strong>{{ appeal.expectedOutcome || '未填写' }}</p>
            <p><strong>处理意见：</strong>{{ appeal.processComment || '待处理' }}</p>
            <p class="muted">处理人：{{ appeal.processedByName || '待处理' }} · 处理时间：{{ formatDateTime(appeal.processedAt) }}</p>
          </article>
        </section>

        <section class="panel-card detail-panel" v-if="detail">
          <div class="detail-panel-head">
            <strong>逐题结果</strong>
            <span class="muted">当前仅展示已发布成绩所对应的答卷与解析</span>
          </div>
          <article v-for="item in detail.items" :key="item.questionId" class="score-item">
            <div class="score-item-head">
              <div>
                <p class="eyebrow">第 {{ item.questionOrder }} 题 · {{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</p>
                <h3>{{ item.stem }}</h3>
              </div>
              <el-tag type="warning">{{ item.scoreAwarded ?? 0 }} / {{ item.maxScore ?? 0 }} 分</el-tag>
            </div>

            <div v-if="parseOptions(item).length" class="option-list">
              <div v-for="option in parseOptions(item)" :key="option" class="option-pill">{{ option }}</div>
            </div>

            <div class="answer-grid">
              <article class="answer-card">
                <strong>我的答案</strong>
                <p>{{ item.answerContent || '未作答' }}</p>
              </article>
              <article class="answer-card">
                <strong>参考答案</strong>
                <p>{{ item.referenceAnswer || '当前题目未提供标准答案展示' }}</p>
              </article>
              <article class="answer-card">
                <strong>解析说明</strong>
                <p>{{ item.analysisText || '当前题目未配置解析说明。' }}</p>
              </article>
              <article class="answer-card">
                <strong>阅卷备注</strong>
                <p>{{ item.reviewComment || '当前题目无附加阅卷备注。' }}</p>
              </article>
            </div>
          </article>
        </section>
      </div>
    </el-drawer>
  </AppShellSection>
</template>

<style scoped>
.section-card,
.detail-head,
.detail-panel {
  padding: 1rem;
}

.detail-shell,
.score-summary,
.answer-grid,
.appeal-form {
  display: grid;
  gap: 1rem;
}

.detail-head {
  margin-bottom: 1rem;
}

.detail-head h2,
.score-item-head h3,
.appeal-card h3 {
  margin: 0.3rem 0 0;
  font-family: 'Literata', Georgia, serif;
}

.score-summary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 1rem;
}

.summary-card,
.answer-card,
.option-pill,
.score-item,
.appeal-card {
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
}

.summary-card,
.answer-card,
.score-item,
.appeal-card {
  padding: 0.95rem 1rem;
}

.summary-card strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.45rem;
  color: var(--brand-deep);
}

.detail-panel-head,
.score-item-head,
.appeal-card-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.score-item + .score-item,
.appeal-card + .appeal-card {
  margin-top: 1rem;
}

.option-list,
.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.option-pill {
  padding: 0.45rem 0.8rem;
}

.answer-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.answer-card p,
.appeal-card p {
  margin: 0.45rem 0 0;
  line-height: 1.6;
  color: var(--muted);
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .score-summary,
  .answer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
