<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { DIFFICULTY_OPTIONS, QUESTION_TYPE_OPTIONS, REVIEW_STATUS_OPTIONS } from '../../constants/exam'
import { createQuestion, fetchQuestions, generateAiQuestionDraft, polishQuestionWithAi, updateQuestion } from '../../api/exam'
import type { AiQuestionDraftRequest, QuestionBank } from '../../types/exam'

type AttachmentPreview = { name: string; url: string; type?: string }

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const aiLoading = ref(false)
const formRef = ref<FormInstance>()
const allQuestions = ref<QuestionBank[]>([])

const editingId = computed(() => {
  const raw = route.params.questionId
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isNaN(parsed) ? null : parsed
})
const pageTitle = computed(() => editingId.value ? '编辑题目' : '新建题目')

const aiDraftDialogVisible = ref(false)
const aiDraftForm = reactive<AiQuestionDraftRequest>({
  subject: '',
  questionType: 'SINGLE_CHOICE',
  difficultyLevel: 'MEDIUM',
  knowledgePoint: '',
  chapterName: '',
  extraRequirements: ''
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
}

async function loadData() {
  loading.value = true
  try {
    allQuestions.value = await fetchQuestions()
    resetForm()
    if (editingId.value) {
      const current = allQuestions.value.find((item) => item.id === editingId.value)
      if (!current) {
        ElMessage.error('未找到对应题目')
        await router.push('/exam/questions')
        return
      }
      Object.assign(form, {
        ...current,
        stemHtml: current.stemHtml || '',
        materialContent: current.materialContent || '',
        attachmentJson: current.attachmentJson || '[]',
        optionsJson: current.optionsJson || '[]',
        usageCount: current.usageCount || 0
      })
    }
  } finally {
    loading.value = false
  }
}

function openAiDraft() {
  aiDraftForm.subject = form.subject || allQuestions.value[0]?.subject || ''
  aiDraftForm.questionType = 'SINGLE_CHOICE'
  aiDraftForm.difficultyLevel = 'MEDIUM'
  aiDraftForm.knowledgePoint = ''
  aiDraftForm.chapterName = ''
  aiDraftForm.extraRequirements = ''
  aiDraftDialogVisible.value = true
}

async function handleAiGenerateDraft() {
  aiLoading.value = true
  try {
    const result = await generateAiQuestionDraft(aiDraftForm)
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

function normalizeOptionsByQuestionType() {
  if (form.questionType === 'MATERIAL') return '[]'
  if (form.questionType === 'ESSAY' || form.questionType === 'SHORT_ANSWER') return '[]'
  return form.optionsJson || '[]'
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      ...form,
      stemHtml: form.stemHtml || undefined,
      materialContent: form.questionType === 'MATERIAL' ? form.materialContent : '',
      attachmentJson: form.attachmentJson || '[]',
      optionsJson: normalizeOptionsByQuestionType(),
      usageCount: undefined
    }
    if (editingId.value) {
      await updateQuestion(editingId.value, payload as Omit<QuestionBank, 'id'>)
    } else {
      await createQuestion(payload as Omit<QuestionBank, 'id'>)
    }
    ElMessage.success(editingId.value ? '题目已更新' : '题目已创建')
    await router.push('/exam/questions')
  } finally {
    saving.value = false
  }
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
    return parsed.map((item) => typeof item === 'string'
      ? { name: item, url: item }
      : { name: item.name || item.url, url: item.url, type: item.type }).filter((item) => item.url)
  } catch {
    return []
  }
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="题库管理"
    :title="pageTitle"
    description="题目录入与编辑改为独立页面，支持新题型、富文本、附件和 AI 草稿优化。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button @click="router.push('/exam/questions')">返回题库列表</el-button>
        <el-button @click="openAiDraft">AI 生成题目草稿</el-button>
      </div>
    </template>

    <section class="panel-card section-card" v-loading="loading">
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
        <el-form-item label="题干富文本 HTML">
          <el-input v-model="form.stemHtml" type="textarea" :rows="4" placeholder="<p><strong>重点词</strong> 可以用 HTML 包裹</p>" />
        </el-form-item>
        <div v-if="form.stemHtml" class="preview-card">
          <strong>题干富文本预览</strong>
          <div class="html-preview" v-html="form.stemHtml"></div>
        </div>
        <el-form-item v-if="form.questionType === 'MATERIAL'" label="材料内容（支持 HTML 片段）">
          <el-input v-model="form.materialContent" type="textarea" :rows="4" placeholder="请填写材料背景、案例正文或阅读材料摘要" />
        </el-form-item>
        <el-form-item label="附件 JSON">
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
        <div class="page-actions">
          <el-button @click="router.push('/exam/questions')">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存题目</el-button>
        </div>
      </el-form>
    </section>

    <el-dialog v-model="aiDraftDialogVisible" title="AI 生成题目草稿" width="min(760px, 96vw)">
      <p class="muted">系统会调用 DeepSeek 生成候选题目草稿，结果会回填到当前独立编辑页中。请务必复核后再保存。</p>
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
  </AppShellSection>
</template>

<style scoped>
.hero-actions,
.page-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.page-actions {
  justify-content: flex-end;
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
