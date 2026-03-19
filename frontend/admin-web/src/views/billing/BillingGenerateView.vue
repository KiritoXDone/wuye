<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import PageSection from '@/components/common/PageSection.vue'
import { generatePropertyBill, generateWaterBill } from '@/api/billing'
import { overwriteStrategyOptions } from '@/constants/options'

const propertyLoading = ref(false)
const waterLoading = ref(false)
const propertyRef = ref<FormInstance>()
const waterRef = ref<FormInstance>()

const propertyForm = reactive({
  communityId: 100,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  overwriteStrategy: 'SKIP',
})

const waterForm = reactive({
  communityId: 100,
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  overwriteStrategy: 'SKIP',
})

const rules: FormRules = {
  communityId: [{ required: true, message: '请输入小区 ID', trigger: 'blur' }],
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  month: [{ required: true, message: '请选择月份', trigger: 'change' }],
}

async function submitProperty() {
  if (!propertyRef.value) {
    return
  }
  await propertyRef.value.validate()
  propertyLoading.value = true
  try {
    const result = await generatePropertyBill(propertyForm)
    ElMessage.success(`物业费账单生成完成，本次生成 ${result.generatedCount} 条`)
  } finally {
    propertyLoading.value = false
  }
}

async function submitWater() {
  if (!waterRef.value) {
    return
  }
  await waterRef.value.validate()
  waterLoading.value = true
  try {
    const result = await generateWaterBill(waterForm)
    ElMessage.success(`水费账单生成完成，本次生成 ${result.generatedCount} 条`)
  } finally {
    waterLoading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">账单生成</h1>
        <p class="page-description">分别触发物业费开单和水费开单，参数与后端生成接口完全对齐。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="物业费开单" description="POST /admin/bills/generate/property">
        <el-form ref="propertyRef" :model="propertyForm" :rules="rules" label-position="top">
          <div class="layout-tag" style="margin-bottom: 16px;">物业费会按对应费率的“月 / 年”周期自动计算；若使用年费率，系统会按月折算生成账单。</div>
          <div class="form-card-grid">
            <el-form-item label="小区 ID" prop="communityId">
              <el-input-number v-model="propertyForm.communityId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年份" prop="year">
              <el-input-number v-model="propertyForm.year" :min="2020" :max="2100" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="月份" prop="month">
              <el-select v-model="propertyForm.month" style="width: 100%">
                <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="覆盖策略">
              <el-select v-model="propertyForm.overwriteStrategy" style="width: 100%">
                <el-option v-for="option in overwriteStrategyOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </div>
          <el-button type="primary" :loading="propertyLoading" @click="submitProperty">生成物业费账单</el-button>
        </el-form>
      </PageSection>

      <PageSection title="水费开单" description="POST /admin/bills/generate/water，需要先有对应账期抄表记录。">
        <el-form ref="waterRef" :model="waterForm" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="小区 ID" prop="communityId">
              <el-input-number v-model="waterForm.communityId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年份" prop="year">
              <el-input-number v-model="waterForm.year" :min="2020" :max="2100" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="月份" prop="month">
              <el-select v-model="waterForm.month" style="width: 100%">
                <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
              </el-select>
            </el-form-item>
            <el-form-item label="覆盖策略">
              <el-select v-model="waterForm.overwriteStrategy" style="width: 100%">
                <el-option v-for="option in overwriteStrategyOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </div>
          <el-button type="primary" :loading="waterLoading" @click="submitWater">生成水费账单</el-button>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
