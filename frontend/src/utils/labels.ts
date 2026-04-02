const questionTypeMap: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  SHORT_ANSWER: '简答题'
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

const answerSheetStatusMap: Record<string, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '作答中',
  SUBMITTED: '已提交',
  PARTIALLY_GRADED: '部分阅卷完成',
  GRADED: '已完成评分',
  PENDING_GRADING: '待阅卷',
  PUBLISHED: '已发布'
}

const messageTypeMap: Record<string, string> = {
  EXAM_PUBLISH: '考试发布提醒',
  SCORE_PUBLISH: '成绩发布提醒'
}

const severityMap: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高'
}

const eventTypeMap: Record<string, string> = {
  TAB_SWITCH: '切换页面标签',
  WINDOW_BLUR: '窗口失焦',
  FULLSCREEN_EXIT: '退出全屏'
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

export function labelAnswerSheetStatus(value?: string) {
  return value ? (answerSheetStatusMap[value] || value) : ''
}

export function labelMessageType(value?: string) {
  return value ? (messageTypeMap[value] || value) : ''
}

export function labelSeverity(value?: string) {
  return value ? (severityMap[value] || value) : ''
}

export function labelEventType(value?: string) {
  return value ? (eventTypeMap[value] || value) : ''
}
