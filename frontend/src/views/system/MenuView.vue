<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchMenus, type SystemMenu } from '../../api/system'
import AppShellSection from '../../components/AppShellSection.vue'

const menus = ref<SystemMenu[]>([])
const loading = ref(false)

async function loadMenus() {
  loading.value = true
  try {
    menus.value = await fetchMenus()
  } finally {
    loading.value = false
  }
}

onMounted(loadMenus)
</script>

<template>
  <AppShellSection
    eyebrow="System Module"
    title="Menu and route permissions"
    description="Menu records now carry role visibility and route ownership so candidate, grading, operations, and administration experiences stay separated inside one monolith."
  >
    <section class="panel-card table-card">
      <el-table
        :data="menus"
        v-loading="loading"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="Menu" min-width="180" />
        <el-table-column prop="path" label="Path" min-width="200" />
        <el-table-column prop="permissionCode" label="Permission" min-width="180" />
        <el-table-column prop="menuType" label="Type" min-width="120" />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.table-card {
  padding: 1rem;
}
</style>
