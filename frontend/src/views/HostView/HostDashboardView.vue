<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// Active tab based on current route
const activeTab = computed(() => {
  const path = route.path
  if (path.includes('/host/accommodation')) return 'property'
  if (path.includes('/host/booking')) return 'booking'
  if (path.includes('/host/review')) return 'review'
  if (path.includes('/host/revenue')) return 'revenue'
  return 'dashboard'
})

const tabs = [
  { id: 'dashboard', label: '대시보드', path: '/host' },
  { id: 'property', label: '숙소', path: '/host/accommodation' },
  { id: 'booking', label: '예약', path: '/host/booking' },
  { id: 'revenue', label: '매출', path: '/host/revenue' },
  { id: 'review', label: '리뷰', path: '/host/review' }
]

const setTab = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="host-dashboard">
    <!-- Main Content -->
    <div class="container content">
      <router-view></router-view>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <button 
        v-for="tab in tabs" 
        :key="tab.id" 
        class="nav-item"
        :class="{ active: activeTab === tab.id }"
        @click="setTab(tab.path)"
      >
        <span class="nav-icon">{{ tab.icon }}</span>
        <span class="nav-label">{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.host-dashboard {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 80px;
}

.content {
  padding-top: 2rem;
  max-width: 500px;
}

/* Bottom Navigation */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: space-around;
  padding: 0.5rem 0;
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 0.5rem 1rem;
  background: none;
  border: none;
  cursor: pointer;
  color: #888;
  transition: color 0.2s;
}

.nav-item.active {
  color: #004d40;
}

.nav-icon {
  font-size: 1.3rem;
}

.nav-label {
  font-size: 0.75rem;
  font-weight: 500;
}
</style>
