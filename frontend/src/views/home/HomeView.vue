<script setup>
import { onMounted, ref, computed } from 'vue'
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import SkeletonCard from '../../components/SkeletonCard.vue'
import { useRouter } from 'vue-router'
import { fetchThemes, fetchUserThemes } from '@/api/theme'
import { fetchList, fetchListBulk } from '@/api/list'
import { fetchWishlistIds, addWishlist, removeWishlist } from '@/api/wishlist'
import { isAuthenticated, getUserId } from '@/api/authClient'
import { fetchRecommendations, fetchAiRecommendations } from '@/api/recommendation'
import { useSearchStore } from '@/stores/search'
import EventCarousel from '@/components/EventCarousel.vue'

// AI 추천 관련
const aiSearchQuery = ref('')
const aiRecommendations = ref([])
const aiAnalysis = ref(null)
const isAiLoading = ref(false)
const aiError = ref('')

const handleAiSearch = async () => {
  if (!aiSearchQuery.value.trim()) return
  
  isAiLoading.value = true
  aiError.value = ''
  aiRecommendations.value = []
  aiAnalysis.value = null
  
  try {
    const response = await fetchAiRecommendations(aiSearchQuery.value)
    if (response.ok && response.data) {
      aiAnalysis.value = {
        themes: response.data.matchedThemes || [],
        reasoning: response.data.reasoning || '',
        confidence: response.data.confidence || 0
      }
      aiRecommendations.value = (response.data.accommodations || []).map(acc => ({
        id: acc.accommodationsId,
        title: acc.accommodationsName,
        description: '',
        rating: acc.rating,
        reviewCount: acc.reviewCount,
        location: [acc.city, acc.district].filter(Boolean).join(' '),
        price: acc.minPrice ?? 0,
        imageUrl: acc.thumbnailUrl || 'https://placehold.co/400x300'
      }))
    } else {
      aiError.value = '추천 결과를 불러오지 못했습니다.'
    }
  } catch (e) {
    console.error('AI Recommendation failed:', e)
    aiError.value = 'AI 추천 중 오류가 발생했습니다.'
  } finally {
    isAiLoading.value = false
  }
}

const router = useRouter()
const sections = ref([])
const recommendations = ref([])
const wishlistIds = ref(new Set())
const isLoading = ref(false)
const isLoadingRecommendations = ref(false)
const loadError = ref('')
const searchStore = useSearchStore()
const MAX_ROW_CARDS = 12
const MAX_ITEMS_PER_ROW = MAX_ROW_CARDS - 1
const MAX_THEME_SECTIONS = 8

import { getCardDescription } from '@/utils/accommodationUtils'

const normalizeItem = (item) => {
  const id = item.accommodationsId ?? item.accommodationId ?? item.id
  const title = item.accommodationsName ?? item.accommodationName ?? item.title ?? ''
  const description = getCardDescription(item)
  const rating = item.rating ?? null
  const reviewCount = item.reviewCount ?? item.review_count ?? null
  const location = [item.city, item.district, item.township].filter(Boolean).join(' ')
  const price = Number(item.minPrice ?? item.price ?? 0)
  const imageUrl = item.imageUrl || 'https://placehold.co/400x300'
  const score = item.score ?? null
  const matchedThemes = item.matchedThemes ?? []
  const matchedTags = item.matchedTags ?? []
  return { id, title, description, rating, reviewCount, location, price, imageUrl, score, matchedThemes, matchedTags }
}

const visibleItems = (items) => items.slice(0, MAX_ITEMS_PER_ROW)
const shouldShowMore = (items) => items.length > MAX_ITEMS_PER_ROW
const getMoreLabel = (name) => {
  if (!name) return '더 많은 게스트하우스 보러가기'
  return `${name} 게스트하우스 보러가기`
}
const goToThemeList = (themeId) => {
  router.push({ path: '/list', query: { themeIds: String(themeId) } })
}

const extractListItems = (payload) => {
  if (Array.isArray(payload)) {
    return payload
  }
  if (payload?.recommendedAccommodations || payload?.generalAccommodations) {
    return [
      ...(payload.recommendedAccommodations || []),
      ...(payload.generalAccommodations || [])
    ]
  }
  return payload?.items ?? payload?.content ?? payload?.data ?? []
}

const loadWishlist = async () => {
  // 로그인하지 않은 경우 위시리스트를 로드하지 않음
  if (!isAuthenticated()) {
    return
  }

  try {
    const res = await fetchWishlistIds()
    if (res.status === 200 && Array.isArray(res.data)) {
      wishlistIds.value = new Set(res.data)
    }
  } catch (e) {
    console.error('Failed to load wishlist', e)
  }
}

const toggleWishlist = async (id) => {
  if (!isAuthenticated()) {
    if (confirm('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동하시겠습니까?')) {
      router.push('/login')
    }
    return
  }

  const isAdded = wishlistIds.value.has(id)
  if (isAdded) {
    wishlistIds.value.delete(id)
    try {
      await removeWishlist(id)
    } catch (e) {
      wishlistIds.value.add(id)
      console.error(e)
    }
  } else {
    wishlistIds.value.add(id)
    try {
      await addWishlist(id)
    } catch (e) {
      wishlistIds.value.delete(id)
      console.error(e)
    }
  }
}

const loadSections = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const themeResponse = await fetchThemes()
    if (!themeResponse.ok || !Array.isArray(themeResponse.data)) {
      loadError.value = '테마 목록을 불러오지 못했습니다.'
      return
    }

    const themeList = themeResponse.data
    let preferredThemes = []

    if (isAuthenticated()) {
      try {
        const preferredResponse = await fetchUserThemes()
        if (preferredResponse.ok && Array.isArray(preferredResponse.data)) {
          preferredThemes = preferredResponse.data
        }
      } catch (err) {
        console.warn('Failed to fetch user themes, continuing with all themes:', err)
        // 사용자 선호 테마를 불러오지 못해도 전체 테마 목록은 표시
      }
    }

    const preferredThemeIds = new Set(preferredThemes.map((theme) => theme.id))
    const remainingThemes = themeList
      .filter((theme) => !preferredThemeIds.has(theme.id))
      .sort((a, b) => {
        const countA = Number(a.accommodationCount ?? 0)
        const countB = Number(b.accommodationCount ?? 0)
        if (countA !== countB) return countB - countA
        return (a.id ?? 0) - (b.id ?? 0)
      })
    const orderedThemes = [...preferredThemes, ...remainingThemes].slice(0, MAX_THEME_SECTIONS)

    const themeIds = orderedThemes.map((theme) => theme.id).filter(Boolean)
    let bulkMap = {}
    if (themeIds.length) {
      const bulkResponse = await fetchListBulk(themeIds)
      if (bulkResponse.ok && bulkResponse.data) {
        bulkMap = bulkResponse.data
      }
    }

    const results = await Promise.all(
      orderedThemes.map(async (theme) => {
        const payload = bulkMap?.[theme.id] ?? bulkMap?.[String(theme.id)]
        let list = payload ? extractListItems(payload) : []
        if (!payload) {
          const response = await fetchList([theme.id])
          list = response.ok ? extractListItems(response.data) : []
        }
        return {
          id: theme.id,
          name: theme.themeName,
          items: list.map(normalizeItem)
        }
      })
    )

    sections.value = results.filter((section) => section.items.length)
  } catch (error) {
    console.error('Failed to load sections', error)
    loadError.value = '테마 목록을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

// 맞춤 추천 로딩
const loadRecommendations = async () => {
  if (!isAuthenticated()) return
  
  isLoadingRecommendations.value = true
  try {
    // userId 가져오기 - userInfo에서 먼저 확인, 없으면 API 호출
    let userId = getUserId()
    if (!userId) {
      const { getCurrentUser } = await import('@/api/authClient')
      const userResponse = await getCurrentUser()
      if (userResponse.ok && userResponse.data?.userId) {
        userId = userResponse.data.userId
      }
    }
    if (!userId) return
    
    const response = await fetchRecommendations(userId, 10)
    if (response.ok && response.data?.recommendations) {
      recommendations.value = response.data.recommendations.map(normalizeItem)
    }
  } catch (error) {
    console.error('Failed to load recommendations', error)
  } finally {
    isLoadingRecommendations.value = false
  }
}

onMounted(() => {
  searchStore.setKeyword('')
  searchStore.resetDates()
  searchStore.setGuestCount(1)
  loadSections()
  loadWishlist()
  loadRecommendations()
})
</script>

<template>
  <main class="container main-content">
    <!-- 이벤트 배너 캐러셀 -->
    <EventCarousel />

    <!-- AI 자연어 추천 검색 섹션 -->
    <section class="ai-search-section">
      <div class="ai-search-input-wrapper">
        <input
          v-model="aiSearchQuery"
          type="text"
          class="ai-search-input"
          placeholder="예: 조용한 곳에서 풍경 보면서 힐링하고 싶어"
          @keydown.enter="handleAiSearch"
          :disabled="isAiLoading"
        />
        <button
          class="ai-search-button"
          @click="handleAiSearch"
          :disabled="isAiLoading || !aiSearchQuery.trim()"
        >
          <span v-if="isAiLoading" class="loading-spinner"></span>
          <span v-else class="ai-search-button-label">✨ 추천받기</span>
        </button>
      </div>
      
      <!-- AI 분석 결과 -->
      <div v-if="aiAnalysis" class="ai-result-info">
        <div class="ai-themes">
          <span class="ai-theme-label">분석된 테마:</span>
          <span v-for="theme in aiAnalysis.themes" :key="theme" class="ai-theme-tag">{{ theme }}</span>
        </div>
        <p v-if="aiAnalysis.reasoning" class="ai-reasoning">💡 {{ aiAnalysis.reasoning }}</p>
      </div>
      
      <!-- AI 추천 결과 -->
      <div v-if="aiRecommendations.length > 0" class="ai-recommendations">
        <div class="row-scroll">
          <GuesthouseCard 
            v-for="item in aiRecommendations" 
            :key="item.id"
            :id="item.id"
            :title="item.title"
            :description="item.description"
            :rating="item.rating"
            :review-count="item.reviewCount"
            :location="item.location"
            :price="item.price"
            :image-url="item.imageUrl"
            :is-favorite="wishlistIds.has(item.id)"
            @toggle-favorite="toggleWishlist"
            @click="router.push(`/room/${item.id}`)"
            class="row-card"
          />
        </div>
      </div>
      
      <!-- 에러 메시지 -->
      <div v-if="aiError" class="ai-error">{{ aiError }}</div>
    </section>

    <!-- 맞춤 추천 섹션 (로그인 사용자만) -->
    <section v-if="recommendations.length > 0" class="theme-section recommendation-section">
      <div class="section-header">
        <h2 class="section-title recommendation-title">
          <span>✨ 당신을 위한 맞춤 추천</span>
        </h2>
      </div>
      <div class="row-scroll">
        <GuesthouseCard 
          v-for="item in recommendations" 
          :key="item.id"
          :id="item.id"
          :title="item.title"
          :description="item.description"
          :rating="item.rating"
          :review-count="item.reviewCount"
          :location="item.location"
          :price="item.price"
          :image-url="item.imageUrl"
          :is-favorite="wishlistIds.has(item.id)"
          @toggle-favorite="toggleWishlist"
          @click="router.push(`/room/${item.id}`)"
          class="row-card"
        />
      </div>
    </section>

    <!-- 스켈레톤 로딩 UI -->
    <section v-if="isLoading" class="theme-section skeleton-section">
      <div class="section-header">
        <div class="skeleton-section-title shimmer"></div>
      </div>
      <div class="row-scroll">
        <SkeletonCard v-for="n in 5" :key="n" />
      </div>
    </section>
    <section v-if="isLoading" class="theme-section skeleton-section">
      <div class="section-header">
        <div class="skeleton-section-title shimmer"></div>
      </div>
      <div class="row-scroll">
        <SkeletonCard v-for="n in 5" :key="n" />
      </div>
    </section>

    <div v-else-if="loadError" class="empty-state">{{ loadError }}</div>
    <section v-else v-for="section in sections" :key="section.id" class="theme-section">
      <div class="section-header">
        <h2
          class="section-title theme-title"
          role="button"
          tabindex="0"
          @click="goToThemeList(section.id)"
          @keydown.enter.prevent="goToThemeList(section.id)"
          @keydown.space.prevent="goToThemeList(section.id)"
        >
          <span>{{ getMoreLabel(section.name) }}</span>
          <span class="theme-title-arrow">&#8594;</span>
        </h2>
      </div>
      <div v-if="section.items.length" class="row-scroll">
        <GuesthouseCard 
          v-for="item in visibleItems(section.items)" 
          :key="item.id"
          :id="item.id"
          :title="item.title"
          :description="item.description"
          :rating="item.rating"
          :review-count="item.reviewCount"
          :location="item.location"
          :price="item.price"
          :image-url="item.imageUrl"
          :is-favorite="wishlistIds.has(item.id)"
          @toggle-favorite="toggleWishlist"
          @click="router.push(`/room/${item.id}`)"
          class="row-card"
        />
        <div
          v-if="shouldShowMore(section.items)"
          class="row-card more-card"
        >
          <button
            type="button"
            class="more-text"
            @click="goToThemeList(section.id)"
          >
            {{ getMoreLabel(section.name) }}
          </button>
          <button
            type="button"
            class="more-arrow"
            :aria-label="getMoreLabel(section.name)"
            @click="goToThemeList(section.id)"
          >&#8594;</button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.main-content {
  padding-top: 2rem;
  padding-bottom: 4rem;
}

/* PC 웹에서 header와 동일한 max-width 적용 */
@media (min-width: 769px) {
  .main-content {
    max-width: 1400px;
    margin: 0 auto;
    padding-left: 24px;
    padding-right: 24px;
  }
}

.bannner {
  margin-bottom: 2rem;
  padding: 0;
  position: relative;
}

.banner-image {
  display: block;
  width: 100%;
  height: 130px;
  object-fit: cover;
  border-radius: 16px;
}

/* PC 웹에서만 배너 높이 제한 - 이미지 잘림 방지 */
@media (min-width: 768px) {
  .bannner {
    background-color: #1a1a1a;
    border-radius: 16px;
    overflow: hidden;
  }
  
  .banner-image {
    height: 480px;
    width: 100%;
    object-fit: contain;
    background-color: #1a1a1a;
  }
}

.banner-text {
  position: absolute;
  left: 1.25rem;
  bottom: 1.25rem;
  z-index: 1;
  max-width: calc(100% - 2.5rem);
  color: #ffffff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
}

.banner-caption {
  margin: 0.75rem 0 0;
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.3;
  color: inherit;
}

.banner-title {
  margin: 0.3rem 0 0;
  font-size: 1.8rem;
  font-weight: 800;
  line-height: 1.2;
  color: inherit;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 2rem;
}

.theme-section {
  margin-bottom: 2.5rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.section-title {
  font-size: 1.4rem;
  font-weight: 800;
  margin: 0;
}

.theme-title {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.15rem 0.4rem;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.theme-title:hover {
  background-color: #e5e7eb;
}

.theme-title:focus-visible,
.theme-title:active {
  background-color: #e5e7eb;
}

@media (hover: none) {
  .theme-title:active {
    background-color: #e5e7eb;
  }
}

.theme-title-arrow {
  font-size: 1.1rem;
  line-height: 1;
  transform: translateY(1px);
}

.row-scroll {
  display: flex;
  gap: 1.25rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
  scroll-snap-type: x mandatory;
}

.row-card {
  flex: 0 0 var(--card-width, 280px);
  scroll-snap-align: start;
  cursor: pointer;
}

/* 모바일에서 카드 크기 축소 */
@media (max-width: 768px) {
  .row-scroll {
    gap: 0.75rem;
  }
  
  .row-card {
    flex: 0 0 230px;
  }
}

.more-card {
  border: 1px dashed #cbd5f5;
  background: #f8fafc;
  color: #1f2937;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 1.5rem;
  border-radius: 16px;
  transition: background-color 0.2s, transform 0.2s;
}

.more-card:hover,
.more-card:focus-within {
  background: #eef2f7;
  transform: translateY(-2px);
}

.more-text {
  background: transparent;
  border: none;
  padding: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: inherit;
  text-align: left;
  cursor: pointer;
  flex: 1;
  min-width: 0;
  white-space: normal;
  word-break: keep-all;
  line-height: 1.3;
}

.more-arrow {
  width: 44px;
  height: 44px;
  border-radius: 999px;
  border: 1px solid #cbd5f5;
  background: white;
  color: #111827;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s, border-color 0.2s, transform 0.2s;
}

.more-arrow:hover {
  background: #e2e8f0;
  border-color: #94a3b8;
  transform: translateX(2px);
}

.empty-state {
  padding: 1.5rem 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

/* 맞춤 추천 섹션 스타일 */
.recommendation-section {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  padding: 1.5rem;
  border-radius: 16px;
  margin-bottom: 1rem;
}

.recommendation-title {
  color: #7c3aed;
  font-size: 1.25rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* 스켈레톤 로딩 UI 스타일 */
.skeleton-section {
  margin-bottom: 2.5rem;
}

.skeleton-section-title {
  height: 1.4rem;
  width: 200px;
  background: #e5e7eb;
  border-radius: 4px;
}

/* 쉬머 애니메이션 효과 */
.shimmer {
  position: relative;
  overflow: hidden;
}

.shimmer::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 100%
  );
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* AI 검색 섹션 스타일 */
.ai-search-section {
  padding: 0.5rem 0;
  margin-bottom: 0.5rem;
}

.ai-search-header {
  text-align: center;
  margin-bottom: 1rem;
}

.ai-search-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0369a1;
  margin: 0 0 0.5rem;
}

.ai-search-subtitle {
  font-size: 0.95rem;
  color: #0c4a6e;
  margin: 0;
}

.ai-search-input-wrapper {
  display: flex;
  gap: 0.75rem;
  max-width: 800px;
  margin: 0 auto;
  align-items: center;
}

.ai-search-input {
  flex: 1;
  padding: calc(1rem + 2px) 1.25rem calc(1rem - 2px);
  font-size: 1rem;
  border: 2px solid var(--brand-primary, #BFE7DF);
  border-radius: 50px;
  background: white;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.ai-search-input:focus {
  border-color: var(--brand-primary-strong, #6DC3BB);
  box-shadow: 0 0 0 3px rgba(109, 195, 187, 0.2);
}

.ai-search-input::placeholder {
  color: #94a3b8;
}

.ai-search-button {
  padding: 0.85rem 1.2rem 0.85rem 0.9rem;
  min-height: 48px;
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f766e;
  background: linear-gradient(135deg, var(--brand-primary, #BFE7DF) 0%, #7fd3ca 100%);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 999px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, background 0.2s;
  white-space: nowrap;
  box-shadow: 0 6px 14px rgba(109, 195, 187, 0.25);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ai-search-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #9fe0d7 0%, var(--brand-primary-strong, #6DC3BB) 100%);
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(109, 195, 187, 0.35);
}

.ai-search-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 4px 10px rgba(109, 195, 187, 0.25);
}

.ai-search-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.ai-search-button-label {
  display: inline-block;
  transform: translateY(-1px);
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.ai-result-info {
  max-width: 800px;
  margin: 1rem auto 0;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
}

.ai-themes {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.ai-theme-label {
  font-weight: 600;
  color: #0369a1;
}

.ai-theme-tag {
  padding: 0.25rem 0.75rem;
  background: #0ea5e9;
  color: white;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
}

.ai-reasoning {
  margin: 0;
  color: #475569;
  font-size: 0.9rem;
}

.ai-recommendations {
  margin-top: 1.5rem;
}

.ai-error {
  max-width: 800px;
  margin: 1rem auto 0;
  padding: 1rem;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 8px;
  text-align: center;
}
</style>

