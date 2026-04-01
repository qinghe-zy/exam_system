import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'
import AdminLayout from '../layouts/AdminLayout.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import AnalysisView from '../views/exam/AnalysisView.vue'
import CandidateExamView from '../views/exam/CandidateExamView.vue'
import ExamPaperView from '../views/exam/ExamPaperView.vue'
import ExamPlanView from '../views/exam/ExamPlanView.vue'
import ExamRecordView from '../views/exam/ExamRecordView.vue'
import GradingView from '../views/exam/GradingView.vue'
import ProctorView from '../views/exam/ProctorView.vue'
import LoginView from '../views/login/LoginView.vue'
import NoticeView from '../views/notices/NoticeView.vue'
import QuestionBankView from '../views/exam/QuestionBankView.vue'
import MenuView from '../views/system/MenuView.vue'
import RoleView from '../views/system/RoleView.vue'
import UserView from '../views/system/UserView.vue'

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
          path: 'notices',
          name: 'notices',
          component: NoticeView,
          meta: { title: 'Notices' }
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
