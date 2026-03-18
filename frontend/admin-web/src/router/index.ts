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
        {
          path: 'import-export',
          name: 'import-export',
          component: () => import('@/views/imports/ImportExportView.vue'),
          meta: { title: '导入导出' },
        },
        {
          path: 'coupon-templates',
          name: 'coupon-templates',
          component: () => import('@/views/coupons/CouponTemplatesView.vue'),
          meta: { title: '券模板' },
        },
        {
          path: 'coupon-rules',
          name: 'coupon-rules',
          component: () => import('@/views/coupons/CouponRulesView.vue'),
          meta: { title: '发券规则' },
        },
        {
          path: 'agent-groups',
          name: 'agent-groups',
          component: () => import('@/views/agents/AgentGroupsView.vue'),
          meta: { title: 'Agent 授权' },
        },
        {
          path: 'org-units',
          name: 'org-units',
          component: () => import('@/views/org-units/OrgUnitsView.vue'),
          meta: { title: '组织架构' },
        },
        {
          path: 'water-alerts',
          name: 'water-alerts',
          component: () => import('@/views/water/WaterAlertsView.vue'),
          meta: { title: '水量预警' },
        },
        {
          path: 'dunning',
          name: 'dunning',
          component: () => import('@/views/dunning/DunningView.vue'),
          meta: { title: '催缴任务' },
        },
        {
          path: 'audit-logs',
          name: 'audit-logs',
          component: () => import('@/views/audit/AuditLogsView.vue'),
          meta: { title: '审计日志' },
        },
        {
          path: 'invoice-applications',
          name: 'invoice-applications',
          component: () => import('@/views/invoices/InvoiceApplicationsView.vue'),
          meta: { title: '发票申请' },
        },
        {
          path: 'ai-runtime-config',
          name: 'ai-runtime-config',
          component: () => import('@/views/ai/AiRuntimeConfigView.vue'),
          meta: { title: 'Agent 运行配置' },
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
