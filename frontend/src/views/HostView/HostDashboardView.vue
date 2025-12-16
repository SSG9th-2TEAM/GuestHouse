<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const tabs = [
  { id: 'dashboard', label: '대시보드', icon: '🏠', path: '/host' },
  { id: 'property', label: '숙소', icon: '🛏️', path: '/host/accommodation' },
  { id: 'booking', label: '예약', icon: '📆', path: '/host/booking' },
  { id: 'revenue', label: '매출', icon: '💰', path: '/host/revenue' },
  { id: 'review', label: '리뷰', icon: '⭐', path: '/host/review' }
]

const activeTab = computed(() => {
  const path = route.path
  if (path.includes('/host/accommodation')) return 'property'
  if (path.includes('/host/booking')) return 'booking'
  if (path.includes('/host/revenue')) return 'revenue'
  if (path.includes('/host/review')) return 'review'
  return 'dashboard'
})

const setTab = (path) => router.push(path)
</script>

<template>
  <div class="host-dashboard">
    <!-- Desktop top navigation -->
    <header class="top-nav">
      <div class="top-nav__inner">
        <div class="brand">Host Center</div>
        <nav class="top-menu">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            class="top-menu__item"
            :class="{ active: activeTab === tab.id }"
            @click="setTab(tab.path)"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>
    </header>

    <!-- Main Content -->
    <main class="content">
      <router-view />
    </main>

    <!-- Mobile bottom navigation -->
    <nav class="bottom-nav">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="nav-item"
        :class="{ active: activeTab === tab.id }"
        @click="setTab(tab.path)"
      >
        <span class="nav-icon" aria-hidden="true">{{ tab.icon }}</span>
        <span class="nav-label">{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.host-dashboard {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 72px; /* bottom nav space on mobile */
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: none;
}

.top-nav__inner {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0.75rem 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.brand {
  font-weight: 700;
  font-size: 1rem;
  color: #0f766e;
}

.top-menu {
  display: flex;
  gap: 0.75rem;
}

.top-menu__item {
  border: none;
  background: transparent;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  color: #4b5563;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.top-menu__item:hover {
  background: #e0f2f1;
  color: #0f766e;
}

.top-menu__item.active {
  background: #0f766e;
  color: white;
}

.content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 1.5rem 1rem 2.5rem;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid #e5e7eb;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  z-index: 20;
  padding: 0.2rem 0.4rem;
}

.nav-item {
  padding: 0.85rem 0.5rem;
  background: none;
  border: none;
  text-align: center;
  font-size: 1rem;
  color: #475569;
  font-weight: 700;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.nav-item.active {
  color: #0f766e;
}

.nav-icon {
  font-size: 1.35rem;
  line-height: 1;
}

@media (min-width: 768px) {
  .host-dashboard {
    padding-bottom: 0;
  }

  .bottom-nav {
    display: none;
  }
}

@media (min-width: 1024px) {
  .top-nav {
    display: block;
  }

  .content {
    padding-top: 2rem;
  }
}
</style>
