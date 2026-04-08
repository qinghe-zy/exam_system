<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchMyExamRecordDetail, fetchMyExamRecords, fetchMyWrongQuestions } from '../../api/exam'
import type { CandidateScoreDetail, CandidateScoreItem, CandidateWrongQuestion, ExamRecord } from '../../types/exam'
import { labelQuestionType } from '../../utils/labels'

const route = useRoute()
const router = useRouter()
const activeTab = ref<'answer-book' | 'wrong-book'>('answer-book')
const recordLoading = ref(false)
const detailLoading = ref(false)
const wrongLoading = ref(false)
const records = ref<ExamRecord[]>([])
const detail = ref<CandidateScoreDetail | null>(null)
const selectedRecordId = ref<number | null>(null)
const wrongQuestions = ref<CandidateWrongQuestion[]>([])

const answerBookSummary = computed(() => {
  const items = detail.value?.items || []
  return {
    total: items.length,
    wrong: items.filter((item) => Number(item.scoreAwarded || 0) < Number(item.maxScore || 0)).length,
    marked: items.filter((item) => item.reviewLaterFlag === 1).length
  }
})

function parseOptions(item: CandidateScoreItem | CandidateWrongQuestion) {
  try {
    return item.optionsJson ? (JSON.parse(item.optionsJson) as string[]) : []
  } catch {
    return []
  }
}

function parseAttachments(item: CandidateScoreItem | CandidateWrongQuestion) {
  try {
    const parsed = item.attachmentJson ? JSON.parse(item.attachmentJson) : []
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((entry) => (typeof entry === 'string' ? { name: entry, url: entry } : entry))
      .filter((entry) => entry.url)
  } catch {
    return []
  }
}

function isWrong(item: CandidateScoreItem) {
  return Number(item.scoreAwarded || 0) < Number(item.maxScore || 0)
}

function formatDateTime(value?: string) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function syncRoute(extraQuery?: Record<string, string | undefined>) {
  const nextQuery: Record<string, string> = {}
  nextQuery.tab = activeTab.value
  if (selectedRecordId.value) {
    nextQuery.recordId = String(selectedRecordId.value)
  }
  Object.entries(extraQuery || {}).forEach(([key, value]) => {
    if (value) {
      nextQuery[key] = value
    }
  })
  await router.replace({ path: '/candidate/review-center', query: nextQuery })
}

async function loadRecords() {
  recordLoading.value = true
  try {
    records.value = await fetchMyExamRecords()
  } finally {
    recordLoading.value = false
  }
}

async function loadWrongQuestions() {
  wrongLoading.value = true
  try {
    wrongQuestions.value = await fetchMyWrongQuestions()
  } finally {
    wrongLoading.value = false
  }
}

async function openRecord(recordId: number, sync = true) {
  detailLoading.value = true
  try {
    detail.value = await fetchMyExamRecordDetail(recordId)
    selectedRecordId.value = recordId
    if (sync) {
      await syncRoute()
    }
  } catch {
    ElMessage.error('答卷详情加载失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

async function focusWrongQuestion(item: CandidateWrongQuestion) {
  if (!item.latestRecordId) return
  activeTab.value = 'answer-book'
  await openRecord(item.latestRecordId)
}

watch(activeTab, async () => {
  await syncRoute()
})

onMounted(async () => {
  const requestedTab = route.query.tab === 'wrong-book' ? 'wrong-book' : 'answer-book'
  activeTab.value = requestedTab
  await Promise.all([loadRecords(), loadWrongQuestions()])
  const requestedRecordId = Number(route.query.recordId || 0)
  const fallbackRecordId = requestedRecordId > 0 ? requestedRecordId : records.value[0]?.id
  if (fallbackRecordId) {
    await openRecord(fallbackRecordId, false)
  }
  await syncRoute()
})
</script>

<template>
  <AppShellSection
    eyebrow="答卷回看"
    title="回看个人答卷，并集中处理错题"
    description="这里将学生已发布成绩对应的答卷按“完整回看”和“错题沉淀”两个视角组织起来，便于考后复盘与针对性纠错。"
  >
    <el-tabs v-model="activeTab" class="review-tabs">
      <el-tab-pane label="答卷回看" name="answer-book">
        <section class="review-layout">
          <article class="panel-card list-panel">
            <div class="panel-head">
              <div>
                <strong>已发布答卷</strong>
                <p class="muted">选择一场考试，右侧会展示完整作答与逐题结果。</p>
              </div>
            </div>
            <el-table
              :data="records"
              v-loading="recordLoading"
              highlight-current-row
              :current-row-key="selectedRecordId || undefined"
              row-key="id"
              @current-change="(row: ExamRecord | undefined) => row && openRecord(row.id)"
            >
              <el-table-column prop="examName" label="考试名称" min-width="200" />
              <el-table-column prop="submittedAt" label="提交时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
              </el-table-column>
              <el-table-column prop="finalScore" label="总分" min-width="90" />
              <el-table-column label="操作" min-width="110" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openRecord(row.id)">查看答卷</el-button>
                </template>
              </el-table-column>
            </el-table>
          </article>

          <article class="panel-card detail-panel" v-loading="detailLoading">
            <template v-if="detail">
              <div class="detail-head">
                <div>
                  <p class="eyebrow">答卷档案</p>
                  <h2>{{ detail.examName }}</h2>
                  <p class="muted">试卷：{{ detail.paperName }} · 提交时间：{{ formatDateTime(detail.submittedAt) }}</p>
                </div>
                <div class="summary-grid">
                  <div class="summary-card">
                    <span>总题数</span>
                    <strong>{{ answerBookSummary.total }}</strong>
                  </div>
                  <div class="summary-card">
                    <span>错题数</span>
                    <strong>{{ answerBookSummary.wrong }}</strong>
                  </div>
                  <div class="summary-card">
                    <span>待复查标记</span>
                    <strong>{{ answerBookSummary.marked }}</strong>
                  </div>
                  <div class="summary-card">
                    <span>总分</span>
                    <strong>{{ detail.finalScore }}</strong>
                  </div>
                </div>
              </div>

              <article
                v-for="item in detail.items"
                :key="item.questionId"
                class="answer-item"
                :class="{ 'answer-item--wrong': isWrong(item), 'answer-item--marked': item.reviewLaterFlag === 1 }"
              >
                <div class="answer-item-head">
                  <div>
                    <p class="eyebrow">第 {{ item.questionOrder }} 题 · {{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</p>
                    <div v-if="item.materialContent" class="material-block" v-html="item.materialContent"></div>
                    <h3 v-if="!item.stemHtml">{{ item.stem }}</h3>
                    <div v-else class="stem-html" v-html="item.stemHtml"></div>
                  </div>
                  <div class="item-tags">
                    <el-tag v-if="item.reviewLaterFlag === 1" type="warning">考试中标记为待复查</el-tag>
                    <el-tag :type="isWrong(item) ? 'danger' : 'success'">{{ item.scoreAwarded ?? 0 }} / {{ item.maxScore ?? 0 }} 分</el-tag>
                  </div>
                </div>

                <div v-if="item.knowledgePoint || item.chapterName" class="meta-row">
                  <span v-if="item.knowledgePoint">知识点：{{ item.knowledgePoint }}</span>
                  <span v-if="item.chapterName">章节：{{ item.chapterName }}</span>
                </div>

                <div v-if="parseAttachments(item).length" class="attachment-list">
                  <a v-for="attachment in parseAttachments(item)" :key="attachment.url" :href="attachment.url" target="_blank" rel="noreferrer">
                    {{ attachment.name || attachment.url }}
                  </a>
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
            </template>

            <el-empty v-else description="当前还没有可回看的已发布答卷" />
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="错题本" name="wrong-book">
        <section class="wrong-grid" v-loading="wrongLoading">
          <article v-for="item in wrongQuestions" :key="item.questionId" class="panel-card wrong-card">
            <div class="wrong-card-head">
              <div>
                <p class="eyebrow">{{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</p>
                <h3 v-if="!item.stemHtml">{{ item.stem }}</h3>
                <div v-else class="stem-html" v-html="item.stemHtml"></div>
              </div>
              <el-tag type="danger">累计错 {{ item.mistakeCount }} 次</el-tag>
            </div>

            <div v-if="item.materialContent" class="material-block" v-html="item.materialContent"></div>

            <div class="meta-row">
              <span v-if="item.latestExamName">最近考试：{{ item.latestExamName }}</span>
              <span v-if="item.knowledgePoint">知识点：{{ item.knowledgePoint }}</span>
              <span v-if="item.chapterName">章节：{{ item.chapterName }}</span>
            </div>

            <div v-if="parseAttachments(item).length" class="attachment-list">
              <a v-for="attachment in parseAttachments(item)" :key="attachment.url" :href="attachment.url" target="_blank" rel="noreferrer">
                {{ attachment.name || attachment.url }}
              </a>
            </div>

            <div v-if="parseOptions(item).length" class="option-list">
              <div v-for="option in parseOptions(item)" :key="option" class="option-pill">{{ option }}</div>
            </div>

            <div class="answer-grid">
              <article class="answer-card">
                <strong>最近一次作答</strong>
                <p>{{ item.latestAnswerContent || '未作答' }}</p>
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
                <strong>最近得分</strong>
                <p>{{ item.latestScoreAwarded ?? 0 }} / {{ item.latestMaxScore ?? 0 }} 分 · {{ formatDateTime(item.latestSubmittedAt) }}</p>
              </article>
            </div>

            <div class="wrong-card-actions">
              <el-button type="primary" plain @click="focusWrongQuestion(item)">定位到最近答卷</el-button>
              <el-tag v-if="item.latestReviewLaterFlag === 1" type="warning">最近一次仍标记为待复查</el-tag>
            </div>
          </article>

          <el-empty v-if="!wrongLoading && !wrongQuestions.length" description="当前没有已沉淀的错题" />
        </section>
      </el-tab-pane>
    </el-tabs>
  </AppShellSection>
</template>

<style scoped>
.review-tabs :deep(.el-tabs__content) {
  padding-top: 0.8rem;
}

.review-layout {
  display: grid;
  grid-template-columns: minmax(20rem, 26rem) minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.list-panel,
.detail-panel,
.wrong-card {
  padding: 1rem;
}

.panel-head p,
.detail-head p,
.answer-card p {
  margin: 0;
}

.detail-head,
.summary-grid,
.answer-grid,
.wrong-grid {
  display: grid;
  gap: 1rem;
}

.detail-head h2,
.answer-item-head h3,
.wrong-card-head h3 {
  margin: 0.35rem 0 0;
  font-family: 'Literata', Georgia, serif;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card,
.answer-card,
.option-pill,
.answer-item {
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 95%, var(--panel-soft));
}

.summary-card,
.answer-card,
.answer-item {
  padding: 0.95rem 1rem;
}

.summary-card strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.45rem;
  color: var(--brand-deep);
}

.answer-item + .answer-item,
.wrong-card + .wrong-card {
  margin-top: 1rem;
}

.answer-item--wrong {
  border-color: color-mix(in oklch, #d45353 36%, white);
}

.answer-item--marked {
  box-shadow: inset 0 0 0 1px color-mix(in oklch, #c68b3c 42%, white);
}

.answer-item-head,
.wrong-card-head,
.item-tags,
.wrong-card-actions,
.meta-row,
.option-list,
.attachment-list {
  display: flex;
  gap: 0.7rem;
}

.answer-item-head,
.wrong-card-head {
  justify-content: space-between;
  align-items: flex-start;
}

.item-tags,
.wrong-card-actions,
.meta-row,
.option-list,
.attachment-list {
  flex-wrap: wrap;
}

.meta-row {
  margin: 0.85rem 0;
  color: var(--muted);
}

.option-pill {
  padding: 0.45rem 0.8rem;
}

.answer-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.answer-card p {
  margin-top: 0.45rem;
  line-height: 1.6;
  color: var(--muted);
  white-space: pre-wrap;
}

.material-block,
.stem-html {
  line-height: 1.75;
}

.wrong-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

@media (max-width: 1120px) {
  .review-layout,
  .summary-grid,
  .answer-grid,
  .wrong-grid {
    grid-template-columns: 1fr;
  }
}
</style>
