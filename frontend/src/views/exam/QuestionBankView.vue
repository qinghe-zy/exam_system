<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { DIFFICULTY_OPTIONS, QUESTION_TYPE_OPTIONS, REVIEW_STATUS_OPTIONS } from '../../constants/exam'
import { autoGroupQuestionsByKnowledgePoint, createQuestion, deleteQuestion, exportQuestions, fetchQuestions, generateAiQuestionDraft, importQuestions, polishQuestionWithAi, updateQuestion } from '../../api/exam'
import { usePermission } from '../../hooks/usePermission'
import type { AiQuestionDraftRequest, QuestionBank } from '../../types/exam'
import { labelDifficulty, labelQuestionType, labelReviewStatus } from '../../utils/labels'

type AttachmentPreview = { name: string; url: string; type?: string }

const { hasPermission } = usePermission()
const loading = ref(false)
const questions = ref<QuestionBank[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const importDialogVisible = ref(false)
const importText = ref('')
const aiDraftDialogVisible = ref(false)
const aiLoading = ref(false)
const knowledgeDialogVisible = ref(false)
const knowledgeLoading = ref(false)
const knowledgeResults = ref<QuestionBank[]>([])

const aiDraftForm = reactive<AiQuestionDraftRequest>({
  subject: '',
  questionType: 'SINGLE_CHOICE',
  difficultyLevel: 'MEDIUM',
  knowledgePoint: '',
  chapterName: '',
  extraRequirements: ''
})

const knowledgeForm = reactive({
  subject: '',
  difficultyLevel: '',
  questionType: '',
  quotaText: '现代文阅读,2\n古诗文默写,2'
})

const form = reactive<Omit<QuestionBank, 'id'>>({
  questionCode: '',
  subject: '',
  questionType: 'SINGLE_CHOICE',
  difficultyLevel: 'MEDIUM',
  stem: '',
  stemHtml: '',
  materialContent: '',
  attachmentJson: '[]',
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
  status: 1,
  usageCount: 0
})

const rules: FormRules<typeof form> = {
  questionCode: [{ required: true, message: '请输入题目编码', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入学科名称', trigger: 'blur' }],
  stem: [{ required: true, message: '请输入题干内容', trigger: 'blur' }],
  answerKey: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

const objectiveHint = computed(() => {
  if (form.questionType === 'MULTIPLE_CHOICE') return '多选题请使用“|”分隔多个答案'
  if (form.questionType === 'TRUE_FALSE') return '判断题请填写“正确”或“错误”'
  if (form.questionType === 'FILL_BLANK') return '填空题请使用“|”按空位顺序填写标准答案，例如：牛顿|伽利略'
  if (form.questionType === 'MATERIAL') return '材料题建议在“材料内容”中填写背景材料，并在答案中写评分参考'
  if (form.questionType === 'ESSAY') return '论述题请填写评分参考要点或示例答案'
  return '请填写标准答案'
})

const optionPlaceholder = computed(() =>
  form.questionType === 'FILL_BLANK' ? '["第1空","第2空"]' : '["Option A","Option B"]'
)

const attachmentPreviewList = computed(() => parseAttachments(form.attachmentJson))
const blankSlotPreview = computed(() => parseOptionList(form.optionsJson))

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
    stemHtml: '',
    materialContent: '',
    attachmentJson: '[]',
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
    status: 1,
    usageCount: 0
  })
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openAiDraft() {
  aiDraftForm.subject = form.subject || questions.value[0]?.subject || ''
  aiDraftForm.questionType = 'SINGLE_CHOICE'
  aiDraftForm.difficultyLevel = 'MEDIUM'
  aiDraftForm.knowledgePoint = ''
  aiDraftForm.chapterName = ''
  aiDraftForm.extraRequirements = ''
  aiDraftDialogVisible.value = true
}

function openKnowledgeGroup() {
  knowledgeForm.subject = form.subject || questions.value[0]?.subject || ''
  knowledgeForm.difficultyLevel = ''
  knowledgeForm.questionType = ''
  knowledgeDialogVisible.value = true
  knowledgeResults.value = []
}

function openEdit(row: QuestionBank) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    ...row,
    stemHtml: row.stemHtml || '',
    materialContent: row.materialContent || '',
    attachmentJson: row.attachmentJson || '[]',
    optionsJson: row.optionsJson || '[]',
    usageCount: row.usageCount || 0
  })
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      ...form,
      stemHtml: form.stemHtml || undefined,
      materialContent: form.questionType === 'MATERIAL' ? form.materialContent : '',
      attachmentJson: form.attachmentJson || '[]',
      optionsJson: normalizeOptionsByQuestionType(),
      usageCount: undefined
    }
    if (dialogMode.value === 'create') {
      await createQuestion(payload as Omit<QuestionBank, 'id'>)
    } else if (editingId.value) {
      await updateQuestion(editingId.value, payload as Omit<QuestionBank, 'id'>)
    }
    ElMessage.success(dialogMode.value === 'create' ? '题目已创建' : '题目已更新')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该题目？', '提示', { type: 'warning' })
  await deleteQuestion(id)
  ElMessage.success('题目已删除')
  await loadData()
}

async function handleExport() {
  const payload = await exportQuestions()
  await navigator.clipboard.writeText(JSON.stringify(payload, null, 2))
  ElMessage.success('题库 JSON 已复制到剪贴板')
}

async function handleImport() {
  const payload = JSON.parse(importText.value)
  await importQuestions({ questions: payload })
  ElMessage.success('题目导入完成')
  importDialogVisible.value = false
  importText.value = ''
  await loadData()
}

async function handleAiGenerateDraft() {
  aiLoading.value = true
  try {
    const result = await generateAiQuestionDraft(aiDraftForm)
    dialogMode.value = 'create'
    Object.assign(form, {
      questionCode: '',
      subject: result.subject,
      questionType: result.questionType,
      difficultyLevel: result.difficultyLevel,
      stem: result.stem,
      stemHtml: '',
      materialContent: '',
      attachmentJson: '[]',
      optionsJson: result.optionsJson,
      answerKey: result.answerKey,
      analysisText: result.analysisText,
      knowledgePoint: result.knowledgePoint,
      chapterName: result.chapterName || '',
      sourceName: 'AI 辅助生成',
      tags: result.tags || '',
      defaultScore: result.defaultScore,
      reviewerStatus: 'DRAFT',
      versionNo: 1,
      status: 1,
      usageCount: 0
    })
    aiDraftDialogVisible.value = false
    dialogVisible.value = true
    ElMessage.success(result.aiHint)
  } finally {
    aiLoading.value = false
  }
}

async function handleAiPolishCurrentForm() {
  if (!form.subject || !form.questionType || !form.difficultyLevel || !form.stem) {
    ElMessage.warning('请至少填写学科、题型、难度和题干后再使用 AI 优化')
    return
  }
  aiLoading.value = true
  try {
    const result = await polishQuestionWithAi({
      subject: form.subject,
      questionType: form.questionType,
      difficultyLevel: form.difficultyLevel,
      stem: form.stem,
      optionsJson: form.optionsJson,
      answerKey: form.answerKey,
      analysisText: form.analysisText,
      knowledgePoint: form.knowledgePoint,
      chapterName: form.chapterName
    })
    form.stem = result.improvedStem || form.stem
    form.answerKey = result.improvedAnswerKey || form.answerKey
    form.analysisText = result.improvedAnalysisText || form.analysisText
    form.optionsJson = result.suggestedOptionsJson || form.optionsJson
    ElMessage.success(result.aiHint)
  } finally {
    aiLoading.value = false
  }
}

async function handleKnowledgeAutoGroup() {
  knowledgeLoading.value = true
  try {
    const quotas = knowledgeForm.quotaText
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const [knowledgePoint, questionCount] = line.split(',')
        return {
          knowledgePoint: knowledgePoint.trim(),
          questionCount: Number(questionCount)
        }
      })
    knowledgeResults.value = await autoGroupQuestionsByKnowledgePoint({
      subject: knowledgeForm.subject,
      difficultyLevel: knowledgeForm.difficultyLevel || undefined,
      questionType: knowledgeForm.questionType || undefined,
      quotas
    })
    ElMessage.success(`已按知识点生成 ${knowledgeResults.value.length} 道候选题`)
  } finally {
    knowledgeLoading.value = false
  }
}

function copyKnowledgeResults() {
  navigator.clipboard.writeText(JSON.stringify(knowledgeResults.value, null, 2))
  ElMessage.success('候选题组已复制到剪贴板')
}

function normalizeOptionsByQuestionType() {
  if (form.questionType === 'MATERIAL') {
    return '[]'
  }
  if (form.questionType === 'ESSAY' || form.questionType === 'SHORT_ANSWER') {
    return '[]'
  }
  return form.optionsJson || '[]'
}

function parseOptionList(value?: string) {
  try {
    const parsed = value ? JSON.parse(value) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function parseAttachments(value?: string): AttachmentPreview[] {
  try {
    const parsed = value ? JSON.parse(value) : []
    if (!Array.isArray(parsed)) return []
    return parsed.map((item) => {
      if (typeof item === 'string') {
        return { name: item, url: item }
      }
      return {
        name: item.name || item.url,
        url: item.url,
        type: item.type
      }
    }).filter((item) => item.url)
  } catch {
    return []
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="题库管理"
    title="新题型、富文本、附件与知识点自动组题"
    description="当前题库页除了基础题型维护，还补充了填空题、论述题、材料题、富文本 HTML、附件 JSON 和知识点自动组题基础版。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button v-if="hasPermission('exam:question:knowledge:auto-group')" @click="openKnowledgeGroup">按知识点自动组题</el-button>
        <el-button v-if="hasPermission('exam:question:create')" @click="openAiDraft">AI 生成题目草稿</el-button>
        <el-button @click="handleExport">导出 JSON</el-button>
        <el-button v-if="hasPermission('exam:question:import')" @click="importDialogVisible = true">导入 JSON</el-button>
        <el-button v-if="hasPermission('exam:question:create')" type="primary" @click="openCreate">新建题目</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="questions" v-loading="loading">
        <el-table-column prop="questionCode" label="题目编码" min-width="140" />
        <el-table-column prop="subject" label="学科" min-width="120" />
        <el-table-column label="题型" min-width="120"><template #default="{ row }">{{ labelQuestionType(row.questionType) }}</template></el-table-column>
        <el-table-column label="难度" min-width="100"><template #default="{ row }">{{ labelDifficulty(row.difficultyLevel) }}</template></el-table-column>
        <el-table-column prop="knowledgePoint" label="知识点" min-width="150" show-overflow-tooltip />
        <el-table-column prop="usageCount" label="使用次数" min-width="100" />
        <el-table-column label="富文本" min-width="90"><template #default="{ row }">{{ row.stemHtml ? '已配置' : '未配置' }}</template></el-table-column>
        <el-table-column label="附件" min-width="90"><template #default="{ row }">{{ parseAttachments(row.attachmentJson).length }} 个</template></el-table-column>
        <el-table-column prop="defaultScore" label="默认分值" min-width="90" />
        <el-table-column label="审核状态" min-width="110"><template #default="{ row }">{{ labelReviewStatus(row.reviewerStatus) }}</template></el-table-column>
        <el-table-column prop="stem" label="题干摘要" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" min-width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('exam:question:update')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('exam:question:delete')" link type="danger" @click="removeItem(row.id)">删除</el-button>
            <span v-if="!hasPermission('exam:question:update') && !hasPermission('exam:question:delete')" class="muted">仅查看</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建题目' : '编辑题目'" width="min(1080px, 96vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="ai-banner">
          <span>AI 辅助只提供草稿和优化建议，最终内容仍需教师人工复核。</span>
          <el-button size="small" :loading="aiLoading" @click="handleAiPolishCurrentForm">AI 优化当前题目</el-button>
        </div>
        <div class="grid-three">
          <el-form-item label="题目编码" prop="questionCode"><el-input v-model="form.questionCode" /></el-form-item>
          <el-form-item label="学科" prop="subject"><el-input v-model="form.subject" /></el-form-item>
          <el-form-item label="题型"><el-select v-model="form.questionType"><el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="难度"><el-select v-model="form.difficultyLevel"><el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="默认分值"><el-input-number v-model="form.defaultScore" :min="1" :max="100" /></el-form-item>
          <el-form-item label="审核状态"><el-select v-model="form.reviewerStatus"><el-option v-for="item in REVIEW_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="知识点"><el-input v-model="form.knowledgePoint" /></el-form-item>
          <el-form-item label="章节"><el-input v-model="form.chapterName" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="form.sourceName" /></el-form-item>
        </div>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="例如：函数、文学常识、细胞结构" /></el-form-item>
        <el-form-item label="题干（纯文本）" prop="stem"><el-input v-model="form.stem" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="题干富文本 HTML（基础版）">
          <el-input v-model="form.stemHtml" type="textarea" :rows="4" placeholder="<p><strong>重点词</strong> 可以用 HTML 包裹</p>" />
        </el-form-item>
        <div v-if="form.stemHtml" class="preview-card">
          <strong>题干富文本预览</strong>
          <div class="html-preview" v-html="form.stemHtml"></div>
        </div>
        <el-form-item v-if="form.questionType === 'MATERIAL'" label="材料内容（支持 HTML 片段）">
          <el-input v-model="form.materialContent" type="textarea" :rows="4" placeholder="请填写材料背景、案例正文或阅读材料摘要" />
        </el-form-item>
        <el-form-item label="附件 JSON（基础版）">
          <el-input v-model="form.attachmentJson" type="textarea" :rows="3" placeholder='[{"name":"示意图","url":"https://...","type":"image"}]' />
        </el-form-item>
        <div v-if="attachmentPreviewList.length" class="preview-card">
          <strong>附件预览</strong>
          <div class="attachment-list">
            <a v-for="item in attachmentPreviewList" :key="item.url" :href="item.url" target="_blank" rel="noreferrer">{{ item.name }}</a>
          </div>
        </div>
        <el-form-item label="选项 / 填空位 JSON">
          <el-input v-model="form.optionsJson" type="textarea" :rows="3" :placeholder="optionPlaceholder" />
        </el-form-item>
        <div v-if="form.questionType === 'FILL_BLANK' && blankSlotPreview.length" class="preview-card">
          <strong>填空位预览</strong>
          <div class="attachment-list">
            <span v-for="slot in blankSlotPreview" :key="slot" class="slot-pill">{{ slot }}</span>
          </div>
        </div>
        <el-form-item :label="`答案说明：${objectiveHint}`" prop="answerKey"><el-input v-model="form.answerKey" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="解析 / 评分说明"><el-input v-model="form.analysisText" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
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

    <el-dialog v-model="aiDraftDialogVisible" title="AI 生成题目草稿" width="min(760px, 96vw)">
      <p class="muted">系统会调用 DeepSeek 生成候选题目草稿，结果会回填到题目编辑表单中。请务必复核后再保存。</p>
      <div class="grid-three">
        <el-form-item label="学科"><el-input v-model="aiDraftForm.subject" /></el-form-item>
        <el-form-item label="题型"><el-select v-model="aiDraftForm.questionType"><el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="难度"><el-select v-model="aiDraftForm.difficultyLevel"><el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      </div>
      <el-form-item label="知识点"><el-input v-model="aiDraftForm.knowledgePoint" /></el-form-item>
      <el-form-item label="章节"><el-input v-model="aiDraftForm.chapterName" /></el-form-item>
      <el-form-item label="补充要求"><el-input v-model="aiDraftForm.extraRequirements" type="textarea" :rows="4" placeholder="例如：偏重理解题、避免太偏太怪、题干简洁" /></el-form-item>
      <template #footer>
        <el-button @click="aiDraftDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="handleAiGenerateDraft">生成草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="knowledgeDialogVisible" title="按知识点自动组题（基础版）" width="min(860px, 96vw)">
      <p class="muted">每行格式：知识点,题数。系统会在当前学科范围内按知识点抽取候选题组，适用于备课和试卷蓝图准备。</p>
      <div class="grid-three">
        <el-form-item label="学科"><el-input v-model="knowledgeForm.subject" /></el-form-item>
        <el-form-item label="题型筛选"><el-select v-model="knowledgeForm.questionType" clearable><el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="难度筛选"><el-select v-model="knowledgeForm.difficultyLevel" clearable><el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      </div>
      <el-form-item label="知识点配额"><el-input v-model="knowledgeForm.quotaText" type="textarea" :rows="6" /></el-form-item>
      <div class="dialog-actions">
        <el-button :loading="knowledgeLoading" type="primary" @click="handleKnowledgeAutoGroup">生成候选题组</el-button>
        <el-button v-if="knowledgeResults.length" @click="copyKnowledgeResults">复制结果</el-button>
      </div>
      <el-table v-if="knowledgeResults.length" :data="knowledgeResults" max-height="360">
        <el-table-column prop="questionCode" label="题号" min-width="120" />
        <el-table-column prop="knowledgePoint" label="知识点" min-width="140" />
        <el-table-column label="题型" min-width="100"><template #default="{ row }">{{ labelQuestionType(row.questionType) }}</template></el-table-column>
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="knowledgeDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.hero-actions,
.dialog-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.ai-banner {
  margin-bottom: 1rem;
  padding: 0.9rem 1rem;
  border-radius: 16px;
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: center;
  background: color-mix(in oklch, var(--brand) 10%, white);
}

.section-card,
.preview-card {
  padding: 1rem;
}

.preview-card {
  border: 1px solid color-mix(in oklch, var(--line) 78%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 95%, var(--panel-soft));
  margin-bottom: 1rem;
}

.html-preview {
  margin-top: 0.7rem;
}

.attachment-list {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-top: 0.7rem;
}

.slot-pill {
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: color-mix(in oklch, var(--accent) 15%, white);
}

.grid-three {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

@media (max-width: 900px) {
  .ai-banner,
  .grid-three {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
