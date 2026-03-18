<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, DataBoard, Document, Files, Notebook, OfficeBuilding, Odometer, SetUp } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menus = [
  { path: '/dashboard', label: '仪表盘', icon: Odometer },
  { path: '/bills', label: '账单管理', icon: Document },
  { path: '/fee-rules', label: '费用规则', icon: Notebook },
  { path: '/water-readings', label: '水表与抄表', icon: SetUp },
  { path: '/billing-generate', label: '账单生成', icon: DataBoard },
  { path: '/import-export', label: '导入导出', icon: Document },
  { path: '/coupon-templates', label: '券模板', icon: Notebook },
  { path: '/coupon-rules', label: '发券规则', icon: DataBoard },
  { path: '/agent-groups', label: 'Agent 授权', icon: SetUp },
  { path: '/org-units', label: '组织架构', icon: OfficeBuilding },
  { path: '/water-alerts', label: '水量预警', icon: Bell },
  { path: '/dunning', label: '催缴任务', icon: DataBoard },
  { path: '/audit-logs', label: '审计日志', icon: Files },
  { path: '/invoice-applications', label: '发票申请', icon: Files },
  { path: '/ai-runtime-config', label: 'Agent 运行配置', icon: SetUp },
]

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) || '物业管理后台')
const userName = computed(() => authStore.profile?.realName || '管理员')

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="220" class="app-sidebar">
      <div class="app-logo">
        <span class="app-logo__title">物业管理后台</span>
        <span class="app-logo__subtitle">MVP 管理端闭环</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.82)"
        active-text-color="#ffffff"
        router
        borderless
      >
        <el-menu-item v-for="menu in menus" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div>
          <div class="layout-tag">{{ pageTitle }}</div>
        </div>
        <div class="inline-actions">
          <el-text>{{ userName }}</el-text>
          <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
