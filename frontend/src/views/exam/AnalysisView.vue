<script setup lang="ts">
import { onMounted } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import MetricCard from '../../components/MetricCard.vue'
import { useAsyncState } from '../../hooks/useAsyncState'
import { fetchAnalysisOverview } from '../../api/exam'

const { data: overview, run } = useAsyncState({
  totalExamPlans: 0,
  totalAnswerSheets: 0,
  averageScore: 0,
  passRate: 0,
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
  }>,
  rankings: [] as Array<{ rankNo: number; candidateName: string; examName: string; finalScore: number }>,
  scoreBands: [] as Array<{ bandName: string; candidateCount: number }>,
  knowledgePoints: [] as Array<{ knowledgePoint: string; averageScoreRate: number; answerCount: number }>,
  questionScoreRates: [] as Array<{ questionId: number; questionCode?: string; stem?: string; averageScoreRate: number; answerCount: number }>
})

onMounted(() => {
  run(fetchAnalysisOverview)
})
</script>

<template>
  <AppShellSection
    eyebrow="Analytics"
    title="Track score spread, participation, and pass rate"
    description="The analytics view turns raw score records into operational signals. It helps teaching teams see participation, grading completion, and exam quality without leaving the core monolith."
  >
    <div class="metrics-grid">
      <MetricCard label="Exam Plans" :value="overview.totalExamPlans" description="Configured release units" />
      <MetricCard label="Answer Sheets" :value="overview.totalAnswerSheets" description="Tracked candidate attempts" />
      <MetricCard label="Average Score" :value="overview.averageScore" description="Overall final-score mean" />
      <MetricCard label="Pass Rate" :value="overview.passRate" description="Percentage of passing records" />
    </div>

    <section class="panel-card section-card">
      <h3>考试表现概览</h3>
      <el-table :data="overview.examPerformances">
        <el-table-column prop="examName" label="Exam" min-width="220" />
        <el-table-column prop="candidateCount" label="Candidates" min-width="100" />
        <el-table-column prop="submittedCount" label="Submitted" min-width="100" />
        <el-table-column prop="gradedCount" label="Published" min-width="100" />
        <el-table-column prop="averageScore" label="Average" min-width="100" />
        <el-table-column prop="highestScore" label="Highest" min-width="100" />
        <el-table-column prop="lowestScore" label="Lowest" min-width="100" />
        <el-table-column prop="passRate" label="Pass Rate" min-width="100" />
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

.section-card h3 {
  margin: 0 0 0.9rem;
  font-family: 'Literata', Georgia, serif;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 980px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .analysis-grid {
    grid-template-columns: 1fr;
  }
}
</style>
