<script setup>
const props = defineProps({
  label: { type: String, required: true },
  value: { type: String, required: true },
  sub: { type: String, default: '' },
  tone: { type: String, default: 'primary' }
})

const toneClass = `admin-stat-card--${props.tone}`
</script>

<template>
  <div class="admin-stat-card" :class="toneClass">
    <div class="admin-stat-card__label">{{ label }}</div>
    <div class="admin-stat-card__value">{{ value }}</div>
    <div v-if="sub" class="admin-stat-card__sub">{{ sub }}</div>
    <div v-if="$slots.icon" class="admin-stat-card__icon">
      <slot name="icon" />
    </div>
  </div>
</template>

<style scoped>
.admin-stat-card {
  position: relative;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px 18px;
  background: var(--bg-white);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.admin-stat-card__label {
  font-size: 0.95rem;
  color: var(--text-sub);
  font-weight: 700;
}

.admin-stat-card__value {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0b3b32;
}

.admin-stat-card__sub {
  font-size: 0.9rem;
  color: var(--text-sub);
  font-weight: 600;
}

.admin-stat-card__icon {
  position: absolute;
  right: 14px;
  top: 14px;
  color: #0f766e;
  opacity: 0.4;
}

.admin-stat-card--primary {
  border-color: #d9efe9;
}

.admin-stat-card--success {
  border-color: #d8f5dd;
  background: #f2fbf4;
}

.admin-stat-card--neutral {
  border-color: #e5e7eb;
}

.admin-stat-card--accent {
  border-color: #cce6ff;
  background: #f3f8ff;
}
</style>
