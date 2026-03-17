<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { getAuditLogs } from '@/api/audit-logs'
import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import type { AuditLogItem, AuditLogListQuery } from '@/types/audit-log'
import { formatDateTime } from '@/utils/format'

const auditBizTypeOptions = [
  { label: '全部业务', value: '' },
  { label: '账单', value: 'BILL' },
  { label: '支付', value: 'PAYMENT' },
  { label: '券', value: 'COUPON' },
  { label: '登录鉴权', value: 'AUTH' },
  { label: '导入', value: 'IMPORT' },
  { label: '导出', value: 'EXPORT' },
]

const auditBizTypeLabelMap: Record<string, string> = {
  BILL: '账单',
  PAYMENT: '支付',
  COUPON: '券',
  AUTH: '登录鉴权',
  IMPORT: '导入',
  EXPORT: '导出',
}

const loading = ref(false)
const error = ref('')
const drawerVisible = ref(false)
const list = ref<AuditLogItem[]>([])
const total = ref(0)
const selectedLog = ref<AuditLogItem | null>(null)

const filters = reactive({
  bizType: '',
  bizId: '',
  operatorId: undefined as number | undefined,
  createdAtRange: [] as string[],
  pageNo: 1,
  pageSize: 10,
})

const formattedDetailJson = computed(() => {
  const detailJson = selectedLog.value?.detailJson
  if (!detailJson) {
    return '--'
  }
  try {
    return JSON.stringify(JSON.parse(detailJson), null, 2)
  } catch {
    return detailJson
  }
})

function buildQuery(): AuditLogListQuery {
  return {
    bizType: filters.bizType || undefined,
    bizId: filters.bizId || undefined,
    operatorId: filters.operatorId,
    createdAtStart: filters.createdAtRange[0] || undefined,
    createdAtEnd: filters.createdAtRange[1] || undefined,
    pageNo: filters.pageNo,
    pageSize: filters.pageSize,
  }
}

function getBizTypeLabel(value?: string) {
  if (!value) {
    return '--'
  }
  return auditBizTypeLabelMap[value] || value
}

function getBizTypeTagType(value?: string) {
  switch (value) {
    case 'BILL':
      return 'warning'
    case 'PAYMENT':
      return 'success'
    case 'COUPON':
      return 'primary'
    case 'AUTH':
      return 'info'
    case 'IMPORT':
    case 'EXPORT':
      return ''
    default:
      return 'info'
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const result = await getAuditLogs(buildQuery())
    list.value = result.list
    total.value = result.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '审计日志加载失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  filters.pageNo = 1
  loadData()
}

function handleReset() {
  filters.bizType = ''
  filters.bizId = ''
  filters.operatorId = undefined
  filters.createdAtRange = []
  filters.pageNo = 1
  loadData()
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

function openDetail(log: AuditLogItem) {
  selectedLog.value = log
  drawerVisible.value = true
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">审计日志</h1>
        <p class="page-description">按业务类型、业务主键、操作人和创建时间查询后台关键操作记录，并查看明细 JSON。</p>
      </div>
    </div>

    <PageSection title="筛选条件" description="当前仅提供最小查询能力，便于按业务线和操作人回溯后台关键动作。">
      <div class="filter-form">
        <el-select v-model="filters.bizType" placeholder="业务类型" style="width: 160px">
          <el-option v-for="option in auditBizTypeOptions" :key="option.value || 'ALL'" :label="option.label" :value="option.value" />
        </el-select>
        <el-input v-model="filters.bizId" clearable placeholder="业务主键" style="width: 180px" />
        <el-input-number v-model="filters.operatorId" :min="1" controls-position="right" placeholder="操作人 ID" />
        <el-date-picker
          v-model="filters.createdAtRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
        />
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </PageSection>

    <PageSection title="日志列表" description="列表聚焦最关键的定位字段，详情明细统一放入右侧抽屉，避免表格过宽。">
      <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="当前条件下暂无审计日志">
        <el-table :data="list" stripe>
          <el-table-column prop="id" label="审计ID" width="100" />
          <el-table-column label="业务类型" width="120">
            <template #default="scope">
              <el-tag :type="getBizTypeTagType(scope.row.bizType)" effect="light" round>
                {{ getBizTypeLabel(scope.row.bizType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizId" label="业务主键" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作动作" width="120">
            <template #default="scope">
              <StatusTag :value="scope.row.action" />
            </template>
          </el-table-column>
          <el-table-column label="操作人ID" width="120">
            <template #default="scope">{{ scope.row.operatorId ?? '--' }}</template>
          </el-table-column>
          <el-table-column label="来源IP" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.ip || '--' }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="180">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row)">查看明细</el-button>
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

    <el-drawer v-model="drawerVisible" title="审计明细" size="680px">
      <template v-if="selectedLog">
        <div class="drawer-field-list">
          <div class="drawer-field">
            <div class="drawer-field__label">审计ID</div>
            <div class="drawer-field__value">{{ selectedLog.id }}</div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">业务类型</div>
            <div class="drawer-field__value">{{ getBizTypeLabel(selectedLog.bizType) }}</div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">业务主键</div>
            <div class="drawer-field__value">{{ selectedLog.bizId }}</div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">操作动作</div>
            <div class="drawer-field__value"><StatusTag :value="selectedLog.action" /></div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">操作人ID</div>
            <div class="drawer-field__value">{{ selectedLog.operatorId ?? '--' }}</div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">来源IP</div>
            <div class="drawer-field__value">{{ selectedLog.ip || '--' }}</div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">User-Agent</div>
            <div class="drawer-field__value">{{ selectedLog.userAgent || '--' }}</div>
          </div>
          <div class="drawer-field">
            <div class="drawer-field__label">创建时间</div>
            <div class="drawer-field__value">{{ formatDateTime(selectedLog.createdAt) }}</div>
          </div>
        </div>

        <div class="audit-detail-block">
          <div class="audit-detail-block__header">
            <h3 class="panel-card__title">detailJson</h3>
            <span class="page-description">展示后端返回的明细快照，便于定位变更上下文。</span>
          </div>
          <pre class="audit-detail-json">{{ formattedDetailJson }}</pre>
        </div>
      </template>
      <el-empty v-else description="请选择一条日志查看详情" />
    </el-drawer>
  </div>
</template>

<style scoped>
.audit-detail-block {
  margin-top: var(--space-3);
}

.audit-detail-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.audit-detail-json {
  margin: 0;
  padding: var(--space-2);
  overflow: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-button);
  background: var(--color-surface-muted);
  color: var(--color-text);
  font: var(--font-caption);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .audit-detail-block__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
