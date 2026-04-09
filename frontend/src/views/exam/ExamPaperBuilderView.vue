<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { DIFFICULTY_OPTIONS, PAPER_MODE_OPTIONS, QUESTION_TYPE_OPTIONS } from '../../constants/exam'
import { createPaper, fetchPaper, fetchQuestions, updatePaper } from '../../api/exam'
import type { ExamPaper, PaperQuestionItem, PaperRuleConfigItem, QuestionBank } from '../../types/exam'
import { labelDifficulty, labelPaperMode, labelQuestionType } from '../../utils/labels'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const questions = ref<QuestionBank[]>([])
const formRef = ref<FormInstance>()
const activeStep = ref<'manual' | 'random' | 'strategy' | 'preview'>('manual')
const editingId = computed(() => Number(route.params.paperId || 0) || null)

const form = reactive<Omit<ExamPaper, 'id' | 'questionCount' | 'totalScore'>>({
  paperCode: '',
  paperName: '',
  subject: '',
  assemblyMode: 'MANUAL',
  descriptionText: '',
  paperVersion: '2026春季A卷',
  remarkText: '',
  durationMinutes: 90,
  passScore: 60,
  shuffleEnabled: 0,
  questionTypeConfigs: [],
  difficultyConfigs: [],
  publishStatus: 0,
  questionItems: []
})

const questionFilters = reactive({
  subject: '',
  questionType: '',
  difficultyLevel: '',
  keyword: ''
})

const rules: FormRules<typeof form> = {
  paperCode: [{ required: true, message: '请输入试卷编码', trigger: 'blur' }],
  paperName: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  subject: [{ required: true, message: '请选择学科', trigger: 'change' }]
}

function defaultScoreByQuestionType(questionType: string) {
  if (questionType === 'MULTIPLE_CHOICE') return 10
  if (questionType === 'SHORT_ANSWER') return 15
  return 5
}

function createEmptyTypeConfigs(): PaperRuleConfigItem[] {
  return QUESTION_TYPE_OPTIONS.map((item) => ({
    code: item.value,
    label: item.label,
    count: 0,
    score: defaultScoreByQuestionType(item.value)
  }))
}

function createEmptyDifficultyConfigs(): PaperRuleConfigItem[] {
  return DIFFICULTY_OPTIONS.map((item) => ({
    code: item.value,
    label: item.label,
    count: 0,
    score: null
  }))
}

function resetForm() {
  Object.assign(form, {
    paperCode: '',
    paperName: '',
    subject: '',
    assemblyMode: 'MANUAL',
    descriptionText: '',
    paperVersion: '2026春季A卷',
    remarkText: '',
    durationMinutes: 90,
    passScore: 60,
    shuffleEnabled: 0,
    questionTypeConfigs: createEmptyTypeConfigs(),
    difficultyConfigs: createEmptyDifficultyConfigs(),
    publishStatus: 0,
    questionItems: []
  })
  questionFilters.subject = ''
  questionFilters.questionType = ''
  questionFilters.difficultyLevel = ''
  questionFilters.keyword = ''
  activeStep.value = resolveStepByMode(String(route.query.mode || 'MANUAL'))
}

function resolveStepByMode(mode: string) {
  if (mode === 'RANDOM') return 'random'
  if (mode === 'STRATEGY') return 'strategy'
  return 'manual'
}

function cloneConfigs(configs: PaperRuleConfigItem[] | undefined, fallback: PaperRuleConfigItem[]) {
  if (!configs || configs.length === 0) return fallback.map((item) => ({ ...item }))
  const fallbackMap = new Map(fallback.map((item) => [item.code, item]))
  return configs.map((item) => ({
    code: item.code,
    label: item.label || fallbackMap.get(item.code)?.label || item.code,
    count: item.count ?? 0,
    score: item.score ?? fallbackMap.get(item.code)?.score ?? null
  }))
}

const availableSubjects = computed(() => [...new Set(questions.value.map((item) => item.subject))].sort((left, right) => left.localeCompare(right)))
const selectedQuestionIds = computed(() => new Set(form.questionItems.map((item) => item.questionId)))
const filteredQuestions = computed(() => {
  return questions.value.filter((item) => {
    if (questionFilters.subject && item.subject !== questionFilters.subject) return false
    if (questionFilters.questionType && item.questionType !== questionFilters.questionType) return false
    if (questionFilters.difficultyLevel && item.difficultyLevel !== questionFilters.difficultyLevel) return false
    if (questionFilters.keyword) {
      const keyword = questionFilters.keyword.trim().toLowerCase()
      const haystack = `${item.questionCode} ${item.stem} ${item.knowledgePoint || ''}`.toLowerCase()
      if (!haystack.includes(keyword)) return false
    }
    return true
  })
})

const totalQuestionScore = computed(() => form.questionItems.reduce((sum, item) => sum + Number(item.score || 0), 0))
const blueprintTypeTotal = computed(() => form.questionTypeConfigs.reduce((sum, item) => sum + Number(item.count || 0), 0))
const blueprintDifficultyTotal = computed(() => form.difficultyConfigs.reduce((sum, item) => sum + Number(item.count || 0), 0))
const typeSummary = computed(() => summarizeByQuestionType(form.questionItems))
const difficultySummary = computed(() => summarizeByDifficulty(form.questionItems))
const poolSummaryText = computed(() => {
  if (!questionFilters.subject) return `当前题库共 ${questions.value.length} 道题，可按学科、题型、难度快速筛题。`
  const sameSubjectCount = questions.value.filter((item) => item.subject === questionFilters.subject).length
  return `当前学科 ${questionFilters.subject} 共 ${sameSubjectCount} 道题，筛选结果 ${filteredQuestions.value.length} 道。`
})

async function initBuilder() {
  loading.value = true
  try {
    questions.value = await fetchQuestions()
    resetForm()
    if (!editingId.value) {
      if (availableSubjects.value.length > 0) {
        form.subject = availableSubjects.value[0]
        questionFilters.subject = form.subject
      }
      if (activeStep.value === 'random' || activeStep.value === 'strategy') {
        seedBlueprintCounts(10)
      }
      return
    }
    const detail = await fetchPaper(editingId.value)
    Object.assign(form, {
      paperCode: detail.paperCode,
      paperName: detail.paperName,
      subject: detail.subject,
      assemblyMode: detail.assemblyMode,
      descriptionText: detail.descriptionText || '',
      paperVersion: detail.paperVersion || '2026春季A卷',
      remarkText: detail.remarkText || '',
      durationMinutes: detail.durationMinutes,
      passScore: detail.passScore,
      shuffleEnabled: detail.shuffleEnabled ?? 0,
      questionTypeConfigs: cloneConfigs(detail.questionTypeConfigs, createEmptyTypeConfigs()),
      difficultyConfigs: cloneConfigs(detail.difficultyConfigs, createEmptyDifficultyConfigs()),
      publishStatus: detail.publishStatus,
      questionItems: detail.questionItems.map((item) => ({ ...item }))
    })
    questionFilters.subject = detail.subject
    rebuildBlueprintFromSelection(true)
  } finally {
    loading.value = false
  }
}

watch(() => route.fullPath, () => initBuilder())
watch(() => form.subject, (subject) => {
  if (!questionFilters.subject) questionFilters.subject = subject
})

function resequenceQuestionItems() {
  form.questionItems.forEach((item, index) => {
    item.sortNo = index + 1
  })
}

function addQuestion(question: QuestionBank) {
  if (selectedQuestionIds.value.has(question.id)) return
  form.questionItems.push({
    questionId: question.id,
    sortNo: form.questionItems.length + 1,
    score: question.defaultScore,
    requiredFlag: 1,
    questionCode: question.questionCode,
    questionType: question.questionType,
    difficultyLevel: question.difficultyLevel,
    stem: question.stem
  })
  if (!form.subject) {
    form.subject = question.subject
    questionFilters.subject = question.subject
  }
  rebuildBlueprintFromSelection(true)
}

function removeQuestion(questionId: number) {
  form.questionItems = form.questionItems.filter((item) => item.questionId !== questionId)
  resequenceQuestionItems()
  rebuildBlueprintFromSelection(true)
}

function moveQuestion(index: number, direction: -1 | 1) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= form.questionItems.length) return
  const [current] = form.questionItems.splice(index, 1)
  form.questionItems.splice(targetIndex, 0, current)
  resequenceQuestionItems()
}

function distributeCounts(target: PaperRuleConfigItem[], totalCount: number, weights: number[]) {
  const next = target.map((item, index) => ({
    ...item,
    count: Math.floor(totalCount * (weights[index] ?? 0))
  }))
  let allocated = next.reduce((sum, item) => sum + Number(item.count || 0), 0)
  let cursor = 0
  while (allocated < totalCount) {
    next[cursor % next.length].count += 1
    allocated += 1
    cursor += 1
  }
  while (allocated > totalCount) {
    const item = next[cursor % next.length]
    if (item.count > 0) {
      item.count -= 1
      allocated -= 1
    }
    cursor += 1
  }
  target.splice(0, target.length, ...next)
}

function seedBlueprintCounts(totalCount: number) {
  distributeCounts(form.questionTypeConfigs, totalCount, [0.4, 0.2, 0.2, 0.2])
  distributeCounts(form.difficultyConfigs, totalCount, [0.3, 0.4, 0.3])
}

function rebuildBlueprintFromSelection(preserveScores: boolean) {
  const typeMap = new Map(form.questionTypeConfigs.map((item) => [item.code, item]))
  const difficultyMap = new Map(form.difficultyConfigs.map((item) => [item.code, item]))
  form.questionTypeConfigs = QUESTION_TYPE_OPTIONS.map((item) => {
    const matched = form.questionItems.filter((question) => question.questionType === item.value)
    return {
      code: item.value,
      label: item.label,
      count: matched.length,
      score: preserveScores ? typeMap.get(item.value)?.score ?? matched[0]?.score ?? defaultScoreByQuestionType(item.value) : matched[0]?.score ?? defaultScoreByQuestionType(item.value)
    }
  })
  form.difficultyConfigs = DIFFICULTY_OPTIONS.map((item) => {
    const matched = form.questionItems.filter((question) => question.difficultyLevel === item.value)
    return {
      code: item.value,
      label: item.label,
      count: matched.length,
      score: preserveScores ? difficultyMap.get(item.value)?.score ?? null : null
    }
  })
}

function pickRandom<T>(pool: T[], count: number) {
  const copy = [...pool]
  for (let index = copy.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1))
    ;[copy[index], copy[target]] = [copy[target], copy[index]]
  }
  return copy.slice(0, count)
}

function summarizeByQuestionType(items: PaperQuestionItem[]) {
  return QUESTION_TYPE_OPTIONS.map((option) => ({
    code: option.value,
    label: option.label,
    count: items.filter((item) => item.questionType === option.value).length
  })).filter((item) => item.count > 0)
}

function summarizeByDifficulty(items: PaperQuestionItem[]) {
  return DIFFICULTY_OPTIONS.map((option) => ({
    code: option.value,
    label: option.label,
    count: items.filter((item) => item.difficultyLevel === option.value).length
  })).filter((item) => item.count > 0)
}

function applyGeneratedSelection(selectedQuestions: QuestionBank[], mode: 'RANDOM' | 'STRATEGY') {
  const typeScoreMap = new Map(form.questionTypeConfigs.map((item) => [item.code, item.score ?? defaultScoreByQuestionType(item.code)]))
  const shuffled = [...selectedQuestions].sort(() => Math.random() - 0.5)
  form.questionItems = shuffled.map((question, index) => ({
    questionId: question.id,
    sortNo: index + 1,
    score: Number(typeScoreMap.get(question.questionType) ?? question.defaultScore),
    requiredFlag: 1,
    questionCode: question.questionCode,
    questionType: question.questionType,
    difficultyLevel: question.difficultyLevel,
    stem: question.stem
  }))
  form.assemblyMode = mode
  resequenceQuestionItems()
  rebuildBlueprintFromSelection(true)
  activeStep.value = 'preview'
}

function applyRandomAssembly() {
  const totalCount = blueprintTypeTotal.value || form.questionItems.length || 10
  if (!form.subject) return ElMessage.warning('请先选择试卷学科，再执行随机组卷')
  const pool = questions.value.filter((item) => item.subject === form.subject)
  if (pool.length < totalCount) {
    return ElMessage.error(`当前学科可用题目仅 ${pool.length} 道，无法随机抽取 ${totalCount} 道题`)
  }
  applyGeneratedSelection(pickRandom(pool, totalCount), 'RANDOM')
  ElMessage.success(`已随机生成 ${totalCount} 道题，可进入“卷面预览”确认`)
}

function buildStrategyCells(typeConfigs: PaperRuleConfigItem[], difficultyConfigs: PaperRuleConfigItem[], availability: Map<string, QuestionBank[]>) {
  return typeConfigs.flatMap((typeConfig) =>
    difficultyConfigs.map((difficultyConfig) => ({
      key: `${typeConfig.code}__${difficultyConfig.code}`,
      typeCode: typeConfig.code,
      difficultyCode: difficultyConfig.code,
      questions: availability.get(`${typeConfig.code}__${difficultyConfig.code}`) || []
    }))
  )
}

function allocateStrategyMatrix(
  cells: Array<{ key: string; typeCode: string; difficultyCode: string; questions: QuestionBank[] }>,
  typeConfigs: PaperRuleConfigItem[],
  difficultyConfigs: PaperRuleConfigItem[]
) {
  const rowRemaining = new Map(typeConfigs.map((item) => [item.code, item.count]))
  const colRemaining = new Map(difficultyConfigs.map((item) => [item.code, item.count]))
  const current = new Map<string, number>()

  function dfs(index: number): boolean {
    if (index === cells.length) {
      return [...rowRemaining.values()].every((value) => value === 0) && [...colRemaining.values()].every((value) => value === 0)
    }
    const cell = cells[index]
    const rowLeft = rowRemaining.get(cell.typeCode) || 0
    const colLeft = colRemaining.get(cell.difficultyCode) || 0
    const maxAssignable = Math.min(cell.questions.length, rowLeft, colLeft)
    for (let count = maxAssignable; count >= 0; count -= 1) {
      current.set(cell.key, count)
      rowRemaining.set(cell.typeCode, rowLeft - count)
      colRemaining.set(cell.difficultyCode, colLeft - count)
      if (dfs(index + 1)) return true
      rowRemaining.set(cell.typeCode, rowLeft)
      colRemaining.set(cell.difficultyCode, colLeft)
      current.delete(cell.key)
    }
    return false
  }

  return dfs(0) ? current : null
}

function buildShortageMessage(typeConfigs: PaperRuleConfigItem[], difficultyConfigs: PaperRuleConfigItem[]) {
  const typeShortages = typeConfigs.map((item) => {
    const actual = questions.value.filter((question) => question.subject === form.subject && question.questionType === item.code).length
    return actual < item.count ? `${item.label}仅有 ${actual} 道，目标 ${item.count} 道` : ''
  }).filter(Boolean)
  const difficultyShortages = difficultyConfigs.map((item) => {
    const actual = questions.value.filter((question) => question.subject === form.subject && question.difficultyLevel === item.code).length
    return actual < item.count ? `${item.label}题仅有 ${actual} 道，目标 ${item.count} 道` : ''
  }).filter(Boolean)
  if (typeShortages.length || difficultyShortages.length) {
    return ['当前题库无法满足策略组卷：', ...typeShortages, ...difficultyShortages].join('；')
  }
  return '当前题库无法满足所选题型与难度的组合条件，请调整策略后重试'
}

function applyStrategyAssembly() {
  if (!form.subject) return ElMessage.warning('请先选择试卷学科，再执行策略组卷')
  const totalCount = blueprintTypeTotal.value
  if (totalCount <= 0) return ElMessage.warning('请先填写题型分布数量')
  if (blueprintDifficultyTotal.value !== totalCount) {
    return ElMessage.error(`题型分布合计 ${totalCount} 题，但难度分布合计 ${blueprintDifficultyTotal.value} 题，请保持一致`)
  }
  const typeConfigs = form.questionTypeConfigs.filter((item) => item.count > 0)
  const difficultyConfigs = form.difficultyConfigs.filter((item) => item.count > 0)
  const availability = new Map<string, QuestionBank[]>()
  questions.value.filter((item) => item.subject === form.subject).forEach((question) => {
    const key = `${question.questionType}__${question.difficultyLevel}`
    const list = availability.get(key) || []
    list.push(question)
    availability.set(key, list)
  })
  const cells = buildStrategyCells(typeConfigs, difficultyConfigs, availability)
  const allocation = allocateStrategyMatrix(cells, typeConfigs, difficultyConfigs)
  if (!allocation) return ElMessage.error(buildShortageMessage(typeConfigs, difficultyConfigs))
  const selected: QuestionBank[] = []
  cells.forEach((cell) => {
    const count = allocation.get(cell.key) || 0
    if (count > 0) selected.push(...pickRandom(cell.questions, count))
  })
  if (selected.length !== totalCount) {
    return ElMessage.error(`策略组卷仅生成了 ${selected.length} 道题，少于目标 ${totalCount} 道，请调整条件后重试`)
  }
  applyGeneratedSelection(selected, 'STRATEGY')
  ElMessage.success(`已按题型与难度策略生成 ${selected.length} 道题，可进入“卷面预览”确认`)
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.questionItems.length === 0) {
      ElMessage.warning('请至少加入一道题目后再保存试卷')
      return
    }
    if (totalQuestionScore.value <= 0) {
      ElMessage.warning('试卷总分必须大于 0')
      return
    }
    if (form.passScore > totalQuestionScore.value) {
      ElMessage.warning('及格线不能高于试卷总分')
      return
    }
    saving.value = true
    try {
      const payload = { ...form, totalScore: totalQuestionScore.value, questionTypeConfigs: form.questionTypeConfigs, difficultyConfigs: form.difficultyConfigs }
      if (editingId.value) {
        await updatePaper(editingId.value, payload)
      } else {
        await createPaper(payload)
      }
      ElMessage.success(editingId.value ? '试卷已更新' : '试卷已创建')
      router.push('/exam/papers')
    } finally {
      saving.value = false
    }
  })
}

function backToList() {
  router.push('/exam/papers')
}

onMounted(initBuilder)
</script>

<template>
  <AppShellSection
    eyebrow="试卷创建"
    :title="editingId ? '编辑试卷' : '新建试卷'"
    description="按步骤完成试卷基础信息、组卷策略与卷面确认，确保建卷过程清晰、稳定、可追溯。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button @click="backToList">返回试卷列表</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存试卷</el-button>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="loading">
      <section class="builder-shell">
        <section class="panel-card builder-panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">步骤一</p>
              <h3>试卷基础信息</h3>
            </div>
            <div class="summary-strip">
              <div class="summary-chip"><strong>{{ form.questionItems.length }}</strong><span>已选题量</span></div>
              <div class="summary-chip"><strong>{{ totalQuestionScore }}</strong><span>试卷总分</span></div>
              <div class="summary-chip"><strong>{{ form.durationMinutes }}</strong><span>考试时长</span></div>
            </div>
          </div>
          <div class="field-grid field-grid--four">
            <el-form-item label="试卷编码" prop="paperCode"><el-input v-model="form.paperCode" placeholder="如：YW-2026-01" /></el-form-item>
            <el-form-item label="试卷名称" prop="paperName"><el-input v-model="form.paperName" placeholder="请输入正式卷面名称" /></el-form-item>
            <el-form-item label="学科" prop="subject">
              <el-select v-model="form.subject" filterable allow-create default-first-option>
                <el-option v-for="subject in availableSubjects" :key="subject" :label="subject" :value="subject" />
              </el-select>
            </el-form-item>
            <el-form-item label="组卷方式">
              <el-select v-model="form.assemblyMode">
                <el-option v-for="item in PAPER_MODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="试卷版本"><el-input v-model="form.paperVersion" placeholder="如：2026春季A卷" /></el-form-item>
            <el-form-item label="考试时长（分钟）"><el-input-number v-model="form.durationMinutes" :min="1" :max="300" /></el-form-item>
            <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="1" :max="500" /></el-form-item>
            <el-form-item label="发布状态">
              <el-select v-model="form.publishStatus">
                <el-option :value="0" label="草稿" />
                <el-option :value="1" label="已发布" />
              </el-select>
            </el-form-item>
          </div>
          <div class="field-grid field-grid--two">
            <el-form-item label="卷面说明"><el-input v-model="form.descriptionText" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="版本备注 / 教师备注"><el-input v-model="form.remarkText" type="textarea" :rows="3" /></el-form-item>
          </div>
          <div class="toggle-row">
            <el-switch v-model="form.shuffleEnabled" :active-value="1" :inactive-value="0" inline-prompt active-text="乱序" inactive-text="顺序" />
            <span class="muted">开启后，学生端会按稳定随机顺序展示题目与选项。</span>
          </div>
        </section>

        <section class="panel-card builder-panel">
          <div class="panel-head">
            <div>
              <p class="eyebrow">步骤二</p>
              <h3>组卷流程</h3>
            </div>
            <div class="step-tabs">
              <el-button :type="activeStep === 'manual' ? 'primary' : 'default'" @click="activeStep = 'manual'">手工选题</el-button>
              <el-button :type="activeStep === 'random' ? 'primary' : 'default'" @click="activeStep = 'random'">随机组卷</el-button>
              <el-button :type="activeStep === 'strategy' ? 'primary' : 'default'" @click="activeStep = 'strategy'">策略组卷</el-button>
              <el-button :type="activeStep === 'preview' ? 'primary' : 'default'" @click="activeStep = 'preview'">卷面预览</el-button>
            </div>
          </div>

          <template v-if="activeStep === 'manual'">
            <div class="panel-head"><span class="muted">{{ poolSummaryText }}</span></div>
            <div class="filter-grid">
              <el-select v-model="questionFilters.subject" clearable placeholder="按学科"><el-option v-for="subject in availableSubjects" :key="subject" :label="subject" :value="subject" /></el-select>
              <el-select v-model="questionFilters.questionType" clearable placeholder="按题型"><el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select>
              <el-select v-model="questionFilters.difficultyLevel" clearable placeholder="按难度"><el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" /></el-select>
              <el-input v-model="questionFilters.keyword" clearable placeholder="按题号、题干或知识点搜索" />
            </div>
            <div class="builder-grid">
              <section class="panel-card builder-panel">
                <strong>可选题目</strong>
                <div class="question-pool">
                  <article v-for="question in filteredQuestions" :key="question.id" class="question-chip" :class="{ selected: selectedQuestionIds.has(question.id) }">
                    <div class="question-chip-body">
                      <div class="question-chip-head">
                        <strong>{{ question.questionCode }}</strong>
                        <div class="tag-cluster">
                          <el-tag size="small" type="info">{{ labelQuestionType(question.questionType) }}</el-tag>
                          <el-tag size="small" type="warning">{{ labelDifficulty(question.difficultyLevel) }}</el-tag>
                          <el-tag size="small">{{ question.defaultScore }} 分</el-tag>
                        </div>
                      </div>
                      <p>{{ question.stem }}</p>
                    </div>
                    <el-button :disabled="selectedQuestionIds.has(question.id)" type="primary" plain @click="addQuestion(question)">{{ selectedQuestionIds.has(question.id) ? '已加入' : '加入试卷' }}</el-button>
                  </article>
                </div>
              </section>
              <section class="panel-card builder-panel">
                <strong>当前卷面</strong>
                <div class="composition-list">
                  <article v-for="(item, index) in form.questionItems" :key="item.questionId" class="composition-item">
                    <div class="composition-copy">
                      <div class="question-chip-head">
                        <strong>{{ item.sortNo }}. {{ item.questionCode }}</strong>
                        <div class="tag-cluster">
                          <el-tag size="small" type="info">{{ labelQuestionType(item.questionType) }}</el-tag>
                          <el-tag size="small" type="warning">{{ labelDifficulty(item.difficultyLevel) }}</el-tag>
                        </div>
                      </div>
                      <p>{{ item.stem }}</p>
                    </div>
                    <div class="composition-actions">
                      <el-input-number v-model="item.score" :min="1" :max="100" />
                      <el-switch v-model="item.requiredFlag" :active-value="1" :inactive-value="0" inline-prompt active-text="必答" inactive-text="选答" />
                      <el-button circle @click="moveQuestion(index, -1)" :disabled="index === 0">↑</el-button>
                      <el-button circle @click="moveQuestion(index, 1)" :disabled="index === form.questionItems.length - 1">↓</el-button>
                      <el-button link type="danger" @click="removeQuestion(item.questionId)">移除</el-button>
                    </div>
                  </article>
                </div>
              </section>
            </div>
          </template>

          <template v-else-if="activeStep === 'random'">
            <div class="hint-card"><strong>随机组卷</strong><p>按当前学科和题量目标抽题，适合快速生成日常测验卷。</p></div>
            <div class="blueprint-grid">
              <section class="blueprint-card">
                <div class="blueprint-card-head"><strong>题型分布</strong><span class="muted">合计 {{ blueprintTypeTotal }} 题</span></div>
                <div class="blueprint-list">
                  <article v-for="item in form.questionTypeConfigs" :key="item.code" class="blueprint-row">
                    <div><strong>{{ item.label }}</strong><p class="muted">随机后沿用该题型默认分值</p></div>
                    <div class="blueprint-controls"><el-input-number v-model="item.count" :min="0" :max="40" /><el-input-number v-model="item.score" :min="0" :max="100" /></div>
                  </article>
                </div>
              </section>
            </div>
            <div class="panel-actions"><el-button @click="seedBlueprintCounts(Math.max(form.questionItems.length, 10))">均衡填充</el-button><el-button type="primary" @click="applyRandomAssembly">执行随机组卷</el-button></div>
          </template>

          <template v-else-if="activeStep === 'strategy'">
            <div class="hint-card"><strong>策略组卷</strong><p>先设定题型和难度目标，再自动组合出满足约束条件的卷面；若题库不足，系统会阻止保存并给出中文提示。</p></div>
            <div class="blueprint-grid">
              <section class="blueprint-card">
                <div class="blueprint-card-head"><strong>题型分布</strong><span class="muted">合计 {{ blueprintTypeTotal }} 题</span></div>
                <div class="blueprint-list">
                  <article v-for="item in form.questionTypeConfigs" :key="item.code" class="blueprint-row">
                    <div><strong>{{ item.label }}</strong><p class="muted">按题型设置数量与默认分值</p></div>
                    <div class="blueprint-controls"><el-input-number v-model="item.count" :min="0" :max="40" /><el-input-number v-model="item.score" :min="0" :max="100" /></div>
                  </article>
                </div>
              </section>
              <section class="blueprint-card">
                <div class="blueprint-card-head"><strong>难度分布</strong><span class="muted">合计 {{ blueprintDifficultyTotal }} 题</span></div>
                <div class="blueprint-list">
                  <article v-for="item in form.difficultyConfigs" :key="item.code" class="blueprint-row">
                    <div><strong>{{ item.label }}</strong><p class="muted">总题量需与题型分布保持一致</p></div>
                    <div class="blueprint-controls"><el-input-number v-model="item.count" :min="0" :max="40" /></div>
                  </article>
                </div>
              </section>
            </div>
            <div class="panel-actions"><el-button @click="seedBlueprintCounts(Math.max(form.questionItems.length, 10))">均衡填充</el-button><el-button type="primary" @click="applyStrategyAssembly">执行策略组卷</el-button></div>
          </template>

          <template v-else>
            <section class="preview-grid">
              <article class="panel-card preview-card"><strong>基础信息</strong><p>{{ form.paperName || '未填写试卷名称' }} · {{ form.subject || '未选学科' }} · {{ labelPaperMode(form.assemblyMode) }}</p><p>版本：{{ form.paperVersion || '未填写' }} / 及格线：{{ form.passScore }} / 时长：{{ form.durationMinutes }} 分钟</p></article>
              <article class="panel-card preview-card"><strong>卷面统计</strong><p>题量：{{ form.questionItems.length }} 题 / 总分：{{ totalQuestionScore }} 分 / 乱序：{{ form.shuffleEnabled === 1 ? '开启' : '关闭' }}</p><p>题型：{{ typeSummary.map((item) => `${item.label} ${item.count} 题`).join(' / ') || '暂无' }}</p></article>
            </section>
            <section class="panel-card builder-panel">
              <div class="panel-head"><strong>卷面预览</strong><span class="muted">保存前最后确认题序、分值和题型分布</span></div>
              <div class="composition-list">
                <article v-for="item in form.questionItems" :key="item.questionId" class="composition-item">
                  <div class="composition-copy">
                    <div class="question-chip-head">
                      <strong>{{ item.sortNo }}. {{ item.questionCode }}</strong>
                      <div class="tag-cluster">
                        <el-tag size="small" type="info">{{ labelQuestionType(item.questionType) }}</el-tag>
                        <el-tag size="small" type="warning">{{ labelDifficulty(item.difficultyLevel) }}</el-tag>
                        <el-tag size="small">{{ item.score }} 分</el-tag>
                      </div>
                    </div>
                    <p>{{ item.stem }}</p>
                  </div>
                </article>
              </div>
            </section>
          </template>
        </section>
      </section>
    </el-form>
  </AppShellSection>
</template>

<style scoped>
.hero-actions,
.panel-head,
.summary-strip,
.step-tabs,
.panel-actions,
.question-chip-head,
.tag-cluster,
.blueprint-card-head,
.blueprint-row,
.blueprint-controls,
.composition-actions,
.toggle-row {
  display: flex;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.panel-head,
.question-chip-head,
.blueprint-card-head,
.blueprint-row {
  justify-content: space-between;
  align-items: flex-start;
}

.hero-actions {
  margin-top: 1rem;
}

.builder-shell,
.builder-panel,
.question-pool,
.composition-list,
.blueprint-list {
  display: grid;
  gap: 1rem;
}

.builder-grid,
.blueprint-grid,
.preview-grid,
.field-grid--two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.field-grid {
  display: grid;
  gap: 0.8rem;
}

.field-grid--four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-chip,
.hint-card,
.blueprint-card,
.question-chip,
.composition-item,
.preview-card {
  border: 1px solid color-mix(in oklch, var(--line) 78%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
  padding: 0.95rem 1rem;
}

.summary-chip strong {
  font-size: 1.35rem;
  color: var(--brand-deep);
}

.panel-head h3,
.blueprint-card-head strong {
  margin: 0.25rem 0 0;
  font-family: 'Literata', Georgia, serif;
}

.hint-card p,
.question-chip-body p,
.composition-copy p,
.preview-card p {
  margin: 0.4rem 0 0;
  line-height: 1.6;
  color: var(--muted);
}

.question-pool {
  max-height: 38rem;
  overflow: auto;
}

.question-chip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.9rem;
  align-items: start;
}

.question-chip.selected {
  background: color-mix(in oklch, var(--brand) 10%, white);
}

.composition-actions,
.toggle-row {
  align-items: center;
}

@media (max-width: 1180px) {
  .field-grid--four,
  .field-grid--two,
  .builder-grid,
  .blueprint-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
