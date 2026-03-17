import { DEFAULT_PAGE_SIZE } from '../config/env'
import type { BillDetail, BillListItem, BillStatusFilter } from '../types/bill'
import type { PageResponse } from '../types/api'
import { request } from '../utils/request'

export function getResidentBills(status: BillStatusFilter) {
  const query: WechatMiniprogram.IAnyObject = {
    pageNo: 1,
    pageSize: DEFAULT_PAGE_SIZE
  }

  if (status !== 'ALL') {
    query.status = status
  }

  return request<PageResponse<BillListItem>>({
    url: '/api/v1/me/bills',
    data: query
  })
}

export function getRoomBills(roomId: number) {
  return request<PageResponse<BillListItem>>({
    url: `/api/v1/me/rooms/${roomId}/bills`
  })
}

export function getBillDetail(billId: number) {
  return request<BillDetail>({
    url: `/api/v1/bills/${billId}`
  })
}
