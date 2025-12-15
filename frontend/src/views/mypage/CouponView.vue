<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('available')

// Mock data - set to empty array to test empty state
const availableCoupons = ref([
  {
    id: 1,
    name: '신규 회원 환영 쿠폰',
    discount: '10,000원 할인',
    minPurchase: '50,000원 이상 구매 시',
    expiry: '2026.01.31까지'
  },
  {
    id: 2,
    name: '주말 특가 할인',
    discount: '20% 할인',
    minPurchase: '30,000원 이상 구매 시',
    expiry: '2025.12.31까지'
  },
  {
    id: 3,
    name: '제주 여행 특별 쿠폰',
    discount: '15,000원 할인',
    minPurchase: '100,000원 이상 구매 시',
    expiry: '2026.02.28까지'
  },
  {
    id: 4,
    name: '리뷰 작성 감사 쿠폰',
    discount: '3,000원 할인',
    minPurchase: '10,000원 이상 구매 시',
    expiry: '2026.03.31까지'
  }
])

const expiredCoupons = ref([
  {
    id: 5,
    name: '겨울 시즌 할인',
    discount: '5,000원 할인',
    minPurchase: '20,000원 이상 구매 시',
    expiry: '2025.11.30까지',
    expired: true
  }
])

const currentCoupons = ref(availableCoupons.value)

const switchTab = (tab) => {
  activeTab.value = tab
  currentCoupons.value = tab === 'available' ? availableCoupons.value : expiredCoupons.value
}

const hasCoupons = () => availableCoupons.value.length > 0 || expiredCoupons.value.length > 0
</script>

<template>
  <div class="coupon-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>보유 쿠폰</h1>
    </div>

    <!-- Empty State -->
    <div v-if="!hasCoupons()" class="empty-state">
      <div class="empty-icon">🎟️</div>
      <h2>보유한 쿠폰이 없어요</h2>
      <p>쿠폰을 받으면 더 저렴하게 예약할 수 있어요.</p>
      <p>이벤트와 프로모션을 확인해보세요.</p>
    </div>

    <!-- Coupon List -->
    <template v-else>
      <!-- Tabs -->
      <div class="tabs">
        <button 
          class="tab" 
          :class="{ active: activeTab === 'available' }"
          @click="switchTab('available')"
        >
          사용 가능<br><span class="count">{{ availableCoupons.length }}개</span>
        </button>
        <button 
          class="tab" 
          :class="{ active: activeTab === 'expired' }"
          @click="switchTab('expired')"
        >
          만료됨<br><span class="count">{{ expiredCoupons.length }}개</span>
        </button>
      </div>

      <!-- Section Title -->
      <div class="section-title">
        <span v-if="activeTab === 'available'">✓ 사용 가능한 쿠폰</span>
        <span v-else>⏱ 만료된 쿠폰</span>
      </div>

      <!-- Coupon Cards -->
      <div class="coupon-list">
        <div 
          v-for="coupon in currentCoupons" 
          :key="coupon.id" 
          class="coupon-card"
          :class="{ expired: coupon.expired }"
        >
          <div class="coupon-icon">🏷️</div>
          <div class="coupon-info">
            <span class="coupon-name">{{ coupon.name }}</span>
            <span class="coupon-discount">{{ coupon.discount }}</span>
            <span class="coupon-condition">{{ coupon.minPurchase }}</span>
            <span class="coupon-expiry">⏱ {{ coupon.expiry }}</span>
          </div>
          <div v-if="coupon.expired" class="expired-badge">만료됨</div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.coupon-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-header h1 {
  font-size: 1.2rem;
  font-weight: 700;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 6rem 2rem;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 50%;
  margin: 0 auto 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
}

.empty-state h2 {
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: #333;
}

.empty-state p {
  color: #888;
  font-size: 0.95rem;
  line-height: 1.6;
}

/* Tabs */
.tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-bottom: 1.5rem;
}

.tab {
  flex: 1;
  padding: 1rem;
  background: none;
  border: none;
  text-align: center;
  font-size: 0.9rem;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.tab.active {
  color: #333;
  font-weight: bold;
  border-bottom-color: #333;
}

.tab .count {
  font-weight: bold;
}

/* Section Title */
.section-title {
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 1rem;
  font-weight: 500;
}

/* Coupon Cards */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.coupon-card {
  display: flex;
  gap: 1rem;
  padding: 1.2rem;
  border: 1px solid #eee;
  border-radius: 12px;
  background: white;
  position: relative;
}

.coupon-card.expired {
  opacity: 0.6;
}

.coupon-icon {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.coupon-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-name {
  font-size: 0.85rem;
  color: #666;
}

.coupon-discount {
  font-size: 1.1rem;
  font-weight: 800;
  color: #2563eb;
}

.coupon-condition {
  font-size: 0.8rem;
  color: #888;
}

.coupon-expiry {
  font-size: 0.8rem;
  color: #888;
}

.expired-badge {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  background: #f87171;
  color: white;
  font-size: 0.75rem;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: bold;
}
</style>
