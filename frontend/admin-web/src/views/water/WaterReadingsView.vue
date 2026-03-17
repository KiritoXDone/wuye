<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { createWaterMeter, createWaterReading, getWaterReadings } from '@/api/water'
import type { WaterReading } from '@/types/water'
import { formatDateTime, formatQuantity } from '@/utils/format'

const now = new Date()
const loading = ref(false)
const error = ref('')
const meterLoading = ref(false)
const readingLoading = ref(false)
const readings = ref<WaterReading[]>([])
const meterFormRef = ref<FormInstance>()
const readingFormRef = ref<FormInstance>()

const filters = reactive({
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
})

const meterForm = reactive({
  roomId: 1001,
  meterNo: '',
  installAt: '',
})

const readingForm = reactive({
  roomId: 1001,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  prevReading: 0,
  currReading: 0,
  readAt: '',
  photoUrl: '',
  remark: '',
})

const usagePreview = computed(() => Number(readingForm.currReading) - Number(readingForm.prevReading))

const meterRules: FormRules<typeof meterForm> = {
  roomId: [{ required: true, message: '请输入房间 ID', trigger: 'blur' }],
}

const readingRules: FormRules<typeof readingForm> = {
  roomId: [{ required: true, message: '请输入房间 ID', trigger: 'blur' }],
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  month: [{ required: true, message: '请输入月份', trigger: 'blur' }],
  prevReading: [{ required: true, message: '请输入上期读数', trigger: 'blur' }],
  currReading: [{ required: true, message: '请输入本期读数', trigger: 'blur' }],
  readAt: [{ required: true, message: '请选择抄表时间', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    readings.value = await getWaterReadings(filters)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '抄表记录加载失败'
  } finally {
    loading.value = false
  }
}

async function handleCreateMeter() {
  if (!meterFormRef.value) {
    return
  }
  await meterFormRef.value.validate()
  meterLoading.value = true
  try {
    await createWaterMeter({
      ...meterForm,
      installAt: meterForm.installAt || undefined,
    })
    ElMessage.success('水表配置成功')
  } finally {
    meterLoading.value = false
  }
}

async function handleCreateReading() {
  if (!readingFormRef.value) {
    return
  }
  if (usagePreview.value < 0) {
    ElMessage.error('本期读数不能小于上期读数')
    return
  }
  await readingFormRef.value.validate()
  readingLoading.value = true
  try {
    await createWaterReading({
      ...readingForm,
      photoUrl: readingForm.photoUrl || undefined,
      remark: readingForm.remark || undefined,
    })
    ElMessage.success('抄表录入成功')
    await loadData()
  } finally {
    readingLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">水表与抄表</h1>
        <p class="page-description">先配置房间水表，再按账期录入抄表。当前后端只开放水表创建/更新与抄表列表接口。</p>
      </div>
      <div class="filter-form">
        <el-input-number v-model="filters.periodYear" :min="2020" :max="2100" controls-position="right" />
        <el-select v-model="filters.periodMonth" style="width: 140px">
          <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
        </el-select>
        <el-button type="primary" @click="loadData">查询记录</el-button>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="配置水表" description="通过 POST /admin/water-meters 创建或更新房间水表。">
        <el-form ref="meterFormRef" :model="meterForm" :rules="meterRules" label-position="top">
          <el-form-item label="房间 ID" prop="roomId">
            <el-input-number v-model="meterForm.roomId" :min="1" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="水表编号">
            <el-input v-model="meterForm.meterNo" placeholder="可选" />
          </el-form-item>
          <el-form-item label="安装日期">
            <el-date-picker v-model="meterForm.installAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-button type="primary" :loading="meterLoading" @click="handleCreateMeter">保存水表</el-button>
        </el-form>
      </PageSection>

      <PageSection title="录入抄表" description="前端即时校验 currReading 不能小于 prevReading。">
        <el-form ref="readingFormRef" :model="readingForm" :rules="readingRules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="房间 ID" prop="roomId">
              <el-input-number v-model="readingForm.roomId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年份" prop="year">
              <el-input-number v-model="readingForm.year" :min="2020" :max="2100" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="月份" prop="month">
              <el-select v-model="readingForm.month" style="width: 100%">
                <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="抄表时间" prop="readAt">
              <el-date-picker v-model="readingForm.readAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
            <el-form-item label="上期读数" prop="prevReading">
              <el-input-number v-model="readingForm.prevReading" :min="0" :precision="3" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="本期读数" prop="currReading">
              <el-input-number v-model="readingForm.currReading" :min="0" :precision="3" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </div>
          <el-alert
            :title="`预计用量：${formatQuantity(usagePreview)}`"
            :type="usagePreview < 0 ? 'error' : 'info'"
            show-icon
            :closable="false"
            style="margin-bottom: 16px"
          />
          <el-form-item label="照片地址">
            <el-input v-model="readingForm.photoUrl" placeholder="可选，保留后端字段映射" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="readingForm.remark" type="textarea" :rows="3" maxlength="100" show-word-limit />
          </el-form-item>
          <el-button type="primary" :loading="readingLoading" @click="handleCreateReading">保存抄表</el-button>
        </el-form>
      </PageSection>
    </div>

    <PageSection title="抄表记录" description="按账期查看当前已录入的抄表记录。">
      <AsyncState :loading="loading" :error="error" :empty="!readings.length" empty-description="当前账期暂无抄表记录">
        <el-table :data="readings" stripe>
          <el-table-column prop="roomLabel" label="房间" min-width="140" />
          <el-table-column label="账期" width="110">
            <template #default="scope">{{ scope.row.periodYear }}-{{ String(scope.row.periodMonth).padStart(2, '0') }}</template>
          </el-table-column>
          <el-table-column label="上期读数" min-width="120" align="right">
            <template #default="scope">{{ formatQuantity(scope.row.prevReading) }}</template>
          </el-table-column>
          <el-table-column label="本期读数" min-width="120" align="right">
            <template #default="scope">{{ formatQuantity(scope.row.currReading) }}</template>
          </el-table-column>
          <el-table-column label="用量" min-width="100" align="right">
            <template #default="scope">{{ formatQuantity(scope.row.usageAmount) }}</template>
          </el-table-column>
          <el-table-column label="抄表时间" min-width="160">
            <template #default="scope">{{ formatDateTime(scope.row.readAt) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <StatusTag :value="scope.row.status" />
            </template>
          </el-table-column>
        </el-table>
      </AsyncState>
    </PageSection>
  </div>
</template>
