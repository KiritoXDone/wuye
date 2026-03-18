<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { getAiRuntimeConfig, updateAiRuntimeConfig } from '@/api/ai-runtime-config'
import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import type { AiRuntimeConfig } from '@/types/ai-runtime-config'

const loading = ref(false)
const submitLoading = ref(false)
const error = ref('')
const formRef = ref<FormInstance>()
const current = ref<AiRuntimeConfig | null>(null)

const form = reactive({
  enabled: false,
  apiBaseUrl: 'https://api.openai.com/v1',
  provider: 'openai',
  model: 'gpt-4o-mini',
  apiKey: '',
  timeoutMs: 30000,
  maxTokens: 4096,
  temperature: 0.2,
})

const rules: FormRules<typeof form> = {
  apiBaseUrl: [{ required: true, message: '请输入 API 源地址', trigger: 'blur' }],
  provider: [{ required: true, message: '请输入 Provider', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  timeoutMs: [{ required: true, message: '请输入超时时间', trigger: 'blur' }],
  maxTokens: [{ required: true, message: '请输入最大 Token', trigger: 'blur' }],
  temperature: [{ required: true, message: '请输入温度参数', trigger: 'blur' }],
}

function patchForm(config: AiRuntimeConfig) {
  form.enabled = config.enabled
  form.apiBaseUrl = config.apiBaseUrl
  form.provider = config.provider
  form.model = config.model
  form.apiKey = ''
  form.timeoutMs = config.timeoutMs
  form.maxTokens = config.maxTokens
  form.temperature = config.temperature
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    current.value = await getAiRuntimeConfig()
    patchForm(current.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Agent 运行配置加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  submitLoading.value = true
  try {
    current.value = await updateAiRuntimeConfig({
      enabled: form.enabled,
      apiBaseUrl: form.apiBaseUrl,
      provider: form.provider,
      model: form.model,
      apiKey: form.apiKey || undefined,
      timeoutMs: form.timeoutMs,
      maxTokens: form.maxTokens,
      temperature: form.temperature,
    })
    patchForm(current.value)
    ElMessage.success('Agent 运行配置保存成功')
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
        <h1 class="page-title">Agent 运行配置</h1>
        <p class="page-description">统一配置 Agent 使用的 API 源、Provider 和模型，供后台 Agent 能力统一读取。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="当前 Agent 配置" description="后端返回的密钥只显示脱敏结果；如需更新，请在右侧输入新密钥覆盖。">
        <AsyncState :loading="loading" :error="error" :empty="!current" empty-description="暂无 Agent 运行配置">
          <div v-if="current" class="drawer-field-list">
            <div class="drawer-field">
              <div class="drawer-field__label">启用状态</div>
              <div class="drawer-field__value">{{ current.enabled ? '已启用' : '未启用' }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">API 源</div>
              <div class="drawer-field__value">{{ current.apiBaseUrl }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">Provider</div>
              <div class="drawer-field__value">{{ current.provider }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">模型</div>
              <div class="drawer-field__value">{{ current.model }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">已存密钥</div>
              <div class="drawer-field__value">{{ current.apiKeyMasked || '--' }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">超时（ms）</div>
              <div class="drawer-field__value">{{ current.timeoutMs }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">最大 Tokens</div>
              <div class="drawer-field__value">{{ current.maxTokens }}</div>
            </div>
            <div class="drawer-field">
              <div class="drawer-field__label">Temperature</div>
              <div class="drawer-field__value">{{ current.temperature }}</div>
            </div>
          </div>
        </AsyncState>
      </PageSection>

      <PageSection title="更新 Agent 配置" description="修改后会写入后端单例配置；留空 API Key 时保留后端已有密钥。">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="启用" prop="enabled">
              <el-switch v-model="form.enabled" />
            </el-form-item>
            <el-form-item label="API 源" prop="apiBaseUrl">
              <el-input v-model="form.apiBaseUrl" placeholder="https://api.openai.com/v1" />
            </el-form-item>
            <el-form-item label="Provider" prop="provider">
              <el-input v-model="form.provider" placeholder="openai / openai-compatible / anthropic" />
            </el-form-item>
            <el-form-item label="模型" prop="model">
              <el-input v-model="form.model" placeholder="gpt-4o-mini" />
            </el-form-item>
            <el-form-item label="新 API Key（可选）">
              <el-input v-model="form.apiKey" type="password" show-password placeholder="不填则保留后端已存密钥" />
            </el-form-item>
            <el-form-item label="超时（ms）" prop="timeoutMs">
              <el-input-number v-model="form.timeoutMs" :min="1000" :max="120000" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="最大 Tokens" prop="maxTokens">
              <el-input-number v-model="form.maxTokens" :min="1" :max="32768" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="Temperature" prop="temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </div>
          <div class="inline-actions">
            <el-button type="primary" :loading="submitLoading" @click="handleSave">保存配置</el-button>
            <el-button @click="loadData">重新加载</el-button>
          </div>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
