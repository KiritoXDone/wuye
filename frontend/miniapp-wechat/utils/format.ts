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
  return map[value || ''] || value || '--'
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
    INACTIVE: '已失效'
  }
  return map[value || ''] || value || '--'
}

export function formatRelationType(value?: string): string {
  const map: Record<string, string> = {
    OWNER: '业主',
    TENANT: '租户',
    FAMILY: '家属'
  }
  return map[value || ''] || value || '--'
}

export function formatPeriod(year?: number, month?: number): string {
  if (!year || !month) {
    return '--'
  }
  return `${year}-${String(month).padStart(2, '0')}`
}
