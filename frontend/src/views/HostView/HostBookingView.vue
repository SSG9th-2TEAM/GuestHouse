<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { fetchHostBookings, fetchHostBookingCalendar } from '@/api/hostBooking'

const bookings = ref([])
const calendarBookings = ref([])
const isLoading = ref(false)
const loadError = ref('')

const statusColor = {
  확정: 'badge-green',
  대기중: 'badge-gray',
  체크인: 'badge-outline'
}

const activeTab = ref('list')
const selectedDate = ref(null)
const selectedBooking = ref(null)
const showModal = ref(false)

const currentMonth = ref(new Date())

const daysInMonth = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 0).getDate())
const firstDay = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), 1).getDay())

const calendarCells = computed(() => {
  const cells = []
  for (let i = 0; i < firstDay.value; i++) cells.push({ empty: true })
  for (let d = 1; d <= daysInMonth.value; d++) {
    const dateObj = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), d)
    const dateStr = dateObj.toISOString().split('T')[0]
    const count = calendarBookings.value.filter(
      b => dateObj >= new Date(b.checkIn) && dateObj <= new Date(b.checkOut)
    ).length
    cells.push({ empty: false, day: d, dateStr, count })
  }
  return cells
})

const selectedDateBookings = computed(() => {
  if (!selectedDate.value) return []
  const target = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), selectedDate.value)
  return calendarBookings.value.filter(
    b => target >= new Date(b.checkIn) && target <= new Date(b.checkOut)
  )
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

const normalizeStatus = (status) => {
  const value = String(status ?? '').toLowerCase()
  if (value === 'confirmed' || value === '확정' || value === '2') return '확정'
  if (value === 'pending' || value === '대기' || value === '대기중' || value === '1') return '대기중'
  if (value === 'checkin' || value === '체크인' || value === '3') return '체크인'
  return status || '대기중'
}

const normalizeBooking = (item) => ({
  id: item.bookingId ?? item.reservationId ?? item.id,
  guestName: item.guestName ?? item.reserverName ?? item.name ?? '',
  guestPhone: item.guestPhone ?? item.reserverPhone ?? item.phone ?? '',
  guestEmail: item.guestEmail ?? item.email ?? '',
  property: item.accommodationName ?? item.property ?? '',
  checkIn: item.checkin ?? item.checkIn ?? '',
  checkOut: item.checkout ?? item.checkOut ?? '',
  guests: item.guestCount ?? item.guests ?? 0,
  amount: item.finalPaymentAmount ?? item.amount ?? item.totalAmount ?? 0,
  status: normalizeStatus(item.status ?? item.reservationStatus)
})

const loadBookings = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchHostBookings()
  if (response.ok) {
    const payload = response.data
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    bookings.value = list.map(normalizeBooking)
  } else {
    loadError.value = '예약 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const loadCalendar = async (month) => {
  const response = await fetchHostBookingCalendar(month)
  if (response.ok) {
    const payload = response.data
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    calendarBookings.value = list.map(normalizeBooking)
  }
}

const toMonthParam = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

onMounted(async () => {
  await loadBookings()
  await loadCalendar(toMonthParam(currentMonth.value))
})

watch(currentMonth, (value) => {
  selectedDate.value = null
  loadCalendar(toMonthParam(value))
})
</script>

<template>
  <div class="booking-page">
    <header class="view-header">
      <div>
        <h2>예약 관리</h2>
        <p class="subtitle">총 {{ bookings.length }}건 · 최신순</p>
      </div>

      <div class="tab-switch">
        <button class="tab-btn" :class="{ active: activeTab === 'list' }" @click="activeTab = 'list'">목록</button>
        <button class="tab-btn" :class="{ active: activeTab === 'calendar' }" @click="activeTab = 'calendar'">캘린더</button>
      </div>
    </header>

    <section v-if="activeTab === 'list'" class="list-section">
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

      <div class="table-wrap">
        <table>
          <thead>
          <tr>
            <th>예약번호</th><th>게스트</th><th>숙소</th><th>체크인</th><th>체크아웃</th><th>인원</th><th>금액</th><th>상태</th><th></th>
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
            <td><span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span></td>
            <td><button class="ghost-btn" @click="openModal(booking)">보기</button></td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>

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
            :class="{
              empty: cell.empty,
              selected: !cell.empty && selectedDate === cell.day,
              'has-booking': !cell.empty && cell.count
            }"
            @click="!cell.empty && (selectedDate = cell.day)"
          >
            <span v-if="!cell.empty" class="day">{{ cell.day }}</span>
            <span v-if="!cell.empty && cell.count" class="count-chip">{{ cell.count }}건</span>
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

        <div v-else class="empty-box"><p>예약이 없습니다.</p></div>
      </div>
    </section>

    <p v-if="isLoading" class="empty-box">예약 데이터를 불러오는 중입니다.</p>
    <p v-else-if="loadError" class="empty-box">{{ loadError }}</p>

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
  padding-bottom: 2rem;
}

.view-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}

.view-header h2 {
  margin: 0.15rem 0 0.2rem;
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--host-title, #0b3b32);
  letter-spacing: -0.01em;
}

.subtitle {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

.tab-switch {
  display: inline-flex;
  width: 100%;
  background: #eef2f3;
  border-radius: 12px;
  padding: 0.15rem;
  white-space: nowrap;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0.6rem 0.9rem;
  border-radius: 10px;
  font-weight: 900;
  color: #4b5563;
}

.tab-btn.active {
  background: white;
  color: var(--host-accent, #0f766e);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
}

.mobile-cards {
  display: grid;
  gap: 0.75rem;
}

.mobile-card {
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 14px;
  padding: 1rem;
  background: var(--bg-white, #fff);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.muted {
  color: #9ca3af;
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
}

.mobile-card h3 {
  margin: 0.1rem 0 0;
  font-size: 1.12rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.property {
  margin: 0.45rem 0 0.15rem;
  color: #374151;
  font-weight: 900;
}

.period {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.95rem;
  font-weight: 600;
}

.amount {
  margin: 0.45rem 0;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  font-size: 1.05rem;
}

.ghost-btn {
  width: 100%;
  border: 1px solid #d1d5db;
  background: transparent;
  color: var(--host-accent, #0f766e);
  border-radius: 10px;
  padding: 0.6rem;
  font-weight: 900;
  cursor: pointer;
}

.table-wrap {
  display: none;
  background: var(--bg-white, #fff);
  border: 1px solid var(--border, #e5e7eb);
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
  font-weight: 900;
  font-size: 0.95rem;
}

tbody tr { border-top: 1px solid var(--border, #e5e7eb); }
td { color: #111827; }
.strong { font-weight: 900; }

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 900;
  border: 1px solid var(--border, #e5e7eb);
  white-space: nowrap;
}

.badge-green {
  background: #e0f2f1;
  color: var(--host-accent, #0f766e);
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

.calendar,
.date-panel {
  background: var(--bg-white, #fff);
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 14px;
  padding: 1rem;
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.cal-head h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.cal-nav { display: flex; gap: 0.35rem; }

.circle-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border, #e5e7eb);
  background: white;
  cursor: pointer;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: var(--text-sub, #6b7280);
  font-weight: 900;
  margin-bottom: 0.35rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.35rem;
}

.cell {
  min-height: 68px;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 10px;
  padding: 0.4rem;
  position: relative;
  background: #f9fafb;
  cursor: pointer;
}

.cell.empty { background: transparent; border: none; cursor: default; }

.cell.selected {
  border-color: var(--host-accent, #0f766e);
  background: #e0f2f1;
}

.cell.has-booking {
  border-color: #bfe7df;
  background: #f6fffb;
}

.day { font-weight: 900; color: #111827; }

.count-chip {
  position: absolute;
  left: 0.35rem;
  bottom: 0.3rem;
  border: 1px solid #bfe7df;
  color: #0f766e;
  background: #fff;
  border-radius: 999px;
  padding: 0.05rem 0.35rem;
  font-size: 0.72rem;
  font-weight: 800;
}

.date-panel h4 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.date-list {
  display: grid;
  gap: 0.65rem;
}

.date-card {
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  padding: 0.85rem;
  background: #f9fafb;
  cursor: pointer;
}

.date-card:hover { border-color: var(--host-accent, #0f766e); }

.date-card h5 {
  margin: 0.1rem 0 0;
  font-size: 1rem;
  font-weight: 900;
}

.empty-box {
  text-align: center;
  color: var(--text-sub, #6b7280);
  padding: 1rem 0;
  font-weight: 700;
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

.modal-head h3 { margin: 0.2rem 0 0; font-weight: 900; }

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
  color: var(--text-sub, #6b7280);
  font-weight: 700;
}

@media (min-width: 768px) {
  .view-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
  .tab-switch { width: auto; }
  .mobile-cards { display: none; }
  .table-wrap { display: block; }
}

@media (min-width: 1024px) {
  .calendar-section {
    grid-template-columns: 2fr 1fr;
    align-items: start;
  }
}
</style>
