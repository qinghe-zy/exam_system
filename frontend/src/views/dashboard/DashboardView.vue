<script setup lang="ts">
import { onMounted } from 'vue'

import { fetchDashboardOverview } from '../../api/dashboard'
import AppShellSection from '../../components/AppShellSection.vue'
import MetricCard from '../../components/MetricCard.vue'
import { useAsyncState } from '../../hooks/useAsyncState'

const { data: overview, run } = useAsyncState({
  headline: '',
  summary: '',
  metrics: [] as { label: string; value: number; description: string }[],
  nextActions: [] as string[]
})

onMounted(() => {
  run(fetchDashboardOverview)
})
</script>

<template>
  <AppShellSection
    eyebrow="Exam System"
    title="An assessment-ready workspace derived from the shared admin base."
    description="The dashboard keeps the validated auth and system core, then focuses this seed on question bank, paper management, and result visibility."
  >
    <div class="metrics-grid">
      <MetricCard
        v-for="metric in overview.metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :description="metric.description"
      />
    </div>

    <section class="panel-card detail-grid">
      <article>
        <span class="eyebrow">Current Headline</span>
        <h3>{{ overview.headline }}</h3>
        <p class="muted">{{ overview.summary }}</p>
      </article>
      <article>
        <span class="eyebrow">Next Actions</span>
        <ul>
          <li v-for="item in overview.nextActions" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>
  </AppShellSection>
</template>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.detail-grid {
  padding: 1.25rem 1.35rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.2rem;
}

h3 {
  margin: 0.5rem 0;
  font-family: 'Literata', Georgia, serif;
}

ul {
  margin: 0.65rem 0 0;
  padding-left: 1.1rem;
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 980px) {
  .metrics-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
