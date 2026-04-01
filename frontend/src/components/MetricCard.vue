<script setup lang="ts">
defineProps<{
  label: string
  value: number
  description: string
}>()
</script>

<template>
  <article class="panel-card metric-card">
    <span class="eyebrow">{{ label }}</span>
    <strong>{{ value }}</strong>
    <p>{{ description }}</p>
  </article>
</template>

<style scoped>
.metric-card {
  padding: 1.1rem 1.15rem;
  min-height: 9rem;
  display: grid;
  align-content: start;
  gap: 0.6rem;
}

strong {
  font-size: clamp(2rem, 4vw, 2.9rem);
  font-weight: 800;
  color: var(--brand-deep);
}

p {
  margin: 0;
  color: var(--muted);
  line-height: 1.55;
}
</style>
