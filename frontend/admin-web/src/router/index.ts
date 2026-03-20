import type { LucideIcon } from 'lucide-react'
import { AlertTriangle, BellRing, Bot, DatabaseBackup, Droplets, FileSearch, FileText, Gift, LayoutDashboard, Network, ReceiptText, ScrollText, ShieldCheck, TicketPercent, Waves } from 'lucide-react'

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
    label: '年度物业费',
    description: '按自然年生成物业费账单，服务周期清晰可追溯。',
    icon: ReceiptText,
  },
  {
    path: '/water-readings',
    label: '月度水费抄表',
    description: '录入抄表后立即出账，维持月度水费闭环。',
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
    label: '组织架构',
    description: '核对组织单元、父级关系与小区映射。',
    icon: Network,
  },
  {
    path: '/agent-groups',
    label: 'Agent 授权',
    description: '维护 Agent 与用户组的最小授权关系。',
    icon: ShieldCheck,
  },
  {
    path: '/audit-logs',
    label: '审计日志',
    description: '按业务、操作人和时间回溯关键后台操作。',
    icon: FileSearch,
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
