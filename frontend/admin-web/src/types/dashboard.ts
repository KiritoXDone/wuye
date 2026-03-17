export interface DashboardSummary {
  periodYear: number
  periodMonth: number
  paidCount: number
  totalCount: number
  payRate: number | string
  paidAmount: number | string
  discountAmount: number | string
  unpaidAmount: number | string
}

export interface MonthlyReport extends DashboardSummary {}
