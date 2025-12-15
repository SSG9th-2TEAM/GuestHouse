<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import HostAccommodationView from './HostAccommodationView.vue'

const router = useRouter()
const activeTab = ref('property')

const tabs = [
  { id: 'dashboard', label: '대시보드', icon: '📊' },
  { id: 'property', label: '숙소', icon: '🏠' },
  { id: 'booking', label: '예약', icon: '📅' },
  { id: 'revenue', label: '매출', icon: '📈' },
  { id: 'review', label: '리뷰', icon: '💬' }
]

const setTab = (tabId) => {
  activeTab.value = tabId
}
</script>

<template>
  <div class="host-dashboard">
    <!-- Main Content -->
    <div class="container content">
      <!-- Property Tab - Accommodation Management -->
      <HostAccommodationView v-if="activeTab === 'property'" />

      <!-- Dashboard Tab - Get Started Card -->
      <div v-else-if="activeTab === 'dashboard'" class="start-card">
        <div class="icon-circle">
        </div>
        
        <h1>호스트를 시작해보세요!</h1>
        
        <p class="desc">
          첫 숙소를 등록하고 게스트를 맞이할 준비를 시작하세요.<br/>
          간단한 정보 입력만으로 바로 시작할 수 있습니다.
        </p>

        <button class="register-btn">숙소 등록하기</button>

        <p class="guide-link">
          궁금한 점이 있으신가요? <a href="#">호스트 가이드 보기</a>
        </p>
      </div>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <button 
        v-for="tab in tabs" 
        :key="tab.id" 
        class="nav-item"
        :class="{ active: activeTab === tab.id }"
        @click="setTab(tab.id)"
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

/* Start Card */
.start-card {
  background: white;
  border-radius: 16px;
  padding: 3rem 2rem;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  margin-top: 2rem;
}

.icon-circle {
  width: 80px;
  height: 80px;
  background: var(--primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 2rem;
}

.host-icon {
  font-size: 2rem;
}

.start-card h1 {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  color: #333;
}

.desc {
  font-size: 0.95rem;
  color: #666;
  line-height: 1.6;
  margin-bottom: 2rem;
}

.register-btn {
  width: 100%;
  padding: 1rem;
  background: var(--primary);
  color: #004d40;
  font-size: 1rem;
  font-weight: 700;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 1.5rem;
}

.register-btn:hover {
  opacity: 0.9;
}

.guide-link {
  font-size: 0.9rem;
  color: #666;
}

.guide-link a {
  color: #004d40;
  font-weight: 600;
  text-decoration: underline;
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
