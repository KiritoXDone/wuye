import request from '@/utils/request'
import type {
  BillDetail,
  BillPage,
  BillListQuery,
  HouseholdPaymentOverviewPage,
  HouseholdPaymentOverviewQuery,
  MarkBillPaidPayload,
} from '@/types/bill'

export function getAdminBills(params: Partial<BillListQuery>) {
  return request.get<BillPage>('/admin/bills', { params })
}

export function getBillDetail(billId: number) {
  return request.get<BillDetail>(`/admin/bills/${billId}`)
}

export function deleteBill(billId: number) {
  return request.delete(`/admin/bills/${billId}`)
}

export function getHouseholdPaymentOverview(params: Partial<HouseholdPaymentOverviewQuery>) {
  return request.get<HouseholdPaymentOverviewPage>('/admin/billing/households', { params })
}

export function markBillPaid(billId: number, payload: MarkBillPaidPayload) {
  return request.post<MarkBillPaidPayload, void>(`/admin/bills/${billId}/mark-paid`, payload)
}
