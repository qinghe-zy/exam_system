import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'
import AdminLayout from '../layouts/AdminLayout.vue'

const DashboardView = () => import('../views/dashboard/DashboardView.vue')
const AnalysisView = () => import('../views/exam/AnalysisView.vue')
const CandidateExamView = () => import('../views/exam/CandidateExamView.vue')
const ExamPaperView = () => import('../views/exam/ExamPaperView.vue')
const ExamPlanView = () => import('../views/exam/ExamPlanView.vue')
const ExamRecordView = () => import('../views/exam/ExamRecordView.vue')
const GradingView = () => import('../views/exam/GradingView.vue')
const ProctorView = () => import('../views/exam/ProctorView.vue')
const LoginView = () => import('../views/login/LoginView.vue')
const MessageCenterView = () => import('../views/notices/MessageCenterView.vue')
const NoticeView = () => import('../views/notices/NoticeView.vue')
const QuestionBankView = () => import('../views/exam/QuestionBankView.vue')
const MenuView = () => import('../views/system/MenuView.vue')
const AuditLogView = () => import('../views/system/AuditLogView.vue')
const ConfigCenterView = () => import('../views/system/ConfigCenterView.vue')
const OrganizationView = () => import('../views/system/OrganizationView.vue')
const RoleView = () => import('../views/system/RoleView.vue')
const UserView = () => import('../views/system/UserView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: 'Login' }
    },
    {
      path: '/',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard'
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: 'Dashboard' }
        },
        {
          path: 'system/organizations',
          name: 'system-organizations',
          component: OrganizationView,
          meta: { title: 'Organizations' }
        },
        {
          path: 'system/users',
          name: 'system-users',
          component: UserView,
          meta: { title: 'Users' }
        },
        {
          path: 'system/roles',
          name: 'system-roles',
          component: RoleView,
          meta: { title: 'Roles' }
        },
        {
          path: 'system/menus',
          name: 'system-menus',
          component: MenuView,
          meta: { title: 'Menus' }
        },
        {
          path: 'system/audit-logs',
          name: 'system-audit-logs',
          component: AuditLogView,
          meta: { title: 'Audit Logs' }
        },
        {
          path: 'system/config-center',
          name: 'system-config-center',
          component: ConfigCenterView,
          meta: { title: 'Config Center' }
        },
        {
          path: 'notices',
          name: 'notices',
          component: NoticeView,
          meta: { title: 'Notices' }
        },
        {
          path: 'messages',
          name: 'messages',
          component: MessageCenterView,
          meta: { title: 'Message Center' }
        },
        {
          path: 'exam/questions',
          name: 'exam-questions',
          component: QuestionBankView,
          meta: { title: 'Question Bank' }
        },
        {
          path: 'exam/papers',
          name: 'exam-papers',
          component: ExamPaperView,
          meta: { title: 'Exam Papers' }
        },
        {
          path: 'exam/plans',
          name: 'exam-plans',
          component: ExamPlanView,
          meta: { title: 'Exam Plans' }
        },
        {
          path: 'exam/grading',
          name: 'exam-grading',
          component: GradingView,
          meta: { title: 'Grading Center' }
        },
        {
          path: 'exam/records',
          name: 'exam-records',
          component: ExamRecordView,
          meta: { title: 'Score Center' }
        },
        {
          path: 'exam/analytics',
          name: 'exam-analytics',
          component: AnalysisView,
          meta: { title: 'Analytics' }
        },
        {
          path: 'candidate/exams',
          name: 'candidate-exams',
          component: CandidateExamView,
          meta: { title: 'My Exams' }
        },
        {
          path: 'exam/proctor',
          name: 'exam-proctor',
          component: ProctorView,
          meta: { title: 'Proctor Events' }
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.initialized) {
    await authStore.bootstrap()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login' && authStore.isAuthenticated) {
    return '/dashboard'
  }

  if (typeof to.meta.title === 'string') {
    document.title = `${to.meta.title} | Projectexample Exam System`
  }
})

export default router
