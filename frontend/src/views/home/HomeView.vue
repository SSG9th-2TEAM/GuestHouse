<script setup>
import { onMounted, ref, computed } from 'vue'
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import { useRouter } from 'vue-router'
import { fetchThemes, fetchUserThemes } from '@/api/theme'
import { fetchList, fetchListBulk } from '@/api/list'
import { fetchWishlistIds, addWishlist, removeWishlist } from '@/api/wishlist'
import { isAuthenticated, getUserId } from '@/api/authClient'
import { fetchRecommendations } from '@/api/recommendation'
import { useSearchStore } from '@/stores/search'

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

const normalizeItem = (item) => {
  const id = item.accomodationsId ?? item.accommodationsId ?? item.accommodationId ?? item.id
  const title = item.accomodationsName ?? item.accommodationsName ?? item.accommodationName ?? item.title ?? ''
  const description = item.shortDescription ?? item.description ?? ''
  const rating = item.rating ?? null
  const location = [item.city, item.district, item.township].filter(Boolean).join(' ')
  const price = Number(item.minPrice ?? item.price ?? 0)
  const imageUrl = item.imageUrl || 'https://placehold.co/400x300'
  const score = item.score ?? null
  const matchedThemes = item.matchedThemes ?? []
  const matchedTags = item.matchedTags ?? []
  return { id, title, description, rating, location, price, imageUrl, score, matchedThemes, matchedTags }
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
    <div class="bannner">
      <img src="@/assets/home-banner.png" alt="Home banner" class="banner-image" />
      <div class="banner-text">
        <h2 class="banner-caption">좋은 사람, 좋은 장소, 좋은 시간</h2>
        <h1 class="banner-title">지금, 이곳</h1>
      </div>
    </div>

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

    <div v-if="isLoading" class="empty-state">로딩 중입니다.</div>
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

.bannner {
  margin-bottom: 2rem;
  padding: 0;
  position: relative;
}

.banner-image {
  display: block;
  width: 100%;
  height: auto;
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
  min-height: 360px;
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
</style>
