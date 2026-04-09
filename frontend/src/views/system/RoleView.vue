<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchRoles, type SystemRole } from '../../api/system'
import AppShellSection from '../../components/AppShellSection.vue'

const roles = ref<SystemRole[]>([])
const loading = ref(false)

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await fetchRoles()
  } finally {
    loading.value = false
  }
}

onMounted(loadRoles)
</script>

<template>
  <AppShellSection
    eyebrow="系统管理"
    title="角色与职责边界"
    description="查看系统内置角色与职责边界，快速确认管理端、教师、阅卷老师、监考员和学生的权限分工。"
  >
    <section class="panel-card table-card">
      <el-table :data="roles" v-loading="loading">
        <el-table-column prop="roleCode" label="角色编码" min-width="180" />
        <el-table-column prop="roleName" label="角色名称" min-width="180" />
        <el-table-column prop="remark" label="说明" min-width="220" />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.table-card {
  padding: 1rem;
}
</style>
