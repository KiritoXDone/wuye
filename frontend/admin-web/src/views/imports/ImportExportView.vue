<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { createBillExport, createBillImport, getExportJob, getImportBatch, getImportBatchErrors } from '@/api/import-export'
import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { billStatusOptions, feeTypeOptions } from '@/constants/options'
import type { ExportJob, ImportBatch, ImportRowError } from '@/types/import-export'
import { formatDateTime } from '@/utils/format'

const importFormRef = ref<FormInstance>()
const exportFormRef = ref<FormInstance>()
const importLoading = ref(false)
const importQueryLoading = ref(false)
const exportLoading = ref(false)
const exportQueryLoading = ref(false)
const importErrorLoading = ref(false)
const importBatch = ref<ImportBatch | null>(null)
const exportJob = ref<ExportJob | null>(null)
const importErrors = ref<ImportRowError[]>([])

const importForm = reactive({
  fileUrl: 'https://example.com/imports/bills-2026-03.xlsx',
})

const exportForm = reactive({
  periodYear: new Date().getFullYear(),
  periodMonth: new Date().getMonth() + 1,
  feeType: 'PROPERTY',
  status: 'ISSUED',
})

const queryForm = reactive({
  importBatchId: undefined as number | undefined,
  exportJobId: undefined as number | undefined,
})

const importRules: FormRules<typeof importForm> = {
  fileUrl: [{ required: true, message: '请输入导入文件地址', trigger: 'blur' }],
}

const exportRules: FormRules<typeof exportForm> = {
  periodYear: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  periodMonth: [{ required: true, message: '请选择月份', trigger: 'change' }],
}

async function handleCreateImport() {
  if (!importFormRef.value) {
    return
  }
  await importFormRef.value.validate()
  importLoading.value = true
  try {
    importBatch.value = await createBillImport(importForm)
    queryForm.importBatchId = importBatch.value.id
    ElMessage.success(`导入批次已创建，当前状态 ${importBatch.value.status}，批次号 ${importBatch.value.batchNo}`)
    importErrors.value = []
  } finally {
    importLoading.value = false
  }
}

async function handleQueryImport() {
  if (!queryForm.importBatchId) {
    ElMessage.warning('请先输入导入批次 ID')
    return
  }
  importQueryLoading.value = true
  try {
    importBatch.value = await getImportBatch(queryForm.importBatchId)
  } finally {
    importQueryLoading.value = false
  }
}

async function handleLoadImportErrors() {
  if (!queryForm.importBatchId) {
    ElMessage.warning('请先输入导入批次 ID')
    return
  }
  importErrorLoading.value = true
  try {
    importErrors.value = await getImportBatchErrors(queryForm.importBatchId)
  } finally {
    importErrorLoading.value = false
  }
}

async function handleCreateExport() {
  if (!exportFormRef.value) {
    return
  }
  await exportFormRef.value.validate()
  exportLoading.value = true
  try {
    exportJob.value = await createBillExport(exportForm)
    queryForm.exportJobId = exportJob.value.id
    ElMessage.success(`导出任务已创建，当前状态 ${exportJob.value.status}，任务 ID ${exportJob.value.id}`)
  } finally {
    exportLoading.value = false
  }
}

async function handleQueryExport() {
  if (!queryForm.exportJobId) {
    ElMessage.warning('请先输入导出任务 ID')
    return
  }
  exportQueryLoading.value = true
  try {
    exportJob.value = await getExportJob(queryForm.exportJobId)
  } finally {
    exportQueryLoading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">导入导出</h1>
        <p class="page-description">提供账单导入、导出任务创建与结果查询的最小闭环，并补充批次错误列表查看。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="账单导入" description="当前后端仅接收 fileUrl，因此页面使用文件地址方式触发导入。">
        <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-position="top">
          <el-form-item label="导入文件地址" prop="fileUrl">
            <el-input v-model="importForm.fileUrl" placeholder="请输入对象存储或可访问文件 URL" />
          </el-form-item>
          <div class="inline-actions">
            <el-button type="primary" :loading="importLoading" @click="handleCreateImport">创建导入批次</el-button>
            <el-input-number v-model="queryForm.importBatchId" :min="1" controls-position="right" placeholder="批次 ID" />
            <el-button :loading="importQueryLoading" @click="handleQueryImport">查询批次</el-button>
            <el-button :loading="importErrorLoading" @click="handleLoadImportErrors">查看错误行</el-button>
          </div>
        </el-form>

        <div style="margin-top: 20px">
          <AsyncState :loading="importQueryLoading" :error="''" :empty="!importBatch" empty-description="创建或查询后可查看导入结果">
            <div v-if="importBatch" class="drawer-field-list">
              <div class="drawer-field">
                <div class="drawer-field__label">批次号</div>
                <div class="drawer-field__value">{{ importBatch.batchNo }}</div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">状态</div>
                <div class="drawer-field__value"><StatusTag :value="importBatch.status" /></div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">总行数</div>
                <div class="drawer-field__value">{{ importBatch.totalCount }}</div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">成功 / 失败</div>
                <div class="drawer-field__value">{{ importBatch.successCount }} / {{ importBatch.failCount }}</div>
              </div>
            </div>
          </AsyncState>
        </div>

        <div style="margin-top: 20px">
          <AsyncState :loading="importErrorLoading" :error="''" :empty="!importErrors.length" empty-description="当前批次暂无错误行">
            <el-table :data="importErrors" stripe>
              <el-table-column prop="rowNo" label="行号" width="80" />
              <el-table-column prop="errorCode" label="错误码" width="140" />
              <el-table-column prop="errorMessage" label="错误信息" min-width="180" />
              <el-table-column prop="rawData" label="原始数据" min-width="220" show-overflow-tooltip />
            </el-table>
          </AsyncState>
        </div>
      </PageSection>

      <PageSection title="账单导出" description="创建导出任务后可继续轮询任务状态，成功时展示 fileUrl。">
        <el-form ref="exportFormRef" :model="exportForm" :rules="exportRules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="年份" prop="periodYear">
              <el-input-number v-model="exportForm.periodYear" :min="2020" :max="2100" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="月份" prop="periodMonth">
              <el-select v-model="exportForm.periodMonth" style="width: 100%">
                <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="费用类型">
              <el-select v-model="exportForm.feeType" clearable>
                <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="账单状态">
              <el-select v-model="exportForm.status" clearable>
                <el-option v-for="option in billStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </div>
          <div class="inline-actions">
            <el-button type="primary" :loading="exportLoading" @click="handleCreateExport">创建导出任务</el-button>
            <el-input-number v-model="queryForm.exportJobId" :min="1" controls-position="right" placeholder="任务 ID" />
            <el-button :loading="exportQueryLoading" @click="handleQueryExport">查询任务</el-button>
          </div>
        </el-form>

        <div style="margin-top: 20px">
          <AsyncState :loading="exportQueryLoading" :error="''" :empty="!exportJob" empty-description="创建或查询后可查看导出任务详情">
            <div v-if="exportJob" class="drawer-field-list">
              <div class="drawer-field">
                <div class="drawer-field__label">任务 ID</div>
                <div class="drawer-field__value">{{ exportJob.id }}</div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">状态</div>
                <div class="drawer-field__value"><StatusTag :value="exportJob.status" /></div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">导出类型</div>
                <div class="drawer-field__value">{{ exportJob.exportType }}</div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">过期时间</div>
                <div class="drawer-field__value">{{ formatDateTime(exportJob.expiredAt) }}</div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">文件地址</div>
                <div class="drawer-field__value">{{ exportJob.fileUrl || '--' }}</div>
              </div>
              <div class="drawer-field">
                <div class="drawer-field__label">请求快照</div>
                <div class="drawer-field__value">{{ exportJob.requestJson || '--' }}</div>
              </div>
            </div>
          </AsyncState>
        </div>
      </PageSection>
    </div>
  </div>
</template>
