import request from '@/utils/request'
import type { AgentAdminBillStats } from '@/types/agent'

export function getAdminAgentBillStats(params: { periodYear?: number; periodMonth?: number }) {
  return request.get<AgentAdminBillStats>('/ai/agent/admin/bill-stats', { params })
}
