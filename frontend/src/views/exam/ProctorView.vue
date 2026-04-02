<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchProctorEvents } from '../../api/exam'
import type { AntiCheatEvent } from '../../types/exam'
import { labelEventType, labelSeverity } from '../../utils/labels'

const loading = ref(false)
const events = ref<AntiCheatEvent[]>([])

async function loadData() {
  loading.value = true
  try {
    events.value = await fetchProctorEvents()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="监考事件"
    title="查看考试过程中的监考与防作弊事件"
    description="当前页面展示考生作答期间产生的基础监考事件，包括切屏、失焦、退出全屏等行为留痕。"
  >
    <section class="panel-card section-card">
      <el-table :data="events" v-loading="loading">
        <el-table-column prop="occurredAt" label="发生时间" min-width="180" />
        <el-table-column prop="examPlanId" label="考试计划" min-width="100" />
        <el-table-column prop="userId" label="用户ID" min-width="90" />
        <el-table-column label="事件类型" min-width="160"><template #default="{ row }">{{ labelEventType(row.eventType) }}</template></el-table-column>
        <el-table-column label="严重级别" min-width="110"><template #default="{ row }">{{ labelSeverity(row.severity) }}</template></el-table-column>
        <el-table-column prop="detailText" label="详情" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}
</style>
