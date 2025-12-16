<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Carousel State
const currentSlide = ref(0)
const stats = ref([
  {
    label: '이번 달 예상 수익',
    value: '₩5,200,000',
    trend: '▲ 15%',
    trendClass: 'positive',
    iconType: 'chart'
  },
  {
    label: '이번 달 예약률',
    value: '85%',
    trend: '▲ 5%',
    trendClass: 'positive',
    iconType: 'calendar'
  },
  {
    label: '평균 평점',
    value: '4.8',
    trend: '-',
    trendClass: 'neutral',
    iconType: 'star'
  }
])

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % stats.value.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + stats.value.length) % stats.value.length
}

// Stats Mock Data
const schedule = ref([
  {
    id: 1,
    type: 'checkin', // checkin, checkout
    time: '15:00',
    accommodationName: '강남 모던 게스트하우스',
    roomName: '201호',
    guestName: '김철수',
    note: '바베큐 숯 추가 요청'
  },
  {
    id: 2,
    type: 'checkout',
    time: '11:00',
    accommodationName: '제주도 감성 숙소',
    roomName: '별채',
    guestName: '이영희',
    note: ''
  }
])

const currentDate = '12월 10일 (수)' // Mock date matching image

const getActionBadgeClass = (type) => {
  return type === 'checkin' ? 'badge-checkin' : 'badge-checkout'
}

const getActionLabel = (type) => {
  return type === 'checkin' ? '체크인' : '체크아웃'
}
</script>

<template>
  <div class="dashboard-home">
    <div class="view-header">
      <h2>대시보드 개요</h2>
      <p class="subtitle">2025년 12월</p>
    </div>

    <!-- Stats Carousel -->
    <div class="stats-carousel-container">
      <button class="nav-btn prev" @click="prevSlide">
        &#10094;
      </button>
      
      <div class="stats-card">
        <div class="stats-icon-circle">
          <!-- Icon: Chart -->
          <svg v-if="stats[currentSlide].iconType === 'chart'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="20" x2="12" y2="10"></line><line x1="18" y1="20" x2="18" y2="4"></line><line x1="6" y1="20" x2="6" y2="16"></line></svg>
          
          <!-- Icon: Calendar -->
          <svg v-else-if="stats[currentSlide].iconType === 'calendar'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          
          <!-- Icon: Star -->
          <svg v-else-if="stats[currentSlide].iconType === 'star'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
        </div>

        <div class="stats-content">
          <p class="stats-label">{{ stats[currentSlide].label }}</p>
          <h1 class="stats-value">{{ stats[currentSlide].value }}</h1>
          <p class="stats-trend" :class="stats[currentSlide].trendClass">
            {{ stats[currentSlide].trend }}
          </p>
        </div>
      </div>

      <button class="nav-btn next" @click="nextSlide">
        &#10095;
      </button>
    </div>

    <!-- Carousel Indicators -->
    <div class="carousel-dots">
      <span 
        v-for="(stat, index) in stats" 
        :key="index"
        class="dot"
        :class="{ active: currentSlide === index }"
        @click="currentSlide = index"
      ></span>
    </div>

    <!-- Today's Schedule -->
    <div class="schedule-section">
      <div class="schedule-header">
        <h3>오늘의 스케줄</h3>
        <span class="date-label">{{ currentDate }}</span>
      </div>

      <div class="schedule-list">
        <div v-for="item in schedule" :key="item.id" class="schedule-card">
          <div class="card-top">
            <span class="action-badge" :class="getActionBadgeClass(item.type)">
              {{ getActionLabel(item.type) }}
            </span>
            <span class="time-label">{{ item.time }}</span>
          </div>

          <div class="card-body">
            <h4 class="accommodation-name">{{ item.accommodationName }}</h4>
            <p class="room-name">{{ item.roomName }}</p>
            <p class="guest-name">{{ item.guestName }}</p>
            
            <div v-if="item.note" class="note-box">
              <span class="note-icon">📝</span>
              {{ item.note }}
            </div>
          </div>

          <div class="card-actions">
            <button class="action-btn">
              <!-- Phone Icon -->
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
              전화
            </button>
            <button class="action-btn">
              <!-- Message Icon -->
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
              문자
            </button>
          </div>
        </div>
      </div>

      <div v-if="schedule.length === 0" class="empty-schedule">
        <p>오늘 예정된 스케줄이 없습니다.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-home {
  padding-bottom: 2rem;
}

.view-header {
  margin-bottom: 1.5rem;
}

.view-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.subtitle {
  color: #666;
  font-size: 0.95rem;
  margin-top: 0.25rem;
}

/* Carousel */
.stats-carousel-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.nav-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: white;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-weight: bold;
  font-size: 1rem;
}

.stats-card {
  flex: 1;
  background: white;
  border-radius: 20px;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  border: 1px solid #f0f0f0;
  min-height: 120px;
}

.stats-icon-circle {
  width: 60px;
  height: 60px;
  min-width: 60px; /* Prevent shrinking */
  background: #E0F2F1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #00695C;
}

.stats-content {
  flex: 1;
  min-width: 0; /* Enable truncation if needed */
}

.stats-label {
  font-size: 0.9rem;
  color: #666;
  margin: 0 0 0.25rem;
  white-space: nowrap;
}

.stats-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.25rem;
  white-space: nowrap; 
}

.stats-trend {
  font-size: 0.85rem;
  font-weight: 600;
}

.stats-trend.positive { color: #00C853; }
.stats-trend.neutral { color: #757575; }
.stats-trend.negative { color: #FF5252; }

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 2rem;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
  cursor: pointer;
  transition: background 0.2s;
}

.dot.active {
  background: #00695C;
  width: 24px;
  border-radius: 4px;
}

/* Schedule Section */
.schedule-section {
  background: white;
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  border: 1px solid #f0f0f0;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.schedule-header h3 {
  font-size: 1.1rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.date-label {
  font-size: 0.9rem;
  color: #888;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.schedule-card {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 1.25rem;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.action-badge {
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  color: white;
}

.badge-checkin {
  background: #00875A;
}

.badge-checkout {
  background: #757575;
}

.time-label {
  font-size: 1rem;
  font-weight: 700;
  color: #333;
}

.card-body {
  margin-bottom: 1.25rem;
}

.accommodation-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 0.2rem;
}

.room-name {
  font-size: 0.85rem;
  color: #888;
  margin: 0 0 0.5rem;
}

.guest-name {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 0.5rem;
}

.note-box {
  background: #FFF8E1;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  font-size: 0.85rem;
  color: #F57F17;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.note-icon {
  font-size: 0.9rem;
}

.card-actions {
  display: flex;
  gap: 0.75rem;
}

.action-btn {
  flex: 1;
  padding: 0.6rem;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  color: #333;
  font-size: 0.9rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
}

.action-btn:hover {
  background: #f9f9f9;
}

.empty-schedule {
  text-align: center;
  color: #888;
  padding: 2rem 0;
  font-size: 0.9rem;
}
</style>
