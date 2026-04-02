<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchAuditLogs, type AuditLogRecord } from '../../api/system'

const loading = ref(false)
const logs = ref<AuditLogRecord[]>([])

async function loadData() {
  loading.value = true
  try {
    logs.value = await fetchAuditLogs()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="系统管理"
    title="审计日志"
    description="该页面用于查看关键业务操作留痕，包括题目、考试、作答与阅卷等链路中的关键写操作记录。"
  >
    <section class="panel-card table-card">
      <el-table :data="logs" v-loading="loading">
        <el-table-column prop="createTime" label="时间" min-width="180" />
        <el-table-column prop="operatorName" label="操作人" min-width="140" />
        <el-table-column prop="moduleName" label="模块" min-width="140" />
        <el-table-column prop="actionName" label="动作" min-width="140" />
        <el-table-column prop="targetType" label="目标类型" min-width="140" />
        <el-table-column prop="targetId" label="目标 ID" min-width="100" />
        <el-table-column prop="detailText" label="详情" min-width="320" show-overflow-tooltip />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.table-card {
  padding: 1rem;
}
</style>
