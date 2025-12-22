<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import {
  dashboardStats,
  dashboardPendingListings,
  dashboardRevenueTrend,
  dashboardAlerts
} from '../../mocks/adminMockData'

const router = useRouter()
const stats = ref([])
const pendingListings = ref([])
const revenueTrend = ref({ months: [], values: [] })
const alerts = ref([])
const pendingQuery = ref('')
const pendingStatus = ref('all')
const activePeriod = ref('30')

const loadDashboard = () => {
  stats.value = dashboardStats
  pendingListings.value = dashboardPendingListings
  revenueTrend.value = dashboardRevenueTrend
  alerts.value = dashboardAlerts
}

const filteredPending = computed(() => {
  const query = pendingQuery.value.trim().toLowerCase()
  return pendingListings.value.filter((item) => {
    const matchesQuery = !query ||
      item.name.toLowerCase().includes(query) ||
      item.host.toLowerCase().includes(query) ||
      item.location.toLowerCase().includes(query)
    const matchesStatus = pendingStatus.value === 'all' || item.status === pendingStatus.value
    return matchesQuery && matchesStatus
  })
})

const pendingStatusVariant = (status) => {
  if (status === '대기') return 'warning'
  if (status === '검토') return 'accent'
  return 'neutral'
}

const alertVariant = (tone) => {
  if (tone === 'warning') return 'warning'
  if (tone === 'success') return 'success'
  if (tone === 'accent') return 'accent'
  return 'neutral'
}

const revenueMax = computed(() => Math.max(...revenueTrend.value.values, 1))
const revenueHeight = (value) => `${(value / revenueMax.value) * 100}%`

const goTo = (target) => {
  if (target) router.push(target)
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '요약 지표',
      columns: [
        { key: 'label', label: '지표' },
        { key: 'value', label: '값' },
        { key: 'sub', label: '설명' }
      ],
      rows: stats.value
    },
    {
      name: '승인 대기 숙소',
      columns: [
        { key: 'id', label: 'ID' },
        { key: 'name', label: '숙소명' },
        { key: 'host', label: '호스트' },
        { key: 'location', label: '위치' },
        { key: 'type', label: '유형' },
        { key: 'rooms', label: '객실' },
        { key: 'nightlyPrice', label: '가격(1박)' },
        { key: 'contact', label: '연락처' },
        { key: 'submittedAt', label: '신청일' },
        { key: 'status', label: '상태' }
      ],
      rows: filteredPending.value
    },
    {
      name: '운영 알림',
      columns: [
        { key: 'title', label: '알림' },
        { key: 'meta', label: '메타' },
        { key: 'time', label: '시간' }
      ],
      rows: alerts.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-dashboard-report-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-dashboard-report-${today}.csv`, sheets })
}

onMounted(loadDashboard)
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">관리자 대시보드</h1>
        <p class="admin-subtitle">주요 지표와 거래 현황을 한 눈에 확인하세요.</p>
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
        :clickable="Boolean(card.target)"
        @click="goTo(card.target)"
      />
    </div>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">운영 기간</span>
        <select v-model="activePeriod" class="admin-select">
          <option value="7">최근 7일</option>
          <option value="30">최근 30일</option>
          <option value="90">최근 3개월</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">빠른 작업</span>
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

    <div class="admin-grid admin-grid--2 admin-grid--main">
      <AdminTableCard title="승인 대기 숙소" class="admin-table-card--wide">
        <template #actions>
          <input
            v-model="pendingQuery"
            class="admin-input"
            type="search"
            placeholder="숙소/호스트/지역 검색"
          />
          <select v-model="pendingStatus" class="admin-select">
            <option value="all">전체 상태</option>
            <option value="대기">대기</option>
            <option value="검토">검토</option>
          </select>
        </template>
        <table class="admin-table--nowrap admin-table--roomy">
          <thead>
            <tr>
              <th>ID</th>
              <th>숙소명</th>
              <th>호스트</th>
              <th>위치</th>
              <th>유형</th>
              <th>객실</th>
              <th>가격(1박)</th>
              <th>연락처</th>
              <th>신청일</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredPending" :key="item.id">
              <td class="admin-strong">{{ item.id }}</td>
              <td class="admin-strong">{{ item.name }}</td>
              <td>{{ item.host }}</td>
              <td>{{ item.location }}</td>
              <td>{{ item.type }}</td>
              <td>{{ item.rooms }}개</td>
              <td>{{ item.nightlyPrice }}</td>
              <td>{{ item.contact }}</td>
              <td>{{ item.submittedAt }}</td>
              <td>
                <AdminBadge :text="item.status" :variant="pendingStatusVariant(item.status)" />
              </td>
              <td>
                <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/accommodations')">
                  상세 보기
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </AdminTableCard>

    </div>

    <div class="admin-grid admin-grid--2 admin-grid--summary">
      <div class="admin-card admin-alerts-card">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">운영 알림</p>
            <h3 class="admin-card__title">오늘 확인할 이슈</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button">전체 보기</button>
        </div>
        <div class="admin-alert-list">
          <div v-for="alert in alerts" :key="alert.id" class="admin-alert-item">
            <div>
              <div class="admin-alert-title">{{ alert.title }}</div>
              <div class="admin-alert-meta">{{ alert.meta }}</div>
            </div>
            <div class="admin-alert-right">
              <AdminBadge :text="alert.time" :variant="alertVariant(alert.tone)" />
            </div>
          </div>
        </div>
      </div>

      <div class="admin-card admin-highlight">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">운영 요약</p>
            <h3 class="admin-card__title">이번 주 주요 지표</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button">리포트 보기</button>
        </div>
        <div class="admin-highlight-list">
          <div class="admin-highlight-item">
            <div>
              <p class="admin-highlight-label">승인 평균 소요</p>
              <p class="admin-highlight-sub">전주 대비 -0.6일</p>
            </div>
            <p class="admin-highlight-value">2.4일</p>
          </div>
          <div class="admin-highlight-item">
            <div>
              <p class="admin-highlight-label">환불 처리</p>
              <p class="admin-highlight-sub">24시간 내 완료</p>
            </div>
            <p class="admin-highlight-value">98.1%</p>
          </div>
          <div class="admin-highlight-item">
            <div>
              <p class="admin-highlight-label">CS 응답 속도</p>
              <p class="admin-highlight-sub">목표 4시간 이하</p>
            </div>
            <p class="admin-highlight-value">2시간</p>
          </div>
          <div class="admin-highlight-item">
            <div>
              <p class="admin-highlight-label">숙소 승인 SLA</p>
              <p class="admin-highlight-sub">48시간 이내 처리</p>
            </div>
            <p class="admin-highlight-value">92%</p>
          </div>
        </div>
      </div>
    </div>

    <div class="admin-card admin-placeholder-chart">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">수익 추이</p>
          <h3 class="admin-card__title">플랫폼 수익 추이</h3>
        </div>
      </div>
      <div class="admin-chart-area">
        <div class="admin-chart-y">
          <span>40억</span>
          <span>30억</span>
          <span>20억</span>
          <span>10억</span>
        </div>
        <div class="admin-chart-bars">
          <div
            v-for="(value, idx) in revenueTrend.values"
            :key="idx"
            class="admin-chart-bar"
            :style="{ height: revenueHeight(value) }"
          >
            <span class="admin-chart-bar__label">{{ value }}억</span>
          </div>
        </div>
      </div>
      <div class="admin-chart-x">
        <span v-for="month in revenueTrend.months" :key="month">{{ month }}</span>
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

.admin-grid--2 {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: start;
}

.admin-grid--main {
  grid-template-columns: minmax(640px, 2.2fr) minmax(320px, 1fr);
}

.admin-grid--summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: stretch;
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

.admin-placeholder-chart {
  min-height: 260px;
}

.admin-chart-area {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px;
  align-items: end;
  margin-top: 12px;
}

.admin-chart-y {
  display: grid;
  grid-template-rows: repeat(4, 1fr);
  gap: 12px;
  color: #6b7280;
  font-weight: 700;
}

.admin-chart-bars {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(40px, 1fr));
  gap: 10px;
  align-items: end;
  height: 200px;
}

.admin-chart-bar {
  position: relative;
  background: #e5f3ef;
  border-radius: 10px 10px 4px 4px;
  min-height: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 8px;
}

.admin-chart-bar__label {
  font-size: 0.85rem;
  font-weight: 800;
  color: #0f766e;
}

.admin-chart-x {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(40px, 1fr));
  gap: 10px;
  text-align: center;
  color: #6b7280;
  font-weight: 700;
}

.admin-hint {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

.admin-btn-ghost {
  background: transparent;
  border: 1px solid #d1d5db;
  color: #0f766e;
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 800;
}

.admin-btn-ghost:hover {
  border-color: #0f766e;
}

.admin-table--nowrap th,
.admin-table--nowrap td {
  white-space: nowrap;
}

.admin-table--roomy th,
.admin-table--roomy td {
  padding: 14px 12px;
}

.admin-table-card--wide :deep(table) {
  min-width: 1020px;
}

.admin-table-card--wide {
  grid-column: 1 / -1;
}

.admin-inline-actions--nowrap {
  flex-wrap: nowrap;
}

@media (max-width: 1024px) {
  .admin-grid--main {
    grid-template-columns: 1fr;
  }

  .admin-grid--summary {
    grid-template-columns: 1fr;
  }

  .admin-inline-actions--nowrap {
    flex-wrap: wrap;
  }
}

.admin-alerts-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.admin-alert-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #eef1f4;
  background: #f8fafc;
}

.admin-alert-title {
  font-weight: 800;
  color: #0b3b32;
}

.admin-alert-meta {
  font-size: 0.85rem;
  color: #6b7280;
}

.admin-alert-right {
  display: flex;
  align-items: center;
}

.admin-highlight {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.admin-highlight-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  justify-content: space-between;
}

.admin-highlight-item {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.admin-highlight-label {
  margin: 0;
  color: #6b7280;
  font-weight: 700;
  font-size: 0.85rem;
}

.admin-highlight-value {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: #0b3b32;
  white-space: nowrap;
}

.admin-highlight-sub {
  color: #0f766e;
  font-size: 0.8rem;
  font-weight: 700;
  margin: 4px 0 0;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
