<script setup>
import { onMounted, ref, computed } from 'vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { payments, paymentsStats, dashboardTransactions } from '../../mocks/adminMockData'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const stats = ref([])
const paymentList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')
const transactionMode = ref('yearly')
const transactionSeries = ref([])

const loadPayments = () => {
  stats.value = [
    { label: '총 거래액', value: paymentsStats.totalVolume, sub: '주간 +5.2%', tone: 'primary' },
    { label: '플랫폼 수수료', value: paymentsStats.platformFee, sub: '수수료율 7%', tone: 'accent' },
    { label: '거래 건수', value: paymentsStats.transactions, sub: '성공률 98.2%', tone: 'success' }
  ]
  paymentList.value = payments
  setTransactionMode('yearly')
}

onMounted(loadPayments)

const statusVariant = (status) => {
  if (status === '완료') return 'success'
  if (status === '환불') return 'warning'
  if (status === '보류') return 'danger'
  return 'neutral'
}

const setTransactionMode = (mode) => {
  transactionMode.value = mode
  transactionSeries.value = dashboardTransactions[mode] || []
}

const transactionMax = computed(() => Math.max(...transactionSeries.value, 1))
const transactionHeight = (value) => `${(value / transactionMax.value) * 100}%`

const filteredPayments = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return paymentList.value.filter((item) => {
    const matchesQuery = !query ||
      item.id.toLowerCase().includes(query) ||
      item.host.toLowerCase().includes(query) ||
      item.guest.toLowerCase().includes(query) ||
      item.accommodation.toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchesType = typeFilter.value === 'all' || item.type === typeFilter.value
    return matchesQuery && matchesStatus && matchesType
  })
})

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '결제 내역',
      columns: [
        { key: 'id', label: '거래ID' },
        { key: 'host', label: '호스트' },
        { key: 'guest', label: '게스트' },
        { key: 'accommodation', label: '숙소' },
        { key: 'amount', label: '거래액' },
        { key: 'fee', label: '수수료' },
        { key: 'type', label: '유형' },
        { key: 'status', label: '상태' },
        { key: 'method', label: '결제수단' },
        { key: 'settlementDate', label: '정산일' },
        { key: 'date', label: '날짜' }
      ],
      rows: filteredPayments.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-payments-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-payments-${today}.csv`, sheets })
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">결제 관리</h1>
        <p class="admin-subtitle">거래 흐름과 수수료를 모니터링합니다.</p>
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

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="거래ID, 호스트, 게스트, 숙소"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="typeFilter" class="admin-select">
          <option value="all">전체 유형</option>
          <option value="예약">예약</option>
          <option value="환불">환불</option>
        </select>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="완료">완료</option>
          <option value="환불">환불</option>
          <option value="보류">보류</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">정산</span>
        <button class="admin-btn admin-btn--ghost" type="button">정산 예정</button>
        <button class="admin-btn admin-btn--primary" type="button">정산 실행</button>
      </div>
      <div class="admin-filter-group">
        <details class="admin-dropdown">
          <summary class="admin-btn admin-btn--ghost">다운로드</summary>
          <div class="admin-dropdown__menu">
            <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">CSV</button>
            <button class="admin-btn admin-btn--primary admin-dropdown__item" type="button" @click="downloadReport('xlsx')">XLSX</button>
          </div>
        </details>
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
          <div class="admin-transaction-bar__fill" :style="{ height: transactionHeight(value) }" />
          <span class="admin-transaction-bar__value">{{ value }}</span>
        </div>
      </div>
      <p class="admin-hint">실제 차트 라이브러리는 추후 연동 예정</p>
    </div>

    <AdminTableCard title="결제 내역">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>거래ID</th>
            <th>호스트</th>
            <th>게스트</th>
            <th>숙소</th>
            <th>거래액</th>
            <th>수수료</th>
            <th>유형</th>
            <th>상태</th>
            <th>결제수단</th>
            <th>정산일</th>
            <th>날짜</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredPayments" :key="item.id">
            <td class="admin-strong">{{ item.id }}</td>
            <td>{{ item.host }}</td>
            <td>{{ item.guest }}</td>
            <td>{{ item.accommodation }}</td>
            <td>{{ item.amount }}</td>
            <td>{{ item.fee }}</td>
            <td>{{ item.type }}</td>
            <td>
              <AdminBadge :text="item.status" :variant="statusVariant(item.status)" />
            </td>
            <td>{{ item.method }}</td>
            <td>{{ item.settlementDate }}</td>
            <td>{{ item.date }}</td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">영수증</button>
                <button class="admin-btn admin-btn--muted" type="button">정산</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </AdminTableCard>
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

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
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
  height: 180px;
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
  min-height: 0;
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

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
