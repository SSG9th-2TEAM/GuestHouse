<script setup>
import { ref, computed } from 'vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const selectedYear = ref(2025)
const years = [2024, 2025]

// Mock Data
const monthlyRevenue = {
  2024: [
    { month: 1, amount: 3200000 },
    { month: 2, amount: 2800000 },
    { month: 3, amount: 3500000 },
    { month: 4, amount: 4100000 },
    { month: 5, amount: 4800000 },
    { month: 6, amount: 5200000 },
    { month: 7, amount: 6800000 },
    { month: 8, amount: 7500000 },
    { month: 9, amount: 5900000 },
    { month: 10, amount: 4500000 },
    { month: 11, amount: 3800000 },
    { month: 12, amount: 4200000 }
  ],
  2025: [
    { month: 1, amount: 4500000 },
    { month: 2, amount: 3900000 },
    { month: 3, amount: 4800000 },
    { month: 4, amount: 5100000 },
    { month: 5, amount: 5500000 },
    { month: 6, amount: 5800000 },
    { month: 7, amount: 7200000 },
    { month: 8, amount: 8100000 },
    { month: 9, amount: 6200000 },
    { month: 10, amount: 5200000 },
    { month: 11, amount: 4800000 },
    { month: 12, amount: 5200000 } // Projected for demo
  ]
}

const currentYearData = computed(() => monthlyRevenue[selectedYear.value])

const maxRevenue = computed(() => {
  return Math.max(...currentYearData.value.map(d => d.amount))
})

// Summary Calculations (Mock for "This Month" - assuming Dec for demo)
const thisMonthRevenue = computed(() => {
  const data = currentYearData.value
  return data[data.length - 1].amount // Last month in data
})

const formatPrice = (price) => {
  return price.toLocaleString()
}

const getBarHeight = (amount) => {
  const percentage = (amount / maxRevenue.value) * 100
  return `${percentage}%`
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const rows = currentYearData.value.map((item) => ({
    year: selectedYear.value,
    month: `${item.month}월`,
    revenue: item.amount
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
</script>

<template>
  <div class="revenue-view">
    <div class="view-header">
      <div>
        <h2>매출 리포트</h2>
        <p class="subtitle">{{ selectedYear }}년 재무 현황</p>
      </div>
      <div class="header-actions">
        <select v-model="selectedYear" class="year-select">
          <option v-for="year in years" :key="year" :value="year">{{ year }}년</option>
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
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <!-- This Month -->
      <div class="summary-card">
        <div class="card-icon green-bg">💲</div>
        <div class="trend-icon up">↗</div>
        <p class="card-label">이번 달 총 매출</p>
        <h3 class="card-value">₩{{ formatPrice(thisMonthRevenue) }}</h3>
        <p class="card-trend positive">전월 대비 +15.3%</p>
      </div>

      <!-- Next Month Expected -->
      <div class="summary-card">
        <div class="card-icon blue-bg">📅</div>
        <div class="trend-icon up">↗</div>
        <p class="card-label">예상 다음 달 매출</p>
        <h3 class="card-value">₩{{ formatPrice(thisMonthRevenue * 1.1) }}</h3>
        <p class="card-sub">예약 35건 기준</p>
      </div>

      <!-- Platform Fee -->
      <div class="summary-card">
        <div class="card-icon orange-bg">📉</div>
        <div class="trend-icon down">↘</div>
        <p class="card-label">플랫폼 수수료</p>
        <h3 class="card-value">₩{{ formatPrice(thisMonthRevenue * 0.1) }}</h3>
      </div>
    </div>

    <!-- Monthly Revenue Chart -->
    <div class="chart-section">
      <h3>월별 매출 추이</h3>
      <div class="bar-chart">
        <div
            v-for="data in currentYearData"
            :key="data.month"
            class="bar-column"
        >
          <div class="bar-container">
            <div class="bar" :style="{ height: getBarHeight(data.amount) }">
              <span class="tooltip">₩{{ formatPrice(data.amount) }}</span>
            </div>
          </div>
          <span class="month-label">{{ data.month }}월</span>
        </div>
      </div>
    </div>

    <!-- Detailed Stats List -->
    <div class="stats-list">
      <h3>상세 내역</h3>
      <div class="list-item header">
        <span>기간</span>
        <span>매출액</span>
        <span>점유율 (예시)</span>
      </div>
      <div v-for="data in currentYearData.slice().reverse()" :key="data.month" class="list-item">
        <span class="month-col">{{ selectedYear }}년 {{ data.month }}월</span>
        <span class="amount-col">₩{{ formatPrice(data.amount) }}</span>
        <span class="occupancy-col">{{ Math.floor(Math.random() * 30 + 70) }}%</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.revenue-view {
  padding-bottom: 2rem;
}

/* ✅ 대시보드 톤: 헤더 선명/굵게 + 모바일 퍼스트 스택 */
.view-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
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
}

/* Summary Cards (모바일: 세로, 태블릿+: 3열) */
.summary-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.summary-card {
  background: white;
  border-radius: 16px;
  padding: 1.25rem 1.25rem 1.2rem;
  position: relative;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
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
  display: flex;
  align-items: flex-end;
  height: 210px;
  padding-top: 2rem; /* tooltip 공간 */
  overflow-x: auto;
  gap: 0.7rem;
  justify-content: flex-start;
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  flex: 0 0 44px; /* 모바일에서 한 칸 폭 고정 → 스크롤 자연스러움 */
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

  .year-select {
    width: auto;
  }

  .summary-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
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
</style>
