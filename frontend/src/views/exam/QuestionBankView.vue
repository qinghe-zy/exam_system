<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { autoGroupQuestionsByKnowledgePoint, deleteQuestion, exportQuestions, fetchQuestions, importQuestions } from '../../api/exam'
import { DIFFICULTY_OPTIONS, QUESTION_TYPE_OPTIONS } from '../../constants/exam'
import { usePermission } from '../../hooks/usePermission'
import type { QuestionBank } from '../../types/exam'
import { labelDifficulty, labelQuestionType, labelReviewStatus } from '../../utils/labels'

type AttachmentPreview = { name: string; url: string; type?: string }

const router = useRouter()
const { hasPermission } = usePermission()
const loading = ref(false)
const questions = ref<QuestionBank[]>([])
const importDialogVisible = ref(false)
const importText = ref('')
const knowledgeDialogVisible = ref(false)
const knowledgeLoading = ref(false)
const knowledgeResults = ref<QuestionBank[]>([])

const filters = reactive({
  keyword: '',
  questionType: '',
  difficultyLevel: ''
})

const knowledgeForm = reactive({
  subject: '',
  difficultyLevel: '',
  questionType: '',
  quotaText: '现代文阅读,2\n古诗文默写,2'
})

const filteredQuestions = computed(() =>
  questions.value.filter((item) => {
    if (filters.keyword) {
      const source = `${item.questionCode} ${item.stem} ${item.knowledgePoint || ''} ${item.subject}`.toLowerCase()
      if (!source.includes(filters.keyword.toLowerCase())) return false
    }
    if (filters.questionType && item.questionType !== filters.questionType) return false
    if (filters.difficultyLevel && item.difficultyLevel !== filters.difficultyLevel) return false
    return true
  })
)

async function loadData() {
  loading.value = true
  try {
    questions.value = await fetchQuestions()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  router.push('/exam/questions/create')
}

function openEdit(row: QuestionBank) {
  router.push(`/exam/questions/${row.id}/edit`)
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

function openKnowledgeGroup() {
  knowledgeForm.subject = questions.value[0]?.subject || ''
  knowledgeForm.difficultyLevel = ''
  knowledgeForm.questionType = ''
  knowledgeDialogVisible.value = true
  knowledgeResults.value = []
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
    title="题库总览与题目管理"
    description="统一检索题目、导入导出题库，并进入独立编辑页维护题干、答案、知识点与附件信息。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button v-if="hasPermission('exam:question:knowledge:auto-group')" @click="openKnowledgeGroup">按知识点自动组题</el-button>
        <el-button @click="handleExport">导出 JSON</el-button>
        <el-button v-if="hasPermission('exam:question:import')" @click="importDialogVisible = true">导入 JSON</el-button>
        <el-button v-if="hasPermission('exam:question:create')" type="primary" @click="openCreate">新建题目</el-button>
      </div>
    </template>

    <section class="panel-card filter-card">
      <div class="filter-grid">
        <el-input v-model="filters.keyword" placeholder="按编码、题干、知识点搜索" clearable />
        <el-select v-model="filters.questionType" placeholder="题型" clearable>
          <el-option v-for="item in QUESTION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.difficultyLevel" placeholder="难度" clearable>
          <el-option v-for="item in DIFFICULTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </section>

    <section class="panel-card section-card">
      <el-table :data="filteredQuestions" v-loading="loading">
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

    <el-dialog v-model="importDialogVisible" title="导入题目 JSON" width="min(900px, 96vw)">
      <p class="muted">请粘贴题目数组 JSON，字段结构与导出结果一致。</p>
      <el-input v-model="importText" type="textarea" :rows="12" />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="knowledgeDialogVisible" title="按知识点自动组题" width="min(860px, 96vw)">
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

.section-card,
.filter-card {
  padding: 1rem;
}

.filter-grid,
.grid-three {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

@media (max-width: 980px) {
  .filter-grid,
  .grid-three {
    grid-template-columns: 1fr;
  }
}
</style>
