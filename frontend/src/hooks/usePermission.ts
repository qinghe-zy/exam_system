import { computed } from 'vue'

import { useAuthStore } from '../stores/auth'

export function usePermission() {
  const authStore = useAuthStore()
  const permissions = computed(() => authStore.permissions)

  function hasPermission(permission: string) {
    return permissions.value.includes(permission)
  }

  return {
    permissions,
    hasPermission
  }
}
