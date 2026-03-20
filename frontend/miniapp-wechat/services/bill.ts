import { DEFAULT_PAGE_SIZE } from '../config/env'
import type { BillDetail, BillListItem, BillStatusFilter, ResidentBillQuery } from '../types/bill'
import type { PageResponse } from '../types/api'
import { request } from '../utils/request'

export function getResidentBills(queryInput: ResidentBillQuery) {
  const query: WechatMiniprogram.IAnyObject = {
    pageNo: 1,
    pageSize: DEFAULT_PAGE_SIZE
  }

  if (queryInput.status !== 'ALL') {
    query.status = queryInput.status
  }
  if (queryInput.roomId) {
    query.roomId = queryInput.roomId
  }

  return request<PageResponse<BillListItem>>({
    url: '/api/v1/me/bills',
    data: query
  })
}

export function getBillDetail(billId: number) {
  return request<BillDetail>({
    url: `/api/v1/bills/${billId}`
  })
}
