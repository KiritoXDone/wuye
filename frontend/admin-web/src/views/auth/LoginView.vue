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
      ElMessage.warning('当前账号需尽快完成密码更新，请在登录后前往账户设置处理。')
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
      <span class="login-hero__eyebrow">PROPERTY OPERATIONS CONSOLE</span>
      <h1 class="login-hero__title">面向收费、财务与运营团队的物业运营管理平台</h1>
      <p class="login-hero__description">
        在同一控制台完成账单查询、费率配置、抄表协同、经营监控与审计追踪，帮助管理团队以统一台账处理日常收费与配置事项。
      </p>
      <div class="login-highlight-grid">
        <article class="login-highlight-card">
          <span class="login-highlight-card__label">统一台账</span>
          <strong class="login-highlight-card__value">账单、规则、抄表一体协同</strong>
          <p class="login-highlight-card__text">按同一业务口径查看收费状态、配置结果与执行节点，减少跨页面切换成本。</p>
        </article>
        <article class="login-highlight-card">
          <span class="login-highlight-card__label">角色适配</span>
          <strong class="login-highlight-card__value">支持管理、财务、运营值班</strong>
          <p class="login-highlight-card__text">首页概览、账单复核与配置维护均围绕高频后台工作场景组织。</p>
        </article>
        <article class="login-highlight-card">
          <span class="login-highlight-card__label">作业重点</span>
          <strong class="login-highlight-card__value">收费闭环与配置治理</strong>
          <p class="login-highlight-card__text">聚焦账期经营结果、异常追踪和规则维护，适合正式运营值班使用。</p>
        </article>
      </div>
    </section>

    <section class="login-card-wrap">
      <div class="panel-card login-card">
        <div class="login-card__meta">平台登录</div>
        <h2 class="login-card__title">进入运营控制台</h2>
        <p class="login-card__subtitle">请输入管理员账号信息以进入物业运营管理平台。</p>
        <div class="login-card__notice">
          <span class="login-card__notice-label">默认账号</span>
          <span class="login-card__notice-value">admin</span>
          <span class="login-card__notice-divider">/</span>
          <span class="login-card__notice-value">123456</span>
        </div>
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
