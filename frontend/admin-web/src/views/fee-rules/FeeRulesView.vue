<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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

const propertyRuleCount = computed(() => list.value.filter((item) => item.feeType === 'PROPERTY').length)
const waterRuleCount = computed(() => list.value.filter((item) => item.feeType === 'WATER').length)
const tieredRuleCount = computed(() => list.value.filter((item) => item.pricingMode === 'TIERED').length)
const thresholdRuleCount = computed(() => list.value.filter((item) => item.feeType === 'WATER' && (item.abnormalAbsThreshold !== undefined || item.abnormalMultiplierThreshold !== undefined)).length)
const activePeriodHint = computed(() => (list.value[0]?.effectiveFrom ? `最近生效开始于 ${formatDate(list.value[0]?.effectiveFrom)}` : '可按小区 ID 查询当前规则'))

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
      <div class="page-header__eyebrow">计费配置中心</div>
      <div class="page-header__headline">
        <div>
          <h1 class="page-title">费用规则</h1>
          <p class="page-description">围绕小区费率、水价分段和异常阈值维护正式配置台账，让运营配置、财务复核与开单链路共享同一套规则入口。</p>
        </div>
        <div class="page-header__actions">
          <div class="layout-tag">当前小区 ID {{ communityId }}</div>
          <div class="layout-tag">水费支持阶梯价</div>
          <div class="layout-tag">提交字段保持现有 DTO</div>
        </div>
      </div>
      <div class="page-stat-grid">
        <article class="page-stat-card">
          <span class="page-stat-card__label">当前小区规则数</span>
          <p class="page-stat-card__value">{{ list.length }}</p>
          <p class="page-stat-card__hint">{{ activePeriodHint }}</p>
        </article>
        <article class="page-stat-card">
          <span class="page-stat-card__label">物业费规则</span>
          <p class="page-stat-card__value">{{ propertyRuleCount }}</p>
          <p class="page-stat-card__hint">固定单价配置为主</p>
        </article>
        <article class="page-stat-card">
          <span class="page-stat-card__label">水费规则</span>
          <p class="page-stat-card__value">{{ waterRuleCount }}</p>
          <p class="page-stat-card__hint">可配置异常阈值和阶梯价</p>
        </article>
        <article class="page-stat-card">
          <span class="page-stat-card__label">阈值规则</span>
          <p class="page-stat-card__value">{{ thresholdRuleCount }}</p>
          <p class="page-stat-card__hint">用于识别已配置异常水量阈值的水费规则</p>
        </article>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="规则台账" description="保留现有 communityId 查询方式，在同页完成规则检索、结果复核和分段水价查看。">
        <div class="page-grid">
          <div class="workspace-block">
            <div class="workspace-block__header">
              <div>
                <h3 class="workspace-block__title">检索范围</h3>
                <p class="workspace-block__description">先锁定小区，再在右侧维护新增规则，保持“查询台账 + 配置录入”同屏协作。</p>
                </div>
                <div class="layout-tag">规则查询</div>
              </div>
              <div class="filter-form">
                <el-input-number v-model="communityId" :min="1" controls-position="right" style="width: 180px" />
                <el-button type="primary" @click="loadData">查询规则</el-button>
              </div>
          </div>

          <div class="workspace-block">
            <div class="workspace-block__header">
              <div>
                <h3 class="workspace-block__title">配置概览</h3>
                <p class="workspace-block__description">快速判断当前小区已覆盖哪些收费策略，便于在新增前先完成现状复核。</p>
              </div>
            </div>
            <el-space wrap>
              <span class="layout-tag">物业费 {{ propertyRuleCount }} 条</span>
              <span class="layout-tag">水费 {{ waterRuleCount }} 条</span>
              <span class="layout-tag">阶梯价 {{ tieredRuleCount }} 条</span>
              <span class="layout-tag">阈值 {{ thresholdRuleCount }} 条</span>
            </el-space>
          </div>
        </div>

        <div>
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

      <PageSection title="新增规则" description="沿用现有 FeeRuleCreateDTO 字段与提交逻辑，只优化正式配置台的录入层级和提示方式。">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="page-grid">
            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">录入要求</h3>
                  <p class="workspace-block__description">提交前请先确认生效时间、费种和计价方式，避免同小区规则维护时出现误配。</p>
                </div>
              </div>
              <div class="metric-strip metric-strip--compact">
                <article class="metric-strip__item">
                  <span class="metric-strip__label">生效控制</span>
                  <strong class="metric-strip__value">按日期生效</strong>
                  <p class="metric-strip__hint">开始日期必填，结束日期为空则表示长期有效</p>
                </article>
                <article class="metric-strip__item">
                  <span class="metric-strip__label">水费扩展</span>
                  <strong class="metric-strip__value">阈值 + 阶梯价</strong>
                  <p class="metric-strip__hint">水费规则可叠加异常阈值和分段水价配置</p>
                </article>
              </div>
            </div>

            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">基础配置</h3>
                  <p class="workspace-block__description">先确定小区、费种、周期与生效范围，再继续录入价格和扩展参数。</p>
                </div>
                <div class="layout-tag">基础字段</div>
              </div>
              <div class="form-card-grid">
                <el-form-item label="小区 ID" prop="communityId">
                  <el-input-number v-model="form.communityId" :min="1" controls-position="right" style="width: 100%" />
                </el-form-item>
                <el-form-item label="费用类型" prop="feeType">
                  <el-select v-model="form.feeType" style="width: 100%">
                    <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="单价" prop="unitPrice">
                  <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
                  <div class="form-helper-text">{{ form.feeType === 'WATER' ? '水费规则需保留基础单价，启用阶梯价后再按分段单价计费。' : '物业费按固定单价维护，供后续账单生成直接引用。' }}</div>
                </el-form-item>
                <el-form-item label="周期类型" prop="cycleType">
                  <el-select v-model="form.cycleType" style="width: 100%">
                    <el-option v-for="option in cycleTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="计价方式" prop="pricingMode">
                  <el-select v-model="form.pricingMode" :disabled="form.feeType !== 'WATER'" style="width: 100%">
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
            </div>

            <template v-if="form.feeType === 'WATER'">
              <div class="workspace-block">
                <div class="workspace-block__header">
                  <div>
                    <h3 class="workspace-block__title">水费扩展配置</h3>
                    <p class="workspace-block__description">异常阈值用于识别风险读数，阶梯价用于精细化配置不同用量区间的单价。</p>
                  </div>
                  <div class="layout-tag">水费专属</div>
                </div>
                <div class="form-card-grid">
                  <el-form-item label="异常绝对阈值（吨）">
                    <el-input-number v-model="form.abnormalAbsThreshold" :min="0" :precision="3" :step="1" controls-position="right" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="异常倍数阈值">
                    <el-input-number v-model="form.abnormalMultiplierThreshold" :min="0" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
                  </el-form-item>
                </div>
              </div>

              <div v-if="form.pricingMode === 'TIERED'" class="workspace-block">
                <div class="workspace-block__header">
                  <div>
                    <h3 class="workspace-block__title">阶梯水价</h3>
                    <p class="workspace-block__description">逐段维护用量区间和单价，最后一档可不填结束用量表示“以上”，便于后续计费解释和追溯。</p>
                  </div>
                  <el-button type="primary" plain @click="addTier">新增阶梯</el-button>
                </div>
                <div class="page-grid">
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

            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">补充说明</h3>
                  <p class="workspace-block__description">用于记录运营、财务或后续规则调整需要同步的信息，方便正式环境追踪。</p>
                </div>
              </div>
              <el-form-item label="备注" style="margin-bottom: 0">
                <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="100" show-word-limit />
              </el-form-item>
            </div>

            <div class="workspace-action-bar">
              <el-space wrap>
                <span class="layout-tag">保存后自动刷新列表</span>
                <span class="layout-tag">提交字段与后端保持一致</span>
              </el-space>
              <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存规则</el-button>
            </div>
          </div>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
