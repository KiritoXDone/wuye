import { getBillDetail } from '../../services/bill'
import { createPayment } from '../../services/payment'
import type { BillDetail, BillLine } from '../../types/bill'
import { hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatDate, formatFeeType, formatMoney, formatPeriod } from '../../utils/format'
import { buildPaymentIdempotencyKey, invokeWechatPayment } from '../../utils/payment'

type BillLineView = BillLine & {
  unitPriceText: string
  quantityText: string
  lineAmountText: string
  extSummary: string
  showDivider: boolean
}

type CouponView = {
  couponInstanceId: number
  templateCode: string
  discountAmountText: string
  expiresAtText: string
}

type BillDetailView = Omit<BillDetail, 'billLines' | 'availableCoupons'> & {
  feeTypeLabel: string
  statusLabel: string
  periodText: string
  dueDateText: string
  amountDueText: string
  billLines: BillLineView[]
  availableCoupons: CouponView[]
}

function buildExtSummary(ext?: Record<string, unknown>): string {
  if (!ext) {
    return ''
  }

  const parts: string[] = []
  if (typeof ext.areaM2 === 'number') {
    parts.push(`面积 ${formatMoney(ext.areaM2)}㎡`)
  }
  if (typeof ext.usage === 'number') {
    parts.push(`用量 ${formatMoney(ext.usage)}`)
  }
  if (typeof ext.formula === 'string') {
    parts.push(`公式 ${ext.formula}`)
  }
  return parts.join(' · ')
}

function normalizeBillDetail(detail: BillDetail): BillDetailView {
  return {
    ...detail,
    feeTypeLabel: formatFeeType(detail.feeType),
    statusLabel: formatBillStatus(detail.status),
    periodText: formatPeriod(detail.periodYear, detail.periodMonth),
    dueDateText: formatDate(detail.dueDate),
    amountDueText: formatMoney(detail.amountDue),
    billLines: detail.billLines.map((line, index) => ({
      ...line,
      unitPriceText: formatMoney(line.unitPrice),
      quantityText: formatMoney(line.quantity),
      lineAmountText: formatMoney(line.lineAmount),
      extSummary: buildExtSummary(line.ext),
      showDivider: index !== detail.billLines.length - 1
    })),
    availableCoupons: detail.availableCoupons.map((coupon) => ({
      couponInstanceId: coupon.couponInstanceId,
      templateCode: coupon.templateCode,
      discountAmountText: formatMoney(coupon.discountAmount),
      expiresAtText: formatDate(coupon.expiresAt)
    }))
  }
}

Page({
  data: {
    billId: 0,
    loading: true,
    paying: false,
    errorMessage: '',
    billDetail: null as BillDetailView | null,
    latestPayOrderNo: ''
  },

  onLoad(query: Record<string, string>) {
    this.setData({ billId: Number(query.billId || 0) })
  },

  onShow() {
    if (!hasAuthSession()) {
      wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.fetchBillDetail()
  },

  onPullDownRefresh() {
    this.fetchBillDetail().finally(() => wx.stopPullDownRefresh())
  },

  async fetchBillDetail() {
    if (!this.data.billId) {
      this.setData({ loading: false, errorMessage: '缺少 billId 参数' })
      return
    }

    this.setData({ loading: true, errorMessage: '' })

    try {
      const detail = await getBillDetail(this.data.billId)
      this.setData({ billDetail: normalizeBillDetail(detail) })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '账单详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async handlePay() {
    if (!this.data.billDetail || this.data.paying) {
      return
    }

    this.setData({ paying: true })

    try {
      const payment = await createPayment({
        billId: this.data.billDetail.billId,
        channel: 'WECHAT',
        idempotencyKey: buildPaymentIdempotencyKey(this.data.billDetail.billId)
      })

      this.setData({ latestPayOrderNo: payment.payOrderNo })

      try {
        await invokeWechatPayment(payment)
      } catch (paymentError) {
        wx.showToast({
          title: paymentError && typeof paymentError === 'object' && 'errMsg' in paymentError
            ? String(paymentError.errMsg)
            : '未完成微信收银台支付，已进入结果查询页',
          icon: 'none',
          duration: 2200
        })
      }

      wx.navigateTo({
        url: `/pages/payment-result/index?payOrderNo=${payment.payOrderNo}&billId=${this.data.billDetail.billId}`
      })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '创建支付单失败',
        icon: 'none',
        duration: 2200
      })
    } finally {
      this.setData({ paying: false })
    }
  },

  openPaymentResultFromPaid() {
    if (!this.data.latestPayOrderNo) {
      wx.showToast({
        title: '当前没有最近支付单号，请重新发起一次支付查看结果',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: `/pages/payment-result/index?payOrderNo=${this.data.latestPayOrderNo}&billId=${this.data.billId}`
    })
  }
})
