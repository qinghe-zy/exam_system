<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { fetchCandidateAdmissionTicket, fetchCandidateWorkspace, fetchMyExams, reportCandidateEvent, saveCandidateAnswers, signInCandidateExam, submitCandidateAnswers } from '../../api/exam'
import type { CandidateAdmissionTicket, CandidateAnswerItem, CandidateExam, CandidateExamWorkspace } from '../../types/exam'
import { formatDateTime } from '../../utils/datetime'
import { labelAnswerSheetStatus, labelExamMode, labelQuestionType } from '../../utils/labels'

type DeviceCheckItem = {
  code: string
  label: string
  status: 'pass' | 'fail'
  detail: string
}

type DeviceCheckState = {
  passed: boolean
  blocking: boolean
  items: DeviceCheckItem[]
}

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const autoSubmitting = ref(false)
const exams = ref<CandidateExam[]>([])
const workspaceVisible = ref(false)
const workspace = ref<CandidateExamWorkspace | null>(null)
const answers = ref<Record<number, string | string[]>>({})
const countdownText = ref('--:--:--')
const passwordDialogVisible = ref(false)
const deviceCheckDialogVisible = ref(false)
const submitConfirmVisible = ref(false)
const ticketDialogVisible = ref(false)
const pendingExamPlanId = ref<number | null>(null)
const pendingWorkspace = ref<CandidateExamWorkspace | null>(null)
const currentTicket = ref<CandidateAdmissionTicket | null>(null)
const examPassword = ref('')
const currentQuestionId = ref<number | null>(null)
const lastSavedText = ref('尚未保存')
const questionObserver = ref<IntersectionObserver | null>(null)
const isFullscreen = ref(false)
const manualFullscreenExitPending = ref(false)
const latestWarningAt = reactive<Record<string, number>>({})
const violationStats = reactive({
  tabSwitch: 0,
  blur: 0,
  fullscreenExit: 0,
  restrictedOps: 0
})
let countdownTimer: number | undefined
let autoSaveTimer: number | undefined

const totalViolationCount = computed(() => violationStats.tabSwitch + violationStats.blur + violationStats.fullscreenExit + violationStats.restrictedOps)
const entryWindowText = computed(() => {
  if (!workspace.value) return '--'
  return `${formatDateTime(workspace.value.startTime)} 至 ${formatDateTime(workspace.value.entryDeadlineAt)}`
})
const autoSubmitText = computed(() => {
  if (!workspace.value) return '--'
  return workspace.value.autoSubmitEnabled === 1
    ? `当剩余时间归零或达到 ${formatDateTime(workspace.value.answerDeadlineAt)} 时自动交卷`
    : '未开启自动交卷，到时后系统会禁止继续保存答案'
})
const answeredCount = computed(() => {
  if (!workspace.value) return 0
  return workspace.value.items.filter((item) => hasAnswer(item.questionId)).length
})
const markedCount = computed(() => {
  if (!workspace.value) return 0
  return workspace.value.items.filter((item) => item.reviewLaterFlag === 1).length
})
const antiCheatPolicySummary = computed(() => {
  const policy = workspace.value?.antiCheatPolicy
  if (!policy) return '当前考试未下发额外监考策略。'
  const enabledItems = [
    policy.blockCopyEnabled === 1 ? '禁止复制' : '',
    policy.blockPasteEnabled === 1 ? '禁止粘贴' : '',
    policy.blockContextMenuEnabled === 1 ? '禁止右键' : '',
    policy.blockShortcutEnabled === 1 ? `拦截快捷键 ${policy.blockedShortcutKeys.join(' / ')}` : '',
    policy.deviceLoggingEnabled === 1 ? '记录设备上下文' : '',
    policy.deviceCheckEnabled === 1 ? `进入前执行设备检测（最小 ${policy.minWindowWidth}x${policy.minWindowHeight}）` : ''
  ].filter(Boolean)
  return enabledItems.length ? enabledItems.join('；') : '当前考试仅保留基础行为留痕。'
})
const deviceCheckState = ref<DeviceCheckState>({
  passed: true,
  blocking: false,
  items: []
})

function parseOptions(item: CandidateAnswerItem) {
  try {
    const parsed = item.optionsJson ? JSON.parse(item.optionsJson) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function parseAttachments(item: CandidateAnswerItem) {
  try {
    const parsed = item.attachmentJson ? JSON.parse(item.attachmentJson) : []
    if (!Array.isArray(parsed)) return []
    return parsed.map((entry) => typeof entry === 'string' ? { name: entry, url: entry } : entry).filter((entry) => entry.url)
  } catch {
    return []
  }
}

function hydrateAnswers(items: CandidateAnswerItem[]) {
  const next: Record<number, string | string[]> = {}
  items.forEach((item) => {
    if (item.questionType === 'MULTIPLE_CHOICE' || item.questionType === 'FILL_BLANK') {
      next[item.questionId] = item.answerContent ? item.answerContent.split('|').filter(Boolean) : []
    } else {
      next[item.questionId] = item.answerContent || ''
    }
  })
  answers.value = next
}

function collectAnswers() {
  if (!workspace.value) return []
  return workspace.value.items.map((item) => ({
    questionId: item.questionId,
    reviewLaterFlag: item.reviewLaterFlag === 1 ? 1 : 0,
    answerContent:
      item.questionType === 'MULTIPLE_CHOICE' || item.questionType === 'FILL_BLANK'
        ? ((answers.value[item.questionId] as string[] | undefined) || []).join('|')
        : String(answers.value[item.questionId] || '')
  }))
}

function hasAnswer(questionId: number) {
  const value = answers.value[questionId]
  if (Array.isArray(value)) return value.length > 0
  return String(value || '').trim().length > 0
}

async function loadExams() {
  loading.value = true
  try {
    exams.value = await fetchMyExams()
  } finally {
    loading.value = false
  }
}

async function openWorkspace(examPlanId: number) {
  pendingExamPlanId.value = examPlanId
  examPassword.value = ''
  passwordDialogVisible.value = true
}

async function signInExam(examPlanId: number) {
  await signInCandidateExam(examPlanId)
  ElMessage.success('签到完成，当前可以查看准考证并进入考试')
  await loadExams()
}

async function openAdmissionTicket(examPlanId: number) {
  currentTicket.value = await fetchCandidateAdmissionTicket(examPlanId)
  ticketDialogVisible.value = true
}

function printAdmissionTicket() {
  if (!currentTicket.value) return
  const ticket = currentTicket.value
  const opened = window.open('', '_blank', 'width=980,height=720')
  if (!opened) {
    ElMessage.warning('浏览器阻止了打印窗口，请允许弹窗后重试')
    return
  }
  opened.document.write(`
    <html lang="zh-CN">
      <head>
        <title>准考证 - ${ticket.examName}</title>
        <style>
          body { font-family: "Microsoft YaHei", sans-serif; padding: 24px; color: #2f2a24; }
          .sheet { border: 1px solid #d5cdc2; border-radius: 16px; padding: 24px; max-width: 860px; margin: 0 auto; }
          h1 { margin: 0 0 8px; font-size: 28px; }
          p { line-height: 1.7; margin: 6px 0; }
          .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 24px; margin-top: 20px; }
          .item { padding: 12px 14px; border-radius: 12px; background: #f8f5f0; }
          .label { color: #7d6f60; font-size: 13px; }
          .value { margin-top: 6px; font-size: 16px; font-weight: 600; }
        </style>
      </head>
      <body>
        <section class="sheet">
          <h1>在线考试准考证</h1>
          <p>请考生按时签到并在允许进入窗口内进入考试，保持设备与浏览器环境稳定。</p>
          <div class="grid">
            <div class="item"><div class="label">考试名称</div><div class="value">${ticket.examName}</div></div>
            <div class="item"><div class="label">考试编码</div><div class="value">${ticket.examCode}</div></div>
            <div class="item"><div class="label">考试类型</div><div class="value">${labelExamMode(ticket.examMode)}</div></div>
            <div class="item"><div class="label">批次</div><div class="value">${ticket.batchLabel || '默认批次'}</div></div>
            <div class="item"><div class="label">考场</div><div class="value">${ticket.examRoom || '待分配'}</div></div>
            <div class="item"><div class="label">考生姓名</div><div class="value">${ticket.candidateName}</div></div>
            <div class="item"><div class="label">组织 / 班级</div><div class="value">${ticket.organizationName || '—'}</div></div>
            <div class="item"><div class="label">座位号</div><div class="value">${ticket.seatNo || '待分配'}</div></div>
            <div class="item"><div class="label">考试开始</div><div class="value">${formatDateTime(ticket.startTime)}</div></div>
            <div class="item"><div class="label">最晚进入</div><div class="value">${formatDateTime(ticket.entryDeadlineAt)}</div></div>
            <div class="item"><div class="label">签到规则</div><div class="value">${ticket.signInRequired === 1 ? `需签到，${formatDateTime(ticket.signInOpenAt || ticket.startTime)} 开放` : '无需签到'}</div></div>
            <div class="item"><div class="label">签到状态</div><div class="value">${ticket.signedInFlag === 1 ? `已签到（${formatDateTime(ticket.signedInAt || '')}）` : '未签到'}</div></div>
            <div class="item"><div class="label">试卷</div><div class="value">${ticket.paperName}</div></div>
            <div class="item"><div class="label">准入码</div><div class="value">${ticket.accessCode || '系统自动分配'}</div></div>
          </div>
          <p style="margin-top: 18px;">考试说明：${ticket.instructionText || '请按监考要求进行签到、入场和作答。'}</p>
        </section>
      </body>
    </html>
  `)
  opened.document.close()
  opened.focus()
  opened.print()
}

async function confirmWorkspace() {
  if (!pendingExamPlanId.value) return
  const loadedWorkspace = await fetchCandidateWorkspace(pendingExamPlanId.value, examPassword.value || undefined)
  passwordDialogVisible.value = false
  if (loadedWorkspace.antiCheatPolicy?.deviceLoggingEnabled === 1) {
    await reportCandidateEvent(loadedWorkspace.examPlanId, {
      answerSheetId: loadedWorkspace.answerSheetId,
      eventType: 'DEVICE_CONTEXT',
      severity: 'LOW',
      triggeredAutoSave: 0,
      saveVersion: loadedWorkspace.saveVersion,
      deviceFingerprint: getDeviceFingerprint(),
      deviceInfo: buildDeviceInfo(),
      detailText: `进入考试时记录设备上下文，策略等级：${loadedWorkspace.antiCheatLevel || 'BASIC'}。`
    })
  }
  if (loadedWorkspace.antiCheatPolicy?.deviceCheckEnabled === 1) {
    const detection = buildDeviceCheckState(loadedWorkspace)
    deviceCheckState.value = detection
    if (!detection.passed) {
      await reportCandidateEvent(loadedWorkspace.examPlanId, {
        answerSheetId: loadedWorkspace.answerSheetId,
        eventType: 'DEVICE_CHECK_FAILED',
        severity: detection.blocking ? 'HIGH' : 'MEDIUM',
        triggeredAutoSave: 0,
        saveVersion: loadedWorkspace.saveVersion,
        deviceFingerprint: getDeviceFingerprint(),
        deviceInfo: buildDeviceInfo(),
        detailText: detection.items
          .filter((item) => item.status === 'fail')
          .map((item) => `${item.label}：${item.detail}`)
          .join('；')
      })
      pendingWorkspace.value = loadedWorkspace
      deviceCheckDialogVisible.value = true
      return
    }
    await reportCandidateEvent(loadedWorkspace.examPlanId, {
      answerSheetId: loadedWorkspace.answerSheetId,
      eventType: 'DEVICE_CHECK_PASSED',
      severity: 'LOW',
      triggeredAutoSave: 0,
      saveVersion: loadedWorkspace.saveVersion,
      deviceFingerprint: getDeviceFingerprint(),
      deviceInfo: buildDeviceInfo(),
      detailText: '设备检测通过，允许进入考试。'
    })
  }
  await activateWorkspace(loadedWorkspace)
}

async function activateWorkspace(loadedWorkspace: CandidateExamWorkspace) {
  workspace.value = loadedWorkspace
  pendingWorkspace.value = null
  hydrateAnswers(loadedWorkspace.items)
  workspaceVisible.value = true
  currentQuestionId.value = loadedWorkspace.items[0]?.questionId || null
  resetViolationStats()
  lastSavedText.value = loadedWorkspace.saveVersion > 0 ? `已保存 ${loadedWorkspace.saveVersion} 次` : '进入考试后将自动保存答案'
  await nextTick()
  bindQuestionObserver()
}

function buildDeviceCheckState(loadedWorkspace: CandidateExamWorkspace): DeviceCheckState {
  const policy = loadedWorkspace.antiCheatPolicy
  if (!policy || policy.deviceCheckEnabled !== 1) {
    return { passed: true, blocking: false, items: [] }
  }
  const viewportWidth = window.innerWidth || window.screen.width || 0
  const viewportHeight = window.innerHeight || window.screen.height || 0
  const userAgent = navigator.userAgent || ''
  const browserAllowed = !policy.allowedBrowserKeywords.length
    || policy.allowedBrowserKeywords.some((item) => userAgent.includes(item))
  const mobileDetected = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent)
  const fullscreenSupported = typeof document.documentElement.requestFullscreen === 'function'
  const items: DeviceCheckItem[] = [
    {
      code: 'browser',
      label: '浏览器环境',
      status: browserAllowed ? 'pass' : 'fail',
      detail: browserAllowed ? `当前浏览器：${userAgent}` : `当前浏览器不在允许列表：${policy.allowedBrowserKeywords.join(' / ')}`
    },
    {
      code: 'viewport',
      label: '窗口尺寸',
      status: viewportWidth >= policy.minWindowWidth && viewportHeight >= policy.minWindowHeight ? 'pass' : 'fail',
      detail:
        viewportWidth >= policy.minWindowWidth && viewportHeight >= policy.minWindowHeight
          ? `当前窗口 ${viewportWidth}x${viewportHeight}`
          : `当前窗口 ${viewportWidth}x${viewportHeight}，要求至少 ${policy.minWindowWidth}x${policy.minWindowHeight}`
    },
    {
      code: 'mobile',
      label: '设备类型',
      status: policy.forbidMobileEntry === 1 && mobileDetected ? 'fail' : 'pass',
      detail: mobileDetected ? '检测到移动端特征' : '当前为桌面端环境'
    },
    {
      code: 'fullscreen',
      label: '全屏能力',
      status: policy.requireFullscreenSupport === 1 && !fullscreenSupported ? 'fail' : 'pass',
      detail: fullscreenSupported ? '浏览器支持全屏 API' : '浏览器不支持全屏 API'
    }
  ]
  const passed = items.every((item) => item.status === 'pass')
  return {
    passed,
    blocking: !passed && policy.blockOnDeviceCheckFail === 1,
    items
  }
}

function closeDeviceCheck() {
  deviceCheckDialogVisible.value = false
  pendingWorkspace.value = null
}

async function continueAfterDeviceCheck() {
  if (!pendingWorkspace.value) return
  deviceCheckDialogVisible.value = false
  await activateWorkspace(pendingWorkspace.value)
}

function resetViolationStats() {
  violationStats.tabSwitch = 0
  violationStats.blur = 0
  violationStats.fullscreenExit = 0
  violationStats.restrictedOps = 0
}

async function saveCurrent(showMessage = true, triggerSource: 'manual' | 'timer' | 'event' = 'manual') {
  if (!workspace.value || saving.value || submitting.value) return false
  saving.value = true
  try {
    workspace.value = await saveCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    lastSavedText.value = `${triggerSource === 'event' ? '异常行为触发' : '系统'}保存成功，当前版本 ${workspace.value.saveVersion}`
    if (showMessage) {
      ElMessage.success(triggerSource === 'manual' ? '答案已保存' : '系统已自动保存当前答案')
    }
    return true
  } catch {
    if (showMessage) {
      ElMessage.error('保存失败，请检查网络后重试')
    }
    lastSavedText.value = '最近一次保存失败，请尽快手动保存'
    return false
  } finally {
    saving.value = false
  }
}

async function submitCurrent(mode: 'prompt' | 'manual' | 'auto' = 'prompt') {
  if (!workspace.value || submitting.value) return
  if (mode === 'prompt') {
    submitConfirmVisible.value = true
    return
  }
  submitting.value = true
  try {
    workspace.value = await submitCandidateAnswers(workspace.value.examPlanId, collectAnswers())
    hydrateAnswers(workspace.value.items)
    ElMessage.success(mode === 'auto' ? '作答时间已到，系统已自动交卷' : '试卷已提交')
    workspaceVisible.value = false
    await loadExams()
  } catch {
    ElMessage.error(mode === 'auto' ? '自动交卷失败，请立即联系监考老师' : '提交试卷失败，请稍后重试')
  } finally {
    submitting.value = false
    autoSubmitting.value = false
    submitConfirmVisible.value = false
  }
}

async function autoSaveCurrent(triggerSource: 'timer' | 'event' = 'timer') {
  if (!workspaceVisible.value || !workspace.value) return false
  return saveCurrent(false, triggerSource)
}

async function reportSuspiciousEvent(eventType: string, severity: string, detailText: string, violationCount: number) {
  if (!workspace.value) return
  const autoSaved = await autoSaveCurrent('event')
  try {
    await reportCandidateEvent(workspace.value.examPlanId, {
      answerSheetId: workspace.value.answerSheetId,
      eventType,
      severity,
      leaveCount: violationCount,
      triggeredAutoSave: autoSaved ? 1 : 0,
      saveVersion: workspace.value.saveVersion,
      deviceFingerprint: getDeviceFingerprint(),
      deviceInfo: buildDeviceInfo(),
      detailText
    })
  } catch {
    // 不阻塞考试主流程
  }
}

function toastPolicyWarning(key: string, message: string) {
  const now = Date.now()
  if (now - (latestWarningAt[key] || 0) < 1800) return
  latestWarningAt[key] = now
  ElMessage.warning(message)
}

function buildDeviceInfo() {
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'unknown'
  return [
    `UA=${navigator.userAgent}`,
    `Platform=${navigator.platform || 'unknown'}`,
    `Lang=${navigator.language || 'unknown'}`,
    `TZ=${timezone}`,
    `Screen=${window.screen.width}x${window.screen.height}`
  ].join(' | ')
}

function getDeviceFingerprint() {
  const base = [
    navigator.userAgent,
    navigator.platform,
    navigator.language,
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    `${window.screen.width}x${window.screen.height}`
  ].join('::')
  let hash = 0
  for (let index = 0; index < base.length; index += 1) {
    hash = (hash << 5) - hash + base.charCodeAt(index)
    hash |= 0
  }
  return `dev-${Math.abs(hash)}`
}

function normalizeShortcutKey(rawKey: string) {
  if (!rawKey) return ''
  return rawKey.length === 1 ? rawKey.toUpperCase() : rawKey
}

function buildShortcutText(event: KeyboardEvent) {
  const parts: string[] = []
  if (event.ctrlKey) parts.push('Ctrl')
  if (event.metaKey) parts.push('Meta')
  if (event.altKey) parts.push('Alt')
  if (event.shiftKey) parts.push('Shift')
  parts.push(normalizeShortcutKey(event.key))
  return parts.join('+')
}

function matchesBlockedShortcut(event: KeyboardEvent) {
  const shortcuts = workspace.value?.antiCheatPolicy?.blockedShortcutKeys || []
  const current = buildShortcutText(event).toUpperCase()
  return shortcuts.some((item) => item.trim().toUpperCase() === current)
}

async function logRestrictedOperation(eventType: string, detailText: string) {
  violationStats.restrictedOps += 1
  await reportSuspiciousEvent(eventType, 'HIGH', detailText, violationStats.restrictedOps)
}

function updateCountdown() {
  if (!workspace.value) {
    countdownText.value = '--:--:--'
    return
  }
  const diff = new Date(workspace.value.answerDeadlineAt).getTime() - Date.now()
  if (diff <= 0) {
    countdownText.value = '00:00:00'
    if (!autoSubmitting.value && workspace.value.autoSubmitEnabled === 1 && workspace.value.answerSheetStatus !== 'SUBMITTED') {
      autoSubmitting.value = true
      submitCurrent('auto')
    }
    return
  }
  const hours = Math.floor(diff / 3_600_000)
  const minutes = Math.floor((diff % 3_600_000) / 60_000)
  const seconds = Math.floor((diff % 60_000) / 1000)
  countdownText.value = [hours, minutes, seconds].map((item) => String(item).padStart(2, '0')).join(':')
}

function startCountdown() {
  window.clearInterval(countdownTimer)
  updateCountdown()
  countdownTimer = window.setInterval(updateCountdown, 1000)
}

function stopCountdown() {
  window.clearInterval(countdownTimer)
}

function startAutoSave() {
  window.clearInterval(autoSaveTimer)
  autoSaveTimer = window.setInterval(() => autoSaveCurrent('timer'), 20_000)
}

function stopAutoSave() {
  window.clearInterval(autoSaveTimer)
}

async function handleVisibilityChange() {
  if (!workspaceVisible.value || !workspace.value || !document.hidden) return
  violationStats.tabSwitch += 1
  await reportSuspiciousEvent('TAB_SWITCH', 'MEDIUM', `考生第 ${violationStats.tabSwitch} 次切换页面标签，系统已联动自动保存当前答案。`, violationStats.tabSwitch)
}

async function handleWindowBlur() {
  if (!workspaceVisible.value || !workspace.value) return
  violationStats.blur += 1
  await reportSuspiciousEvent('WINDOW_BLUR', violationStats.blur >= 3 ? 'MEDIUM' : 'LOW', `考生第 ${violationStats.blur} 次离开当前考试窗口，系统已联动自动保存当前答案。`, violationStats.blur)
}

async function handleFullscreenChange() {
  isFullscreen.value = Boolean(document.fullscreenElement)
  if (!workspaceVisible.value || !workspace.value || document.fullscreenElement) return
  if (manualFullscreenExitPending.value) {
    manualFullscreenExitPending.value = false
    return
  }
  violationStats.fullscreenExit += 1
  await reportSuspiciousEvent('FULLSCREEN_EXIT', 'HIGH', `考生第 ${violationStats.fullscreenExit} 次异常退出全屏考试态，建议监考端重点关注。`, violationStats.fullscreenExit)
}

async function handleCopy(event: ClipboardEvent) {
  if (!workspaceVisible.value || workspace.value?.antiCheatPolicy?.blockCopyEnabled !== 1) return
  event.preventDefault()
  toastPolicyWarning('copy', '当前考试已禁止复制操作，系统已记录本次行为。')
  await logRestrictedOperation('COPY_ATTEMPT', '考生尝试复制页面内容，操作已被系统拦截。')
}

async function handlePaste(event: ClipboardEvent) {
  if (!workspaceVisible.value || workspace.value?.antiCheatPolicy?.blockPasteEnabled !== 1) return
  event.preventDefault()
  toastPolicyWarning('paste', '当前考试已禁止粘贴操作，系统已记录本次行为。')
  await logRestrictedOperation('PASTE_ATTEMPT', '考生尝试粘贴内容到考试工作区，操作已被系统拦截。')
}

async function handleContextMenu(event: MouseEvent) {
  if (!workspaceVisible.value || workspace.value?.antiCheatPolicy?.blockContextMenuEnabled !== 1) return
  event.preventDefault()
  toastPolicyWarning('contextmenu', '当前考试已禁止右键菜单，系统已记录本次行为。')
  await logRestrictedOperation('CONTEXT_MENU_BLOCKED', '考生尝试打开右键菜单，操作已被系统拦截。')
}

async function handleKeydown(event: KeyboardEvent) {
  if (!workspaceVisible.value || workspace.value?.antiCheatPolicy?.blockShortcutEnabled !== 1) return
  if (!matchesBlockedShortcut(event)) return
  event.preventDefault()
  const shortcut = buildShortcutText(event)
  toastPolicyWarning(`shortcut:${shortcut}`, `当前考试已拦截快捷键 ${shortcut}，系统已记录本次行为。`)
  await logRestrictedOperation('SHORTCUT_BLOCKED', `考生尝试使用快捷键 ${shortcut}，操作已被系统拦截。`)
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) {
      manualFullscreenExitPending.value = true
      await document.exitFullscreen?.()
      isFullscreen.value = false
      return
    }
    manualFullscreenExitPending.value = false
    await document.documentElement.requestFullscreen?.()
    isFullscreen.value = true
  } catch {
    manualFullscreenExitPending.value = false
    ElMessage.warning('当前浏览器未允许全屏切换，请手动调整')
  }
}

async function jumpToQuestion(questionId: number) {
  currentQuestionId.value = questionId
  await nextTick()
  document.getElementById(`question-${questionId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function toggleReviewLater(item: CandidateAnswerItem) {
  item.reviewLaterFlag = item.reviewLaterFlag === 1 ? 0 : 1
}

function getBlankAnswers(questionId: number) {
  const value = answers.value[questionId]
  return Array.isArray(value) ? value : ['']
}

function setBlankAnswer(questionId: number, index: number, value: string) {
  const next = [...getBlankAnswers(questionId)]
  next[index] = value
  answers.value[questionId] = next
}

function bindQuestionObserver() {
  questionObserver.value?.disconnect()
  if (!workspace.value) return
  questionObserver.value = new IntersectionObserver(
    (entries) => {
      const visibleEntry = entries
        .filter((entry) => entry.isIntersecting)
        .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0]
      if (!visibleEntry) return
      const questionId = Number(visibleEntry.target.getAttribute('data-question-id'))
      if (!Number.isNaN(questionId)) {
        currentQuestionId.value = questionId
      }
    },
    { rootMargin: '-20% 0px -55% 0px', threshold: [0.15, 0.4, 0.75] }
  )
  workspace.value.items.forEach((item) => {
    const element = document.getElementById(`question-${item.questionId}`)
    if (element) {
      questionObserver.value?.observe(element)
    }
  })
}

watch(
  workspaceVisible,
  (visible) => {
    document.body.style.overflow = visible ? 'hidden' : ''
    if (visible) {
      startCountdown()
      startAutoSave()
    } else {
      stopCountdown()
      stopAutoSave()
      questionObserver.value?.disconnect()
      workspace.value = null
      pendingWorkspace.value = null
      submitConfirmVisible.value = false
      deviceCheckDialogVisible.value = false
      isFullscreen.value = false
      manualFullscreenExitPending.value = false
    }
  },
  { immediate: false }
)

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!workspaceVisible.value || !workspace.value || workspace.value.answerSheetStatus === 'SUBMITTED') {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

onMounted(async () => {
  await loadExams()
  isFullscreen.value = Boolean(document.fullscreenElement)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  window.addEventListener('beforeunload', handleBeforeUnload)
  document.addEventListener('copy', handleCopy)
  document.addEventListener('paste', handlePaste)
  document.addEventListener('contextmenu', handleContextMenu)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  stopCountdown()
  stopAutoSave()
  questionObserver.value?.disconnect()
  document.body.style.overflow = ''
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  document.removeEventListener('copy', handleCopy)
  document.removeEventListener('paste', handlePaste)
  document.removeEventListener('contextmenu', handleContextMenu)
  document.removeEventListener('keydown', handleKeydown)
})

onBeforeRouteLeave((_to, _from, next) => {
  if (!workspaceVisible.value || !workspace.value || workspace.value.answerSheetStatus === 'SUBMITTED') {
    next()
    return
  }
  const shouldLeave = window.confirm('当前考试仍在进行中，离开页面前请确认答案已经保存。确定继续离开吗？')
  next(shouldLeave)
})
</script>

<template>
  <AppShellSection
    eyebrow="考生中心"
    title="我的考试：先查看入场窗口，再进入沉浸式作答"
    description="先确认考试类型、批次、原考试和入场窗口，再进入正式作答。进入后页面会展示本场剩余作答时间，并在异常行为发生时自动保存当前答案。"
  >
    <section class="panel-card section-card">
      <el-table :data="exams" v-loading="loading">
        <el-table-column prop="examName" label="考试名称" min-width="220" />
        <el-table-column label="考试类型" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.examMode === 'NORMAL' ? 'info' : 'warning'">{{ labelExamMode(row.examMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批次" min-width="120">
          <template #default="{ row }">{{ row.batchLabel || '默认批次' }}</template>
        </el-table-column>
        <el-table-column label="考场 / 座位" min-width="180">
          <template #default="{ row }">{{ row.examRoom || '待分配' }}{{ row.seatNo ? ` / ${row.seatNo}` : '' }}</template>
        </el-table-column>
        <el-table-column prop="sourceExamName" label="原考试" min-width="220" />
        <el-table-column prop="paperName" label="试卷" min-width="200" />
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="最晚进入时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.entryDeadlineAt) }}</template>
        </el-table-column>
        <el-table-column label="签到状态" min-width="180">
          <template #default="{ row }">
            <span v-if="row.signInRequired === 1">
              {{ row.signedInFlag === 1 ? `已签到 ${formatDateTime(row.signedInAt || '')}` : `待签到（${formatDateTime(row.signInOpenAt || row.startTime)} 开放）` }}
            </span>
            <span v-else>无需签到</span>
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="作答时长" min-width="110" />
        <el-table-column label="答卷状态" min-width="120">
          <template #default="{ row }">{{ labelAnswerSheetStatus(row.answerSheetStatus) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain :disabled="row.signInRequired === 1 && row.signedInFlag !== 1" @click="openWorkspace(row.examPlanId)">进入考试</el-button>
            <el-button plain @click="openAdmissionTicket(row.examPlanId)">准考证</el-button>
            <el-button
              v-if="row.signInRequired === 1 && row.signedInFlag !== 1"
              type="warning"
              plain
              @click="signInExam(row.examPlanId)"
            >
              签到
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="passwordDialogVisible" title="进入考试" width="min(460px, 92vw)" append-to-body :z-index="2600">
      <p class="muted">如果考试配置了口令，请输入后进入。进入后会按照“考试时长”和“窗口剩余时间”共同计算本场实际倒计时。</p>
      <el-input v-model="examPassword" type="password" show-password placeholder="请输入考试口令（如有）" />
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmWorkspace">进入考试</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ticketDialogVisible" title="准考证 / 通知单" width="min(760px, 96vw)" append-to-body :z-index="2600">
      <section v-if="currentTicket" class="ticket-sheet">
        <header class="ticket-head">
          <div>
            <p class="eyebrow">准考证</p>
            <h3>{{ currentTicket.examName }}</h3>
            <p class="muted">请在签到开放时间内完成签到，并在最晚进入时间前进入正式考试。</p>
          </div>
          <el-tag :type="currentTicket.signedInFlag === 1 ? 'success' : 'warning'">
            {{ currentTicket.signedInFlag === 1 ? '已签到' : '待签到' }}
          </el-tag>
        </header>
        <div class="ticket-grid">
          <article><strong>考试编码</strong><span>{{ currentTicket.examCode }}</span></article>
          <article><strong>考试类型</strong><span>{{ labelExamMode(currentTicket.examMode) }}</span></article>
          <article><strong>批次</strong><span>{{ currentTicket.batchLabel || '默认批次' }}</span></article>
          <article><strong>考场</strong><span>{{ currentTicket.examRoom || '待分配' }}</span></article>
          <article><strong>原考试</strong><span>{{ currentTicket.sourceExamName || '—' }}</span></article>
          <article><strong>考生姓名</strong><span>{{ currentTicket.candidateName }}</span></article>
          <article><strong>组织 / 班级</strong><span>{{ currentTicket.organizationName || '—' }}</span></article>
          <article><strong>座位号</strong><span>{{ currentTicket.seatNo || '待分配' }}</span></article>
          <article><strong>考试开始</strong><span>{{ formatDateTime(currentTicket.startTime) }}</span></article>
          <article><strong>最晚进入</strong><span>{{ formatDateTime(currentTicket.entryDeadlineAt) }}</span></article>
          <article><strong>签到规则</strong><span>{{ currentTicket.signInRequired === 1 ? `需签到（${formatDateTime(currentTicket.signInOpenAt || currentTicket.startTime)} 开放）` : '无需签到' }}</span></article>
          <article><strong>签到状态</strong><span>{{ currentTicket.signedInFlag === 1 ? `已签到 ${formatDateTime(currentTicket.signedInAt || '')}` : '未签到' }}</span></article>
          <article><strong>试卷</strong><span>{{ currentTicket.paperName }}</span></article>
          <article><strong>准入码</strong><span>{{ currentTicket.accessCode || '系统自动分配' }}</span></article>
        </div>
        <div class="ticket-note">
          <strong>考试说明</strong>
          <p>{{ currentTicket.instructionText || '请携带本通知单，在签到开放时间内完成签到后进入考试。' }}</p>
        </div>
      </section>
      <template #footer>
        <el-button @click="ticketDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="printAdmissionTicket">打印</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deviceCheckDialogVisible" title="设备检测结果" width="min(560px, 96vw)" append-to-body :z-index="2600">
      <p class="muted">
        当前考试启用了设备检测。只有浏览器环境、窗口尺寸、设备类型和全屏能力满足要求时，才建议进入正式考试态。
      </p>
      <div class="device-check-list">
        <article v-for="item in deviceCheckState.items" :key="item.code" class="device-check-item">
          <div class="device-check-head">
            <strong>{{ item.label }}</strong>
            <el-tag :type="item.status === 'pass' ? 'success' : 'danger'">{{ item.status === 'pass' ? '通过' : '未通过' }}</el-tag>
          </div>
          <p>{{ item.detail }}</p>
        </article>
      </div>
      <el-alert
        v-if="!deviceCheckState.passed"
        :type="deviceCheckState.blocking ? 'error' : 'warning'"
        :closable="false"
        show-icon
        :title="deviceCheckState.blocking ? '当前设备检测未通过，系统已阻止进入考试。' : '设备检测存在风险项，请谨慎继续。'"
      />
      <template #footer>
        <el-button @click="closeDeviceCheck">返回考试列表</el-button>
        <el-button v-if="!deviceCheckState.blocking" type="primary" @click="continueAfterDeviceCheck">仍然进入</el-button>
      </template>
    </el-dialog>

    <section v-if="workspaceVisible && workspace" class="exam-overlay">
      <div class="exam-stage">
        <header class="exam-header">
          <div class="exam-title-block">
            <p class="eyebrow">考试工作区</p>
            <h2>{{ workspace.examName }}</h2>
            <p class="muted">试卷：{{ workspace.paperName }}{{ workspace.paperVersion ? ` · ${workspace.paperVersion}` : '' }}</p>
          </div>
          <div class="exam-metrics">
            <div class="metric-card metric-card--timer">
              <span>本场剩余作答时间</span>
              <strong>{{ countdownText }}</strong>
            </div>
            <div class="metric-card">
              <span>允许进入窗口</span>
              <strong>{{ entryWindowText }}</strong>
            </div>
            <div class="metric-card">
              <span>自动交卷规则</span>
              <strong>{{ autoSubmitText }}</strong>
            </div>
          </div>
          <div class="exam-toolbar">
            <div class="toolbar-note">
              <span>已答 {{ answeredCount }}/{{ workspace.items.length }}</span>
              <span>待复查 {{ markedCount }} 题</span>
              <span>异常 {{ totalViolationCount }} 次</span>
              <span>{{ lastSavedText }}</span>
            </div>
            <div class="toolbar-actions">
              <el-button @click="toggleFullscreen">{{ isFullscreen ? '退出全屏' : '进入全屏' }}</el-button>
              <el-button :loading="saving" @click="saveCurrent(true, 'manual')">保存答案</el-button>
              <el-button type="primary" :loading="submitting || autoSubmitting" @click="submitCurrent('prompt')">提交试卷</el-button>
            </div>
          </div>
        </header>

        <div class="exam-content">
          <main class="question-column">
            <section class="exam-notice">
              <div class="notice-card">
                <strong>考前提示</strong>
                <p>{{ workspace.instructionText || '请专注作答，保持页面可见，避免切屏、退出全屏或长期离开当前考试窗口。' }}</p>
              </div>
              <div class="notice-card">
                <strong>监考说明</strong>
                <p>页面切换、窗口失焦、退出全屏等异常行为会实时记录，并联动自动保存当前作答内容。</p>
              </div>
              <div class="notice-card">
                <strong>本场监考策略</strong>
                <p>{{ antiCheatPolicySummary }}</p>
              </div>
            </section>

            <article
              v-for="item in workspace.items"
              :id="`question-${item.questionId}`"
              :key="item.questionId"
              :data-question-id="item.questionId"
              class="question-card"
              :class="{ active: currentQuestionId === item.questionId }"
            >
              <div class="question-head">
                <div>
                  <p class="eyebrow">第 {{ item.questionOrder }} 题 · {{ item.questionCode }} · {{ labelQuestionType(item.questionType) }}</p>
                  <div v-if="item.materialContent" class="material-block" v-html="item.materialContent"></div>
                  <h3 v-if="!item.stemHtml">{{ item.stem }}</h3>
                  <div v-else class="stem-html" v-html="item.stemHtml"></div>
                </div>
                <div class="question-tags">
                  <el-button size="small" plain :type="item.reviewLaterFlag === 1 ? 'warning' : 'default'" @click="toggleReviewLater(item)">
                    {{ item.reviewLaterFlag === 1 ? '取消待复查' : '标记待复查' }}
                  </el-button>
                  <el-tag type="warning">{{ item.maxScore }} 分</el-tag>
                </div>
              </div>

              <div v-if="parseAttachments(item).length" class="attachment-list">
                <a v-for="attachment in parseAttachments(item)" :key="attachment.url" :href="attachment.url" target="_blank" rel="noreferrer">
                  {{ attachment.name || attachment.url }}
                </a>
              </div>

              <template v-if="['SINGLE_CHOICE', 'TRUE_FALSE'].includes(item.questionType) && parseOptions(item).length">
                <el-radio-group v-model="answers[item.questionId]" class="choice-group">
                  <el-radio v-for="option in parseOptions(item)" :key="option" :label="option">{{ option }}</el-radio>
                </el-radio-group>
              </template>

              <template v-else-if="item.questionType === 'MULTIPLE_CHOICE' && parseOptions(item).length">
                <el-checkbox-group v-model="answers[item.questionId]" class="choice-group">
                  <el-checkbox v-for="option in parseOptions(item)" :key="option" :label="option">{{ option }}</el-checkbox>
                </el-checkbox-group>
              </template>

              <template v-else-if="item.questionType === 'FILL_BLANK'">
                <div class="blank-grid">
                  <el-input
                    v-for="(slot, index) in (parseOptions(item).length ? parseOptions(item) : ['第1空'])"
                    :key="`${item.questionId}-${index}`"
                    :model-value="getBlankAnswers(item.questionId)[index] || ''"
                    :placeholder="String(slot)"
                    @update:model-value="setBlankAnswer(item.questionId, index, String($event || ''))"
                  />
                </div>
              </template>

              <template v-else>
                <el-input v-model="answers[item.questionId]" type="textarea" :rows="6" placeholder="请在此输入答案，系统会自动保存" />
              </template>
            </article>
          </main>

          <aside class="aside-column">
            <section class="answer-card-panel">
              <div class="aside-head">
                <strong>答题卡</strong>
                <span class="muted">聚焦当前作答节奏</span>
              </div>
              <div class="answer-card-grid">
                <button
                  v-for="item in workspace.items"
                  :key="item.questionId"
                  type="button"
                  class="answer-card-cell"
                  :class="{ answered: hasAnswer(item.questionId), active: currentQuestionId === item.questionId, marked: item.reviewLaterFlag === 1 }"
                  @click="jumpToQuestion(item.questionId)"
                >
                  {{ item.questionOrder }}
                </button>
              </div>
            </section>

            <section class="aside-panel">
              <div class="aside-head">
                <strong>考试状态</strong>
                <span class="muted">{{ labelAnswerSheetStatus(workspace.answerSheetStatus) }}</span>
              </div>
              <ul class="status-list">
                <li>允许进入：{{ entryWindowText }}</li>
                <li>作答截止：{{ formatDateTime(workspace.answerDeadlineAt) }}</li>
                <li>自动交卷：{{ workspace.autoSubmitEnabled === 1 ? '开启' : '关闭' }}</li>
                <li>监考等级：{{ workspace.antiCheatLevel || 'BASIC' }}</li>
                <li>待复查：{{ markedCount }} 题</li>
                <li>异常次数：{{ totalViolationCount }}</li>
              </ul>
            </section>
          </aside>
        </div>
      </div>
    </section>

    <el-dialog v-model="submitConfirmVisible" title="提交确认" width="min(460px, 92vw)" append-to-body :z-index="2600">
      <p class="muted">提交后将无法继续修改答案。请确认当前保存状态正常，并再次检查是否还有未作答题目。</p>
      <template #footer>
        <el-button @click="submitConfirmVisible = false">再检查一下</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCurrent('manual')">确认提交</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.section-card {
  padding: 1rem;
}

.exam-overlay {
  position: fixed;
  inset: 0;
  z-index: 1500;
  background:
    radial-gradient(circle at top, rgba(207, 190, 162, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(244, 241, 236, 0.98), rgba(237, 233, 224, 0.99));
  overflow: auto;
}

.exam-stage {
  min-height: 100vh;
  padding: 1.4rem;
  display: grid;
  gap: 1.1rem;
}

.exam-header,
.metric-card,
.notice-card,
.question-card,
.answer-card-panel,
.aside-panel {
  border: 1px solid color-mix(in oklch, var(--line) 70%, white);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
}

.exam-header,
.notice-card,
.question-card,
.answer-card-panel,
.aside-panel {
  padding: 1.1rem 1.2rem;
}

.exam-header {
  position: sticky;
  top: 0;
  z-index: 3;
  display: grid;
  gap: 1rem;
  box-shadow: 0 18px 40px rgba(112, 97, 71, 0.08);
}

.exam-title-block h2,
.question-head h3 {
  margin: 0.3rem 0 0;
  font-family: 'Literata', Georgia, serif;
}

.exam-title-block p,
.notice-card p,
.toolbar-note {
  margin: 0;
}

.exam-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.metric-card {
  padding: 0.9rem 1rem;
  display: grid;
  gap: 0.35rem;
}

.metric-card span {
  color: var(--muted);
  font-size: 0.92rem;
}

.metric-card strong {
  color: var(--brand-deep);
  line-height: 1.4;
}

.metric-card--timer strong {
  font-size: 1.8rem;
}

.exam-toolbar,
.toolbar-actions,
.exam-notice,
.exam-content,
.question-head,
.aside-head,
.question-tags {
  display: flex;
  gap: 0.8rem;
}

.exam-toolbar,
.question-head,
.aside-head {
  justify-content: space-between;
  align-items: center;
}

.toolbar-actions {
  flex-wrap: wrap;
}

.question-tags {
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-note {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  color: var(--muted);
}

.exam-content {
  align-items: flex-start;
}

.question-column {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 1rem;
  padding-right: min(25rem, 33vw);
}

.aside-column {
  width: min(22rem, 30vw);
  display: grid;
  gap: 1rem;
  position: fixed;
  right: 1.4rem;
  top: 13.7rem;
  bottom: 1.4rem;
  overflow: auto;
  padding-right: 0.1rem;
}

.exam-notice {
  flex-wrap: wrap;
}

.notice-card {
  flex: 1 1 18rem;
}

.question-card {
  display: grid;
  gap: 1rem;
  box-shadow: 0 10px 30px rgba(112, 97, 71, 0.06);
}

.question-card.active {
  border-color: color-mix(in oklch, var(--brand) 45%, white);
  box-shadow: 0 16px 36px rgba(92, 119, 144, 0.12);
}

.choice-group {
  display: grid;
  gap: 0.8rem;
}

.material-block,
.stem-html,
.attachment-list {
  margin-top: 0.7rem;
}

.material-block,
.stem-html {
  line-height: 1.75;
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.blank-grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.device-check-list {
  display: grid;
  gap: 0.8rem;
  margin: 1rem 0;
}

.device-check-item {
  border: 1px solid color-mix(in oklch, var(--line) 72%, white);
  border-radius: 18px;
  padding: 0.85rem 1rem;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
}

.device-check-head {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  align-items: center;
}

.device-check-item p {
  margin: 0.45rem 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.ticket-sheet {
  display: grid;
  gap: 1rem;
}

.ticket-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.ticket-head h3 {
  margin: 0.2rem 0 0;
}

.ticket-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.ticket-grid article,
.ticket-note {
  border: 1px solid color-mix(in oklch, var(--line) 72%, white);
  border-radius: 18px;
  background: color-mix(in oklch, white 94%, var(--panel-soft));
  padding: 0.9rem 1rem;
}

.ticket-grid strong,
.ticket-note strong {
  display: block;
}

.ticket-grid span,
.ticket-note p {
  display: block;
  margin-top: 0.45rem;
  color: var(--muted);
  line-height: 1.6;
}

.answer-card-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.6rem;
  margin-top: 0.9rem;
}

.answer-card-cell {
  min-height: 2.9rem;
  border-radius: 16px;
  border: 1px solid color-mix(in oklch, var(--line) 72%, white);
  background: color-mix(in oklch, white 92%, var(--panel-soft));
  cursor: pointer;
  font-weight: 700;
  transition: all 0.2s ease;
}

.answer-card-cell.answered {
  background: color-mix(in oklch, var(--brand) 18%, white);
  color: var(--brand-deep);
}

.answer-card-cell.marked {
  box-shadow: inset 0 0 0 2px color-mix(in oklch, #c68b3c 48%, white);
}

.answer-card-cell.active {
  border-color: color-mix(in oklch, var(--brand) 50%, white);
  transform: translateY(-2px);
}

.status-list {
  margin: 0.9rem 0 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.55rem;
  color: var(--muted);
}

@media (max-width: 1180px) {
  .exam-metrics,
  .exam-content {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .aside-column {
    width: 100%;
    position: static;
    top: auto;
    right: auto;
    bottom: auto;
    overflow: visible;
  }

  .question-column {
    padding-right: 0;
  }

  .answer-card-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .blank-grid {
    grid-template-columns: 1fr;
  }

  .ticket-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .exam-stage {
    padding: 1rem;
  }

  .answer-card-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
