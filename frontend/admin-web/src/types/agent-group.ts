export interface AgentGroup {
  groupId: number
  groupCode: string
  groupName: string
  permission: string
  orgUnitId?: number
  orgUnitName?: string
  tenantCode?: string
}

export interface AgentGroupCreatePayload {
  agentCode: string
  groupCode: string
  permission: string
  status: number
}
