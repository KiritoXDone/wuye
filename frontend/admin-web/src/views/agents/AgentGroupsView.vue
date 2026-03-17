<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

import { createAgentGroup, getAgentGroups } from '@/api/agent-groups'
import AsyncState from '@/components/common/AsyncState.vue'
import PageSection from '@/components/common/PageSection.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { enabledStatusOptions, permissionOptions } from '@/constants/options'
import type { AgentGroup } from '@/types/agent-group'

const loading = ref(false)
const error = ref('')
const submitLoading = ref(false)
const list = ref<AgentGroup[]>([])
const formRef = ref<FormInstance>()

const form = reactive({
  agentCode: 'AGENT-A',
  groupCode: 'G-COMM001-1-2',
  permission: 'VIEW',
  status: 1,
})

const rules: FormRules<typeof form> = {
  agentCode: [{ required: true, message: '请输入 Agent 编码', trigger: 'blur' }],
  groupCode: [{ required: true, message: '请输入用户组编码', trigger: 'blur' }],
  permission: [{ required: true, message: '请选择权限', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getAgentGroups()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Agent 授权列表加载失败'
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  submitLoading.value = true
  try {
    await createAgentGroup(form)
    ElMessage.success('Agent 授权保存成功')
    await loadData()
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
        <h1 class="page-title">Agent 授权</h1>
        <p class="page-description">维护 Agent 与用户组的授权关系，当前列表接口返回组维度信息，表单用于最小新增授权。</p>
      </div>
    </div>

    <div class="two-column-grid">
      <PageSection title="授权列表" description="按当前后端返回字段展示用户组与权限，用于核对 Agent 数据范围配置。">
        <AsyncState :loading="loading" :error="error" :empty="!list.length" empty-description="暂无授权记录">
          <el-table :data="list" stripe>
            <el-table-column prop="groupId" label="组ID" width="88" />
            <el-table-column prop="groupCode" label="组编码" min-width="160" />
            <el-table-column prop="groupName" label="组名称" min-width="160" />
            <el-table-column label="权限" width="100">
              <template #default="scope">
                <StatusTag :value="scope.row.permission" />
              </template>
            </el-table-column>
          </el-table>
        </AsyncState>
      </PageSection>

      <PageSection title="新增授权" description="保持与 AgentGroupAssignDTO 一致，只提交 agentCode、groupCode、permission、status。">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-card-grid">
            <el-form-item label="Agent 编码" prop="agentCode">
              <el-input v-model="form.agentCode" />
            </el-form-item>
            <el-form-item label="用户组编码" prop="groupCode">
              <el-input v-model="form.groupCode" />
            </el-form-item>
            <el-form-item label="权限" prop="permission">
              <el-select v-model="form.permission">
                <el-option v-for="option in permissionOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status">
                <el-option v-for="option in enabledStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
          </div>
          <el-button type="primary" :loading="submitLoading" @click="handleCreate">保存授权</el-button>
        </el-form>
      </PageSection>
    </div>
  </div>
</template>
