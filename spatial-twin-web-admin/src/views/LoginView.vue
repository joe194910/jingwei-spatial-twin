<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cuboid, LockKeyhole, UserRound } from '@lucide/vue'
import { ElMessage } from 'element-plus'

import { ApiError } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const username = ref('admin')
const password = ref('')
const submitting = ref(false)

async function submitLogin() {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  submitting.value = true
  try {
    await authStore.login(username.value, password.value)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-visual">
      <img src="/assets/demo-campus.png" alt="经纬空间示范园区" />
      <div class="brand-copy">
        <div class="brand-line"><Cuboid :size="26" /><span>经纬空间</span></div>
        <h1>Jingwei Spatial Twin</h1>
        <p>让空间连接真实，让管理变得直观。</p>
        <small>标注驱动 · 资源分级 · 按需加载</small>
      </div>
    </section>
    <section class="login-panel">
      <div class="login-form">
        <span class="eyebrow">管理控制台</span>
        <h2>欢迎登录</h2>
        <p>使用已授权账号进入数字孪生管理平台。</p>
        <el-form label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="用户名">
            <el-input v-model="username" size="large" autocomplete="username" placeholder="请输入用户名">
              <template #prefix><UserRound :size="17" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="password"
              size="large"
              type="password"
              autocomplete="current-password"
              show-password
              placeholder="请输入密码"
            >
              <template #prefix><LockKeyhole :size="17" /></template>
            </el-input>
          </el-form-item>
          <div class="login-options">
            <span>登录状态仅保留在当前浏览器会话</span>
            <el-button link type="primary" @click="ElMessage.info('请联系管理员重置密码。')">忘记密码</el-button>
          </div>
          <el-button
            class="login-button"
            type="primary"
            size="large"
            native-type="submit"
            :loading="submitting"
          >
            安全登录
          </el-button>
        </el-form>
        <div class="security-note">自助注册默认关闭。登录失败锁定、Sa-Token 会话和安全审计已由后端统一处理。</div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 1.2fr) minmax(420px, 0.8fr);
  background: #f5f7f7;
}

.login-visual {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #fff;
}

.login-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-visual::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(20deg, rgb(12 41 49 / 88%), rgb(24 77 78 / 22%));
  content: '';
}

.brand-copy {
  position: absolute;
  z-index: 2;
  right: 8%;
  bottom: 10%;
  left: 8%;
}

.brand-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 800;
}

.brand-copy h1 {
  max-width: 680px;
  margin: 14px 0 8px;
  font-size: 54px;
  line-height: 1.08;
}

.brand-copy p {
  margin: 0 0 8px;
  font-size: 18px;
}

.brand-copy small {
  color: #b8d9d9;
}

.login-panel {
  display: grid;
  min-height: 100vh;
  padding: 40px;
  place-items: center;
}

.login-form {
  width: min(410px, 100%);
}

.eyebrow {
  color: var(--jw-accent);
  font-size: 11px;
  font-weight: 800;
}

.login-form h2 {
  margin: 5px 0;
  font-size: 28px;
}

.login-form > p {
  margin: 0 0 28px;
  color: var(--jw-muted);
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 20px;
  color: var(--jw-muted);
  font-size: 12px;
}

.login-button {
  width: 100%;
}

.security-note {
  margin-top: 22px;
  padding: 12px;
  border-left: 3px solid #b77a42;
  background: #f8f1e8;
  color: #74583d;
  font-size: 12px;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-visual {
    min-height: 38vh;
  }

  .login-panel {
    min-height: 62vh;
    padding: 32px 20px;
  }

  .brand-copy h1 {
    font-size: 36px;
  }
}
</style>
