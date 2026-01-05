<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { getUserInfo } from '@/api/authClient'
import {
  fetchHostReviewReportSummary,
  fetchHostReviewReportTrend,
  fetchHostThemeReport,
  fetchHostDemandForecast,
  fetchHostReviewAiSummary
} from '@/api/hostReport'
import { formatCurrency, formatDate } from '@/utils/formatters'

const tabs = [
  { id: 'reviews', label: '리뷰 리포트' },
  { id: 'themes', label: '테마 인기' },
  { id: 'forecast', label: '수요 예측' }
]

const activeTab = ref('reviews')
const router = useRouter()
const userInfo = ref(getUserInfo())
const authReady = ref(false)
const isHostUser = computed(() => {
  if (!userInfo.value) return false
  return userInfo.value.role === 'HOST' || userInfo.value.role === 'ROLE_HOST'
})
const accommodations = ref([])
const accommodationLoading = ref(false)
const accommodationError = ref('')

const todayISO = () => new Date().toISOString().slice(0, 10)
const daysAgoISO = (days) => {
  const date = new Date()
  date.setDate(date.getDate() - days)
  return date.toISOString().slice(0, 10)
}

const defaultFrom = daysAgoISO(30)
const defaultTo = todayISO()

const reviewFilters = ref({
  accommodationId: 'all',
  from: defaultFrom,
  to: defaultTo
})
const reviewPreset = ref('30days')
const themePreset = ref('30days')
const reviewSummary = ref(null)
const reviewTrend = ref([])
const reviewLoading = ref(false)
const reviewError = ref('')
const aiSummary = ref(null)
const aiLoading = ref(false)
const aiError = ref('')
const expandedReviews = ref({})
const formatNumber = (value) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return '0'
  return new Intl.NumberFormat('ko-KR').format(numberValue)
}

const formatKrw = (value) => `${formatNumber(value)}원`

const formatGeneratedAt = (value) => {
  if (!value) return '-'
  const normalized = String(value)
    .replace(/\[[^\]]+]/g, '')
    .replace(/(\.\d{3})\d+/g, '$1')
    .trim()
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return '-'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const aiHasContent = computed(() => {
  if (!aiSummary.value) return false
  const fields = ['overview', 'positives', 'negatives', 'actions', 'risks']
  return fields.some((key) => Array.isArray(aiSummary.value?.[key]) && aiSummary.value[key].length > 0)
})

const themeFilters = ref({
  accommodationId: 'all',
  from: defaultFrom,
  to: defaultTo,
  metric: 'reservations'
})
const themeLoading = ref(false)
const themeError = ref('')
const themeReport = ref(null)

const forecastFilters = ref({
  accommodationId: 'all',
  target: 'reservations',
  horizonDays: 30,
  historyDays: 180
})
const forecastLoading = ref(false)
const forecastError = ref('')
const forecastReport = ref(null)

const reviewRatingEntries = computed(() => {
  const distribution = reviewSummary.value?.ratingDistribution || {}
  const total = (reviewSummary.value?.reviewCount ?? 0) || 0
  return [5, 4, 3, 2, 1].map((rating) => {
    const count = distribution[rating] ?? 0
    const percent = total > 0 ? Math.round((count / total) * 100) : 0
    return { rating, count, percent }
  })
})

const reviewHasData = computed(() => (reviewSummary.value?.reviewCount ?? 0) > 0)
const canGenerateAi = computed(() => reviewHasData.value && !reviewLoading.value && !aiLoading.value)
const aiMetaChips = computed(() => {
  const chips = []
  if (reviewFilters.value.from && reviewFilters.value.to) {
    chips.push(`기간 ${reviewFilters.value.from} ~ ${reviewFilters.value.to}`)
  }
  const count = formatNumber(reviewSummary.value?.reviewCount ?? 0)
  chips.push(`리뷰 ${count}건 기준`)
  if (aiSummary.value?.generatedAt) {
    chips.push(`생성 ${formatGeneratedAt(aiSummary.value.generatedAt)}`)
  }
  return chips
})

const aiNegativesDisplay = computed(() => {
  const negatives = Array.isArray(aiSummary.value?.negatives) ? [...aiSummary.value.negatives] : []
  const ratingDist = reviewSummary.value?.ratingDistribution ?? {}
  const lowRatingCount = [1, 2, 3].reduce((sum, rating) => sum + Number(ratingDist?.[rating] ?? 0), 0)
  const fallbacks = []
  if (lowRatingCount > 0) {
    fallbacks.push('낮은 평점(3점 이하) 리뷰가 있습니다. 원인 태그를 점검하세요.')
  } else {
    fallbacks.push('부정 신호가 적습니다(유지관리 권장).')
  }
  fallbacks.push('반복 키워드/불만 흐름을 주간으로 모니터링하세요.')
  fallbacks.push('청소·소음·응대 체크리스트를 정기 점검하세요.')
  fallbacks.push('체크인 안내/응대 속도에 대한 피드백을 점검하세요.')

  fallbacks.forEach((item) => {
    if (negatives.length < 3 && !negatives.includes(item)) {
      negatives.push(item)
    }
  })
  return negatives.slice(0, 3)
})

const themeRows = computed(() => themeReport.value?.rows ?? [])
const themeTotals = computed(() => {
  return themeRows.value.reduce((acc, row) => {
    acc.reservations += Number(row.reservationCount ?? 0)
    acc.revenue += Number(row.revenueSum ?? 0)
    return acc
  }, { reservations: 0, revenue: 0 })
})
const themeTopRow = computed(() => {
  if (themeRows.value.length === 0) return null
  const key = themeFilters.value.metric === 'revenue' ? 'revenueSum' : 'reservationCount'
  return themeRows.value.reduce((best, row) => {
    const value = Number(row[key] ?? 0)
    if (!best) return row
    return value > Number(best[key] ?? 0) ? row : best
  }, null)
})
const themeKpis = computed(() => ([
  {
    label: '기간 예약수',
    value: `${formatNumber(themeTotals.value.reservations)}건`
  },
  {
    label: '기간 매출',
    value: `${formatNumber(themeTotals.value.revenue)}원`
  },
  {
    label: 'Top 테마',
    value: themeTopRow.value?.themeName ?? '데이터 없음'
  }
]))
const themeViewMode = ref('cards')
const isZeroValue = (value) => Number(value ?? 0) === 0
const forecastDaily = computed(() => forecastReport.value?.forecastDaily ?? [])
const topTagLabel = computed(() => {
  const tags = reviewSummary.value?.topTags ?? []
  return tags.length > 0 ? `${tags[0].tagName}` : '데이터 없음'
})
const formatForecastValue = (value) => {
  if (forecastFilters.value.target === 'revenue') {
    return formatKrw(value)
  }
  return `${formatNumber(value)}건`
}

const forecastValueLabel = computed(() => (
  forecastFilters.value.target === 'revenue' ? '예측 매출(원)' : '예측 예약(건)'
))
const forecastRangeLabel = computed(() => (
  forecastFilters.value.target === 'revenue' ? '예측 범위(원)' : '예측 범위(건)'
))
const forecastMetaText = computed(() => {
  const explain = forecastReport.value?.explain
  const mape = forecastReport.value?.diagnostics?.mape
  const parts = []
  if (explain) parts.push(explain)
  if (Number.isFinite(mape)) parts.push(`백테스트 MAPE ${formatNumber(mape)}%`)
  return parts.join(' · ')
})
const formatForecastRange = (row) => {
  if (!row || (!row.low && !row.high)) return '-'
  if (Number(row.low) === 0 && Number(row.high) === 0) return '-'
  const low = formatNumber(row.low ?? 0)
  const high = formatNumber(row.high ?? 0)
  return `${low} ~ ${high}`
}

const contextSummaryText = computed(() => {
  const reviewCount = formatNumber(reviewSummary.value?.reviewCount ?? 0)
  const avgRating = reviewSummary.value?.avgRating ?? 0
  const tag = topTagLabel.value && topTagLabel.value !== '데이터 없음' ? ` · 대표태그 ${topTagLabel.value}` : ''
  return `기간 ${reviewFilters.value.from} ~ ${reviewFilters.value.to} · 리뷰 ${reviewCount} · 평점 ${avgRating}${tag}`
})

const getPresetRange = (preset) => {
  if (preset === '7days') {
    return { from: daysAgoISO(7), to: todayISO() }
  }
  if (preset === '30days') {
    return { from: daysAgoISO(30), to: todayISO() }
  }
  const date = new Date()
  const from = new Date(date.getFullYear(), date.getMonth(), 1)
  return { from: from.toISOString().slice(0, 10), to: todayISO() }
}

const applyPreset = (filtersRef, preset) => {
  const range = getPresetRange(preset)
  filtersRef.value = { ...filtersRef.value, ...range }
}

const applyReviewPreset = (preset) => {
  reviewPreset.value = preset
  applyPreset(reviewFilters, preset)
}

const applyThemePreset = (preset) => {
  themePreset.value = preset
  applyPreset(themeFilters, preset)
}

const loadAccommodations = async () => {
  accommodationLoading.value = true
  accommodationError.value = ''
  const response = await fetchHostAccommodations()
  if (response.ok) {
    const payload = response.data
    accommodations.value = Array.isArray(payload) ? payload : payload?.items ?? payload?.content ?? payload?.data ?? []
  } else {
    accommodationError.value = '숙소 목록을 불러오지 못했습니다.'
  }
  accommodationLoading.value = false
}

const loadReviewReport = async () => {
  reviewLoading.value = true
  reviewError.value = ''
  const params = {
    from: reviewFilters.value.from,
    to: reviewFilters.value.to
  }
  if (reviewFilters.value.accommodationId !== 'all') {
    params.accommodationId = reviewFilters.value.accommodationId
  }
  const [summaryRes, trendRes] = await Promise.all([
    fetchHostReviewReportSummary(params),
    fetchHostReviewReportTrend({
      accommodationId: params.accommodationId,
      months: 6
    })
  ])

  if (summaryRes.ok) {
    reviewSummary.value = summaryRes.data
  } else {
    reviewError.value = '리뷰 리포트를 불러오지 못했습니다.'
  }

  if (trendRes.ok) {
    reviewTrend.value = Array.isArray(trendRes.data) ? trendRes.data : []
  } else {
    reviewTrend.value = []
  }
  reviewLoading.value = false
}

const toggleReviewContent = (reviewId) => {
  expandedReviews.value[reviewId] = !expandedReviews.value[reviewId]
}

const loadAiSummary = async () => {
  if (aiLoading.value) return
  if (!reviewHasData.value) {
    aiError.value = '리뷰가 없는 기간입니다. 기간을 바꿔 다시 시도해주세요.'
    aiSummary.value = null
    return
  }
  aiLoading.value = true
  aiError.value = ''
  const payload = {
    from: reviewFilters.value.from,
    to: reviewFilters.value.to
  }
  if (reviewFilters.value.accommodationId !== 'all') {
    payload.accommodationId = reviewFilters.value.accommodationId
  }
  const response = await fetchHostReviewAiSummary(payload)
  if (response.ok) {
    aiSummary.value = response.data
  } else {
    aiError.value = 'AI 요약을 불러오지 못했습니다.'
    aiSummary.value = null
  }
  aiLoading.value = false
}

const loadThemeReport = async () => {
  themeLoading.value = true
  themeError.value = ''
  const params = {
    from: themeFilters.value.from,
    to: themeFilters.value.to,
    metric: themeFilters.value.metric
  }
  if (themeFilters.value.accommodationId !== 'all') {
    params.accommodationId = themeFilters.value.accommodationId
  }
  const response = await fetchHostThemeReport(params)
  if (response.ok) {
    themeReport.value = response.data
  } else {
    themeError.value = '테마 리포트를 불러오지 못했습니다.'
  }
  themeLoading.value = false
}

const loadForecast = async () => {
  forecastLoading.value = true
  forecastError.value = ''
  const params = {
    target: forecastFilters.value.target,
    horizonDays: forecastFilters.value.horizonDays,
    historyDays: forecastFilters.value.historyDays
  }
  if (forecastFilters.value.accommodationId !== 'all') {
    params.accommodationId = forecastFilters.value.accommodationId
  }
  const response = await fetchHostDemandForecast(params)
  if (response.ok) {
    forecastReport.value = response.data
  } else {
    forecastError.value = '수요 예측을 불러오지 못했습니다.'
  }
  forecastLoading.value = false
}

onMounted(async () => {
  if (!userInfo.value) {
    router.replace('/login')
    return
  }
  if (!isHostUser.value) {
    router.replace('/host')
    return
  }
  authReady.value = true
  await loadAccommodations()
  loadReviewReport()
  loadThemeReport()
  loadForecast()
})

watch(reviewFilters, () => {
  if (activeTab.value === 'reviews') {
    loadReviewReport()
    aiSummary.value = null
    aiError.value = ''
  }
}, { deep: true })

watch(themeFilters, () => {
  loadThemeReport()
}, { deep: true })

watch(forecastFilters, () => {
  if (activeTab.value === 'forecast') {
    loadForecast()
  }
}, { deep: true })
</script>

<template>
  <section class="report-view" v-if="authReady && isHostUser">
    <header class="view-header">
      <div>
        <h2>리포트</h2>
        <p class="subtitle">리뷰/테마/수요 흐름을 한눈에 확인하세요.</p>
      </div>
      <p v-if="accommodationLoading" class="subtitle">숙소 불러오는 중...</p>
      <p v-else-if="accommodationError" class="error-text">{{ accommodationError }}</p>
    </header>

    <div class="context-bar">
      <p class="context-text">{{ contextSummaryText }}</p>
      <div class="context-chips">
        <span v-if="topTagLabel !== '데이터 없음'" class="tag-chip">대표 태그: {{ topTagLabel }}</span>
        <span class="tag-chip">기간: {{ reviewFilters.from }} ~ {{ reviewFilters.to }}</span>
      </div>
    </div>

    <div class="report-tabs" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="report-tab"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <section v-if="activeTab === 'reviews'" class="report-section">
      <div class="filter-card">
        <div class="filter-title">필터</div>
        <div class="filters filters-grid">
          <label>
            숙소
            <select v-model="reviewFilters.accommodationId">
              <option value="all">전체 숙소</option>
              <option v-for="acc in accommodations" :key="acc.accommodationsId" :value="acc.accommodationsId">
                {{ acc.accommodationsName }}
              </option>
            </select>
          </label>
          <div class="filter-group">
            <button type="button" :class="{ active: reviewPreset === '7days' }" @click="applyReviewPreset('7days')">7일</button>
            <button type="button" :class="{ active: reviewPreset === '30days' }" @click="applyReviewPreset('30days')">30일</button>
            <button type="button" :class="{ active: reviewPreset === 'thisMonth' }" @click="applyReviewPreset('thisMonth')">이번달</button>
          </div>
          <label>
            시작일
            <input type="date" v-model="reviewFilters.from" />
          </label>
          <label>
            종료일
            <input type="date" v-model="reviewFilters.to" />
          </label>
        </div>
      </div>

      <div v-if="reviewLoading" class="loading-box">리뷰 리포트 로딩 중...</div>
      <div v-else-if="reviewError" class="error-box">{{ reviewError }}</div>
      <div v-else>
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-icon">★</div>
            <div>
              <p>평균 평점</p>
              <strong>{{ reviewSummary?.avgRating ?? 0 }}</strong>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon">✉</div>
            <div>
              <p>리뷰 수</p>
              <strong>{{ reviewSummary?.reviewCount ?? 0 }}</strong>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon">#</div>
            <div>
              <p>대표 태그</p>
              <strong>{{ topTagLabel }}</strong>
            </div>
          </div>
        </div>

        <div v-if="!reviewHasData" class="empty-box">선택한 기간에 리뷰가 없습니다.</div>

        <div v-else class="grid-two">
          <div class="panel">
            <h3>별점 분포</h3>
            <ul class="rating-bars">
              <li v-for="entry in reviewRatingEntries" :key="entry.rating">
                <span class="rating-label">{{ entry.rating }}점</span>
                <div class="rating-bar">
                  <span class="rating-fill" :style="{ width: entry.percent + '%' }"></span>
                </div>
                <span class="rating-count">{{ formatNumber(entry.count) }}건</span>
                <span class="rating-percent">{{ entry.percent }}%</span>
              </li>
            </ul>
          </div>
          <div class="panel">
            <h3>TOP 태그</h3>
            <p class="muted">자주 언급된 키워드를 모았어요.</p>
            <div class="tag-list">
              <span v-if="(reviewSummary?.topTags ?? []).length === 0" class="muted">태그 데이터가 없습니다.</span>
              <span v-for="tag in reviewSummary?.topTags" :key="tag.tagName" class="tag-chip">
                {{ tag.tagName }} · {{ tag.count }}
              </span>
            </div>
          </div>
        </div>

        <div class="panel ai-panel">
          <div class="ai-head">
            <div>
              <p class="ai-kicker">AI 리뷰 인사이트</p>
              <h3>요약 리포트</h3>
              <p class="muted">선택 기간 리뷰를 기반으로 핵심 포인트를 정리합니다.</p>
            </div>
            <div class="ai-meta">
              <button
                type="button"
                class="primary-btn ai-btn"
                :disabled="!canGenerateAi || aiLoading"
                :aria-busy="aiLoading ? 'true' : 'false'"
                @click="loadAiSummary"
              >
                <span v-if="aiLoading" class="spinner" aria-hidden="true"></span>
                {{ aiLoading ? '생성 중...' : aiHasContent ? '재생성' : 'AI 요약 생성' }}
              </button>
              <div class="ai-meta-chips">
                <span v-for="chip in aiMetaChips" :key="chip" class="ai-chip">{{ chip }}</span>
              </div>
            </div>
          </div>
          <div v-if="aiError" class="error-box ai-state">
            <p>{{ aiError }}</p>
            <button type="button" class="link-btn" @click="loadAiSummary">다시 시도</button>
          </div>
          <div v-else-if="aiSummary && !aiHasContent" class="empty-box ai-state">AI 요약 대상 리뷰가 없습니다.</div>
          <div v-else-if="aiSummary" class="ai-grid">
            <div class="ai-block">
              <h4>총평</h4>
              <ul>
                <li v-for="line in aiSummary.overview" :key="line">{{ line }}</li>
              </ul>
            </div>
            <div class="ai-block">
              <h4>좋았던 점</h4>
              <ul>
                <li v-for="line in aiSummary.positives" :key="line">{{ line }}</li>
              </ul>
            </div>
            <div class="ai-block">
              <h4>개선 포인트</h4>
              <ul>
                <li v-for="line in aiNegativesDisplay" :key="line">{{ line }}</li>
              </ul>
            </div>
            <div class="ai-block">
              <h4>다음 액션</h4>
              <ul>
                <li v-for="line in aiSummary.actions" :key="line">{{ line }}</li>
              </ul>
            </div>
            <div class="ai-block ai-wide">
              <h4>주의/리스크</h4>
              <ul>
                <li v-for="line in aiSummary.risks" :key="line">{{ line }}</li>
              </ul>
            </div>
          </div>
          <div v-else class="muted ai-state">AI 요약을 생성해보세요.</div>
        </div>

        <div class="panel">
          <h3>최근 리뷰</h3>
          <div v-if="(reviewSummary?.recentReviews ?? []).length === 0" class="muted">리뷰가 없습니다.</div>
          <div v-else class="review-cards">
            <article v-for="review in reviewSummary?.recentReviews" :key="review.reviewId" class="review-card">
              <div class="review-head">
                <div>
                  <strong>{{ review.accommodationName }}</strong>
                  <p class="review-meta">{{ review.authorName }} · {{ review.rating }}점</p>
                </div>
                <span class="review-date">{{ formatDate(review.createdAt, true) }}</span>
              </div>
              <p class="review-content" :class="{ expanded: expandedReviews[review.reviewId] }">
                {{ review.content }}
              </p>
              <button
                v-if="review.content && review.content.length > 120"
                type="button"
                class="link-btn"
                @click="toggleReviewContent(review.reviewId)"
              >
                {{ expandedReviews[review.reviewId] ? '접기' : '더보기' }}
              </button>
            </article>
          </div>
        </div>

        <div class="panel" v-if="reviewTrend.length">
          <h3>리뷰 추이</h3>
          <table class="simple-table table-only">
            <thead>
              <tr><th>월</th><th>리뷰수</th><th>평균 평점</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in reviewTrend" :key="row.period">
                <td>{{ row.period }}</td>
                <td>{{ formatNumber(row.reviewCount) }}</td>
                <td>{{ row.avgRating }}</td>
              </tr>
            </tbody>
          </table>
          <div class="card-list mobile-only">
            <div v-for="row in reviewTrend" :key="row.period" class="report-card">
              <div class="report-card__row">
                <strong>{{ row.period }}</strong>
                <span>{{ formatNumber(row.reviewCount) }}건</span>
              </div>
              <p class="muted">평균 평점 {{ row.avgRating }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'themes'" class="report-section">
      <div class="theme-section-wrap">
        <div class="theme-layout">
          <aside class="filter-card theme-filter">
            <div class="filter-title">필터</div>
            <div class="filters filters-grid">
              <label>
                숙소
                <select v-model="themeFilters.accommodationId">
                  <option value="all">전체 숙소</option>
                  <option v-for="acc in accommodations" :key="acc.accommodationsId" :value="acc.accommodationsId">
                    {{ acc.accommodationsName }}
                  </option>
                </select>
              </label>
              <div class="filter-group">
                <button type="button" :class="{ active: themePreset === '7days' }" @click="applyThemePreset('7days')">7일</button>
                <button type="button" :class="{ active: themePreset === '30days' }" @click="applyThemePreset('30days')">30일</button>
                <button type="button" :class="{ active: themePreset === 'thisMonth' }" @click="applyThemePreset('thisMonth')">이번달</button>
              </div>
              <label>
                시작일
                <input type="date" v-model="themeFilters.from" />
              </label>
              <label>
                종료일
                <input type="date" v-model="themeFilters.to" />
              </label>
              <label>
                지표
                <select v-model="themeFilters.metric">
                  <option value="reservations">예약수</option>
                  <option value="revenue">매출</option>
                </select>
              </label>
            </div>
          </aside>

          <div class="theme-content">
            <div class="theme-kpi-grid">
              <div v-for="item in themeKpis" :key="item.label" class="kpi-card">
                <div>
                  <p>{{ item.label }}</p>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </div>

            <div class="theme-results-card">
              <div class="theme-results-head">
                <div>
                  <h4>테마 인기 결과</h4>
                  <p class="muted">선택 기간 기준으로 테마별 성과를 보여줍니다.</p>
                </div>
                <div class="theme-view-toggle" role="tablist" aria-label="보기 전환">
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="themeViewMode === 'cards'"
                    :class="{ active: themeViewMode === 'cards' }"
                    @click="themeViewMode = 'cards'"
                  >카드</button>
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="themeViewMode === 'table'"
                    :class="{ active: themeViewMode === 'table' }"
                    @click="themeViewMode = 'table'"
                  >테이블</button>
                </div>
              </div>

              <div class="theme-results-body">
                <div v-if="themeLoading" class="loading-box">테마 리포트 로딩 중...</div>
                <div v-else-if="themeError" class="error-box">{{ themeError }}</div>
                <div v-else>
                  <div v-if="themeRows.length === 0" class="empty-box">선택한 기간에 테마 데이터가 없습니다.</div>

                  <div v-else>
                    <div v-if="themeViewMode === 'cards'" class="theme-grid-desktop">
                      <article v-for="row in themeRows" :key="row.themeId" class="theme-card">
                        <div class="theme-card__head">
                          <strong>{{ row.themeName }}</strong>
                        </div>
                        <div class="theme-card__metrics">
                          <div class="theme-metric">
                            <span>예약</span>
                            <strong>{{ formatNumber(row.reservationCount) }}건</strong>
                          </div>
                          <div class="theme-metric">
                            <span>매출</span>
                            <strong>{{ formatNumber(row.revenueSum) }}원</strong>
                          </div>
                          <div class="theme-metric">
                            <span>숙소</span>
                            <strong>{{ formatNumber(row.accommodationCount) }}개</strong>
                          </div>
                        </div>
                      </article>
                    </div>

                    <div v-else class="theme-table-wrapper">
                      <table class="simple-table theme-table">
                        <thead>
                          <tr>
                            <th>테마</th>
                            <th class="cell-right">예약(건)</th>
                            <th class="cell-right">매출(원)</th>
                            <th class="cell-right">숙소(개)</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="row in themeRows" :key="row.themeId">
                            <td>{{ row.themeName }}</td>
                            <td class="cell-right">
                              <span :class="{ 'zero-value': isZeroValue(row.reservationCount) }">
                                {{ formatNumber(row.reservationCount) }}
                              </span>
                            </td>
                            <td class="cell-right">
                              <span :class="{ 'zero-value': isZeroValue(row.revenueSum) }">
                                {{ formatNumber(row.revenueSum) }}
                              </span>
                            </td>
                            <td class="cell-right">
                              <span :class="{ 'zero-value': isZeroValue(row.accommodationCount) }">
                                {{ formatNumber(row.accommodationCount) }}
                              </span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div v-if="themeRows.length" class="theme-grid theme-grid-stack">
                    <article v-for="row in themeRows" :key="row.themeId" class="theme-card">
                      <div class="theme-card__head">
                        <strong>{{ row.themeName }}</strong>
                      </div>
                      <div class="theme-card__metrics">
                        <div class="theme-metric">
                          <span>예약</span>
                          <strong>{{ formatNumber(row.reservationCount) }}건</strong>
                        </div>
                        <div class="theme-metric">
                          <span>매출</span>
                          <strong>{{ formatNumber(row.revenueSum) }}원</strong>
                        </div>
                        <div class="theme-metric">
                          <span>숙소</span>
                          <strong>{{ formatNumber(row.accommodationCount) }}개</strong>
                        </div>
                      </div>
                    </article>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-else class="report-section">
      <div class="filter-card">
        <div class="filter-title">필터</div>
        <div class="filters filters-grid">
          <label>
            숙소
            <select v-model="forecastFilters.accommodationId">
              <option value="all">전체 숙소</option>
              <option v-for="acc in accommodations" :key="acc.accommodationsId" :value="acc.accommodationsId">
                {{ acc.accommodationsName }}
              </option>
            </select>
          </label>
          <label>
            대상
            <select v-model="forecastFilters.target">
              <option value="reservations">예약수</option>
              <option value="revenue">매출</option>
            </select>
          </label>
          <label>
            예측 기간(일)
            <input type="number" min="7" max="60" v-model.number="forecastFilters.horizonDays" />
          </label>
          <label>
            과거 데이터(일)
            <input type="number" min="30" max="365" v-model.number="forecastFilters.historyDays" />
          </label>
        </div>
      </div>

      <div v-if="forecastLoading" class="loading-box">수요 예측 계산 중...</div>
      <div v-else-if="forecastError" class="error-box">{{ forecastError }}</div>
      <div v-else>
        <div class="kpi-grid">
          <div class="kpi-card">
            <p>최근 7일 평균{{ forecastFilters.target === 'revenue' ? '(원/일)' : '(건/일)' }}</p>
            <strong>{{ formatForecastValue(forecastReport?.baseline?.recentAvg7 ?? 0) }}</strong>
          </div>
          <div class="kpi-card">
            <p>최근 28일 평균{{ forecastFilters.target === 'revenue' ? '(원/일)' : '(건/일)' }}</p>
            <strong>{{ formatForecastValue(forecastReport?.baseline?.recentAvg28 ?? 0) }}</strong>
          </div>
          <div class="kpi-card">
            <p>예측 합계</p>
            <strong>{{ formatForecastValue(forecastReport?.forecastSummary?.predictedTotal ?? 0) }}</strong>
          </div>
        </div>
        <p v-if="forecastMetaText" class="forecast-meta">{{ forecastMetaText }}</p>

        <div v-if="forecastDaily.length === 0" class="empty-box">예측 데이터가 없습니다.</div>
        <table v-else class="simple-table table-only forecast-table">
          <thead>
            <tr>
              <th>날짜</th>
              <th>요일</th>
              <th class="cell-right">{{ forecastValueLabel }}</th>
              <th class="cell-right">{{ forecastRangeLabel }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in forecastDaily"
              :key="row.date"
              :class="[{ 'forecast-weekend': row.isWeekend }, { 'forecast-holiday': row.isHoliday }]"
            >
              <td>{{ row.date }}</td>
              <td>{{ row.dowLabel }}</td>
              <td class="cell-right">{{ formatForecastValue(row.predictedValue) }}</td>
              <td class="cell-right">{{ formatForecastRange(row) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="forecastDaily.length" class="card-list mobile-only">
          <div v-for="row in forecastDaily" :key="row.date" class="report-card">
            <div class="report-card__row">
              <strong>{{ row.date }}</strong>
              <span>{{ formatForecastValue(row.predictedValue) }}</span>
            </div>
            <p class="muted forecast-card-meta">
              {{ row.dowLabel }}{{ row.isHoliday ? ' · 공휴일' : row.isWeekend ? ' · 주말' : '' }}
            </p>
            <p class="muted forecast-card-range">예측 범위 {{ formatForecastRange(row) }}</p>
          </div>
        </div>
      </div>
    </section>
  </section>
  <section v-else class="report-view">
    <div class="loading-box">권한을 확인하는 중입니다...</div>
  </section>
</template>

<style scoped>
.report-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding-bottom: 2rem;
}

.error-text {
  color: var(--danger);
  font-size: 0.85rem;
}

.view-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--brand-accent);
  margin: 0.15rem 0 0.2rem;
  letter-spacing: -0.01em;
}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0;
}

.context-bar {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 1rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.context-text {
  margin: 0;
  color: var(--text-muted);
  width: 100%;
}

.context-chips {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.context-chips .tag-chip {
  max-width: 220px;
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.3;
}

.report-tabs {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
  scrollbar-width: none;
}

.report-tabs::-webkit-scrollbar {
  display: none;
}

.report-tab {
  border: 1px solid var(--brand-border);
  background: var(--bg-white);
  border-radius: 999px;
  padding: 0.4rem 0.9rem;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}

.report-tab.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.filter-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  padding: 1rem;
}

.filter-title {
  font-weight: 700;
  margin-bottom: 0.8rem;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  align-items: flex-end;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.filters label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.filters select,
.filters input {
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--brand-border);
  border-radius: 0.5rem;
  background: var(--bg-white);
  color: var(--text-default);
}

.filter-group {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.filter-group button {
  border: 1px solid var(--brand-border);
  background: var(--bg-white);
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.filter-group button.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.kpi-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.kpi-icon {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 999px;
  background: var(--brand-primary);
  color: var(--brand-accent);
  display: grid;
  place-items: center;
  font-weight: 700;
}

.panel {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem;
  margin-top: 1rem;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.theme-section-wrap {
  max-width: 1240px;
  margin: 0 auto;
  width: 100%;
}

.theme-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.theme-results-card {
  border: 1px solid var(--brand-border);
  border-radius: 1rem;
  background: var(--bg-white);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-results-head {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.theme-results-head h4 {
  margin: 0;
  font-size: 1.05rem;
}

.theme-results-head p {
  margin: 0;
}

.theme-results-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-view-toggle {
  display: none;
  align-items: center;
  gap: 0.2rem;
  padding: 0.2rem;
  border-radius: 999px;
  border: 1px solid var(--brand-border);
  background: #f8fafc;
}

.theme-view-toggle button {
  border: none;
  background: transparent;
  border-radius: 999px;
  padding: 0.3rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  color: var(--text-muted);
}

.theme-view-toggle button.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.theme-grid-desktop {
  display: none;
  gap: 1rem;
}

.theme-grid-stack {
  display: grid;
}

.theme-table-wrapper {
  display: none;
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  overflow: hidden;
  background: var(--bg-white);
}

.theme-table thead th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  z-index: 1;
}

.theme-table tbody tr:hover {
  background: #f8fafc;
}

.zero-value {
  color: var(--text-muted);
}

.theme-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.theme-card__head {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-weight: 700;
}

.theme-card__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.theme-metric {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.theme-metric strong {
  font-size: 1rem;
  color: var(--text-default);
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.rating-bars {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.rating-bars li {
  display: grid;
  grid-template-columns: 3rem 1fr auto auto;
  gap: 0.5rem;
  align-items: center;
}

.rating-bar {
  height: 0.5rem;
  background: var(--brand-border);
  border-radius: 999px;
  overflow: hidden;
}

.rating-fill {
  display: block;
  height: 100%;
  background: var(--brand-accent);
}

.rating-label,
.rating-count,
.rating-percent {
  font-size: 0.85rem;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag-chip {
  background: var(--brand-primary);
  color: var(--brand-accent);
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-size: 0.8rem;
}

.review-cards {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.review-card {
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 0.9rem;
  background: #fafafa;
}

.review-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.review-meta {
  color: var(--text-muted);
  font-size: 0.85rem;
  margin: 0.25rem 0 0;
}

.review-content {
  margin: 0.3rem 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.review-content.expanded {
  -webkit-line-clamp: unset;
}

.review-date {
  font-size: 0.8rem;
  color: var(--text-muted);
  white-space: nowrap;
}

.link-btn {
  margin-top: 0.35rem;
  border: none;
  background: none;
  color: var(--brand-accent);
  font-weight: 600;
  cursor: pointer;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 0.8rem;
  font-size: 0.9rem;
}

.simple-table th,
.simple-table td {
  padding: 0.55rem 0.6rem;
  border-bottom: 1px solid var(--brand-border);
  text-align: left;
}

.simple-table tr.forecast-weekend td {
  background: #f8fafc;
}

.simple-table tr.forecast-holiday td {
  background: #fff7ed;
}

.forecast-table thead th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  z-index: 1;
}

.forecast-table tbody tr:not(.forecast-holiday):hover td {
  background: #f8fafc;
}

.theme-table tbody tr {
  transition: background 0.2s ease;
}

.theme-table tbody tr:hover {
  background: #f8fafc;
}

.cell-right {
  text-align: right;
}

.loading-box,
.error-box,
.empty-box {
  padding: 1rem;
  border-radius: 0.8rem;
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  margin-top: 1rem;
}

.error-box {
  color: var(--danger);
}

.muted {
  color: var(--text-muted);
}

.forecast-meta {
  margin: 0.6rem 0 0;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.forecast-card-meta,
.forecast-card-range {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.toolbar-note {
  margin: 0;
  color: var(--text-muted);
}

.primary-btn {
  border: none;
  background: var(--brand-accent);
  color: #fff;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary-btn:not(:disabled):hover {
  background: var(--brand-primary-strong);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.2);
  transform: translateY(-1px);
}

.primary-btn:not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
}

.ai-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 40px;
}

.spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
}

.ghost-btn {
  border: 1px solid var(--brand-border);
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.ghost-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-panel h3 {
  margin: 0.2rem 0 0;
}

.ai-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.ai-meta {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: flex-end;
  justify-content: flex-end;
  text-align: right;
}

.ai-meta-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  justify-content: flex-end;
}

.ai-chip {
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.75rem;
  color: var(--text-muted);
  background: #f8fafc;
  border: 1px solid var(--brand-border);
}

.ai-kicker {
  text-transform: uppercase;
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.08em;
  margin: 0;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0.85rem;
  align-items: stretch;
  grid-auto-rows: 1fr;
}

.ai-wide {
  grid-column: 1 / -1;
}

.ai-block {
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  padding: 0.9rem 1rem;
  background: #fafafa;
  max-width: 46ch;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.ai-block h4 {
  margin: 0;
}

.ai-block ul {
  padding-left: 1.1rem;
  margin: 0.35rem 0 0;
  line-height: 1.6;
  display: grid;
  gap: 0.35rem;
  word-break: keep-all;
  overflow-wrap: anywhere;
  flex: 1;
}


.ai-state {
  margin-top: 0;
}

.table-only {
  display: table;
}

.mobile-only {
  display: none;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.report-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 0.8rem;
}

.report-card__row {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-weight: 700;
}

@media (max-width: 768px) {
  .filters-grid {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .rating-bars li {
    grid-template-columns: 2.5rem 1fr auto;
  }

  .rating-percent {
    display: none;
  }

  .table-only {
    display: none;
  }

  .mobile-only {
    display: block;
  }

  .ai-grid {
    grid-template-columns: 1fr;
  }

  .ai-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .ai-meta {
    align-items: flex-start;
    text-align: left;
  }

  .ai-meta-chips {
    justify-content: flex-start;
  }
}

@media (max-width: 480px) {
  .context-bar {
    flex-direction: column;
    align-items: flex-start;
    padding: 0.75rem;
  }

  .context-text {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .context-chips {
    margin-top: 0;
    position: static;
    width: 100%;
  }
}

@media (min-width: 1024px) {
  .filters-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .theme-grid {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }

  .ai-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .theme-layout {
    flex-direction: row;
    align-items: flex-start;
  }

  .theme-filter {
    flex: 0 0 300px;
    position: sticky;
    top: 96px;
  }

  .theme-content {
    flex: 1;
  }

  .theme-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .theme-view-toggle {
    display: flex;
  }

  .theme-results-head {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .theme-grid-desktop {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .theme-table-wrapper {
    display: block;
  }

  .theme-grid-stack {
    display: none;
  }
}

@media (min-width: 1280px) {
  .theme-grid-desktop {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1440px) {
  .theme-grid-desktop {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
