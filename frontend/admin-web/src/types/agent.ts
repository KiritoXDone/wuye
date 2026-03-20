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
