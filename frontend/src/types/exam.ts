export interface QuestionBank {
  id: number
  questionCode: string
  subject: string
  questionType: string
  difficultyLevel: string
  stem: string
  optionsJson?: string
  answerKey: string
  analysisText?: string
  knowledgePoint?: string
  chapterName?: string
  sourceName?: string
  tags?: string
  defaultScore: number
  reviewerStatus: string
  versionNo: number
  status: number
}

export interface PaperRuleConfigItem {
  code: string
  label: string
  count: number
  score?: number | null
}

export interface PaperQuestionItem {
  questionId: number
  sortNo: number
  score: number
  requiredFlag: number
  questionCode?: string
  questionType?: string
  difficultyLevel?: string
  stem?: string
}

export interface ExamPaper {
  id: number
  paperCode: string
  paperName: string
  subject: string
  assemblyMode: string
  descriptionText?: string
  paperVersion?: string
  remarkText?: string
  durationMinutes: number
  totalScore: number
  passScore: number
  questionCount: number
  shuffleEnabled: number
  questionTypeConfigs: PaperRuleConfigItem[]
  difficultyConfigs: PaperRuleConfigItem[]
  publishStatus: number
  questionItems: PaperQuestionItem[]
}

export interface ExamPlan {
  id: number
  examCode: string
  examName: string
  paperId: number
  paperName: string
  subject: string
  startTime: string
  endTime: string
  durationMinutes: number
  passScore: number
  candidateScope: string
  attemptLimit: number
  examPassword?: string
  lateEntryMinutes: number
  earlySubmitMinutes: number
  autoSubmitEnabled: number
  antiCheatLevel: string
  instructionText?: string
  status: number
  publishStatus: number
  candidateCount: number
  submittedCount: number
  candidateUserIds: number[]
}

export interface CandidateExam {
  examPlanId: number
  examName: string
  paperName: string
  subject: string
  startTime: string
  endTime: string
  entryDeadlineAt?: string
  answerDeadlineAt?: string
  durationMinutes: number
  candidateStatus: string
  attemptCount: number
  answerSheetStatus: string
}

export interface CandidateAnswerItem {
  answerItemId?: number
  questionId: number
  questionOrder: number
  questionCode?: string
  questionType: string
  stem: string
  optionsJson?: string
  maxScore: number
  answerContent?: string
  scoreAwarded?: number
  status: string
  markedFlag: number
  reviewComment?: string
}

export interface CandidateExamWorkspace {
  examPlanId: number
  examName: string
  paperName: string
  subject: string
  instructionText?: string
  startTime: string
  endTime: string
  entryDeadlineAt: string
  answerDeadlineAt: string
  durationMinutes: number
  remainingSeconds: number
  answerSheetId: number
  answerSheetStatus: string
  autoSubmitEnabled: number
  autoSubmitFlag: number
  saveVersion: number
  shuffleEnabled: number
  paperVersion?: string
  items: CandidateAnswerItem[]
}

export interface GradingTask {
  answerSheetId: number
  examName: string
  candidateName: string
  submittedAt: string
  objectiveScore: number
  subjectiveQuestionCount: number
  pendingQuestionCount: number
  status: string
}

export interface GradingWorkspace {
  answerSheetId: number
  examName: string
  candidateName: string
  objectiveScore: number
  subjectiveScore: number
  finalScore: number
  items: CandidateAnswerItem[]
}

export interface ExamRecord {
  id: number
  candidateName: string
  examName: string
  paperName: string
  submittedAt: string
  objectiveScore: number
  subjectiveScore: number
  finalScore: number
  passedFlag: number
  publishedFlag: number
  status: string
}

export interface ExamPerformance {
  examPlanId: number
  examName: string
  candidateCount: number
  submittedCount: number
  gradedCount: number
  averageScore: number
  highestScore: number
  lowestScore: number
  passRate: number
}

export interface AnalysisOverview {
  totalExamPlans: number
  totalAnswerSheets: number
  averageScore: number
  passRate: number
  examPerformances: ExamPerformance[]
  rankings: Array<{
    rankNo: number
    candidateName: string
    examName: string
    finalScore: number
  }>
  scoreBands: Array<{
    bandName: string
    candidateCount: number
  }>
  knowledgePoints: Array<{
    knowledgePoint: string
    averageScoreRate: number
    answerCount: number
  }>
  questionScoreRates: Array<{
    questionId: number
    questionCode?: string
    stem?: string
    averageScoreRate: number
    answerCount: number
  }>
}

export interface AntiCheatEvent {
  id: number
  examPlanId: number
  examName?: string
  answerSheetId?: number
  userId: number
  candidateName?: string
  eventType: string
  severity: string
  leaveCount?: number
  triggeredAutoSave?: number
  saveVersion?: number
  detailText?: string
  occurredAt: string
}
