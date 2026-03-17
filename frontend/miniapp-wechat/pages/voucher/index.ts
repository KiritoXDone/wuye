import { getPaymentVoucher } from '../../services/voucher'
import type { PaymentVoucher } from '../../types/voucher'
import { hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatDateTime, formatFeeType, formatMoney } from '../../utils/format'

type VoucherView = PaymentVoucher & {
  amountText: string
  issuedAtText: string
  feeTypeLabel: string
  voucherStatusLabel: string
  billNo: string
  voucherAmountText: string
}

function normalizeVoucher(voucher: PaymentVoucher): VoucherView {
  const content = voucher.content || {}
  const feeType = typeof content.feeType === 'string' ? content.feeType : ''
  const billNo = typeof content.billNo === 'string' ? content.billNo : '--'
  const voucherAmount = content.amount as number | string | undefined

  return {
    ...voucher,
    amountText: formatMoney(voucher.amount),
    issuedAtText: formatDateTime(voucher.issuedAt),
    feeTypeLabel: formatFeeType(feeType),
    voucherStatusLabel: voucher.status === 'ISSUED' ? '已签发' : formatBillStatus(voucher.status),
    billNo,
    voucherAmountText: formatMoney(voucherAmount)
  }
}

Page({
  data: {
    payOrderNo: '',
    loading: true,
    errorMessage: '',
    voucher: null as VoucherView | null
  },

  onLoad(query: Record<string, string>) {
    this.setData({
      payOrderNo: query.payOrderNo || ''
    })
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.fetchVoucher()
  },

  onPullDownRefresh() {
    this.fetchVoucher().finally(() => wx.stopPullDownRefresh())
  },

  async fetchVoucher() {
    if (!this.data.payOrderNo) {
      this.setData({ loading: false, errorMessage: '缺少 payOrderNo 参数' })
      return
    }

    this.setData({ loading: true, errorMessage: '' })
    try {
      const voucher = await getPaymentVoucher(this.data.payOrderNo)
      this.setData({ voucher: normalizeVoucher(voucher) })
    } catch (error) {
      this.setData({
        errorMessage: error instanceof Error ? error.message : '电子凭证加载失败'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  openBillDetail() {
    const billId = this.data.voucher?.billId || 0
    if (!billId) {
      wx.navigateBack({ delta: 1 })
      return
    }
    wx.navigateTo({
      url: `/pages/bill-detail/index?billId=${billId}&payOrderNo=${this.data.payOrderNo}`
    })
  }
})
