import type { BillListItem } from './bill'
import type { RoomItem } from './room'

export interface AgentResidentBillSummary {
  accountId: number
  realName: string
  roomCount: number
  activeRoomCount: number
  issuedBillCount: number
  unpaidBillCount: number
  unpaidAmountTotal: number
  rooms: RoomItem[]
  recentBills: BillListItem[]
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
  result: unknown
}
