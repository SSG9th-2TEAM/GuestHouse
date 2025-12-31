<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { exportCSV } from '../../utils/reportExport'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import { fetchAdminWeeklyReport } from '../../api/adminApi'

const activeDays = ref(7)
const report = ref(null)
const isLoading = ref(false)
const loadError = ref('')

const formatCurrency = (value) => `₩${Number(value ?? 0).toLocaleString()}`

const loadReport = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminWeeklyReport({ days: activeDays.value })
  if (response.ok && response.data) {
    report.value = response.data
  } else {
    loadError.value = '주간 리포트를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const stats = computed(() => {
  if (!report.value) return []
  return [
    { label: '예약 수', value: `${report.value.reservationCount ?? 0}건`, sub: '선택 기간', tone: 'primary' },
    { label: '결제 성공', value: `${report.value.paymentSuccessCount ?? 0}건`, sub: '완료 기준', tone: 'success' },
    { label: '취소', value: `${report.value.cancelCount ?? 0}건`, sub: '예약 취소', tone: 'neutral' },
    { label: '환불', value: `${report.value.refundCount ?? 0}건`, sub: formatCurrency(report.value.refundAmount), tone: 'warning' },
    { label: '신규 가입자', value: `${report.value.newUsers ?? 0}명`, sub: '전체 계정', tone: 'accent' },
    { label: '신규 숙소', value: `${report.value.newAccommodations ?? 0}개`, sub: '등록 완료', tone: 'primary' },
    { label: '승인 대기 숙소', value: `${report.value.pendingAccommodations ?? 0}개`, sub: '심사 대기', tone: 'warning' },
    { label: '신규 호스트', value: `${report.value.newHosts ?? 0}명`, sub: '전환 기준', tone: 'success' }
  ]
})

const series = computed(() => report.value?.revenueSeries ?? [])

const maxValue = computed(() => {
  const values = series.value.map((item) => Number(item.value ?? 0))
  return values.length ? Math.max(...values, 1) : 1
})

const heightFor = (value) => `${(Number(value ?? 0) / maxValue.value) * 100}%`

const hasSeriesData = computed(() => {
  return series.value.some((item) => Number(item.value ?? 0) > 0)
})

const exportDailyCsv = () => {
  if (!report.value) return
  const rows = series.value.map((item) => ({
    date: item.date,
    revenue: item.value ?? 0
  }))
  exportCSV({
    filename: `admin-weekly-report-${report.value.from}-to-${report.value.to}.csv`,
    sheets: [
      {
        name: '일별 매출',
        columns: [
          { key: 'date', label: '날짜' },
          { key: 'revenue', label: '매출' }
        ],
        rows
      }
    ]
  })
}

onMounted(loadReport)

watch(activeDays, loadReport)
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">주간 리포트</h1>
        <p class="admin-subtitle">선택 기간의 KPI와 매출 추이를 요약합니다.</p>
      </div>
      <div class="admin-inline-actions admin-inline-actions--nowrap">
        <select v-model="activeDays" class="admin-select">
          <option :value="7">최근 7일</option>
          <option :value="30">최근 30일</option>
        </select>
        <button class="admin-btn admin-btn--ghost" type="button" @click="exportDailyCsv">CSV 다운로드</button>
      </div>
    </header>

    <div class="admin-grid">
      <AdminStatCard
        v-for="card in stats"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :sub="card.sub"
        :tone="card.tone"
      />
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadReport">다시 시도</button>
      </div>
    </div>

    <div class="admin-card admin-report-card">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">일별 매출</p>
          <h3 class="admin-card__title">
            {{ report?.from ?? '-' }} ~ {{ report?.to ?? '-' }}
          </h3>
        </div>
        <span v-if="report && !report.statsReady" class="admin-chip admin-chip--muted">통계 데이터 준비 중</span>
      </div>
      <div class="admin-chart-area">
        <div v-if="!hasSeriesData" class="admin-status">표시할 데이터가 없습니다.</div>
        <div v-else class="admin-chart-bars">
          <div
            v-for="point in series"
            :key="point.date"
            class="admin-chart-bar"
            :style="{ height: heightFor(point.value) }"
          >
            <span class="admin-chart-bar__label">{{ formatCurrency(point.value) }}</span>
          </div>
        </div>
      </div>
      <div class="admin-chart-x">
        <span v-for="point in series" :key="point.date">{{ point.date?.slice?.(5) ?? point.date }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-page__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.admin-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-subtitle {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-weight: 600;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
}

.admin-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  padding: 16px;
}

.admin-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.admin-card__eyebrow {
  margin: 0;
  color: #0f766e;
  font-weight: 800;
  font-size: 0.9rem;
}

.admin-card__title {
  margin: 4px 0 0;
  font-size: 1.15rem;
  color: #0b3b32;
  font-weight: 900;
}

.admin-chart-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  min-height: 200px;
}

.admin-chart-bars {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(40px, 1fr));
  gap: 10px;
  align-items: end;
  height: 200px;
  width: 100%;
}

.admin-chart-bar {
  position: relative;
  background: #e5f3ef;
  border-radius: 10px 10px 4px 4px;
  min-height: 0;
}

.admin-chart-bar__label {
  position: absolute;
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.7rem;
  color: #0f766e;
  font-weight: 700;
  white-space: nowrap;
}

.admin-chart-x {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(40px, 1fr));
  gap: 10px;
  margin-top: 8px;
  color: #6b7280;
  font-weight: 700;
  font-size: 0.8rem;
}

.admin-chip {
  border-radius: 999px;
  padding: 6px 12px;
  font-weight: 700;
  font-size: 0.8rem;
}

.admin-chip--muted {
  background: #f1f5f9;
  color: #64748b;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
