<script setup>
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchHostRevenueDetails, fetchHostRevenueSummary, fetchHostRevenueTrend } from '@/api/hostRevenue'
import { formatCurrency, formatNumber, formatDate } from '@/utils/formatters'

const now = new Date()
const currentYear = now.getFullYear()
const selectedYear = ref(currentYear)
const selectedMonth = ref('all')
const years = computed(() => Array.from({ length: 6 }, (_, idx) => currentYear - idx))
const months = ['all', ...Array.from({ length: 12 }, (_, idx) => String(idx + 1))]

const revenueSummary = ref({
  year: selectedYear.value,
  month: 1,
  totalRevenue: 0,
  expectedNextMonthRevenue: 0,
  platformFeeRate: 0.04,
  platformFeeAmount: 0,
  reservationCount: 0
})
const revenueTrend = ref([])
const revenueDetails = ref([])
const isLoading = ref(false)
const loadError = ref('')
const prefersReducedMotion = ref(false)
const animateCharts = ref(false)

const currentYearSummary = computed(() => revenueSummary.value)
const currentYearTrend = computed(() => revenueTrend.value)
const currentYearDetails = computed(() => revenueDetails.value)

const isDailyView = computed(() => selectedMonth.value !== 'all')

const chartSeries = computed(() => {
  if (isDailyView.value) {
    return currentYearDetails.value.map((item) => ({
      label: Number(item.period.split('-')[2]),
      revenue: item.revenue
    }))
  }

  const byMonth = new Map(currentYearTrend.value.map((item) => [item.month, item.revenue]))
  return Array.from({ length: 12 }, (_, idx) => {
    const month = idx + 1
    return { label: month, revenue: byMonth.get(month) ?? 0 }
  })
})

const maxRevenue = computed(() => {
  const values = chartSeries.value.map(d => d.revenue)
  return values.length ? Math.max(...values) : 0
})

const formatPrice = (price) => formatNumber(price)

const getBarHeight = (revenue) => {
  if (!maxRevenue.value) return '0%'
  const percentage = (revenue / maxRevenue.value) * 100
  return `${percentage}%`
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const rows = currentYearTrend.value.map((item) => ({
    year: selectedYear.value,
    month: `${item.month}월`,
    revenue: item.revenue
  }))
  const sheets = [
    {
      name: '매출 리포트',
      columns: [
        { key: 'year', label: '연도' },
        { key: 'month', label: '월' },
        { key: 'revenue', label: '매출액' }
      ],
      rows
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `host-revenue-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `host-revenue-${today}.csv`, sheets })
}

const loadRevenue = async (year, month) => {
  isLoading.value = true
  loadError.value = ''
  const currentMonthValue = now.getMonth() + 1
  const summaryMonth = year === now.getFullYear() ? currentMonthValue : 12
  const monthValue = month === 'all' ? null : Number(month)
  const detailFrom = monthValue ? `${year}-${String(monthValue).padStart(2, '0')}-01` : `${year}-01-01`
  const detailTo = monthValue
    ? `${year}-${String(monthValue).padStart(2, '0')}-${String(new Date(year, monthValue, 0).getDate()).padStart(2, '0')}`
    : `${year}-12-31`
  const detailGranularity = monthValue ? 'day' : 'month'

  const [summaryRes, trendRes, detailsRes] = await Promise.all([
    fetchHostRevenueSummary({ year, month: summaryMonth }),
    fetchHostRevenueTrend({ year }),
    fetchHostRevenueDetails({ from: detailFrom, to: detailTo, granularity: detailGranularity })
  ])

  if (summaryRes.ok && summaryRes.data) {
    revenueSummary.value = summaryRes.data
  } else {
    loadError.value = '데이터를 불러오지 못했어요.'
  }

  if (trendRes.ok && Array.isArray(trendRes.data)) {
    revenueTrend.value = trendRes.data
  } else {
    loadError.value = '데이터를 불러오지 못했어요.'
  }

  if (detailsRes.ok && Array.isArray(detailsRes.data)) {
    revenueDetails.value = detailsRes.data
  } else if (trendRes.ok && Array.isArray(trendRes.data)) {
    revenueDetails.value = trendRes.data.map((item) => ({
      period: `${year}-${String(item.month).padStart(2, '0')}`,
      revenue: item.revenue,
      occupancyRate: item.occupancyRate
    }))
  }

  isLoading.value = false
  if (!prefersReducedMotion.value) {
    animateCharts.value = false
    nextTick(() => {
      requestAnimationFrame(() => {
        animateCharts.value = true
      })
    })
  }
}

onMounted(() => loadRevenue(selectedYear.value, selectedMonth.value))
watch([selectedYear, selectedMonth], ([year, month]) => {
  loadRevenue(year, month)
})

const netRevenue = computed(() => {
  return (currentYearSummary.value?.totalRevenue ?? 0) - (currentYearSummary.value?.platformFeeAmount ?? 0)
})

const animatedSummary = ref({
  totalRevenue: 0,
  platformFeeAmount: 0,
  netRevenue: 0,
  expectedNextMonthRevenue: 0
})

const animateValue = (key, target, duration = 420) => {
  const start = animatedSummary.value[key] ?? 0
  if (prefersReducedMotion.value) {
    animatedSummary.value[key] = target
    return
  }
  const startTime = performance.now()
  const step = (now) => {
    const progress = Math.min((now - startTime) / duration, 1)
    const value = start + (target - start) * progress
    animatedSummary.value[key] = Math.round(value)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(
  () => [currentYearSummary.value, netRevenue.value],
  () => {
    animateValue('totalRevenue', currentYearSummary.value?.totalRevenue ?? 0)
    animateValue('platformFeeAmount', currentYearSummary.value?.platformFeeAmount ?? 0)
    animateValue('netRevenue', netRevenue.value ?? 0)
    animateValue('expectedNextMonthRevenue', currentYearSummary.value?.expectedNextMonthRevenue ?? 0)
  },
  { immediate: true }
)

onMounted(() => {
  prefersReducedMotion.value = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
})

const summaryCards = computed(() => ([
  {
    label: '이번 달 확정 매출',
    value: animatedSummary.value.totalRevenue ?? 0,
    tone: 'green',
    note: null
  },
  {
    label: '플랫폼 수수료',
    value: animatedSummary.value.platformFeeAmount ?? 0,
    tone: 'orange',
    note: null
  },
  {
    label: '순매출',
    value: animatedSummary.value.netRevenue ?? 0,
    tone: 'blue',
    note: null
  },
  {
    label: '다음 달 예상 매출',
    value: animatedSummary.value.expectedNextMonthRevenue ?? 0,
    tone: 'green',
    note: `예약 ${currentYearSummary.value?.reservationCount ?? 0}건 기준`
  }
]))

const hasRevenueData = computed(() => {
  return chartSeries.value.some((item) => item.revenue > 0)
})

const formatPeriodLabel = (period) => {
  if (!period) return ''
  const normalized = period.length === 7 ? `${period}-01` : period
  return formatDate(normalized, true)
}
</script>

<template>
  <div class="revenue-view">
    <div class="view-header">
      <div>
        <h2>매출 리포트</h2>
        <p class="subtitle">{{ selectedYear }}년 재무 현황</p>
      </div>
    </div>

    <div class="header-actions">
      <select v-model="selectedYear" class="year-select">
        <option v-for="year in years" :key="year" :value="year">{{ year }}년</option>
      </select>
      <select v-model="selectedMonth" class="year-select">
        <option value="all">전체</option>
        <option v-for="month in months.slice(1)" :key="month" :value="month">{{ Number(month) }}월</option>
      </select>
      <details class="admin-dropdown">
        <summary class="admin-btn admin-btn--ghost">다운로드</summary>
        <div class="admin-dropdown__menu">
          <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">
            CSV
          </button>
          <button class="admin-btn admin-btn--primary admin-dropdown__item" type="button" @click="downloadReport('xlsx')">
            XLSX
          </button>
        </div>
      </details>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards" :class="{ 'fade-section': !isLoading }">
      <div v-if="isLoading" class="summary-skeleton">
        <div v-for="i in 3" :key="i" class="skeleton-card" />
      </div>
      <div v-else-if="loadError" class="status-card">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" type="button" @click="loadRevenue(selectedYear, selectedMonth)">다시 시도</button>
      </div>
      <div v-for="(card, index) in summaryCards" :key="card.label" class="summary-card" :class="{ 'fade-item': !isLoading }"
           :style="{ animationDelay: `${Math.min(index, 5) * 70}ms` }">
        <div class="card-icon" :class="`${card.tone}-bg`">💲</div>
        <p class="card-label">{{ card.label }}</p>
        <h3 class="card-value">{{ formatCurrency(card.value) }}</h3>
        <p v-if="card.note" class="card-sub">{{ card.note }}</p>
      </div>
    </div>

    <!-- Monthly Revenue Chart -->
    <div class="chart-section" :class="{ 'fade-section': !isLoading }">
      <h3>{{ isDailyView ? `${selectedMonth}월 일별 매출` : '월별 매출 추이' }}</h3>
      <div v-if="isLoading" class="chart-skeleton" />
      <div v-else-if="!hasRevenueData" class="status-card">
        <p>선택한 기간에 확정 매출이 없습니다.</p>
        <button class="ghost-btn" type="button" @click="selectedMonth = 'all'">기간 변경</button>
      </div>
      <div v-else class="bar-chart" :class="[isDailyView ? 'daily' : 'monthly', { animate: animateCharts }]">
        <div
            v-for="data in chartSeries"
            :key="data.label"
            class="bar-column"
        >
          <div class="bar-container">
            <div class="bar" :style="{ height: getBarHeight(data.revenue) }">
              <span class="tooltip">{{ formatCurrency(data.revenue) }}</span>
            </div>
          </div>
          <span class="month-label">{{ data.label }}</span>
        </div>
      </div>
    </div>

    <!-- Detailed Stats List -->
    <div class="stats-list" :class="{ 'fade-section': !isLoading }">
      <h3>{{ isDailyView ? '일별 상세' : '상세 내역' }}</h3>
      <div v-if="isLoading" class="list-skeleton">
        <div v-for="i in 5" :key="i" class="skeleton-row" />
      </div>
      <div v-else-if="loadError" class="status-card">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" type="button" @click="loadRevenue(selectedYear, selectedMonth)">다시 시도</button>
      </div>
      <div v-else-if="!currentYearDetails.length" class="status-card">
        <p>선택한 기간에 확정 매출이 없습니다.</p>
        <button class="ghost-btn" type="button" @click="selectedMonth = 'all'">기간 변경</button>
      </div>
      <div v-else class="list-item header">
        <span>기간</span>
        <span>매출액</span>
        <span>점유율 (예시)</span>
      </div>
      <div v-for="data in currentYearDetails.slice().reverse()" :key="data.period" class="list-item" :class="{ 'fade-item': !isLoading }">
        <span class="month-col">{{ formatPeriodLabel(data.period) }}</span>
        <span class="amount-col">{{ formatCurrency(data.revenue) }}</span>
        <span class="occupancy-col">{{ data.occupancyRate.toFixed(1) }}%</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.revenue-view {
  padding-bottom: calc(2rem + var(--bn-h, 0px) + (var(--bn-pad, 0px) * 2) + env(safe-area-inset-bottom));
}

/* ✅ 대시보드 톤: 헤더 선명/굵게 + 모바일 퍼스트 스택 */
.view-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: #0b3b32;
  margin: 0.15rem 0 0.2rem;
  letter-spacing: -0.01em;
}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0;
}

.year-select {
  padding: 0.65rem 0.9rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0f172a;
  outline: none;
  background: white;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 6;
  background: #f5f5f5;
  padding: 0.6rem 0;
}

.status-card {
  width: 100%;
  padding: 1rem;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
  background: #f8fafc;
  text-align: center;
  display: grid;
  gap: 0.5rem;
}

.status-card p {
  margin: 0;
  color: #6b7280;
  font-weight: 700;
}

.ghost-btn {
  border: 1px solid #d1d5db;
  background: white;
  color: #0f766e;
  border-radius: 10px;
  padding: 0.55rem 0.9rem;
  font-weight: 800;
  min-height: 44px;
  cursor: pointer;
}

.fade-section {
  animation: fadeUp 240ms ease both;
}

.fade-item {
  animation: fadeUp 240ms ease both;
}

/* Summary Cards (모바일: 세로, 태블릿+: 3열) */
.summary-cards {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: 0.25rem;
  position: relative;
}

.summary-skeleton {
  display: flex;
  gap: 0.75rem;
  width: 100%;
}

.skeleton-card {
  flex: 0 0 78%;
  height: 140px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

.summary-card {
  background: white;
  border-radius: 16px;
  padding: 1.25rem 1.25rem 1.2rem;
  position: relative;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  flex: 0 0 78%;
  scroll-snap-align: start;
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  margin-bottom: 0.9rem;
}

.green-bg { background: #E0F2F1; color: #0f766e; }
.blue-bg { background: #E3F2FD; color: #1565C0; }
.orange-bg { background: #FFF3E0; color: #B45309; }

.trend-icon {
  position: absolute;
  top: 1.15rem;
  right: 1.15rem;
  font-weight: 900;
}

.trend-icon.up { color: #0f766e; }
.trend-icon.down { color: #b45309; }

.card-label {
  font-size: 0.92rem;
  color: #6b7280;
  font-weight: 700;
  margin: 0 0 0.45rem;
}

.card-value {
  font-size: 1.55rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 0.4rem;
}

.card-trend {
  font-size: 0.86rem;
  font-weight: 800;
  margin: 0;
}

.card-trend.positive { color: #0f766e; }

.card-sub {
  font-size: 0.86rem;
  font-weight: 700;
  color: #1565C0;
  margin: 0;
}

/* Chart Section */
.chart-section {
  background: white;
  padding: 1.25rem;
  border-radius: 16px;
  margin-bottom: 1.5rem;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
}

.chart-section h3 {
  font-size: 1.1rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 1rem;
}

/* ✅ 모바일에서 12개월 막대가 답답해서: 가로 스크롤 허용(템플릿 변경 없이 CSS만) */
.bar-chart {
  align-items: flex-end;
  height: 210px;
  padding-top: 2rem; /* tooltip 공간 */
}

.bar-chart.monthly {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.35rem;
}

.bar-chart.daily {
  display: flex;
  overflow-x: auto;
  gap: 0.5rem;
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar-chart.daily .bar-column {
  flex: 0 0 36px;
}

.bar-container {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  position: relative;
}

.bar {
  width: 100%;
  background: #BFE7DF;
  border-radius: 6px 6px 0 0;
  transition: height 0.3s ease, background 0.2s;
  position: relative;
  cursor: pointer;
  transform-origin: bottom;
}

.chart-skeleton {
  height: 210px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

.bar-chart.animate .bar {
  animation: barGrow 480ms ease;
}

.bar:hover {
  background: #0f766e;
}

.tooltip {
  position: absolute;
  top: -26px;
  left: 50%;
  transform: translateX(-50%);
  background: #111827;
  color: white;
  font-size: 0.72rem;
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.bar:hover .tooltip {
  opacity: 1;
}

.month-label {
  font-size: 0.82rem;
  color: #6b7280;
  font-weight: 700;
  margin-top: 0.45rem;
}

/* Stats List */
.stats-list {
  background: white;
  padding: 1.25rem;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
}

.list-skeleton {
  display: grid;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.skeleton-row {
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

.stats-list h3 {
  font-size: 1.1rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 0.75rem;
}

.list-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.95rem 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.95rem;
}

.list-item:last-child {
  border-bottom: none;
}

.list-item.header {
  font-weight: 900;
  color: #475569;
  border-bottom: 2px solid #eef2f7;
  font-size: 0.92rem;
}

.month-col { flex: 1; color: #0f172a; font-weight: 700; }
.amount-col { flex: 1; text-align: right; font-weight: 900; color: #0f172a; }
.occupancy-col { flex: 1; text-align: right; color: #6b7280; font-weight: 700; }

/* 태블릿+에서는 헤더 가로, 카드 3열, 차트 스크롤 해제 */
@media (min-width: 768px) {
  .view-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .header-actions {
    position: static;
    background: transparent;
    padding: 0;
  }

  .year-select {
    width: auto;
  }

  .summary-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    display: grid;
    overflow: visible;
  }

  .summary-card {
    flex: 1;
  }

  .summary-skeleton,
  .skeleton-card {
    flex: 1;
  }

  .bar-chart {
    overflow-x: visible;
    justify-content: space-between;
    gap: 0;
  }

  .bar-column {
    flex: 1;
  }

  .bar {
    width: 60%;
  }
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

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

@keyframes barGrow {
  from {
    transform: scaleY(0);
  }
  to {
    transform: scaleY(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .fade-section,
  .fade-item,
  .bar-chart.animate .bar,
  .skeleton-card,
  .skeleton-row {
    animation: none !important;
  }
}
</style>
