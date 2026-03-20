import { getResidentBillSummary } from '../../services/agent'
import { getMyRooms } from '../../services/room'
import { hasAuthSession } from '../../utils/auth'
import { formatMoney } from '../../utils/format'

type RoomViewItem = {
  roomId: number
  roomLabel: string
  areaText: string
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    rooms: [] as RoomViewItem[],
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
      const activeRooms = rooms
        .filter((room) => room.bindingStatus === 'ACTIVE')
        .map((room) => ({
          roomId: room.roomId,
          roomLabel: room.roomLabel,
          areaText: formatMoney(room.areaM2),
        }))
      this.setData({
        rooms: activeRooms,
        unpaidBillCount: summary.unpaidBillCount,
        unpaidAmountText: formatMoney(summary.unpaidAmountTotal)
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '加载首页失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openRoomBills(event: WechatMiniprogram.BaseEvent) {
    const { roomId } = event.currentTarget.dataset
    wx.switchTab({ url: '/pages/bills/index' })
    const app = getApp<{ globalData: { selectedBillRoomId?: number } }>()
    app.globalData.selectedBillRoomId = Number(roomId)
  },

})
