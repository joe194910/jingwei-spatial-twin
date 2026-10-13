import { createPinia } from 'pinia'

/**
 * 复用同一个 Pinia 实例，保证路由守卫和页面组件读取同一份登录状态。
 */
export const pinia = createPinia()
