export interface AgentGroup {
  groupId: number
  groupCode: string
  groupName: string
  permission: string
}

export interface AgentGroupCreatePayload {
  agentCode: string
  groupCode: string
  permission: string
  status: number
}
