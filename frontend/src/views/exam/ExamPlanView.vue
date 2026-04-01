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
  examCode: [{ required: true, message: 'Please enter the exam code', trigger: 'blur' }],
  examName: [{ required: true, message: 'Please enter the exam name', trigger: 'blur' }],
  paperId: [{ required: true, message: 'Please select a paper', trigger: 'change' }],
  startTime: [{ required: true, message: 'Please select the start time', trigger: 'change' }],
  endTime: [{ required: true, message: 'Please select the end time', trigger: 'change' }]
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
    ElMessage.success(dialogMode.value === 'create' ? 'Exam plan created' : 'Exam plan updated')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('Delete this exam plan?', 'Confirm', { type: 'warning' })
  await deleteExamPlan(id)
  ElMessage.success('Exam plan deleted')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="Exam Release"
    title="Publish time windows, papers, and candidate rosters"
    description="Exam plans bind a published paper, time window, anti-cheat posture, and candidate roster into one operational release unit. This is the control point before students can enter the assessment flow."
  >
    <template #actions>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate">New Exam Plan</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="plans" v-loading="loading">
        <el-table-column prop="examCode" label="Code" min-width="130" />
        <el-table-column prop="examName" label="Exam Name" min-width="220" />
        <el-table-column prop="paperName" label="Paper" min-width="220" />
        <el-table-column prop="startTime" label="Start" min-width="180" />
        <el-table-column prop="endTime" label="End" min-width="180" />
        <el-table-column prop="candidateCount" label="Candidates" min-width="100" />
        <el-table-column prop="submittedCount" label="Submitted" min-width="100" />
        <el-table-column label="Status" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">{{ row.publishStatus === 1 ? 'Published' : 'Draft' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="removeItem(row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? 'Create Exam Plan' : 'Edit Exam Plan'" width="min(920px, 96vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-three">
          <el-form-item label="Exam Code" prop="examCode"><el-input v-model="form.examCode" /></el-form-item>
          <el-form-item label="Exam Name" prop="examName"><el-input v-model="form.examName" /></el-form-item>
          <el-form-item label="Paper" prop="paperId"><el-select v-model="form.paperId"><el-option v-for="paper in papers" :key="paper.id" :label="paper.paperName" :value="paper.id" /></el-select></el-form-item>
          <el-form-item label="Start Time" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="End Time" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="Duration Minutes"><el-input-number v-model="form.durationMinutes" :min="1" /></el-form-item>
          <el-form-item label="Pass Score"><el-input-number v-model="form.passScore" :min="1" :max="100" /></el-form-item>
          <el-form-item label="Attempt Limit"><el-input-number v-model="form.attemptLimit" :min="1" :max="5" /></el-form-item>
          <el-form-item label="Exam Password"><el-input v-model="form.examPassword" placeholder="Optional" /></el-form-item>
          <el-form-item label="Anti-Cheat Level"><el-select v-model="form.antiCheatLevel"><el-option label="Basic" value="BASIC" /><el-option label="Strict" value="STRICT" /></el-select></el-form-item>
          <el-form-item label="Late Entry Minutes"><el-input-number v-model="form.lateEntryMinutes" :min="0" :max="120" /></el-form-item>
          <el-form-item label="Publish Status"><el-select v-model="form.publishStatus"><el-option :value="0" label="Draft" /><el-option :value="1" label="Published" /></el-select></el-form-item>
        </div>
        <el-form-item label="Instructions"><el-input v-model="form.instructionText" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="Candidate Roster">
          <el-select v-model="form.candidateUserIds" multiple filterable collapse-tags>
            <el-option v-for="user in users" :key="user.id" :label="user.fullName || user.nickname" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submit">Save</el-button>
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
