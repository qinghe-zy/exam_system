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
    eyebrow="系统管理"
    title="菜单与路由权限"
    description="菜单页用于展示当前系统的菜单节点、路由路径、权限编码与菜单类型，便于核对角色可见范围与系统导航结构。"
  >
    <section class="panel-card table-card">
      <el-table
        :data="menus"
        v-loading="loading"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="菜单名称" min-width="180" />
        <el-table-column prop="path" label="路由路径" min-width="200" />
        <el-table-column prop="permissionCode" label="权限编码" min-width="180" />
        <el-table-column prop="menuType" label="菜单类型" min-width="120" />
      </el-table>
    </section>
  </AppShellSection>
</template>

<style scoped>
.table-card {
  padding: 1rem;
}
</style>
