<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchUsers, type SystemUser } from '../../api/system'
import AppShellSection from '../../components/AppShellSection.vue'

const users = ref<SystemUser[]>([])
const loading = ref(false)

async function loadUsers() {
  loading.value = true
  try {
    users.value = await fetchUsers()
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)
</script>

<template>
  <AppShellSection
    eyebrow="System Module"
    title="User and role baseline"
    description="The platform now seeds explicit administrator, teacher, grader, proctor, and student accounts together with organization context. This list is the current authority baseline for the exam lifecycle."
  >
    <section class="panel-card table-card">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="username" label="Username" min-width="160" />
        <el-table-column prop="fullName" label="Full Name" min-width="160" />
        <el-table-column prop="roleCode" label="Role" min-width="140" />
        <el-table-column prop="organizationName" label="Organization" min-width="220" />
        <el-table-column prop="departmentName" label="Department" min-width="180" />
        <el-table-column prop="email" label="Email" min-width="220" />
        <el-table-column label="Status" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.table-card {
  padding: 1rem;
}
</style>
