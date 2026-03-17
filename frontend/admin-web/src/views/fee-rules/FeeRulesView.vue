<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { createFeeRule, getFeeRules } from '@/api/fee-rules'
import { cycleTypeOptions, feeTypeOptions } from '@/constants/options'
import type { FeeRule } from '@/types/fee-rule'
import { formatDate, formatMoney } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const submitLoading = ref(false)
const communityId = ref(100)
const list = ref<FeeRule[]>([])
const formRef = ref<FormInstance>()
const form = reactive({
  communityId: 100,
  feeType: 'PROPERTY',
  unitPrice: 2.5,
  cycleType: 'MONTH',
  effectiveFrom: '',
  effectiveTo: '',
  remark: '',
})

const rules: FormRules<typeof form> = {
  communityId: [{ required: true, message: '请输入小区 ID', trigger: 'blur' }],
  feeType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  cycleType: [{ required: true, message: '请选择周期类型', trigger: 'change' }],
  effectiveFrom: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getFeeRules(communityId.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '费用规则加载失败'
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
    await createFeeRule({
      ...form,
      effectiveTo: form.effectiveTo || undefined,
    })
    ElMessage.success('费用规则创建成功')
    communityId.value = form.communityId
    await loadData()
    form.unitPrice = 2.5
    form.effectiveTo = ''
    form.remark = ''
  } finally {
    submitLoading.value = false
  }
}

watch(communityId, () => {
  form.communityId = communityId.value
})

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">费用规则</h1>
        <p class="page-description">按小区查询并新增费用规则，当前 MVP 主要面向物业费规则配置。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="规则查询" description="后端当前要求传入 communityId。">
        <div class="filter-form">
          <el-input-number v-model="communityId" :min="1" controls-position="right" />
          <el-button type="primary" @click="loadData">查询规则</el-button>
        </div>

        <div style="margin-top: 20px">
          <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="该小区暂无费用规则">
            <el-table :data="list" stripe>
              <el-table-column prop="id" label="ID" width="88" />
              <el-table-column prop="communityId" label="小区ID" width="100" />
              <el-table-column label="费种" width="120">
                <template #default="scope">
                  <StatusTag :value="scope.row.feeType" />
                </template>
              </el-table-column>
              <el-table-column label="单价" min-width="120" align="right">
                <template #default="scope">{{ formatMoney(scope.row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column label="周期" width="100">
                <template #default="scope">
                  <StatusTag :value="scope.row.cycleType" />
                </template>
              </el-table-column>
              <el-table-column label="生效日期" min-width="220">
                <template #default="scope">
                  {{ formatDate(scope.row.effectiveFrom) }} ~ {{ formatDate(scope.row.effectiveTo, '长期有效') }}
                </template>
              </el-table-column>
            </el-table>
          </AsyncState>
        </div>
      </PageSection>

      <PageSection title="新增规则" description="字段与后端 FeeRuleCreateDTO 保持一致。">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="小区 ID" prop="communityId">
              <el-input-number v-model="form.communityId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="费用类型" prop="feeType">
              <el-select v-model="form.feeType">
                <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="周期类型" prop="cycleType">
              <el-select v-model="form.cycleType">
                <el-option v-for="option in cycleTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="生效开始日期" prop="effectiveFrom">
              <el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="生效结束日期">
              <el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </div>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="100" show-word-limit />
          </el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存规则</el-button>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
