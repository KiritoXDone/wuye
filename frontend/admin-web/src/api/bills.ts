import request from '@/utils/request'
import type { BillDetail, BillPage, BillListQuery } from '@/types/bill'

export function getAdminBills(params: Partial<BillListQuery>) {
  return request.get<never, BillPage>('/admin/bills', { params })
}

export function getBillDetail(billId: number) {
  return request.get<never, BillDetail>(`/admin/bills/${billId}`)
}
