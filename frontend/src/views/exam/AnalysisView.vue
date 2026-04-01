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
  }>
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

@media (max-width: 980px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
