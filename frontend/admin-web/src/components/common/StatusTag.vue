<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value?: string | number
}>()

const normalizedValue = computed(() => {
  if (props.value === undefined || props.value === null) {
    return ''
  }
  return String(props.value)
})

const tagType = computed(() => {
  switch (normalizedValue.value) {
    case 'PAID':
    case 'NORMAL':
    case 'SUCCESS':
    case 'ACTIVE':
    case 'APPROVED':
    case 'MANAGE':
    case '1':
      return 'success'
    case 'ISSUED':
    case 'PROCESSING':
    case 'APPLIED':
    case 'OPEN':
      return 'warning'
    case 'FAILED':
    case 'INACTIVE':
    case 'CLOSED':
    case 'REJECTED':
    case '0':
      return 'danger'
    case 'CANCELLED':
      return 'info'
    default:
      return 'info'
  }
})

const label = computed(() => {
  const map: Record<string, string> = {
    ALL: '全部',
    PROPERTY: '物业费',
    WATER: '水费',
    PAYMENT: '抵扣券',
    VOUCHER: '奖励券',
    FIXED: '固定金额',
    PERCENT: '比例折扣',
    PAID: '已支付',
    ISSUED: '已出账',
    CANCELLED: '已取消',
    NORMAL: '正常',
    MONTH: '按月',
    PROCESSING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
    APPROVED: '已通过',
    APPLIED: '已申请',
    REJECTED: '已驳回',
    VIEW: '查看',
    MANAGE: '管理',
    ACTIVE: '启用',
    INACTIVE: '停用',
    FLAT: '固定单价',
    TIERED: '阶梯水价',
    ABS_THRESHOLD: '绝对阈值',
    MULTIPLIER_THRESHOLD: '倍数阈值',
    AUTO: '自动触发',
    MANUAL: '手动触发',
    SENT: '已发送',
    OPEN: '待处理',
    SYSTEM: '系统发送',
    1: '启用',
    0: '停用',
  }
  return normalizedValue.value ? map[normalizedValue.value] || normalizedValue.value : '--'
})
</script>

<template>
  <el-tag :type="tagType" effect="light" round>{{ label }}</el-tag>
</template>
