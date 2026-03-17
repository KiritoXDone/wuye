<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { createCouponTemplate, getCouponTemplates } from '@/api/coupons'
import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { couponTemplateTypeOptions, discountModeOptions, enabledStatusOptions, feeTypeOptions } from '@/constants/options'
import type { CouponTemplate } from '@/types/coupon'
import { formatDateTime, formatMoney } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const submitLoading = ref(false)
const list = ref<CouponTemplate[]>([])
const formRef = ref<FormInstance>()

const form = reactive({
  templateCode: 'PAY-OFF-10',
  type: 'PAYMENT',
  feeType: 'PROPERTY',
  name: '满100减10物业券',
  discountMode: 'FIXED',
  valueAmount: 10,
  thresholdAmount: 100,
  validFrom: '2026-01-01 00:00:00',
  validTo: '2026-12-31 23:59:59',
})

const rules: FormRules<typeof form> = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择券类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  discountMode: [{ required: true, message: '请选择抵扣模式', trigger: 'change' }],
  valueAmount: [{ required: true, message: '请输入抵扣值', trigger: 'blur' }],
  thresholdAmount: [{ required: true, message: '请输入使用门槛', trigger: 'blur' }],
  validFrom: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  validTo: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getCouponTemplates()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '券模板加载失败'
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  submitLoading.value = true
  try {
    await createCouponTemplate({
      ...form,
      feeType: form.feeType || undefined,
    })
    ElMessage.success('券模板创建成功')
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">券模板</h1>
        <p class="page-description">维护支付前抵扣券与支付后奖励券模板，字段与后端 CouponTemplateCreateDTO 对齐。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="模板列表" description="当前接口返回启用中的模板，可快速核对费种、有效期和抵扣规则。">
        <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="暂无券模板">
          <el-table :data="list" stripe>
            <el-table-column prop="templateCode" label="模板编码" min-width="140" />
            <el-table-column prop="name" label="券名称" min-width="160" />
            <el-table-column label="类型" width="120">
              <template #default="scope">
                <StatusTag :value="scope.row.type" />
              </template>
            </el-table-column>
            <el-table-column label="费种" width="120">
              <template #default="scope">
                <StatusTag :value="scope.row.feeType || 'ALL'" />
              </template>
            </el-table-column>
            <el-table-column label="抵扣模式" width="120">
              <template #default="scope">
                <StatusTag :value="scope.row.discountMode" />
              </template>
            </el-table-column>
            <el-table-column label="抵扣值" min-width="120" align="right">
              <template #default="scope">{{ formatMoney(scope.row.valueAmount) }}</template>
            </el-table-column>
            <el-table-column label="门槛" min-width="120" align="right">
              <template #default="scope">{{ formatMoney(scope.row.thresholdAmount) }}</template>
            </el-table-column>
            <el-table-column label="有效期" min-width="220">
              <template #default="scope">
                {{ formatDateTime(scope.row.validFrom) }} ~ {{ formatDateTime(scope.row.validTo) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <StatusTag :value="scope.row.status" />
              </template>
            </el-table-column>
          </el-table>
        </AsyncState>
      </PageSection>

      <PageSection title="新增模板" description="默认给出仓库示例值，便于直接联调创建。">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" />
            </el-form-item>
            <el-form-item label="券类型" prop="type">
              <el-select v-model="form.type">
                <el-option v-for="option in couponTemplateTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="适用费种">
              <el-select v-model="form.feeType" clearable placeholder="不填则按后端默认逻辑处理">
                <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="券名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="抵扣模式" prop="discountMode">
              <el-select v-model="form.discountMode">
                <el-option v-for="option in discountModeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="抵扣值" prop="valueAmount">
              <el-input-number v-model="form.valueAmount" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="使用门槛" prop="thresholdAmount">
              <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="建议状态">
              <el-select :model-value="1" disabled>
                <el-option v-for="option in enabledStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始时间" prop="validFrom">
              <el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
            <el-form-item label="结束时间" prop="validTo">
              <el-date-picker v-model="form.validTo" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </div>
          <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存模板</el-button>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
