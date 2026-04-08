<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchProctorEvents } from '../../api/exam'
import type { AntiCheatEvent } from '../../types/exam'
import { labelEventType, labelSeverity } from '../../utils/labels'

const loading = ref(false)
const events = ref<AntiCheatEvent[]>([])
const filters = reactive({
  severity: '',
  keyword: ''
})

const filteredEvents = computed(() => {
  return events.value.filter((event) => {
    if (filters.severity && event.severity !== filters.severity) return false
    if (filters.keyword) {
      const keyword = filters.keyword.trim().toLowerCase()
      const haystack = `${event.examName || ''} ${event.candidateName || ''} ${event.detailText || ''}`.toLowerCase()
      if (!haystack.includes(keyword)) return false
    }
    return true
  })
})

const summary = computed(() => ({
  total: events.value.length,
  high: events.value.filter((item) => item.severity === 'HIGH').length,
  autoSaved: events.value.filter((item) => item.triggeredAutoSave === 1).length,
  deviceLogged: events.value.filter((item) => Boolean(item.deviceFingerprint)).length
}))

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
    title="基础防作弊事件已形成“触发-保存-落库-可查”闭环"
    description="这里可以查看考试过程中的切屏、失焦、退出全屏、复制粘贴拦截、右键拦截、高风险快捷键拦截以及设备上下文记录，用于老师、监考员和管理员快速核查本场考试风险。"
  >
    <section class="summary-row">
      <div class="summary-card panel-card">
        <strong>{{ summary.total }}</strong>
        <span>事件总数</span>
      </div>
      <div class="summary-card panel-card">
        <strong>{{ summary.high }}</strong>
        <span>高风险事件</span>
      </div>
      <div class="summary-card panel-card">
        <strong>{{ summary.autoSaved }}</strong>
        <span>已联动自动保存</span>
      </div>
      <div class="summary-card panel-card">
        <strong>{{ summary.deviceLogged }}</strong>
        <span>含设备上下文</span>
      </div>
    </section>

    <section class="panel-card section-card">
      <div class="filter-row">
        <el-select v-model="filters.severity" clearable placeholder="按严重级别筛选">
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
        <el-input v-model="filters.keyword" clearable placeholder="按考试名称、考生姓名或详情搜索" />
      </div>

      <el-table :data="filteredEvents" v-loading="loading">
        <el-table-column prop="occurredAt" label="发生时间" min-width="180" />
        <el-table-column prop="examName" label="考试" min-width="220" />
        <el-table-column prop="candidateName" label="考生" min-width="120" />
        <el-table-column label="事件类型" min-width="160">
          <template #default="{ row }">{{ labelEventType(row.eventType) }}</template>
        </el-table-column>
        <el-table-column label="严重级别" min-width="110">
          <template #default="{ row }">{{ labelSeverity(row.severity) }}</template>
        </el-table-column>
        <el-table-column prop="leaveCount" label="累计次数" min-width="90" />
        <el-table-column label="自动保存" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.triggeredAutoSave === 1 ? 'success' : 'info'">{{ row.triggeredAutoSave === 1 ? '已触发' : '未触发' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientIp" label="IP" min-width="120" />
        <el-table-column prop="deviceFingerprint" label="设备指纹" min-width="150" show-overflow-tooltip />
        <el-table-column prop="saveVersion" label="答卷版本" min-width="100" />
        <el-table-column prop="deviceInfo" label="设备摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="detailText" label="详情" min-width="320" show-overflow-tooltip />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.summary-card {
  padding: 1rem;
  display: grid;
  gap: 0.35rem;
}

.summary-card strong {
  font-size: 1.5rem;
  color: var(--brand-deep);
}

.section-card {
  padding: 1rem;
}

.filter-row {
  display: grid;
  grid-template-columns: 14rem minmax(0, 1fr);
  gap: 0.8rem;
  margin-bottom: 1rem;
}

@media (max-width: 980px) {
  .summary-row,
  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
