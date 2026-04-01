<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <section class="page-shell">
    <header class="panel-card hero-block">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1 class="display-title">{{ title }}</h1>
      <p class="muted hero-copy">{{ description }}</p>
      <slot name="actions" />
    </header>
    <slot />
  </section>
</template>

<style scoped>
.hero-block {
  padding: 1.3rem 1.45rem;
}

.hero-copy {
  max-width: 52rem;
  margin: 0.65rem 0 0;
  line-height: 1.7;
}
</style>
