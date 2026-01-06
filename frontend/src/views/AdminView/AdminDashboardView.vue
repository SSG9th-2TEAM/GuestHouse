<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import AdminBarChart from '../../components/admin/AdminBarChart.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminDashboardSummary, fetchAdminDashboardTimeseries } from '../../api/adminApi'
import { buildDateRange, fillSeriesByDate, formatKRW, toISODate } from '../../utils/admin/chartSeries'

const router = useRouter()
const stats = ref([])
const summary = ref(null)
const pendingListings = ref([])
const openReportListings = ref([])
const revenuePoints = ref([])
const revenueRange = ref({ from: '', to: '' })
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
  return { from: toISODate(start), to: toISODate(end) }
}

const periodLabelMap = {
  7: '최근 7일',
  30: '최근 30일',
  90: '최근 3개월'
}

const activePeriodLabel = computed(() => periodLabelMap[Number(activePeriod.value)] || '최근 30일')

const formatCurrency = (value) => formatKRW(value)
const formatRate = (value) => `${(Number(value ?? 0) * 100).toFixed(1)}%`
const formatCurrencySafe = (value) => `₩${Number(value ?? 0).toLocaleString()}`
const formatRoomCount = (value) => Number(value ?? 0).toLocaleString()
const formatPhone = (value) => value || '미등록'
const formatNumber = (value) => Number(value ?? 0).toLocaleString()
const formatDecimal = (value) => Number(value ?? 0).toFixed(1)
const formatLeadTime = (value) => {
  const numeric = Number(value ?? 0)
  if (!Number.isFinite(numeric)) return '0'
  if (numeric < 1) return '당일'
  const rounded = Math.round(numeric * 10) / 10
  return Number.isInteger(rounded) ? String(Math.round(rounded)) : rounded.toFixed(1)
}

const formatPercentValue = (value) => {
  const numeric = Number(value ?? 0)
  if (!Number.isFinite(numeric) || numeric < 1) return '0'
  const rounded = Math.round(numeric * 10) / 10
  return Number.isInteger(rounded) ? String(Math.round(rounded)) : rounded.toFixed(1)
}

const loadDashboard = async () => {
  isLoading.value = true
  loadError.value = ''
  trendLoading.value = true
  trendError.value = ''
  const range = resolveRange(activePeriod.value)
  revenueRange.value = range
  const [summaryResponse, trendResponse] = await Promise.all([
    fetchAdminDashboardSummary(range),
    fetchAdminDashboardTimeseries({ metric: 'platform_fee', ...range })
  ])
  if (summaryResponse.ok && summaryResponse.data) {
    const data = summaryResponse.data
    summary.value = data
    const platformFeeRate = data.platformFeeRate ?? 0
    stats.value = [
      { label: '승인 대기 숙소', value: `${data.pendingAccommodations ?? 0}건`, badge: '심사 대기', sub: '', tone: 'warning', target: '/admin/accommodations?status=PENDING' },
      { label: '미처리 신고', value: `${data.openReports ?? 0}건`, badge: '처리 필요', sub: '', tone: 'warning', target: '/admin/reports?status=WAIT' },
      { label: '예약 생성', value: `${data.reservationCount ?? 0}건`, badge: '선택 기간', sub: '', tone: 'success', target: '/admin/bookings?sort=latest' },
      { label: '결제 성공', value: formatCurrency(data.paymentSuccessAmount), badge: '선택 기간', sub: '', tone: 'accent', target: '/admin/payments?status=success' },
      { label: '플랫폼 수익(수수료)', value: formatCurrency(data.platformFeeAmount), badge: '선택 기간', sub: `수수료율 ${formatRate(platformFeeRate)}`, tone: 'primary', target: '/admin/payments?status=success' },
      { label: '결제 실패', value: `${data.paymentFailureCount ?? 0}건`, badge: '실패/취소', sub: '', tone: 'neutral', target: '/admin/payments?status=failed' },
      { label: '환불 요청', value: `${data.refundRequestCount ?? 0}건`, badge: '요청', sub: '', tone: 'neutral', target: '/admin/payments?type=refund' },
      { label: '환불 완료', value: `${data.refundCompletedCount ?? 0}건`, badge: '완료', sub: '', tone: 'neutral', target: '/admin/payments?type=refund' }
    ]
    pendingListings.value = data.pendingAccommodationsList ?? []
    openReportListings.value = data.openReportsList ?? []
    alerts.value = buildAlerts(openReportListings.value, pendingListings.value)
    if (trendResponse.ok && trendResponse.data) {
      const points = trendResponse.data.points ?? []
      revenuePoints.value = points
    } else {
      trendError.value = '수익 추이를 불러오지 못했습니다.'
      revenuePoints.value = []
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

const issueStatusVariant = (cardId, status) => {
  if (cardId === 'pending') return pendingStatusVariant(status)
  if (cardId === 'reports') {
    if (['WAIT', 'OPEN', 'UNRESOLVED'].includes(status)) return 'danger'
  }
  return 'neutral'
}

const formatIssueStatus = (status) => {
  if (!status) return '-'
  if (status === 'PENDING') return '대기'
  if (status === 'APPROVED') return '승인'
  if (status === 'REJECTED') return '반려'
  if (['WAIT', 'OPEN', 'UNRESOLVED'].includes(status)) return '미처리'
  if (status === 'DONE') return '처리'
  return status
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

const formatElapsed = (value) => {
  if (!value) return '-'
  const created = new Date(value)
  if (Number.isNaN(created.getTime())) return '-'
  const diffMs = Date.now() - created.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  if (diffHours < 24) return `${diffHours}시간`
  return `${Math.floor(diffHours / 24)}일`
}

const resolveReportTarget = (type) => {
  if (!type) return '기타'
  const value = String(type).toUpperCase()
  if (value.includes('REVIEW')) return '리뷰'
  if (value.includes('ACCOM')) return '숙소'
  if (value.includes('USER')) return '회원'
  if (value.includes('BOOK')) return '예약'
  return '기타'
}

const summaryItems = computed(() => {
  if (!summary.value) return []
  return [
    { label: '승인 리드타임', sub: '평균 대기일', value: formatLeadTime(summary.value.pendingLeadTimeAvgDays), unit: formatLeadTime(summary.value.pendingLeadTimeAvgDays) === '당일' ? '' : '일' },
    { label: '48시간+ 미처리 신고', sub: '리스크', value: formatNumber(summary.value.overdueOpenReports48h), unit: '건' },
    { label: '환불 요청(주간)', sub: '주간 합', value: formatNumber(summary.value.weeklyRefundRequestCount), unit: '건' },
    { label: '환불 완료(주간)', sub: '주간 합', value: formatNumber(summary.value.weeklyRefundCompletedCount), unit: '건' },
    { label: '결제 실패율(주간)', sub: '결제 실패/시도', value: formatPercentValue(summary.value.weeklyPaymentFailureRate), unit: '%' },
    { label: '7일+ 승인 대기', sub: '대기 경과', value: formatNumber(summary.value.weeklyPendingOver7Days), unit: '건' }
  ]
})

const issueCards = computed(() => {
  const data = summary.value ?? {}
  const today = toDateString(new Date())
  const pendingToday = pendingListings.value.filter((item) => item.createdAt?.slice?.(0, 10) === today).length
  const pendingPreview = pendingListings.value.slice(0, 2).map((item) => ({
    id: `#${item.accommodationsId}`,
    name: item.name ?? '-',
    date: formatDateOnly(item.createdAt),
    status: item.approvalStatus ?? 'PENDING'
  }))
  const reportPreview = openReportListings.value.slice(0, 2).map((item) => ({
    id: `#${item.reportId}`,
    target: resolveReportTarget(item.type),
    reason: item.title ?? '-',
    date: formatDateOnly(item.createdAt),
    elapsed: formatElapsed(item.createdAt),
    status: item.status ?? 'WAIT'
  }))
  const failureCount = data.paymentFailureCount ?? 0
  const refundRequestCount = data.refundRequestCount ?? 0
  const refundCompletedCount = data.refundCompletedCount ?? 0

  return [
    {
      id: 'pending',
      title: '숙소 승인 대기',
      count: data.pendingAccommodations ?? 0,
      tone: 'warning',
      target: '/admin/accommodations?status=PENDING',
      columns: [
        { key: 'id', label: 'ID', width: '0.8fr' },
        { key: 'name', label: '숙소명', width: '1.5fr' },
        { key: 'date', label: '신청일', width: '1fr' },
        { key: 'status', label: '상태', align: 'right', width: '0.9fr' }
      ],
      rows: pendingPreview,
      emptyMeta: pendingListings.value.length ? `오늘 신규 ${pendingToday}건` : '기간 내 신규 없음'
    },
    {
      id: 'reports',
      title: '미처리 신고',
      count: data.openReports ?? 0,
      tone: 'danger',
      target: '/admin/reports?status=WAIT',
      columns: [
        { key: 'id', label: '신고ID', width: '0.9fr' },
        { key: 'target', label: '대상', width: '1fr' },
        { key: 'reason', label: '사유', width: '1.6fr' },
        { key: 'date', label: '등록일', width: '1fr' },
        { key: 'elapsed', label: '경과', width: '0.9fr' },
        { key: 'status', label: '상태', align: 'right', width: '0.9fr' }
      ],
      rows: reportPreview,
      emptyMeta: '기간 내 신규 없음'
    },
    {
      id: 'payments',
      title: '결제 실패/취소',
      count: failureCount,
      tone: 'neutral',
      target: '/admin/payments?status=failed',
      columns: [
        { key: 'metric', label: '구분', width: '1.6fr' },
        { key: 'value', label: '건수', align: 'right', width: '0.8fr' }
      ],
      rows: failureCount > 0 ? [{ metric: '주간 합계', value: `${failureCount}건` }] : [],
      emptyMeta: '최근 24h 신규 없음'
    },
    {
      id: 'refunds',
      title: '환불 요청/완료',
      count: refundRequestCount + refundCompletedCount,
      tone: 'accent',
      target: '/admin/payments?type=refund',
      columns: [
        { key: 'metric', label: '구분', width: '1.6fr' },
        { key: 'value', label: '건수', align: 'right', width: '0.8fr' }
      ],
      rows: (refundRequestCount + refundCompletedCount) > 0
        ? [
            { metric: '요청', value: `${refundRequestCount}건` },
            { metric: '완료', value: `${refundCompletedCount}건` }
          ]
        : [],
      emptyMeta: '이번 기간 신규 없음'
    }
  ]
})

const resolveIssueGridTemplate = (columns) => {
  if (!columns?.length) return '1fr'
  return columns.map((col) => col.width || '1fr').join(' ')
}

const revenueLabels = computed(() => buildDateRange(revenueRange.value.from, revenueRange.value.to))
const revenueValues = computed(() => {
  if (!revenueLabels.value.length) return []
  return fillSeriesByDate(
    revenueLabels.value,
    revenuePoints.value,
    (point) => String(point.date ?? '').slice(0, 10),
    (point) => point.value
  )
})
const revenueMaxXTicks = computed(() => {
  const total = revenueLabels.value.length
  return total <= 8 ? total : 8
})
const hasRevenueData = computed(() => revenueValues.value.some((value) => Number(value ?? 0) !== 0))

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
        :badge="card.badge"
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
        <span class="admin-period-label">선택 기간: {{ activePeriodLabel }}</span>
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
              <p class="admin-issue-card__title" :title="card.title">{{ card.title }}</p>
              <p class="admin-issue-card__count">{{ card.count }}건</p>
            </div>
              <button class="admin-btn admin-btn--ghost admin-issue-card__link" type="button" @click="goTo(card.target)">
                바로가기
              </button>
            </div>
            <div class="admin-issue-card__body">
              <div v-if="isLoading" class="admin-issue-skeleton">
                <span class="admin-issue-skeleton__line" />
                <span class="admin-issue-skeleton__line" />
                <span class="admin-issue-skeleton__line admin-issue-skeleton__line--short" />
              </div>
              <div v-else-if="loadError" class="admin-issue-error">
                <p class="admin-issue-error__text">데이터를 불러오지 못했습니다.</p>
                <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">재시도</button>
              </div>
              <div v-else-if="card.count === 0" class="admin-issue-empty">
                <div class="admin-issue-empty__count">0건</div>
                <AdminBadge text="정상" variant="success" />
                <p class="admin-issue-empty__desc" :title="card.emptyMeta">{{ card.emptyMeta }}</p>
              </div>
              <div v-else class="admin-issue-table">
                <div
                  class="admin-issue-table__row admin-issue-table__head"
                  :style="{ gridTemplateColumns: resolveIssueGridTemplate(card.columns) }"
                >
                  <span
                    v-for="col in card.columns"
                    :key="col.key"
                    class="admin-issue-table__cell"
                    :class="{ 'is-right': col.align === 'right' }"
                  >
                    {{ col.label }}
                  </span>
                </div>
                <div
                  v-for="(row, rowIndex) in card.rows"
                  :key="rowIndex"
                  class="admin-issue-table__row"
                  :style="{ gridTemplateColumns: resolveIssueGridTemplate(card.columns) }"
                >
                  <span
                    v-for="col in card.columns"
                    :key="col.key"
                    class="admin-issue-table__cell"
                    :class="{ 'is-right': col.align === 'right' }"
                    :title="String(row[col.key] ?? '-')"
                  >
                    <AdminBadge
                      v-if="col.key === 'status'"
                      :text="formatIssueStatus(row[col.key])"
                      :variant="issueStatusVariant(card.id, row[col.key])"
                    />
                    <span v-else>{{ row[col.key] ?? '-' }}</span>
                  </span>
                </div>
              </div>
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
            <p class="admin-card__caption">기간: 이번 주(월~오늘)</p>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/weekly')">리포트 보기</button>
        </div>
        <div class="admin-highlight-grid">
          <div v-for="item in summaryItems" :key="item.label" class="admin-highlight-item">
            <div>
              <p class="admin-highlight-label">{{ item.label }}</p>
              <p class="admin-highlight-sub">{{ item.sub }}</p>
            </div>
            <div class="admin-highlight-metric">
              <p class="admin-highlight-value">{{ item.value }}</p>
              <span class="admin-highlight-unit">{{ item.unit }}</span>
            </div>
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
        <AdminBarChart
          v-else
          :labels="revenueLabels"
          :values="revenueValues"
          :height="260"
          :max-x-ticks="revenueMaxXTicks"
        />
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
  display: flex;
  align-items: flex-end;
  gap: 12px;
  min-height: 260px;
  margin-top: 12px;
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
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.admin-issue-card {
  position: relative;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 12px 14px 12px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-issue-card__count {
  margin: 4px 0 0;
  font-size: 1.2rem;
  font-weight: 900;
  color: #111827;
}

.admin-issue-card__link {
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-issue-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-issue-table {
  display: grid;
  gap: 6px;
  font-size: 0.82rem;
  color: #4b5563;
}

.admin-issue-table__row {
  display: grid;
  gap: 8px;
  align-items: center;
}

.admin-issue-table__head {
  font-size: 0.75rem;
  font-weight: 700;
  color: #6b7280;
}

.admin-issue-table__cell {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-issue-table__cell.is-right {
  text-align: right;
  justify-self: end;
}

.admin-issue-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  color: #6b7280;
}

.admin-issue-empty__count {
  font-size: 1.1rem;
  font-weight: 800;
  color: #111827;
}

.admin-issue-empty__desc {
  margin: 0;
  font-size: 0.8rem;
}

.admin-issue-skeleton {
  display: grid;
  gap: 6px;
}

.admin-issue-skeleton__line {
  height: 10px;
  border-radius: 999px;
  background: #e5e7eb;
}

.admin-issue-skeleton__line--short {
  width: 60%;
}

.admin-issue-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #b91c1c;
  font-size: 0.82rem;
}

.admin-issue-error__text {
  margin: 0;
}

.admin-highlight {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.admin-highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  grid-auto-rows: 96px;
}

.admin-highlight-item {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: stretch;
  gap: 8px;
  min-height: 96px;
}

.admin-highlight-label {
  margin: 0;
  color: #6b7280;
  font-weight: 700;
  font-size: 0.88rem;
  line-height: 1.2;
}

.admin-highlight-value {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: #0b3b32;
  white-space: nowrap;
}

.admin-highlight-metric {
  display: flex;
  align-items: baseline;
  gap: 6px;
  justify-content: flex-end;
}

.admin-highlight-unit {
  font-size: 0.85rem;
  font-weight: 700;
  color: #6b7280;
}

.admin-highlight-sub {
  color: #0f766e;
  font-size: 0.78rem;
  font-weight: 700;
  margin: 2px 0 0;
  line-height: 1.2;
  white-space: nowrap;
}

.admin-period-label {
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 700;
  margin-left: 8px;
}

.admin-card__caption {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 700;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .admin-highlight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
