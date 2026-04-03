<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createNotice,
  deleteNotice,
  fetchNotice,
  fetchNoticePage,
  updateNotice,
  type Notice,
  type NoticePayload
} from '../../api/notice'
import AppShellSection from '../../components/AppShellSection.vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const filters = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  status: '' as number | ''
})

const pageState = reactive({
  records: [] as Notice[],
  total: 0
})

const form = reactive<NoticePayload>({
  title: '',
  category: '',
  status: 1,
  content: ''
})

const canManageNotice = computed(() => ['ADMIN', 'ORG_ADMIN', 'TEACHER'].includes(authStore.currentUser?.roleCode || ''))

const rules: FormRules<typeof form> = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

async function loadPage() {
  loading.value = true
  try {
    const result = await fetchNoticePage(filters)
    pageState.records = result.records
    pageState.total = result.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.category = ''
  form.status = 1
  form.content = ''
  currentId.value = null
}

function openCreateDialog() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(id: number) {
  dialogMode.value = 'edit'
  currentId.value = id
  const detail = await fetchNotice(id)
  form.title = detail.title
  form.category = detail.category
  form.status = detail.status
  form.content = detail.content
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (dialogMode.value === 'create') {
      await createNotice(form)
      ElMessage.success('公告已创建')
    } else if (currentId.value) {
      await updateNotice(currentId.value, form)
      ElMessage.success('公告已更新')
    }
    dialogVisible.value = false
    await loadPage()
  })
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该公告？', '提示', { type: 'warning' })
  await deleteNotice(id)
  ElMessage.success('公告已删除')
  await loadPage()
}

function handleSearch() {
  filters.pageNum = 1
  loadPage()
}

function handleReset() {
  filters.title = ''
  filters.status = ''
  filters.pageNum = 1
  loadPage()
}

function handlePageChange(pageNum: number) {
  filters.pageNum = pageNum
  loadPage()
}

onMounted(loadPage)
</script>

<template>
  <AppShellSection
    eyebrow="公告管理"
    title="考试通知与成绩说明公告"
    description="公告页用于维护考试发布通知、阅卷安排说明、成绩发布说明等站内公告，是通知与流程协同的基础能力之一。"
  >
    <section class="panel-card filter-card">
      <div class="filter-grid">
        <el-input v-model="filters.title" placeholder="按标题搜索" clearable />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option :value="1" label="已发布" />
          <el-option :value="0" label="草稿" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button v-if="canManageNotice" type="success" @click="openCreateDialog">新建公告</el-button>
        </div>
      </div>
    </section>

    <section class="panel-card table-card">
      <el-table :data="pageState.records" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="category" label="分类" min-width="140" />
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="180" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="canManageNotice">
              <el-button link type="primary" @click="openEditDialog(row.id)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
            </template>
            <span v-else class="muted">仅查看</span>
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建公告' : '编辑公告'"
      width="min(680px, 92vw)"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option :value="1" label="已发布" />
            <el-option :value="0" label="草稿" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" />
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
.filter-card,
.table-card {
  padding: 1rem;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(180px, 240px) auto;
  gap: 0.8rem;
}

.filter-actions {
  display: flex;
  gap: 0.6rem;
  justify-content: flex-end;
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
