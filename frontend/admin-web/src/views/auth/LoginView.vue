<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  loading.value = true
  try {
    const result = await authStore.login(form)
    if (result.needResetPassword) {
      ElMessage.warning('当前账号提示需要重置密码，MVP 暂以提醒代替改密流程。')
    } else {
      ElMessage.success('登录成功')
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <section class="login-hero">
      <span class="login-hero__eyebrow">WUYE SYSTEM</span>
      <h1 class="login-hero__title">物业账单、抄表与规则配置的最小管理闭环</h1>
      <p class="login-hero__description">
        当前管理端 MVP 仅对接已实现的后台接口，覆盖登录、仪表盘、账单查询、费用规则、抄表与开单流程，方便直接联调和验收。
      </p>
      <el-space wrap>
        <el-tag type="primary" size="large">Vue 3 + TypeScript</el-tag>
        <el-tag size="large">Element Plus</el-tag>
        <el-tag size="large">Pinia + Router</el-tag>
      </el-space>
    </section>

    <section class="login-card-wrap">
      <div class="panel-card login-card">
        <h2 class="login-card__title">管理员登录</h2>
        <p class="login-card__subtitle">默认演示账号：admin / 123456</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
            登录系统
          </el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>
