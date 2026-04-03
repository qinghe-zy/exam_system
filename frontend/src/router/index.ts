import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '../stores/auth'
import AdminLayout from '../layouts/AdminLayout.vue'
import type { SystemMenu } from '../api/system'

const DashboardView = () => import('../views/dashboard/DashboardView.vue')
const AnalysisView = () => import('../views/exam/AnalysisView.vue')
const CandidateExamView = () => import('../views/exam/CandidateExamView.vue')
const CandidateScoreView = () => import('../views/exam/CandidateScoreView.vue')
const ExamPaperView = () => import('../views/exam/ExamPaperView.vue')
const ExamPaperBuilderView = () => import('../views/exam/ExamPaperBuilderView.vue')
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
      meta: { title: '登录' }
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
          meta: { title: '首页看板' }
        },
        {
          path: 'system/organizations',
          name: 'system-organizations',
          component: OrganizationView,
          meta: { title: '组织管理' }
        },
        {
          path: 'system/users',
          name: 'system-users',
          component: UserView,
          meta: { title: '用户管理' }
        },
        {
          path: 'system/roles',
          name: 'system-roles',
          component: RoleView,
          meta: { title: '角色管理' }
        },
        {
          path: 'system/menus',
          name: 'system-menus',
          component: MenuView,
          meta: { title: '菜单管理' }
        },
        {
          path: 'system/audit-logs',
          name: 'system-audit-logs',
          component: AuditLogView,
          meta: { title: '审计日志' }
        },
        {
          path: 'system/config-center',
          name: 'system-config-center',
          component: ConfigCenterView,
          meta: { title: '配置中心' }
        },
        {
          path: 'notices',
          name: 'notices',
          component: NoticeView,
          meta: { title: '公告管理' }
        },
        {
          path: 'messages',
          name: 'messages',
          component: MessageCenterView,
          meta: { title: '消息中心' }
        },
        {
          path: 'exam/questions',
          name: 'exam-questions',
          component: QuestionBankView,
          meta: { title: '题库管理' }
        },
        {
          path: 'exam/papers',
          name: 'exam-papers',
          component: ExamPaperView,
          meta: { title: '试卷管理' }
        },
        {
          path: 'exam/papers/create',
          name: 'exam-paper-create',
          component: ExamPaperBuilderView,
          meta: { title: '新建试卷' }
        },
        {
          path: 'exam/papers/:paperId/edit',
          name: 'exam-paper-edit',
          component: ExamPaperBuilderView,
          meta: { title: '编辑试卷' }
        },
        {
          path: 'exam/plans',
          name: 'exam-plans',
          component: ExamPlanView,
          meta: { title: '考试发布' }
        },
        {
          path: 'exam/grading',
          name: 'exam-grading',
          component: GradingView,
          meta: { title: '阅卷中心' }
        },
        {
          path: 'exam/records',
          name: 'exam-records',
          component: ExamRecordView,
          meta: { title: '成绩中心' }
        },
        {
          path: 'exam/analytics',
          name: 'exam-analytics',
          component: AnalysisView,
          meta: { title: '成绩分析' }
        },
        {
          path: 'candidate/exams',
          name: 'candidate-exams',
          component: CandidateExamView,
          meta: { title: '我的考试' }
        },
        {
          path: 'candidate/scores',
          name: 'candidate-scores',
          component: CandidateScoreView,
          meta: { title: '我的成绩' }
        },
        {
          path: 'exam/proctor',
          name: 'exam-proctor',
          component: ProctorView,
          meta: { title: '监考事件' }
        }
      ]
    }
  ]
})

function flattenPagePaths(menus: SystemMenu[]): string[] {
  const result: string[] = []
  const walk = (items: SystemMenu[]) => {
    items.forEach((item) => {
      if (item.path && (item.menuType === 'PAGE' || Boolean(item.component))) {
        result.push(item.path)
      }
      if (item.children?.length) {
        walk(item.children)
      }
    })
  }
  walk(menus)
  return result
}

function resolveFallbackPath(menus: SystemMenu[]) {
  const pagePaths = flattenPagePaths(menus)
  if (pagePaths.includes('/dashboard')) {
    return '/dashboard'
  }
  return pagePaths[0] || '/dashboard'
}

function canAccessPath(path: string, menus: SystemMenu[]) {
  const pagePaths = flattenPagePaths(menus)
  return pagePaths.some((menuPath) => path === menuPath || path.startsWith(`${menuPath}/`))
}

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

  if (to.meta.requiresAuth && authStore.isAuthenticated && to.path !== '/' && !canAccessPath(to.path, authStore.menus)) {
    ElMessage.warning('当前账号无权访问该页面，已为你切换到可访问入口')
    return resolveFallbackPath(authStore.menus)
  }

  if (typeof to.meta.title === 'string') {
    document.title = `${to.meta.title} | 在线考试系统`
  }
})

export default router
