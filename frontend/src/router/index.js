import { createRouter, createWebHistory } from 'vue-router'
import ForumLayout from '@/shared/components/ForumLayout.vue'
import HomeView from '@/apps/Forum/views/HomeView.vue'
import AdminLayout from '@/apps/Admin/views/AdminLayout.vue'
import MenuConfig from '@/apps/Admin/views/MenuConfig.vue'
import CategoryConfig from '@/apps/Admin/views/CategoryConfig.vue'
import CategoryGroupConfig from '@/apps/Admin/views/CategoryGroupConfig.vue'
import LabelConfig from '@/apps/Admin/views/LabelConfig.vue'
import ReactionIconConfig from '@/apps/Admin/views/ReactionIconConfig.vue'
import ThreadManagement from '@/apps/Admin/views/ThreadManagement.vue'
import AdminCreateThread from '@/apps/Admin/views/AdminCreateThread.vue'
import Login from '@/apps/Auth/views/Login.vue'
import Register from '@/apps/Auth/views/Register.vue'
import CreateThread from '@/apps/Forum/views/CreateThread.vue'
import ThreadDetail from '@/apps/Forum/views/ThreadDetail.vue'
import CategoryView from '@/apps/Forum/views/CategoryView.vue'
import LatestThreadsView from '@/apps/Forum/views/LatestThreadsView.vue'
import ForgotPassword from '@/apps/Auth/views/ForgotPassword.vue'
import AddConversation from '@/apps/Forum/views/AddConversation.vue'
import ConversationDetail from '@/apps/Forum/views/ConversationDetail.vue'
import ConversationList from '@/apps/Forum/views/ConversationList.vue'
import UserManagement from '@/apps/Admin/views/UserManagement.vue'
import SystemSettingConfig from '@/apps/Admin/views/SystemSettingConfig.vue'
import ReportManagement from '@/apps/Admin/views/ReportManagement.vue'
import ReceivedReactions from '@/apps/Forum/views/ReceivedReactions.vue'
import UserProfile from '@/apps/Forum/views/UserProfile.vue'
import ChangePassword from '@/apps/Forum/views/ChangePassword.vue'
import NotificationsList from '@/apps/Forum/views/NotificationsList.vue'
import { activeTracker } from '@/shared/services/activeTracker'

const routes = [
  {
    path: '/',
    component: ForumLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: HomeView,
        alias: 'trang-chu'
      },
      {
        path: 'latest',
        name: 'LatestThreads',
        component: LatestThreadsView
      },
      {
        path: 'category/:id',
        name: 'CategoryDetail',
        component: CategoryView
      },
      {
        path: 'thread/:id',
        name: 'ThreadDetail',
        component: ThreadDetail
      },
      {
        path: 'create-thread',
        name: 'CreateThread',
        component: CreateThread,
        meta: { requiresAuth: true }
      },
      {
        path: 'conversations',
        name: 'ConversationList',
        component: ConversationList,
        meta: { requiresAuth: true }
      },
      {
        path: 'conversations/add',
        name: 'AddConversation',
        component: AddConversation,
        meta: { requiresAuth: true }
      },
      {
        path: 'conversations/:id',
        name: 'ConversationDetail',
        component: ConversationDetail,
        meta: { requiresAuth: true }
      },
      {
        path: 'account/received-reactions',
        name: 'ReceivedReactions',
        component: ReceivedReactions,
        meta: { requiresAuth: true }
      },
      {
        path: 'account/notifications',
        name: 'NotificationsList',
        component: NotificationsList,
        meta: { requiresAuth: true }
      },
      {
        path: 'account/profile',
        name: 'UserProfile',
        component: UserProfile,
        meta: { requiresAuth: true }
      },
      {
        path: 'account/password',
        name: 'ChangePassword',
        component: ChangePassword,
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { guestOnly: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: ForgotPassword,
    meta: { guestOnly: true }
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'menu',
        name: 'AdminMenu',
        component: MenuConfig
      },
      {
        path: 'category',
        name: 'AdminCategory',
        component: CategoryConfig
      },
      {
        path: 'labels',
        name: 'AdminLabels',
        component: LabelConfig
      },
      {
        path: 'reaction-icons',
        name: 'AdminReactionIcons',
        component: ReactionIconConfig
      },
      {
        path: 'category-group',
        name: 'AdminCategoryGroup',
        component: CategoryGroupConfig
      },
      {
        path: 'category-group/:groupId/categories',
        name: 'AdminCategoryGroupDetail',
        component: CategoryConfig
      },
      {
        path: 'threads',
        name: 'AdminThreads',
        component: ThreadManagement
      },
      {
        path: 'threads/create',
        name: 'AdminThreadCreate',
        component: AdminCreateThread
      },
      {
        path: 'threads/edit/:id',
        name: 'AdminThreadEdit',
        component: AdminCreateThread
      },
      {
        path: 'threads/view/:id',
        name: 'AdminThreadView',
        component: AdminCreateThread
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: UserManagement
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: SystemSettingConfig
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: ReportManagement
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  let userRoles = []
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      userRoles = userData.roles || []
    }
  } catch (e) {
    userRoles = []
  }

  const isAuthRequired = to.matched.some(record => record.meta.requiresAuth)
  const isAdminRequired = to.matched.some(record => record.meta.requiresAdmin)
  const isGuestOnly = to.matched.some(record => record.meta.guestOnly)

  if (isAuthRequired) {
    if (!token) {
      return next('/login')
    }
    
    // Kiểm tra quyền Admin hoặc Super Admin
    if (isAdminRequired && !userRoles.includes('ROLE_ADMIN') && !userRoles.includes('ROLE_SUPER_ADMIN')) {
      return next({ name: 'Home' }) // Không có quyền Admin hoặc Super Admin đẩy về Trang chủ
    }
    
    return next()
  } 
  
  if (isGuestOnly) {
    if (token) {
      return next({ name: 'Home' })
    }
    return next()
  }

  next()
})

router.afterEach(() => {
  activeTracker.updateActive()
})

export default router
