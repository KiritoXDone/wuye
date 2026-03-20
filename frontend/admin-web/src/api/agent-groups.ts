import request from '@/utils/request'
import type { AgentGroup, AgentGroupCreatePayload } from '@/types/agent-group'

export function getAgentGroups() {
  return request.get<AgentGroup[]>('/admin/agent-groups')
}

export function createAgentGroup(payload: AgentGroupCreatePayload) {
  return request.post<AgentGroupCreatePayload, AgentGroup>('/admin/agent-groups', payload)
}
