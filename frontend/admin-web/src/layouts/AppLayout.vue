<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, DataBoard, Document, Files, Notebook, OfficeBuilding, Odometer, SetUp } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuGroups = [
  {
    label: '经营总览',
    items: [
      { path: '/dashboard', label: '仪表盘', description: '查看本月经营摘要、实收与欠费关键指标。', icon: Odometer },
      { path: '/bills', label: '账单管理', description: '按账期和状态检索账单，并查看详细收费条目。', icon: Document },
      { path: '/billing-generate', label: '账单生成', description: '按月生成物业费与水费账单，维持收费闭环。', icon: DataBoard },
    ],
  },
  {
    label: '计费配置',
    items: [
      { path: '/fee-rules', label: '费用规则', description: '维护物业费、水费及阶梯价策略。', icon: Notebook },
      { path: '/water-readings', label: '水表与抄表', description: '配置水表并录入抄表数据，支撑水费开单。', icon: SetUp },
      { path: '/water-alerts', label: '水量预警', description: '跟踪异常水量并辅助定位抄表风险。', icon: Bell },
      { path: '/coupon-templates', label: '券模板', description: '维护支付前抵扣券与奖励券模板。', icon: Notebook },
      { path: '/coupon-rules', label: '发券规则', description: '配置自动或手动发券规则，连接支付与营销。', icon: DataBoard },
    ],
  },
  {
    label: '运营协同',
    items: [
      { path: '/import-export', label: '导入导出', description: '处理批量数据导入导出与异步任务。', icon: Document },
      { path: '/invoice-applications', label: '发票申请', description: '跟踪发票申请处理状态与回执。', icon: Files },
      { path: '/dunning', label: '催缴任务', description: '管理欠费催缴任务和执行进度。', icon: DataBoard },
      { path: '/agent-groups', label: 'Agent 授权', description: '维护代理人分组权限和数据范围。', icon: SetUp },
      { path: '/org-units', label: '组织架构', description: '管理小区、组织单元与职责归属。', icon: OfficeBuilding },
    ],
  },
  {
    label: '治理与系统',
    items: [
      { path: '/audit-logs', label: '审计日志', description: '按业务、操作人和时间回溯关键后台操作。', icon: Files },
      { path: '/ai-runtime-config', label: 'Agent 运行配置', description: '维护 Agent 运行参数与相关开关。', icon: SetUp },
    ],
  },
] as const

const allMenus = menuGroups.flatMap((group) =>
  group.items.map((item) => ({
    ...item,
    groupLabel: group.label,
  })),
)

const activeMenu = computed(() => route.path)
const currentMenu = computed(() => allMenus.find((menu) => route.path.startsWith(menu.path)))
const pageTitle = computed(() => (route.meta.title as string) || currentMenu.value?.label || '物业管理后台')
const currentGroupLabel = computed(() => currentMenu.value?.groupLabel || '后台工作台')
const totalMenuCount = allMenus.length
const userName = computed(() => authStore.profile?.realName || '管理员')
const groupScopeText = computed(() => {
  const groupCount = authStore.profile?.groupIds?.length ?? 0
  return groupCount > 0 ? `已授权 ${groupCount} 个用户组` : '全局管理范围'
})

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside class="app-sidebar">
      <div class="app-sidebar__inner">
        <div class="app-logo">
          <span class="app-logo__badge">运营中心</span>
          <span class="app-logo__title">物业运营管理平台</span>
          <span class="app-logo__subtitle">账单收费、计费配置、运营协同与治理审计统一入口</span>
        </div>

        <div class="app-nav-groups">
          <section v-for="group in menuGroups" :key="group.label" class="app-nav-group">
            <div class="app-nav-group__label">{{ group.label }}</div>
            <el-menu
              :default-active="activeMenu"
              class="app-menu"
              background-color="transparent"
              text-color="rgba(255,255,255,0.82)"
              active-text-color="#ffffff"
              router
            >
              <el-menu-item v-for="menu in group.items" :key="menu.path" :index="menu.path">
                <el-icon><component :is="menu.icon" /></el-icon>
                <span>{{ menu.label }}</span>
              </el-menu-item>
            </el-menu>
          </section>
        </div>

      </div>
    </el-aside>

    <el-container class="app-workspace">
      <el-header class="app-header">
        <div class="app-header__surface">
          <div class="app-header__meta">
            <div class="app-header__topbar">
              <span class="app-header__section-label">{{ currentGroupLabel }}</span>
            </div>
            <h1 class="app-header__meta-title">{{ pageTitle }}</h1>
          </div>

          <div class="app-header__actions">
            <div class="app-user-card">
              <span class="app-user-card__name">{{ userName }}</span>
            </div>
            <el-button type="primary" plain @click="handleLogout">安全退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main class="app-main">
        <div class="app-content-shell">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>
