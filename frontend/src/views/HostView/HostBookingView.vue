<script setup>
import { ref, computed } from 'vue'

const bookings = ref([
  { id: 1, guestName: '김민수', guestPhone: '010-1234-5678', guestEmail: 'minsu.kim@email.com', property: '제주도 감성 숙소', checkIn: '2025-12-15', checkOut: '2025-12-17', guests: 2, amount: 320000, status: '확정' },
  { id: 2, guestName: '이서연', guestPhone: '010-2345-6789', guestEmail: 'seoyeon.lee@email.com', property: '부산 해운대 오션뷰', checkIn: '2025-12-20', checkOut: '2025-12-23', guests: 4, amount: 620000, status: '확정' },
  { id: 3, guestName: '박지훈', guestPhone: '010-3456-7890', guestEmail: 'jihun.park@email.com', property: '서울 강남 모던하우스', checkIn: '2025-12-18', checkOut: '2025-12-19', guests: 1, amount: 95000, status: '대기중' },
  { id: 4, guestName: '최유진', guestPhone: '010-4567-8901', guestEmail: 'yujin.choi@email.com', property: '경주 한옥 스테이', checkIn: '2025-12-25', checkOut: '2025-12-27', guests: 3, amount: 325000, status: '확정' },
  { id: 5, guestName: '정대호', guestPhone: '010-5678-9012', guestEmail: 'daeho.jung@email.com', property: '제주도 감성 숙소', checkIn: '2025-12-12', checkOut: '2025-12-14', guests: 2, amount: 240000, status: '체크인' },
  { id: 6, guestName: '한소희', guestPhone: '010-6789-0123', guestEmail: 'sohee.han@email.com', property: '부산 해운대 오션뷰', checkIn: '2025-12-28', checkOut: '2025-12-30', guests: 5, amount: 535000, status: '확정' },
  { id: 7, guestName: '윤세아', guestPhone: '010-7890-1234', guestEmail: 'sea.yoon@email.com', property: '경주 한옥 스테이', checkIn: '2025-12-22', checkOut: '2025-12-24', guests: 4, amount: 280000, status: '대기중' }
])

const statusColor = {
  확정: 'badge-green',
  대기중: 'badge-gray',
  체크인: 'badge-outline'
}

const activeTab = ref('list')
const selectedDate = ref(null)
const selectedBooking = ref(null)
const showModal = ref(false)

const currentMonth = ref(new Date(2025, 11, 1)) // 2025-12

const daysInMonth = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 0).getDate())
const firstDay = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), 1).getDay())

const calendarCells = computed(() => {
  const cells = []
  for (let i = 0; i < firstDay.value; i++) cells.push({ empty: true })
  for (let d = 1; d <= daysInMonth.value; d++) {
    const dateObj = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), d)
    const dateStr = dateObj.toISOString().split('T')[0]
    const count = bookings.value.filter(b => dateObj >= new Date(b.checkIn) && dateObj <= new Date(b.checkOut)).length
    cells.push({ empty: false, day: d, dateStr, count })
  }
  return cells
})

const selectedDateBookings = computed(() => {
  if (!selectedDate.value) return []
  const target = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), selectedDate.value)
  return bookings.value.filter(b => target >= new Date(b.checkIn) && target <= new Date(b.checkOut))
})

const prevMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
  selectedDate.value = null
}

const nextMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
  selectedDate.value = null
}

const formatAmount = (n) => `₩${n.toLocaleString()}`

const openModal = (booking) => {
  selectedBooking.value = booking
  showModal.value = true
}
</script>

<template>
  <div class="booking-page">
    <header class="view-header">
      <div>
        <p class="eyebrow">예약 관리</p>
        <h2>예약 현황</h2>
        <p class="subtitle">총 {{ bookings.length }}건 · 최신순</p>
      </div>
      <div class="tab-switch">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'list' }"
          @click="activeTab = 'list'"
        >목록</button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'calendar' }"
          @click="activeTab = 'calendar'"
        >캘린더</button>
      </div>
    </header>

    <!-- List view -->
    <section v-if="activeTab === 'list'" class="list-section">
      <!-- Mobile cards -->
      <div class="mobile-cards">
        <article v-for="booking in bookings" :key="booking.id" class="mobile-card">
          <div class="card-top">
            <div>
              <p class="muted">#{{ booking.id.toString().padStart(4, '0') }}</p>
              <h3>{{ booking.guestName }}</h3>
            </div>
            <span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span>
          </div>
          <p class="property">{{ booking.property }}</p>
          <p class="period">{{ booking.checkIn }} ~ {{ booking.checkOut }} · {{ booking.guests }}명</p>
          <p class="amount">{{ formatAmount(booking.amount) }}</p>
          <button class="ghost-btn" @click="openModal(booking)">상세 보기</button>
        </article>
      </div>

      <!-- Desktop table -->
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>예약번호</th>
              <th>게스트</th>
              <th>숙소</th>
              <th>체크인</th>
              <th>체크아웃</th>
              <th>인원</th>
              <th>금액</th>
              <th>상태</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="booking in bookings" :key="booking.id">
              <td>#{{ booking.id.toString().padStart(4, '0') }}</td>
              <td>{{ booking.guestName }}</td>
              <td>{{ booking.property }}</td>
              <td>{{ booking.checkIn }}</td>
              <td>{{ booking.checkOut }}</td>
              <td>{{ booking.guests }}명</td>
              <td class="strong">{{ formatAmount(booking.amount) }}</td>
              <td>
                <span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span>
              </td>
              <td><button class="ghost-btn" @click="openModal(booking)">보기</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Calendar view -->
    <section v-else class="calendar-section">
      <div class="calendar">
        <div class="cal-head">
          <h3>{{ currentMonth.getFullYear() }}년 {{ currentMonth.getMonth() + 1 }}월</h3>
          <div class="cal-nav">
            <button class="circle-btn" @click="prevMonth">‹</button>
            <button class="circle-btn" @click="nextMonth">›</button>
          </div>
        </div>
        <div class="weekdays">
          <span v-for="day in ['일','월','화','수','목','금','토']" :key="day">{{ day }}</span>
        </div>
        <div class="grid">
          <div
            v-for="(cell, idx) in calendarCells"
            :key="idx"
            class="cell"
            :class="{ empty: cell.empty, selected: !cell.empty && selectedDate === cell.day }"
            @click="!cell.empty && (selectedDate = cell.day)"
          >
            <span v-if="!cell.empty" class="day">{{ cell.day }}</span>
            <span v-if="!cell.empty && cell.count" class="badge">{{ cell.count }}건</span>
          </div>
        </div>
      </div>

      <div class="date-panel">
        <h4>{{ selectedDate ? `${currentMonth.getMonth() + 1}월 ${selectedDate}일 예약` : '날짜를 선택하세요' }}</h4>
        <div v-if="selectedDate && selectedDateBookings.length" class="date-list">
          <article v-for="booking in selectedDateBookings" :key="booking.id" class="date-card" @click="openModal(booking)">
            <div class="card-top">
              <div>
                <p class="muted">#{{ booking.id.toString().padStart(4, '0') }}</p>
                <h5>{{ booking.guestName }}</h5>
              </div>
              <span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span>
            </div>
            <p class="property">{{ booking.property }}</p>
            <p class="period">{{ booking.checkIn }} ~ {{ booking.checkOut }}</p>
            <p class="amount">{{ formatAmount(booking.amount) }}</p>
          </article>
        </div>
        <div v-else class="empty-box">
          <p>예약이 없습니다.</p>
        </div>
      </div>
    </section>

    <!-- Modal -->
    <div v-if="showModal && selectedBooking" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal">
        <header class="modal-head">
          <div>
            <p class="muted">예약 #{{ selectedBooking.id.toString().padStart(4, '0') }}</p>
            <h3>{{ selectedBooking.guestName }}</h3>
          </div>
          <button class="circle-btn" @click="showModal = false">×</button>
        </header>
        <div class="modal-body">
          <div class="modal-row"><span>숙소</span><strong>{{ selectedBooking.property }}</strong></div>
          <div class="modal-row"><span>체크인</span><strong>{{ selectedBooking.checkIn }}</strong></div>
          <div class="modal-row"><span>체크아웃</span><strong>{{ selectedBooking.checkOut }}</strong></div>
          <div class="modal-row"><span>인원</span><strong>{{ selectedBooking.guests }}명</strong></div>
          <div class="modal-row"><span>연락처</span><strong>{{ selectedBooking.guestPhone }}</strong></div>
          <div class="modal-row"><span>이메일</span><strong>{{ selectedBooking.guestEmail }}</strong></div>
          <div class="modal-row"><span>금액</span><strong>{{ formatAmount(selectedBooking.amount) }}</strong></div>
          <div class="modal-row"><span>상태</span><span class="pill" :class="statusColor[selectedBooking.status]">{{ selectedBooking.status }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.booking-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.eyebrow {
  margin: 0;
  color: #0f766e;
  font-weight: 700;
  font-size: 0.9rem;
}

.view-header h2 {
  margin: 0.2rem 0;
  font-size: 1.6rem;
  font-weight: 800;
  color: #0b3b32;
}

.subtitle {
  margin: 0;
  color: #6b7280;
}

.tab-switch {
  display: inline-flex;
  background: #eef2f3;
  border-radius: 12px;
  padding: 0.15rem;
}

.tab-btn {
  border: none;
  background: transparent;
  padding: 0.55rem 0.9rem;
  border-radius: 10px;
  font-weight: 700;
  color: #4b5563;
}

.tab-btn.active {
  background: white;
  color: #0f766e;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
}

.list-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.mobile-cards {
  display: grid;
  gap: 0.75rem;
}

.mobile-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 1rem;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.muted {
  color: #9ca3af;
  margin: 0;
  font-size: 0.9rem;
}

.mobile-card h3 {
  margin: 0.15rem 0 0;
  font-size: 1.1rem;
  color: #0f172a;
}

.property {
  margin: 0.4rem 0 0.15rem;
  color: #374151;
  font-weight: 600;
}

.period {
  margin: 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.amount {
  margin: 0.35rem 0;
  font-weight: 800;
  color: #0f172a;
}

.ghost-btn {
  width: 100%;
  border: 1px solid #d1d5db;
  background: transparent;
  color: #0f766e;
  border-radius: 10px;
  padding: 0.55rem;
  font-weight: 700;
  cursor: pointer;
}

.table-wrap {
  display: none;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}

th, td {
  padding: 0.85rem 1rem;
  text-align: left;
}

th {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  font-size: 0.95rem;
}

tbody tr:nth-child(every) {
  background: white;
}

tbody tr {
  border-top: 1px solid #e5e7eb;
}

td {
  color: #111827;
}

.strong {
  font-weight: 800;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 700;
  border: 1px solid #e5e7eb;
}

.badge-green {
  background: #e0f2f1;
  color: #0f766e;
  border-color: #c0e6df;
}

.badge-gray {
  background: #f1f5f9;
  color: #475569;
}

.badge-outline {
  background: white;
  color: #111827;
}

.calendar-section {
  display: grid;
  gap: 1rem;
}

.calendar {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 1rem;
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.cal-head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
}

.cal-nav {
  display: flex;
  gap: 0.35rem;
}

.circle-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  background: white;
  cursor: pointer;
  font-weight: 700;
  color: #0f172a;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: #6b7280;
  font-weight: 700;
  margin-bottom: 0.35rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.35rem;
}

.cell {
  min-height: 72px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0.4rem;
  position: relative;
  background: #f9fafb;
  cursor: pointer;
}

.cell.empty {
  background: transparent;
  border: none;
  cursor: default;
}

.cell.selected {
  border-color: #0f766e;
  background: #e0f2f1;
}

.day {
  font-weight: 700;
  color: #111827;
}

.badge {
  position: absolute;
  bottom: 0.35rem;
  right: 0.35rem;
  background: #0f766e;
  color: white;
  border-radius: 999px;
  padding: 0.15rem 0.45rem;
  font-size: 0.8rem;
  font-weight: 700;
}

.date-panel {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 1rem;
}

.date-panel h4 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  font-weight: 700;
}

.date-list {
  display: grid;
  gap: 0.65rem;
}

.date-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 0.85rem;
  background: #f9fafb;
  cursor: pointer;
  transition: border 0.2s ease;
}

.date-card:hover {
  border-color: #0f766e;
}

.date-card h5 {
  margin: 0.1rem 0 0;
  font-size: 1rem;
}

.empty-box {
  text-align: center;
  color: #6b7280;
  padding: 1rem 0;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 50;
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 1.25rem;
  width: min(560px, 100%);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-head h3 {
  margin: 0.2rem 0 0;
}

.modal-body {
  margin-top: 1rem;
  display: grid;
  gap: 0.65rem;
}

.modal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.modal-row span:first-child {
  color: #6b7280;
}

@media (min-width: 768px) {
  .mobile-cards {
    display: none;
  }

  .table-wrap {
    display: block;
  }
}

@media (min-width: 1024px) {
  .calendar-section {
    grid-template-columns: 2fr 1fr;
    align-items: start;
  }
}
</style>
