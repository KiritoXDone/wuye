<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getWaterAlerts } from '@/api/water-alerts'
import type { WaterAlert } from '@/types/water-alert'
import { formatDateTime, formatQuantity } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const list = ref<WaterAlert[]>([])
const filters = reactive({
  periodYear: new Date().getFullYear(),
  periodMonth: new Date().getMonth() + 1,
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getWaterAlerts(filters)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '水量预警加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">水量预警</h1>
        <p class="page-description">按账期查看异常用水预警，帮助快速识别绝对阈值（ABS_THRESHOLD）或倍数阈值（MULTIPLIER_THRESHOLD）触发的房间。</p>
      </div>
      <div class="filter-form">
        <el-input-number v-model="filters.periodYear" :min="2020" :max="2100" controls-position="right" />
        <el-select v-model="filters.periodMonth" style="width: 140px">
          <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
        </el-select>
        <el-button type="primary" @click="loadData">查询预警</el-button>
      </div>
    </div>

    <PageSection title="预警列表" description="阈值与实际值统一保留三位小数；当前后端仅返回 OPEN 状态预警事件，用于提示与追踪，不阻断开单。">
      <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="当前账期暂无水量预警">
        <el-table :data="list" stripe>
          <el-table-column prop="roomLabel" label="房间" min-width="140" />
          <el-table-column prop="readingId" label="抄表ID" width="110" />
          <el-table-column label="预警类型" width="140">
            <template #default="scope">
              <StatusTag :value="scope.row.alertCode" />
            </template>
          </el-table-column>
          <el-table-column prop="alertMessage" label="预警说明" min-width="220" />
          <el-table-column label="阈值" min-width="120" align="right">
            <template #default="scope">{{ formatQuantity(scope.row.thresholdValue) }}</template>
          </el-table-column>
          <el-table-column label="实际值" min-width="120" align="right">
            <template #default="scope">{{ formatQuantity(scope.row.actualValue) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <StatusTag :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column prop="roomId" label="房间ID" width="110" />
          <el-table-column label="创建时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </AsyncState>
    </PageSection>
  </div>
</template>
