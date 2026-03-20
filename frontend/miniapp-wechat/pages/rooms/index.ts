import { getResidentBillSummary } from '../../services/agent'
import { getMyRooms } from '../../services/room'
import { clearAuthSession, hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatMoney, formatRelationType } from '../../utils/format'

type RoomViewItem = {
  roomId: number
  roomLabel: string
  bindingStatus: string
  bindingStatusLabel: string
  areaText: string
  relationTypeLabel: string
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    rooms: [] as RoomViewItem[],
    activeRoomCount: 0,
    unpaidBillCount: 0,
    unpaidAmountText: '0.00'
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.fetchRooms()
  },

  onPullDownRefresh() {
    this.fetchRooms().finally(() => wx.stopPullDownRefresh())
  },

  async fetchRooms() {
    this.setData({ loading: true, errorMessage: '' })
    try {
      const [rooms, summary] = await Promise.all([getMyRooms(), getResidentBillSummary()])
      const normalizedRooms = rooms.map((room) => ({
        roomId: room.roomId,
        roomLabel: room.roomLabel,
        bindingStatus: room.bindingStatus,
        bindingStatusLabel: formatBillStatus(room.bindingStatus),
        areaText: formatMoney(room.areaM2),
        relationTypeLabel: formatRelationType(room.relationType)
      }))
      this.setData({
        rooms: normalizedRooms,
        activeRoomCount: normalizedRooms.filter((item) => item.bindingStatus === 'ACTIVE').length,
        unpaidBillCount: summary.unpaidBillCount,
        unpaidAmountText: formatMoney(summary.unpaidAmountTotal)
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '加载房间失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openRoomBills(event: WechatMiniprogram.BaseEvent) {
    const { roomId, roomLabel } = event.currentTarget.dataset
    wx.navigateTo({ url: `/pages/bills/index?roomId=${roomId}&roomLabel=${encodeURIComponent(roomLabel)}` })
  },

  openResidentBills() {
    wx.navigateTo({ url: '/pages/bills/index' })
  },

  handleLogout() {
    clearAuthSession()
    wx.reLaunch({ url: '/pages/login/index' })
  }
})
