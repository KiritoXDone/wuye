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
  return `${Number(value).toFixed(2)}%`
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

export function formatPeriod(year?: number | null, month?: number | null, cycleType?: string | null) {
  if (!year) {
    return '--'
  }
  if (cycleType === 'YEAR' || !month) {
    return `${year} 年度`
  }
  return `${year}-${String(month).padStart(2, '0')}`
}

export function formatServicePeriod(start?: string | null, end?: string | null) {
  if (!start && !end) {
    return '--'
  }
  if (start && end) {
    return `${formatDate(start)} ~ ${formatDate(end)}`
  }
  return formatDate(start || end)
}
