<script setup>
import { onMounted, ref } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import {
  dashboardStats,
  dashboardPendingListings,
  dashboardRevenueTrend,
  dashboardTransactions
} from '../../mocks/adminMockData'

const stats = ref([])
const pendingListings = ref([])
const revenueTrend = ref({ months: [], values: [] })
const transactionMode = ref('yearly')
const transactionSeries = ref([])

const loadDashboard = () => {
  stats.value = dashboardStats
  pendingListings.value = dashboardPendingListings
  revenueTrend.value = dashboardRevenueTrend
  setTransactionMode('yearly')
}

const setTransactionMode = (mode) => {
  transactionMode.value = mode
  transactionSeries.value = dashboardTransactions[mode] || []
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
      />
    </div>

    <div class="admin-grid admin-grid--2">
      <AdminTableCard title="승인 대기 숙소">
        <table>
          <thead>
            <tr>
              <th>숙소명</th>
              <th>호스트</th>
              <th>위치</th>
              <th>신청일</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pendingListings" :key="item.name">
              <td class="admin-strong">{{ item.name }}</td>
              <td>{{ item.host }}</td>
              <td>{{ item.location }}</td>
              <td>{{ item.submittedAt }}</td>
              <td>
                <button class="admin-btn-ghost" type="button">상세보기</button>
              </td>
            </tr>
          </tbody>
        </table>
      </AdminTableCard>

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
              :style="{ height: `${value * 2}%` }"
            >
              <span class="admin-chart-bar__label">{{ value }}억</span>
            </div>
          </div>
        </div>
        <div class="admin-chart-x">
          <span v-for="month in revenueTrend.months" :key="month">{{ month }}</span>
        </div>
      </div>
    </div>

    <div class="admin-card admin-transaction-card">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">거래 모니터링</p>
          <h3 class="admin-card__title">거래 추이</h3>
        </div>
        <div class="admin-toggle">
          <button
            type="button"
            class="admin-toggle__btn"
            :class="{ 'is-active': transactionMode === 'yearly' }"
            @click="setTransactionMode('yearly')"
          >
            연간
          </button>
          <button
            type="button"
            class="admin-toggle__btn"
            :class="{ 'is-active': transactionMode === 'monthly' }"
            @click="setTransactionMode('monthly')"
          >
            월간
          </button>
        </div>
      </div>
      <div class="admin-transaction-bars">
        <div
          v-for="(value, idx) in transactionSeries"
          :key="idx"
          class="admin-transaction-bar"
        >
          <div class="admin-transaction-bar__fill" :style="{ height: `${Math.min(value, 80)}%` }" />
          <span class="admin-transaction-bar__value">{{ value }}</span>
        </div>
      </div>
      <p class="admin-hint">실제 차트 라이브러리는 추후 연동 예정</p>
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
}

.admin-chart-bar {
  position: relative;
  background: #e5f3ef;
  border-radius: 10px 10px 4px 4px;
  min-height: 60px;
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

.admin-transaction-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-toggle {
  display: inline-flex;
  background: #f0fdf4;
  padding: 6px;
  border-radius: 12px;
}

.admin-toggle__btn {
  border: none;
  background: transparent;
  padding: 8px 12px;
  border-radius: 10px;
  font-weight: 800;
  color: #0f766e;
}

.admin-toggle__btn.is-active {
  background: #0f766e;
  color: #ecfdf3;
}

.admin-transaction-bars {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(50px, 1fr));
  gap: 10px;
  align-items: end;
}

.admin-transaction-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.admin-transaction-bar__fill {
  width: 100%;
  background: linear-gradient(180deg, #0f766e, #34d399);
  border-radius: 12px 12px 6px 6px;
  min-height: 40px;
  max-height: 160px;
}

.admin-transaction-bar__value {
  font-weight: 800;
  color: #0b3b32;
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

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
