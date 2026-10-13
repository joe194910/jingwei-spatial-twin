<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft, MapPinned } from '@lucide/vue'

const router = useRouter()
</script>

<template>
  <main class="not-found">
    <MapPinned :size="42" />
    <span>404</span>
    <h1>没有找到这个空间页面</h1>
    <p>地址可能已变更，或对应模块尚未加入当前里程碑。</p>
    <el-button type="primary" size="large" @click="router.push('/')">
      <ArrowLeft :size="17" />
      返回工作台
    </el-button>
  </main>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  align-content: center;
  justify-items: center;
  padding: 24px;
  background: #eef3f3;
  color: var(--jw-text);
  text-align: center;
}

.not-found > svg {
  color: var(--jw-accent);
}

.not-found > span {
  margin-top: 12px;
  color: #789096;
  font-family: Consolas, monospace;
  font-size: 14px;
}

.not-found h1 {
  margin: 8px 0;
  font-size: 27px;
}

.not-found p {
  margin: 0 0 22px;
  color: var(--jw-muted);
}
</style>
