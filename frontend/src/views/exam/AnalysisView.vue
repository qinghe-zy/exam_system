<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import MetricCard from '../../components/MetricCard.vue'
import { useAsyncState } from '../../hooks/useAsyncState'
import { exportAnalysisOverviewCsv, exportAnalysisQualityReportMarkdown, fetchAnalysisOverview, fetchAnalysisQualityReport } from '../../api/exam'
import type { AnalysisQualityReport } from '../../types/exam'

const { data: overview, run } = useAsyncState({
  totalExamPlans: 0,
  totalAnswerSheets: 0,
  averageScore: 0,
  passRate: 0,
  excellentRate: 0,
  examPerformances: [] as Array<{
    examPlanId: number
    examName: string
    candidateCount: number
    submittedCount: number
    gradedCount: number
    averageScore: number
    highestScore: number
    lowestScore: number
    passRate: number
    excellentRate: number
  }>,
  organizationComparisons: [] as Array<{
    organizationName: string
    candidateCount: number
    averageScore: number
    passRate: number
    excellentRate: number
  }>,
  trendPoints: [] as Array<{
    examPlanId: number
    examName: string
    periodLabel: string
    averageScore: number
    passRate: number
    excellentRate: number
  }>,
  rankings: [] as Array<{ rankNo: number; candidateName: string; examName: string; finalScore: number }>,
  scoreBands: [] as Array<{ bandName: string; candidateCount: number }>,
  knowledgePoints: [] as Array<{ knowledgePoint: string; averageScoreRate: number; answerCount: number }>,
  questionScoreRates: [] as Array<{ questionId: number; questionCode?: string; stem?: string; averageScoreRate: number; answerCount: number }>
})

const qualityReportVisible = ref(false)
const qualityReportLoading = ref(false)
const qualityReport = ref<AnalysisQualityReport | null>(null)

onMounted(() => {
  run(fetchAnalysisOverview)
})

async function exportCsv() {
  const csv = await exportAnalysisOverviewCsv()
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'analysis-overview.csv'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('分析报表导出已开始')
}

async function openQualityReport() {
  qualityReportVisible.value = true
  qualityReportLoading.value = true
  try {
    qualityReport.value = await fetchAnalysisQualityReport()
  } finally {
    qualityReportLoading.value = false
  }
}

async function exportQualityReport() {
  const markdown = await exportAnalysisQualityReportMarkdown()
  const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'exam-quality-report.md'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('质量报告导出已开始')
}
</script>

<template>
  <AppShellSection
    eyebrow="成绩分析"
    title="查看成绩分布、参与情况与及格率"
    description="将成绩记录转化为教学与运营可读的统计结果，并支持查看质量报告与导出分析材料。"
  >
    <template #actions>
      <el-button plain @click="openQualityReport">查看质量报告</el-button>
      <el-button type="primary" plain @click="exportCsv">导出分析报表</el-button>
      <el-button type="success" plain @click="exportQualityReport">导出质量报告</el-button>
    </template>
    <div class="metrics-grid">
      <MetricCard label="考试计划数" :value="overview.totalExamPlans" description="当前纳入统计的考试计划数量" />
      <MetricCard label="答卷数" :value="overview.totalAnswerSheets" description="当前统计范围内的答卷数量" />
      <MetricCard label="平均分" :value="overview.averageScore" description="所有成绩记录的平均总分" />
      <MetricCard label="及格率" :value="overview.passRate" description="已达到及格线的成绩占比" />
      <MetricCard label="优秀率" :value="overview.excellentRate" description="分数达到 90 分及以上的成绩占比" />
    </div>

    <section class="panel-card section-card">
      <h3>考试表现概览</h3>
      <el-table :data="overview.examPerformances">
        <el-table-column prop="examName" label="考试" min-width="220" />
        <el-table-column prop="candidateCount" label="考生数" min-width="100" />
        <el-table-column prop="submittedCount" label="已提交" min-width="100" />
        <el-table-column prop="gradedCount" label="已发布" min-width="100" />
        <el-table-column prop="averageScore" label="平均分" min-width="100" />
        <el-table-column prop="highestScore" label="最高分" min-width="100" />
        <el-table-column prop="lowestScore" label="最低分" min-width="100" />
        <el-table-column prop="passRate" label="及格率" min-width="100" />
        <el-table-column prop="excellentRate" label="优秀率" min-width="100" />
      </el-table>
    </section>

    <section class="analysis-grid">
      <article class="panel-card section-card">
        <h3>总分排名</h3>
        <el-table :data="overview.rankings">
          <el-table-column prop="rankNo" label="名次" min-width="80" />
          <el-table-column prop="candidateName" label="考生" min-width="120" />
          <el-table-column prop="examName" label="考试" min-width="180" />
          <el-table-column prop="finalScore" label="成绩" min-width="90" />
        </el-table>
      </article>

      <article class="panel-card section-card">
        <h3>分数段分布</h3>
        <el-table :data="overview.scoreBands">
          <el-table-column prop="bandName" label="分数段" min-width="120" />
          <el-table-column prop="candidateCount" label="人数" min-width="100" />
        </el-table>
      </article>
    </section>

    <section class="analysis-grid">
      <article class="panel-card section-card">
        <h3>组织对比</h3>
        <el-table :data="overview.organizationComparisons">
          <el-table-column prop="organizationName" label="组织" min-width="180" />
          <el-table-column prop="candidateCount" label="人数" min-width="90" />
          <el-table-column prop="averageScore" label="平均分" min-width="100" />
          <el-table-column prop="passRate" label="及格率" min-width="100" />
          <el-table-column prop="excellentRate" label="优秀率" min-width="100" />
        </el-table>
      </article>

      <article class="panel-card section-card">
        <h3>历次考试趋势</h3>
        <el-table :data="overview.trendPoints">
          <el-table-column prop="periodLabel" label="时间" min-width="160" />
          <el-table-column prop="examName" label="考试" min-width="180" />
          <el-table-column prop="averageScore" label="平均分" min-width="100" />
          <el-table-column prop="passRate" label="及格率" min-width="100" />
          <el-table-column prop="excellentRate" label="优秀率" min-width="100" />
        </el-table>
      </article>
    </section>

    <section class="analysis-grid">
      <article class="panel-card section-card">
        <h3>知识点掌握分析</h3>
        <el-table :data="overview.knowledgePoints">
          <el-table-column prop="knowledgePoint" label="知识点" min-width="180" />
          <el-table-column prop="averageScoreRate" label="平均得分率" min-width="120" />
          <el-table-column prop="answerCount" label="作答次数" min-width="100" />
        </el-table>
      </article>

      <article class="panel-card section-card">
        <h3>题目得分率</h3>
        <el-table :data="overview.questionScoreRates">
          <el-table-column prop="questionCode" label="题目编码" min-width="120" />
          <el-table-column prop="averageScoreRate" label="平均得分率" min-width="120" />
          <el-table-column prop="answerCount" label="作答次数" min-width="100" />
          <el-table-column prop="stem" label="题干" min-width="220" show-overflow-tooltip />
        </el-table>
      </article>
    </section>

    <el-drawer v-model="qualityReportVisible" size="min(920px, 100vw)" :with-header="false">
      <section class="report-shell" v-loading="qualityReportLoading">
        <header v-if="qualityReport" class="panel-card section-card">
          <p class="eyebrow">考试质量报告</p>
          <h2>综合质量分 {{ qualityReport.overallQualityScore }} · {{ qualityReport.overallQualityLevel }}</h2>
          <p class="muted">{{ qualityReport.summary }}</p>
        </header>

        <section v-if="qualityReport" class="panel-card section-card">
          <h3>风险提示</h3>
          <p class="report-copy">{{ qualityReport.riskSummary }}</p>
        </section>

        <section v-if="qualityReport" class="analysis-grid">
          <article class="panel-card section-card">
            <h3>维度评分</h3>
            <article v-for="item in qualityReport.dimensionScores" :key="item.dimensionName" class="report-card">
              <div class="report-card-head">
                <strong>{{ item.dimensionName }}</strong>
                <el-tag>{{ item.level }} · {{ item.score }}</el-tag>
              </div>
              <p>{{ item.summary }}</p>
            </article>
          </article>

          <article class="panel-card section-card">
            <h3>建议动作</h3>
            <ul class="report-list">
              <li v-for="item in qualityReport.recommendations" :key="item">{{ item }}</li>
            </ul>
          </article>
        </section>

        <section v-if="qualityReport" class="panel-card section-card">
          <h3>分考试质量观察</h3>
          <el-table :data="qualityReport.examInsights">
            <el-table-column prop="examName" label="考试" min-width="220" />
            <el-table-column prop="candidateCount" label="考生数" min-width="100" />
            <el-table-column prop="gradedCount" label="已发布" min-width="100" />
            <el-table-column prop="averageScore" label="平均分" min-width="100" />
            <el-table-column prop="passRate" label="及格率" min-width="100" />
            <el-table-column prop="level" label="等级" min-width="100" />
            <el-table-column prop="summary" label="结论" min-width="220" show-overflow-tooltip />
            <el-table-column prop="risk" label="风险" min-width="220" show-overflow-tooltip />
          </el-table>
        </section>

        <section v-if="qualityReport" class="analysis-grid">
          <article class="panel-card section-card">
            <h3>薄弱知识点</h3>
            <el-table :data="qualityReport.weakKnowledgePoints">
              <el-table-column prop="knowledgePoint" label="知识点" min-width="180" />
              <el-table-column prop="averageScoreRate" label="平均得分率" min-width="120" />
              <el-table-column prop="answerCount" label="作答次数" min-width="100" />
            </el-table>
          </article>

          <article class="panel-card section-card">
            <h3>薄弱题目</h3>
            <el-table :data="qualityReport.weakQuestions">
              <el-table-column prop="questionCode" label="题目编码" min-width="120" />
              <el-table-column prop="averageScoreRate" label="平均得分率" min-width="120" />
              <el-table-column prop="answerCount" label="作答次数" min-width="100" />
              <el-table-column prop="stem" label="题干" min-width="220" show-overflow-tooltip />
            </el-table>
          </article>
        </section>
      </section>
    </el-drawer>
  </AppShellSection>
</template>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.section-card {
  padding: 1rem;
}

.section-card h3,
.report-shell h2 {
  margin: 0 0 0.9rem;
  font-family: 'Literata', Georgia, serif;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.report-shell {
  display: grid;
  gap: 1rem;
}

.report-card + .report-card {
  margin-top: 1rem;
}

.report-card {
  border: 1px solid color-mix(in oklch, var(--line) 74%, white);
  border-radius: 18px;
  padding: 1rem;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
}

.report-card-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.report-list {
  margin: 0;
  padding-left: 1.2rem;
  display: grid;
  gap: 0.6rem;
}

.report-copy {
  margin: 0;
  line-height: 1.75;
}

@media (max-width: 980px) {
  .metrics-grid,
  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
