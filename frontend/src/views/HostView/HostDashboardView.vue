<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const tabs = [
  { id: 'dashboard', label: '대시보드', icon: 'home', path: '/host' },
  { id: 'property', label: '숙소', icon: 'property', path: '/host/accommodation' },
  { id: 'booking', label: '예약', icon: 'calendar', path: '/host/booking' },
  { id: 'revenue', label: '매출', icon: 'revenue', path: '/host/revenue' },
  { id: 'review', label: '리뷰', icon: 'review', path: '/host/review' }
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
        <svg v-if="tab.icon === 'home'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 10.5 12 4l9 6.5V20a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-6H10v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1Z" />
        </svg>
        <svg v-else-if="tab.icon === 'property'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2h6a1 1 0 0 1 1 1v9h-2v-4H5v4H3Z" />
          <path d="M5 12h6V9H5Z" />
          <path d="M13 12h6V9h-6Z" />
        </svg>
        <svg v-else-if="tab.icon === 'calendar'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="6" width="16" height="14" rx="2" />
          <path d="M8 3v4" />
          <path d="M16 3v4" />
          <path d="M4 10h16" />
        </svg>
        <svg v-else-if="tab.icon === 'revenue'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="11" width="3" height="8" rx="1" />
          <rect x="10.5" y="7" width="3" height="12" rx="1" />
          <rect x="17" y="4" width="3" height="15" rx="1" />
        </svg>
        <svg v-else-if="tab.icon === 'review'" class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3.5 14.6 9l5.4.4-4.2 3.3 1.2 5.4L12 15.8 7 18.1l1.2-5.4L4 9.4 9.4 9Z" />
        </svg>
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
