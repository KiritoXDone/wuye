export const feeTypeOptions = [
  { label: '物业费', value: 'PROPERTY' },
  { label: '水费', value: 'WATER' },
]

export const feeTypeWithAllOptions = [
  { label: '全部费种', value: 'ALL' },
  ...feeTypeOptions,
]

export const billStatusOptions = [
  { label: '已出账', value: 'ISSUED' },
  { label: '已支付', value: 'PAID' },
  { label: '已取消', value: 'CANCELLED' },
]

export const cycleTypeOptions = [
  { label: '月', value: 'MONTH' },
  { label: '年', value: 'YEAR' },
]

export const pricingModeOptions = [
  { label: '固定单价', value: 'FLAT' },
  { label: '阶梯水价', value: 'TIERED' },
]

export const overwriteStrategyOptions = [
  { label: '跳过已存在账单', value: 'SKIP' },
]

export const couponTemplateTypeOptions = [
  { label: '支付前抵扣券', value: 'PAYMENT' },
  { label: '支付后奖励券', value: 'VOUCHER' },
]

export const couponTriggerTypeOptions = [
  { label: '不自动发放', value: '' },
  { label: '支付完物业费发放', value: 'PROPERTY_PAYMENT' },
  { label: '支付完水费发放', value: 'WATER_PAYMENT' },
  { label: '登录赠送', value: 'LOGIN' },
]

export const discountModeOptions = [
  { label: '固定金额', value: 'FIXED' },
  { label: '比例折扣', value: 'PERCENT' },
]

export const permissionOptions = [
  { label: '查看', value: 'VIEW' },
  { label: '管理', value: 'MANAGE' },
]

export const enabledStatusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

export const invoiceStatusOptions = [
  { label: '已申请', value: 'APPLIED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]
