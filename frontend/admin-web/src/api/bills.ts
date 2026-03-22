import request from '@/utils/request'
import type { BillDetail, BillPage, BillListQuery } from '@/types/bill'

export function getAdminBills(params: Partial<BillListQuery>) {
  return request.get<BillPage>('/admin/bills', { params })
}

export function getBillDetail(billId: number) {
  return request.get<BillDetail>(`/admin/bills/${billId}`)
}

export function deleteBill(billId: number) {
  return request.delete(`/admin/bills/${billId}`)
}
