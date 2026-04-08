import http from './http'
import type {
  AnalysisQualityReport,
  AnalysisOverview,
  AiQuestionDraftRequest,
  AiQuestionDraftResult,
  AiQuestionPolishRequest,
  AiQuestionPolishResult,
  AntiCheatEvent,
  CandidateExam,
  CandidateScoreDetail,
  CandidateWrongQuestion,
  CandidateExamWorkspace,
  ExamPaper,
  ExamPlan,
  ExamRecord,
  GradingTask,
  GradingWorkspace,
  KnowledgePointQuotaItem,
  QuestionBank,
  ScoreAppeal
} from '../types/exam'

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
  return `exam-${Math.abs(hash)}`
}

export function fetchQuestions() {
  return http.get<never, QuestionBank[]>('/api/exam/questions')
}

export function createQuestion(payload: Omit<QuestionBank, 'id'>) {
  return http.post<never, QuestionBank>('/api/exam/questions', payload)
}

export function exportQuestions() {
  return http.get<never, QuestionBank[]>('/api/exam/questions/export')
}

export function importQuestions(payload: { questions: Omit<QuestionBank, 'id'>[] }) {
  return http.post<never, QuestionBank[]>('/api/exam/questions/import', payload)
}

export function autoGroupQuestionsByKnowledgePoint(payload: {
  subject: string
  difficultyLevel?: string
  questionType?: string
  quotas: KnowledgePointQuotaItem[]
}) {
  return http.post<never, QuestionBank[]>('/api/exam/questions/auto-group/knowledge-points', payload)
}

export function generateAiQuestionDraft(payload: AiQuestionDraftRequest) {
  return http.post<never, AiQuestionDraftResult>('/api/exam/questions/ai/draft', payload)
}

export function polishQuestionWithAi(payload: AiQuestionPolishRequest) {
  return http.post<never, AiQuestionPolishResult>('/api/exam/questions/ai/polish', payload)
}

export function updateQuestion(id: number, payload: Omit<QuestionBank, 'id'>) {
  return http.put<never, QuestionBank>(`/api/exam/questions/${id}`, payload)
}

export function deleteQuestion(id: number) {
  return http.delete(`/api/exam/questions/${id}`)
}

export function fetchPapers() {
  return http.get<never, ExamPaper[]>('/api/exam/papers')
}

export function fetchPaper(id: number) {
  return http.get<never, ExamPaper>(`/api/exam/papers/${id}`)
}

export function createPaper(payload: Omit<ExamPaper, 'id' | 'questionCount'>) {
  return http.post<never, ExamPaper>('/api/exam/papers', payload)
}

export function updatePaper(id: number, payload: Omit<ExamPaper, 'id' | 'questionCount'>) {
  return http.put<never, ExamPaper>(`/api/exam/papers/${id}`, payload)
}

export function deletePaper(id: number) {
  return http.delete(`/api/exam/papers/${id}`)
}

export function fetchExamPlans() {
  return http.get<never, ExamPlan[]>('/api/exam/plans')
}

export function createExamPlan(payload: Omit<ExamPlan, 'id' | 'candidateCount' | 'submittedCount' | 'paperName' | 'subject'>) {
  return http.post<never, ExamPlan>('/api/exam/plans', payload)
}

export function updateExamPlan(id: number, payload: Omit<ExamPlan, 'id' | 'candidateCount' | 'submittedCount' | 'paperName' | 'subject'>) {
  return http.put<never, ExamPlan>(`/api/exam/plans/${id}`, payload)
}

export function deleteExamPlan(id: number) {
  return http.delete(`/api/exam/plans/${id}`)
}

export function fetchMyExams() {
  return http.get<never, CandidateExam[]>('/api/exam/candidate/my-exams')
}

export function fetchCandidateWorkspace(examPlanId: number, examPassword?: string) {
  return http.get<never, CandidateExamWorkspace>(`/api/exam/candidate/exams/${examPlanId}`, {
    params: examPassword ? { examPassword } : undefined,
    headers: {
      'X-Device-Fingerprint': getDeviceFingerprint(),
      'X-Device-Info': buildDeviceInfo()
    }
  })
}

export function saveCandidateAnswers(examPlanId: number, answers: { questionId: number; answerContent?: string }[]) {
  return http.post<never, CandidateExamWorkspace>(`/api/exam/candidate/exams/${examPlanId}/save`, { answers })
}

export function submitCandidateAnswers(examPlanId: number, answers: { questionId: number; answerContent?: string }[]) {
  return http.post<never, CandidateExamWorkspace>(`/api/exam/candidate/exams/${examPlanId}/submit`, { answers })
}

export function reportCandidateEvent(
  examPlanId: number,
  payload: {
    answerSheetId?: number
    eventType: string
    severity: string
    leaveCount?: number
    triggeredAutoSave?: number
    saveVersion?: number
    deviceFingerprint?: string
    deviceInfo?: string
    detailText?: string
  }
) {
  return http.post(`/api/exam/candidate/exams/${examPlanId}/events`, payload)
}

export function fetchGradingTasks() {
  return http.get<never, GradingTask[]>('/api/exam/grading/tasks')
}

export function fetchGradingWorkspace(answerSheetId: number) {
  return http.get<never, GradingWorkspace>(`/api/exam/grading/${answerSheetId}`)
}

export function submitGrading(answerSheetId: number, gradeItems: { answerItemId: number; scoreAwarded: number; reviewComment?: string }[]) {
  return http.post<never, GradingWorkspace>(`/api/exam/grading/${answerSheetId}/submit`, { gradeItems })
}

export function reviewGrading(answerSheetId: number, action: 'APPROVE' | 'REJECT_REJUDGE', reviewComment?: string) {
  return http.post<never, GradingWorkspace>(`/api/exam/grading/${answerSheetId}/review`, { action, reviewComment })
}

export function fetchExamRecords() {
  return http.get<never, ExamRecord[]>('/api/exam/records')
}

export async function exportExamRecordsCsv() {
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083'}/api/exam/records/export`, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('exam-system-template-token') || ''}`
    }
  })
  return response.text()
}

export function fetchMyExamRecords() {
  return http.get<never, ExamRecord[]>('/api/exam/records/my')
}

export function fetchMyExamRecordDetail(id: number) {
  return http.get<never, CandidateScoreDetail>(`/api/exam/records/my/${id}`)
}

export function fetchScoreAppeals(scoreRecordId?: number) {
  return http.get<never, ScoreAppeal[]>('/api/exam/score-appeals', {
    params: scoreRecordId ? { scoreRecordId } : undefined
  })
}

export function fetchMyScoreAppeals(scoreRecordId: number) {
  return http.get<never, ScoreAppeal[]>(`/api/exam/score-appeals/my/${scoreRecordId}`)
}

export function submitScoreAppeal(scoreRecordId: number, payload: { appealReason: string; expectedOutcome?: string }) {
  return http.post<never, ScoreAppeal>(`/api/exam/score-appeals/my/${scoreRecordId}`, payload)
}

export function processScoreAppeal(appealId: number, payload: { action: 'REJECT' | 'REJUDGE'; processComment?: string }) {
  return http.post<never, ScoreAppeal>(`/api/exam/score-appeals/${appealId}/process`, payload)
}

export function fetchMyWrongQuestions() {
  return http.get<never, CandidateWrongQuestion[]>('/api/exam/records/my/wrong-book')
}

export function fetchAnalysisOverview() {
  return http.get<never, AnalysisOverview>('/api/exam/analytics/overview')
}

export function fetchAnalysisQualityReport() {
  return http.get<never, AnalysisQualityReport>('/api/exam/analytics/quality-report')
}

export async function exportAnalysisOverviewCsv() {
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083'}/api/exam/analytics/export`, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('exam-system-template-token') || ''}`
    }
  })
  return response.text()
}

export async function exportAnalysisQualityReportMarkdown() {
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083'}/api/exam/analytics/quality-report/export`, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('exam-system-template-token') || ''}`
    }
  })
  return response.text()
}

export function fetchProctorEvents() {
  return http.get<never, AntiCheatEvent[]>('/api/exam/proctor/events')
}
