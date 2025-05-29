<!-- src/components/MetricCard.vue -->
<template>
  <div class="metric-card">
    <h3 class="metric-title">{{ title }}</h3>
    <div class="metric-value">{{ value }}</div>
    <div v-if="subtitle" class="metric-subtitle">{{ subtitle }}</div>
  </div>
</template>

<script>
export default {
  props: {
    title: String,
    value: [String, Number],
    subtitle: String
  }
}
</script>

<style scoped>
.metric-card {
  padding: 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin: 0.5rem;
  min-width: 200px;
  text-align: center;
  background: white;
}
.metric-title {
  color: #666;
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
}
.metric-value {
  font-size: 2rem;
  font-weight: bold;
  color: #2c3e50;
}
.metric-subtitle {
  font-size: 0.8rem;
  color: #888;
}
</style>