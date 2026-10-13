<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw, ShieldCheck } from '@lucide/vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import {
  createRole,
  deleteRole,
  listMenus,
  listRoles,
  updateRole,
} from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/types/api'
import type { MenuItem, RoleItem, RoleSaveRequest } from '@/types/system'

interface MenuTreeInstance {
  getCheckedKeys: () => Array<string | number>
  setCheckedKeys: (keys: number[]) => void
}

// 角色页维护角色与菜单权限节点的关联，保存后由后端统一注销受影响用户会话。
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const roles = ref<RoleItem[]>([])
const menus = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const editingRoleId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const treeRef = ref<MenuTreeInstance | null>(null)

const roleForm = reactive<RoleSaveRequest>({
  code: '',
  name: '',
  status: 1,
  description: '',
  menuIds: [],
})

const roleRules: FormRules<RoleSaveRequest> = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{2,63}$/,
      message: '角色编码仅支持大写字母、数字和下划线',
      trigger: 'blur',
    },
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [roleList, menuTree] = await Promise.all([listRoles(), listMenus()])
    roles.value = roleList
    menus.value = menuTree
  } catch (error) {
    loadError.value = errorMessage(error, '角色与菜单数据加载失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingRoleId.value = null
  Object.assign(roleForm, {
    code: '',
    name: '',
    status: 1,
    description: '',
    menuIds: [],
  })
  dialogVisible.value = true
  void nextTick(() => treeRef.value?.setCheckedKeys([]))
}

function openEditDialog(role: RoleItem) {
  editingRoleId.value = role.id
  Object.assign(roleForm, {
    code: role.code,
    name: role.name,
    status: role.status,
    description: role.description ?? '',
    menuIds: [...role.menuIds],
  })
  dialogVisible.value = true
  void nextTick(() => treeRef.value?.setCheckedKeys([...role.menuIds]))
}

async function submitRole() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const checkedKeys = treeRef.value?.getCheckedKeys() ?? []
  roleForm.menuIds = checkedKeys.map(Number).filter(Number.isFinite)
  saving.value = true
  try {
    const payload: RoleSaveRequest = {
      code: roleForm.code.trim(),
      name: roleForm.name.trim(),
      status: roleForm.status,
      description: roleForm.description.trim(),
      menuIds: [...roleForm.menuIds],
    }
    if (editingRoleId.value === null) {
      await createRole(payload)
      ElMessage.success('角色创建成功')
    } else {
      await updateRole(editingRoleId.value, payload)
      ElMessage.success('角色修改成功，关联用户旧会话已失效')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(errorMessage(error, '角色保存失败'))
  } finally {
    saving.value = false
  }
}

async function removeRole(role: RoleItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除角色“${role.name}”吗？只有未关联用户的普通角色可以删除。`,
      '删除角色',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteRole(role.id)
    ElMessage.success('角色删除成功')
    await loadData()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '角色删除失败'))
  }
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="management-heading">
      <div>
        <span class="management-eyebrow">RBAC 权限</span>
        <h2>角色管理</h2>
        <p>通过菜单与按钮权限节点维护角色能力。授权变更会让关联用户重新登录后生效。</p>
      </div>
      <el-button
        v-if="authStore.hasPermission('system:role:add')"
        type="primary"
        @click="openCreateDialog"
      >
        <Plus :size="17" />
        新增角色
      </el-button>
    </header>

    <div class="management-toolbar toolbar-end">
      <el-button :loading="loading" @click="loadData">
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
      <el-table v-loading="loading" :data="roles" empty-text="暂无角色数据">
        <el-table-column label="角色" min-width="210">
          <template #default="{ row }">
            <div class="primary-cell">
              <span class="primary-cell-icon"><ShieldCheck :size="17" /></span>
              <span>
                <strong>{{ row.name }}</strong>
                <small>{{ row.code }}</small>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="240">
          <template #default="{ row }">{{ row.description || '暂无说明' }}</template>
        </el-table-column>
        <el-table-column label="权限节点" width="115">
          <template #default="{ row }">{{ row.menuIds.length }}</template>
        </el-table-column>
        <el-table-column prop="userCount" label="关联用户" width="115" />
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="right">
          <template #default="{ row }">
            <el-button
              v-if="authStore.hasPermission('system:role:edit')"
              link
              type="primary"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="authStore.hasPermission('system:role:delete')"
              link
              type="danger"
              :disabled="row.code === 'SUPER_ADMIN' || row.userCount > 0"
              @click="removeRole(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingRoleId === null ? '新增角色' : '编辑角色'"
      width="min(680px, calc(100vw - 32px))"
      destroy-on-close
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="roleForm" :rules="roleRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="角色编码" prop="code">
            <el-input
              v-model="roleForm.code"
              maxlength="64"
              :disabled="roleForm.code === 'SUPER_ADMIN'"
              placeholder="例如 PROJECT_EDITOR"
            />
          </el-form-item>
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="roleForm.name" maxlength="64" placeholder="请输入角色名称" />
          </el-form-item>
        </div>
        <el-form-item label="角色状态" prop="status">
          <el-radio-group v-model="roleForm.status" :disabled="roleForm.code === 'SUPER_ADMIN'">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色说明" prop="description">
          <el-input
            v-model="roleForm.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            placeholder="说明该角色的业务范围和使用边界"
          />
        </el-form-item>
        <el-form-item label="菜单与按钮权限">
          <div class="permission-tree">
            <el-tree
              ref="treeRef"
              :data="menus"
              node-key="id"
              show-checkbox
              check-strictly
              default-expand-all
              :props="{ label: 'name', children: 'children' }"
              empty-text="暂无可分配权限"
            >
              <template #default="{ data }">
                <span class="tree-node">
                  <span>{{ data.name }}</span>
                  <small v-if="data.permissionCode">{{ data.permissionCode }}</small>
                </span>
              </template>
            </el-tree>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
