<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { PAPER_MODE_OPTIONS } from '../../constants/exam'
import { createPaper, deletePaper, fetchPapers, fetchQuestions, updatePaper } from '../../api/exam'
import type { ExamPaper, PaperQuestionItem, QuestionBank } from '../../types/exam'
import { labelPaperMode } from '../../utils/labels'

const loading = ref(false)
const papers = ref<ExamPaper[]>([])
const questions = ref<QuestionBank[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<Omit<ExamPaper, 'id' | 'questionCount'>>({
  paperCode: '',
  paperName: '',
  subject: '',
  assemblyMode: 'MANUAL',
  descriptionText: '',
  durationMinutes: 90,
  totalScore: 100,
  passScore: 60,
  publishStatus: 0,
  questionItems: []
})

const rules: FormRules<typeof form> = {
  paperCode: [{ required: true, message: '请输入试卷编码', trigger: 'blur' }],
  paperName: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入学科名称', trigger: 'blur' }]
}

const totalQuestionScore = computed(() => form.questionItems.reduce((sum, item) => sum + Number(item.score || 0), 0))
const generatorDialogVisible = ref(false)
const generator = reactive({
  subject: '',
  targetCount: 4
})

const availableSubjects = computed(() => [...new Set(questions.value.map((item) => item.subject))])

async function loadData() {
  loading.value = true
  try {
    const [paperResult, questionResult] = await Promise.allSettled([fetchPapers(), fetchQuestions()])
    papers.value = paperResult.status === 'fulfilled' ? paperResult.value : []
    questions.value = questionResult.status === 'fulfilled' ? questionResult.value : []
  } finally {
    loading.value = false
  }
}

async function ensureQuestionPoolLoaded() {
  if (questions.value.length > 0) return
  questions.value = await fetchQuestions()
}

function resetForm() {
  Object.assign(form, {
    paperCode: '',
    paperName: '',
    subject: '',
    assemblyMode: 'MANUAL',
    descriptionText: '',
    durationMinutes: 90,
    totalScore: 100,
    passScore: 60,
    publishStatus: 0,
    questionItems: []
  })
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  ensureQuestionPoolLoaded()
  dialogVisible.value = true
}

function openEdit(row: ExamPaper) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

function addQuestion(question: QuestionBank) {
  if (form.questionItems.some((item) => item.questionId === question.id)) return
  form.questionItems.push({
    questionId: question.id,
    sortNo: form.questionItems.length + 1,
    score: question.defaultScore,
    requiredFlag: 1,
    questionCode: question.questionCode,
    questionType: question.questionType,
    stem: question.stem
  })
}

function removeQuestion(questionId: number) {
  form.questionItems = form.questionItems.filter((item) => item.questionId !== questionId)
  form.questionItems.forEach((item, index) => {
    item.sortNo = index + 1
  })
}

function generateRandom() {
  const pool = questions.value.filter((item) => !generator.subject || item.subject === generator.subject)
  const shuffled = [...pool].sort(() => Math.random() - 0.5).slice(0, generator.targetCount)
  form.questionItems = shuffled.map((question, index) => ({
    questionId: question.id,
    sortNo: index + 1,
    score: question.defaultScore,
    requiredFlag: 1,
    questionCode: question.questionCode,
    questionType: question.questionType,
    stem: question.stem
  }))
  generatorDialogVisible.value = false
}

function generateStrategy() {
  const pool = questions.value.filter((item) => !generator.subject || item.subject === generator.subject)
  const easy = pool.filter((item) => item.difficultyLevel === 'EASY')
  const medium = pool.filter((item) => item.difficultyLevel === 'MEDIUM')
  const hard = pool.filter((item) => item.difficultyLevel === 'HARD')
  const targetEasy = Math.max(1, Math.floor(generator.targetCount * 0.3))
  const targetMedium = Math.max(1, Math.floor(generator.targetCount * 0.5))
  const targetHard = Math.max(0, generator.targetCount - targetEasy - targetMedium)
  const selected = [
    ...easy.slice(0, targetEasy),
    ...medium.slice(0, targetMedium),
    ...hard.slice(0, targetHard)
  ].slice(0, generator.targetCount)
  form.questionItems = selected.map((question, index) => ({
    questionId: question.id,
    sortNo: index + 1,
    score: question.defaultScore,
    requiredFlag: 1,
    questionCode: question.questionCode,
    questionType: question.questionType,
    stem: question.stem
  }))
  generatorDialogVisible.value = false
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!form.questionItems.length) {
      ElMessage.warning('请至少选择一道题目')
      return
    }
    form.totalScore = totalQuestionScore.value
    if (dialogMode.value === 'create') {
      await createPaper(form)
    } else if (editingId.value) {
      await updatePaper(editingId.value, form)
    }
    ElMessage.success(dialogMode.value === 'create' ? '试卷已创建' : '试卷已更新')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该试卷？', '提示', { type: 'warning' })
  await deletePaper(id)
  ElMessage.success('试卷已删除')
  await loadData()
}

async function openGenerator() {
  await ensureQuestionPoolLoaded()
  if (!generator.subject && availableSubjects.value.length > 0) {
    generator.subject = availableSubjects.value[0]
  }
  generatorDialogVisible.value = true
}
</script>

<template>
  <AppShellSection
    eyebrow="试卷管理"
    title="显式组卷与题目分值配置"
    description="试卷工作台用于维护试卷编码、学科、组卷方式、卷面组成和分值配置，确保每场考试都能清晰追溯卷面来源。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button @click="openGenerator">随机/策略组卷</el-button>
        <el-button type="primary" @click="openCreate">新建试卷</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="papers" v-loading="loading">
        <el-table-column prop="paperCode" label="试卷编码" min-width="140" />
        <el-table-column prop="paperName" label="试卷名称" min-width="220" />
        <el-table-column prop="subject" label="学科" min-width="140" />
        <el-table-column label="组卷方式" min-width="120"><template #default="{ row }">{{ labelPaperMode(row.assemblyMode) }}</template></el-table-column>
        <el-table-column prop="questionCount" label="题目数" min-width="100" />
        <el-table-column prop="totalScore" label="总分" min-width="90" />
        <el-table-column prop="passScore" label="及格线" min-width="90" />
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建试卷' : '编辑试卷'" width="min(1080px, 96vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-three">
          <el-form-item label="试卷编码" prop="paperCode"><el-input v-model="form.paperCode" /></el-form-item>
          <el-form-item label="试卷名称" prop="paperName"><el-input v-model="form.paperName" /></el-form-item>
          <el-form-item label="学科" prop="subject"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="组卷方式"><el-select v-model="form.assemblyMode"><el-option v-for="item in PAPER_MODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="考试时长（分钟）"><el-input-number v-model="form.durationMinutes" :min="1" /></el-form-item>
          <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="1" :max="100" /></el-form-item>
        </div>
        <el-form-item label="试卷说明"><el-input v-model="form.descriptionText" type="textarea" :rows="3" /></el-form-item>

        <div class="selector-grid">
          <section class="panel-card sub-card">
            <div class="sub-card-header">
              <h3>可选题目</h3>
              <span class="muted">共 {{ questions.length }} 道</span>
            </div>
            <div class="question-pool">
              <button v-for="question in questions" :key="question.id" type="button" class="question-chip" @click="addQuestion(question)">
                <strong>{{ question.questionCode }}</strong>
                <span>{{ question.stem }}</span>
              </button>
            </div>
          </section>

          <section class="panel-card sub-card">
            <div class="sub-card-header">
              <h3>卷面组成</h3>
              <span class="muted">已选 {{ form.questionItems.length }} 题，共 {{ totalQuestionScore }} 分</span>
            </div>
            <div class="composition-list">
              <article v-for="item in form.questionItems" :key="item.questionId" class="composition-item">
                <div>
                  <strong>{{ item.questionCode }}</strong>
                  <p class="muted">{{ item.stem }}</p>
                </div>
                <div class="composition-actions">
                  <el-input-number v-model="item.sortNo" :min="1" :max="form.questionItems.length" />
                  <el-input-number v-model="item.score" :min="1" :max="100" />
                  <el-switch v-model="item.requiredFlag" :active-value="1" :inactive-value="0" inline-prompt active-text="必答" inactive-text="选答" />
                  <el-button link type="danger" @click="removeQuestion(item.questionId)">移除</el-button>
                </div>
              </article>
            </div>
          </section>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="generatorDialogVisible" title="随机 / 策略组卷" width="min(720px, 94vw)">
      <div class="grid-two">
        <el-form-item label="科目">
          <el-select v-model="generator.subject" clearable>
            <el-option v-for="subject in availableSubjects" :key="subject" :label="subject" :value="subject" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标题数">
          <el-input-number v-model="generator.targetCount" :min="1" :max="20" />
        </el-form-item>
      </div>
      <div class="generator-actions">
        <el-button @click="generateRandom">随机组卷</el-button>
        <el-button type="primary" @click="generateStrategy">按难度策略组卷</el-button>
      </div>
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

.selector-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 1rem;
  margin-top: 1rem;
}

.generator-actions {
  display: flex;
  gap: 0.8rem;
  justify-content: flex-end;
}

.sub-card {
  padding: 1rem;
  display: grid;
  gap: 0.9rem;
}

.sub-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub-card-header h3 {
  margin: 0;
  font-family: 'Literata', Georgia, serif;
}

.question-pool,
.composition-list {
  display: grid;
  gap: 0.75rem;
}

.question-chip,
.composition-item {
  border: 1px solid color-mix(in oklch, var(--line) 82%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 92%, var(--panel-soft));
}

.question-chip {
  padding: 0.95rem;
  text-align: left;
  display: grid;
  gap: 0.4rem;
  cursor: pointer;
}

.composition-item {
  padding: 0.9rem;
  display: grid;
  gap: 0.8rem;
}

.composition-item p {
  margin: 0.35rem 0 0;
}

.composition-actions {
  display: flex;
  gap: 0.7rem;
  align-items: center;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .grid-three,
  .selector-grid {
    grid-template-columns: 1fr;
  }
}
</style>
