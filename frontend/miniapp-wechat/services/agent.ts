import type { AgentResidentBillSummary } from '../types/agent'
import { request } from '../utils/request'

export function getResidentBillSummary() {
  return request<AgentResidentBillSummary>({
    url: '/api/v1/ai/agent/me/bill-summary'
  })
}
