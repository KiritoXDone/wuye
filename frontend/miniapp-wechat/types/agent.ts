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
