import { exchangeRewardVoucher, getMyRewardVouchers } from '../../services/reward'
import type { RewardVoucherItem } from '../../types/reward'
import { hasAuthSession } from '../../utils/auth'
import { formatDateTime, formatBillStatus, formatExchangeStatus } from '../../utils/format'

Page({
  data: {
    loading: true,
    exchangingId: 0,
    errorMessage: '',
    successMessage: '',
    vouchers: [] as Array<RewardVoucherItem & { expiresAtText: string; issuedAtText: string; canExchange: boolean; statusText: string; exchangeStatusText: string }>,
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.loadData()
  },

  async loadData() {
    this.setData({ loading: true, errorMessage: '', successMessage: '' })
    try {
      const vouchers = await getMyRewardVouchers()
      this.setData({
        vouchers: vouchers.map((item) => ({
          ...item,
          expiresAtText: formatDateTime(item.expiresAt),
          issuedAtText: formatDateTime(item.issuedAt),
          canExchange: item.status === 'NEW' && !item.exchangeStatus,
          statusText: formatBillStatus(item.status),
          exchangeStatusText: formatExchangeStatus(item.exchangeStatus),
        }))
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '奖励券加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleExchange(event: WechatMiniprogram.BaseEvent) {
    const couponInstanceId = Number(event.currentTarget.dataset.couponInstanceId || 0)
    if (!couponInstanceId) {
      return
    }
    this.setData({ exchangingId: couponInstanceId, errorMessage: '', successMessage: '' })
    try {
      const result = await exchangeRewardVoucher(couponInstanceId)
      this.setData({ successMessage: `${result.goodsName || result.name} 已提交线下自提，请按说明前往领取。` })
      await this.loadData()
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '兑换失败' })
    } finally {
      this.setData({ exchangingId: 0 })
    }
  }
})
