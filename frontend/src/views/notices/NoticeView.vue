<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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

const rules: FormRules<typeof form> = {
  title: [{ required: true, message: 'Please enter the title', trigger: 'blur' }],
  category: [{ required: true, message: 'Please enter the category', trigger: 'blur' }],
  status: [{ required: true, message: 'Please select the status', trigger: 'change' }],
  content: [{ required: true, message: 'Please enter the content', trigger: 'blur' }]
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
      ElMessage.success('Notice created')
    } else if (currentId.value) {
      await updateNotice(currentId.value, form)
      ElMessage.success('Notice updated')
    }
    dialogVisible.value = false
    await loadPage()
  })
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('Delete this notice record?', 'Confirm', { type: 'warning' })
  await deleteNotice(id)
  ElMessage.success('Notice deleted')
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
    eyebrow="Representative CRUD"
    title="Notice management baseline"
    description="This is the current example CRUD module. Future templates can swap the business meaning while preserving the same search-table-dialog rhythm and backend response shape."
  >
    <section class="panel-card filter-card">
      <div class="filter-grid">
        <el-input v-model="filters.title" placeholder="Search by title" clearable />
        <el-select v-model="filters.status" placeholder="Status" clearable>
          <el-option :value="1" label="Published" />
          <el-option :value="0" label="Draft" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="handleSearch">Search</el-button>
          <el-button @click="handleReset">Reset</el-button>
          <el-button type="success" @click="openCreateDialog">New Notice</el-button>
        </div>
      </div>
    </section>

    <section class="panel-card table-card">
      <el-table :data="pageState.records" v-loading="loading">
        <el-table-column prop="title" label="Title" min-width="220" />
        <el-table-column prop="category" label="Category" min-width="140" />
        <el-table-column label="Status" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Published' : 'Draft' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="Updated At" min-width="180" />
        <el-table-column label="Actions" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row.id)">Edit</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">Delete</el-button>
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
      :title="dialogMode === 'create' ? 'Create Notice' : 'Edit Notice'"
      width="min(680px, 92vw)"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Title" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="Category" prop="category">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="Status" prop="status">
          <el-select v-model="form.status">
            <el-option :value="1" label="Published" />
            <el-option :value="0" label="Draft" />
          </el-select>
        </el-form-item>
        <el-form-item label="Content" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submitForm">Save</el-button>
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
