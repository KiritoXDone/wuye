import type { PageResponse } from '@/types/api'

export interface AuditLogListQuery {
  bizType?: string
  bizId?: string
  operatorId?: number
  createdAtStart?: string
  createdAtEnd?: string
  pageNo: number
  pageSize: number
}

export interface AuditLogItem {
  id: number
  bizType: string
  bizId: string
  action: string
  operatorId?: number
  ip?: string
  userAgent?: string
  detailJson?: string
  createdAt: string
}

export type AuditLogPage = PageResponse<AuditLogItem>
