<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchMyMessages, markMessageRead, type InAppMessage } from '../../api/message'
import { labelMessageType } from '../../utils/labels'

const router = useRouter()
const loading = ref(false)
const messages = ref<InAppMessage[]>([])

async function loadData() {
  loading.value = true
  try {
    messages.value = await fetchMyMessages()
  } finally {
    loading.value = false
  }
}

async function markRead(id: number) {
  await markMessageRead(id)
  ElMessage.success('消息已标记为已读')
  await loadData()
}

async function openRelated(message: InAppMessage) {
  if (message.readFlag !== 1) {
    await markRead(message.id)
  }
  if (message.relatedType === 'SCORE_RECORD' && message.relatedId) {
    await router.push({ path: '/candidate/scores', query: { recordId: String(message.relatedId) } })
    return
  }
  if (message.relatedType === 'EXAM_PLAN') {
    await router.push('/candidate/exams')
    return
  }
  ElMessage.info('当前消息没有可跳转的详情页面')
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="消息中心"
    title="站内消息与流程提醒"
    description="该页面展示考试发布提醒、成绩发布提醒等站内消息。当前版本先实现站内消息链路，邮件、短信、企业微信等外部通道仍为扩展位。"
  >
    <section class="panel-card section-card">
      <el-table :data="messages" v-loading="loading">
        <el-table-column prop="createTime" label="时间" min-width="180" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="类型" min-width="140"><template #default="{ row }">{{ labelMessageType(row.messageType) }}</template></el-table-column>
        <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.readFlag === 1 ? 'success' : 'warning'">{{ row.readFlag === 1 ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.readFlag !== 1" link type="primary" @click="markRead(row.id)">标记已读</el-button>
            <el-button link type="success" @click="openRelated(row)">查看详情</el-button>
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
