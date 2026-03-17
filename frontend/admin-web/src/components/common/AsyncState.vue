<script setup lang="ts">
defineProps<{
  loading: boolean
  error: string
  empty: boolean
  emptyDescription?: string
}>()
</script>

<template>
  <div v-if="loading" class="status-stack">
    <el-skeleton :rows="6" animated style="width: 100%" />
  </div>
  <div v-else-if="error" class="error-state">
    <el-result icon="error" title="加载失败" :sub-title="error" />
  </div>
  <div v-else-if="empty" class="empty-state">
    <el-empty :description="emptyDescription || '暂无数据'" />
  </div>
  <slot v-else />
</template>
