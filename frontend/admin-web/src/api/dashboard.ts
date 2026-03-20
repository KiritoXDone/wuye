import request from '@/utils/request'
import type { DashboardSummary, MonthlyReport } from '@/types/dashboard'

export function getDashboardSummary(params: { periodYear?: number; periodMonth?: number }) {
  return request.get<DashboardSummary>('/admin/dashboard/summary', { params })
}

export function getPropertyYearlyReport(params: { periodYear: number }) {
  return request.get<MonthlyReport>('/admin/reports/property-yearly', { params })
}

export function getWaterMonthlyReport(params: { periodYear: number; periodMonth: number }) {
  return request.get<MonthlyReport>('/admin/reports/water-monthly', { params })
}

export function getMonthlyReport(params: { periodYear: number; periodMonth: number }) {
  return request.get<MonthlyReport>('/admin/reports/monthly', { params })
}
