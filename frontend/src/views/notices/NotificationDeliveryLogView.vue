<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import {
  dispatchUpcomingExamReminders,
  fetchNotificationDeliveryLogs,
  type NotificationDeliveryLog,
  type NotificationDeliveryLogQuery
} from '../../api/notification'
import { formatDateTime } from '../../utils/datetime'
import {
  labelNotificationBusinessType,
  labelNotificationChannelType,
  labelNotificationDeliveryStatus
} from '../../utils/labels'

const loading = ref(false)
const dispatching = ref(false)
const pageState = reactive({
  records: [] as NotificationDeliveryLog[],
  total: 0
})

const filters = reactive<NotificationDeliveryLogQuery>({
  pageNum: 1,
  pageSize: 10,
  businessType: '',
  channelType: '',
  deliveryStatus: '',
  recipientUserId: ''
})

const businessTypeOptions = [
  { value: 'EXAM_PUBLISH', label: '考试发布' },
  { value: 'EXAM_REMINDER', label: '开考前提醒' },
  { value: 'SCORE_PUBLISH', label: '成绩发布' },
  { value: 'SCORE_APPEAL', label: '成绩申诉' },
  { value: 'SCORE_APPEAL_RESULT', label: '申诉结果' },
  { value: 'SECURITY_ALERT', label: '安全告警' }
]

const channelTypeOptions = [
  { value: 'IN_APP', label: '站内消息' },
  { value: 'MOCK_SMS', label: 'Mock 短信' }
]

const statusOptions = [
  { value: 'DELIVERED', label: '已投递' },
  { value: 'SKIPPED', label: '已跳过' },
  { value: 'FAILED', label: '失败' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await fetchNotificationDeliveryLogs(filters)
    pageState.records = result.records
    pageState.total = result.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filters.pageNum = 1
  filters.businessType = ''
  filters.channelType = ''
  filters.deliveryStatus = ''
  filters.recipientUserId = ''
  loadData()
}

function handlePageChange(pageNum: number) {
  filters.pageNum = pageNum
  loadData()
}

async function handleDispatchReminders() {
  dispatching.value = true
  try {
    const count = await dispatchUpcomingExamReminders()
    ElMessage.success(count > 0 ? `已触发 ${count} 条开考前提醒投递` : '当前没有需要投递的开考前提醒')
    await loadData()
  } finally {
    dispatching.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="通知投递日志"
    title="站内消息与 Mock 短信投递留痕"
    description="查看通知模板渲染后的投递留痕，并可手动触发开考前提醒扫描，核对通知链路的实际结果。"
  >
    <section class="panel-card filter-card">
      <div class="filter-grid">
        <el-select v-model="filters.businessType" placeholder="业务类型" clearable>
          <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.channelType" placeholder="投递通道" clearable>
          <el-option v-for="item in channelTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.deliveryStatus" placeholder="投递状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="filters.recipientUserId" placeholder="按接收人 ID 查询" clearable />
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button :loading="dispatching" type="success" @click="handleDispatchReminders">立即扫描开考前提醒</el-button>
      </div>
    </section>

    <section class="panel-card table-card">
      <el-table :data="pageState.records" v-loading="loading">
        <el-table-column label="投递时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.sentAt) }}</template>
        </el-table-column>
        <el-table-column label="业务类型" min-width="140">
          <template #default="{ row }">{{ labelNotificationBusinessType(row.businessType) }}</template>
        </el-table-column>
        <el-table-column label="投递通道" min-width="120">
          <template #default="{ row }">{{ labelNotificationChannelType(row.channelType) }}</template>
        </el-table-column>
        <el-table-column prop="templateCode" label="模板编码" min-width="180" />
        <el-table-column prop="recipientName" label="接收人" min-width="120" />
        <el-table-column prop="recipientTarget" label="投递目标" min-width="140" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.deliveryStatus === 'DELIVERED' ? 'success' : row.deliveryStatus === 'SKIPPED' ? 'warning' : 'danger'">
              {{ labelNotificationDeliveryStatus(row.deliveryStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="providerTrace" label="通道回执" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :page-size="filters.pageSize"
          :current-page="filters.pageNum"
          :total="pageState.total"
          @current-change="handlePageChange"
        />
      </div>
    </section>
  </AppShellSection>
</template>

<style scoped>
.filter-card,
.table-card {
  padding: 1rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.filter-actions {
  display: flex;
  gap: 0.6rem;
  justify-content: flex-end;
  margin-top: 0.9rem;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
