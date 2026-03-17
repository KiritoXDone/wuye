<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getDunningLogs, getDunningTasks, triggerDunning } from '@/api/dunning'
import type { DunningLog, DunningTask } from '@/types/dunning'
import { formatDate, formatDateTime } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const triggerLoading = ref(false)
const logLoading = ref(false)
const tasks = ref<DunningTask[]>([])
const logs = ref<DunningLog[]>([])
const selectedBillId = ref<number>()
const form = reactive({
  triggerDate: '',
})

async function loadTasks() {
  loading.value = true
  error.value = ''
  try {
    tasks.value = await getDunningTasks()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '催缴任务加载失败'
  } finally {
    loading.value = false
  }
}

async function handleTrigger() {
  triggerLoading.value = true
  try {
    const result = await triggerDunning({ triggerDate: form.triggerDate || undefined })
    tasks.value = result
    ElMessage.success(`催缴触发完成，本次返回 ${result.length} 条任务`)
  } finally {
    triggerLoading.value = false
  }
}

async function handleLoadLogs(billId: number) {
  selectedBillId.value = billId
  logLoading.value = true
  try {
    logs.value = await getDunningLogs(billId)
  } finally {
    logLoading.value = false
  }
}

onMounted(loadTasks)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">催缴任务</h1>
        <p class="page-description">支持手动触发催缴，并按账单查看发送日志，用于核对组织与用户组范围是否正确。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="手动触发" description="triggerDate 留空时使用后端默认日期，填写后按指定日期执行一次催缴扫描。">
        <div class="inline-actions">
          <el-date-picker v-model="form.triggerDate" type="date" value-format="YYYY-MM-DD" placeholder="触发日期（可选）" />
          <el-button type="primary" :loading="triggerLoading" @click="handleTrigger">立即触发</el-button>
          <el-button @click="loadTasks">刷新任务</el-button>
        </div>
      </PageSection>

      <PageSection title="发送日志" :description="selectedBillId ? `当前查看账单 ${selectedBillId} 的催缴日志。` : '点击下方任务行的查看日志后，在这里展示发送明细。'">
        <AsyncState :loading="logLoading" :error="''" :empty="!logs.length" empty-description="暂无催缴日志">
            <el-table :data="logs" stripe>
              <el-table-column prop="taskId" label="任务ID" width="100" />
              <el-table-column label="发送渠道" width="120">
                <template #default="scope">
                  <StatusTag :value="scope.row.sendChannel" />
                </template>
              </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <StatusTag :value="scope.row.status" />
              </template>
            </el-table-column>
            <el-table-column prop="content" label="发送内容" min-width="220" show-overflow-tooltip />
            <el-table-column label="发送时间" min-width="160">
              <template #default="scope">{{ formatDateTime(scope.row.sentAt) }}</template>
            </el-table-column>
          </el-table>
        </AsyncState>
      </PageSection>
    </div>

    <PageSection title="催缴任务列表" description="列表展示账单、组织、用户组与触发方式，支持按账单查看日志。">
      <AsyncState :loading="loading" :error="error" :empty="!tasks.length" empty-description="暂无催缴任务">
        <el-table :data="tasks" stripe>
          <el-table-column prop="taskNo" label="任务编号" min-width="180" />
          <el-table-column prop="billId" label="账单ID" width="100" />
          <el-table-column prop="groupId" label="用户组ID" width="110" />
          <el-table-column prop="orgUnitId" label="组织ID" width="100" />
          <el-table-column prop="tenantCode" label="租户编码" min-width="140" />
          <el-table-column prop="remark" label="任务说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="触发方式" width="120">
            <template #default="scope">
              <StatusTag :value="scope.row.triggerType" />
            </template>
          </el-table-column>
          <el-table-column label="触发日期" width="120">
            <template #default="scope">{{ formatDate(scope.row.triggerDate) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <StatusTag :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="执行时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.executedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleLoadLogs(scope.row.billId)">查看日志</el-button>
            </template>
          </el-table-column>
        </el-table>
      </AsyncState>
    </PageSection>
  </div>
</template>
