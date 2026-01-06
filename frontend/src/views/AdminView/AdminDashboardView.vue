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

const formatDateOnly = (value) => (value ? value.slice(0, 10) : '-')

const latestDateOf = (items) => {
  if (!items?.length) return '-'
  const latest = items
    .map((item) => item.createdAt)
    .filter(Boolean)
    .sort()
    .pop()
  return formatDateOnly(latest)
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

const issueCards = computed(() => {
  if (!summary.value) return []
  const today = toDateString(new Date())
  const pendingToday = pendingListings.value.filter((item) => item.createdAt?.slice?.(0, 10) === today).length
  const latestPending = latestDateOf(pendingListings.value)
  const overdueReports = openReportListings.value.filter((item) => {
    const createdAt = item.createdAt ? new Date(item.createdAt).getTime() : 0
    return createdAt && createdAt <= Date.now() - 48 * 60 * 60 * 1000
  }).length
  const latestReport = latestDateOf(openReportListings.value)
  const range = resolveRange(activePeriod.value)

  return [
    {
      id: 'pending',
      title: '숙소 승인 대기',
      count: summary.value.pendingAccommodations ?? 0,
      tone: 'warning',
      badge: '주의',
      target: '/admin/accommodations?status=PENDING',
      meta: [
        { label: '오늘 신규', value: pendingListings.value.length ? `${pendingToday}건` : '-' },
        { label: '최신 신청일', value: latestPending }
      ]
    },
    {
      id: 'reports',
      title: '미처리 신고',
      count: summary.value.openReports ?? 0,
      tone: 'danger',
      badge: '긴급',
      target: '/admin/reports?status=WAIT',
      meta: [
        { label: '48시간 초과', value: openReportListings.value.length ? `${overdueReports}건` : '-' },
        { label: '최근 신고일', value: latestReport }
      ]
    },
    {
      id: 'payments',
      title: '결제 실패/취소',
      count: summary.value.paymentFailureCount ?? 0,
      tone: 'neutral',
      badge: '일반',
      target: '/admin/payments?status=failed',
      meta: [
        { label: '최근 24h', value: '-' },
        { label: '선택 기간', value: `${range.from} ~ ${range.to}` }
      ]
    },
    {
      id: 'refund-request',
      title: '환불 요청',
      count: summary.value.refundRequestCount ?? 0,
      tone: 'accent',
      badge: '요청',
      target: '/admin/payments?type=refund',
      meta: [
        { label: '요청/완료', value: `${summary.value.refundRequestCount ?? 0}/${summary.value.refundCompletedCount ?? 0}` },
        { label: '최근 24h', value: '-' }
      ]
    },
    {
      id: 'refund-completed',
      title: '환불 완료',
      count: summary.value.refundCompletedCount ?? 0,
      tone: 'success',
      badge: '완료',
      target: '/admin/payments?type=refund',
      meta: [
        { label: '완료/요청', value: `${summary.value.refundCompletedCount ?? 0}/${summary.value.refundRequestCount ?? 0}` },
        { label: '선택 기간', value: `${range.from} ~ ${range.to}` }
      ]
    }
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
  const maxU = Math.max(0, ...values) / divisor
  if (maxU === 0) {
    return { minRaw: 0, maxRaw: divisor * 4, rangeRaw: divisor * 4, ticksRaw: [0, divisor, divisor * 2, divisor * 3, divisor * 4] }
  }
  const targetTicks = 5
  const stepU = niceStep(maxU / (targetTicks - 1))
  const niceMaxU = stepU * (targetTicks - 1)
  const ticksU = Array.from({ length: targetTicks }, (_, idx) => niceMaxU - stepU * idx)
  const ticksRaw = ticksU.map((tick) => tick * divisor)
  const minRaw = 0
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
  const period = Number(activePeriod.value) || 30
  const points = revenueSeriesFull.value
  const labels = points.map((point) => point.label)
  if (!labels.length) return []
  if (period <= 7) return labels
  const last = labels.length - 1
  const weekTicks = []
  for (let idx = 1; idx < last; idx += 1) {
    const date = points[idx]?.date
    if (!date) continue
    const day = new Date(`${date}T00:00:00`).getDay()
    if (day === 1) weekTicks.push(idx)
  }
  const filteredWeekTicks = weekTicks.filter((idx) => last - idx > 2)
  const baseIndices = [0, ...filteredWeekTicks, last]
  let selectedIndices = baseIndices
  const maxTicks = 8
  if (baseIndices.length > maxTicks) {
    const keep = maxTicks - 2
    const mids = baseIndices.slice(1, -1)
    const step = Math.ceil(mids.length / Math.max(keep, 1))
    const sampled = mids.filter((_, idx) => idx % step === 0).slice(0, keep)
    selectedIndices = [0, ...sampled, last]
  }
  const labelIndices = new Set(selectedIndices)
  return labels.map((label, idx) => (labelIndices.has(idx) ? label : ''))
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
            <p class="admin-card__eyebrow">운영 이슈 센터</p>
            <h3 class="admin-card__title">바로 조치할 항목</h3>
          </div>
        </div>
        <div class="admin-issue-grid">
          <div
            v-for="card in issueCards"
            :key="card.id"
            class="admin-issue-card"
            :class="`admin-issue-card--${card.tone}`"
          >
            <div class="admin-issue-card__head">
              <div>
                <p class="admin-issue-card__title">{{ card.title }}</p>
                <p class="admin-issue-card__count">{{ card.count }}건</p>
              </div>
              <AdminBadge :text="card.badge" :variant="card.tone" />
            </div>
            <div class="admin-issue-card__meta">
              <div v-for="item in card.meta" :key="item.label" class="admin-issue-card__meta-row">
                <span class="admin-issue-card__meta-label">{{ item.label }}</span>
                <span class="admin-issue-card__meta-value">{{ item.value }}</span>
              </div>
            </div>
            <div class="admin-issue-card__footer">
              <button class="admin-btn admin-btn--ghost" type="button" @click="goTo(card.target)">
                바로가기
              </button>
            </div>
          </div>
          <div v-if="!issueCards.length" class="admin-status">이슈 데이터를 불러오는 중입니다.</div>
        </div>
        <div class="admin-card__footer">
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/issues')">전체 보기</button>
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
            <span v-for="tick in revenueTicks" :key="tick">{{ formatRevenueTick(tick) }}</span>
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

.admin-card__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
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
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding-top: 14px;
  color: #6b7280;
  font-weight: 700;
}

.admin-chart-y__unit {
  position: absolute;
  top: -2px;
  left: 0;
  font-size: 0.75rem;
  opacity: 0.8;
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

.admin-chart-x__tick:first-child {
  justify-self: start;
  transform-origin: left;
}

.admin-chart-x__tick:last-child {
  justify-self: end;
  transform-origin: right;
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

  .admin-issue-grid {
    grid-template-columns: 1fr;
  }
}

.admin-alerts-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-issue-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.admin-issue-card {
  position: relative;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 12px 14px 10px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.admin-issue-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 12px;
  background: #94a3b8;
}

.admin-issue-card--warning::before {
  background: #f59e0b;
}

.admin-issue-card--danger::before {
  background: #ef4444;
}

.admin-issue-card--neutral::before {
  background: #94a3b8;
}

.admin-issue-card--accent::before {
  background: #3b82f6;
}

.admin-issue-card--success::before {
  background: #10b981;
}

.admin-issue-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.admin-issue-card__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0b3b32;
}

.admin-issue-card__count {
  margin: 4px 0 0;
  font-size: 1.2rem;
  font-weight: 900;
  color: #111827;
}

.admin-issue-card__meta {
  display: grid;
  gap: 6px;
}

.admin-issue-card__meta-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 0.85rem;
  color: #6b7280;
}

.admin-issue-card__meta-label {
  color: #6b7280;
  font-weight: 600;
}

.admin-issue-card__meta-value {
  color: #111827;
  font-weight: 700;
}

.admin-issue-card__footer {
  margin-top: 2px;
  display: flex;
  justify-content: flex-end;
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f766e;
}

.admin-issue-card__link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
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
