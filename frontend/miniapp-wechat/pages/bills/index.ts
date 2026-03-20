import { getResidentBills } from '../../services/bill'
import { getMyRooms } from '../../services/room'
import type { BillListItem, BillStatusFilter } from '../../types/bill'
import type { RoomItem } from '../../types/room'
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
    loading: true,
    errorMessage: '',
    statusFilter: 'ALL' as BillStatusFilter,
    statusOptions: [
      { value: 'ALL', label: '全部' },
      { value: 'ISSUED', label: '待缴' },
      { value: 'PAID', label: '已缴' }
    ],
    roomOptions: [{ roomId: 0, roomLabel: '全部房间' }] as Array<{ roomId: number; roomLabel: string }>,
    selectedRoomIndex: 0,
    selectedRoomId: 0,
    displayBills: [] as BillViewItem[]
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    const app = getApp<{ globalData: { selectedBillRoomId?: number } }>()
    const nextRoomId = Number(app.globalData.selectedBillRoomId || 0)
    app.globalData.selectedBillRoomId = 0
    this.fetchBillFilters(nextRoomId)
  },

  onPullDownRefresh() {
    this.fetchBills().finally(() => wx.stopPullDownRefresh())
  },

  async fetchBillFilters(preselectedRoomId = 0) {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const rooms = await getMyRooms()
      const activeRooms = rooms.filter((item: RoomItem) => item.bindingStatus === 'ACTIVE')
      const roomOptions = [{ roomId: 0, roomLabel: '全部房间' }, ...activeRooms.map((room) => ({ roomId: room.roomId, roomLabel: room.roomLabel }))]
      const selectedRoomIndex = Math.max(roomOptions.findIndex((item) => item.roomId === preselectedRoomId), 0)
      this.setData({
        roomOptions,
        selectedRoomIndex,
        selectedRoomId: roomOptions[selectedRoomIndex]?.roomId || 0,
      })
      await this.fetchBills()
    } catch (error) {
      this.setData({ loading: false, errorMessage: error instanceof Error ? error.message : '账单筛选加载失败' })
    }
  },

  async fetchBills() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const response = await getResidentBills({
        status: this.data.statusFilter,
        roomId: this.data.selectedRoomId || undefined,
      })
      this.setData({ displayBills: normalizeBills(response.list) })
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
    this.fetchBills()
  },

  handleRoomChange(event: WechatMiniprogram.CustomEvent) {
    const selectedRoomIndex = Number(event.detail.value || 0)
    const selectedRoomId = this.data.roomOptions[selectedRoomIndex]?.roomId || 0
    this.setData({ selectedRoomIndex, selectedRoomId })
    this.fetchBills()
  },

  openBillDetail(event: WechatMiniprogram.BaseEvent) {
    const { billId } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/bill-detail/index?billId=${billId}` })
  }
})
