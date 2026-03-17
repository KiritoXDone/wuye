<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { invoiceStatusOptions } from '@/constants/options'
import { processInvoiceApplication } from '@/api/invoices'
import type { InvoiceApplication } from '@/types/invoice'
import { formatDateTime } from '@/utils/format'

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const result = ref<InvoiceApplication | null>(null)
const processedHistory = ref<InvoiceApplication[]>([])

const form = reactive({
  applicationId: undefined as number | undefined,
  status: 'APPROVED',
  remark: '已开具电子发票',
})

const rules: FormRules<typeof form> = {
  applicationId: [{ required: true, message: '请输入发票申请 ID', trigger: 'blur' }],
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
}

async function handleProcess() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  submitLoading.value = true
  try {
    result.value = await processInvoiceApplication(form.applicationId, {
      status: form.status,
      remark: form.remark || undefined,
    })
    processedHistory.value = [
      result.value,
      ...processedHistory.value.filter((item) => item.id !== result.value?.id),
    ]
    ElMessage.success('发票申请处理成功')
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <h1 class="page-title">发票申请</h1>
        <p class="page-description">当前后端已开放住户端申请列表与后台处理接口，但未提供后台申请列表查询。本页保留最小处理入口，并明确说明该限制。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="处理申请" description="输入申请 ID 后，可将状态更新为已通过或已驳回，并写入备注。">
        <el-alert title="请先从住户侧申请记录或联调结果中拿到 applicationId，再在此页执行处理。" type="info" :closable="false" show-icon style="margin-bottom: 16px" />
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="申请 ID" prop="applicationId">
              <el-input-number v-model="form.applicationId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="处理状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="option in invoiceStatusOptions.filter((item) => item.value !== 'APPLIED')" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="处理备注" style="margin-top: 16px">
            <el-input v-model="form.remark" type="textarea" :rows="4" maxlength="120" show-word-limit />
          </el-form-item>
          <div class="inline-actions">
            <el-button type="primary" :loading="submitLoading" @click="handleProcess">提交处理</el-button>
          </div>
        </el-form>
      </PageSection>

      <PageSection title="接口边界说明" description="当前页面严格对齐现有后端能力，不额外虚构后台列表或详情查询接口。">
        <div class="info-list">
          <div class="info-row">
            <span class="info-key">住户侧列表</span>
            <span class="info-value">GET /api/v1/me/invoices/applications</span>
          </div>
          <div class="info-row">
            <span class="info-key">后台处理</span>
            <span class="info-value">POST /api/v1/admin/invoices/applications/{applicationId}/process</span>
          </div>
          <div class="info-row">
            <span class="info-key">电子凭证入口</span>
            <span class="info-value">住户支付结果页 / 已支付账单详情</span>
          </div>
        </div>
      </PageSection>
    </div>

    <PageSection title="处理结果" description="处理成功后展示后端返回的最新申请状态，便于联调确认。">
      <div v-if="result" class="drawer-field-list">
        <div class="drawer-field">
          <div class="drawer-field__label">申请单号</div>
          <div class="drawer-field__value">{{ result.applicationNo }}</div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">状态</div>
          <div class="drawer-field__value"><StatusTag :value="result.status" /></div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">账单ID</div>
          <div class="drawer-field__value">{{ result.billId }}</div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">支付单号</div>
          <div class="drawer-field__value">{{ result.payOrderNo }}</div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">抬头</div>
          <div class="drawer-field__value">{{ result.invoiceTitle }}</div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">备注</div>
          <div class="drawer-field__value">{{ result.remark || '--' }}</div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">申请时间</div>
          <div class="drawer-field__value">{{ formatDateTime(result.appliedAt) }}</div>
        </div>
        <div class="drawer-field">
          <div class="drawer-field__label">处理时间</div>
          <div class="drawer-field__value">{{ formatDateTime(result.processedAt) }}</div>
        </div>
      </div>
      <el-empty v-else description="提交处理后可在这里查看返回结果" />
    </PageSection>

    <PageSection title="本次会话已处理记录" description="仅展示当前页面会话内成功处理的记录，便于联调回看；刷新页面后会清空。">
      <el-empty v-if="!processedHistory.length" description="当前会话尚未处理任何申请" />
      <el-table v-else :data="processedHistory" stripe>
        <el-table-column prop="applicationNo" label="申请单号" min-width="180" />
        <el-table-column prop="billId" label="账单ID" width="100" />
        <el-table-column prop="payOrderNo" label="支付单号" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <StatusTag :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="invoiceTitle" label="发票抬头" min-width="140" />
        <el-table-column label="处理时间" min-width="160">
          <template #default="scope">{{ formatDateTime(scope.row.processedAt) }}</template>
        </el-table-column>
      </el-table>
    </PageSection>
  </div>
</template>
