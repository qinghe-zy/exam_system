<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { createExamPlan, deleteExamPlan, exportExamPlanSignInSheetCsv, fetchExamPlans, fetchPapers, updateExamPlan } from '../../api/exam'
import { fetchAssignableCandidates, type SystemUser } from '../../api/system'
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
  examMode: 'NORMAL',
  batchLabel: '',
  examRoom: '',
  sourceExamPlanId: undefined,
  sourceExamName: '',
  paperId: 0,
  startTime: '',
  endTime: '',
  durationMinutes: 90,
  passScore: 60,
  candidateScope: 'ASSIGNED',
  attemptLimit: 1,
  examPassword: '',
  lateEntryMinutes: 0,
  signInRequired: 0,
  signInStartMinutes: 60,
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
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  sourceExamPlanId: [{
    validator: (_rule, value, callback) => {
      if (form.examMode !== 'NORMAL' && !value) {
        callback(new Error('补考、缓考或重考必须关联原考试'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

const selectedPaper = computed(() => papers.value.find((item) => item.id === form.paperId))
const availableSourcePlans = computed(() => plans.value.filter((item) => item.id !== editingId.value))
const selectedSourcePlan = computed(() => availableSourcePlans.value.find((item) => item.id === form.sourceExamPlanId))
const selectedCandidates = computed(() => users.value.filter((user) => form.candidateUserIds.includes(user.id)))
const effectiveEntryDeadlineText = computed(() => {
  if (!form.startTime || !form.endTime) return '请先设置考试开始和结束时间'
  const start = new Date(form.startTime)
  const end = new Date(form.endTime)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '请检查时间格式'
  const lateDeadline = form.lateEntryMinutes > 0 ? new Date(start.getTime() + form.lateEntryMinutes * 60_000) : end
  const actualDeadline = lateDeadline.getTime() < end.getTime() ? lateDeadline : end
  return `${formatDateTime(start)} 至 ${formatDateTime(actualDeadline)}`
})
const answerRuleText = computed(() => {
  if (!form.endTime) return '系统会在考试窗口结束时自动交卷'
  const end = new Date(form.endTime)
  if (Number.isNaN(end.getTime())) return '系统会在考试窗口结束时自动交卷'
  return `学生进入后，实际可作答时长取“考试时长 ${form.durationMinutes} 分钟”和“距离 ${formatDateTime(end)} 的剩余时间”中的较短值。`
})
const signInRuleText = computed(() => {
  if (form.signInRequired !== 1) return '当前未启用签到，学生可在允许进入窗口内直接进入考试。'
  if (!form.startTime) return '请先设置考试开始时间'
  const start = new Date(form.startTime)
  if (Number.isNaN(start.getTime())) return '请检查考试开始时间'
  const open = new Date(start.getTime() - form.signInStartMinutes * 60_000)
  return `签到将于 ${formatDateTime(open)} 开放，并持续到最晚进入时间。`
})

const examModeTagType: Record<string, 'info' | 'warning'> = {
  NORMAL: 'info',
  MAKEUP: 'warning',
  DEFERRED: 'warning',
  RETAKE: 'warning'
}

async function loadData() {
  loading.value = true
  try {
    const [planList, paperList, userList] = await Promise.all([fetchExamPlans(), fetchPapers(), fetchAssignableCandidates()])
    plans.value = planList
    papers.value = paperList.filter((item) => item.publishStatus === 1)
    users.value = userList
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    examCode: '',
    examName: '',
    examMode: 'NORMAL',
    batchLabel: '',
    examRoom: '',
    sourceExamPlanId: undefined,
    sourceExamName: '',
    paperId: 0,
    startTime: '',
    endTime: '',
    durationMinutes: 90,
    passScore: 60,
    candidateScope: 'ASSIGNED',
    attemptLimit: 1,
    examPassword: '',
    lateEntryMinutes: 0,
    signInRequired: 0,
    signInStartMinutes: 60,
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
    examMode: row.examMode || 'NORMAL',
    batchLabel: row.batchLabel || '',
    examRoom: row.examRoom || '',
    sourceExamPlanId: row.sourceExamPlanId,
    sourceExamName: row.sourceExamName || '',
    paperId: row.paperId,
    startTime: row.startTime,
    endTime: row.endTime,
    durationMinutes: row.durationMinutes,
    passScore: row.passScore,
    candidateScope: row.candidateScope,
    attemptLimit: row.attemptLimit,
    examPassword: row.examPassword || '',
    lateEntryMinutes: row.lateEntryMinutes,
    signInRequired: row.signInRequired,
    signInStartMinutes: row.signInStartMinutes,
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

watch(
  () => form.paperId,
  (paperId) => {
    const paper = papers.value.find((item) => item.id === paperId)
    if (!paper) return
    if (dialogMode.value === 'create' || !form.examName) {
      form.durationMinutes = paper.durationMinutes
      form.passScore = paper.passScore
    }
  }
)

watch(
  () => form.examMode,
  (examMode) => {
    if (examMode === 'NORMAL') {
      form.sourceExamPlanId = undefined
      form.sourceExamName = ''
    }
  }
)

watch(
  selectedSourcePlan,
  (plan) => {
    form.sourceExamName = plan?.examName || ''
  }
)

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (new Date(form.endTime).getTime() <= new Date(form.startTime).getTime()) {
      ElMessage.error('考试结束时间必须晚于开始时间')
      return
    }
    if (form.examMode !== 'NORMAL' && !form.sourceExamPlanId) {
      ElMessage.error('补考、缓考或重考必须关联原考试')
      return
    }
    if (selectedPaper.value && form.passScore > selectedPaper.value.totalScore) {
      ElMessage.error('考试及格线不能高于试卷总分')
      return
    }
    if (form.candidateUserIds.length === 0) {
      ElMessage.warning('请至少选择一名考生')
      return
    }
    if (dialogMode.value === 'create') {
      await createExamPlan(form)
    } else if (editingId.value) {
      await updateExamPlan(editingId.value, form)
    }
    ElMessage.success(dialogMode.value === 'create' ? '考试已创建并保存' : '考试配置已更新')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该考试？删除后已分配的考生将无法继续进入该场考试。', '提示', { type: 'warning' })
  await deleteExamPlan(id)
  ElMessage.success('考试已删除')
  await loadData()
}

async function exportSignInSheet(id: number) {
  const csv = await exportExamPlanSignInSheetCsv(id)
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `exam-sign-in-sheet-${id}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('签到名单导出已开始')
}

function formatDateTime(value: string | Date) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function labelExamMode(value: string) {
  if (value === 'MAKEUP') return '补考'
  if (value === 'DEFERRED') return '缓考'
  if (value === 'RETAKE') return '重考'
  return '正常考试'
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="考试发布"
    title="考试安排与发布"
    description="在这里统一设置考试类型、批次、入场窗口、作答规则和参考名单，确保发布信息一次收口。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate">新建考试</el-button>
      </div>
    </template>

    <section class="panel-card section-card">
      <el-table :data="plans" v-loading="loading">
        <el-table-column prop="examCode" label="考试编码" min-width="130" />
        <el-table-column prop="examName" label="考试名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="考试类型" min-width="120">
          <template #default="{ row }">
            <el-tag :type="examModeTagType[row.examMode] || 'info'">{{ labelExamMode(row.examMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批次" min-width="120">
          <template #default="{ row }">{{ row.batchLabel || '默认批次' }}</template>
        </el-table-column>
        <el-table-column label="考场" min-width="140">
          <template #default="{ row }">{{ row.examRoom || '待分配' }}</template>
        </el-table-column>
        <el-table-column label="原考试" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.sourceExamName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="paperName" label="试卷" min-width="220" show-overflow-tooltip />
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="签到规则" min-width="160">
          <template #default="{ row }">
            {{ row.signInRequired === 1 ? `需签到（提前 ${row.signInStartMinutes} 分钟开放）` : '无需签到' }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长" min-width="90" />
        <el-table-column prop="candidateCount" label="考生数" min-width="90" />
        <el-table-column label="已签到" min-width="100">
          <template #default="{ row }">{{ row.signedInCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="签到率" min-width="120">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.signInRate || 0)" :stroke-width="10" :show-text="true" />
          </template>
        </el-table-column>
        <el-table-column prop="submittedCount" label="已提交" min-width="90" />
        <el-table-column label="发布状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'">{{ row.publishStatus === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link @click="exportSignInSheet(row.id)">导出签到名单</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeItem(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建考试' : '编辑考试'" width="min(1240px, 98vw)" top="4vh" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <section class="builder-shell">
          <section class="panel-card builder-panel">
            <div class="panel-head">
              <div>
                <p class="eyebrow">考试基本信息</p>
                <h3>确定考试名称、试卷和发布状态</h3>
              </div>
              <div class="summary-strip">
                <div class="summary-chip">
                  <strong>{{ selectedPaper?.questionCount || 0 }}</strong>
                  <span>题量</span>
                </div>
                <div class="summary-chip">
                  <strong>{{ selectedPaper?.totalScore || 0 }}</strong>
                  <span>试卷总分</span>
                </div>
                <div class="summary-chip">
                  <strong>{{ form.candidateUserIds.length }}</strong>
                  <span>已选考生</span>
                </div>
              </div>
            </div>

            <div class="field-grid field-grid--four">
              <el-form-item label="考试编码" prop="examCode"><el-input v-model="form.examCode" placeholder="考试编码" /></el-form-item>
              <el-form-item label="考试名称" prop="examName"><el-input v-model="form.examName" placeholder="考试名称" /></el-form-item>
              <el-form-item label="考试类型">
                <el-select v-model="form.examMode">
                  <el-option label="正常考试" value="NORMAL" />
                  <el-option label="补考" value="MAKEUP" />
                  <el-option label="缓考" value="DEFERRED" />
                  <el-option label="重考" value="RETAKE" />
                </el-select>
              </el-form-item>
              <el-form-item label="试卷" prop="paperId">
                <el-select v-model="form.paperId" filterable>
                  <el-option v-for="paper in papers" :key="paper.id" :label="paper.paperName" :value="paper.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="批次名称">
                <el-input v-model="form.batchLabel" placeholder="批次名称" />
              </el-form-item>
              <el-form-item label="考场">
                <el-input v-model="form.examRoom" placeholder="例如 第一教学楼 201 教室" />
              </el-form-item>
              <el-form-item label="发布状态">
                <el-select v-model="form.publishStatus">
                  <el-option :value="0" label="草稿" />
                  <el-option :value="1" label="立即发布" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item v-if="form.examMode !== 'NORMAL'" label="关联原考试" prop="sourceExamPlanId">
              <el-select v-model="form.sourceExamPlanId" filterable clearable placeholder="选择原考试">
                <el-option
                  v-for="plan in availableSourcePlans"
                  :key="plan.id"
                  :label="`${plan.examName}（${plan.examCode}）`"
                  :value="plan.id"
                />
              </el-select>
            </el-form-item>

            <div v-if="selectedPaper" class="paper-brief">
              <div class="brief-card">
                <strong>{{ selectedPaper.paperName }}</strong>
                <p>{{ selectedPaper.subject }} · {{ selectedPaper.questionCount }} 题 · {{ selectedPaper.totalScore }} 分 · {{ selectedPaper.durationMinutes }} 分钟</p>
              </div>
              <div class="brief-card">
                <strong>卷面说明</strong>
                <p>{{ selectedPaper.descriptionText || '暂无卷面说明。' }}</p>
              </div>
              <div class="brief-card">
                <strong>考试安排摘要</strong>
                <p>
                  {{ labelExamMode(form.examMode) }}
                  <span v-if="form.batchLabel"> · {{ form.batchLabel }}</span>
                  <span v-if="form.examRoom"> · {{ form.examRoom }}</span>
                  <span v-if="selectedSourcePlan"> · 原考试：{{ selectedSourcePlan.examName }}</span>
                </p>
              </div>
            </div>
          </section>

          <section class="panel-card builder-panel">
            <div class="panel-head">
              <div>
                <p class="eyebrow">时间语义</p>
                <h3>把“允许进入窗口”和“实际作答时长”分开设置</h3>
              </div>
            </div>

            <div class="field-grid field-grid--four">
              <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
              <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
              <el-form-item label="考试时长（分钟）"><el-input-number v-model="form.durationMinutes" :min="1" :max="300" /></el-form-item>
              <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="1" :max="500" /></el-form-item>
              <el-form-item label="允许迟到分钟">
                <el-input-number v-model="form.lateEntryMinutes" :min="0" :max="720" />
              </el-form-item>
              <el-form-item label="签到规则">
                <el-select v-model="form.signInRequired">
                  <el-option :value="0" label="不启用签到" />
                  <el-option :value="1" label="启用签到" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.signInRequired === 1" label="签到开放提前分钟">
                <el-input-number v-model="form.signInStartMinutes" :min="0" :max="1440" />
              </el-form-item>
              <el-form-item label="允许提前交卷分钟">
                <el-input-number v-model="form.earlySubmitMinutes" :min="0" :max="300" />
              </el-form-item>
              <el-form-item label="参考次数">
                <el-input-number v-model="form.attemptLimit" :min="1" :max="5" />
              </el-form-item>
              <el-form-item label="考试口令">
                <el-input v-model="form.examPassword" placeholder="不设置则可直接进入" />
              </el-form-item>
            </div>

            <div class="rule-grid">
              <div class="rule-card">
                <strong>学生允许进入时间窗口</strong>
                <p>{{ effectiveEntryDeadlineText }}</p>
              </div>
              <div class="rule-card">
                <strong>签到规则</strong>
                <p>{{ signInRuleText }}</p>
              </div>
              <div class="rule-card">
                <strong>学生实际作答规则</strong>
                <p>{{ answerRuleText }}</p>
              </div>
              <div class="rule-card">
                <strong>自动交卷条件</strong>
                <p>{{ form.autoSubmitEnabled === 1 ? '倒计时归零后自动交卷，以最近一次保存内容作为最终卷面。' : '关闭自动交卷后，到时会停止继续保存答案。' }}</p>
              </div>
            </div>
          </section>

          <section class="panel-card builder-panel">
            <div class="panel-head">
              <div>
                <p class="eyebrow">监考与考生范围</p>
                <h3>配置防作弊等级与应考名单</h3>
              </div>
            </div>

            <div class="field-grid field-grid--four">
              <el-form-item label="防作弊等级">
                <el-select v-model="form.antiCheatLevel">
                  <el-option label="基础" value="BASIC" />
                  <el-option label="严格" value="STRICT" />
                </el-select>
              </el-form-item>
              <el-form-item label="自动交卷">
                <el-select v-model="form.autoSubmitEnabled">
                  <el-option :value="1" label="开启" />
                  <el-option :value="0" label="关闭" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="考试说明">
              <el-input v-model="form.instructionText" type="textarea" :rows="3" placeholder="填写学生入场提醒、纪律要求和交卷说明" />
            </el-form-item>

            <el-form-item label="考生名单">
              <el-select v-model="form.candidateUserIds" multiple filterable collapse-tags placeholder="请选择需要参加本场考试的学生">
                <el-option v-for="user in users" :key="user.id" :label="user.fullName || user.nickname" :value="user.id" />
              </el-select>
            </el-form-item>

            <div class="candidate-summary">
              <div class="candidate-pill" v-for="user in selectedCandidates.slice(0, 12)" :key="user.id">
                {{ user.fullName || user.nickname }}
              </div>
              <span class="muted" v-if="selectedCandidates.length > 12">还有 {{ selectedCandidates.length - 12 }} 名考生已选中</span>
            </div>
          </section>
        </section>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存考试</el-button>
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

.builder-shell,
.builder-panel {
  display: grid;
  gap: 1rem;
}

.panel-head,
.summary-strip,
.paper-brief,
.rule-grid,
.candidate-summary {
  display: flex;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.panel-head {
  justify-content: space-between;
  align-items: flex-start;
}

.field-grid {
  display: grid;
  gap: 0.8rem;
}

.field-grid--four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-chip,
.brief-card,
.rule-card,
.candidate-pill {
  border: 1px solid color-mix(in oklch, var(--line) 78%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
}

.summary-chip,
.brief-card,
.rule-card {
  padding: 0.95rem 1rem;
}

.summary-chip strong {
  font-size: 1.35rem;
  color: var(--brand-deep);
}

.panel-head h3 {
  margin: 0.25rem 0 0;
  font-family: 'Literata', Georgia, serif;
}

.brief-card,
.rule-card {
  flex: 1 1 20rem;
}

.brief-card p,
.rule-card p {
  margin: 0.45rem 0 0;
  line-height: 1.65;
  color: var(--muted);
}

.candidate-summary {
  align-items: center;
}

.candidate-pill {
  padding: 0.45rem 0.8rem;
}

@media (max-width: 1180px) {
  .field-grid--four {
    grid-template-columns: 1fr;
  }
}
</style>
