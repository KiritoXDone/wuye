import request from '@/utils/request'
import type { AgentGroup, AgentGroupCreatePayload } from '@/types/agent-group'

export function getAgentGroups() {
  return request.get<never, AgentGroup[]>('/admin/agent-groups')
}

export function createAgentGroup(payload: AgentGroupCreatePayload) {
  return request.post<never, AgentGroup>('/admin/agent-groups', payload)
}
