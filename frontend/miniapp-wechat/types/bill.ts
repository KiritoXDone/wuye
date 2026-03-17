export interface BillListItem {
  billId: number
  billNo: string
  roomId: number
  roomLabel: string
  feeType: string
  period: string
  amountDue: number
  amountPaid: number
  status: string
  dueDate: string
}

export interface BillLine {
  lineNo: number
  lineType: string
  itemName: string
  unitPrice: number
  quantity: number
  lineAmount: number
  ext?: Record<string, unknown>
}

export interface AvailableCoupon {
  couponInstanceId: number
  templateCode: string
  name?: string
  discountAmount: number
  expiresAt: string
}

export interface BillDetail {
  billId: number
  billNo: string
  roomId: number
  roomLabel: string
  feeType: string
  periodYear: number
  periodMonth: number
  amountDue: number
  amountPaid: number
  status: string
  dueDate: string
  billLines: BillLine[]
  availableCoupons: AvailableCoupon[]
}

export type BillStatusFilter = 'ALL' | 'ISSUED' | 'PAID'
