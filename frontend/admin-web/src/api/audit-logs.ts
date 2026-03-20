import request from '@/utils/request'
import type { AuditLogListQuery, AuditLogPage } from '@/types/audit-log'

export function getAuditLogs(params: Partial<AuditLogListQuery>) {
  return request.get<AuditLogPage>('/admin/audit-logs', { params })
}
