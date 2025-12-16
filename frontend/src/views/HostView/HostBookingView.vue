<script setup>
import { ref, computed } from 'vue'

// Mock Data for Reservations
const reservations = ref([
  {
    id: '0002',
    guestName: '이서연',
    status: 'confirmed', // confirmed, pending, cancelled
    accommodationName: '부산 해운대 오션뷰',
    checkIn: '2025-12-20',
    checkOut: '2025-12-23',
    guests: 4,
    price: 620000
  },
  {
    id: '0007',
    guestName: '윤세아',
    status: 'pending',
    accommodationName: '경주 한옥 스테이',
    checkIn: '2025-12-22',
    checkOut: '2025-12-24',
    guests: 4,
    price: 350000
  },
  {
    id: '0005',
    guestName: '김민수',
    status: 'confirmed',
    accommodationName: '강릉 오션뷰 펜션',
    checkIn: '2025-12-23',
    checkOut: '2025-12-25',
    guests: 2,
    price: 450000
  },
  {
    id: '0008',
    guestName: '박지성',
    status: 'confirmed',
    accommodationName: '제주도 감성 숙소',
    checkIn: '2025-12-23',
    checkOut: '2025-12-26',
    guests: 3,
    price: 580000
  },
  {
    id: '0009',
    guestName: '최현우',
    status: 'pending',
    accommodationName: '부산 해운대 오션뷰',
    checkIn: '2025-12-13',
    checkOut: '2025-12-15',
    guests: 2,
    price: 220000
  }
])

// Calendar Logic
const currentDate = ref(new Date('2025-12-16')) // Default to Dec 2025 for demo as per image
const selectedDate = ref('2025-12-23') // Default selected date

const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())

const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value + 1, 0).getDate()
})

const firstDayOfMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value, 1).getDay()
})

const calendarDays = computed(() => {
  const days = []
  // Empty slots for previous month
  for (let i = 0; i < firstDayOfMonth.value; i++) {
    days.push({ date: '', isEmpty: true })
  }
  // Days of current month
  for (let i = 1; i <= daysInMonth.value; i++) {
    const dateStr = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    days.push({
      day: i,
      date: dateStr,
      isEmpty: false,
      count: getReservationCount(dateStr)
    })
  }
  return days
})

function getReservationCount(date) {
  return reservations.value.filter(r => r.checkIn === date).length
}

function prevMonth() {
  currentDate.value = new Date(currentYear.value, currentMonth.value - 1, 1)
}

function nextMonth() {
  currentDate.value = new Date(currentYear.value, currentMonth.value + 1, 1)
}

function selectDate(date) {
  if (date) {
    selectedDate.value = date
  }
}

const selectedDateReservations = computed(() => {
  if (!selectedDate.value) return []
  return reservations.value.filter(r => r.checkIn === selectedDate.value)
})

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const [y, m, d] = selectedDate.value.split('-')
  return `${m}월 ${d}일`
})

const formatPrice = (price) => {
  return price.toLocaleString()
}

const getStatusLabel = (status) => {
  switch(status) {
    case 'confirmed': return '확정'
    case 'pending': return '대기중'
    case 'cancelled': return '취소'
    default: return status
  }
}
</script>

<template>
  <div class="booking-view">
    <div class="view-header">
      <h2>예약 현황</h2>
      <p class="subtitle">총 {{ reservations.length }}개의 예약</p>
    </div>

    <!-- Calendar View -->
    <div class="calendar-card">
      <div class="calendar-header">
        <h3>{{ currentYear }}년 {{ currentMonth + 1 }}월</h3>
        <div class="month-nav">
          <button class="nav-btn" @click="prevMonth">&lt;</button>
          <button class="nav-btn" @click="nextMonth">&gt;</button>
        </div>
      </div>

      <div class="calendar-grid">
        <div class="weekday" v-for="day in ['일', '월', '화', '수', '목', '금', '토']" :key="day">{{ day }}</div>
        
        <div 
          v-for="(day, index) in calendarDays" 
          :key="index"
          class="calendar-day" 
          :class="{ 
            'empty': day.isEmpty, 
            'selected': day.date === selectedDate,
            'today': day.date === new Date().toISOString().split('T')[0]
          }"
          @click="!day.isEmpty && selectDate(day.date)"
        >
          <div v-if="!day.isEmpty" class="day-content">
            <span class="day-number">{{ day.day }}</span>
            <span v-if="day.count > 0" class="count-badge">{{ day.count }}건</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Reservation Details -->
    <div v-if="selectedDate" class="reservation-details">
      <h3 class="detail-title">{{ selectedDateLabel }} 예약 내역</h3>
      
      <div v-if="selectedDateReservations.length > 0" class="reservation-list">
        <div v-for="res in selectedDateReservations" :key="res.id" class="reservation-card">
          <div class="card-header">
            <span class="res-id">#{{ res.id }}</span>
            <span class="res-status" :class="res.status">{{ getStatusLabel(res.status) }}</span>
          </div>
          
          <h4 class="guest-name">{{ res.guestName }}</h4>
          
          <div class="res-info">
            <div class="info-item">
              {{ res.accommodationName }}
            </div>
            <div class="info-item">
              {{ res.checkIn }} ~ {{ res.checkOut }}
            </div>
            <div class="info-item">
              {{ res.guests }}명
            </div>
          </div>
          
          <div class="card-footer">
            <span class="res-price-label">결제금액</span>
            <span class="res-price">₩{{ formatPrice(res.price) }}</span>
          </div>
        </div>
      </div>
      
      <div v-else class="no-reservation">
        <p>해당 날짜에 체크인 예정인 예약이 없습니다.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.booking-view {
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

/* View Toggle */
.view-toggle {
  display: flex;
  background: white;
  padding: 0.3rem;
  border-radius: 12px;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.toggle-btn {
  flex: 1;
  border: none;
  background: none;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 0.95rem;
  color: #666;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.toggle-btn.active {
  background: white;
  color: #333;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.icon {
  font-family: 'Material Icons', sans-serif;
  font-size: 1.1rem;
}

/* Calendar Card */
.calendar-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  margin-bottom: 2rem;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.calendar-header h3 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.month-nav {
  display: flex;
  gap: 0.5rem;
}

.nav-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.5rem;
  text-align: center;
}

.weekday {
  font-size: 0.85rem;
  color: #888;
  padding-bottom: 0.5rem;
}

.calendar-day {
  aspect-ratio: 1;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  padding: 0.25rem;
  cursor: pointer;
  transition: all 0.2s;
}

.calendar-day.empty {
  border: none;
  cursor: default;
}

.calendar-day:not(.empty):hover {
  border-color: #BFE7DF;
}

.calendar-day.selected {
  border: 2px solid #7de0ccc7;
}

.day-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.day-number {
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  align-self: flex-start;
  padding-left: 0.2rem;
}

.count-badge {
  background: #07b99c;
  color: white;
  font-size: 0.7rem;
  padding: 0.2rem 0;
  border-radius: 4px;
  font-weight: 600;
}

/* Reservation Details */
.detail-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 1rem;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.reservation-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  border: 1px solid #f0f0f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.res-id {
  font-size: 0.85rem;
  color: #888;
}

.res-status {
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
}

.res-status.confirmed {
  background: #07b99c;
  color: white;
}

.res-status.pending {
  background: #f5f5f5;
  color: #666;
}

.res-status.cancelled {
  background: #ffebee;
  color: #c62828;
}

.guest-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 1rem;
}

.res-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #666;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 1rem;
}

.res-price-label {
  font-size: 0.9rem;
  color: #888;
}

.res-price {
  font-size: 1.1rem;
  font-weight: 700;
  color: #333;
}

.no-reservation {
  text-align: center;
  padding: 3rem;
  background: white;
  border-radius: 16px;
  color: #888;
}
</style>
