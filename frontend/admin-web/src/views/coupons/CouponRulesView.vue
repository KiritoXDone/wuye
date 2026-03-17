<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { createCouponRule, getCouponRules, getCouponTemplates } from '@/api/coupons'
import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { feeTypeOptions } from '@/constants/options'
import type { CouponRule, CouponTemplate } from '@/types/coupon'
import { formatMoney } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const submitLoading = ref(false)
const list = ref<CouponRule[]>([])
const templates = ref<CouponTemplate[]>([])
const formRef = ref<FormInstance>()

const form = reactive({
  name: '物业费支付成功送停车券',
  feeType: 'PROPERTY',
  templateCode: 'VCH-PARK-1H',
  minPayAmount: 100,
  rewardCount: 1,
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  feeType: [{ required: true, message: '请选择费种', trigger: 'change' }],
  templateCode: [{ required: true, message: '请选择奖励券模板', trigger: 'change' }],
  minPayAmount: [{ required: true, message: '请输入最低实付金额', trigger: 'blur' }],
  rewardCount: [{ required: true, message: '请输入发券数量', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [rulesResult, templatesResult] = await Promise.all([getCouponRules(), getCouponTemplates()])
    list.value = rulesResult
    templates.value = templatesResult.filter((item) => item.type === 'VOUCHER')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发券规则加载失败'
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
    await createCouponRule(form)
    ElMessage.success('发券规则创建成功')
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
        <h1 class="page-title">发券规则</h1>
        <p class="page-description">配置支付成功后的奖励券发放规则，当前仅做最小新增与列表核对。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="规则列表" description="列表直接展示后端当前有效规则，方便联调支付成功后的奖励券发放。">
        <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="暂无发券规则">
          <el-table :data="list" stripe>
            <el-table-column prop="ruleName" label="规则名称" min-width="180" />
            <el-table-column label="费种" width="120">
              <template #default="scope">
                <StatusTag :value="scope.row.feeType" />
              </template>
            </el-table-column>
            <el-table-column prop="templateId" label="模板ID" width="100" />
            <el-table-column label="最低实付金额" min-width="130" align="right">
              <template #default="scope">{{ formatMoney(scope.row.minPayAmount) }}</template>
            </el-table-column>
            <el-table-column prop="rewardCount" label="发券数量" width="100" align="right" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <StatusTag :value="scope.row.status" />
              </template>
            </el-table-column>
          </el-table>
        </AsyncState>
      </PageSection>

      <PageSection title="新增规则" description="templateCode 直接提交给后端，前端仅做最小字段校验。">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="规则名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="费种" prop="feeType">
              <el-select v-model="form.feeType">
                <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="奖励券模板" prop="templateCode">
              <el-select v-model="form.templateCode" placeholder="请选择奖励券模板">
                <el-option v-for="template in templates" :key="template.templateCode" :label="`${template.name}（${template.templateCode}）`" :value="template.templateCode" />
              </el-select>
            </el-form-item>
            <el-form-item label="最低实付金额" prop="minPayAmount">
              <el-input-number v-model="form.minPayAmount" :min="0" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="发券数量" prop="rewardCount">
              <el-input-number v-model="form.rewardCount" :min="1" :precision="0" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </div>
          <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存规则</el-button>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
