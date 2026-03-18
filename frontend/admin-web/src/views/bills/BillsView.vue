<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getAdminBills, getBillDetail } from '@/api/bills'
import { billStatusOptions, feeTypeOptions } from '@/constants/options'
import type { BillDetail, BillListItem } from '@/types/bill'
import { formatDate, formatMoney } from '@/utils/format'

const loading = ref(false)
const error = ref('')
const detailLoading = ref(false)
const drawerVisible = ref(false)
const list = ref<BillListItem[]>([])
const total = ref(0)
const detail = ref<BillDetail | null>(null)

const filters = reactive({
  periodYear: undefined as number | undefined,
  periodMonth: undefined as number | undefined,
  feeType: '',
  status: '',
  pageNo: 1,
  pageSize: 10,
})

const currentPageCount = computed(() => list.value.length)
const totalDueAmount = computed(() => list.value.reduce((sum, item) => sum + Number(item.amountDue || 0), 0))
const totalPaidAmount = computed(() => list.value.reduce((sum, item) => sum + Number(item.amountPaid || 0), 0))
const outstandingAmount = computed(() => Math.max(totalDueAmount.value - totalPaidAmount.value, 0))
const activeFilterCount = computed(() => {
  let count = 0
  if (filters.periodYear) count += 1
  if (filters.periodMonth) count += 1
  if (filters.feeType) count += 1
  if (filters.status) count += 1
  return count
})

const summaryCards = computed(() => {
  const paidItems = list.value.filter((item) => item.status === 'PAID')
  const issuedItems = list.value.filter((item) => item.status === 'ISSUED')

  return [
    {
      label: '查询结果总数',
      value: String(total.value),
      hint: `第 ${filters.pageNo} 页 · 每页 ${filters.pageSize} 条`,
    },
    {
      label: '当前页已支付',
      value: String(paidItems.length),
      hint: '仅统计当前页内已支付账单',
    },
    {
      label: '当前页待支付',
      value: String(issuedItems.length),
      hint: '当前页已出账未支付账单数',
    },
    {
      label: '当前页待核对余额',
      value: formatMoney(outstandingAmount.value),
      hint: `${formatMoney(totalDueAmount.value)} 应收 / ${formatMoney(totalPaidAmount.value)} 已收`,
    },
  ]
})

const availableCouponCount = computed(() => detail.value?.availableCoupons?.length ?? 0)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getAdminBills(filters)
    list.value = result.list
    total.value = result.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '账单列表加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(billId: number) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getBillDetail(billId)
  } finally {
    detailLoading.value = false
  }
}

function handlePageChange(page: number) {
  filters.pageNo = page
  loadData()
}

function handleSizeChange(size: number) {
  filters.pageSize = size
  filters.pageNo = 1
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header__eyebrow">对账工作台</div>
      <div class="page-header__headline">
        <div>
          <h1 class="page-title">账单管理</h1>
          <p class="page-description">围绕账期、费种、状态组织正式账单台账，支持列表筛查、分页复核和右侧详情抽屉的连续对账流程。</p>
        </div>
        <div class="page-header__actions">
          <div class="layout-tag">{{ activeFilterCount ? `已启用 ${activeFilterCount} 项筛选` : '当前为全量检索' }}</div>
          <div class="layout-tag">右侧详情抽屉复核</div>
          <div class="layout-tag">收费 / 财务共用台账</div>
        </div>
      </div>
      <div class="page-stat-grid">
        <article v-for="card in summaryCards" :key="card.label" class="page-stat-card">
          <span class="page-stat-card__label">{{ card.label }}</span>
          <p class="page-stat-card__value amount-text">{{ card.value }}</p>
          <p class="page-stat-card__hint">{{ card.hint }}</p>
        </article>
      </div>
    </div>

    <PageSection title="查询条件" description="保留原有账期、费种、状态筛选能力，并按正式台账使用场景收敛为统一检索区。">
      <div class="page-grid">
        <div class="filter-grid">
          <el-form-item label="账期年份">
            <el-input-number v-model="filters.periodYear" :min="2020" :max="2100" controls-position="right" placeholder="年份" style="width: 100%" />
          </el-form-item>
          <el-form-item label="账期月份">
            <el-select v-model="filters.periodMonth" clearable placeholder="月份" style="width: 100%">
              <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
            </el-select>
          </el-form-item>
          <el-form-item label="费用类型">
            <el-select v-model="filters.feeType" clearable placeholder="费用类型" style="width: 100%">
              <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="账单状态">
            <el-select v-model="filters.status" clearable placeholder="状态" style="width: 100%">
              <el-option v-for="option in billStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
        </div>

        <div class="filter-panel__meta">
          <el-space wrap>
            <span class="layout-tag">当前页 {{ filters.pageNo }}</span>
            <span class="layout-tag">每页 {{ filters.pageSize }} 条</span>
            <span class="layout-tag">结果 {{ total }} 条</span>
            <span class="layout-tag">本页 {{ currentPageCount }} 条</span>
          </el-space>
          <div class="inline-actions">
            <el-button type="primary" @click="filters.pageNo = 1; loadData()">查询</el-button>
            <el-button @click="filters.periodYear = undefined; filters.periodMonth = undefined; filters.feeType = ''; filters.status = ''; filters.pageNo = 1; loadData()">重置</el-button>
          </div>
        </div>
      </div>
    </PageSection>

    <PageSection title="账单台账" description="列表聚焦编号、房间、账期、金额和状态，详情继续在右侧抽屉统一复核，不改变原有查看路径。">
      <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="当前条件下暂无账单">
        <div class="recon-grid">
          <div class="workspace-block">
            <div class="workspace-block__header">
              <div>
                <h3 class="workspace-block__title">台账范围</h3>
                <p class="workspace-block__description">当前结果基于已选条件返回，分页切换后仍可保持原有查询上下文。</p>
              </div>
            </div>
            <el-space wrap>
              <span class="layout-tag">账单结果 {{ total }} 条</span>
              <span class="layout-tag">当前页 {{ filters.pageNo }}</span>
              <span class="layout-tag">每页 {{ filters.pageSize }} 条</span>
            </el-space>
          </div>
          <div class="workspace-block">
            <div class="workspace-block__header">
              <div>
                <h3 class="workspace-block__title">复核重点</h3>
                <p class="workspace-block__description">金额列保持右对齐，详情抽屉继续保留账单行项目与可用券信息，适合收费与财务同屏核对。</p>
              </div>
            </div>
            <el-space wrap>
              <span class="layout-tag">金额右对齐</span>
              <span class="layout-tag">详情抽屉复核</span>
              <span class="layout-tag">支持分页切换</span>
            </el-space>
          </div>
        </div>

        <el-table :data="list" stripe>
          <el-table-column prop="billNo" label="账单编号" min-width="180" />
          <el-table-column prop="roomLabel" label="房间" min-width="140" />
          <el-table-column label="费用类型" width="120">
            <template #default="scope">
              <StatusTag :value="scope.row.feeType" />
            </template>
          </el-table-column>
          <el-table-column prop="period" label="账期" width="110" />
          <el-table-column label="应收金额" min-width="140" align="right">
            <template #default="scope">
              <span class="amount-text">{{ formatMoney(scope.row.amountDue) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已收金额" min-width="140" align="right">
            <template #default="scope">
              <span class="amount-text">{{ formatMoney(scope.row.amountPaid) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <StatusTag :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column label="到期日" width="120">
            <template #default="scope">{{ formatDate(scope.row.dueDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row.billId)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :current-page="filters.pageNo"
            :page-size="filters.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </AsyncState>
    </PageSection>

    <el-drawer v-model="drawerVisible" title="账单详情复核" size="760px" class="detail-drawer">
      <AsyncState :loading="detailLoading" :error="''" :empty="!detail" empty-description="暂无详情">
        <template v-if="detail">
          <div class="drawer-hero">
            <div>
              <div class="layout-tag">{{ detail.billNo }}</div>
              <h2 class="drawer-hero__title">{{ detail.roomLabel }}</h2>
              <p class="drawer-hero__subtitle">账期 {{ detail.periodYear }}-{{ String(detail.periodMonth).padStart(2, '0') }} · 到期日 {{ formatDate(detail.dueDate) }} · 保留原有详情字段用于逐笔复核</p>
            </div>
            <div class="page-header__actions">
              <StatusTag :value="detail.feeType" />
              <StatusTag :value="detail.status" />
            </div>
          </div>

          <div class="drawer-kpi-grid">
            <article class="drawer-kpi-card">
              <p class="drawer-kpi-card__label">应收金额</p>
              <p class="drawer-kpi-card__value amount-text">{{ formatMoney(detail.amountDue) }}</p>
            </article>
            <article class="drawer-kpi-card">
              <p class="drawer-kpi-card__label">已收金额</p>
              <p class="drawer-kpi-card__value amount-text">{{ formatMoney(detail.amountPaid) }}</p>
            </article>
            <article class="drawer-kpi-card">
              <p class="drawer-kpi-card__label">账单行项目</p>
              <p class="drawer-kpi-card__value">{{ detail.billLines.length }}</p>
            </article>
            <article class="drawer-kpi-card">
              <p class="drawer-kpi-card__label">可用券数量</p>
              <p class="drawer-kpi-card__value">{{ availableCouponCount }}</p>
            </article>
          </div>

          <div class="drawer-field-list">
            <div class="drawer-field">
              <div class="drawer-field__label">账单编号</div>
              <div class="drawer-field__value">{{ detail.billNo }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">房间</div>
              <div class="drawer-field__value">{{ detail.roomLabel }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">费用类型</div>
              <div class="drawer-field__value"><StatusTag :value="detail.feeType" /></div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">状态</div>
              <div class="drawer-field__value"><StatusTag :value="detail.status" /></div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">应收金额</div>
              <div class="drawer-field__value amount-text">{{ formatMoney(detail.amountDue) }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">已收金额</div>
              <div class="drawer-field__value amount-text">{{ formatMoney(detail.amountPaid) }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">账期</div>
              <div class="drawer-field__value">{{ detail.periodYear }}-{{ String(detail.periodMonth).padStart(2, '0') }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">可用券</div>
              <div class="drawer-field__value">{{ availableCouponCount }} 张</div>
            </div>
          </div>

          <div class="drawer-section">
            <div class="drawer-section__header">
              <div>
                <h3 class="drawer-section__title">账单行项目</h3>
                <p class="workspace-block__description">保留原有字段与顺序，便于核对数量、单价和行项目金额是否一致。</p>
              </div>
            </div>
            <el-table :data="detail.billLines" stripe>
              <el-table-column prop="lineNo" label="序号" width="80" />
              <el-table-column prop="itemName" label="项目" min-width="180" />
              <el-table-column prop="quantity" label="数量" min-width="120" align="right" />
              <el-table-column prop="unitPrice" label="单价" min-width="120" align="right" />
              <el-table-column prop="lineAmount" label="金额" min-width="140" align="right">
                <template #default="scope">{{ formatMoney(scope.row.lineAmount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </AsyncState>
    </el-drawer>
  </div>
</template>
