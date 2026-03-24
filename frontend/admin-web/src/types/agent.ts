import type { DashboardSummary, MonthlyReport } from '@/types/dashboard'

export interface AgentResidentBillSummary {
  accountId: number
  realName: string
  roomCount: number
  activeRoomCount: number
  issuedBillCount: number
  unpaidBillCount: number
  unpaidAmountTotal: number | string
  rooms: Array<{
    roomId: number
    roomLabel: string
    areaM2: number | string
    bindingStatus: string
    relationType: string
  }>
  recentBills: Array<{
    billId: number
    billNo: string
    roomLabel: string
    feeType: string
    cycleType: string
    period: string
    servicePeriod: string
    amountDue: number | string
    amountPaid: number | string
    status: string
    dueDate: string
  }>
}

export interface AgentAdminBillStats {
  periodYear: number
  periodMonth: number
  summary: DashboardSummary
  propertyYearly: MonthlyReport
  waterMonthly: MonthlyReport
}

export interface AgentCommandPreview {
  commandId: string
  originalPrompt: string
  normalizedPrompt: string
  action: string
  summary: string
  riskLevel: 'L1' | 'L2' | 'L3' | 'L4' | string
  confirmationRequired: boolean
  confirmationToken?: string
  executable: boolean
  message: string
  missingArguments?: string[]
  parsedArguments: Record<string, unknown>
  resolvedContext: Record<string, unknown>
  warnings: string[]
  resultSummary?: string
  resultMarkdown?: string
  result?: unknown
}

export interface AgentConversationMessage {
  id?: string
  role: 'user' | 'assistant' | string
  mode?: 'CHAT' | 'QUERY' | 'ACTION' | 'RESULT' | string
  content: string
  action?: string
  commandId?: string
  riskLevel?: string
  confirmationRequired: boolean
  confirmationToken?: string
  payload?: Record<string, unknown>
  streaming?: boolean
}

export interface AgentConversation {
  sessionId: string
  title?: string
  context: Record<string, unknown>
  messages: AgentConversationMessage[]
  messageCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface AgentConversationListItem {
  sessionId: string
  title?: string
  lastMessagePreview?: string
  messageCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface AgentCommandExecution {
  commandId: string
  status: string
  action: string
  riskLevel: string
  summary: string
  resultSummary?: string
  resultMarkdown?: string
  result: unknown
}
