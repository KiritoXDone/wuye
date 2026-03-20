import type { LucideIcon } from 'lucide-react'
import { AlertTriangle, BellRing, Bot, Building2, DatabaseBackup, Droplets, FileSearch, FileText, Gift, LayoutDashboard, ReceiptText, ScrollText, TicketPercent, UserCog, Waves } from 'lucide-react'

export interface AppRouteMeta {
  path: string
  label: string
  description: string
  icon: LucideIcon
}

export const coreRoutes: AppRouteMeta[] = [
  {
    path: '/dashboard',
    label: '运营总览',
    description: '查看房间口径下的实收、欠费与收缴率。',
    icon: LayoutDashboard,
  },
  {
    path: '/billing-generate',
    label: '开账中心',
    description: '统一处理物业费开单与水费补出账。',
    icon: ReceiptText,
  },
  {
    path: '/water-readings',
    label: '水费抄表',
    description: '录入抄表后立即出账。',
    icon: Droplets,
  },
  {
    path: '/bills',
    label: '账单管理',
    description: '统一复核年度物业费与月度水费账单。',
    icon: FileText,
  },
  {
    path: '/water-alerts',
    label: '水量预警',
    description: '按月查看异常用量预警，辅助复核抄表与出账。',
    icon: AlertTriangle,
  },
  {
    path: '/import-export',
    label: '导入导出',
    description: '围绕批次、错误行与异步任务处理账单导入导出。',
    icon: DatabaseBackup,
  },
  {
    path: '/fee-rules',
    label: '费用规则',
    description: '维护年度物业费与月度水费计费基线。',
    icon: Waves,
  },
  {
    path: '/org-units',
    label: '房间管理',
    description: '按小区维护户型并给房间绑定户型与面积。',
    icon: Building2,
  },
  {
    path: '/users',
    label: '用户管理',
    description: '维护后台管理员账户的创建、启停与密码重置。',
    icon: UserCog,
  },
  {
    path: '/audit-logs',
    label: '审计日志',
    description: '按业务、操作人和时间回溯关键后台操作。',
    icon: FileSearch,
  },
  {
    path: '/built-in-agent',
    label: '内置 Agent',
    description: '用于账单查询与统计的系统内置智能能力。',
    icon: Bot,
  },
  {
    path: '/ai-runtime-config',
    label: 'AI 运行配置',
    description: '统一维护 AI 运行时配置与密钥更新语义。',
    icon: Bot,
  },
  {
    path: '/dunning',
    label: '催缴任务',
    description: '支持手动触发催缴并查看发送日志。',
    icon: BellRing,
  },
  {
    path: '/coupon-templates',
    label: '券模板',
    description: '维护支付前抵扣券与支付后奖励券模板。',
    icon: TicketPercent,
  },
  {
    path: '/coupon-rules',
    label: '发券规则',
    description: '配置支付成功后的奖励券发放规则。',
    icon: Gift,
  },
  {
    path: '/invoice-applications',
    label: '发票申请',
    description: '保留发票申请最小处理入口与结果回看。',
    icon: ScrollText,
  },
]

export function getRouteMeta(pathname: string) {
  return coreRoutes.find((route) => pathname.startsWith(route.path)) || coreRoutes[0]
}
