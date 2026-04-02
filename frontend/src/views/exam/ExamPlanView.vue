<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { createExamPlan, deleteExamPlan, fetchExamPlans, fetchPapers, updateExamPlan } from '../../api/exam'
import { fetchUsers, type SystemUser } from '../../api/system'
import type { ExamPaper, ExamPlan } from '../../types/exam'

const loading = ref(false)
const plans = ref<ExamPlan[]>([])
const papers = ref<ExamPaper[]>([])
const users = ref<SystemUser[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<Omit<ExamPlan, 'id' | 'candidateCount' | 'submittedCount' | 'paperName' | 'subject'>>({
  examCode: '',
  examName: '',
  paperId: 0,
  startTime: '',
  endTime: '',
  durationMinutes: 90,
  passScore: 60,
  candidateScope: 'ASSIGNED',
  attemptLimit: 1,
  examPassword: '',
  lateEntryMinutes: 15,
  earlySubmitMinutes: 0,
  autoSubmitEnabled: 1,
  antiCheatLevel: 'BASIC',
  instructionText: '',
  status: 1,
  publishStatus: 0,
  candidateUserIds: []
})

const rules: FormRules<typeof form> = {
  examCode: [{ required: true, message: '请输入考试编码', trigger: 'blur' }],
  examName: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const [planList, paperList, userList] = await Promise.all([fetchExamPlans(), fetchPapers(), fetchUsers()])
    plans.value = planList
    papers.value = paperList.filter((item) => item.publishStatus === 1)
    users.value = userList.filter((item) => item.roleCode === 'STUDENT')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    examCode: '',
    examName: '',
    paperId: 0,
    startTime: '',
    endTime: '',
    durationMinutes: 90,
    passScore: 60,
    candidateScope: 'ASSIGNED',
    attemptLimit: 1,
    examPassword: '',
    lateEntryMinutes: 15,
    earlySubmitMinutes: 0,
    autoSubmitEnabled: 1,
    antiCheatLevel: 'BASIC',
    instructionText: '',
    status: 1,
    publishStatus: 0,
    candidateUserIds: []
  })
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ExamPlan) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    examCode: row.examCode,
    examName: row.examName,
    paperId: row.paperId,
    startTime: row.startTime,
    endTime: row.endTime,
    durationMinutes: row.durationMinutes,
    passScore: row.passScore,
    candidateScope: row.candidateScope,
    attemptLimit: row.attemptLimit,
    examPassword: row.examPassword || '',
    lateEntryMinutes: row.lateEntryMinutes,
    earlySubmitMinutes: row.earlySubmitMinutes,
    autoSubmitEnabled: row.autoSubmitEnabled,
    antiCheatLevel: row.antiCheatLevel,
    instructionText: row.instructionText || '',
    status: row.status,
    publishStatus: row.publishStatus,
    candidateUserIds: [...row.candidateUserIds]
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (dialogMode.value === 'create') {
      await createExamPlan(form)
    } else if (editingId.value) {
      await updateExamPlan(editingId.value, form)
    }
    ElMessage.success(dialogMode.value === 'create' ? '考试计划已创建' : '考试计划已更新')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该考试计划？', '提示', { type: 'warning' })
  await deleteExamPlan(id)
  ElMessage.success('考试计划已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="考试发布"
    title="考试计划、时间窗口与考生分配"
    description="考试计划将试卷、考试时间、防作弊等级和考生名单组合成一条可执行发布记录，是考生进入考试前的统一控制入口。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate">新建考试计划</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="plans" v-loading="loading">
        <el-table-column prop="examCode" label="考试编码" min-width="130" />
        <el-table-column prop="examName" label="考试名称" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="220" />
        <el-table-column prop="startTime" label="开始时间" min-width="180" />
        <el-table-column prop="endTime" label="结束时间" min-width="180" />
        <el-table-column prop="candidateCount" label="考生数" min-width="100" />
        <el-table-column prop="submittedCount" label="已提交" min-width="100" />
        <el-table-column label="发布状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">{{ row.publishStatus === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeItem(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建考试计划' : '编辑考试计划'" width="min(920px, 96vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-three">
          <el-form-item label="考试编码" prop="examCode"><el-input v-model="form.examCode" /></el-form-item>
          <el-form-item label="考试名称" prop="examName"><el-input v-model="form.examName" /></el-form-item>
          <el-form-item label="试卷" prop="paperId"><el-select v-model="form.paperId"><el-option v-for="paper in papers" :key="paper.id" :label="paper.paperName" :value="paper.id" /></el-select></el-form-item>
          <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="考试时长（分钟）"><el-input-number v-model="form.durationMinutes" :min="1" /></el-form-item>
          <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="1" :max="100" /></el-form-item>
          <el-form-item label="参考次数"><el-input-number v-model="form.attemptLimit" :min="1" :max="5" /></el-form-item>
          <el-form-item label="考试口令"><el-input v-model="form.examPassword" placeholder="可选" /></el-form-item>
          <el-form-item label="防作弊等级"><el-select v-model="form.antiCheatLevel"><el-option label="基础" value="BASIC" /><el-option label="严格" value="STRICT" /></el-select></el-form-item>
          <el-form-item label="允许迟到分钟"><el-input-number v-model="form.lateEntryMinutes" :min="0" :max="120" /></el-form-item>
          <el-form-item label="发布状态"><el-select v-model="form.publishStatus"><el-option :value="0" label="草稿" /><el-option :value="1" label="已发布" /></el-select></el-form-item>
        </div>
        <el-form-item label="考试说明"><el-input v-model="form.instructionText" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="考生名单">
          <el-select v-model="form.candidateUserIds" multiple filterable collapse-tags>
            <el-option v-for="user in users" :key="user.id" :label="user.fullName || user.nickname" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.hero-actions {
  margin-top: 1rem;
}

.section-card {
  padding: 1rem;
}

.grid-three {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

@media (max-width: 980px) {
  .grid-three {
    grid-template-columns: 1fr;
  }
}
</style>
