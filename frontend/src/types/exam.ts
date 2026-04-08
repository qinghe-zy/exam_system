export interface QuestionBank {
  id: number
  questionCode: string
  subject: string
  questionType: string
  difficultyLevel: string
  stem: string
  stemHtml?: string
  materialContent?: string
  attachmentJson?: string
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
  usageCount?: number
}

export interface KnowledgePointQuotaItem {
  knowledgePoint: string
  questionCount: number
}

export interface AiQuestionDraftRequest {
  subject: string
  questionType: string
  difficultyLevel: string
  knowledgePoint: string
  chapterName?: string
  extraRequirements?: string
}

export interface AiQuestionDraftResult {
  subject: string
  questionType: string
  difficultyLevel: string
  stem: string
  optionsJson: string
  answerKey: string
  analysisText: string
  knowledgePoint: string
  chapterName?: string
  tags?: string
  defaultScore: number
  aiHint: string
}

export interface AiQuestionPolishRequest {
  subject: string
  questionType: string
  difficultyLevel: string
  stem: string
  optionsJson?: string
  answerKey?: string
  analysisText?: string
  knowledgePoint?: string
  chapterName?: string
}

export interface AiQuestionPolishResult {
  improvedStem: string
  improvedAnswerKey: string
  improvedAnalysisText: string
  suggestedOptionsJson: string
  aiHint: string
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
  stemHtml?: string
  materialContent?: string
  attachmentJson?: string
  optionsJson?: string
  maxScore: number
  answerContent?: string
  scoreAwarded?: number
  status: string
  markedFlag: number
  reviewLaterFlag?: number
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
  antiCheatLevel?: string
  antiCheatPolicy?: {
    blockCopyEnabled: number
    blockPasteEnabled: number
    blockContextMenuEnabled: number
    blockShortcutEnabled: number
    deviceLoggingEnabled: number
    deviceCheckEnabled: number
    blockOnDeviceCheckFail: number
    forbidMobileEntry: number
    requireFullscreenSupport: number
    minWindowWidth: number
    minWindowHeight: number
    blockedShortcutKeys: string[]
    allowedBrowserKeywords: string[]
  }
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
  reviewStatus?: string
  appealStatus?: string
}

export interface GradingWorkspace {
  answerSheetId: number
  examName: string
  candidateName: string
  objectiveScore: number
  subjectiveScore: number
  finalScore: number
  status: string
  reviewStatus?: string
  appealStatus?: string
  items: CandidateAnswerItem[]
}

export interface ExamRecord {
  id: number
  examPlanId: number
  answerSheetId: number
  candidateName: string
  examName: string
  paperName: string
  submittedAt: string
  objectiveScore: number
  subjectiveScore: number
  finalScore: number
  passedFlag: number
  publishedFlag: number
  reviewStatus?: string
  appealStatus?: string
  status: string
}

export interface ScoreAppeal {
  id: number
  scoreRecordId: number
  answerSheetId: number
  examPlanId: number
  userId: number
  candidateName: string
  examName: string
  appealReason: string
  expectedOutcome?: string
  status: string
  resolutionAction?: string
  processComment?: string
  processedBy?: number
  processedByName?: string
  submittedAt: string
  processedAt?: string
}

export interface CandidateScoreItem {
  questionId: number
  questionOrder: number
  questionCode?: string
  questionType: string
  stem?: string
  stemHtml?: string
  materialContent?: string
  attachmentJson?: string
  optionsJson?: string
  knowledgePoint?: string
  chapterName?: string
  answerContent?: string
  referenceAnswer?: string
  analysisText?: string
  maxScore?: number
  scoreAwarded?: number
  status: string
  reviewLaterFlag?: number
  reviewComment?: string
}

export interface CandidateWrongQuestion {
  questionId: number
  questionCode?: string
  questionType: string
  stem?: string
  stemHtml?: string
  materialContent?: string
  attachmentJson?: string
  optionsJson?: string
  knowledgePoint?: string
  chapterName?: string
  referenceAnswer?: string
  analysisText?: string
  latestRecordId?: number
  latestExamName?: string
  latestPaperName?: string
  latestSubmittedAt?: string
  latestAnswerContent?: string
  latestMaxScore?: number
  latestScoreAwarded?: number
  latestReviewLaterFlag?: number
  mistakeCount: number
}

export interface CandidateScoreDetail {
  id: number
  examPlanId: number
  answerSheetId: number
  examName: string
  paperName: string
  candidateName: string
  submittedAt: string
  objectiveScore: number
  subjectiveScore: number
  finalScore: number
  passedFlag: number
  publishedFlag: number
  reviewStatus?: string
  appealStatus?: string
  status: string
  items: CandidateScoreItem[]
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
  excellentRate: number
}

export interface AnalysisOverview {
  totalExamPlans: number
  totalAnswerSheets: number
  averageScore: number
  passRate: number
  excellentRate: number
  examPerformances: ExamPerformance[]
  organizationComparisons: Array<{
    organizationName: string
    candidateCount: number
    averageScore: number
    passRate: number
    excellentRate: number
  }>
  trendPoints: Array<{
    examPlanId: number
    examName: string
    periodLabel: string
    averageScore: number
    passRate: number
    excellentRate: number
  }>
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

export interface QualityDimension {
  dimensionName: string
  score: number
  level: string
  summary: string
}

export interface ExamQualityInsight {
  examPlanId: number
  examName: string
  candidateCount: number
  submittedCount: number
  gradedCount: number
  averageScore: number
  passRate: number
  level: string
  summary: string
  risk: string
}

export interface AnalysisQualityReport {
  generatedAt: string
  overallQualityScore: number
  overallQualityLevel: string
  summary: string
  riskSummary: string
  recommendations: string[]
  dimensionScores: QualityDimension[]
  examInsights: ExamQualityInsight[]
  weakKnowledgePoints: Array<{
    knowledgePoint: string
    averageScoreRate: number
    answerCount: number
  }>
  weakQuestions: Array<{
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
  clientIp?: string
  deviceFingerprint?: string
  deviceInfo?: string
  detailText?: string
  occurredAt: string
}
