<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminDashboardSummary, fetchAdminDashboardTimeseries } from '../../api/adminApi'

const router = useRouter()
const stats = ref([])
const summary = ref(null)
const pendingListings = ref([])
const openReportListings = ref([])
const revenueTrend = ref({ dates: [], labels: [], values: [] })
const trendLoading = ref(false)
const trendError = ref('')
const alerts = ref([])
const pendingQuery = ref('')
const pendingStatus = ref('all')
const activePeriod = ref('30')
const isLoading = ref(false)
const loadError = ref('')

const toDateString = (date) => date.toISOString().slice(0, 10)

const buildAlerts = (openReports, pendingAcc) => {
  const reportAlerts = openReports.map((item) => ({
    id: `report-${item.reportId}`,
    title: `리뷰 신고 #${item.reportId}`,
    meta: item.title ?? '신고 사유 확인 필요',
    time: item.createdAt?.slice?.(0, 10) ?? '-',
    tone: 'warning',
    target: `/admin/reports?highlight=${item.reportId}`
  }))
  const accAlerts = pendingAcc.map((item) => ({
    id: `acc-${item.accommodationsId}`,
    title: `숙소 심사 대기 #${item.accommodationsId}`,
    meta: item.name ?? '승인 대기 숙소',
    time: item.createdAt?.slice?.(0, 10) ?? '-',
    tone: 'accent',
    target: `/admin/accommodations?highlight=${item.accommodationsId}`
  }))
  return [...reportAlerts, ...accAlerts].slice(0, 6)
}

const resolveRange = (days) => {
  const count = Number(days) || 30
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (count - 1))
  return { from: toDateString(start), to: toDateString(end) }
}

const formatCurrency = (value) => `₩${Number(value ?? 0).toLocaleString()}`
const formatRate = (value) => `${(Number(value ?? 0) * 100).toFixed(1)}%`
const formatCurrencySafe = (value) => `₩${Number(value ?? 0).toLocaleString()}`
const formatRoomCount = (value) => Number(value ?? 0).toLocaleString()
const formatPhone = (value) => value || '미등록'

const loadDashboard = async () => {
  isLoading.value = true
  loadError.value = ''
  trendLoading.value = true
  trendError.value = ''
  const range = resolveRange(activePeriod.value)
  const [summaryResponse, trendResponse] = await Promise.all([
    fetchAdminDashboardSummary(range),
    fetchAdminDashboardTimeseries({ metric: 'platform_fee', ...range })
  ])
  if (summaryResponse.ok && summaryResponse.data) {
    const data = summaryResponse.data
    summary.value = data
    const platformFeeRate = data.platformFeeRate ?? 0
    stats.value = [
      { label: '승인 대기 숙소', value: `${data.pendingAccommodations ?? 0}건`, sub: '심사 대기', tone: 'warning', target: '/admin/accommodations?status=PENDING' },
      { label: '미처리 신고', value: `${data.openReports ?? 0}건`, sub: '처리 필요', tone: 'warning', target: '/admin/reports?status=WAIT' },
      { label: '예약 생성', value: `${data.reservationCount ?? 0}건`, sub: '선택 기간 기준', tone: 'success', target: '/admin/bookings?sort=latest' },
      { label: '결제 성공', value: formatCurrency(data.paymentSuccessAmount), sub: '선택 기간 기준', tone: 'accent', target: '/admin/payments?status=success' },
      { label: '플랫폼 수익(수수료)', value: formatCurrency(data.platformFeeAmount), sub: `수수료율 ${formatRate(platformFeeRate)}`, tone: 'primary', target: '/admin/payments?status=success' },
      { label: '결제 실패', value: `${data.paymentFailureCount ?? 0}건`, sub: '실패/취소', tone: 'neutral', target: '/admin/payments?status=failed' },
      { label: '환불 요청', value: `${data.refundRequestCount ?? 0}건`, sub: '요청 건수', tone: 'neutral', target: '/admin/payments?type=refund' },
      { label: '환불 완료', value: `${data.refundCompletedCount ?? 0}건`, sub: '완료 건수', tone: 'neutral', target: '/admin/payments?type=refund' }
    ]
    pendingListings.value = data.pendingAccommodationsList ?? []
    openReportListings.value = data.openReportsList ?? []
    alerts.value = buildAlerts(openReportListings.value, pendingListings.value)
    if (trendResponse.ok && trendResponse.data) {
      const points = trendResponse.data.points ?? []
      revenueTrend.value = {
        dates: points.map((point) => point.date),
        labels: points.map((point) => point.date?.slice?.(5) ?? point.date),
        values: points.map((point) => Number(point.value ?? 0))
      }
    } else {
      trendError.value = '수익 추이를 불러오지 못했습니다.'
      revenueTrend.value = { dates: [], labels: [], values: [] }
    }
  } else {
    loadError.value = '대시보드 데이터를 불러오지 못했습니다.'
  }
  trendLoading.value = false
  isLoading.value = false
}

const filteredPending = computed(() => {
  const query = pendingQuery.value.trim().toLowerCase()
  return pendingListings.value.filter((item) => {
    const matchesQuery = !query ||
      (item.name ?? '').toLowerCase().includes(query) ||
      String(item.hostUserId ?? '').includes(query) ||
      `${item.city ?? ''} ${item.district ?? ''}`.toLowerCase().includes(query)
    const matchesStatus = pendingStatus.value === 'all' || item.approvalStatus === pendingStatus.value
    return matchesQuery && matchesStatus
  })
})

const pendingStatusVariant = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'neutral'
}

const alertVariant = (tone) => {
  if (tone === 'warning') return 'warning'
  if (tone === 'success') return 'success'
  if (tone === 'accent') return 'accent'
  return 'neutral'
}

const summaryItems = computed(() => {
  if (!summary.value) return []
  return [
    { label: '승인 대기 숙소', sub: '심사 필요', value: `${summary.value.pendingAccommodations ?? 0}건` },
    { label: '미처리 신고', sub: '처리 필요', value: `${summary.value.openReports ?? 0}건` },
    { label: '결제 실패', sub: '실패/취소', value: `${summary.value.paymentFailureCount ?? 0}건` },
    { label: '환불 요청', sub: '접수 건수', value: `${summary.value.refundRequestCount ?? 0}건` },
    { label: '환불 완료', sub: '완료 건수', value: `${summary.value.refundCompletedCount ?? 0}건` }
  ]
})

const activeRevenueIndex = ref(null)

const revenueSeriesFull = computed(() => {
  const dates = revenueTrend.value.dates ?? []
  const labels = revenueTrend.value.labels ?? []
  const values = revenueTrend.value.values ?? []
  return dates.map((date, idx) => ({
    date,
    label: labels[idx] ?? '',
    value: Number(values[idx] ?? 0)
  }))
})

const niceStep = (rawStep) => {
  const step = Math.abs(rawStep)
  if (!Number.isFinite(step) || step === 0) return 1
  const pow10 = Math.pow(10, Math.floor(Math.log10(step)))
  const err = step / pow10
  const nice = err <= 1 ? 1 : err <= 2 ? 2 : err <= 5 ? 5 : 10
  return nice * pow10
}

const revenueScale = computed(() => ({ divisor: 10000, unitText: '만원', decimals: 0 }))

const revenueDomain = computed(() => {
  const values = revenueSeriesFull.value.map((point) => point.value)
  const { divisor } = revenueScale.value
  if (!values.length) {
    return { minRaw: 0, maxRaw: 0, rangeRaw: 1, ticksRaw: [0] }
  }
  const minU = Math.min(0, ...values) / divisor
  const maxU = Math.max(0, ...values) / divisor
  if (minU === maxU) {
    const only = minU
    const ticksU = [only + 2, only + 1, only, only - 1, only - 2]
    const ticksRaw = ticksU.map((tick) => tick * divisor)
    return { minRaw: (only - 2) * divisor, maxRaw: (only + 2) * divisor, rangeRaw: 4 * divisor, ticksRaw }
  }
  const targetTicks = 5
  const stepU = niceStep((maxU - minU) / (targetTicks - 1))
  const niceMinU = Math.floor(minU / stepU) * stepU
  const niceMaxU = Math.ceil(maxU / stepU) * stepU
  const ticksU = []
  for (let value = niceMaxU; value >= niceMinU - stepU / 2; value -= stepU) {
    ticksU.push(value)
  }
  const ticksRaw = ticksU.map((tick) => tick * divisor)
  const minRaw = niceMinU * divisor
  const maxRaw = niceMaxU * divisor
  const rangeRaw = Math.max(maxRaw - minRaw, 1)
  return { minRaw, maxRaw, rangeRaw, ticksRaw }
})

const revenueBaselineFromTopPct = computed(() => {
  const { minRaw, maxRaw } = revenueDomain.value
  const top = Math.max(0, maxRaw)
  const bottom = Math.min(0, minRaw)
  const range = Math.max(top - bottom, 1)
  return (top / range) * 100
})

const revenueBaselineFromBottomPct = computed(() => {
  return 100 - revenueBaselineFromTopPct.value
})

const revenueTicks = computed(() => revenueDomain.value.ticksRaw)

const formatRevenueTick = (raw) => {
  const { divisor, decimals } = revenueScale.value
  const value = raw / divisor
  return decimals > 0 ? value.toFixed(decimals) : Math.round(value).toLocaleString()
}

const revenuePositiveStyle = (value) => {
  const { minRaw, maxRaw } = revenueDomain.value
  const top = Math.max(0, maxRaw)
  const bottom = Math.min(0, minRaw)
  const range = Math.max(top - bottom, 1)
  const height = (Math.max(0, value) / range) * 100
  if (height <= 0) return null
  return { height: `${height}%` }
}

const revenueNegativeStyle = (value) => {
  const { minRaw, maxRaw } = revenueDomain.value
  const top = Math.max(0, maxRaw)
  const bottom = Math.min(0, minRaw)
  const range = Math.max(top - bottom, 1)
  const height = (Math.max(0, -value) / range) * 100
  if (height <= 0) return null
  return { height: `${height}%` }
}

const revenueAxisLabels = computed(() => {
  const labels = revenueSeriesFull.value.map((point) => point.label)
  if (!labels.length) return []
  const last = labels.length - 1
  return labels.map((label, idx) => (idx === 0 || idx === last || idx % 7 === 0 ? label : ''))
})

const handleRevenueEnter = (idx) => {
  activeRevenueIndex.value = idx
}

const handleRevenueLeave = () => {
  activeRevenueIndex.value = null
}

const hasRevenueData = computed(() => {
  return revenueTrend.value.values?.some((value) => Number(value ?? 0) !== 0)
})

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

watch(activePeriod, loadDashboard)
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
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
      </div>
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
            <option value="PENDING">대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">반려</option>
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
            <tr v-for="item in filteredPending" :key="item.accommodationsId">
              <td class="admin-strong">#{{ item.accommodationsId }}</td>
              <td class="admin-strong">{{ item.name }}</td>
              <td>{{ item.hostUserId }}</td>
              <td>{{ item.city }} {{ item.district }}</td>
              <td>{{ item.category }}</td>
              <td>{{ formatRoomCount(item.roomCount) }}</td>
              <td>{{ formatCurrencySafe(item.minPrice) }}</td>
              <td>{{ formatPhone(item.hostPhone) }}</td>
              <td>{{ item.createdAt?.slice?.(0, 10) ?? '-' }}</td>
              <td>
                <AdminBadge :text="item.approvalStatus" :variant="pendingStatusVariant(item.approvalStatus)" />
              </td>
              <td>
                <button class="admin-btn admin-btn--ghost" type="button" @click="goTo(`/admin/accommodations?highlight=${item.accommodationsId}`)">
                  상세 보기
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filteredPending.length" class="admin-status">승인 대기 숙소가 없습니다.</div>
      </AdminTableCard>

    </div>

    <div class="admin-grid admin-grid--2 admin-grid--summary">
      <div class="admin-card admin-alerts-card">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">운영 알림</p>
            <h3 class="admin-card__title">오늘 확인할 이슈</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/issues')">전체 보기</button>
        </div>
        <div class="admin-alert-list">
          <button
            v-for="alert in alerts"
            :key="alert.id"
            class="admin-alert-item"
            type="button"
            @click="goTo(alert.target)"
          >
            <div>
              <div class="admin-alert-title">{{ alert.title }}</div>
              <div class="admin-alert-meta">{{ alert.meta }}</div>
            </div>
            <div class="admin-alert-right">
              <AdminBadge :text="alert.time" :variant="alertVariant(alert.tone)" />
            </div>
          </button>
          <div v-if="!alerts.length" class="admin-status">현재 확인할 알림이 없습니다.</div>
        </div>
      </div>

      <div class="admin-card admin-highlight">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">운영 요약</p>
            <h3 class="admin-card__title">이번 주 주요 지표</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/weekly')">리포트 보기</button>
        </div>
        <div class="admin-highlight-list">
          <div v-for="item in summaryItems" :key="item.label" class="admin-highlight-item">
            <div>
              <p class="admin-highlight-label">{{ item.label }}</p>
              <p class="admin-highlight-sub">{{ item.sub }}</p>
            </div>
            <p class="admin-highlight-value">{{ item.value }}</p>
          </div>
          <div v-if="!summaryItems.length" class="admin-status">요약 데이터를 불러오는 중입니다.</div>
        </div>
      </div>
    </div>

    <div class="admin-card admin-placeholder-chart">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">수익 추이</p>
          <h3 class="admin-card__title">플랫폼 수익(수수료) 추이</h3>
        </div>
      </div>
      <div class="admin-chart-area">
        <div v-if="isLoading || trendLoading" class="admin-status">불러오는 중...</div>
        <div v-else-if="loadError || trendError" class="admin-status">
          <span>{{ loadError || trendError }}</span>
          <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
        </div>
        <div v-else-if="!hasRevenueData" class="admin-status">표시할 데이터가 없습니다.</div>
        <template v-else>
          <div class="admin-chart-y">
            <span class="admin-chart-y__unit">(단위: {{ revenueScale.unitText }})</span>
            <div class="admin-chart-y__ticks">
              <span v-for="tick in revenueTicks" :key="tick">{{ formatRevenueTick(tick) }}</span>
            </div>
          </div>
          <div class="admin-chart-bars" :style="{ '--zero-pos': `${revenueBaselineFromTopPct}%` }">
            <div class="admin-chart-zero-line" :style="{ top: `${revenueBaselineFromTopPct}%` }" />
            <div
              v-for="(item, idx) in revenueSeriesFull"
              :key="idx"
              class="admin-chart-bar"
              tabindex="0"
              @mouseenter="handleRevenueEnter(idx)"
              @mouseleave="handleRevenueLeave"
              @focus="handleRevenueEnter(idx)"
              @blur="handleRevenueLeave"
            >
              <div
                v-if="item.value > 0"
                class="admin-chart-bar__fill admin-chart-bar__fill--positive"
                :style="revenuePositiveStyle(item.value)"
              />
              <div
                v-else-if="item.value < 0"
                class="admin-chart-bar__fill admin-chart-bar__fill--negative"
                :style="revenueNegativeStyle(item.value)"
              />
              <div v-if="activeRevenueIndex === idx" class="admin-chart-tooltip">
                <p class="admin-chart-tooltip__date">{{ item.date }}</p>
                <p class="admin-chart-tooltip__value">{{ formatCurrency(item.value) }}</p>
              </div>
            </div>
          </div>
        </template>
      </div>
      <div class="admin-chart-x">
        <span
          v-for="(label, idx) in revenueAxisLabels"
          :key="idx"
          class="admin-chart-x__tick"
          :class="{ 'is-empty': !label }"
          aria-hidden="true"
        >
          {{ label || '•' }}
        </span>
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
  height: 200px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #6b7280;
  font-weight: 700;
}

.admin-chart-y__unit {
  font-size: 0.75rem;
  opacity: 0.8;
  margin-bottom: 2px;
}

.admin-chart-y__ticks {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.admin-chart-bars {
  position: relative;
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(8px, 1fr);
  gap: 10px;
  align-items: stretch;
  height: 200px;
  background:
    linear-gradient(to bottom, rgba(15, 118, 110, 0.08) 1px, transparent 1px) 0 0 / 100% 25%,
    linear-gradient(to bottom, rgba(15, 118, 110, 0.08) 1px, transparent 1px) 0 0 / 100% 50%,
    linear-gradient(to bottom, rgba(15, 118, 110, 0.08) 1px, transparent 1px) 0 0 / 100% 75%;
}

.admin-chart-zero-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(15, 118, 110, 0.25);
  pointer-events: none;
}

.admin-chart-bar {
  position: relative;
  background: transparent;
  border-radius: 10px;
  min-height: 0;
  outline: none;
}

.admin-chart-tooltip {
  position: absolute;
  bottom: 100%;
  transform: translateY(-8px);
  background: #0f172a;
  color: #f8fafc;
  padding: 6px 8px;
  border-radius: 8px;
  font-size: 0.75rem;
  white-space: nowrap;
  z-index: 2;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.25);
}

.admin-chart-tooltip__date {
  margin: 0;
  opacity: 0.8;
}

.admin-chart-tooltip__value {
  margin: 2px 0 0;
  font-weight: 800;
}

.admin-chart-bar__fill {
  position: absolute;
  left: 0;
  right: 0;
  width: 100%;
  border-radius: 8px 8px 6px 6px;
  background: linear-gradient(180deg, #0f766e 0%, #14b8a6 100%);
}

.admin-chart-bar__fill--positive {
  bottom: calc(100% - var(--zero-pos));
}

.admin-chart-bar__fill--negative {
  background: linear-gradient(180deg, #fca5a5 0%, #ef4444 100%);
  border-radius: 6px 6px 8px 8px;
  top: var(--zero-pos);
}

.admin-chart-x {
  margin-top: 10px;
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(8px, 1fr);
  gap: 10px;
  text-align: center;
  color: #6b7280;
  font-weight: 700;
}

.admin-chart-x__tick {
  white-space: nowrap;
  transform: rotate(-30deg);
  display: inline-block;
  font-size: 0.75rem;
}

.admin-chart-x__tick.is-empty {
  visibility: hidden;
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
