const questionTypeMap: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  SHORT_ANSWER: '简答题',
  FILL_BLANK: '填空题',
  ESSAY: '论述题',
  MATERIAL: '材料题'
}

const difficultyMap: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const reviewStatusMap: Record<string, string> = {
  DRAFT: '待审核',
  APPROVED: '已审核'
}

const paperModeMap: Record<string, string> = {
  MANUAL: '手工组卷',
  STRATEGY: '策略组卷',
  RANDOM: '随机组卷'
}

const examModeMap: Record<string, string> = {
  NORMAL: '正常考试',
  MAKEUP: '补考',
  DEFERRED: '缓考',
  RETAKE: '重考'
}

const answerSheetStatusMap: Record<string, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '作答中',
  SUBMITTED: '已提交',
  PARTIALLY_GRADED: '部分阅卷完成',
  GRADED: '已完成评分',
  REVIEW_PENDING: '待复核',
  REJUDGING: '待重判',
  PENDING_GRADING: '待阅卷',
  PUBLISHED: '已发布'
}

const messageTypeMap: Record<string, string> = {
  EXAM_PUBLISH: '考试发布提醒',
  EXAM_REMINDER: '开考前提醒',
  SCORE_PUBLISH: '成绩发布提醒',
  SCORE_APPEAL: '成绩申诉提醒',
  SCORE_APPEAL_RESULT: '成绩申诉处理结果',
  SECURITY_ALERT: '登录安全告警'
}

const notificationBusinessTypeMap: Record<string, string> = {
  EXAM_PUBLISH: '考试发布',
  EXAM_REMINDER: '开考前提醒',
  SCORE_PUBLISH: '成绩发布',
  SCORE_APPEAL: '成绩申诉',
  SCORE_APPEAL_RESULT: '申诉结果',
  SECURITY_ALERT: '安全告警'
}

const notificationChannelTypeMap: Record<string, string> = {
  IN_APP: '站内消息',
  MOCK_SMS: 'Mock 短信'
}

const notificationDeliveryStatusMap: Record<string, string> = {
  DELIVERED: '已投递',
  SKIPPED: '已跳过',
  FAILED: '失败'
}

const gradingReviewStatusMap: Record<string, string> = {
  PENDING: '待复核',
  IN_PROGRESS: '阅卷中',
  APPROVED: '复核通过',
  REJUDGE_REQUIRED: '退回重判'
}

const appealStatusMap: Record<string, string> = {
  NONE: '无申诉',
  SUBMITTED: '申诉待处理',
  APPROVED_REJUDGE: '申诉通过待重判',
  REJECTED: '申诉已驳回',
  RESOLVED: '申诉已完成'
}

const severityMap: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高'
}

const eventTypeMap: Record<string, string> = {
  TAB_SWITCH: '切换页面标签',
  WINDOW_BLUR: '窗口失焦',
  FULLSCREEN_EXIT: '退出全屏',
  AUTO_SAVE_FAILURE: '自动保存失败',
  COPY_ATTEMPT: '尝试复制',
  PASTE_ATTEMPT: '尝试粘贴',
  CONTEXT_MENU_BLOCKED: '右键菜单被拦截',
  SHORTCUT_BLOCKED: '高风险快捷键被拦截',
  DEVICE_CONTEXT: '记录设备上下文',
  DEVICE_CHECK_PASSED: '设备检测通过',
  DEVICE_CHECK_FAILED: '设备检测未通过'
}

export function labelQuestionType(value?: string) {
  return value ? (questionTypeMap[value] || value) : ''
}

export function labelDifficulty(value?: string) {
  return value ? (difficultyMap[value] || value) : ''
}

export function labelReviewStatus(value?: string) {
  return value ? (reviewStatusMap[value] || value) : ''
}

export function labelPaperMode(value?: string) {
  return value ? (paperModeMap[value] || value) : ''
}

export function labelExamMode(value?: string) {
  return value ? (examModeMap[value] || value) : ''
}

export function labelAnswerSheetStatus(value?: string) {
  return value ? (answerSheetStatusMap[value] || value) : ''
}

export function labelMessageType(value?: string) {
  return value ? (messageTypeMap[value] || value) : ''
}

export function labelNotificationBusinessType(value?: string) {
  return value ? (notificationBusinessTypeMap[value] || value) : ''
}

export function labelNotificationChannelType(value?: string) {
  return value ? (notificationChannelTypeMap[value] || value) : ''
}

export function labelNotificationDeliveryStatus(value?: string) {
  return value ? (notificationDeliveryStatusMap[value] || value) : ''
}

export function labelGradingReviewStatus(value?: string) {
  return value ? (gradingReviewStatusMap[value] || value) : ''
}

export function labelAppealStatus(value?: string) {
  return value ? (appealStatusMap[value] || value) : ''
}

export function labelSeverity(value?: string) {
  return value ? (severityMap[value] || value) : ''
}

export function labelEventType(value?: string) {
  return value ? (eventTypeMap[value] || value) : ''
}
