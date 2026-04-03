import http from './http'
import type {
  AnalysisOverview,
  AiQuestionDraftRequest,
  AiQuestionDraftResult,
  AiQuestionPolishRequest,
  AiQuestionPolishResult,
  AntiCheatEvent,
  CandidateExam,
  CandidateScoreDetail,
  CandidateExamWorkspace,
  ExamPaper,
  ExamPlan,
  ExamRecord,
  GradingTask,
  GradingWorkspace,
  KnowledgePointQuotaItem,
  QuestionBank
} from '../types/exam'

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
    params: examPassword ? { examPassword } : undefined
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
  payload: { answerSheetId?: number; eventType: string; severity: string; leaveCount?: number; triggeredAutoSave?: number; saveVersion?: number; detailText?: string }
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

export function fetchExamRecords() {
  return http.get<never, ExamRecord[]>('/api/exam/records')
}

export function fetchMyExamRecords() {
  return http.get<never, ExamRecord[]>('/api/exam/records/my')
}

export function fetchMyExamRecordDetail(id: number) {
  return http.get<never, CandidateScoreDetail>(`/api/exam/records/my/${id}`)
}

export function fetchAnalysisOverview() {
  return http.get<never, AnalysisOverview>('/api/exam/analytics/overview')
}

export function fetchProctorEvents() {
  return http.get<never, AntiCheatEvent[]>('/api/exam/proctor/events')
}
