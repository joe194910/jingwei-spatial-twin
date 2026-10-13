<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { KeyRound, Plus, RefreshCw, Search } from '@lucide/vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import {
  createUser,
  deleteUser,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUser,
} from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/types/api'
import type { RoleItem, UserItem, UserSaveRequest } from '@/types/system'

// 用户页负责账号、状态、角色和密码重置；权限最终仍以后端校验结果为准。
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const resettingPassword = ref(false)
const loadError = ref('')
const keyword = ref('')
const users = ref<UserItem[]>([])
const roles = ref<RoleItem[]>([])
const formVisible = ref(false)
const passwordVisible = ref(false)
const editingUserId = ref<number | null>(null)
const resettingUser = ref<UserItem | null>(null)
const formRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

const userForm = reactive<UserSaveRequest>({
  username: '',
  nickname: '',
  status: 1,
  roleIds: [],
  password: '',
})

const passwordForm = reactive({
  password: '',
  confirmPassword: '',
})

const userRules: FormRules<UserSaveRequest> = {
  username: [
    { required: true, message: '请输入登录名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_.-]{2,63}$/,
      message: '登录名需以字母开头，仅包含字母、数字、点、短横线或下划线',
      trigger: 'blur',
    },
  ],
  nickname: [{ required: true, message: '请输入用户姓名', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个角色', trigger: 'change' }],
  password: [
    {
      validator: (_rule, value: string | undefined, callback) => {
        if (editingUserId.value === null && !value) {
          callback(new Error('创建用户时必须设置密码'))
          return
        }
        if (value && (value.length < 12 || value.length > 128)) {
          callback(new Error('密码长度需为12至128位'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const passwordRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 12, max: 128, message: '密码长度需为12至128位', trigger: 'blur' },
  ],
  confirmPassword: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) {
          callback(new Error('请再次输入新密码'))
          return
        }
        if (value !== passwordForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const enabledRoles = computed(() => roles.value.filter((role) => role.status === 1))

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function formatUtcTime(value: string | null) {
  if (!value) return '从未'
  const normalized = /Z$|[+-]\d{2}:\d{2}$/.test(value) ? value : `${value}Z`
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [userList, roleList] = await Promise.all([listUsers(keyword.value), listRoles()])
    users.value = userList
    roles.value = roleList
  } catch (error) {
    loadError.value = errorMessage(error, '用户数据加载失败')
  } finally {
    loading.value = false
  }
}

async function searchUsers() {
  loading.value = true
  loadError.value = ''
  try {
    users.value = await listUsers(keyword.value)
  } catch (error) {
    loadError.value = errorMessage(error, '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingUserId.value = null
  Object.assign(userForm, {
    username: '',
    nickname: '',
    status: 1,
    roleIds: [],
    password: '',
  })
  formVisible.value = true
}

function openEditDialog(user: UserItem) {
  editingUserId.value = user.id
  Object.assign(userForm, {
    username: user.username,
    nickname: user.nickname,
    status: user.status,
    roleIds: [...user.roleIds],
    password: '',
  })
  formVisible.value = true
}

async function submitUser() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload: UserSaveRequest = {
      username: userForm.username.trim(),
      nickname: userForm.nickname.trim(),
      status: userForm.status,
      roleIds: [...userForm.roleIds],
      password: editingUserId.value === null ? userForm.password : undefined,
    }
    if (editingUserId.value === null) {
      await createUser(payload)
      ElMessage.success('用户创建成功')
    } else {
      await updateUser(editingUserId.value, payload)
      ElMessage.success('用户修改成功')
    }
    formVisible.value = false
    await searchUsers()
  } catch (error) {
    ElMessage.error(errorMessage(error, '用户保存失败'))
  } finally {
    saving.value = false
  }
}

function openPasswordDialog(user: UserItem) {
  resettingUser.value = user
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

async function submitPasswordReset() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid || !resettingUser.value) return

  resettingPassword.value = true
  try {
    await resetUserPassword(resettingUser.value.id, passwordForm.password)
    ElMessage.success('密码重置成功，该用户原会话已失效')
    passwordVisible.value = false
  } catch (error) {
    ElMessage.error(errorMessage(error, '密码重置失败'))
  } finally {
    resettingPassword.value = false
  }
}

async function removeUser(user: UserItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除用户“${user.nickname}（${user.username}）”吗？删除后该账号将无法登录。`,
      '删除用户',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteUser(user.id)
    ElMessage.success('用户删除成功')
    await searchUsers()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '用户删除失败'))
  }
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="management-heading">
      <div>
        <span class="management-eyebrow">账号与授权</span>
        <h2>用户管理</h2>
        <p>维护登录账号、启用状态和多角色关联。权限变更后后端会注销该用户旧会话。</p>
      </div>
      <el-button
        v-if="authStore.hasPermission('system:user:add')"
        type="primary"
        @click="openCreateDialog"
      >
        <Plus :size="17" />
        新增用户
      </el-button>
    </header>

    <div class="management-toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索登录名或用户姓名"
        @keyup.enter="searchUsers"
        @clear="searchUsers"
      >
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-button :loading="loading" @click="searchUsers">
        <RefreshCw :size="16" />
        刷新
      </el-button>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      class="management-alert"
    >
      <template #default>
        <el-button link type="primary" @click="loadData">重新加载</el-button>
      </template>
    </el-alert>

    <div class="management-table">
      <el-table v-loading="loading" :data="users" empty-text="暂无用户数据">
        <el-table-column prop="username" label="登录名" min-width="150" />
        <el-table-column prop="nickname" label="用户姓名" min-width="150" />
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="roleName in row.roleNames" :key="roleName" effect="plain">
                {{ roleName }}
              </el-tag>
              <span v-if="row.roleNames.length === 0" class="muted-text">未分配</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="锁定状态" min-width="170">
          <template #default="{ row }">
            {{ row.lockedUntil ? `至 ${formatUtcTime(row.lockedUntil)}` : '未锁定' }}
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="180">
          <template #default="{ row }">{{ formatUtcTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right" align="right">
          <template #default="{ row }">
            <el-button
              v-if="authStore.hasPermission('system:user:edit')"
              link
              type="primary"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="authStore.hasPermission('system:user:edit')"
              link
              type="primary"
              @click="openPasswordDialog(row)"
            >
              <KeyRound :size="15" />
              重置密码
            </el-button>
            <el-button
              v-if="authStore.hasPermission('system:user:delete')"
              link
              type="danger"
              :disabled="row.id === authStore.user?.userId"
              @click="removeUser(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="editingUserId === null ? '新增用户' : '编辑用户'"
      width="min(560px, calc(100vw - 32px))"
      destroy-on-close
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="userForm" :rules="userRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="登录名" prop="username">
            <el-input v-model="userForm.username" maxlength="64" placeholder="例如 zhangsan" />
          </el-form-item>
          <el-form-item label="用户姓名" prop="nickname">
            <el-input v-model="userForm.nickname" maxlength="64" placeholder="请输入用户姓名" />
          </el-form-item>
        </div>
        <el-form-item label="账号状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple collapse-tags placeholder="请选择角色">
            <el-option
              v-for="role in enabledRoles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingUserId === null" label="初始密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            show-password
            maxlength="128"
            autocomplete="new-password"
            placeholder="至少12位，创建后请由用户及时修改"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordVisible"
      title="重置用户密码"
      width="min(480px, calc(100vw - 32px))"
      destroy-on-close
      @closed="passwordFormRef?.clearValidate()"
    >
      <el-alert
        :title="`正在重置 ${resettingUser?.nickname ?? ''}（${resettingUser?.username ?? ''}）的密码`"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form
        ref="passwordFormRef"
        class="dialog-form"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="passwordForm.password"
            type="password"
            show-password
            maxlength="128"
            autocomplete="new-password"
            placeholder="请输入12至128位新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            maxlength="128"
            autocomplete="new-password"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="resettingPassword" @click="submitPasswordReset">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>
