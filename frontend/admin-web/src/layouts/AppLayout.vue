<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, DataBoard, Document, Files, Notebook, OfficeBuilding, Odometer, Operation, SetUp } from '@element-plus/icons-vue'

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
const pageDescription = computed(() => currentMenu.value?.description || '保持现有后台能力不变，聚焦高频业务操作与状态追踪。')
const currentGroupLabel = computed(() => currentMenu.value?.groupLabel || '后台工作台')
const totalMenuCount = allMenus.length
const menuGroupCount = menuGroups.length
const userName = computed(() => authStore.profile?.realName || '管理员')
const mobileMenuVisible = ref(false)
const roleText = computed(() => authStore.profile?.roles?.join(' / ') || '系统管理员')
const accountTypeText = computed(() => {
  const map: Record<string, string> = {
    ADMIN: '平台管理员',
    AGENT: '代理工作台',
    FINANCE: '财务角色',
  }
  const accountType = authStore.profile?.accountType || authStore.loginInfo?.accountType || 'ADMIN'
  return map[accountType] || accountType
})
const groupScopeText = computed(() => {
  const groupCount = authStore.profile?.groupIds?.length ?? 0
  return groupCount > 0 ? `已授权 ${groupCount} 个用户组` : '全局管理范围'
})
const sidebarFocusText = computed(() => currentMenu.value?.description || '围绕收费、计费配置、运营协同与治理能力维持后台日常闭环。')
const workspaceSummary = computed(() => [
  {
    label: '当前模块',
    value: currentGroupLabel.value,
  },
  {
    label: '功能总数',
    value: `${totalMenuCount} 项能力`,
  },
  {
    label: '数据范围',
    value: groupScopeText.value,
  },
])

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}

function handleMenuNavigate() {
  mobileMenuVisible.value = false
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside class="app-sidebar">
      <div class="app-sidebar__inner">
        <div class="app-logo">
          <span class="app-logo__badge">正式运营版</span>
          <span class="app-logo__title">物业管理后台</span>
          <span class="app-logo__subtitle">收费、配置、运营、审计统一入口</span>
        </div>

        <section class="app-sidebar__summary">
          <p class="app-sidebar__summary-title">当前值班关注</p>
          <p class="app-sidebar__summary-text">{{ sidebarFocusText }}</p>
          <div class="app-sidebar__summary-metrics">
            <div class="app-sidebar__summary-metric">
              <span class="app-sidebar__summary-metric-label">功能分组</span>
              <strong class="app-sidebar__summary-metric-value">{{ menuGroupCount }}</strong>
            </div>
            <div class="app-sidebar__summary-metric">
              <span class="app-sidebar__summary-metric-label">当前定位</span>
              <strong class="app-sidebar__summary-metric-value">{{ accountTypeText }}</strong>
            </div>
          </div>
        </section>

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

        <section class="app-sidebar__footer">
          <p class="app-sidebar__footer-label">当前工作区</p>
          <p class="app-sidebar__footer-text">{{ currentGroupLabel }} · {{ pageTitle }}</p>
        </section>
      </div>
    </el-aside>

    <el-container class="app-workspace">
      <el-header class="app-header">
        <div class="app-header__surface">
          <div class="app-header__meta">
            <div class="app-header__topbar">
              <el-button class="app-mobile-menu-btn" circle plain @click="mobileMenuVisible = true">
                <el-icon><Operation /></el-icon>
              </el-button>
              <div class="layout-tag">{{ currentGroupLabel }}</div>
              <div class="layout-tag">正式版工作区</div>
            </div>
            <h1 class="app-header__meta-title">{{ pageTitle }}</h1>
            <p class="app-header__meta-description">{{ pageDescription }}</p>
            <div class="app-header__summary-strip">
              <div v-for="item in workspaceSummary" :key="item.label" class="app-header__summary-item">
                <span class="app-header__summary-label">{{ item.label }}</span>
                <strong class="app-header__summary-value">{{ item.value }}</strong>
              </div>
            </div>
          </div>

          <div class="app-header__actions">
            <div class="app-user-card">
              <span class="app-user-card__label">当前值班账号</span>
              <span class="app-user-card__name">{{ userName }}</span>
              <span class="app-user-card__meta">{{ accountTypeText }} · {{ roleText }}</span>
              <span class="app-user-card__meta">{{ groupScopeText }}</span>
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

    <el-drawer v-model="mobileMenuVisible" title="业务导航" direction="ltr" size="280px" class="app-mobile-drawer">
      <div class="app-mobile-drawer__body">
        <section v-for="group in menuGroups" :key="`mobile-${group.label}`" class="app-nav-group">
          <div class="app-nav-group__label app-nav-group__label--light">{{ group.label }}</div>
          <el-menu :default-active="activeMenu" class="app-mobile-menu" router @select="handleMenuNavigate">
            <el-menu-item v-for="menu in group.items" :key="menu.path" :index="menu.path">
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.label }}</span>
            </el-menu-item>
          </el-menu>
        </section>
      </div>
    </el-drawer>
  </el-container>
</template>
