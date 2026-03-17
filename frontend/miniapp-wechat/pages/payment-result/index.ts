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
        ? `账单与支付单状态已经完成闭环，本次奖励券发放结果：${rewardIssuedCount} 张。`
        : '账单与支付单状态已经完成闭环，可以返回房间或账单详情继续查看。',
      statusEmoji: '✓'
    }
  }

  if (status === 'FAILED' || status === 'CLOSED') {
    return {
      statusTitle: '支付失败',
      statusDescription: '支付单未成功完成，你可以返回账单详情重新发起支付。',
      statusEmoji: '!'
    }
  }

  return {
    statusTitle: '支付处理中',
    statusDescription: '系统正在等待支付结果确认，页面会继续轮询当前支付单状态。',
    statusEmoji: '…'
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
    statusDescription: '系统正在等待支付结果确认。',
    statusEmoji: '…',
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
            ? `本次支付已发放 ${response.rewardIssuedCount || 0} 张奖励券，可在后续账单支付时继续使用。`
            : '本次支付未触发奖励券发放规则。'
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
