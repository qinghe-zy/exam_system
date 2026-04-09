<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchLoginRiskLogs, type LoginRiskLogRecord } from '../../api/system'
import { formatDateTime } from '../../utils/datetime'
import { labelSeverity } from '../../utils/labels'

const loading = ref(false)
const logs = ref<LoginRiskLogRecord[]>([])
const filters = reactive({
  riskLevel: '',
  successFlag: ''
})

const filteredLogs = computed(() => {
  return logs.value.filter((item) => {
    if (filters.riskLevel && item.riskLevel !== filters.riskLevel) return false
    if (filters.successFlag && String(item.successFlag) !== filters.successFlag) return false
    return true
  })
})

const summary = computed(() => ({
  total: logs.value.length,
  failed: logs.value.filter((item) => item.successFlag !== 1).length,
  mediumOrAbove: logs.value.filter((item) => ['MEDIUM', 'HIGH'].includes(item.riskLevel)).length
}))

async function loadData() {
  loading.value = true
  try {
    logs.value = await fetchLoginRiskLogs()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="系统管理"
    title="登录风险记录"
    description="查看登录阶段的基础风险留痕，包括失败登录、设备变化和 IP 变化等可解释信息，为后续风控增强提供依据。"
  >
    <section class="summary-row">
      <div class="summary-card panel-card">
        <strong>{{ summary.total }}</strong>
        <span>记录总数</span>
      </div>
      <div class="summary-card panel-card">
        <strong>{{ summary.failed }}</strong>
        <span>失败登录</span>
      </div>
      <div class="summary-card panel-card">
        <strong>{{ summary.mediumOrAbove }}</strong>
        <span>中高风险</span>
      </div>
    </section>

    <section class="panel-card table-card">
      <div class="filter-row">
        <el-select v-model="filters.riskLevel" clearable placeholder="按风险级别筛选">
          <el-option label="低" value="LOW" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="高" value="HIGH" />
        </el-select>
        <el-select v-model="filters.successFlag" clearable placeholder="按登录结果筛选">
          <el-option label="成功" value="1" />
          <el-option label="失败" value="0" />
        </el-select>
      </div>

      <el-table :data="filteredLogs" v-loading="loading">
        <el-table-column label="登录时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.loginAt) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="roleCode" label="角色" min-width="110" />
        <el-table-column label="结果" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.successFlag === 1 ? 'success' : 'danger'">{{ row.successFlag === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险级别" min-width="110">
          <template #default="{ row }">{{ labelSeverity(row.riskLevel) }}</template>
        </el-table-column>
        <el-table-column prop="clientIp" label="IP" min-width="120" />
        <el-table-column prop="deviceFingerprint" label="设备指纹" min-width="150" show-overflow-tooltip />
        <el-table-column prop="riskReason" label="风险说明" min-width="240" show-overflow-tooltip />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.summary-card,
.table-card {
  padding: 1rem;
}

.summary-card strong {
  display: block;
  font-size: 1.4rem;
  color: var(--brand-deep);
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 16rem));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

@media (max-width: 920px) {
  .summary-row,
  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
