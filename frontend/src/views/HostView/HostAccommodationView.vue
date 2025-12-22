<script setup>
import { ref, computed } from 'vue'
import HostAccommodationRegister from './HostAccommodationRegister.vue'
import { useHostAccommodationsStore } from '@/stores/hostAccommodations'

const viewMode = ref('list')
const accommodationStore = useHostAccommodationsStore()

const accommodationCount = computed(() => accommodationStore.accommodations.length)
const hasAccommodations = computed(() => accommodationStore.accommodations.length > 0)

const formatPrice = (price) => new Intl.NumberFormat('ko-KR').format(price)
const getStatusLabel = (status) => (status === 'active' ? '운영중' : '운영중지')

const handleRegisterCancel = () => (viewMode.value = 'list')

const handleRegisterSubmit = (formData) => {
  accommodationStore.addAccommodation(formData)
  viewMode.value = 'list'
}

const handleDelete = (id) => {
  if (confirm('정말 이 숙소를 삭제하시겠습니까?')) {
    accommodationStore.removeAccommodation(id)
  }
}
</script>

<template>
  <div class="accommodation-container">
    <HostAccommodationRegister
        v-if="viewMode === 'register'"
        @cancel="handleRegisterCancel"
        @submit="handleRegisterSubmit"
    />

    <div v-else class="list-view-wrapper">
      <div class="view-header">
        <div>
          <h2>숙소 관리</h2>
          <p class="subtitle">총 {{ accommodationCount }}개의 숙소</p>
        </div>
      </div>

      <button class="register-btn" @click="$router.push('/host/accommodation/register')">
        <span class="plus-icon">+</span>
        새 숙소 등록
      </button>

      <div v-if="hasAccommodations" class="accommodation-list">
        <article
            v-for="accommodation in accommodationStore.accommodations"
            :key="accommodation.id"
            class="accommodation-card"
        >
          <div class="card-image">
            <img :src="accommodation.images[0] || 'https://via.placeholder.com/400x300'" :alt="accommodation.name"/>
          </div>

          <div class="card-info">
            <div class="info-header">
              <h3 class="accommodation-name">{{ accommodation.name }}</h3>
              <span
                  class="status-badge"
                  :class="{ active: accommodation.status === 'active', inactive: accommodation.status === 'inactive' }"
              >
                {{ getStatusLabel(accommodation.status) }}
              </span>
            </div>

            <div class="info-details">
              <span class="detail-item"><span class="detail-icon">📍</span>{{ accommodation.location }}</span>
              <span class="detail-item"><span class="detail-icon">👥</span>최대 {{ accommodation.maxGuests }}명</span>
              <span class="detail-item"><span class="detail-icon">🛏️</span>{{ accommodation.roomCount }}개 객실</span>
            </div>

            <div class="price-actions">
              <div class="price-info">
                <span class="price">₩{{ formatPrice(accommodation.price) }}</span>
                <span class="price-unit">/박</span>
              </div>
              <div class="action-buttons">
                <button class="action-btn edit-btn"
                        @click="$router.push(`/host/accommodation/edit/${accommodation.id}`)">수정
                </button>
                <button class="action-btn delete-btn" @click="handleDelete(accommodation.id)">삭제</button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">🏠</div>
        <h2>등록된 숙소가 없습니다</h2>
        <p>새 숙소를 등록하여 게스트를 맞이해보세요!</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ✅ 토큰이 있으면 쓰고, 없으면 fallback로 안전하게 */
.accommodation-container {
  padding-bottom: 2rem;
}

.view-header {
  margin-bottom: 1.25rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--host-title, #0b3b32);
  margin: 0.15rem 0 0.2rem;
  letter-spacing: -0.01em;
}

.subtitle {
  color: var(--text-sub, #6b7280);
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.register-btn {
  width: 100%;
  padding: 0.95rem 1rem;
  background: var(--primary, #BFE7DF);
  color: #004d40;
  font-size: 1rem;
  font-weight: 900;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}

.register-btn:hover {
  background: var(--primary-hover, #A0D1C8);
}

.plus-icon {
  font-size: 1.2rem;
  font-weight: 900;
}

.accommodation-list {
  display: grid;
  gap: 1rem;
}

.accommodation-card {
  background: var(--bg-white, #fff);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border, #e5e7eb);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
  display: grid;
  grid-template-columns: 1fr;
}

.card-image {
  height: 210px;
  width: 100%;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-info {
  padding: 1.15rem 1.15rem 1.2rem;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  justify-content: space-between;
  margin-bottom: 0.6rem;
}

.accommodation-name {
  font-size: 1.12rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  margin: 0;
}

.status-badge {
  padding: 0.28rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 900;
  border: 1px solid var(--border, #e5e7eb);
  white-space: nowrap;
}

.status-badge.active {
  background: #e0f2f1;
  color: var(--host-accent, #0f766e);
  border-color: #c0e6df;
}

.status-badge.inactive {
  background: #f1f5f9;
  color: #475569;
}

.info-details {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem 1rem;
  margin-bottom: 1.1rem;
  color: #374151;
  font-size: 0.92rem;
  font-weight: 700;
}

.detail-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.price-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.price {
  font-size: 1.35rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.price-unit {
  font-size: 0.9rem;
  color: var(--text-sub, #6b7280);
  font-weight: 800;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid var(--border, #e5e7eb);
  background: white;
  cursor: pointer;
  font-weight: 900;
}

.edit-btn {
  color: var(--host-accent, #0f766e);
}

.edit-btn:hover {
  border-color: #c0e6df;
  background: #f0fcf9;
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  border-color: #fee2e2;
  background: #fff5f5;
}

@media (min-width: 768px) {
  .accommodation-card {
    grid-template-columns: 260px 1fr;
  }

  .card-image {
    height: 100%;
  }

  .register-btn {
    width: auto;
  }
}

.empty-state {
  text-align: center;
  padding: 3.25rem 1.5rem;
  background: var(--bg-white, #fff);
  border-radius: 16px;
  border: 1px solid var(--border, #e5e7eb);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 0.8rem;
}

.empty-state h2 {
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  margin: 0 0 0.35rem;
}

.empty-state p {
  color: var(--text-sub, #6b7280);
  font-size: 0.95rem;
  margin: 0;
}
</style>
