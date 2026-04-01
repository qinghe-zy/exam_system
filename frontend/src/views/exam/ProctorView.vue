<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchProctorEvents } from '../../api/exam'
import type { AntiCheatEvent } from '../../types/exam'

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
    eyebrow="Proctor View"
    title="Monitor recorded anti-cheat events"
    description="This screen surfaces the baseline telemetry captured from candidate sessions, including tab switches, blur events, and fullscreen exits."
  >
    <section class="panel-card section-card">
      <el-table :data="events" v-loading="loading">
        <el-table-column prop="occurredAt" label="Occurred At" min-width="180" />
        <el-table-column prop="examPlanId" label="Exam Plan" min-width="100" />
        <el-table-column prop="userId" label="User" min-width="90" />
        <el-table-column prop="eventType" label="Event Type" min-width="160" />
        <el-table-column prop="severity" label="Severity" min-width="110" />
        <el-table-column prop="detailText" label="Detail" min-width="260" show-overflow-tooltip />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}
</style>
