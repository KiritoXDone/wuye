import dayjs from 'dayjs'

export function formatMoney(value?: number | string | null) {
  if (value === null || value === undefined || value === '') {
    return '¥0.00'
  }
  return `¥${Number(value).toFixed(2)}`
}

export function formatQuantity(value?: number | string | null) {
  if (value === null || value === undefined || value === '') {
    return '0.000'
  }
  return Number(value).toFixed(3)
}

export function formatPercent(value?: number | string | null) {
  if (value === null || value === undefined || value === '') {
    return '0.00%'
  }
  const numeric = Number(value)
  return `${numeric.toFixed(2)}%`
}

export function formatDate(value?: string | null, fallback = '--') {
  if (!value) {
    return fallback
  }
  return dayjs(value).format('YYYY-MM-DD')
}

export function formatDateTime(value?: string | null, fallback = '--') {
  if (!value) {
    return fallback
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export function formatPeriod(year?: number, month?: number) {
  if (!year || !month) {
    return '--'
  }
  return `${year}-${String(month).padStart(2, '0')}`
}
