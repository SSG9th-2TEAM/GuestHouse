<script setup>
import { ref, computed } from 'vue'
import HostAccommodationRegister from './HostAccommodationRegister.vue'

// viewMode: 'list' | 'register' | 'edit'
const viewMode = ref('list')

// 호스트의 숙소 목록 (실제로는 API에서 가져올 데이터)
const hostAccommodations = ref([
  {
    id: 1,
    name: '제주도 감성 숙소',
    status: 'active', // active, inactive
    location: '제주시 애월읍',
    maxGuests: 4,
    roomCount: 2,
    price: 120000,
    images: [
      'https://picsum.photos/id/49/400/300',
      'https://picsum.photos/id/50/400/300',
      'https://picsum.photos/id/51/400/300',
      'https://picsum.photos/id/52/400/300'
    ]
  },
  {
    id: 2,
    name: '강릉 오션뷰 펜션',
    status: 'active',
    location: '강원도 강릉시',
    maxGuests: 6,
    roomCount: 3,
    price: 180000,
    images: [
      'https://picsum.photos/id/53/400/300',
      'https://picsum.photos/id/54/400/300',
      'https://picsum.photos/id/55/400/300',
      'https://picsum.photos/id/56/400/300'
    ]
  },
  {
    id: 3,
    name: '한옥 게스트하우스',
    status: 'inactive',
    location: '전주시 완산구',
    maxGuests: 8,
    roomCount: 4,
    price: 95000,
    images: [
      'https://picsum.photos/id/57/400/300',
      'https://picsum.photos/id/58/400/300',
      'https://picsum.photos/id/59/400/300',
      'https://picsum.photos/id/60/400/300'
    ]
  }
])

const accommodationCount = computed(() => hostAccommodations.value.length)
const hasAccommodations = computed(() => hostAccommodations.value.length > 0)

const formatPrice = (price) => {
  return new Intl.NumberFormat('ko-KR').format(price)
}

const getStatusLabel = (status) => {
  return status === 'active' ? '운영중' : '운영중지'
}

const handleRegister = () => {
  viewMode.value = 'register'
}

const handleRegisterCancel = () => {
  viewMode.value = 'list'
}

const handleRegisterSubmit = (formData) => {
  // Mock API call to save data
  const newId = Math.max(...hostAccommodations.value.map(a => a.id), 0) + 1
  const newAccommodation = {
    id: newId,
    ...formData,
    status: 'active'
  }
  
  hostAccommodations.value.unshift(newAccommodation)
  viewMode.value = 'list'
}

const handleEdit = (id) => {
  // TODO: 숙소 수정 페이지로 이동 implementation
  console.log('숙소 수정:', id)
}

const handleDelete = (id) => {
  if (confirm('정말 이 숙소를 삭제하시겠습니까?')) {
    hostAccommodations.value = hostAccommodations.value.filter(acc => acc.id !== id)
  }
}
</script>

<template>
  <div class="accommodation-container">
    
    <!-- Registration View -->
    <HostAccommodationRegister 
      v-if="viewMode === 'register'"
      @cancel="handleRegisterCancel"
      @submit="handleRegisterSubmit"
    />

    <!-- List View -->
    <div v-else class="list-view-wrapper">
      <!-- Header -->
      <div class="page-header">
        <h1 class="page-title">숙소 등록/관리</h1>
        <p class="page-subtitle">총 {{ accommodationCount }}개의 숙소</p>
      </div>

      <!-- Register Button -->
      <button class="register-btn" @click="handleRegister">
        <span class="plus-icon">+</span>
        새 숙소 등록
      </button>

      <!-- Accommodation List -->
      <div v-if="hasAccommodations" class="accommodation-list">
        <div 
          v-for="accommodation in hostAccommodations" 
          :key="accommodation.id" 
          class="accommodation-card"
        >
          <!-- Main Image Only -->
          <div class="card-image">
            <img :src="accommodation.images[0] || 'https://via.placeholder.com/400x300'" :alt="accommodation.name" />
          </div>

          <!-- Info Section -->
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
              <span class="detail-item">
                <span class="detail-icon">📍</span>
                {{ accommodation.location }}
              </span>
              <span class="detail-item">
                <span class="detail-icon">👥</span>
                최대 {{ accommodation.maxGuests }}명
              </span>
              <span class="detail-item">
                <span class="detail-icon">🛏️</span>
                {{ accommodation.roomCount }}개 객실
              </span>
            </div>

            <div class="price-actions">
              <div class="price-info">
                <span class="price">₩{{ formatPrice(accommodation.price) }}</span>
                <span class="price-unit">/박</span>
              </div>
              <div class="action-buttons">
                <button class="action-btn edit-btn" @click="handleEdit(accommodation.id)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="action-btn delete-btn" @click="handleDelete(accommodation.id)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    <line x1="10" y1="11" x2="10" y2="17"/>
                    <line x1="14" y1="11" x2="14" y2="17"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state">
        <div class="empty-icon">🏠</div>
        <h2>등록된 숙소가 없습니다</h2>
        <p>새 숙소를 등록하여 게스트를 맞이해보세요!</p>
      </div>

    </div> <!-- End List View Wrapper -->

  </div>
</template>

<style scoped>
.accommodation-container {
  padding: 0 1rem 2rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #222;
  margin-bottom: 0.25rem;
}

.page-subtitle {
  font-size: 0.9rem;
  color: #888;
}

/* Register Button */
.register-btn {
  width: 100%;
  padding: 1rem;
  background: #BFE7DF;
  color: #004d40;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  transition: all 0.2s;
}

.register-btn:hover {
  background: #a8ddd2;
}

.plus-icon {
  font-size: 1.2rem;
  font-weight: 700;
}

/* Accommodation List */
.accommodation-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.accommodation-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

/* Card Image */
.card-image {
  height: 240px;
  width: 100%;
  overflow: hidden;
  position: relative;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.accommodation-card:hover .card-image img {
  transform: scale(1.02);
}

/* Card Info */
.card-info {
  padding: 1.25rem;
  flex-grow: 1;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.accommodation-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: #222;
  margin: 0;
}

.status-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 600;
}

.status-badge.active {
  background: #BFE7DF;
  color: #004d40;
}

.status-badge.inactive {
  background: #f0f0f0;
  color: #888;
}

.info-details {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1.2rem;
  margin-bottom: 1.5rem;
  color: #555;
  font-size: 0.9rem;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.detail-icon {
  font-size: 1rem;
}

/* Price & Actions */
.price-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-info {
  display: flex;
  align-items: baseline;
  gap: 0.2rem;
}

.price {
  font-size: 1.35rem;
  font-weight: 700;
  color: #111;
}

.price-unit {
  font-size: 0.9rem;
  color: #888;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid #eee;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.edit-btn {
  color: #555;
}

.edit-btn:hover {
  border-color: #BFE7DF;
  background: #f0fcf9;
  color: #004d40;
}

.delete-btn {
  color: #e74c3c;
}

.delete-btn:hover {
  border-color: #ffebee;
  background: #fff5f5;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #222;
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: #888;
  font-size: 0.95rem;
}
</style>
