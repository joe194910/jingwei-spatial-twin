<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CornerDownRight, Plus, RefreshCw } from '@lucide/vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import {
  createMenu,
  deleteMenu,
  listMenus,
  updateMenu,
} from '@/api/system'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/types/api'
import type { MenuItem, MenuSaveRequest } from '@/types/system'

interface ParentOption {
  id: number
  label: string
}

// 菜单树同时承载导航项与按钮权限节点，前端只负责编辑和交互约束。
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const menus = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const editingMenuId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const menuForm = reactive<MenuSaveRequest>({
  parentId: 0,
  name: '',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  permissionCode: '',
  visible: 1,
})

const menuRules: FormRules<MenuSaveRequest> = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入菜单路径或按钮标识', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序号', trigger: 'blur' }],
  permissionCode: [
    {
      pattern: /^$|^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$/,
      message: '权限码格式应为模块:对象:动作',
      trigger: 'blur',
    },
  ],
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function collectDescendantIds(items: MenuItem[], menuId: number, collecting = false, result = new Set<number>()) {
  for (const item of items) {
    const isTarget = item.id === menuId
    if (collecting || isTarget) result.add(item.id)
    collectDescendantIds(item.children, menuId, collecting || isTarget, result)
  }
  return result
}

function flattenParentOptions(
  items: MenuItem[],
  depth = 0,
  excludedIds = new Set<number>(),
  result: ParentOption[] = [],
) {
  for (const item of items) {
    if (!excludedIds.has(item.id)) {
      result.push({
        id: item.id,
        label: `${'　'.repeat(depth)}${depth > 0 ? '└ ' : ''}${item.name}`,
      })
      flattenParentOptions(item.children, depth + 1, excludedIds, result)
    }
  }
  return result
}

const parentOptions = computed(() => {
  const excludedIds = editingMenuId.value === null
    ? new Set<number>()
    : collectDescendantIds(menus.value, editingMenuId.value)
  return flattenParentOptions(menus.value, 0, excludedIds)
})

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    menus.value = await listMenus()
  } catch (error) {
    loadError.value = errorMessage(error, '菜单树加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm(parentId = 0) {
  Object.assign(menuForm, {
    parentId,
    name: '',
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    permissionCode: '',
    visible: 1,
  })
}

function openCreateDialog(parent?: MenuItem) {
  editingMenuId.value = null
  resetForm(parent?.id ?? 0)
  dialogVisible.value = true
}

function openEditDialog(menu: MenuItem) {
  editingMenuId.value = menu.id
  Object.assign(menuForm, {
    parentId: menu.parentId,
    name: menu.name,
    path: menu.path,
    component: menu.component ?? '',
    icon: menu.icon ?? '',
    sortOrder: menu.sortOrder,
    permissionCode: menu.permissionCode ?? '',
    visible: menu.visible,
  })
  dialogVisible.value = true
}

async function submitMenu() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload: MenuSaveRequest = {
      parentId: menuForm.parentId,
      name: menuForm.name.trim(),
      path: menuForm.path.trim(),
      component: menuForm.component.trim(),
      icon: menuForm.icon.trim(),
      sortOrder: menuForm.sortOrder,
      permissionCode: menuForm.permissionCode.trim(),
      visible: menuForm.visible,
    }
    if (editingMenuId.value === null) {
      await createMenu(payload)
      ElMessage.success('菜单创建成功')
    } else {
      await updateMenu(editingMenuId.value, payload)
      ElMessage.success('菜单修改成功')
    }
    dialogVisible.value = false
    await Promise.all([loadData(), authStore.refreshMenus()])
  } catch (error) {
    ElMessage.error(errorMessage(error, '菜单保存失败'))
  } finally {
    saving.value = false
  }
}

async function removeMenu(menu: MenuItem) {
  try {
    await ElMessageBox.confirm(
      `确认删除菜单“${menu.name}”吗？存在子节点时后端会拒绝删除。`,
      '删除菜单',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteMenu(menu.id)
    ElMessage.success('菜单删除成功')
    await Promise.all([loadData(), authStore.refreshMenus()])
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorMessage(error, '菜单删除失败'))
  }
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="management-heading">
      <div>
        <span class="management-eyebrow">导航与权限资源</span>
        <h2>菜单管理</h2>
        <p>维护后台菜单、按钮权限节点和稳定权限码。前端隐藏仅改善体验，后端仍执行最终鉴权。</p>
      </div>
      <el-button
        v-if="authStore.hasPermission('system:menu:add')"
        type="primary"
        @click="openCreateDialog()"
      >
        <Plus :size="17" />
        新增根节点
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
      <el-table
        v-loading="loading"
        :data="menus"
        row-key="id"
        default-expand-all
        empty-text="暂无菜单数据"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="菜单名称" min-width="220" />
        <el-table-column prop="path" label="路径或按钮标识" min-width="210" />
        <el-table-column prop="component" label="组件标识" min-width="210">
          <template #default="{ row }">{{ row.component || '-' }}</template>
        </el-table-column>
        <el-table-column prop="permissionCode" label="权限码" min-width="210">
          <template #default="{ row }">
            <code v-if="row.permissionCode" class="permission-code">{{ row.permissionCode }}</code>
            <span v-else class="muted-text">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="85" />
        <el-table-column label="导航可见" width="105">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'" effect="plain">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="right">
          <template #default="{ row }">
            <el-button
              v-if="authStore.hasPermission('system:menu:add')"
              link
              type="primary"
              @click="openCreateDialog(row)"
            >
              <CornerDownRight :size="15" />
              新增子项
            </el-button>
            <el-button
              v-if="authStore.hasPermission('system:menu:edit')"
              link
              type="primary"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="authStore.hasPermission('system:menu:delete')"
              link
              type="danger"
              :disabled="row.children.length > 0"
              @click="removeMenu(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingMenuId === null ? '新增菜单或权限节点' : '编辑菜单或权限节点'"
      width="min(680px, calc(100vw - 32px))"
      destroy-on-close
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="menuForm" :rules="menuRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="上级节点" prop="parentId">
            <el-select v-model="menuForm.parentId" filterable>
              <el-option label="根节点" :value="0" />
              <el-option
                v-for="option in parentOptions"
                :key="option.id"
                :label="option.label"
                :value="option.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="菜单名称" prop="name">
            <el-input v-model="menuForm.name" maxlength="64" placeholder="请输入菜单名称" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="路径或按钮标识" prop="path">
            <el-input v-model="menuForm.path" maxlength="160" placeholder="/system/users 或 #user-add" />
          </el-form-item>
          <el-form-item label="排序号" prop="sortOrder">
            <el-input-number v-model="menuForm.sortOrder" :min="0" :max="9999" controls-position="right" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="组件标识" prop="component">
            <el-input
              v-model="menuForm.component"
              maxlength="160"
              placeholder="例如 system/UserManagementView"
            />
          </el-form-item>
          <el-form-item label="Lucide 图标名" prop="icon">
            <el-input v-model="menuForm.icon" maxlength="64" placeholder="例如 Users" />
          </el-form-item>
        </div>
        <el-form-item label="权限码" prop="permissionCode">
          <el-input
            v-model="menuForm.permissionCode"
            maxlength="128"
            placeholder="例如 system:user:list，无权限要求时留空"
          />
        </el-form-item>
        <el-form-item label="是否在导航显示" prop="visible">
          <el-switch
            v-model="menuForm.visible"
            :active-value="1"
            :inactive-value="0"
            active-text="显示"
            inactive-text="隐藏"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitMenu">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
