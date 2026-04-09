<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { usePermission } from '../../hooks/usePermission'
import {
  createUser,
  fetchOrganizations,
  fetchRoles,
  fetchUsers,
  importCandidates,
  type OrganizationNode,
  type SystemRole,
  type SystemUser,
  updateUser
} from '../../api/system'

const users = ref<SystemUser[]>([])
const { hasPermission } = usePermission()
const organizations = ref<OrganizationNode[]>([])
const roles = ref<SystemRole[]>([])
const loading = ref(false)
const userDialogVisible = ref(false)
const importDialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  nickname: '',
  fullName: '',
  roleCode: 'STUDENT',
  organizationId: 0,
  departmentName: '',
  email: '',
  phone: '',
  candidateNo: '',
  password: '',
  status: 1
})

const importText = ref('student3,王五,20240003,3,软件工程2401,student3@example.local,13800000008')

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  organizationId: [{ required: true, message: '请选择组织', trigger: 'change' }]
}

const flatOrganizations = computed(() => {
  const result: OrganizationNode[] = []
  const walk = (nodes: OrganizationNode[]) => {
    nodes.forEach((node) => {
      result.push(node)
      if (node.children?.length) walk(node.children)
    })
  }
  walk(organizations.value)
  return result
})

async function loadData() {
  loading.value = true
  try {
    const [userList, orgList, roleList] = await Promise.all([fetchUsers(), fetchOrganizations(), fetchRoles()])
    users.value = userList
    organizations.value = orgList
    roles.value = roleList
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    nickname: '',
    fullName: '',
    roleCode: 'STUDENT',
    organizationId: 0,
    departmentName: '',
    email: '',
    phone: '',
    candidateNo: '',
    password: '',
    status: 1
  })
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  userDialogVisible.value = true
}

function openEdit(row: SystemUser) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname,
    fullName: row.fullName || row.nickname,
    roleCode: row.roleCode,
    organizationId: row.organizationId || 0,
    departmentName: row.departmentName || '',
    email: row.email || '',
    phone: '',
    candidateNo: '',
    password: '',
    status: row.status
  })
  userDialogVisible.value = true
}

async function submitUser() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (dialogMode.value === 'create') await createUser(form)
    else if (editingId.value) await updateUser(editingId.value, form)
    ElMessage.success(dialogMode.value === 'create' ? '用户已创建' : '用户已更新')
    userDialogVisible.value = false
    await loadData()
  })
}

async function submitImport() {
  const items = importText.value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [username, fullName, candidateNo, organizationId, departmentName, email, phone] = line.split(',')
      return {
        username: username.trim(),
        fullName: fullName.trim(),
        candidateNo: candidateNo.trim(),
        organizationId: Number(organizationId),
        departmentName: departmentName?.trim(),
        email: email?.trim(),
        phone: phone?.trim()
      }
    })
  const result = await importCandidates({ items })
  ElMessage.success(`批量导入完成，共导入 ${result.data?.importedCount ?? items.length} 名考生`)
  importDialogVisible.value = false
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="系统管理"
    title="用户、角色与考生导入"
    description="统一维护教师、学生与管理账号，并完成组织归属、角色分配和考生导入等准备工作。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button v-if="hasPermission('sys:user:import')" @click="importDialogVisible = true">批量导入考生</el-button>
        <el-button v-if="hasPermission('sys:user:create')" type="primary" @click="openCreate">新建用户</el-button>
      </div>
    </template>

    <section class="panel-card table-card">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="fullName" label="姓名" min-width="140" />
        <el-table-column prop="roleCode" label="角色" min-width="120" />
        <el-table-column prop="organizationName" label="组织" min-width="220" />
        <el-table-column prop="departmentName" label="部门/班级" min-width="160" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('sys:user:update')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <span v-else class="muted">仅查看</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="userDialogVisible" :title="dialogMode === 'create' ? '新建用户' : '编辑用户'" width="min(840px, 96vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-three">
          <el-form-item label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
          <el-form-item label="姓名" prop="fullName"><el-input v-model="form.fullName" /></el-form-item>
          <el-form-item label="角色" prop="roleCode">
            <el-select v-model="form.roleCode">
              <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.roleCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="组织" prop="organizationId">
            <el-select v-model="form.organizationId">
              <el-option v-for="org in flatOrganizations" :key="org.id" :label="org.orgName" :value="org.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="部门/班级"><el-input v-model="form.departmentName" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="考生编号"><el-input v-model="form.candidateNo" /></el-form-item>
          <el-form-item label="初始密码"><el-input v-model="form.password" placeholder="留空时使用默认密码" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="批量导入考生" width="min(760px, 96vw)">
      <p class="muted">每行一名考生，格式：用户名,姓名,考生编号,组织ID,班级/部门,邮箱,手机号</p>
      <el-input v-model="importText" type="textarea" :rows="8" />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.hero-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.table-card {
  padding: 1rem;
}

.grid-three {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

@media (max-width: 980px) {
  .grid-three {
    grid-template-columns: 1fr;
  }
}
</style>
