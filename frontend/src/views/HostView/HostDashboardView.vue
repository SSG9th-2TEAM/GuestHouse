<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import NavHome from '@/components/nav-icons/NavHome.vue'
import NavStay from '@/components/nav-icons/NavStay.vue'
import NavReservation from '@/components/nav-icons/NavReservation.vue'
import NavSales from '@/components/nav-icons/NavSales.vue'
import NavReview from '@/components/nav-icons/NavReview.vue'

const router = useRouter()
const route = useRoute()

const tabs = [
  { id: 'dashboard', label: '대시보드', icon: NavHome, path: '/host' },
  { id: 'property', label: '숙소', icon: NavStay, path: '/host/accommodation' },
  { id: 'booking', label: '예약', icon: NavReservation, path: '/host/booking' },
  { id: 'revenue', label: '매출', icon: NavSales, path: '/host/revenue' },
  { id: 'review', label: '리뷰', icon: NavReview, path: '/host/review' }
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
        <component :is="tab.icon" class="nav-icon" />
        <span class="nav-label">{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
/* ✅ merge-safe: 이 파일 내부에서만 하단 네비 토큰 관리 */
.host-dashboard {
  min-height: 100vh;
  background: #f5f5f5;

  /* 하단 네비 사이즈 토큰(모바일 퍼스트) */
  --bn-h: 56px;      /* bar base height(패딩 제외) */
  --bn-pad: 6px;     /* 위/아래 패딩 */
  --bn-icon: 20px;
  --bn-label: 11px;
  --bn-gap: 2px;
  --bn-stroke: 1.9;

  /* ✅ 하단 네비가 있는 동안 컨텐츠가 가려지지 않도록 */
  padding-bottom: calc(var(--bn-h) + (var(--bn-pad) * 2) + env(safe-area-inset-bottom));
}

/* ===================
   Top Nav (>=1024)
   =================== */
.top-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: none; /* 기본 숨김 */
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
  font-weight: 800;
  font-size: 1rem;
  color: #0f766e;
  letter-spacing: -0.01em;
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
  font-weight: 800;
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

/* ===================
   Content
   =================== */
.content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 1.5rem 1rem 2.5rem;
}

/* ===================
   Bottom Nav (<1024)
   =================== */
.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;

  background: #ffffff;
  border-top: 1px solid #e5e7eb;

  display: grid;
  grid-template-columns: repeat(5, 1fr);
  z-index: 50;

  box-sizing: border-box;
  height: calc(var(--bn-h) + (var(--bn-pad) * 2) + env(safe-area-inset-bottom));
  padding: var(--bn-pad) 0 calc(var(--bn-pad) + env(safe-area-inset-bottom));

  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}

.nav-item {
  padding: 0;
  min-height: 44px; /* ✅ 터치 타겟 확보 */
  background: none;
  border: none;
  text-align: center;
  color: #111827;
  font-weight: 800;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--bn-gap);
}

.nav-item.active {
  color: #BFE7DF;
}

.nav-icon {
  width: var(--bn-icon);
  height: var(--bn-icon);
  display: block;
  color: inherit;
}

/* ✅ 아이콘 컴포넌트 내부 svg 크기 강제 */
.nav-icon :deep(svg),
.nav-icon :deep(img) {
  width: 100% !important;
  height: 100% !important;
  display: block;
}

/* ✅ stroke 아이콘(선) 두께까지 제어 */
.nav-icon :deep(svg) {
  stroke-width: var(--bn-stroke) !important;
}

.nav-label {
  font-size: var(--bn-label);
  line-height: 1;
  margin-top: 0;
  font-weight: 800;
}

/* ===================
   Mobile tuning
   =================== */

/* 대부분 폰 (iPhone 12/13/14/15, 갤럭시 기본 폭 포함) */
@media (max-width: 430px) {
  .host-dashboard {
    --bn-h: 54px;
    --bn-pad: 5px;
    --bn-icon: 19px;
    --bn-label: 11px;
    --bn-stroke: 1.85;
  }
}

/* 작은 폰 (iPhone SE급/작은 안드로이드) */
@media (max-width: 360px) {
  .host-dashboard {
    --bn-h: 50px;
    --bn-pad: 3px; /* 50 - (3+3) = 44 유지 */
    --bn-icon: 18px;
    --bn-label: 10px;
    --bn-stroke: 1.75;
  }
}

/* ===================
   Desktop rule
   =================== */
@media (min-width: 1024px) {
  /* 데스크탑: 상단 네비, 하단 네비 숨김 */
  .bottom-nav {
    display: none;
  }

  .top-nav {
    display: block;
  }

  /* 하단 네비가 없으니 padding-bottom 제거 */
  .host-dashboard {
    padding-bottom: 0;
  }

  .content {
    padding-top: 2rem;
  }
}
</style>
