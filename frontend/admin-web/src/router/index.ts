import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'bills',
          name: 'bills',
          component: () => import('@/views/bills/BillsView.vue'),
          meta: { title: '账单管理' },
        },
        {
          path: 'fee-rules',
          name: 'fee-rules',
          component: () => import('@/views/fee-rules/FeeRulesView.vue'),
          meta: { title: '费用规则' },
        },
        {
          path: 'water-readings',
          name: 'water-readings',
          component: () => import('@/views/water/WaterReadingsView.vue'),
          meta: { title: '水表与抄表' },
        },
        {
          path: 'billing-generate',
          name: 'billing-generate',
          component: () => import('@/views/billing/BillingGenerateView.vue'),
          meta: { title: '账单生成' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (authStore.hasToken && !authStore.profileLoaded) {
    try {
      await authStore.fetchProfile()
    } catch {
      authStore.clearSession()
    }
  }

  if (to.meta.requiresAuth && !authStore.hasToken) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.guestOnly && authStore.hasToken) {
    return '/dashboard'
  }

  return true
})

export default router
