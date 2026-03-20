import type { PageResponse } from '@/types/api'

export interface BillListQuery {
  periodYear?: number
  periodMonth?: number
  feeType?: string
  status?: string
  pageNo: number
  pageSize: number
}

export interface BillListItem {
  billId: number
  billNo: string
  roomId: number
  roomLabel: string
  roomTypeName?: string | null
  feeType: string
  cycleType?: string
  period: string
  servicePeriod?: string
  amountDue: number | string
  amountPaid: number | string
  status: string
  dueDate: string
}

export interface BillLine {
  lineNo: number
  lineType: string
  itemName: string
  unitPrice: number | string
  quantity: number | string
  lineAmount: number | string
  ext?: Record<string, unknown>
}

export interface BillDetail {
  billId: number
  billNo: string
  roomId: number
  roomLabel: string
  roomTypeName?: string | null
  feeType: string
  cycleType?: string
  periodYear: number
  periodMonth?: number | null
  servicePeriodStart?: string | null
  servicePeriodEnd?: string | null
  amountDue: number | string
  amountPaid: number | string
  status: string
  dueDate: string
  billLines: BillLine[]
  availableCoupons: Array<Record<string, unknown>>
}

export type BillPage = PageResponse<BillListItem>
