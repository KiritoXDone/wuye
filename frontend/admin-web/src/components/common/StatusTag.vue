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
    case 'MANAGE':
    case '1':
      return 'success'
    case 'ISSUED':
    case 'PROCESSING':
      return 'warning'
    case 'FAILED':
    case 'INACTIVE':
    case 'CLOSED':
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
    VIEW: '查看',
    MANAGE: '管理',
    ACTIVE: '启用',
    INACTIVE: '停用',
    1: '启用',
    0: '停用',
  }
  return normalizedValue.value ? map[normalizedValue.value] || normalizedValue.value : '--'
})
</script>

<template>
  <el-tag :type="tagType" effect="light" round>{{ label }}</el-tag>
</template>
