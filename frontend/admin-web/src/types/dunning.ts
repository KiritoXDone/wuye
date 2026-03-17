export interface DunningTriggerPayload {
  triggerDate?: string
}

export interface DunningTask {
  id: number
  taskNo: string
  billId: number
  groupId?: number
  orgUnitId?: number
  tenantCode?: string
  triggerType: string
  triggerDate: string
  status: string
  remark?: string
  executedAt?: string
}

export interface DunningLog {
  id: number
  taskId: number
  billId: number
  sendChannel: string
  status: string
  content?: string
  sentAt?: string
}
