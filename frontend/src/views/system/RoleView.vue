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
    eyebrow="System Module"
    title="Role module skeleton"
    description="Roles are seeded to support the current JWT + RBAC baseline. Future templates can extend this into richer permission, department, or data-scope models without rebuilding the starter."
  >
    <section class="panel-card table-card">
      <el-table :data="roles" v-loading="loading">
        <el-table-column prop="roleCode" label="Role Code" min-width="180" />
        <el-table-column prop="roleName" label="Role Name" min-width="180" />
        <el-table-column prop="remark" label="Remark" min-width="220" />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.table-card {
  padding: 1rem;
}
</style>
