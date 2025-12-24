<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminPayments, refundPayment } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const stats = ref([])
const paymentList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')
const transactionMode = ref('yearly')
const transactionSeries = ref([])
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()

const loadPayments = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminPayments({
    status: statusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    paymentList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
    const totalAmount = paymentList.value.reduce((sum, item) => sum + (item.approvedAmount ?? 0), 0)
    stats.value = [
      { label: '총 거래액', value: `${totalAmount.toLocaleString()}원`, sub: '현재 페이지 기준', tone: 'primary' },
      { label: '거래 건수', value: `${paymentList.value.length}건`, sub: '현재 페이지 기준', tone: 'success' },
      { label: '결제 이슈', value: `${paymentList.value.filter((p) => p.paymentStatus !== 1).length}건`, sub: '실패/환불', tone: 'accent' }
    ]
  } else {
    loadError.value = '결제 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const syncQuery = (next) => {
  const params = { ...route.query, ...next }
  const normalized = toQueryParams(params)
  const current = toQueryParams(route.query)
  const isSame = Object.keys({ ...normalized, ...current })
    .every((key) => String(normalized[key] ?? '') === String(current[key] ?? ''))
  if (!isSame) {
    router.replace({ query: normalized })
  }
}

onMounted(() => {
  statusFilter.value = route.query.status ?? 'all'
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadPayments()
})

const statusVariant = (status) => {
  if (status === 1) return 'success'
  if (status === 3) return 'warning'
  if (status === 2) return 'danger'
  return 'neutral'
}

const mapStatusFilter = (filter) => {
  if (filter === 'success') return '1'
  if (filter === 'failed') return '2'
  if (filter === 'refunded') return '3'
  return filter
}

const setTransactionMode = (mode) => {
  transactionMode.value = mode
  transactionSeries.value = []
}

const transactionMax = computed(() => Math.max(...transactionSeries.value, 1))
const transactionHeight = (value) => `${(value / transactionMax.value) * 100}%`

const filteredPayments = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return paymentList.value.filter((item) => {
    const matchesQuery = !query ||
      String(item.paymentId ?? '').includes(query) ||
      (item.orderId ?? '').toLowerCase().includes(query) ||
      (item.paymentKey ?? '').toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || String(item.paymentStatus) === mapStatusFilter(statusFilter.value)
    const matchesType = typeFilter.value === 'all'
    return matchesQuery && matchesStatus && matchesType
  })
})

const handleRefund = async (item) => {
  if (!item?.paymentId) return
  if (!confirm('해당 결제를 환불 처리하시겠습니까?')) return
  await refundPayment(item.paymentId, { reason: '관리자 환불' })
  loadPayments()
}

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({ status: statusFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadPayments()
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
          <option value="success">완료</option>
          <option value="failed">실패</option>
          <option value="refunded">환불</option>
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
            <th>거래액</th>
            <th>상태</th>
            <th>날짜</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredPayments" :key="item.paymentId">
            <td class="admin-strong">#{{ item.paymentId }}</td>
            <td>{{ item.approvedAmount?.toLocaleString?.() ?? '-' }}</td>
            <td>
              <AdminBadge :text="item.paymentStatus" :variant="statusVariant(item.paymentStatus)" />
            </td>
            <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button">상세</button>
                <button class="admin-btn admin-btn--danger" type="button" @click="handleRefund(item)">환불</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadPayments">다시 시도</button>
      </div>
      <div v-else-if="!filteredPayments.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadPayments()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadPayments()">
          다음
        </button>
      </div>
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

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
  margin-top: 12px;
}

.admin-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
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
