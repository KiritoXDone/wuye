<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="panel-card">
    <header class="panel-card__header">
      <div>
        <h2 class="panel-card__title">{{ title }}</h2>
        <p v-if="description" class="page-description">{{ description }}</p>
      </div>
      <slot name="extra" />
    </header>
    <div class="panel-card__body">
      <slot />
    </div>
  </section>
</template>
