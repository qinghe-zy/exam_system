<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { DIFFICULTY_OPTIONS, QUESTION_TYPE_OPTIONS, REVIEW_STATUS_OPTIONS } from '../../constants/exam'
import { createQuestion, deleteQuestion, exportQuestions, fetchQuestions, importQuestions, updateQuestion } from '../../api/exam'
import type { QuestionBank } from '../../types/exam'

const loading = ref(false)
const questions = ref<QuestionBank[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const importDialogVisible = ref(false)
const importText = ref('')

const form = reactive<Omit<QuestionBank, 'id'>>({
  questionCode: '',
  subject: '',
  questionType: 'SINGLE_CHOICE',
  difficultyLevel: 'MEDIUM',
  stem: '',
  optionsJson: '[]',
  answerKey: '',
  analysisText: '',
  knowledgePoint: '',
  chapterName: '',
  sourceName: '',
  tags: '',
  defaultScore: 10,
  reviewerStatus: 'DRAFT',
  versionNo: 1,
  status: 1
})

const rules: FormRules<typeof form> = {
  questionCode: [{ required: true, message: 'Please enter the question code', trigger: 'blur' }],
  subject: [{ required: true, message: 'Please enter the subject', trigger: 'blur' }],
  stem: [{ required: true, message: 'Please enter the question stem', trigger: 'blur' }],
  answerKey: [{ required: true, message: 'Please enter the answer key', trigger: 'blur' }]
}

const objectiveHint = computed(() => {
  if (form.questionType === 'MULTIPLE_CHOICE') return 'Use "|" to separate answers, for example A|C|D'
  if (form.questionType === 'TRUE_FALSE') return 'Use True or False'
  return 'Use the exact expected answer text'
})

async function loadData() {
  loading.value = true
  try {
    questions.value = await fetchQuestions()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    questionCode: '',
    subject: '',
    questionType: 'SINGLE_CHOICE',
    difficultyLevel: 'MEDIUM',
    stem: '',
    optionsJson: '[]',
    answerKey: '',
    analysisText: '',
    knowledgePoint: '',
    chapterName: '',
    sourceName: '',
    tags: '',
    defaultScore: 10,
    reviewerStatus: 'DRAFT',
    versionNo: 1,
    status: 1
  })
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: QuestionBank) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (dialogMode.value === 'create') {
      await createQuestion(form)
    } else if (editingId.value) {
      await updateQuestion(editingId.value, form)
    }
    ElMessage.success(dialogMode.value === 'create' ? 'Question created' : 'Question updated')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('Delete this question?', 'Confirm', { type: 'warning' })
  await deleteQuestion(id)
  ElMessage.success('Question deleted')
  await loadData()
}

async function handleExport() {
  const payload = await exportQuestions()
  await navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
  ElMessage.success('题库 JSON 已复制到剪贴板')
}

async function handleImport() {
  const questions = JSON.parse(importText.value)
  await importQuestions({ questions })
  ElMessage.success('题目导入完成')
  importDialogVisible.value = false
  importText.value = ''
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="Item Bank"
    title="Question bank with review, scoring, and knowledge metadata"
    description="This page now tracks more than stems and answers. It carries scoring defaults, review status, option payloads, source metadata, and question codes so downstream paper assembly and grading remain explainable."
  >
    <template #actions>
      <div class="hero-actions">
        <el-button @click="handleExport">导出 JSON</el-button>
        <el-button @click="importDialogVisible = true">导入 JSON</el-button>
        <el-button type="primary" @click="openCreate">New Question</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="questions" v-loading="loading">
        <el-table-column prop="questionCode" label="Code" min-width="140" />
        <el-table-column prop="subject" label="Subject" min-width="120" />
        <el-table-column prop="questionType" label="Type" min-width="140" />
        <el-table-column prop="difficultyLevel" label="Difficulty" min-width="120" />
        <el-table-column prop="knowledgePoint" label="Knowledge" min-width="150" show-overflow-tooltip />
        <el-table-column prop="defaultScore" label="Score" min-width="90" />
        <el-table-column prop="reviewerStatus" label="Review" min-width="120" />
        <el-table-column prop="stem" label="Stem" min-width="280" show-overflow-tooltip />
        <el-table-column label="Actions" min-width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="removeItem(row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? 'Create Question' : 'Edit Question'" width="min(960px, 96vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-three">
          <el-form-item label="Question Code" prop="questionCode"><el-input v-model="form.questionCode" /></el-form-item>
          <el-form-item label="Subject" prop="subject"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="Question Type"><el-select v-model="form.questionType"><el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="Difficulty"><el-select v-model="form.difficultyLevel"><el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="Default Score"><el-input-number v-model="form.defaultScore" :min="1" :max="100" /></el-form-item>
          <el-form-item label="Review Status"><el-select v-model="form.reviewerStatus"><el-option v-for="item in REVIEW_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="Knowledge Point"><el-input v-model="form.knowledgePoint" /></el-form-item>
          <el-form-item label="Chapter"><el-input v-model="form.chapterName" /></el-form-item>
          <el-form-item label="Source"><el-input v-model="form.sourceName" /></el-form-item>
        </div>
        <el-form-item label="Tags"><el-input v-model="form.tags" placeholder="architecture,module-boundary" /></el-form-item>
        <el-form-item label="Question Stem" prop="stem"><el-input v-model="form.stem" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="Options JSON">
          <el-input v-model="form.optionsJson" type="textarea" :rows="3" placeholder='["Option A","Option B"]' />
        </el-form-item>
        <el-form-item :label="`Answer Key · ${objectiveHint}`" prop="answerKey"><el-input v-model="form.answerKey" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="Analysis"><el-input v-model="form.analysisText" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submit">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入题目 JSON" width="min(900px, 96vw)">
      <p class="muted">请粘贴题目数组 JSON，字段结构与导出结果一致。</p>
      <el-input v-model="importText" type="textarea" :rows="12" />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport">导入</el-button>
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

@media (max-width: 900px) {
  .grid-three {
    grid-template-columns: 1fr;
  }
}
</style>
