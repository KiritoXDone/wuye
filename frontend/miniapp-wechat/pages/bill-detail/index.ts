import { getBillDetail } from '../../services/bill'
import { validateCoupon } from '../../services/coupon'
import { createPayment } from '../../services/payment'
import type { BillDetail, BillLine } from '../../types/bill'
import type { PaymentCreatePayload } from '../../types/payment'
import { hasAuthSession } from '../../utils/auth'
import { formatBillStatus, formatDate, formatFeeType, formatMoney, formatPeriod } from '../../utils/format'
import { buildPaymentIdempotencyKey, invokePaymentByChannel } from '../../utils/payment'

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
  name: string
  discountAmount: number
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
      name: coupon.name || coupon.templateCode,
      discountAmount: Number(coupon.discountAmount),
      discountAmountText: formatMoney(coupon.discountAmount),
      expiresAtText: formatDate(coupon.expiresAt)
    }))
  }
}

function buildDefaultAmountState(detail: BillDetailView | null) {
  const amountDue = Number(detail?.amountDue ?? 0)
  return {
    selectedCouponInstanceId: 0,
    selectedCouponName: '',
    couponHint: detail?.availableCoupons.length
      ? '选择优惠券后会刷新金额。'
      : '暂无可用券。',
    couponValidating: false,
    discountAmountText: formatMoney(0),
    payAmountText: formatMoney(amountDue)
  }
}

Page({
  data: {
    billId: 0,
    loading: true,
    paying: false,
    errorMessage: '',
    billDetail: null as BillDetailView | null,
    latestPayOrderNo: '',
    entryPayOrderNo: '',
    annualPayment: false,
    selectedCouponInstanceId: 0,
    selectedCouponName: '',
    selectedChannel: 'WECHAT' as PaymentCreatePayload['channel'],
    couponHint: '',
    couponValidating: false,
    discountAmountText: formatMoney(0),
    payAmountText: formatMoney(0)
  },

  onLoad(query: Record<string, string>) {
    this.setData({
      billId: Number(query.billId || 0),
      entryPayOrderNo: query.payOrderNo || ''
    })
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
      const billDetail = normalizeBillDetail(detail)
      this.setData({
        billDetail,
        ...buildDefaultAmountState(billDetail)
      })
    } catch (error) {
      this.setData({ errorMessage: error instanceof Error ? error.message : '账单详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  clearCouponSelection() {
    if (!this.data.billDetail) {
      return
    }
    this.setData(buildDefaultAmountState(this.data.billDetail))
  },

  handleAnnualPaymentChange(event: WechatMiniprogram.SwitchChange) {
    if (!this.data.billDetail || this.data.billDetail.feeType !== 'PROPERTY' || this.data.billDetail.status === 'PAID') {
      return
    }
    const annualPayment = Boolean(event.detail.value)
    this.setData({
      annualPayment,
      selectedCouponInstanceId: 0,
      selectedCouponName: '',
      discountAmountText: formatMoney(0),
      payAmountText: annualPayment ? '按年汇总后生成' : formatMoney(this.data.billDetail.amountDue),
      couponHint: annualPayment
        ? '按年缴纳不支持优惠券。'
        : (this.data.billDetail.availableCoupons.length ? '选择优惠券后会刷新金额。' : '暂无可用券。')
    })
  },

  async handleCouponSelect(event: WechatMiniprogram.TouchEvent) {
    if (!this.data.billDetail || this.data.couponValidating || this.data.billDetail.status === 'PAID' || this.data.annualPayment) {
      return
    }

    const couponInstanceId = Number(event.currentTarget.dataset.couponId || 0)
    if (!couponInstanceId) {
      this.clearCouponSelection()
      return
    }

    const coupon = this.data.billDetail.availableCoupons.find((item) => item.couponInstanceId === couponInstanceId)
    if (!coupon) {
      return
    }

    if (this.data.selectedCouponInstanceId === couponInstanceId) {
      this.clearCouponSelection()
      return
    }

    this.setData({
      couponValidating: true,
      couponHint: `正在校验 ${coupon.name}…`
    })

    try {
      const result = await validateCoupon({
        billId: this.data.billDetail.billId,
        couponInstanceId
      })

      if (!result.valid) {
        this.clearCouponSelection()
        wx.showToast({
          title: result.message || '当前优惠券不可使用',
          icon: 'none',
          duration: 2200
        })
        return
      }

      const payAmount = Math.max(Number(this.data.billDetail.amountDue) - Number(result.discountAmount || 0), 0)

      this.setData({
        selectedCouponInstanceId: couponInstanceId,
        selectedCouponName: coupon.name,
        couponHint: result.message || `已选择 ${coupon.name}`,
        discountAmountText: formatMoney(result.discountAmount),
        payAmountText: formatMoney(payAmount)
      })
    } catch (error) {
      this.clearCouponSelection()
      wx.showToast({
        title: error instanceof Error ? error.message : '优惠券校验失败',
        icon: 'none',
        duration: 2200
      })
    } finally {
      this.setData({ couponValidating: false })
    }
  },

  handleChannelSelect(event: WechatMiniprogram.TouchEvent) {
    const channel = String(event.currentTarget.dataset.channel || 'WECHAT') as PaymentCreatePayload['channel']
    this.setData({ selectedChannel: channel })
  },

  async handlePay() {
    if (!this.data.billDetail || this.data.paying) {
      return
    }

    this.setData({ paying: true })

    try {
      const payment = await createPayment({
        billId: this.data.billDetail.billId,
        channel: this.data.selectedChannel,
        couponInstanceId: this.data.annualPayment ? undefined : (this.data.selectedCouponInstanceId || undefined),
        annualPayment: this.data.annualPayment,
        idempotencyKey: buildPaymentIdempotencyKey(this.data.billDetail.billId, this.data.annualPayment)
      })

      this.setData({ latestPayOrderNo: payment.payOrderNo })

      try {
        await invokePaymentByChannel(payment)
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
    const payOrderNo = this.data.latestPayOrderNo || this.data.entryPayOrderNo
    if (!payOrderNo) {
      wx.showToast({
        title: '当前没有最近支付单号，请重新发起一次支付查看结果',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: `/pages/payment-result/index?payOrderNo=${payOrderNo}&billId=${this.data.billId}`
    })
  },

  openVoucher() {
    const payOrderNo = this.data.latestPayOrderNo || this.data.entryPayOrderNo
    if (!payOrderNo) {
      wx.showToast({
        title: '当前缺少支付单号，无法查看电子凭证',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: `/pages/voucher/index?payOrderNo=${payOrderNo}`
    })
  }
})
