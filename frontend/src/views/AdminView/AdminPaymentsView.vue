<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminPayments, fetchAdminPaymentDetail, fetchAdminPaymentMetrics, refundPayment } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'
import { getPaymentStatusLabel, getPaymentStatusVariant } from '../../constants/adminPaymentStatus'
import { canRefundReservation, refundBlockReason } from '../../constants/adminReservationPolicy'

const stats = ref([])
const paymentList = ref([])
const searchQuery = ref('')
const statusFilter = ref('ALL')
const typeFilter = ref('ALL')
const transactionYear = ref(new Date().getFullYear())
const transactionMode = ref('yearly')
const transactionSeries = ref([])
const chartLoading = ref(false)
const chartError = ref('')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailData = ref(null)
const refundOpen = ref(false)
const refundLoading = ref(false)
const refundError = ref('')
const refundTarget = ref(null)
const refundReason = ref('')
const route = useRoute()
const router = useRouter()

const normalizeStatusFilter = (value) => {
  if (!value) return 'ALL'
  const normalized = String(value).trim().toUpperCase()
  return ['ALL', 'SUCCESS', 'FAILED', 'REFUNDED'].includes(normalized) ? normalized : 'ALL'
}

const normalizeTypeFilter = (value) => {
  if (!value) return 'ALL'
  const normalized = String(value).trim().toUpperCase()
  return ['ALL', 'RESERVATION', 'REFUND'].includes(normalized) ? normalized : 'ALL'
}

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

const loadPaymentMetrics = async () => {
  const requestInfo = {
    url: '/api/admin/payments/metrics',
    params: {
      mode: transactionMode.value,
      year: transactionYear.value,
      status: statusFilter.value,
      type: typeFilter.value,
      keyword: searchQuery.value || undefined
    }
  }
  chartLoading.value = true
  chartError.value = ''
  const params = {
    mode: transactionMode.value,
    year: transactionYear.value,
    status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
    type: typeFilter.value === 'ALL' ? undefined : typeFilter.value,
    keyword: searchQuery.value || undefined
  }
  try {
    const response = await fetchAdminPaymentMetrics(params)
    if (response.ok && Array.isArray(response.data)) {
      transactionSeries.value = response.data
    } else if (response.ok) {
      const error = new Error('Invalid metrics response shape')
      console.error('[AdminPaymentsView] metrics response shape error', {
        request: requestInfo,
        status: response?.status,
        responseBody: response?.data,
        error
      })
      chartError.value = '차트를 불러오지 못했습니다.'
      transactionSeries.value = []
    } else {
      console.error('[AdminPaymentsView] metrics request failed', {
        request: requestInfo,
        status: response?.status,
        responseBody: response?.data
      })
      chartError.value = '차트를 불러오지 못했습니다.'
      transactionSeries.value = []
    }
  } catch (error) {
    console.error('[AdminPaymentsView] metrics exception', {
      request: requestInfo,
      error
    })
    chartError.value = '차트를 불러오지 못했습니다.'
    transactionSeries.value = []
  }
  chartLoading.value = false
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
  statusFilter.value = normalizeStatusFilter(route.query.status)
  typeFilter.value = normalizeTypeFilter(route.query.type)
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadPayments()
  loadPaymentMetrics()
})

const mapStatusFilter = (filter) => {
  if (filter === 'SUCCESS') return '1'
  if (filter === 'FAILED') return '2'
  if (filter === 'REFUNDED') return '3'
  return filter
}

const setTransactionMode = (mode) => {
  transactionMode.value = mode
  transactionSeries.value = []
  loadPaymentMetrics()
}

const transactionMax = computed(() => {
  const values = transactionSeries.value.map((item) => Number(item?.totalAmount ?? 0))
  return values.length ? Math.max(...values, 1) : 1
})
const transactionHeight = (value) => `${(value / transactionMax.value) * 100}%`

const chartTitle = computed(() => {
  if (transactionMode.value === 'monthly') {
    return `${transactionYear.value}년 월별 거래액`
  }
  return '연도별 거래액'
})

const hasChartData = computed(() => {
  return transactionSeries.value.some((item) => Number(item?.totalAmount ?? 0) > 0)
})

const filteredPayments = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return paymentList.value.filter((item) => {
    const matchesQuery = !query ||
      String(item.paymentId ?? '').includes(query) ||
      (item.orderId ?? '').toLowerCase().includes(query) ||
      (item.pgPaymentKey ?? '').toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'ALL' || String(item.paymentStatus) === mapStatusFilter(statusFilter.value)
    const matchesType = typeFilter.value === 'ALL' ||
      (typeFilter.value === 'REFUND' ? item.paymentStatus === 3 : item.paymentStatus !== 3)
    return matchesQuery && matchesStatus && matchesType
  })
})

const getRefundStatusLabel = (status) => {
  const code = Number(status)
  if (!Number.isFinite(code)) return '-'
  if (code === 0) return '요청'
  if (code === 1) return '완료'
  if (code === 2) return '실패'
  return `알 수 없음(${status})`
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '-'
  return `₩${Number(value).toLocaleString()}`
}

const handleRefund = (item) => {
  if (!item?.paymentId) return
  refundTarget.value = item
  refundError.value = ''
  refundReason.value = ''
  refundOpen.value = true
}

const closeRefund = () => {
  refundOpen.value = false
  refundLoading.value = false
  refundError.value = ''
  refundTarget.value = null
  refundReason.value = ''
}

const confirmRefund = async () => {
  if (!refundTarget.value?.paymentId) return
  refundLoading.value = true
  refundError.value = ''
  const reason = refundReason.value.trim()
  const payload = reason ? { reason } : {}
  const response = await refundPayment(refundTarget.value.paymentId, payload)
  if (response.ok) {
    closeRefund()
    loadPayments()
    loadPaymentMetrics()
    return
  }
  refundError.value = response?.data?.message || '환불 처리에 실패했습니다.'
  refundLoading.value = false
}

const canRefund = (item) => canRefundReservation(item ?? {})
const refundBlockMessage = (item) => {
  if (!item) return ''
  return refundBlockReason(item ?? {})
}

const openDetail = async (item) => {
  if (!item?.paymentId) return
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  const response = await fetchAdminPaymentDetail(item.paymentId)
  if (response.ok && response.data) {
    detailData.value = response.data
  } else {
    detailError.value = '결제 상세 정보를 불러오지 못했습니다.'
  }
  detailLoading.value = false
}

const closeDetail = () => {
  detailOpen.value = false
  detailLoading.value = false
  detailError.value = ''
  detailData.value = null
}

const refreshDetail = async () => {
  if (!detailData.value?.paymentId) return
  detailLoading.value = true
  detailError.value = ''
  const response = await fetchAdminPaymentDetail(detailData.value.paymentId)
  if (response.ok && response.data) {
    detailData.value = response.data
  } else {
    detailError.value = '결제 상세 정보를 불러오지 못했습니다.'
  }
  detailLoading.value = false
}

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({
    status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value
  })
  loadPayments()
  loadPaymentMetrics()
})

watch([typeFilter, transactionYear], () => {
  loadPaymentMetrics()
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
          placeholder="거래ID, 주문번호, 결제키"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="typeFilter" class="admin-select">
          <option value="ALL">전체 유형</option>
          <option value="RESERVATION">예약</option>
          <option value="REFUND">환불</option>
        </select>
        <select v-model="statusFilter" class="admin-select">
          <option value="ALL">전체 상태</option>
          <option value="SUCCESS">완료</option>
          <option value="FAILED">실패</option>
          <option value="REFUNDED">환불</option>
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
          <h3 class="admin-card__title">{{ chartTitle }}</h3>
        </div>
        <div class="admin-toggle-group">
          <select v-if="transactionMode === 'monthly'" v-model="transactionYear" class="admin-select admin-select--compact">
            <option v-for="offset in 5" :key="offset" :value="new Date().getFullYear() - (offset - 1)">
              {{ new Date().getFullYear() - (offset - 1) }}년
            </option>
          </select>
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
      </div>
      <div v-if="chartLoading" class="admin-chart-status">불러오는 중...</div>
      <div v-else-if="chartError" class="admin-chart-status">
        <span>{{ chartError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadPaymentMetrics">다시 시도</button>
      </div>
      <div v-else-if="!hasChartData" class="admin-chart-status">선택 조건의 거래 데이터가 없습니다.</div>
      <div v-else class="admin-transaction-bars">
        <div
          v-for="item in transactionSeries"
          :key="item.label"
          class="admin-transaction-bar"
        >
          <div class="admin-transaction-bar__fill" :style="{ height: transactionHeight(item.totalAmount) }" />
          <span class="admin-transaction-bar__value">{{ item.label }}</span>
          <span class="admin-transaction-bar__amount">{{ item.totalAmount.toLocaleString() }}원</span>
        </div>
      </div>
    </div>

    <AdminTableCard title="결제 내역">
      <table class="admin-table--nowrap admin-table--tight admin-table--stretch">
        <colgroup>
          <col style="width:140px"/>
          <col style="width:220px"/>
          <col style="width:120px"/>
          <col style="width:140px"/>
          <col style="width:120px"/>
          <col style="width:140px"/>
          <col style="width:200px"/>
        </colgroup>
        <thead>
          <tr>
            <th>거래ID</th>
            <th>주문번호</th>
            <th>예약ID</th>
            <th>거래액</th>
            <th>상태</th>
            <th>날짜</th>
            <th class="admin-align-right">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredPayments" :key="item.paymentId">
            <td class="admin-strong">#{{ item.paymentId }}</td>
            <td class="admin-ellipsis" :title="item.orderId">{{ item.orderId ?? '-' }}</td>
            <td>#{{ item.reservationId ?? '-' }}</td>
            <td class="admin-align-right">₩{{ item.approvedAmount?.toLocaleString?.() ?? '0' }}</td>
            <td>
              <AdminBadge :text="getPaymentStatusLabel(item.paymentStatus)" :variant="getPaymentStatusVariant(item.paymentStatus)" />
            </td>
            <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap admin-actions-right">
                <button class="admin-btn admin-btn--ghost" type="button" @click="openDetail(item)">상세</button>
                <button
                  class="admin-btn admin-btn--danger"
                  type="button"
                  :disabled="!canRefund(item)"
                  :title="refundBlockMessage(item)"
                  @click="handleRefund(item)"
                >
                  환불 처리
                </button>
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

    <div v-if="detailOpen" class="admin-modal">
      <div class="admin-modal__card">
        <div class="admin-modal__header">
          <div>
            <p class="admin-modal__eyebrow">결제 상세</p>
            <h2 class="admin-modal__title">#{{ detailData?.paymentId ?? '-' }}</h2>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="closeDetail">닫기</button>
        </div>
        <div v-if="detailLoading" class="admin-status">불러오는 중...</div>
        <div v-else-if="detailError" class="admin-status">
          <span>{{ detailError }}</span>
          <button class="admin-btn admin-btn--ghost" type="button" @click="refreshDetail">다시 시도</button>
        </div>
        <div v-else class="admin-detail-grid">
          <div><span>예약 ID</span><strong>#{{ detailData?.reservationId ?? '-' }}</strong></div>
          <div><span>숙소 ID</span><strong>#{{ detailData?.accommodationsId ?? '-' }}</strong></div>
          <div><span>게스트 ID</span><strong>#{{ detailData?.userId ?? '-' }}</strong></div>
          <div><span>예약 상태</span><strong>{{ detailData?.reservationStatus ?? '-' }}</strong></div>
          <div><span>주문번호</span><strong>{{ detailData?.orderId ?? '-' }}</strong></div>
          <div><span>PG 키</span><strong>{{ detailData?.pgPaymentKey ?? '-' }}</strong></div>
          <div><span>요청 금액</span><strong>{{ formatCurrency(detailData?.requestAmount) }}</strong></div>
          <div><span>승인 금액</span><strong>{{ formatCurrency(detailData?.approvedAmount) }}</strong></div>
          <div><span>결제 상태</span><strong>{{ getPaymentStatusLabel(detailData?.paymentStatus) }}</strong></div>
          <div><span>결제 승인일</span><strong>{{ formatDate(detailData?.approvedAt) }}</strong></div>
          <div><span>환불 상태</span><strong>{{ getRefundStatusLabel(detailData?.refundStatus) }}</strong></div>
          <div><span>환불 금액</span><strong>{{ formatCurrency(detailData?.refundAmount) }}</strong></div>
          <div><span>환불일시</span><strong>{{ formatDate(detailData?.refundApprovedAt) }}</strong></div>
          <div><span>환불 사유</span><strong>{{ detailData?.refundReason ?? '-' }}</strong></div>
          <div><span>생성일</span><strong>{{ formatDate(detailData?.createdAt) }}</strong></div>
        </div>
      </div>
    </div>

    <div v-if="refundOpen" class="admin-modal">
      <div class="admin-modal__card admin-modal__card--small">
        <div class="admin-modal__header">
          <div>
            <p class="admin-modal__eyebrow">환불 처리</p>
            <h2 class="admin-modal__title">정말 환불 처리하시겠습니까?</h2>
          </div>
        </div>
        <div class="admin-detail-grid admin-detail-grid--compact">
          <div><span>거래ID</span><strong>#{{ refundTarget?.paymentId ?? '-' }}</strong></div>
          <div><span>거래액</span><strong>{{ formatCurrency(refundTarget?.approvedAmount) }}</strong></div>
          <div><span>상태</span><strong>{{ getPaymentStatusLabel(refundTarget?.paymentStatus) }}</strong></div>
        </div>
        <div class="admin-form">
          <label class="admin-form__label" for="refund-reason">환불 사유 (선택)</label>
          <textarea
            id="refund-reason"
            v-model="refundReason"
            class="admin-input"
            rows="3"
            placeholder="예) 고객 요청으로 환불 처리"
            :disabled="refundLoading"
          />
        </div>
        <p class="admin-modal__note">V1에서는 실제 PG 환불이 아닌 상태 변경 처리입니다.</p>
        <div v-if="refundError" class="admin-status">{{ refundError }}</div>
        <div class="admin-modal__actions">
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="refundLoading" @click="closeRefund">취소</button>
          <button class="admin-btn admin-btn--danger" type="button" :disabled="refundLoading" @click="confirmRefund">
            {{ refundLoading ? '처리 중...' : '환불 처리' }}
          </button>
        </div>
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

.admin-toggle-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.admin-select--compact {
  padding: 6px 10px;
  font-weight: 800;
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
  height: 200px;
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

.admin-transaction-bar__amount {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 700;
}

.admin-chart-status {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
  padding: 12px 0;
}

.admin-table--stretch {
  width: 100%;
  table-layout: fixed;
}

.admin-align-right {
  text-align: right;
}

.admin-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-actions-right {
  justify-content: flex-end;
}

.admin-modal {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
  padding: 16px;
}

.admin-modal__card {
  background: #fff;
  border-radius: 18px;
  padding: 20px;
  width: min(720px, 100%);
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.2);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.admin-modal__card--small {
  width: min(520px, 100%);
}

.admin-modal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.admin-modal__eyebrow {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 800;
  color: #0f766e;
}

.admin-modal__title {
  margin: 4px 0 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-modal__note {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

.admin-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  font-size: 0.95rem;
}

.admin-detail-grid--compact {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.admin-detail-grid span {
  display: block;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
  font-size: 0.8rem;
  margin-bottom: 4px;
}

.admin-detail-grid strong {
  color: #0b3b32;
  font-weight: 800;
}

.admin-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.admin-form {
  display: grid;
  gap: 6px;
}

.admin-form__label {
  font-size: 0.85rem;
  font-weight: 800;
  color: #0f172a;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
