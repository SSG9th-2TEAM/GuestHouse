<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchHostBookings, fetchHostBookingCalendar } from '@/api/hostBooking'
import { formatCurrency, formatDate, formatDateRange, formatDateTime } from '@/utils/formatters'

const bookings = ref([])
const calendarBookings = ref([])
const isLoading = ref(false)
const loadError = ref('')

const statusColor = {
  요청: 'badge-gray',
  확정: 'badge-green',
  '체크인 완료': 'badge-outline',
  취소: 'badge-red'
}

const paymentBadge = {
  0: '미결제',
  2: '결제 실패',
  3: '환불 완료'
}

const route = useRoute()
const router = useRouter()
const activeTab = computed(() => {
  return route.query.view === 'calendar' ? 'calendar' : 'list'
})
const selectedDate = ref(null)
const selectedBooking = ref(null)
const showModal = ref(false)

const currentMonth = ref(new Date())

const statusFilters = [
  { value: 'all', label: '전체', statuses: [1, 2, 3, 9] },
  { value: 'pending', label: '요청', statuses: [1] },
  { value: 'confirmed', label: '확정', statuses: [2] },
  { value: 'checkin', label: '체크인 완료', statuses: [3] },
  { value: 'canceled', label: '취소', statuses: [9] }
]

const sortOptions = [
  { value: 'latest', label: '최신순' },
  { value: 'checkin', label: '체크인 임박순' },
  { value: 'checkout', label: '체크아웃 임박순' }
]

const selectedStatus = ref('all')
const selectedSort = ref('latest')

const daysInMonth = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 0).getDate())
const firstDay = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), 1).getDay())

const toDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatSchedule = (booking, includeYear = false) => {
  const checkIn = formatDateTime(booking.checkIn, includeYear)
  const checkOut = formatDateTime(booking.checkOut, includeYear)
  let nights = booking.stayNights ?? 0
  if (!nights) {
    const start = toDate(booking.checkIn)
    const end = toDate(booking.checkOut)
    if (start && end) {
      const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      const endDate = new Date(end.getFullYear(), end.getMonth(), end.getDate())
      const diff = Math.round((endDate - startDate) / (1000 * 60 * 60 * 24))
      nights = Math.max(1, diff)
    }
  }
  return `${checkIn} → ${checkOut} · ${nights}박`
}

const formatScheduleRange = (booking, includeYear = false) => {
  return formatDateRange(booking.checkIn, booking.checkOut, includeYear)
}

const formatBookingMeta = (booking) => {
  let nights = booking.stayNights ?? 0
  if (!nights || nights <= 0) {
    const start = toDate(booking.checkIn)
    const end = toDate(booking.checkOut)
    if (start && end) {
      const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      const endDate = new Date(end.getFullYear(), end.getMonth(), end.getDate())
      const diff = Math.round((endDate - startDate) / (1000 * 60 * 60 * 24))
      nights = Math.max(1, diff)
    } else {
      nights = 1
    }
  }
  return `${nights}박 · ${booking.guests}명`
}

const isSameDate = (a, b) => {
  if (!a || !b) return false
  return a.getFullYear() === b.getFullYear()
    && a.getMonth() === b.getMonth()
    && a.getDate() === b.getDate()
}

const calendarCells = computed(() => {
  const cells = []
  for (let i = 0; i < firstDay.value; i++) cells.push({ empty: true })
  for (let d = 1; d <= daysInMonth.value; d++) {
    const dateObj = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), d)
    const dateStr = dateObj.toISOString().split('T')[0]
    const count = calendarBookings.value.filter((booking) => {
      if (booking.reservationStatus === 0) return false
      const checkIn = toDate(booking.checkIn)
      const checkOut = toDate(booking.checkOut)
      return checkIn && checkOut && dateObj >= checkIn && dateObj <= checkOut
    }).length
    cells.push({ empty: false, day: d, dateStr, count })
  }
  return cells
})

const selectedDateBookings = computed(() => {
  if (!selectedDate.value) return []
  const target = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), selectedDate.value)
  return calendarBookings.value.filter((booking) => {
    if (booking.reservationStatus === 0) return false
    const checkIn = toDate(booking.checkIn)
    const checkOut = toDate(booking.checkOut)
    return checkIn && checkOut && target >= checkIn && target <= checkOut
  })
})

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const date = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), selectedDate.value)
  return formatDate(date, true)
})

const prevMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
  selectedDate.value = null
}
const nextMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
  selectedDate.value = null
}

const formatAmount = (n) => formatCurrency(n)

const openModal = (booking) => {
  selectedBooking.value = booking
  showModal.value = true
}

const resetFilters = () => {
  selectedStatus.value = 'all'
  selectedSort.value = 'latest'
}

const normalizeStatus = (status) => {
  const numeric = Number(status)
  if (numeric === 1) return '요청'
  if (numeric === 2) return '확정'
  if (numeric === 3) return '체크인 완료'
  if (numeric === 9) return '취소'
  const value = String(status ?? '').toLowerCase()
  if (value.includes('확정') || value === 'confirmed') return '확정'
  if (value.includes('요청') || value.includes('대기') || value === 'pending') return '요청'
  if (value.includes('체크인')) return '체크인 완료'
  if (value.includes('취소') || value === 'cancelled') return '취소'
  return '요청'
}

const normalizeNumber = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const normalizeBooking = (item) => ({
  id: item.bookingId ?? item.reservationId ?? item.id,
  reservationStatus: normalizeNumber(item.reservationStatus ?? item.status),
  paymentStatus: normalizeNumber(item.paymentStatus ?? item.paymentStatusCode ?? item.payment),
  guestName: item.guestName ?? item.reserverName ?? item.name ?? '',
  guestPhone: item.guestPhone ?? item.reserverPhone ?? item.phone ?? '',
  guestEmail: item.guestEmail ?? item.email ?? '',
  property: item.accommodationName ?? item.property ?? '',
  checkIn: item.checkin ?? item.checkIn ?? '',
  checkOut: item.checkout ?? item.checkOut ?? '',
  guests: item.guestCount ?? item.guests ?? 0,
  stayNights: normalizeNumber(item.stayNights ?? item.stayNightsCount),
  amount: item.finalPaymentAmount ?? item.amount ?? item.totalAmount ?? 0,
  status: normalizeStatus(item.reservationStatus ?? item.status),
  createdAt: item.createdAt ?? item.created_at ?? ''
})

const normalizeSortQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  if (normalized === 'checkinsoon' || normalized === 'checkin') return 'checkin'
  if (normalized === 'checkoutsoon' || normalized === 'checkout') return 'checkout'
  if (normalized === 'latest') return 'latest'
  return null
}

const normalizeStatusQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  if (!normalized) return null
  const matched = statusFilters.find((item) => item.value === normalized)
  if (matched) return matched.value
  if (normalized.includes('confirm')) return 'confirmed'
  if (normalized.includes('checkin')) return 'checkin'
  if (normalized.includes('cancel')) return 'canceled'
  if (normalized.includes('request') || normalized.includes('pending')) return 'pending'
  return null
}

const filteredBookings = computed(() => {
  const base = bookings.value.filter((booking) => booking.reservationStatus !== 0)
  const filter = statusFilters.find((item) => item.value === selectedStatus.value)
  const filtered = filter ? base.filter((booking) => filter.statuses.includes(booking.reservationStatus)) : base

  if (selectedSort.value === 'checkin') {
    return [...filtered].sort((a, b) => {
      return (toDate(a.checkIn)?.getTime() ?? 0) - (toDate(b.checkIn)?.getTime() ?? 0)
    })
  }
  if (selectedSort.value === 'checkout') {
    return [...filtered].sort((a, b) => {
      return (toDate(a.checkOut)?.getTime() ?? 0) - (toDate(b.checkOut)?.getTime() ?? 0)
    })
  }
  return [...filtered].sort((a, b) => {
    return (toDate(b.createdAt)?.getTime() ?? 0) - (toDate(a.createdAt)?.getTime() ?? 0)
  })
})

const calendarLegend = computed(() => '표시된 건수는 해당 날짜에 포함된 예약 수입니다.')

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
  } else {
    loadError.value = '예약 캘린더를 불러오지 못했습니다.'
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

watch(
  () => route.query,
  (query) => {
    if (!query.view) {
      router.replace({ query: { ...query, view: 'list' } })
      return
    }
    const sort = normalizeSortQuery(query.sort)
    if (sort) selectedSort.value = sort
    const status = normalizeStatusQuery(query.status)
    if (status) selectedStatus.value = status
  },
  { immediate: true }
)

const syncQuery = (next) => {
  const current = route.query
  const normalized = {
    ...current,
    ...next
  }
  const keys = Object.keys(normalized)
  const isSame = keys.every((key) => String(normalized[key] ?? '') === String(current[key] ?? ''))
  if (!isSame) {
    router.replace({ query: normalized })
  }
}

watch(selectedStatus, (value) => {
  syncQuery({ status: value })
})

watch(selectedSort, (value) => {
  syncQuery({ sort: value })
})

const setView = (view) => {
  if (view === activeTab.value) return
  router.replace({ query: { ...route.query, view } })
}
</script>

<template>
  <div class="booking-page">
    <header class="view-header">
      <div>
        <h2>예약 관리</h2>
        <p class="subtitle">총 {{ filteredBookings.length }}건 · {{ sortOptions.find(item => item.value === selectedSort)?.label }}</p>
      </div>

      <div class="tab-switch" role="tablist" aria-label="예약 보기 전환">
        <button
          class="tab-btn host-chip"
          :class="{ 'host-chip--active': activeTab === 'list' }"
          role="tab"
          :aria-selected="activeTab === 'list'"
          :aria-pressed="activeTab === 'list'"
          type="button"
          @click="setView('list')"
        >
          목록
        </button>
        <button
          class="tab-btn host-chip"
          :class="{ 'host-chip--active': activeTab === 'calendar' }"
          role="tab"
          :aria-selected="activeTab === 'calendar'"
          :aria-pressed="activeTab === 'calendar'"
          type="button"
          @click="setView('calendar')"
        >
          캘린더
        </button>
      </div>
    </header>

    <section v-if="activeTab === 'list'" class="list-section">
      <div class="filter-row">
        <div class="filter-chips">
          <button
            v-for="filter in statusFilters"
            :key="filter.value"
            class="filter-chip host-chip"
            :class="{ 'host-chip--active': selectedStatus === filter.value }"
            type="button"
            @click="selectedStatus = filter.value"
          >
            {{ filter.label }}
          </button>
        </div>
        <select v-model="selectedSort" class="sort-select" aria-label="정렬 선택">
          <option v-for="option in sortOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>

      <div v-if="loadError" class="empty-box">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" @click="loadBookings">다시 시도</button>
      </div>

      <div class="mobile-cards">
        <div v-if="!loadError && !filteredBookings.length && !isLoading" class="empty-box">
          <p>조건에 맞는 예약이 없습니다.</p>
          <button class="ghost-btn" @click="resetFilters">필터 초기화</button>
        </div>
        <article v-for="(booking, index) in filteredBookings" :key="booking.id" class="mobile-card fade-item" :style="{ animationDelay: `${Math.min(index, 5) * 70}ms` }">
          <div class="card-top">
            <div>
              <p class="muted">#{{ booking.id.toString().padStart(4, '0') }}</p>
              <h3>{{ booking.guestName }}</h3>
            </div>
            <span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span>
          </div>
          <div class="payment-chip" v-if="paymentBadge[booking.paymentStatus]">
            {{ paymentBadge[booking.paymentStatus] }}
          </div>
          <p class="property">{{ booking.property }}</p>
          <p class="period">{{ formatScheduleRange(booking) }}</p>
          <p class="period-meta">{{ formatBookingMeta(booking) }}</p>
          <p class="amount">{{ formatAmount(booking.amount) }}</p>
          <button class="ghost-btn" @click="openModal(booking)">예약 상세</button>
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
          <tr v-for="booking in filteredBookings" :key="booking.id">
            <td>#{{ booking.id.toString().padStart(4, '0') }}</td>
            <td>{{ booking.guestName }}</td>
            <td>{{ booking.property }}</td>
            <td>{{ formatDateTime(booking.checkIn) }}</td>
            <td>{{ formatDateTime(booking.checkOut) }}</td>
            <td>{{ booking.guests }}명</td>
            <td class="strong">{{ formatAmount(booking.amount) }}</td>
            <td><span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span></td>
            <td><button class="ghost-btn" @click="openModal(booking)">예약 상세</button></td>
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

        <p class="calendar-legend">{{ calendarLegend }}</p>

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
            @keydown.enter="!cell.empty && (selectedDate = cell.day)"
            @keydown.space.prevent="!cell.empty && (selectedDate = cell.day)"
            :role="cell.empty ? undefined : 'button'"
            :tabindex="cell.empty ? -1 : 0"
          >
            <span v-if="!cell.empty" class="day">{{ cell.day }}</span>
            <span v-if="!cell.empty && cell.count" class="count-chip">
              <span class="count-number">{{ cell.count }}</span>
              <span class="count-unit">건</span>
            </span>
          </div>
        </div>
      </div>

      <div class="date-panel">
        <h4>{{ selectedDate ? `${selectedDateLabel} 예약` : '날짜를 선택하세요' }}</h4>
        <div v-if="loadError" class="empty-box">
          <p>데이터를 불러오지 못했어요.</p>
          <button class="ghost-btn" @click="loadCalendar(toMonthParam(currentMonth))">다시 시도</button>
        </div>
        <div v-else-if="selectedDate && selectedDateBookings.length" class="date-list">
          <article v-for="(booking, index) in selectedDateBookings" :key="booking.id" class="date-card fade-item"
                   :style="{ animationDelay: `${Math.min(index, 5) * 60}ms` }" @click="openModal(booking)">
            <div class="card-top">
              <div>
                <p class="muted">#{{ booking.id.toString().padStart(4, '0') }}</p>
                <h5>{{ booking.guestName }}</h5>
              </div>
              <span class="pill" :class="statusColor[booking.status]">{{ booking.status }}</span>
            </div>
            <p class="property">{{ booking.property }}</p>
            <p class="period">{{ formatSchedule(booking) }}</p>
            <p class="amount">{{ formatAmount(booking.amount) }}</p>
          </article>
        </div>

        <div v-else class="empty-box">
          <p>{{ selectedDate ? '선택한 날짜에 예약이 없습니다.' : '날짜를 선택하세요.' }}</p>
          <button v-if="selectedDate" class="ghost-btn" @click="selectedDate = new Date().getDate()">오늘 보기</button>
        </div>
      </div>
    </section>

    <p v-if="isLoading" class="empty-box">예약 데이터를 불러오는 중입니다.</p>

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
          <div class="modal-row"><span>체크인</span><strong>{{ formatDateTime(selectedBooking.checkIn, true) }}</strong></div>
          <div class="modal-row"><span>체크아웃</span><strong>{{ formatDateTime(selectedBooking.checkOut, true) }}</strong></div>
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
  color: var(--brand-accent);
  letter-spacing: -0.01em;
}

.subtitle {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.filter-chips {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
}

.filter-chip {
  font-weight: 800;
  border-radius: 999px;
  padding: 0.4rem 0.85rem;
  font-size: 0.9rem;
  min-height: 44px;
  white-space: nowrap;
}

.sort-select {
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  padding: 0.5rem 0.8rem;
  font-weight: 700;
  min-height: 44px;
  background: #ffffff;
  color: #111827;
}

.tab-switch {
  display: inline-flex;
  width: 100%;
  background: var(--surface);
  border: 1px solid var(--brand-border);
  border-radius: 999px;
  padding: 0.2rem;
  white-space: nowrap;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0.6rem 0.9rem;
  border-radius: 999px;
  font-weight: 800;
  color: #334155;
  min-height: 44px;
}

.tab-btn.host-chip--active {
  background: var(--brand-primary, #BFE7DF);
  color: #0f172a;
  border: 1px solid var(--brand-primary-strong, #0f766e);
}

.tab-btn.host-chip--active:hover,
.tab-btn.host-chip--active:focus-visible {
  background: var(--brand-primary-strong, #0f766e);
  color: #0f172a;
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

.payment-chip {
  display: inline-flex;
  align-items: center;
  margin: 0.35rem 0 0.2rem;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 0.75rem;
  font-weight: 800;
  width: fit-content;
}

.period {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.95rem;
  font-weight: 600;
}

.period-meta {
  margin: 0.2rem 0 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.9rem;
  font-weight: 700;
}

.amount {
  margin: 0.45rem 0;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  font-size: 1.05rem;
}

.ghost-btn {
  width: 100%;
  border: 1px solid var(--brand-border);
  background: transparent;
  color: var(--brand-accent);
  border-radius: 10px;
  padding: 0.6rem;
  font-weight: 900;
  min-height: 44px;
  cursor: pointer;
}

.fade-item {
  animation: fadeUp 240ms ease both;
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
  background: var(--brand-primary);
  color: var(--brand-accent);
  border-color: var(--brand-primary-strong);
}

.badge-gray {
  background: #f1f5f9;
  color: #475569;
}

.badge-outline {
  background: white;
  color: #111827;
}

.badge-red {
  background: #fef2f2;
  color: #b91c1c;
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
  width: 44px;
  height: 44px;
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
  padding: 0.4rem 0.4rem 1.2rem;
  position: relative;
  background: #f9fafb;
  cursor: pointer;
}

.cell.empty { background: transparent; border: none; cursor: default; }

.cell.selected {
  border-color: var(--brand-600);
  background: var(--brand-primary);
}

.cell.has-booking {
  border: 2px solid var(--brand-primary-strong, #0f766e);
  box-shadow: inset 0 0 0 1px var(--brand-primary-strong, #0f766e);
  background: var(--brand-200);
}

.cell.selected.has-booking {
  background: var(--brand-primary);
}

.cell.has-booking:hover,
.cell.has-booking:focus-visible {
  border-color: var(--brand-primary-strong, #0f766e);
  background: var(--brand-primary);
}

.cell:focus-visible {
  outline: 2px solid var(--brand-primary-strong, #0f766e);
  outline-offset: 2px;
}

.day { font-weight: 900; color: #111827; }

.calendar-legend {
  margin: 0.3rem 0 0.8rem;
  font-size: 0.85rem;
  color: var(--text-sub, #6b7280);
}

.count-chip {
  position: absolute;
  right: 0.35rem;
  bottom: 0.3rem;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
  line-height: 1;
  border: 1px solid var(--brand-primary-strong, #0f766e);
  color: var(--brand-accent);
  background: #fff;
  border-radius: 999px;
  padding: 0.1rem 0.3rem;
  font-size: 0.68rem;
  min-width: 30px;
  justify-content: center;
  font-weight: 800;
}

.count-number {
  font-variant-numeric: tabular-nums;
}

@media (max-width: 420px) {
  .count-unit {
    display: none;
  }

  .count-chip {
    min-width: 22px;
    padding: 0.08rem 0.25rem;
  }
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

.date-card:hover { border-color: var(--brand-primary-strong); }

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
  display: grid;
  gap: 0.5rem;
  justify-items: center;
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

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .fade-item {
    animation: none !important;
  }
}

@media (min-width: 1024px) {
  .calendar-section {
    grid-template-columns: 2fr 1fr;
    align-items: start;
  }
}
</style>
