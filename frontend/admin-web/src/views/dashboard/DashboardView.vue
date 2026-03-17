<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import { getDashboardSummary, getMonthlyReport } from '@/api/dashboard'
import type { DashboardSummary, MonthlyReport } from '@/types/dashboard'
import { formatMoney, formatPercent, formatPeriod } from '@/utils/format'

const now = new Date()
const filters = reactive({
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
})

const loading = ref(false)
const error = ref('')
const summary = ref<DashboardSummary | null>(null)
const monthly = ref<MonthlyReport | null>(null)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [summaryResult, monthlyResult] = await Promise.all([
      getDashboardSummary(filters),
      getMonthlyReport(filters),
    ])
    summary.value = summaryResult
    monthly.value = monthlyResult
  } catch (err) {
    error.value = err instanceof Error ? err.message : '仪表盘加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">仪表盘</h1>
        <p class="page-description">查看本月缴费概览与全局月报，便于快速追踪欠费和实收情况。</p>
      </div>
      <div class="filter-form">
        <el-input-number v-model="filters.periodYear" :min="2020" :max="2100" controls-position="right" />
        <el-select v-model="filters.periodMonth" style="width: 140px">
          <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
        </el-select>
        <el-button type="primary" @click="loadData">刷新数据</el-button>
      </div>
    </div>

    <AsyncState :loading="loading" :error="error" :empty="!summary || !monthly">
      <div class="kpi-grid">
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">本月缴费户数</p>
          <p class="kpi-card__value">{{ summary?.paidCount ?? 0 }}</p>
          <p class="kpi-card__hint">总房间数 {{ summary?.totalCount ?? 0 }}</p>
        </article>
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">本月实收金额</p>
          <p class="kpi-card__value amount-text">{{ formatMoney(summary?.paidAmount) }}</p>
          <p class="kpi-card__hint">账期 {{ formatPeriod(summary?.periodYear, summary?.periodMonth) }}</p>
        </article>
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">欠费金额</p>
          <p class="kpi-card__value amount-text">{{ formatMoney(summary?.unpaidAmount) }}</p>
          <p class="kpi-card__hint">未完成支付的账单汇总</p>
        </article>
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">券抵扣金额</p>
          <p class="kpi-card__value amount-text">{{ formatMoney(summary?.discountAmount) }}</p>
          <p class="kpi-card__hint">当前后端月度统计值</p>
        </article>
      </div>

      <div class="two-column-grid">
        <PageSection title="月报摘要" description="与月报接口保持一致，便于核对首页与报表口径。">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="账期">{{ formatPeriod(monthly?.periodYear, monthly?.periodMonth) }}</el-descriptions-item>
            <el-descriptions-item label="缴费率">{{ formatPercent(monthly?.payRate) }}</el-descriptions-item>
            <el-descriptions-item label="实收金额">{{ formatMoney(monthly?.paidAmount) }}</el-descriptions-item>
            <el-descriptions-item label="优惠金额">{{ formatMoney(monthly?.discountAmount) }}</el-descriptions-item>
            <el-descriptions-item label="欠费金额">{{ formatMoney(monthly?.unpaidAmount) }}</el-descriptions-item>
          </el-descriptions>
        </PageSection>

        <PageSection title="使用提示" description="当前 MVP 仅接通后台已存在的核心管理接口。">
          <el-timeline>
            <el-timeline-item timestamp="步骤 1">登录后先确认本月账期摘要。</el-timeline-item>
            <el-timeline-item timestamp="步骤 2">在费用规则页配置物业费规则。</el-timeline-item>
            <el-timeline-item timestamp="步骤 3">在水表与抄表页录入水表与抄表数据。</el-timeline-item>
            <el-timeline-item timestamp="步骤 4">在账单生成页按月生成物业费或水费账单。</el-timeline-item>
          </el-timeline>
        </PageSection>
      </div>
    </AsyncState>
  </div>
</template>
