import { PAYMENT_POLL_INTERVAL, PAYMENT_POLL_LIMIT } from '../../config/env'
import { getPaymentStatus } from '../../services/payment'
import { hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatDateTime } from '../../utils/format'

let pollTimer: number | null = null

function resolveStatusMeta(status: string, rewardIssuedCount?: number) {
  if (status === 'SUCCESS') {
    return {
      statusTitle: '支付成功',
      statusDescription: typeof rewardIssuedCount === 'number'
        ? `已发放 ${rewardIssuedCount} 张奖励券。`
        : '已完成。',
      statusEmoji: ''
    }
  }

  if (status === 'FAILED' || status === 'CLOSED') {
    return {
      statusTitle: '支付失败',
      statusDescription: '请重试。',
      statusEmoji: ''
    }
  }

  return {
    statusTitle: '支付处理中',
    statusDescription: '请稍候。',
    statusEmoji: ''
  }
}

Page({
  data: {
    payOrderNo: '',
    billId: 0,
    loading: true,
    errorMessage: '',
    paymentStatus: 'PAYING',
    paymentStatusLabel: formatBillStatus('PAYING'),
    statusTitle: '支付处理中',
    statusDescription: '请稍候。',
    statusEmoji: '',
    billIdText: '--',
    paidAtText: '--',
    rewardIssuedCount: null as number | null,
    rewardIssuedVisible: false,
    rewardIssuedText: '',
    voucherIssued: false,
    pollAttempt: 0,
    pollLimit: PAYMENT_POLL_LIMIT
  },

  onLoad(query: Record<string, string>) {
    this.setData({
      payOrderNo: query.payOrderNo || '',
      billId: Number(query.billId || 0)
    })
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.refreshStatus(true)
  },

  onUnload() {
    this.clearPolling()
  },

  onHide() {
    this.clearPolling()
  },

  clearPolling() {
    if (pollTimer !== null) {
      clearTimeout(pollTimer)
      pollTimer = null
    }
  },

  async refreshStatus(resetAttempt = false) {
    if (!this.data.payOrderNo) {
      this.setData({ loading: false, errorMessage: '缺少 payOrderNo 参数' })
      return
    }

    this.clearPolling()
    const nextAttempt = resetAttempt ? 1 : this.data.pollAttempt + 1
    this.setData({ loading: true, errorMessage: '', pollAttempt: nextAttempt })

    try {
      const response = await getPaymentStatus(this.data.payOrderNo)
      const hasRewardIssuedCount = typeof response.rewardIssuedCount === 'number'
      const statusMeta = resolveStatusMeta(response.status, response.rewardIssuedCount)
      this.setData({
        billId: response.billId || this.data.billId,
        billIdText: String(response.billId || this.data.billId || '--'),
        paymentStatus: response.status,
        paymentStatusLabel: formatBillStatus(response.status),
        statusTitle: statusMeta.statusTitle,
        statusDescription: statusMeta.statusDescription,
        statusEmoji: statusMeta.statusEmoji,
        paidAtText: formatDateTime(response.paidAt),
        rewardIssuedCount: hasRewardIssuedCount ? response.rewardIssuedCount || 0 : null,
        rewardIssuedVisible: hasRewardIssuedCount,
        voucherIssued: !!response.voucherIssued,
        rewardIssuedText: hasRewardIssuedCount
          ? (response.rewardIssuedCount || 0) > 0
            ? `本次支付已发放 ${response.rewardIssuedCount || 0} 张奖励券，可在后续缴费时使用。`
            : '本次支付未发放奖励券。'
          : ''
      })

      if (response.status === 'PAYING' && nextAttempt < PAYMENT_POLL_LIMIT) {
        pollTimer = setTimeout(() => {
          this.refreshStatus()
        }, PAYMENT_POLL_INTERVAL) as unknown as number
      }
    } catch (error) {
      this.setData({
        errorMessage: error instanceof Error ? error.message : '查询支付状态失败'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  openBillDetail() {
    if (!this.data.billId) {
      wx.navigateBack({ delta: 1 })
      return
    }
    wx.navigateTo({
      url: `/pages/bill-detail/index?billId=${this.data.billId}&payOrderNo=${this.data.payOrderNo}`
    })
  },

  openVoucher() {
    if (!this.data.payOrderNo) {
      return
    }
    wx.navigateTo({
      url: `/pages/voucher/index?payOrderNo=${this.data.payOrderNo}`
    })
  },

  backToRooms() {
    wx.reLaunch({ url: '/pages/rooms/index' })
  }
})
