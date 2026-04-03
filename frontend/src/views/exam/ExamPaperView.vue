<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { DIFFICULTY_OPTIONS, PAPER_MODE_OPTIONS, QUESTION_TYPE_OPTIONS } from '../../constants/exam'
import { createPaper, deletePaper, fetchPapers, fetchQuestions, updatePaper } from '../../api/exam'
import type { ExamPaper, PaperQuestionItem, PaperRuleConfigItem, QuestionBank } from '../../types/exam'
import { labelDifficulty, labelPaperMode, labelQuestionType } from '../../utils/labels'

const loading = ref(false)
const papers = ref<ExamPaper[]>([])
const questions = ref<QuestionBank[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

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

form.questionTypeConfigs = createEmptyTypeConfigs()
form.difficultyConfigs = createEmptyDifficultyConfigs()

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
const typeSummary = computed(() => summarizeByQuestionType(form.questionItems))
const difficultySummary = computed(() => summarizeByDifficulty(form.questionItems))
const blueprintTypeTotal = computed(() => form.questionTypeConfigs.reduce((sum, item) => sum + Number(item.count || 0), 0))
const blueprintDifficultyTotal = computed(() => form.difficultyConfigs.reduce((sum, item) => sum + Number(item.count || 0), 0))
const poolSummaryText = computed(() => {
  if (!questionFilters.subject) return `当前题库共 ${questions.value.length} 道题，可按学科、题型、难度快速筛题。`
  const sameSubjectCount = questions.value.filter((item) => item.subject === questionFilters.subject).length
  return `当前学科 ${questionFilters.subject} 共 ${sameSubjectCount} 道题，筛选结果 ${filteredQuestions.value.length} 道。`
})

async function loadData() {
  loading.value = true
  try {
    const [paperList, questionList] = await Promise.all([fetchPapers(), fetchQuestions()])
    papers.value = paperList
    questions.value = questionList
  } finally {
    loading.value = false
  }
}

async function ensureQuestionPoolLoaded() {
  if (questions.value.length > 0) return
  questions.value = await fetchQuestions()
}

function resetFilters() {
  questionFilters.subject = form.subject
  questionFilters.questionType = ''
  questionFilters.difficultyLevel = ''
  questionFilters.keyword = ''
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
  editingId.value = null
  resetFilters()
}

function cloneConfigs(configs: PaperRuleConfigItem[] | undefined, fallback: PaperRuleConfigItem[]) {
  if (!configs || configs.length === 0) {
    return fallback.map((item) => ({ ...item }))
  }
  const fallbackMap = new Map(fallback.map((item) => [item.code, item]))
  return configs.map((item) => ({
    code: item.code,
    label: item.label || fallbackMap.get(item.code)?.label || item.code,
    count: item.count ?? 0,
    score: item.score ?? fallbackMap.get(item.code)?.score ?? null
  }))
}

async function openCreate(preferredMode: 'MANUAL' | 'RANDOM' | 'STRATEGY' = 'MANUAL') {
  dialogMode.value = 'create'
  resetForm()
  await ensureQuestionPoolLoaded()
  if (!form.subject && availableSubjects.value.length > 0) {
    form.subject = availableSubjects.value[0]
  }
  questionFilters.subject = form.subject
  form.assemblyMode = preferredMode
  if (preferredMode !== 'MANUAL') {
    seedBlueprintCounts(10)
  }
  dialogVisible.value = true
}

async function openEdit(row: ExamPaper) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  await ensureQuestionPoolLoaded()
  Object.assign(form, {
    paperCode: row.paperCode,
    paperName: row.paperName,
    subject: row.subject,
    assemblyMode: row.assemblyMode,
    descriptionText: row.descriptionText || '',
    paperVersion: row.paperVersion || '2026春季A卷',
    remarkText: row.remarkText || '',
    durationMinutes: row.durationMinutes,
    passScore: row.passScore,
    shuffleEnabled: row.shuffleEnabled ?? 0,
    questionTypeConfigs: cloneConfigs(row.questionTypeConfigs, createEmptyTypeConfigs()),
    difficultyConfigs: cloneConfigs(row.difficultyConfigs, createEmptyDifficultyConfigs()),
    publishStatus: row.publishStatus,
    questionItems: row.questionItems.map((item) => ({ ...item }))
  })
  questionFilters.subject = row.subject
  rebuildBlueprintFromSelection(true)
  dialogVisible.value = true
}

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

function applyGeneratedSelection(selectedQuestions: QuestionBank[]) {
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
  resequenceQuestionItems()
  rebuildBlueprintFromSelection(true)
}

function applyRandomAssembly() {
  const totalCount = blueprintTypeTotal.value || form.questionItems.length || 10
  if (!form.subject) {
    ElMessage.warning('请先选择试卷学科，再执行随机组卷')
    return
  }
  const pool = questions.value.filter((item) => item.subject === form.subject)
  if (pool.length < totalCount) {
    ElMessage.error(`当前学科可用题目仅 ${pool.length} 道，无法随机抽取 ${totalCount} 道题`)
    return
  }
  form.assemblyMode = 'RANDOM'
  applyGeneratedSelection(pickRandom(pool, totalCount))
  ElMessage.success(`已随机生成 ${totalCount} 道题，可继续调整分值后保存试卷`)
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
      if (dfs(index + 1)) {
        return true
      }
      rowRemaining.set(cell.typeCode, rowLeft)
      colRemaining.set(cell.difficultyCode, colLeft)
      current.delete(cell.key)
    }
    return false
  }

  return dfs(0) ? current : null
}

function buildShortageMessage(typeConfigs: PaperRuleConfigItem[], difficultyConfigs: PaperRuleConfigItem[]) {
  const typeShortages = typeConfigs
    .map((item) => {
      const actual = questions.value.filter((question) => question.subject === form.subject && question.questionType === item.code).length
      return actual < item.count ? `${item.label}仅有 ${actual} 道，目标 ${item.count} 道` : ''
    })
    .filter(Boolean)
  const difficultyShortages = difficultyConfigs
    .map((item) => {
      const actual = questions.value.filter((question) => question.subject === form.subject && question.difficultyLevel === item.code).length
      return actual < item.count ? `${item.label}题仅有 ${actual} 道，目标 ${item.count} 道` : ''
    })
    .filter(Boolean)
  if (typeShortages.length || difficultyShortages.length) {
    return ['当前题库无法满足策略组卷：', ...typeShortages, ...difficultyShortages].join('；')
  }
  return '当前题库无法满足所选题型与难度的组合条件，请调整策略后重试'
}

function applyStrategyAssembly() {
  if (!form.subject) {
    ElMessage.warning('请先选择试卷学科，再执行策略组卷')
    return
  }
  const totalCount = blueprintTypeTotal.value
  if (totalCount <= 0) {
    ElMessage.warning('请先填写题型分布数量')
    return
  }
  if (blueprintDifficultyTotal.value !== totalCount) {
    ElMessage.error(`题型分布合计 ${totalCount} 题，但难度分布合计 ${blueprintDifficultyTotal.value} 题，请保持一致`)
    return
  }
  const pool = questions.value.filter((item) => item.subject === form.subject)
  const typeConfigs = form.questionTypeConfigs.filter((item) => item.count > 0)
  const difficultyConfigs = form.difficultyConfigs.filter((item) => item.count > 0)
  const availability = new Map<string, QuestionBank[]>()
  for (const question of pool) {
    const key = `${question.questionType}__${question.difficultyLevel}`
    const list = availability.get(key) || []
    list.push(question)
    availability.set(key, list)
  }
  const cells = buildStrategyCells(typeConfigs, difficultyConfigs, availability)
  const allocation = allocateStrategyMatrix(cells, typeConfigs, difficultyConfigs)
  if (!allocation) {
    ElMessage.error(buildShortageMessage(typeConfigs, difficultyConfigs))
    return
  }
  const selected: QuestionBank[] = []
  for (const cell of cells) {
    const count = allocation.get(cell.key) || 0
    if (count > 0) {
      selected.push(...pickRandom(cell.questions, count))
    }
  }
  if (selected.length !== totalCount) {
    ElMessage.error(`策略组卷仅生成了 ${selected.length} 道题，少于目标 ${totalCount} 道，请调整条件后重试`)
    return
  }
  form.assemblyMode = 'STRATEGY'
  applyGeneratedSelection(selected)
  ElMessage.success(`已按题型与难度策略生成 ${selected.length} 道题，可直接保存为试卷`)
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
    const payload = {
      ...form,
      totalScore: totalQuestionScore.value,
      questionTypeConfigs: form.questionTypeConfigs,
      difficultyConfigs: form.difficultyConfigs
    }
    if (dialogMode.value === 'create') {
      await createPaper(payload)
    } else if (editingId.value) {
      await updatePaper(editingId.value, payload)
    }
    ElMessage.success(dialogMode.value === 'create' ? '试卷已保存，可直接用于考试发布' : '试卷已更新')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该试卷？删除后考试发布将无法继续引用该卷。', '提示', { type: 'warning' })
  await deletePaper(id)
  ElMessage.success('试卷已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="试卷管理"
    title="试卷工作台：筛题、组卷、配置一体完成"
    description="在同一工作台内完成试卷基础参数、题型与难度蓝图、题库筛选、卷面组成、分值与乱序设置。保存后的试卷可直接进入考试发布环节。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button @click="openCreate('RANDOM')">快速随机组卷</el-button>
        <el-button @click="openCreate('STRATEGY')">快速策略组卷</el-button>
        <el-button type="primary" @click="openCreate()">新建试卷</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="papers" v-loading="loading">
        <el-table-column prop="paperCode" label="试卷编码" min-width="140" />
        <el-table-column prop="paperName" label="试卷名称" min-width="220" />
        <el-table-column prop="subject" label="学科" min-width="120" />
        <el-table-column prop="paperVersion" label="版本" min-width="120" />
        <el-table-column label="组卷方式" min-width="120">
          <template #default="{ row }">{{ labelPaperMode(row.assemblyMode) }}</template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题量" min-width="90" />
        <el-table-column prop="totalScore" label="总分" min-width="90" />
        <el-table-column prop="passScore" label="及格线" min-width="90" />
        <el-table-column label="乱序" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.shuffleEnabled === 1 ? 'warning' : 'info'">{{ row.shuffleEnabled === 1 ? '开启' : '关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">{{ row.publishStatus === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeItem(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建试卷' : '编辑试卷'" width="min(1380px, 98vw)" top="3vh" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <section class="builder-shell">
          <div class="builder-main">
            <section class="panel-card builder-panel">
              <div class="panel-head">
                <div>
                  <p class="eyebrow">基础参数</p>
                  <h3>试卷基本信息与发布属性</h3>
                </div>
                <div class="summary-strip">
                  <div class="summary-chip">
                    <strong>{{ form.questionItems.length }}</strong>
                    <span>已选题量</span>
                  </div>
                  <div class="summary-chip">
                    <strong>{{ totalQuestionScore }}</strong>
                    <span>试卷总分</span>
                  </div>
                  <div class="summary-chip">
                    <strong>{{ form.durationMinutes }}</strong>
                    <span>考试时长（分钟）</span>
                  </div>
                </div>
              </div>

              <div class="field-grid field-grid--four">
                <el-form-item label="试卷编码" prop="paperCode"><el-input v-model="form.paperCode" placeholder="如：YW-2026-01" /></el-form-item>
                <el-form-item label="试卷名称" prop="paperName"><el-input v-model="form.paperName" placeholder="请输入正式卷面名称" /></el-form-item>
                <el-form-item label="学科" prop="subject">
                  <el-select v-model="form.subject" filterable allow-create default-first-option @change="questionFilters.subject = form.subject">
                    <el-option v-for="subject in availableSubjects" :key="subject" :label="subject" :value="subject" />
                  </el-select>
                </el-form-item>
                <el-form-item label="组卷方式">
                  <el-select v-model="form.assemblyMode">
                    <el-option v-for="item in PAPER_MODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="试卷版本">
                  <el-input v-model="form.paperVersion" placeholder="如：2026春季A卷" />
                </el-form-item>
                <el-form-item label="考试时长（分钟）">
                  <el-input-number v-model="form.durationMinutes" :min="1" :max="300" />
                </el-form-item>
                <el-form-item label="及格线">
                  <el-input-number v-model="form.passScore" :min="1" :max="500" />
                </el-form-item>
                <el-form-item label="发布状态">
                  <el-select v-model="form.publishStatus">
                    <el-option :value="0" label="草稿" />
                    <el-option :value="1" label="已发布" />
                  </el-select>
                </el-form-item>
              </div>

              <div class="field-grid field-grid--two">
                <el-form-item label="卷面说明">
                  <el-input v-model="form.descriptionText" type="textarea" :rows="3" placeholder="说明适用班级、考试目标、答题要求等" />
                </el-form-item>
                <el-form-item label="版本备注 / 教师备注">
                  <el-input v-model="form.remarkText" type="textarea" :rows="3" placeholder="记录版本差异、调整原因、复用建议等" />
                </el-form-item>
              </div>

              <div class="toggle-row">
                <el-switch v-model="form.shuffleEnabled" :active-value="1" :inactive-value="0" inline-prompt active-text="乱序" inactive-text="顺序" />
                <span class="muted">开启后，学生端会按稳定随机顺序展示题目与选项，减少相邻考生卷面一致性。</span>
              </div>
            </section>

            <section class="panel-card builder-panel">
              <div class="panel-head">
                <div>
                  <p class="eyebrow">组卷蓝图</p>
                  <h3>题型分布、难度分布与分值策略</h3>
                </div>
                <div class="panel-actions">
                  <el-button @click="seedBlueprintCounts(Math.max(form.questionItems.length, 10))">均衡填充</el-button>
                  <el-button @click="applyRandomAssembly">随机组卷</el-button>
                  <el-button type="primary" @click="applyStrategyAssembly">按策略组卷</el-button>
                </div>
              </div>
              <p class="muted helper-text">
                先填写题型与难度目标，再执行策略组卷；随机组卷会按照题量直接抽题，但仍会沿用每种题型设置的默认分值。
              </p>

              <div class="blueprint-grid">
                <section class="blueprint-card">
                  <div class="blueprint-card-head">
                    <strong>题型分布</strong>
                    <span class="muted">合计 {{ blueprintTypeTotal }} 题</span>
                  </div>
                  <div class="blueprint-list">
                    <article v-for="item in form.questionTypeConfigs" :key="item.code" class="blueprint-row">
                      <div>
                        <strong>{{ item.label }}</strong>
                        <p class="muted">按题型设置数量与默认分值</p>
                      </div>
                      <div class="blueprint-controls">
                        <el-input-number v-model="item.count" :min="0" :max="40" />
                        <el-input-number v-model="item.score" :min="0" :max="100" />
                      </div>
                    </article>
                  </div>
                </section>

                <section class="blueprint-card">
                  <div class="blueprint-card-head">
                    <strong>难度分布</strong>
                    <span class="muted">合计 {{ blueprintDifficultyTotal }} 题</span>
                  </div>
                  <div class="blueprint-list">
                    <article v-for="item in form.difficultyConfigs" :key="item.code" class="blueprint-row">
                      <div>
                        <strong>{{ item.label }}</strong>
                        <p class="muted">题量必须与题型分布总数一致</p>
                      </div>
                      <div class="blueprint-controls">
                        <el-input-number v-model="item.count" :min="0" :max="40" />
                      </div>
                    </article>
                  </div>
                </section>
              </div>
            </section>

            <section class="builder-grid">
              <section class="panel-card builder-panel">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">题库筛题</p>
                    <h3>从当前题库筛选可加入题目</h3>
                  </div>
                  <span class="muted">{{ poolSummaryText }}</span>
                </div>
                <div class="filter-grid">
                  <el-select v-model="questionFilters.subject" clearable placeholder="按学科">
                    <el-option v-for="subject in availableSubjects" :key="subject" :label="subject" :value="subject" />
                  </el-select>
                  <el-select v-model="questionFilters.questionType" clearable placeholder="按题型">
                    <el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                  <el-select v-model="questionFilters.difficultyLevel" clearable placeholder="按难度">
                    <el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                  <el-input v-model="questionFilters.keyword" clearable placeholder="按题号、题干或知识点搜索" />
                </div>
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
                      <span class="muted">{{ question.knowledgePoint || '未填写知识点' }}</span>
                    </div>
                    <el-button :disabled="selectedQuestionIds.has(question.id)" type="primary" plain @click="addQuestion(question)">
                      {{ selectedQuestionIds.has(question.id) ? '已加入' : '加入试卷' }}
                    </el-button>
                  </article>
                </div>
              </section>

              <section class="panel-card builder-panel">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">卷面组成</p>
                    <h3>题目顺序、分值与卷面统计</h3>
                  </div>
                  <span class="muted">共 {{ form.questionItems.length }} 题，{{ totalQuestionScore }} 分</span>
                </div>

                <div class="stat-row">
                  <div class="stat-card">
                    <strong>题型分布</strong>
                    <p>{{ typeSummary.map((item) => `${item.label} ${item.count} 题`).join(' / ') || '尚未选择题目' }}</p>
                  </div>
                  <div class="stat-card">
                    <strong>难度分布</strong>
                    <p>{{ difficultySummary.map((item) => `${item.label} ${item.count} 题`).join(' / ') || '尚未选择题目' }}</p>
                  </div>
                </div>

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
            </section>
          </div>
        </section>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存试卷</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.hero-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.section-card {
  padding: 1rem;
}

.builder-shell,
.builder-main,
.builder-panel,
.question-pool,
.composition-list,
.blueprint-list {
  display: grid;
  gap: 1rem;
}

.builder-grid,
.blueprint-grid,
.stat-row,
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

.panel-head,
.panel-actions,
.summary-strip,
.question-chip-head,
.tag-cluster,
.blueprint-card-head,
.blueprint-row,
.blueprint-controls,
.composition-actions,
.toggle-row,
.filter-grid {
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

.summary-chip,
.blueprint-card,
.stat-card,
.question-chip,
.composition-item {
  border: 1px solid color-mix(in oklch, var(--line) 78%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
}

.summary-chip,
.stat-card,
.blueprint-card,
.question-chip,
.composition-item {
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

.helper-text,
.question-chip-body p,
.composition-copy p,
.stat-card p {
  margin: 0;
  line-height: 1.6;
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

.question-pool {
  max-height: 32rem;
  overflow: auto;
  padding-right: 0.25rem;
}

.toggle-row {
  align-items: center;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.composition-actions {
  align-items: center;
}

@media (max-width: 1180px) {
  .field-grid--four,
  .field-grid--two,
  .builder-grid,
  .blueprint-grid,
  .stat-row,
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
