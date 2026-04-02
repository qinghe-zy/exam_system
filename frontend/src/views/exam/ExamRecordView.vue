<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchExamRecords } from '../../api/exam'
import type { ExamRecord } from '../../types/exam'
import { labelAnswerSheetStatus } from '../../utils/labels'

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
    eyebrow="成绩中心"
    title="成绩记录与发布状态"
    description="成绩记录由答卷和阅卷流程自动生成，页面默认只读，用于查看考生成绩、发布状态和通过情况。"
  >
    <section class="panel-card section-card">
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="candidateName" label="考生" min-width="140" />
        <el-table-column prop="examName" label="考试" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="220" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
        <el-table-column prop="objectiveScore" label="客观分" min-width="100" />
        <el-table-column prop="subjectiveScore" label="主观分" min-width="100" />
        <el-table-column prop="finalScore" label="总分" min-width="90" />
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
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}
</style>
