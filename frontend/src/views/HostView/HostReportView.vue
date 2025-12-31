<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { getUserInfo } from '@/api/authClient'
import {
  fetchHostReviewReportSummary,
  fetchHostReviewReportTrend,
  fetchHostThemeReport,
  fetchHostDemandForecast,
  fetchHostReviewAiSummary
} from '@/api/hostReport'
import { formatCurrency, formatDate, formatNumber } from '@/utils/formatters'

const tabs = [
  { id: 'reviews', label: '리뷰 리포트' },
  { id: 'themes', label: '테마 인기' },
  { id: 'forecast', label: '수요 예측' }
]

const activeTab = ref('reviews')
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
const reviewSummary = ref(null)
const reviewTrend = ref([])
const reviewLoading = ref(false)
const reviewError = ref('')
const aiSummary = ref(null)
const aiLoading = ref(false)
const aiError = ref('')
const expandedReviews = ref({})
const aiHasContent = computed(() => {
  if (!aiSummary.value) return false
  return (aiSummary.value.overview || []).length > 0
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
  return [1, 2, 3, 4, 5].map((rating) => ({
    rating,
    count: distribution[rating] ?? 0
  }))
})

const reviewHasData = computed(() => (reviewSummary.value?.reviewCount ?? 0) > 0)
const canGenerateAi = computed(() => reviewHasData.value && !reviewLoading.value && !aiLoading.value)

const themeRows = computed(() => themeReport.value?.rows ?? [])
const forecastDaily = computed(() => forecastReport.value?.forecastDaily ?? [])

const applyReviewPreset = (preset) => {
  reviewPreset.value = preset
  if (preset === '7days') {
    reviewFilters.value.from = daysAgoISO(7)
    reviewFilters.value.to = todayISO()
  } else if (preset === '30days') {
    reviewFilters.value.from = daysAgoISO(30)
    reviewFilters.value.to = todayISO()
  } else if (preset === 'thisMonth') {
    const date = new Date()
    const from = new Date(date.getFullYear(), date.getMonth(), 1)
    reviewFilters.value.from = from.toISOString().slice(0, 10)
    reviewFilters.value.to = todayISO()
  }
}

const applyThemePreset = (preset) => {
  if (preset === '7days') {
    themeFilters.value.from = daysAgoISO(7)
    themeFilters.value.to = todayISO()
  } else if (preset === '30days') {
    themeFilters.value.from = daysAgoISO(30)
    themeFilters.value.to = todayISO()
  } else if (preset === 'thisMonth') {
    const date = new Date()
    const from = new Date(date.getFullYear(), date.getMonth(), 1)
    themeFilters.value.from = from.toISOString().slice(0, 10)
    themeFilters.value.to = todayISO()
  }
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
  if (activeTab.value === 'themes') {
    loadThemeReport()
  }
}, { deep: true })

watch(forecastFilters, () => {
  if (activeTab.value === 'forecast') {
    loadForecast()
  }
}, { deep: true })
</script>

<template>
  <section class="report-view" v-if="authReady && isHostUser">
    <header class="report-header">
      <div>
        <p class="eyebrow">인사이트</p>
        <h2>리포트</h2>
        <p class="sub">호스트 숙소의 리뷰/테마/수요 흐름을 확인하세요.</p>
      </div>
      <div v-if="accommodationLoading" class="muted">숙소 불러오는 중...</div>
      <div v-else-if="accommodationError" class="error-text">{{ accommodationError }}</div>
    </header>

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

      <div class="section-toolbar">
        <p class="toolbar-note">
          {{ reviewHasData ? '선택한 기간의 리뷰 데이터를 바탕으로 요약합니다.' : '리뷰가 있는 기간에서만 요약할 수 있습니다.' }}
        </p>
        <button type="button" class="primary-btn" :disabled="!canGenerateAi" @click="loadAiSummary">
          {{ aiLoading ? '요약 생성 중...' : 'AI 요약 생성' }}
        </button>
      </div>

      <div v-if="reviewLoading" class="loading-box">리뷰 리포트 로딩 중...</div>
      <div v-else-if="reviewError" class="error-box">{{ reviewError }}</div>
      <div v-else>
        <div class="kpi-grid">
          <div class="kpi-card">
            <p>평균 평점</p>
            <strong>{{ reviewSummary?.avgRating ?? 0 }}</strong>
          </div>
          <div class="kpi-card">
            <p>리뷰 수</p>
            <strong>{{ reviewSummary?.reviewCount ?? 0 }}</strong>
          </div>
        </div>

        <div v-if="!reviewHasData" class="empty-box">선택한 기간에 리뷰가 없습니다.</div>

        <div v-else class="grid-two">
          <div class="panel">
            <h3>별점 분포</h3>
            <ul class="rating-list">
              <li v-for="entry in reviewRatingEntries" :key="entry.rating">
                <span>{{ entry.rating }}점</span>
                <span>{{ formatNumber(entry.count) }}건</span>
              </li>
            </ul>
          </div>
          <div class="panel">
            <h3>TOP 태그</h3>
            <div class="tag-list">
              <span v-if="(reviewSummary?.topTags ?? []).length === 0" class="muted">태그 데이터가 없습니다.</span>
              <span v-for="tag in reviewSummary?.topTags" :key="tag.tagName" class="tag-chip">
                {{ tag.tagName }} · {{ tag.count }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="aiError" class="error-box">
          <p>{{ aiError }}</p>
          <button type="button" class="link-btn" @click="loadAiSummary">다시 시도</button>
        </div>
        <div v-else-if="aiSummary && !aiHasContent" class="empty-box">AI 요약 대상 리뷰가 없습니다.</div>
        <div v-else-if="aiSummary" class="panel ai-panel">
          <div class="ai-head">
            <div>
              <p class="ai-kicker">AI 요약</p>
              <h3>리뷰 인사이트</h3>
            </div>
            <span class="muted">{{ aiSummary.generatedAt }}</span>
          </div>
          <div class="ai-grid">
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
                <li v-for="line in aiSummary.negatives" :key="line">{{ line }}</li>
              </ul>
            </div>
            <div class="ai-block">
              <h4>다음 액션</h4>
              <ul>
                <li v-for="line in aiSummary.actions" :key="line">{{ line }}</li>
              </ul>
            </div>
            <div class="ai-block">
              <h4>주의/리스크</h4>
              <ul>
                <li v-for="line in aiSummary.risks" :key="line">{{ line }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="panel">
          <h3>최근 리뷰</h3>
          <div v-if="(reviewSummary?.recentReviews ?? []).length === 0" class="muted">리뷰가 없습니다.</div>
          <ul v-else class="recent-list">
            <li v-for="review in reviewSummary?.recentReviews" :key="review.reviewId">
              <div class="recent-head">
                <strong>{{ review.accommodationName }}</strong>
                <span>{{ formatDate(review.createdAt, true) }}</span>
              </div>
              <p class="recent-meta">{{ review.authorName }} · {{ review.rating }}점</p>
              <p class="recent-content" :class="{ expanded: expandedReviews[review.reviewId] }">
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
            </li>
          </ul>
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
          <button type="button" @click="applyThemePreset('7days')">7일</button>
          <button type="button" @click="applyThemePreset('30days')">30일</button>
          <button type="button" @click="applyThemePreset('thisMonth')">이번달</button>
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

      <div v-if="themeLoading" class="loading-box">테마 리포트 로딩 중...</div>
      <div v-else-if="themeError" class="error-box">{{ themeError }}</div>
      <div v-else>
        <div v-if="themeRows.length === 0" class="empty-box">선택한 기간에 테마 데이터가 없습니다.</div>
        <table v-else class="simple-table table-only">
          <thead>
            <tr>
              <th>테마</th>
              <th>예약수</th>
              <th>매출</th>
              <th>숙소수</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in themeRows" :key="row.themeId">
              <td>{{ row.themeName }}</td>
              <td>{{ formatNumber(row.reservationCount) }}</td>
              <td>{{ formatCurrency(row.revenueSum) }}</td>
              <td>{{ formatNumber(row.accommodationCount) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="themeRows.length" class="card-list mobile-only">
          <div v-for="row in themeRows" :key="row.themeId" class="report-card">
            <div class="report-card__row">
              <strong>{{ row.themeName }}</strong>
              <span>{{ formatNumber(row.accommodationCount) }}개 숙소</span>
            </div>
            <p>예약 {{ formatNumber(row.reservationCount) }} · 매출 {{ formatCurrency(row.revenueSum) }}</p>
          </div>
        </div>
      </div>
    </section>

    <section v-else class="report-section">
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

      <div v-if="forecastLoading" class="loading-box">수요 예측 계산 중...</div>
      <div v-else-if="forecastError" class="error-box">{{ forecastError }}</div>
      <div v-else>
        <div class="kpi-grid">
          <div class="kpi-card">
            <p>최근 7일 평균</p>
            <strong>{{ forecastReport?.baseline?.recentAvg7 ?? 0 }}</strong>
          </div>
          <div class="kpi-card">
            <p>최근 28일 평균</p>
            <strong>{{ forecastReport?.baseline?.recentAvg28 ?? 0 }}</strong>
          </div>
          <div class="kpi-card">
            <p>예측 합계</p>
            <strong>{{ formatNumber(forecastReport?.forecastSummary?.predictedTotal ?? 0) }}</strong>
          </div>
        </div>

        <div v-if="forecastDaily.length === 0" class="empty-box">예측 데이터가 없습니다.</div>
        <table v-else class="simple-table table-only">
          <thead>
            <tr>
              <th>날짜</th>
              <th>예측값</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in forecastDaily" :key="row.date">
              <td>{{ row.date }}</td>
              <td>{{ formatNumber(row.predictedValue) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="forecastDaily.length" class="card-list mobile-only">
          <div v-for="row in forecastDaily" :key="row.date" class="report-card">
            <div class="report-card__row">
              <strong>{{ row.date }}</strong>
              <span>{{ formatNumber(row.predictedValue) }}</span>
            </div>
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

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.eyebrow {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.sub {
  color: var(--text-muted);
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
}

.panel {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem;
  margin-top: 1rem;
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.rating-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
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

.recent-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.recent-head {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  font-weight: 700;
}

.recent-meta {
  color: var(--text-muted);
  font-size: 0.85rem;
}

.recent-content {
  margin: 0.3rem 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.recent-content.expanded {
  -webkit-line-clamp: unset;
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
}

.primary-btn:disabled {
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

.ai-kicker {
  text-transform: uppercase;
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.08em;
  margin: 0;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.ai-block ul {
  padding-left: 1.1rem;
  margin: 0.4rem 0 0;
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

  .table-only {
    display: none;
  }

  .mobile-only {
    display: block;
  }
}

@media (min-width: 1024px) {
  .filters-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
