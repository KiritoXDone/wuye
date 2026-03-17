<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value?: string
}>()

const tagType = computed(() => {
  switch (props.value) {
    case 'PAID':
    case 'NORMAL':
      return 'success'
    case 'ISSUED':
      return 'warning'
    case 'CANCELLED':
      return 'info'
    default:
      return 'info'
  }
})

const label = computed(() => {
  const map: Record<string, string> = {
    PROPERTY: '物业费',
    WATER: '水费',
    PAID: '已支付',
    ISSUED: '已出账',
    CANCELLED: '已取消',
    NORMAL: '正常',
    MONTH: '按月',
  }
  return props.value ? map[props.value] || props.value : '--'
})
</script>

<template>
  <el-tag :type="tagType" effect="light" round>{{ label }}</el-tag>
</template>
