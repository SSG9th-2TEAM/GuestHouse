<script setup>
import { ref, computed } from 'vue'

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
</script>

<template>
  <div class="revenue-view">
    <div class="view-header">
      <div>
        <h2>매출 리포트</h2>
        <p class="subtitle">{{ selectedYear }}년 재무 현황</p>
      </div>
      <select v-model="selectedYear" class="year-select">
        <option v-for="year in years" :key="year" :value="year">{{ year }}년</option>
      </select>
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

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.year-select {
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  color: #333;
  outline: none;
  background: white;
}

/* Summary Cards */
.summary-cards {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.summary-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  position: relative;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  border: 1px solid #f0f0f0;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  margin-bottom: 1rem;
}

.green-bg { background: #E0F2F1; color: #00695C; }
.blue-bg { background: #E3F2FD; color: #1565C0; }
.orange-bg { background: #FFF3E0; color: #EF6C00; }

.trend-icon {
  position: absolute;
  top: 1.5rem;
  right: 1.5rem;
  font-weight: 700;
}

.trend-icon.up { color: #00C853; }
.trend-icon.down { color: #FFAB00; }

.card-label {
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.card-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.5rem;
}

.card-trend {
  font-size: 0.85rem;
  font-weight: 600;
}

.card-trend.positive { color: #00C853; }

.card-sub {
  font-size: 0.85rem;
  color: #1565C0;
}

/* Chart Section */
.chart-section {
  background: white;
  padding: 1.5rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.chart-section h3 {
  font-size: 1.1rem;
  color: #333;
  margin: 0 0 1.5rem;
}

.bar-chart {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 200px;
  padding-top: 2rem; /* space for tooltips */
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  height: 100%;
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
  width: 60%;
  background: #BFE7DF;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease, background 0.2s;
  position: relative;
  cursor: pointer;
}

.bar:hover {
  background: #004d40;
}

.tooltip {
  position: absolute;
  top: -25px;
  left: 50%;
  transform: translateX(-50%);
  background: #333;
  color: white;
  font-size: 0.7rem;
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.bar:hover .tooltip {
  opacity: 1;
}

.month-label {
  font-size: 0.8rem;
  color: #888;
  margin-top: 0.5rem;
}

/* Stats List */
.stats-list {
  background: white;
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.stats-list h3 {
  font-size: 1.1rem;
  color: #333;
  margin: 0 0 1rem;
}

.list-item {
  display: flex;
  justify-content: space-between;
  padding: 1rem 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 0.95rem;
}

.list-item:last-child {
  border-bottom: none;
}

.list-item.header {
  font-weight: 600;
  color: #666;
  border-bottom: 2px solid #f0f0f0;
}

.month-col { flex: 1; color: #333; }
.amount-col { flex: 1; text-align: right; font-weight: 600; }
.occupancy-col { flex: 1; text-align: right; color: #666; }
</style>
