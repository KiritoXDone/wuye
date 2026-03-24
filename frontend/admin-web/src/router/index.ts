import type { LucideIcon } from 'lucide-react'
import {
  BellRing,
  Bot,
  Building2,
  DatabaseBackup,
  Droplets,
  FileSearch,
  FileText,
  LayoutDashboard,
  ReceiptText,
  ScrollText,
  TicketPercent,
  UserCog,
  Waves,
} from 'lucide-react'

export interface AppRouteMeta {
  path: string
  label: string
  description: string
  icon: LucideIcon
  showInNav?: boolean
}

export const coreRoutes: AppRouteMeta[] = [
  {
    path: '/dashboard',
    label: '运营总览',
    description: '查看按房间口径统计的应收、实收、欠费与缴费率。',
    icon: LayoutDashboard,
  },
  {
    path: '/household-payments',
    label: '缴费统计',
    description: '按户查看物业费、水费是否已缴，并支持后台手动确认线下已缴。',
    icon: ReceiptText,
  },
  {
    path: '/billing-generate',
    label: '开账中心',
    description: '统一处理物业费开单与水费补齐出账。',
    icon: ReceiptText,
  },
  {
    path: '/water-readings',
    label: '水费抄表',
    description: '录入抄表后立即生成当月水费账单。',
    icon: Droplets,
  },
  {
    path: '/bills',
    label: '账单管理',
    description: '统一复核年度物业费与月度水费账单明细。',
    icon: FileText,
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
    description: '按小区维护房间、户型与组织归属。',
    icon: Building2,
  },
  {
    path: '/users',
    label: '用户管理',
    description: '维护后台账号、启停状态与密码重置。',
    icon: UserCog,
  },
  {
    path: '/audit-logs',
    label: '审计日志',
    description: '按业务、操作人与时间回溯关键后台操作。',
    icon: FileSearch,
  },
  {
    path: '/built-in-agent',
    label: '内置 Agent',
    description: '用于账单查询与统计的系统内置智能能力。',
    icon: Bot,
    showInNav: false,
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
    path: '/coupons',
    label: '优惠券',
    description: '统一维护优惠券、发放规则和发放结果。',
    icon: TicketPercent,
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
