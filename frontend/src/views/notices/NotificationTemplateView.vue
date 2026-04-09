<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import {
  createNotificationTemplate,
  deleteNotificationTemplate,
  fetchNotificationTemplates,
  updateNotificationTemplate,
  type NotificationTemplate,
  type NotificationTemplatePayload
} from '../../api/notification'
import { useAuthStore } from '../../stores/auth'
import { formatDateTime } from '../../utils/datetime'
import { labelNotificationBusinessType, labelNotificationChannelType } from '../../utils/labels'

const authStore = useAuthStore()
const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const templates = ref<NotificationTemplate[]>([])

const canManageTemplate = computed(() => ['ADMIN', 'ORG_ADMIN', 'TEACHER'].includes(authStore.currentUser?.roleCode || ''))
const currentOrgId = computed(() => authStore.currentUser?.organizationId)

function canEditTemplate(item: NotificationTemplate) {
  if (!canManageTemplate.value) return false
  if (authStore.currentUser?.roleCode === 'ADMIN') return true
  return item.organizationId === currentOrgId.value
}

const form = reactive<NotificationTemplatePayload>({
  templateCode: '',
  templateName: '',
  businessType: 'EXAM_PUBLISH',
  channelType: 'IN_APP',
  titleTemplate: '',
  contentTemplate: '',
  status: 1
})

const rules: FormRules<typeof form> = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  channelType: [{ required: true, message: '请选择投递通道', trigger: 'change' }],
  titleTemplate: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
  contentTemplate: [{ required: true, message: '请输入内容模板', trigger: 'blur' }]
}

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

async function loadTemplates() {
  loading.value = true
  try {
    templates.value = await fetchNotificationTemplates()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.templateCode = ''
  form.templateName = ''
  form.businessType = 'EXAM_PUBLISH'
  form.channelType = 'IN_APP'
  form.titleTemplate = ''
  form.contentTemplate = ''
  form.status = 1
  currentId.value = null
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(item: NotificationTemplate) {
  dialogMode.value = 'edit'
  currentId.value = item.id
  form.templateCode = item.templateCode
  form.templateName = item.templateName
  form.businessType = item.businessType
  form.channelType = item.channelType
  form.titleTemplate = item.titleTemplate
  form.contentTemplate = item.contentTemplate
  form.status = item.status
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (dialogMode.value === 'create') {
      await createNotificationTemplate(form)
      ElMessage.success('通知模板已创建')
    } else if (currentId.value) {
      await updateNotificationTemplate(currentId.value, form)
      ElMessage.success('通知模板已更新')
    }
    dialogVisible.value = false
    await loadTemplates()
  })
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该通知模板？', '提示', { type: 'warning' })
  await deleteNotificationTemplate(id)
  ElMessage.success('通知模板已删除')
  await loadTemplates()
}

onMounted(loadTemplates)
</script>

<template>
  <AppShellSection
    eyebrow="通知模板"
    title="通知模板与变量管理"
    description="维护考试发布、开考前提醒、成绩发布、申诉结果和安全告警模板，统一控制站内消息与外部提醒内容。"
  >
    <section class="panel-card intro-card">
      <div class="intro-grid">
        <article>
          <h3>变量说明</h3>
          <p>通用变量包含 <code>{{ '{title}' }}</code>、<code>{{ '{content}' }}</code>；考试类模板常用 <code>{{ '{examName}' }}</code>、<code>{{ '{startTime}' }}</code>、<code>{{ '{leadMinutes}' }}</code>、<code>{{ '{candidateName}' }}</code>。</p>
        </article>
        <article>
          <h3>投递策略</h3>
          <p>站内消息通过消息中心查看；外部通道当前使用可运行的 <code>Mock 短信</code> 投递日志，保留完整的渲染与留痕流程。</p>
        </article>
      </div>
      <div class="intro-actions">
        <el-button v-if="canManageTemplate" type="primary" @click="openCreateDialog">新建模板</el-button>
      </div>
    </section>

    <section class="panel-card table-card">
      <el-table :data="templates" v-loading="loading">
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="templateCode" label="模板编码" min-width="180" />
        <el-table-column label="业务类型" min-width="140">
          <template #default="{ row }">{{ labelNotificationBusinessType(row.businessType) }}</template>
        </el-table-column>
        <el-table-column label="投递通道" min-width="120">
          <template #default="{ row }">{{ labelNotificationChannelType(row.channelType) }}</template>
        </el-table-column>
        <el-table-column prop="titleTemplate" label="标题模板" min-width="240" show-overflow-tooltip />
        <el-table-column prop="contentTemplate" label="内容模板" min-width="360" show-overflow-tooltip />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="canEditTemplate(row)">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
            </template>
            <span v-else class="muted">仅查看</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建通知模板' : '编辑通知模板'"
      width="min(760px, 94vw)"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="模板编码" prop="templateCode">
            <el-input v-model="form.templateCode" placeholder="例如 EXAM_REMINDER_SMS" />
          </el-form-item>
          <el-form-item label="模板名称" prop="templateName">
            <el-input v-model="form.templateName" placeholder="请输入中文名称" />
          </el-form-item>
          <el-form-item label="业务类型" prop="businessType">
            <el-select v-model="form.businessType">
              <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="投递通道" prop="channelType">
            <el-select v-model="form.channelType">
              <el-option v-for="item in channelTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="标题模板" prop="titleTemplate">
          <el-input v-model="form.titleTemplate" placeholder="短信模板也建议保留标题，便于投递日志检索" />
        </el-form-item>
        <el-form-item label="内容模板" prop="contentTemplate">
          <el-input v-model="form.contentTemplate" type="textarea" :rows="6" placeholder="可使用 {{examName}}、{{startTime}}、{{candidateName}} 等变量" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.intro-card,
.table-card {
  padding: 1rem;
}

.intro-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.intro-grid h3 {
  margin: 0 0 0.45rem;
}

.intro-grid p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.intro-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 1rem;
}

@media (max-width: 980px) {
  .intro-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .intro-actions {
    justify-content: flex-start;
  }
}
</style>
