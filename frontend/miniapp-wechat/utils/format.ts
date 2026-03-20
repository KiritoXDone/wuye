export function formatMoney(value?: number | string | null): string {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

export function formatDate(value?: string): string {
  if (!value) {
    return '--'
  }
  return value.slice(0, 10)
}

export function formatDateTime(value?: string): string {
  if (!value) {
    return '--'
  }
  return value.replace('T', ' ').slice(0, 16)
}

export function formatFeeType(value?: string): string {
  const map: Record<string, string> = {
    PROPERTY: '物业费',
    WATER: '水费'
  }
  return map[value || ''] || '其他费用'
}

export function formatBillStatus(value?: string): string {
  const map: Record<string, string> = {
    ALL: '全部',
    ISSUED: '待缴',
    PAID: '已缴',
    PAYING: '支付中',
    SUCCESS: '支付成功',
    FAILED: '支付失败',
    CREATED: '待支付',
    CLOSED: '已关闭',
    ACTIVE: '已绑定',
    PENDING: '待确认',
    INACTIVE: '已失效',
    CANCELLED: '已取消',
    VOID: '已作废'
  }
  return map[value || ''] || '状态未知'
}

export function formatQuantity(value?: number | string | null): string {
  const quantity = Number(value ?? 0)
  return Number.isFinite(quantity) ? quantity.toFixed(3) : '0.000'
}

export function formatPeriod(year?: number, month?: number): string {
  if (!year) {
    return '--'
  }
  if (!month) {
    return `${year} 年度`
  }
  return `${year}-${String(month).padStart(2, '0')}`
}
