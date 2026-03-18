<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

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

const normalizedSummary = computed(() => {
  const totalCount = Number(summary.value?.totalCount ?? 0)
  const paidCount = Number(summary.value?.paidCount ?? 0)
  const unpaidCount = Math.max(totalCount - paidCount, 0)
  const paidAmount = Number(summary.value?.paidAmount ?? 0)
  const unpaidAmount = Number(summary.value?.unpaidAmount ?? 0)
  const discountAmount = Number(summary.value?.discountAmount ?? 0)
  const payRate = Number(summary.value?.payRate ?? monthly.value?.payRate ?? 0)
  const averagePaidAmount = paidCount > 0 ? paidAmount / paidCount : 0

  return {
    totalCount,
    paidCount,
    unpaidCount,
    paidAmount,
    unpaidAmount,
    discountAmount,
    payRate,
    averagePaidAmount,
  }
})

const heroCards = computed(() => [
  {
    label: '账期实收',
    value: formatMoney(summary.value?.paidAmount),
    hint: '作为当前账期经营收入复核基准',
  },
  {
    label: '欠费余额',
    value: formatMoney(summary.value?.unpaidAmount),
    hint: `${normalizedSummary.value.unpaidCount} 个房间待继续跟进`,
  },
  {
    label: '收缴率',
    value: formatPercent(summary.value?.payRate),
    hint: `已缴 ${normalizedSummary.value.paidCount} / 总房间 ${normalizedSummary.value.totalCount}`,
  },
  {
    label: '优惠影响',
    value: formatMoney(summary.value?.discountAmount),
    hint: '用于核对抵扣对本月实收的影响',
  },
])

const operationSignals = computed(() => [
  {
    label: '未缴房间',
    value: `${normalizedSummary.value.unpaidCount} 间`,
    hint: '建议与催缴、账单明细联动跟进',
  },
  {
    label: '平均回款',
    value: formatMoney(normalizedSummary.value.averagePaidAmount),
    hint: '按本月已缴房间均值估算回款表现',
  },
  {
    label: '总房间基数',
    value: `${normalizedSummary.value.totalCount} 间`,
    hint: '首页、月报与后续统计统一按房间口径',
  },
])

const executiveActions = computed(() => [
  {
    title: '先核对账期经营面',
    text: `确认 ${formatPeriod(filters.periodYear, filters.periodMonth)} 的实收、欠费与优惠金额是否与预期一致。`,
  },
  {
    title: '再锁定未缴范围',
    text: `当前尚有 ${normalizedSummary.value.unpaidCount} 个房间未完成支付，建议联动账单管理逐笔复核。`,
  },
  {
    title: '最后回到配置与开单',
    text: '如月度表现异常，再检查费用规则、水表抄表和账单生成链路，避免后续账期继续放大偏差。',
  },
])

const progressPercentage = computed(() => {
  const payRate = Number(summary.value?.payRate ?? 0)
  if (!Number.isFinite(payRate)) {
    return 0
  }
  return Math.min(Math.max(payRate, 0), 100)
})

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
      <div class="page-header__eyebrow">经营驾驶舱</div>
      <div class="page-header__headline">
        <div>
          <h1 class="page-title">运营总览</h1>
          <p class="page-description">围绕当前账期统一查看实收、欠费、优惠与收缴率，让收费运营、财务复核和管理决策共用同一套经营视角。</p>
        </div>
        <div class="page-header__actions">
          <div class="layout-tag">本次查看账期 {{ formatPeriod(filters.periodYear, filters.periodMonth) }}</div>
          <div class="layout-tag">聚焦按房间统计口径</div>
          <div class="layout-tag">摘要与月报双接口复核</div>
        </div>
      </div>
      <div class="page-stat-grid">
        <article v-for="card in heroCards" :key="card.label" class="page-stat-card">
          <span class="page-stat-card__label">{{ card.label }}</span>
          <p class="page-stat-card__value amount-text">{{ card.value }}</p>
          <p class="page-stat-card__hint">{{ card.hint }}</p>
        </article>
      </div>
      <div class="page-header__actions">
        <el-input-number v-model="filters.periodYear" :min="2020" :max="2100" controls-position="right" />
        <el-select v-model="filters.periodMonth" style="width: 140px">
          <el-option v-for="month in 12" :key="month" :label="`${month} 月`" :value="month" />
        </el-select>
        <el-button type="primary" @click="loadData">更新视图</el-button>
      </div>
    </div>

    <AsyncState :loading="loading" :error="error" :empty="!summary || !monthly">
      <div class="kpi-grid">
        <article class="panel-card kpi-card" style="--kpi-accent: var(--color-primary)">
          <p class="kpi-card__label">已缴房间</p>
          <p class="kpi-card__value">{{ summary?.paidCount ?? 0 }}</p>
          <p class="kpi-card__hint">总房间 {{ summary?.totalCount ?? 0 }}，用于追踪本月收费覆盖面</p>
        </article>
        <article class="panel-card kpi-card" style="--kpi-accent: var(--color-success)">
          <p class="kpi-card__label">账期实收</p>
          <p class="kpi-card__value amount-text">{{ formatMoney(summary?.paidAmount) }}</p>
          <p class="kpi-card__hint">账期 {{ formatPeriod(summary?.periodYear, summary?.periodMonth) }} 的累计回款</p>
        </article>
        <article class="panel-card kpi-card" style="--kpi-accent: var(--color-warning)">
          <p class="kpi-card__label">待回收余额</p>
          <p class="kpi-card__value amount-text">{{ formatMoney(summary?.unpaidAmount) }}</p>
          <p class="kpi-card__hint">未完成支付账单汇总，适合作为催缴与对账入口</p>
        </article>
        <article class="panel-card kpi-card" style="--kpi-accent: var(--color-accent-violet)">
          <p class="kpi-card__label">优惠抵扣</p>
          <p class="kpi-card__value amount-text">{{ formatMoney(summary?.discountAmount) }}</p>
          <p class="kpi-card__hint">用于衡量本月优惠策略对回款表现的影响</p>
        </article>
      </div>

      <div class="two-column-grid">
        <PageSection title="经营复核" description="将首页摘要与月报结果并排核对，方便值班运营和财务在同一页面完成经营复盘。">
          <div class="page-grid">
            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">当前账期摘要</h3>
                  <p class="workspace-block__description">重点复核收缴率、实收与欠费金额，确保本页与月报摘要口径一致。</p>
                </div>
                <div class="layout-tag">{{ formatPeriod(monthly?.periodYear, monthly?.periodMonth) }}</div>
              </div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="账期">{{ formatPeriod(monthly?.periodYear, monthly?.periodMonth) }}</el-descriptions-item>
                <el-descriptions-item label="缴费率">{{ formatPercent(monthly?.payRate) }}</el-descriptions-item>
                <el-descriptions-item label="实收金额">{{ formatMoney(monthly?.paidAmount) }}</el-descriptions-item>
                <el-descriptions-item label="优惠金额">{{ formatMoney(monthly?.discountAmount) }}</el-descriptions-item>
                <el-descriptions-item label="欠费金额">{{ formatMoney(monthly?.unpaidAmount) }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">经营信号</h3>
                  <p class="workspace-block__description">把本月收费面最关键的三个信号集中展示，方便快速判断是否需要深入到具体台账。</p>
                </div>
              </div>
              <div class="metric-strip">
                <article v-for="signal in operationSignals" :key="signal.label" class="metric-strip__item">
                  <span class="metric-strip__label">{{ signal.label }}</span>
                  <strong class="metric-strip__value amount-text">{{ signal.value }}</strong>
                  <p class="metric-strip__hint">{{ signal.hint }}</p>
                </article>
              </div>
            </div>
          </div>
        </PageSection>

        <PageSection title="执行重点" description="把值班运营最常用的判断和动作整理为正式工作顺序，帮助从首页直达收费闭环。">
          <div class="page-grid">
            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">收缴进度</h3>
                  <p class="workspace-block__description">先看本月进度条，再决定是否需要进入账单、规则或抄表页面做进一步处理。</p>
                </div>
              </div>
              <el-progress :percentage="progressPercentage" :stroke-width="12" />
              <el-alert
                :title="`当前收缴率 ${formatPercent(summary?.payRate)}，未缴房间 ${normalizedSummary.unpaidCount} 间。`"
                type="info"
                show-icon
                :closable="false"
              />
            </div>

            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">建议执行顺序</h3>
                  <p class="workspace-block__description">按正式运营台节奏处理：先看经营结果，再核对台账，最后回到配置与开单源头。</p>
                </div>
              </div>
              <el-timeline>
                <el-timeline-item v-for="(action, index) in executiveActions" :key="action.title" :timestamp="`步骤 ${index + 1}`">
                  <strong>{{ action.title }}</strong>
                  <div>{{ action.text }}</div>
                </el-timeline-item>
              </el-timeline>
            </div>

            <div class="workspace-block">
              <div class="workspace-block__header">
                <div>
                  <h3 class="workspace-block__title">管理口径</h3>
                  <p class="workspace-block__description">首页判断以账期和房间口径为准，避免不同页面之间出现经营理解偏差。</p>
                </div>
              </div>
              <el-space wrap>
                <span class="layout-tag">账期筛选</span>
                <span class="layout-tag">房间统计口径</span>
                <span class="layout-tag">月报一致性</span>
                <span class="layout-tag">实时接口数据</span>
              </el-space>
            </div>
          </div>
        </PageSection>
      </div>
    </AsyncState>
  </div>
</template>
