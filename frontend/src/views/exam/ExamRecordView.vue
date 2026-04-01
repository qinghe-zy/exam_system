<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchExamRecords } from '../../api/exam'
import type { ExamRecord } from '../../types/exam'

const loading = ref(false)
const records = ref<ExamRecord[]>([])

async function loadData() {
  loading.value = true
  try {
    records.value = await fetchExamRecords()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="Score Center"
    title="Published score records and grading status"
    description="Score records are now system-generated outputs from the answer-sheet and grading flow. This page stays read-only so results cannot be fabricated by hand."
  >
    <section class="panel-card section-card">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="candidateName" label="Candidate" min-width="140" />
        <el-table-column prop="examName" label="Exam" min-width="220" />
        <el-table-column prop="paperName" label="Paper" min-width="220" />
        <el-table-column prop="submittedAt" label="Submitted At" min-width="180" />
        <el-table-column prop="objectiveScore" label="Objective" min-width="100" />
        <el-table-column prop="subjectiveScore" label="Subjective" min-width="100" />
        <el-table-column prop="finalScore" label="Final" min-width="90" />
        <el-table-column label="Pass" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.passedFlag === 1 ? 'success' : 'danger'">{{ row.passedFlag === 1 ? 'Pass' : 'Fail' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Status" min-width="140">
          <template #default="{ row }">
            <el-tag :type="row.publishedFlag === 1 ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}
</style>
