<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import { getOrgUnits } from '@/api/org-units'
import type { OrgUnit } from '@/types/org-unit'

const loading = ref(false)
const error = ref('')
const list = ref<OrgUnit[]>([])

const tenantCount = computed(() => new Set(list.value.map((item) => item.tenantCode).filter(Boolean)).size)
const rootCount = computed(() => list.value.filter((item) => !item.parentId).length)
const communityBoundCount = computed(() => list.value.filter((item) => !!item.communityId).length)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getOrgUnits()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '组织架构加载失败'
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
        <h1 class="page-title">组织架构</h1>
        <p class="page-description">展示当前租户下组织单元、父级关系与小区映射，便于核对 Agent 与催缴数据范围。</p>
      </div>
    </div>

    <PageSection title="组织单元列表" description="当前后端返回 tenantCode、orgCode、parentName 与 communityId。">
      <div class="kpi-grid" style="margin-bottom: 16px">
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">组织单元数</p>
          <p class="kpi-card__value">{{ list.length }}</p>
          <p class="kpi-card__hint">当前只读展示，不提供前端编辑入口。</p>
        </article>
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">租户数</p>
          <p class="kpi-card__value">{{ tenantCount }}</p>
          <p class="kpi-card__hint">按 tenantCode 去重统计。</p>
        </article>
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">根组织数</p>
          <p class="kpi-card__value">{{ rootCount }}</p>
          <p class="kpi-card__hint">未配置 parentId 的组织单元。</p>
        </article>
        <article class="panel-card kpi-card">
          <p class="kpi-card__label">绑定小区组织</p>
          <p class="kpi-card__value">{{ communityBoundCount }}</p>
          <p class="kpi-card__hint">已关联 communityId 的组织单元。</p>
        </article>
      </div>
      <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="暂无组织单元数据">
        <el-table :data="list" stripe>
          <el-table-column prop="id" label="组织ID" width="100" />
          <el-table-column prop="tenantCode" label="租户编码" min-width="160" />
          <el-table-column prop="orgCode" label="组织编码" min-width="160" />
          <el-table-column prop="name" label="组织名称" min-width="160" />
          <el-table-column label="上级组织" min-width="160">
            <template #default="scope">{{ scope.row.parentName || '--' }}</template>
          </el-table-column>
          <el-table-column label="小区ID" width="120">
            <template #default="scope">{{ scope.row.communityId || '--' }}</template>
          </el-table-column>
        </el-table>
      </AsyncState>
    </PageSection>
  </div>
</template>
