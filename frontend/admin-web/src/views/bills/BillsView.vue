<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

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
      <div>
        <h1 class="page-title">账单管理</h1>
        <p class="page-description">按账期、费用类型与状态查询后台账单，并通过详情抽屉查看账单行项目。</p>
      </div>
    </div>

    <PageSection title="筛选条件" description="当前后端支持账期、费用类型、状态和分页查询。">
      <div class="filter-form">
        <el-input-number v-model="filters.periodYear" :min="2020" :max="2100" controls-position="right" placeholder="年份" />
        <el-select v-model="filters.periodMonth" clearable placeholder="月份" style="width: 140px">
          <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
        </el-select>
        <el-select v-model="filters.feeType" clearable placeholder="费用类型" style="width: 160px">
          <el-option v-for="option in feeTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="状态" style="width: 160px">
          <el-option v-for="option in billStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-button type="primary" @click="filters.pageNo = 1; loadData()">查询</el-button>
        <el-button @click="filters.periodYear = undefined; filters.periodMonth = undefined; filters.feeType = ''; filters.status = ''; filters.pageNo = 1; loadData()">重置</el-button>
      </div>
    </PageSection>

    <PageSection title="账单列表" description="金额列右对齐，状态和费种统一使用标签展示。">
      <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="当前条件下暂无账单">
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

        <div style="display: flex; justify-content: flex-end; margin-top: 16px">
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

    <el-drawer v-model="drawerVisible" title="账单详情" size="720px">
      <AsyncState :loading="detailLoading" :error="''" :empty="!detail" empty-description="暂无详情">
        <template v-if="detail">
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
          </div>

          <div style="margin-top: 24px">
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
