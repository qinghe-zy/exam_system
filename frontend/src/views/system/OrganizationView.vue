<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import AppShellSection from '../../components/AppShellSection.vue'
import { createOrganization, deleteOrganization, fetchOrganizations, type OrganizationNode, updateOrganization } from '../../api/system'

const loading = ref(false)
const organizations = ref<OrganizationNode[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  orgCode: '',
  orgName: '',
  orgType: 'COLLEGE',
  parentId: 0,
  status: 1
})

const rules: FormRules<typeof form> = {
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgType: [{ required: true, message: '请选择组织类型', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    organizations.value = await fetchOrganizations()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.orgCode = ''
  form.orgName = ''
  form.orgType = 'COLLEGE'
  form.parentId = 0
  form.status = 1
  editingId.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: OrganizationNode) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.orgCode = row.orgCode
  form.orgName = row.orgName
  form.orgType = row.orgType
  form.parentId = row.parentId
  form.status = row.status
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (dialogMode.value === 'create') await createOrganization(form)
    else if (editingId.value) await updateOrganization(editingId.value, form)
    ElMessage.success(dialogMode.value === 'create' ? '组织已创建' : '组织已更新')
    dialogVisible.value = false
    await loadData()
  })
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确认删除该组织？如存在子组织会被阻止。', '提示', { type: 'warning' })
  await deleteOrganization(id)
  ElMessage.success('组织已删除')
  await loadData()
}

function flatten(nodes: OrganizationNode[], result: OrganizationNode[] = []) {
  nodes.forEach((node) => {
    result.push(node)
    if (node.children?.length) flatten(node.children, result)
  })
  return result
}

onMounted(loadData)
</script>

<template>
  <AppShellSection
    eyebrow="系统管理"
    title="组织、院系与班级管理"
    description="组织树是数据隔离、考生归属、考试发布对象和统计口径的基础。当前页面提供学校、学院、班级等组织节点的基础维护能力。"
  >
    <template #actions>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate">新建组织</el-button>
      </div>
    </template>

    <section class="panel-card table-card">
      <el-table :data="organizations" v-loading="loading" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="orgCode" label="组织编码" min-width="140" />
        <el-table-column prop="orgName" label="组织名称" min-width="220" />
        <el-table-column prop="orgType" label="类型" min-width="120" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeItem(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新建组织' : '编辑组织'" width="min(720px, 94vw)" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-two">
          <el-form-item label="组织编码" prop="orgCode"><el-input v-model="form.orgCode" /></el-form-item>
          <el-form-item label="组织名称" prop="orgName"><el-input v-model="form.orgName" /></el-form-item>
          <el-form-item label="组织类型" prop="orgType">
            <el-select v-model="form.orgType">
              <el-option label="学校" value="SCHOOL" />
              <el-option label="学院" value="COLLEGE" />
              <el-option label="班级" value="CLASS" />
              <el-option label="部门" value="DEPARTMENT" />
            </el-select>
          </el-form-item>
          <el-form-item label="父级组织">
            <el-select v-model="form.parentId">
              <el-option :value="0" label="顶级组织" />
              <el-option v-for="item in flatten(organizations)" :key="item.id" :value="item.id" :label="item.orgName" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </AppShellSection>
</template>

<style scoped>
.hero-actions {
  margin-top: 1rem;
}

.table-card {
  padding: 1rem;
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

@media (max-width: 760px) {
  .grid-two {
    grid-template-columns: 1fr;
  }
}
</style>
