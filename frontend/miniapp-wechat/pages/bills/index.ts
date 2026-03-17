import { getResidentBills, getRoomBills } from '../../services/bill'
import type { BillListItem, BillStatusFilter } from '../../types/bill'
import { hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatDate, formatFeeType, formatMoney } from '../../utils/format'

type BillViewItem = BillListItem & {
  feeTypeLabel: string
  statusLabel: string
  amountDueText: string
  dueDateText: string
}

function normalizeBills(bills: BillListItem[]): BillViewItem[] {
  return bills.map((bill) => ({
    ...bill,
    feeTypeLabel: formatFeeType(bill.feeType),
    statusLabel: formatBillStatus(bill.status),
    amountDueText: formatMoney(bill.amountDue),
    dueDateText: formatDate(bill.dueDate)
  }))
}

Page({
  data: {
    roomId: 0,
    roomLabel: '',
    roomMode: false,
    pageTitle: '全部账单',
    pageSubtitle: '这里展示当前账号在 Resident 范围内可查看的账单。',
    loading: true,
    errorMessage: '',
    loadingHint: '正在调用 /api/v1/me/bills',
    emptyHint: '暂时没有查询到住户账单。',
    statusFilter: 'ALL' as BillStatusFilter,
    statusOptions: [
      { value: 'ALL', label: '全部' },
      { value: 'ISSUED', label: '待缴' },
      { value: 'PAID', label: '已缴' }
    ],
    allBills: [] as BillViewItem[],
    displayBills: [] as BillViewItem[]
  },

  onLoad(query: Record<string, string>) {
    const roomId = Number(query.roomId || 0)
    const roomLabel = query.roomLabel ? decodeURIComponent(query.roomLabel) : ''
    const roomMode = Boolean(roomId)
    this.setData({
      roomId,
      roomLabel,
      roomMode,
      pageTitle: roomMode ? `${roomLabel || '房间'}账单` : '全部账单',
      pageSubtitle: roomMode
        ? '当前优先使用 /api/v1/me/rooms/{roomId}/bills，保持房间作为账单主体。'
        : '当前使用 /api/v1/me/bills 查看本人全部账单。',
      loadingHint: roomMode ? `正在调用 /api/v1/me/rooms/${roomId}/bills` : '正在调用 /api/v1/me/bills',
      emptyHint: roomMode ? '该房间暂时没有匹配的账单。' : '当前账号暂时没有账单。'
    })
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.fetchBills()
  },

  onPullDownRefresh() {
    this.fetchBills().finally(() => wx.stopPullDownRefresh())
  },

  async fetchBills() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      if (this.data.roomMode) {
        const response = await getRoomBills(this.data.roomId)
        const normalized = normalizeBills(response.list)
        this.setData({ allBills: normalized })
        this.applyLocalFilter(this.data.statusFilter, normalized)
      } else {
        const response = await getResidentBills(this.data.statusFilter)
        const normalized = normalizeBills(response.list)
        this.setData({ allBills: normalized, displayBills: normalized })
      }
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '账单加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  handleStatusChange(event: WechatMiniprogram.BaseEvent) {
    const nextStatus = event.currentTarget.dataset.status as BillStatusFilter
    if (nextStatus === this.data.statusFilter) {
      return
    }

    this.setData({ statusFilter: nextStatus })

    if (this.data.roomMode) {
      this.applyLocalFilter(nextStatus, this.data.allBills)
      return
    }

    this.fetchBills()
  },

  applyLocalFilter(status: BillStatusFilter, sourceBills?: BillViewItem[]) {
    const origin = sourceBills || this.data.allBills
    const displayBills = status === 'ALL' ? origin : origin.filter((bill) => bill.status === status)
    this.setData({ displayBills })
  },

  openBillDetail(event: WechatMiniprogram.BaseEvent) {
    const { billId } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/bill-detail/index?billId=${billId}` })
  }
})
