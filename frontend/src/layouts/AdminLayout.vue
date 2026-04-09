<script setup lang="ts">
import type { Component } from 'vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BellFilled,
  Calendar,
  ChatDotRound,
  Collection,
  DataLine,
  Document,
  EditPen,
  Histogram,
  Menu as MenuIcon,
  OfficeBuilding,
  Odometer,
  Reading,
  Setting,
  Tickets,
  Tools,
  User,
  Warning
} from '@element-plus/icons-vue'

import { useAuthStore } from '../stores/auth'
import type { SystemMenu } from '../api/system'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const iconMap: Record<string, Component> = {
  Odometer,
  Setting,
  User,
  Collection,
  Calendar,
  ChatDotRound,
  EditPen,
  DataLine,
  OfficeBuilding,
  Menu: MenuIcon,
  Bell: BellFilled,
  Reading,
  Document,
  Histogram,
  Tickets,
  Tools,
  Warning
}

const navigation = computed(() => authStore.menus)

async function handleCommand(command: string) {
  if (command === 'logout') {
    await authStore.logout()
    await router.push('/login')
  }
}

function resolveIcon(iconName?: string) {
  return iconMap[iconName || 'Menu'] || MenuIcon
}

function handleSelect(index: string) {
  if (index !== route.path) {
    router.push(index)
  }
}

function isGroup(menu: SystemMenu) {
  return menu.children && menu.children.length > 0
}
</script>

<template>
  <div class="layout-shell">
    <aside class="sidebar panel-card">
      <div class="brand-block">
        <span class="eyebrow">系统导航</span>
        <h2>在线考试系统</h2>
        <p>面向考试组织、阅卷治理与运营协同的统一工作台</p>
      </div>

      <el-menu :default-active="route.path" class="nav-menu" @select="handleSelect">
        <template v-for="menu in navigation" :key="menu.id">
          <el-sub-menu v-if="isGroup(menu)" :index="menu.path">
            <template #title>
              <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
              <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <main class="main-panel">
      <header class="topbar panel-card">
        <div>
          <span class="eyebrow">登录身份</span>
          <p class="welcome-text">{{ authStore.currentUser?.nickname || '未登录用户' }}</p>
        </div>
        <el-dropdown @command="handleCommand">
          <span class="account-pill">
            {{ authStore.currentUser?.roleCode || 'ROLE' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <section class="content-area">
        <router-view />
      </section>
    </main>
  </div>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 1rem;
  padding: 1rem;
}

.sidebar {
  padding: 1.1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
}

.brand-block h2 {
  margin: 0.35rem 0;
  font-family: 'Literata', Georgia, serif;
}

.brand-block p,
.welcome-text {
  margin: 0;
  color: var(--muted);
}

.nav-menu {
  border-right: none;
  background: transparent;
}

.main-panel {
  display: grid;
  gap: 1rem;
  align-content: start;
}

.topbar {
  padding: 1rem 1.2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.account-pill {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 6rem;
  padding: 0.72rem 0.95rem;
  border-radius: 999px;
  background: color-mix(in oklch, var(--brand) 18%, white);
  color: var(--brand-deep);
  font-weight: 700;
}

.content-area {
  padding-bottom: 1rem;
}

@media (max-width: 980px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }
}
</style>
