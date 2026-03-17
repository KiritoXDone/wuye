import request from '@/utils/request'
import type { DashboardSummary, MonthlyReport } from '@/types/dashboard'

export function getDashboardSummary(params: { periodYear?: number; periodMonth?: number }) {
  return request.get<never, DashboardSummary>('/admin/dashboard/summary', { params })
}

export function getMonthlyReport(params: { periodYear: number; periodMonth: number }) {
  return request.get<never, MonthlyReport>('/admin/reports/monthly', { params })
}
