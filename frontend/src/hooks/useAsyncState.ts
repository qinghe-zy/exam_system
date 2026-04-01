import { ref } from 'vue'

export function useAsyncState<T>(initialValue: T) {
  const data = ref<T>(initialValue)
  const loading = ref(false)

  async function run(task: () => Promise<T>) {
    loading.value = true
    try {
      data.value = await task()
      return data.value
    } finally {
      loading.value = false
    }
  }

  return {
    data,
    loading,
    run
  }
}
