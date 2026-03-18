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
    pageSubtitle: '在这里查看当前账号下的全部账单与缴费状态。',
    loading: true,
    errorMessage: '',
    loadingHint: '正在加载账单，请稍候。',
    emptyHint: '暂时没有查询到可显示的账单。',
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
        ? '查看当前房间的账单、到期时间与支付状态。'
        : '优先关注待缴账单，完成后可继续查看支付结果。',
      loadingHint: roomMode ? '正在加载当前房间账单，请稍候。' : '正在加载全部账单，请稍候。',
      emptyHint: roomMode ? '该房间暂时还没有可显示的账单。' : '当前账号暂时没有账单。'
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
