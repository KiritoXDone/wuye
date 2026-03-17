<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { createFeeRule, getFeeRules } from '@/api/fee-rules'
import { cycleTypeOptions, feeTypeOptions, pricingModeOptions } from '@/constants/options'
import type { FeeRule, FeeRuleWaterTier } from '@/types/fee-rule'
import { formatDate, formatMoney, formatQuantity } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const submitLoading = ref(false)
const communityId = ref(100)
const list = ref<FeeRule[]>([])
const formRef = ref<FormInstance>()

function createTier(startUsage = 0, endUsage?: number, unitPrice = 0): FeeRuleWaterTier {
  return {
    startUsage,
    endUsage,
    unitPrice,
  }
}

const form = reactive({
  communityId: 100,
  feeType: 'PROPERTY',
  unitPrice: 2.5,
  cycleType: 'MONTH',
  pricingMode: 'FLAT',
  effectiveFrom: '',
  effectiveTo: '',
  remark: '',
  abnormalAbsThreshold: undefined as number | undefined,
  abnormalMultiplierThreshold: undefined as number | undefined,
  waterTiers: [createTier(0, 5, 2)] as FeeRuleWaterTier[],
})

const rules: FormRules<typeof form> = {
  communityId: [{ required: true, message: '请输入小区 ID', trigger: 'blur' }],
  feeType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  cycleType: [{ required: true, message: '请选择周期类型', trigger: 'change' }],
  pricingMode: [{ required: true, message: '请选择计价方式', trigger: 'change' }],
  effectiveFrom: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
}

function resetFormAfterSubmit() {
  form.unitPrice = form.feeType === 'WATER' ? 3.2 : 2.5
  form.pricingMode = form.feeType === 'WATER' ? 'TIERED' : 'FLAT'
  form.effectiveTo = ''
  form.remark = ''
  form.abnormalAbsThreshold = undefined
  form.abnormalMultiplierThreshold = undefined
  form.waterTiers = [createTier(0, 5, 2)]
}

function addTier() {
  const lastTier = form.waterTiers[form.waterTiers.length - 1]
  const startUsage = Number(lastTier?.endUsage ?? lastTier?.startUsage ?? 0)
  form.waterTiers.push(createTier(startUsage, undefined, Number(form.unitPrice) || 0))
}

function removeTier(index: number) {
  if (form.waterTiers.length <= 1) {
    return
  }
  form.waterTiers.splice(index, 1)
}

function buildSubmitPayload() {
  const isWater = form.feeType === 'WATER'
  const isTieredWater = isWater && form.pricingMode === 'TIERED'
  return {
    ...form,
    effectiveTo: form.effectiveTo || undefined,
    abnormalAbsThreshold: isWater ? form.abnormalAbsThreshold : undefined,
    abnormalMultiplierThreshold: isWater ? form.abnormalMultiplierThreshold : undefined,
    waterTiers: isTieredWater
      ? form.waterTiers.map((tier) => ({
          startUsage: Number(tier.startUsage),
          endUsage: tier.endUsage === undefined || tier.endUsage === null || tier.endUsage === '' ? undefined : Number(tier.endUsage),
          unitPrice: Number(tier.unitPrice),
        }))
      : [],
  }
}

function validateTieredWaterRules() {
  if (form.feeType !== 'WATER' || form.pricingMode !== 'TIERED') {
    return true
  }

  for (let index = 0; index < form.waterTiers.length; index += 1) {
    const tier = form.waterTiers[index]
    const startUsage = Number(tier.startUsage)
    const endUsage = tier.endUsage === undefined || tier.endUsage === null || tier.endUsage === '' ? undefined : Number(tier.endUsage)
    const unitPrice = Number(tier.unitPrice)

    if (!Number.isFinite(startUsage) || startUsage < 0 || !Number.isFinite(unitPrice) || unitPrice < 0) {
      ElMessage.error(`第 ${index + 1} 档阶梯的起始用量和单价必须大于等于 0`)
      return false
    }

    if (endUsage !== undefined && (!Number.isFinite(endUsage) || endUsage <= startUsage)) {
      ElMessage.error(`第 ${index + 1} 档阶梯的结束用量必须大于起始用量`)
      return false
    }

    if (index > 0) {
      const previousTier = form.waterTiers[index - 1]
      const previousEnd = previousTier.endUsage === undefined || previousTier.endUsage === null || previousTier.endUsage === ''
        ? undefined
        : Number(previousTier.endUsage)
      if (previousEnd !== undefined && startUsage < previousEnd) {
        ElMessage.error(`第 ${index + 1} 档阶梯的起始用量不能小于上一档结束用量`)
        return false
      }
    }

    if (index !== form.waterTiers.length - 1 && endUsage === undefined) {
      ElMessage.error('只有最后一档阶梯可以不填写结束用量')
      return false
    }
  }

  return true
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
  if (!validateTieredWaterRules()) {
    return
  }
  submitLoading.value = true
  try {
    await createFeeRule(buildSubmitPayload())
    ElMessage.success('费用规则创建成功')
    communityId.value = form.communityId
    await loadData()
    resetFormAfterSubmit()
  } finally {
    submitLoading.value = false
  }
}

watch(communityId, () => {
  form.communityId = communityId.value
})

watch(
  () => form.feeType,
  (value) => {
    if (value === 'WATER') {
      form.pricingMode = 'TIERED'
      if (!form.waterTiers.length) {
        form.waterTiers = [createTier(0, 5, Number(form.unitPrice) || 0)]
      }
      return
    }
    form.pricingMode = 'FLAT'
  },
  { immediate: true },
)

watch(
  () => form.pricingMode,
  (value) => {
    if (value === 'TIERED' && !form.waterTiers.length) {
      form.waterTiers = [createTier(0, 5, Number(form.unitPrice) || 0)]
    }
  },
)

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">费用规则</h1>
        <p class="page-description">按小区查询并新增费用规则，现已支持水费阶梯定价、异常阈值与水价分段配置。</p>
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
              <el-table-column label="计价方式" width="120">
                <template #default="scope">
                  <StatusTag :value="scope.row.pricingMode || 'FLAT'" />
                </template>
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
              <el-table-column label="异常阈值" min-width="220">
                <template #default="scope">
                  <template v-if="scope.row.feeType === 'WATER'">
                    绝对值 {{ scope.row.abnormalAbsThreshold === undefined || scope.row.abnormalAbsThreshold === null ? '--' : formatQuantity(scope.row.abnormalAbsThreshold) }}
                    / 倍数 {{ scope.row.abnormalMultiplierThreshold ?? '--' }}
                  </template>
                  <template v-else>--</template>
                </template>
              </el-table-column>
              <el-table-column label="水价分段" min-width="260">
                <template #default="scope">
                  <template v-if="scope.row.waterTiers?.length">
                    <el-space wrap>
                      <el-tag v-for="tier in scope.row.waterTiers" :key="`${scope.row.id}-${tier.tierOrder}-${tier.startUsage}`" round>
                        {{ formatQuantity(tier.startUsage) }} - {{ tier.endUsage === undefined ? '以上' : formatQuantity(tier.endUsage) }} / {{ formatMoney(tier.unitPrice) }}
                      </el-tag>
                    </el-space>
                  </template>
                  <template v-else>--</template>
                </template>
              </el-table-column>
            </el-table>
          </AsyncState>
        </div>
      </PageSection>

      <PageSection title="新增规则" description="字段与后端 FeeRuleCreateDTO 保持一致，水费支持阶梯价和异常阈值。">
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
              <div class="page-description">{{ form.feeType === 'WATER' ? '水费规则需保留基础单价，阶梯价开启后再按分段单价精细计费。' : '物业费按固定单价配置。' }}</div>
            </el-form-item>
            <el-form-item label="周期类型" prop="cycleType">
              <el-select v-model="form.cycleType">
                <el-option v-for="option in cycleTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="计价方式" prop="pricingMode">
              <el-select v-model="form.pricingMode" :disabled="form.feeType !== 'WATER'">
                <el-option v-for="option in pricingModeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="生效开始日期" prop="effectiveFrom">
              <el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="生效结束日期">
              <el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </div>

          <template v-if="form.feeType === 'WATER'">
            <div class="form-card-grid" style="margin-bottom: 16px">
              <el-form-item label="异常绝对阈值（吨）">
                <el-input-number v-model="form.abnormalAbsThreshold" :min="0" :precision="3" :step="1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="异常倍数阈值">
                <el-input-number v-model="form.abnormalMultiplierThreshold" :min="0" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
              </el-form-item>
            </div>

            <div v-if="form.pricingMode === 'TIERED'" class="panel-card" style="margin-bottom: 16px">
              <div class="panel-card__header">
                <div>
                  <h3 class="panel-card__title">阶梯水价</h3>
                  <p class="page-description">逐段维护用量区间和对应单价，最后一档可不填结束用量表示“以上”。</p>
                </div>
                <el-button type="primary" plain @click="addTier">新增阶梯</el-button>
              </div>
              <div class="panel-card__body page-grid">
                <div v-for="(tier, index) in form.waterTiers" :key="`tier-${index}`" class="form-card-grid" style="align-items: end">
                  <el-form-item :label="`起始用量 ${index + 1}`">
                    <el-input-number v-model="tier.startUsage" :min="0" :precision="3" :step="1" controls-position="right" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="结束用量">
                    <el-input-number v-model="tier.endUsage" :min="0" :precision="3" :step="1" controls-position="right" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="阶梯单价">
                    <el-input-number v-model="tier.unitPrice" :min="0" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="操作">
                    <el-button :disabled="form.waterTiers.length <= 1" @click="removeTier(index)">删除</el-button>
                  </el-form-item>
                </div>
              </div>
            </div>
          </template>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="100" show-word-limit />
          </el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存规则</el-button>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
