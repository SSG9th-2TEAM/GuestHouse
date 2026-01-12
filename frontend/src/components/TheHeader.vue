<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSearchStore } from '@/stores/search'
import { useHolidayStore } from '@/stores/holiday'
import { useCalendarStore } from '@/stores/calendar'
import { fetchSearchSuggestions, resolveSearchAccommodation } from '@/api/list'
import { isAuthenticated, logout, validateToken, getUserInfo, getAccessToken, getCurrentUser, saveUserInfo } from '@/api/authClient'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const holidayStore = useHolidayStore()
const calendarStore = useCalendarStore()
const searchKeyword = ref(searchStore.keyword || '')
const suggestKeyword = ref(searchKeyword.value || '')
const keywordDisplay = computed(() => {
  const keyword = String(searchStore.keyword ?? '').trim()
  return keyword || '어디로 갈까?'
})

const suggestions = ref([])
const isSuggestOpen = ref(false)
const isSuggestLoading = ref(false)
const hasSuggestFetched = ref(false)
const isComposing = ref(false)
const MIN_SUGGEST_LENGTH = 2
const SUGGEST_LIMIT = 10
let suggestTimer = null
let suggestRequestId = 0

const normalizeSuggestKeyword = (value) => {
  const trimmed = String(value ?? '').trim()
  return trimmed.length >= MIN_SUGGEST_LENGTH ? trimmed : ''
}

const canShowSuggestions = computed(() => {
  return Boolean(normalizeSuggestKeyword(suggestKeyword.value) && isSuggestOpen.value)
})

const clearSuggestionTimer = () => {
  if (suggestTimer) {
    clearTimeout(suggestTimer)
    suggestTimer = null
  }
}

const resetSuggestions = () => {
  clearSuggestionTimer()
  suggestions.value = []
  isSuggestLoading.value = false
  hasSuggestFetched.value = false
}

const scheduleSuggestionFetch = (value) => {
  const keyword = normalizeSuggestKeyword(value)
  if (!isSuggestOpen.value || !keyword) {
    resetSuggestions()
    return
  }
  clearSuggestionTimer()
  hasSuggestFetched.value = false
  suggestTimer = setTimeout(async () => {
    const requestId = ++suggestRequestId
    isSuggestLoading.value = true
    try {
      const response = await fetchSearchSuggestions(keyword, SUGGEST_LIMIT)
      if (requestId !== suggestRequestId) return
      if (response.ok && Array.isArray(response.data)) {
        suggestions.value = response.data
      } else {
        suggestions.value = []
      }
    } catch (error) {
      console.error('Failed to load search suggestions', error)
      if (requestId === suggestRequestId) {
        suggestions.value = []
      }
    } finally {
      if (requestId === suggestRequestId) {
        isSuggestLoading.value = false
        hasSuggestFetched.value = true
      }
    }
  }, 250)
}

const openSuggestions = () => {
  isSuggestOpen.value = true
  scheduleSuggestionFetch(suggestKeyword.value || searchKeyword.value)
}

const closeSuggestions = () => {
  isSuggestOpen.value = false
  resetSuggestions()
}

const resolveAccommodation = async (value) => {
  const keyword = String(value ?? '').trim()
  if (!keyword) return null
  try {
    const response = await resolveSearchAccommodation(keyword)
    if (response.ok && response.data?.accommodationsId) {
      return response.data
    }
  } catch (error) {
    console.error('Failed to resolve accommodation', error)
  }
  return null
}

const isAccommodationSuggestion = (suggestion) => {
  return String(suggestion?.type || '').toUpperCase() === 'ACCOMMODATION'
}

const selectSuggestion = async (suggestion) => {
  if (!suggestion?.value) return
  const nextValue = String(suggestion.value)
  searchKeyword.value = nextValue
  suggestKeyword.value = nextValue
  searchStore.setKeyword(nextValue)
  if (isAccommodationSuggestion(suggestion)) {
    const resolved = await resolveAccommodation(nextValue)
    if (resolved?.accommodationsId) {
      closeSuggestions()
      isSearchExpanded.value = false
      router.push({ path: `/room/${resolved.accommodationsId}` })
      return
    }
  }
  closeSuggestions()
}

const handleCompositionStart = () => {
  isComposing.value = true
}

const handleCompositionUpdate = (event) => {
  const value = event?.target?.value ?? ''
  suggestKeyword.value = value
  scheduleSuggestionFetch(value)
}

const handleCompositionEnd = (event) => {
  isComposing.value = false
  const value = event?.target?.value ?? searchKeyword.value
  suggestKeyword.value = value
  scheduleSuggestionFetch(value)
}

const handleInput = (event) => {
  const value = event?.target?.value ?? ''
  suggestKeyword.value = value
  if (!isSuggestOpen.value) return
  if (event?.isComposing || isComposing.value) {
    scheduleSuggestionFetch(value)
  }
}

const getSuggestionLabel = (type) => {
  const normalized = String(type || '').toUpperCase()
  return normalized === 'REGION' ? '지역' : '숙소'
}

watch(
  () => searchStore.keyword,
  (value) => {
    const next = value || ''
    if (next !== searchKeyword.value) {
      searchKeyword.value = next
    }
  }
)

watch(
  () => searchKeyword.value,
  (value) => {
    if (isComposing.value) return
    suggestKeyword.value = value || ''
    if (!isSuggestOpen.value) return
    scheduleSuggestionFetch(value)
  }
)

const isMenuOpen = ref(false)
const isSearchExpanded = ref(false)
const isCalendarOpen = computed(() => calendarStore.activeCalendar === 'header')
const isGuestOpen = ref(false)
const isLoggedIn = ref(isAuthenticated())
const isHostRoute = computed(() => route.path.startsWith('/host'))
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const userInfo = computed(() => getUserInfo())
const isUserHost = computed(() => {
  return (
    userInfo.value?.role === 'HOST' ||
    userInfo.value?.role === 'ROLE_HOST' ||
    userInfo.value?.hostApproved === true
  )
})

// 호스트 모드 전환
const toggleHostMode = async () => {
  if (!isLoggedIn.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath || '/host' } });
    isMenuOpen.value = false;
    return;
  }

  let hostAllowed = isUserHost.value
  if (!hostAllowed && getAccessToken()) {
    const response = await getCurrentUser()
    if (response.ok && response.data) {
      saveUserInfo(response.data)
      hostAllowed = response.data.role === 'HOST' ||
        response.data.role === 'ROLE_HOST' ||
        response.data.hostApproved === true
    }
  }

  if (hostAllowed) {
    // 실제 호스트인 경우, 호스트/게스트 뷰 토글
    if (isHostRoute.value) {
      router.push('/');
    } else {
      router.push('/host');
    }
  } else {
    router.push('/host')
  }
  isMenuOpen.value = false;
}

// 로그인 페이지로 이동
const handleLogin = () => {
  router.push('/login')
  isMenuOpen.value = false
}

// 로그아웃
const handleLogout = () => {
  logout()
  isLoggedIn.value = false
  isMenuOpen.value = false
  // 페이지 새로고침으로 상태 완전 초기화
  window.location.href = '/'
}

// Calendar state - from store
const currentDate = ref(new Date())

const isMobile = () => window.innerWidth <= 768

const toggleGuestPicker = (e) => {
  e.stopPropagation()
  isGuestOpen.value = !isGuestOpen.value
  calendarStore.closeCalendar('header')
  closeSuggestions()
}

const increaseGuest = () => {
  searchStore.increaseGuest()
}

const decreaseGuest = () => {
  searchStore.decreaseGuest()
}

// Calendar computed properties
const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())

// Next month computed properties
const nextMonthDate = computed(() => new Date(currentYear.value, currentMonth.value + 1, 1))
const nextMonthYear = computed(() => nextMonthDate.value.getFullYear())
const nextMonthMonth = computed(() => nextMonthDate.value.getMonth())

const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
const weekDays = ['일', '월', '화', '수', '목', '금', '토']

const toDateKey = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getHolidayInfo = (date) => holidayStore.getHolidayInfo(toDateKey(date))

// Function to generate calendar days for any month
const getCalendarDays = (year, month) => {
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()
  const startingDay = firstDay.getDay()
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  const days = []
  
  // Empty cells for days before the first day of the month
  for (let i = 0; i < startingDay; i++) {
    days.push({ day: '', isEmpty: true })
  }
  
  // Days of the month
  for (let day = 1; day <= daysInMonth; day++) {
    const date = new Date(year, month, day)
    const dayOfWeek = date.getDay()
    const isStartDate = searchStore.startDate && isSameDay(date, searchStore.startDate)
    const isEndDate = searchStore.endDate && isSameDay(date, searchStore.endDate)
    const isInRange = isDateInRange(date)
    const hasEndDate = searchStore.endDate !== null
    const holidayInfo = getHolidayInfo(date)
    const isDisabled = date.getTime() < today.getTime()
    
    days.push({
      day,
      isEmpty: false,
      date,
      isToday: isSameDay(date, new Date()),
      isSaturday: dayOfWeek === 6,
      isSunday: dayOfWeek === 0,
      isHoliday: Boolean(holidayInfo),
      holidayName: holidayInfo?.name || '',
      isStartDate,
      isEndDate,
      isInRange,
      hasEndDate,
      isDisabled
    })
  }
  
  return days
}

// Current month calendar days
const calendarDays = computed(() => getCalendarDays(currentYear.value, currentMonth.value))

// Next month calendar days
const nextMonthDays = computed(() => getCalendarDays(nextMonthYear.value, nextMonthMonth.value))

watch(
  [currentYear, currentMonth],
  () => {
    holidayStore.loadMonth(currentYear.value, currentMonth.value + 1)
    holidayStore.loadMonth(nextMonthYear.value, nextMonthMonth.value + 1)
  },
  { immediate: true }
)

const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear() &&
    date1.getMonth() === date2.getMonth() &&
    date1.getDate() === date2.getDate()
}

const isDateInRange = (date) => {
  if (!searchStore.startDate || !searchStore.endDate) return false
  const time = date.getTime()
  return time > searchStore.startDate.getTime() && time < searchStore.endDate.getTime()
}

const toggleCalendar = (e) => {
  e.stopPropagation()
  calendarStore.toggleCalendar('header')
  isGuestOpen.value = false
  closeSuggestions()
}

const prevMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value - 1, 1)
}

const nextMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value + 1, 1)
}

const selectDate = (dayObj) => {
  if (dayObj.isEmpty || dayObj.isDisabled) return
  
  const clickedDate = dayObj.date
  
  // If no start date or both dates are set, start fresh
  if (!searchStore.startDate || (searchStore.startDate && searchStore.endDate)) {
    searchStore.setStartDate(clickedDate)
    searchStore.setEndDate(null)
  } else {
    // Set end date - must be AFTER start date (not same day)
    if (clickedDate.getTime() <= searchStore.startDate.getTime()) {
      // If clicked date is same or before start date, set as new start date
      searchStore.setStartDate(clickedDate)
      searchStore.setEndDate(null)
    } else {
      searchStore.setEndDate(clickedDate)
    }
  }
}

const formatDateParam = (date) => {
  if (!(date instanceof Date)) return null
  if (Number.isNaN(date.getTime())) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const isMapContext = () => {
  if (route.path === '/map') return true
  const from = route.query.from
  if (Array.isArray(from)) return from.includes('map')
  return from === 'map'
}

const buildSearchQuery = () => {
  const query = {}
  const keyword = String(searchKeyword.value ?? '').trim()
  searchStore.setKeyword(keyword)
  searchKeyword.value = keyword
  if (keyword) query.keyword = keyword
  if (searchStore.guestCount > 0) query.guestCount = String(searchStore.guestCount)
  const checkin = formatDateParam(searchStore.startDate)
  const checkout = formatDateParam(searchStore.endDate)
  if (checkin && checkout) {
    query.checkin = checkin
    query.checkout = checkout
  }

  if (route.path === '/list' || isMapContext()) {
    if (searchStore.minPrice !== null) query.min = String(searchStore.minPrice)
    if (searchStore.maxPrice !== null) query.max = String(searchStore.maxPrice)
    if (searchStore.themeIds.length) query.themeIds = searchStore.themeIds.join(',')
  }

  return query
}

const handleSearch = async () => {
  const keyword = String(searchKeyword.value ?? '').trim()
  if (keyword) {
    const resolved = await resolveAccommodation(keyword)
    if (resolved?.accommodationsId) {
      searchStore.setKeyword(keyword)
      searchKeyword.value = keyword
      isSearchExpanded.value = false
      closeSuggestions()
      router.push({ path: `/room/${resolved.accommodationsId}` })
      return
    }
  }

  const targetPath = isMapContext() ? '/map' : '/list'
  router.push({ path: targetPath, query: buildSearchQuery() })
  isSearchExpanded.value = false
  closeSuggestions()
}

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const toggleSearch = (event) => {
  if (!isMobile()) return
  if (event?.target?.closest?.('.search-btn-mini')) {
    handleSearch()
    return
  }
  isSearchExpanded.value = !isSearchExpanded.value
  if (!isSearchExpanded.value) {
    closeSuggestions()
  }
}

const handleClickOutside = (e) => {
  if (!e.target.closest('.right-menu')) {
    isMenuOpen.value = false
  }
  if (!e.target.closest('.date-picker-wrapper')) {
    calendarStore.closeCalendar('header')
  }
  if (!e.target.closest('.guest-picker-wrapper')) {
    isGuestOpen.value = false
  }
  if (!e.target.closest('.search-keyword-wrapper')) {
    closeSuggestions()
  }
}

const handleResize = () => {
  if (!isMobile()) {
    isSearchExpanded.value = false
    closeSuggestions()
  }
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)

  // 페이지 로드 시 토큰 유효성 검증 (admin 라우트는 제외)
  if (isAuthenticated() && !isAdminRoute.value) {
    const isValid = await validateToken()
    isLoggedIn.value = isValid
  } else {
    isLoggedIn.value = !isAdminRoute.value && isAuthenticated()
  }

  // 페이지 이동 후 로그인 상태 업데이트
  router.afterEach(() => {
    isLoggedIn.value = isAuthenticated()
    if (isMobile()) {
      isSearchExpanded.value = false
      closeSuggestions()
    }
  })
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
  clearSuggestionTimer()
})
</script>

<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-content">
        <div class="header-top">
          <!-- Logo linked to home -->
          <router-link to="/">
            <img src="@/assets/logo.png" alt="Logo" class="logo" />
          </router-link>

          <div class="right-menu">
            <button class="hamburger-btn" @click.stop="toggleMenu" aria-label="메뉴">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
              </svg>
            </button>

            <!-- Enhanced Mobile Menu (Dropdown) -->
            <div class="mobile-menu-dropdown" :class="{ active: isMenuOpen }" @click.stop>
              <div class="menu-header">
                <span class="menu-title">메뉴</span>
                <button class="close-btn" @click="isMenuOpen = false">×</button>
              </div>

              <div class="host-toggle-card" @click="toggleHostMode">
                <div class="toggle-info">
                  <div class="toggle-title">{{ isHostRoute ? '게스트 모드로 전환' : '호스트 모드로 전환' }}</div>
                  <div class="toggle-status">현재: {{ !isLoggedIn ? '비회원' : (isHostRoute ? '호스트 모드' : '게스트 모드') }}</div>
                </div>
                <div class="toggle-icon">›</div>
              </div>

              <ul class="menu-list">
                <li><router-link to="/profile" @click="isMenuOpen = false">프로필 정보</router-link></li>
                <li><router-link to="/reservations" @click="isMenuOpen = false">예약 내역</router-link></li>
                <li><router-link to="/reviews" @click="isMenuOpen = false">리뷰 내역</router-link></li>
                <li><router-link to="/wishlist" @click="isMenuOpen = false">위시리스트</router-link></li>
                <li><router-link to="/coupons" @click="isMenuOpen = false">쿠폰</router-link></li>
                <li><router-link to="/events" @click="isMenuOpen = false">이벤트</router-link></li>
              </ul>

              <div class="menu-footer">
                <button v-if="isLoggedIn" class="logout-btn" @click="handleLogout">로그아웃</button>
                <button v-else class="login-btn" @click="handleLogin">로그인</button>
              </div>
            </div>

          </div>
        </div>

        <div
          v-if="!isHostRoute && !isAdminRoute"
          class="search-bar"
          :class="{ expanded: isSearchExpanded }"
        >
          <!-- Collapsed Mobile View - Click to expand -->
          <div class="search-bar-collapsed" @click="toggleSearch" v-if="!isSearchExpanded">
            <span class="collapsed-text collapsed-text--keyword">{{ keywordDisplay }}</span>
            <span class="collapsed-divider">|</span>
            <span class="collapsed-text">{{ searchStore.dateDisplayText }}</span>
            <span class="collapsed-divider">|</span>
            <span class="collapsed-text">{{ searchStore.guestDisplayText }}</span>
            <button class="search-btn-mini" type="button" aria-label="검색">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
              </svg>
            </button>
          </div>

          <!-- Expanded Mobile View -->
          <div class="search-bar-expanded" v-if="isSearchExpanded" @click.stop>
            <div class="expanded-close" @click="isSearchExpanded = false">×</div>
            <div class="search-item-full search-keyword-wrapper">
              <label>여행지</label>
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="어디로 갈까?"
                @input="handleInput"
                @keydown.enter.prevent="handleSearch"
                @focus="openSuggestions"
                @compositionstart="handleCompositionStart"
                @compositionupdate="handleCompositionUpdate"
                @compositionend="handleCompositionEnd"
              >
              <div v-if="canShowSuggestions" class="search-suggestions">
                <div v-if="isSuggestLoading" class="suggestion-loading">검색 중...</div>
                <template v-else>
                  <button
                    v-for="(suggestion, idx) in suggestions"
                    :key="`${suggestion.type}-${suggestion.value}-${idx}`"
                    type="button"
                    :class="[
                      'search-suggestion',
                      String(suggestion.type || '').toUpperCase() === 'REGION'
                        ? 'search-suggestion--region'
                        : 'search-suggestion--accommodation'
                    ]"
                    @mousedown.prevent="selectSuggestion(suggestion)"
                  >
                    <span class="suggestion-text">{{ suggestion.value }}</span>
                    <span
                      class="suggestion-tag"
                      :class="String(suggestion.type || '').toUpperCase() === 'REGION' ? 'suggestion-tag--region' : 'suggestion-tag--accommodation'"
                    >
                      {{ getSuggestionLabel(suggestion.type) }}
                    </span>
                  </button>
                    <div v-if="hasSuggestFetched && !suggestions.length" class="suggestion-empty">검색 결과 없음</div>
                </template>
              </div>
            </div>

            <div class="search-item-full" @click="toggleCalendar">
              <label>날짜</label>
              <input type="text" :placeholder="searchStore.dateDisplayText" readonly>
            </div>

            <!-- Mobile Calendar -->
            <div class="mobile-calendar-popup" v-if="isCalendarOpen">
              <div class="mobile-calendar-header">
                <button class="calendar-nav-btn" @click="prevMonth">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                </button>
                <span class="calendar-month-title">{{ currentYear }}년 {{ monthNames[currentMonth] }}</span>
                <button class="calendar-nav-btn" @click="nextMonth">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </button>
              </div>
              
              <div class="calendar-weekdays">
                <span
                  v-for="(day, index) in weekDays"
                  :key="'mobile-current-' + day"
                  class="weekday"
                  :class="{ sunday: index === 0, saturday: index === 6 }"
                >{{ day }}</span>
              </div>
              <div class="calendar-days">
                <span
                  v-for="(dayObj, index) in calendarDays"
                  :key="'mobile-current-day-' + index"
                  class="calendar-day"
                  :class="{
                    'empty': dayObj.isEmpty,
                    'today': dayObj.isToday,
                    'weekend-sat': dayObj.isSaturday,
                    'weekend-sun': dayObj.isSunday,
                    'range-start': dayObj.isStartDate,
                    'range-end': dayObj.isEndDate,
                    'in-range': dayObj.isInRange,
                    'has-end': dayObj.isStartDate && dayObj.hasEndDate,
                    'disabled': dayObj.isDisabled,
                    'sunday': dayObj.isSunday,
                    'saturday': dayObj.isSaturday,
                    'holiday': dayObj.isHoliday
                  }"
                  :title="dayObj.holidayName || null"
                  @click.stop="selectDate(dayObj)"
                >
                  {{ dayObj.day }}
                </span>
              </div>
            </div>

            <div class="search-item-full" @click="toggleGuestPicker">
              <label>여행자</label>
              <input type="text" :placeholder="searchStore.guestDisplayText" readonly>
            </div>

            <!-- Mobile Guest Picker -->
            <div class="mobile-guest-popup" v-if="isGuestOpen">
              <div class="guest-header">인원수를 입력하세요</div>
              <div class="guest-row">
                <div class="guest-info">
                  <span class="guest-type">게스트</span>
                </div>
                <div class="guest-controls">
                  <button class="guest-btn" @click.stop="decreaseGuest" :disabled="searchStore.guestCount <= 1">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"></path>
                    </svg>
                  </button>
                  <span class="guest-count">{{ searchStore.guestCount }}</span>
                  <button class="guest-btn" @click.stop="increaseGuest">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <button class="search-btn-full" @click="handleSearch">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="18" height="18">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
              </svg>
            </button>
          </div>

          <!-- Desktop View -->
          <div class="search-bar-desktop">
            <div class="search-item search-keyword-wrapper">
              <label>여행지</label>
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="어디로 갈까?"
                @input="handleInput"
                @keydown.enter.prevent="handleSearch"
                @focus="openSuggestions"
                @compositionstart="handleCompositionStart"
                @compositionupdate="handleCompositionUpdate"
                @compositionend="handleCompositionEnd"
              >
              <div v-if="canShowSuggestions" class="search-suggestions">
                <div v-if="isSuggestLoading" class="suggestion-loading">검색 중...</div>
                <template v-else>
                  <button
                    v-for="(suggestion, idx) in suggestions"
                    :key="`${suggestion.type}-${suggestion.value}-${idx}`"
                    type="button"
                    :class="[
                      'search-suggestion',
                      String(suggestion.type || '').toUpperCase() === 'REGION'
                        ? 'search-suggestion--region'
                        : 'search-suggestion--accommodation'
                    ]"
                    @mousedown.prevent="selectSuggestion(suggestion)"
                  >
                    <span class="suggestion-text">{{ suggestion.value }}</span>
                    <span
                      class="suggestion-tag"
                      :class="String(suggestion.type || '').toUpperCase() === 'REGION' ? 'suggestion-tag--region' : 'suggestion-tag--accommodation'"
                    >
                      {{ getSuggestionLabel(suggestion.type) }}
                    </span>
                  </button>
                    <div v-if="hasSuggestFetched && !suggestions.length" class="suggestion-empty">검색 결과 없음</div>
                </template>
              </div>
            </div>

          <div class="search-divider"></div>

          <div class="date-picker-wrapper" @click.stop>
            <div class="search-item" @click="toggleCalendar">
              <label>날짜</label>
              <input type="text" :placeholder="searchStore.dateDisplayText" readonly>
            </div>

            <!-- Calendar Popup -->
            <div class="calendar-popup" v-if="isCalendarOpen">
              <div class="calendar-container">
                <!-- Navigation -->
                <button class="calendar-nav-btn nav-prev" @click="prevMonth">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                </button>

                <!-- Current Month -->
                <div class="calendar-month">
                  <div class="calendar-month-title">{{ currentYear }}년 {{ monthNames[currentMonth] }}</div>
                  <div class="calendar-weekdays">
                    <span
                      v-for="(day, index) in weekDays"
                      :key="'current-' + day"
                      class="weekday"
                      :class="{ sunday: index === 0, saturday: index === 6 }"
                    >{{ day }}</span>
                  </div>
                  <div class="calendar-days">
                    <span
                      v-for="(dayObj, index) in calendarDays"
                      :key="'current-day-' + index"
                      class="calendar-day"
                      :class="{
                        'empty': dayObj.isEmpty,
                        'today': dayObj.isToday,
                        'weekend-sat': dayObj.isSaturday,
                        'weekend-sun': dayObj.isSunday,
                        'range-start': dayObj.isStartDate,
                        'range-end': dayObj.isEndDate,
                        'in-range': dayObj.isInRange,
                        'has-end': dayObj.isStartDate && dayObj.hasEndDate,
                        'disabled': dayObj.isDisabled,
                        'sunday': dayObj.isSunday,
                        'saturday': dayObj.isSaturday,
                        'holiday': dayObj.isHoliday
                      }"
                      :title="dayObj.holidayName || null"
                      @click="selectDate(dayObj)"
                    >
                      {{ dayObj.day }}
                    </span>
                  </div>
                </div>

                <!-- Next Month -->
                <div class="calendar-month">
                  <div class="calendar-month-title">{{ nextMonthYear }}년 {{ monthNames[nextMonthMonth] }}</div>
                  <div class="calendar-weekdays">
                    <span
                      v-for="(day, index) in weekDays"
                      :key="'next-' + day"
                      class="weekday"
                      :class="{ sunday: index === 0, saturday: index === 6 }"
                    >{{ day }}</span>
                  </div>
                  <div class="calendar-days">
                    <span
                      v-for="(dayObj, index) in nextMonthDays"
                      :key="'next-day-' + index"
                      class="calendar-day"
                      :class="{
                        'empty': dayObj.isEmpty,
                        'today': dayObj.isToday,
                        'weekend-sat': dayObj.isSaturday,
                        'weekend-sun': dayObj.isSunday,
                        'range-start': dayObj.isStartDate,
                        'range-end': dayObj.isEndDate,
                        'in-range': dayObj.isInRange,
                        'has-end': dayObj.isStartDate && dayObj.hasEndDate,
                        'disabled': dayObj.isDisabled,
                        'sunday': dayObj.isSunday,
                        'saturday': dayObj.isSaturday,
                        'holiday': dayObj.isHoliday
                      }"
                      :title="dayObj.holidayName || null"
                      @click="selectDate(dayObj)"
                    >
                      {{ dayObj.day }}
                    </span>
                  </div>
                </div>

                <!-- Navigation -->
                <button class="calendar-nav-btn nav-next" @click="nextMonth">
                  <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <div class="search-divider"></div>

          <div class="guest-picker-wrapper" @click.stop>
            <div class="search-item" @click="toggleGuestPicker">
              <label>여행자</label>
              <input type="text" :placeholder="searchStore.guestDisplayText" readonly>
            </div>

            <!-- Guest Picker Popup -->
            <div class="guest-popup" v-if="isGuestOpen">
              <div class="guest-header">인원수를 입력하세요</div>
              <div class="guest-row">
                <div class="guest-info">
                  <span class="guest-type">게스트</span>
                </div>
                <div class="guest-controls">
                  <button class="guest-btn" @click="decreaseGuest" :disabled="searchStore.guestCount <= 1">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"></path>
                    </svg>
                  </button>
                  <span class="guest-count">{{ searchStore.guestCount }}</span>
                  <button class="guest-btn" @click="increaseGuest">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <button class="search-btn" aria-label="검색" @click="handleSearch">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </button>
        </div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: white;
  border-bottom: 1px solid #e8ecf0;
  position: sticky;
  top: 0;
  z-index: 200;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 10px 16px;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  height: 40px;
  width: auto;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.logo:hover {
  opacity: 0.8;
}

/* Search Bar Styles */
.search-bar {
  width: 100%;
  background: white;
  border: 1px solid #e0e6eb;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-bar:hover {
  border-color: #6DC3BB;
  box-shadow: 0 4px 12px rgba(109, 195, 187, 0.1);
}

/* Hide mobile elements on desktop */
.search-bar-collapsed,
.search-bar-expanded {
  display: none !important;
}

/* Show desktop elements on desktop */
.search-bar-desktop {
  display: contents;
}

.search-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 6px 8px;
  border-radius: 6px;
  min-width: 0;
}

.search-keyword-wrapper {
  position: relative;
}

.search-suggestions {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: auto;
  width: min(560px, calc(100vw - 32px));
  min-width: 100%;
  font-family: sans-serif;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  z-index: 300;
  max-height: 280px;
  overflow-y: auto;
  padding: 6px;
  overscroll-behavior: contain;
  animation: suggestionDrop 0.16s ease-out;
}

.search-suggestion {
  --suggest-accent: #0f766e;
  width: 100%;
  border: 1px solid transparent;
  background: #ffffff;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #111827;
  border-radius: 10px;
  transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
  position: relative;
}

.search-suggestion + .search-suggestion {
  margin-top: 4px;
}

.search-suggestion::before {
  content: '';
  width: 3px;
  height: 18px;
  border-radius: 999px;
  background: var(--suggest-accent);
  opacity: 0.6;
  flex-shrink: 0;
}

.search-suggestion--region {
  --suggest-accent: #2563eb;
}

.search-suggestion--accommodation {
  --suggest-accent: #0f766e;
}

.search-suggestion:hover,
.search-suggestion:focus-visible {
  background: #f8fafc;
  border-color: #e2e8f0;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  outline: none;
}

.suggestion-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 999px;
  letter-spacing: 0.2px;
  flex-shrink: 0;
  border: 1px solid #e2e8f0;
  color: #475569;
  background: #f8fafc;
  margin-left: auto;
}

.search-suggestion--accommodation .suggestion-tag {
  background: #ecfdf5;
  color: #0f766e;
  border-color: #99f6e4;
}

.search-suggestion--region .suggestion-tag {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.suggestion-text {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.suggestion-empty,
.suggestion-loading {
  padding: 14px 8px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}

.suggestion-loading {
  color: #0f766e;
  font-weight: 600;
}

.search-suggestions::-webkit-scrollbar {
  width: 6px;
}

.search-suggestions::-webkit-scrollbar-thumb {
  background: rgba(15, 23, 42, 0.2);
  border-radius: 999px;
}

.search-suggestions::-webkit-scrollbar-track {
  background: transparent;
}

@keyframes suggestionDrop {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .search-suggestions {
    animation: none;
  }

  .search-suggestion {
    transition: none;
  }
}

.search-item:hover {
  background-color: #f0f7f6;
}

.search-item label {
  font-size: 10px;
  font-weight: 600;
  color: #8a92a0;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 2px;
  display: block;
  font-family: 'Poppins', sans-serif;
  white-space: nowrap;
}

.search-item input {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #1a1f36;
  outline: none;
  font-weight: 500;
  font-family: 'Noto Sans KR', sans-serif;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-item input::placeholder {
  color: #c5cdd4;
  font-weight: 400;
}

.calendar-day.weekend-sat {
  color: #2563eb;
}
.calendar-day.weekend-sun {
  color: #dc2626;
}

.search-divider {
  width: 1px;
  height: 20px;
  background-color: #e8ecf0;
  flex-shrink: 0;
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: #6DC3BB;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: white;
  transition: all 0.3s ease;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(109, 195, 187, 0.2);
}

.search-btn:hover {
  background-color: #5aaca3;
  box-shadow: 0 4px 16px rgba(109, 195, 187, 0.3);
  transform: scale(1.05);
}

.search-btn:active {
  transform: scale(0.95);
}

.search-btn svg {
  width: 16px;
  height: 16px;
}

/* Right Menu Area */
.right-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  position: relative; /* Anchor for dropdown */
}

.hamburger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #3d4452;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.hamburger-btn:hover {
  background-color: #f0f4f8;
  color: #1a1f36;
}

.hamburger-btn svg {
  width: 22px;
  height: 22px;
}

/* Enhanced Mobile Menu Dropdown */
.mobile-menu-dropdown {
  display: none;
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 300px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  padding: 24px;
  z-index: 1000;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
}

.mobile-menu-dropdown.active {
  display: block;
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.menu-title {
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

/* Host Toggle Card */
.host-toggle-card {
  background: #EFF6FF; /* Very light blue */
  padding: 16px 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  cursor: pointer;
  transition: all 0.2s;
}

.host-toggle-card:hover {
  background: #DBEAFE;
  transform: translateX(4px);
}

.host-toggle-card:active {
  transform: translateX(2px);
}

.toggle-title {
  font-size: 14px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}

.toggle-status {
  font-size: 12px;
  color: #6B7280;
}

.toggle-icon {
  font-size: 24px;
  color: #6B7280;
  font-weight: bold;
  transition: transform 0.2s;
}

.host-toggle-card:hover .toggle-icon {
  transform: translateX(4px);
  color: #00796b;
}


/* Menu List */
.menu-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px;
}

.menu-list li {
  margin-bottom: 12px;
}

.menu-list li:last-child {
  margin-bottom: 0;
}

.menu-list a {
  text-decoration: none;
  color: #374151;
  font-size: 15px;
  font-weight: 500;
  display: block;
  padding: 8px 0;
  transition: color 0.2s;
}

.menu-list a:hover {
  color: #6DC3BB;
  padding-left: 4px;
}

/* Menu Footer (Logout) */
.menu-footer {
  border-top: 1px solid #F3F4F6;
  padding-top: 20px;
}

.logout-btn {
  background: none;
  border: none;
  color: #EF4444; /* Red */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.logout-btn:hover {
  text-decoration: underline;
}

.login-btn {
  background: none;
  border: none;
  color: #00796b; /* Teal */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.login-btn:hover {
  text-decoration: underline;
}

/* Desktop styles */
@media (min-width: 769px) {
  .header-container {
    padding: 8px 24px 4px;
  }

  .header-content {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    align-items: center;
    column-gap: 16px;
  }

  .header-top {
    display: contents;
  }

  .logo {
    height: 48px;
    flex-shrink: 0;
    order: 1;
    justify-self: start;
    grid-column: 1;
  }

  .search-bar {
    max-width: 850px;
    width: min(850px, 100%);
    order: 2;
    justify-self: center;
    grid-column: 2;
    border-radius: 40px;
    padding: 8px 16px;
    gap: 12px;
  }

  .search-item {
    padding: 6px 10px;
    min-width: 150px;
  }

  .search-item label {
    font-size: 12px;
    display: block !important;
  }

  .search-item input {
    font-size: 14px;
  }

  .search-divider {
    display: block !important;
    height: 20px;
  }

  .search-btn {
    width: 32px;
    height: 32px;
  }

  .search-btn svg {
    width: 16px;
    height: 16px;
  }

  .right-menu {
    flex-shrink: 0;
    order: 3;
    justify-self: end;
    grid-column: 3;
  }

  .hamburger-btn {
    width: 44px;
    height: 44px;
  }

  .hamburger-btn svg {
    width: 24px;
    height: 24px;
  }
}

/* Mobile styles */
@media (max-width: 768px) {
  .app-header {
    overflow: visible;
  }

  .header-container {
    padding: 8px 12px 4px;
    max-width: 100%;
    overflow: visible;
  }

  .header-content {
    gap: 0;
    max-width: 100%;
  }

  .header-top {
    width: 100%;
  }

  .logo {
    height: 36px;
  }

  .right-menu {
    position: relative;
  }
  
  /* Mobile menu full width on very small screens if needed, 
     but 300px width is fine for most mobiles */
  .mobile-menu-dropdown {
    right: -8px;
    width: 280px;
  }

  /* Hide desktop search bar on mobile */
  .search-bar-desktop {
    display: none !important;
  }

  .search-bar .search-btn {
    display: none !important;
  }

  .search-bar {
    width: 100%;
    max-width: 100%;
    padding: 0;
    border: none;
    box-shadow: none;
    background: transparent;
  }
  
  /* Collapsed Mobile Search Bar */
  .search-bar-collapsed {
    display: flex !important;
    align-items: center;
    background: white;
    border: 1px solid #e0e6eb;
    border-radius: 20px;
    padding: 8px 16px;
    gap: 8px;
    width: 100%;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
  
  .search-bar.expanded .search-bar-collapsed {
    display: none !important;
  }
  
    .search-bar.expanded .search-bar-expanded {
      display: flex !important;
    }

    .search-suggestions {
      left: 50%;
      transform: translateX(-50%);
    }

    .search-bar-collapsed,
    .search-bar-expanded,
    .collapsed-text,
    .collapsed-divider,
    .search-item-full label,
    .search-item-full input {
      font-family: revert;
    }

    .search-item-full label {
      font-family: sans-serif;
    }

    .collapsed-text {
      flex: 1 1 0;
      min-width: 0;
      font-size: 13px;
    color: #6b7280;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .collapsed-text--keyword {
    flex: 1 1 0;
    min-width: 0;
  }
  
  .collapsed-divider {
    color: #e0e6eb;
    font-size: 12px;
  }
  
  .search-btn-mini {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    background-color: #6DC3BB;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    color: white;
    flex-shrink: 0;
  }
  
  /* Expanded Mobile Search Bar */
  .search-bar-expanded {
    display: flex;
    flex-direction: column;
    background: white;
    border: 1px solid #e0e6eb;
    border-radius: 20px;
    padding: 20px;
    width: 100%;
    position: relative;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
  
  .expanded-close {
    position: absolute;
    top: 12px;
    right: 16px;
    font-size: 24px;
    color: #9ca3af;
    cursor: pointer;
    line-height: 1;
  }
  
  .expanded-close:hover {
    color: #374151;
  }
  
  .search-item-full {
    display: flex;
    flex-direction: column;
    padding: 16px 0;
    border-bottom: 1px solid #f0f0f0;
  }
  
  .search-item-full:last-of-type {
    border-bottom: none;
  }
  
  .search-item-full label {
    font-size: 12px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 6px;
  }
  
  .search-item-full input {
    background: transparent;
    border: none;
    font-size: 16px;
    color: #1a1f36;
    outline: none;
    padding: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .search-item-full input::placeholder {
    color: #9ca3af;
  }
  
  .search-btn-full {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 48px;
    background-color: #6DC3BB;
    border: none;
    border-radius: 12px;
    cursor: pointer;
    color: white;
    margin-top: 16px;
    font-size: 16px;
    font-weight: 600;
    gap: 8px;
  }
  
  .search-btn-full:hover {
    background-color: #5aaca3;
  }
  
  /* Mobile Calendar Popup */
  .mobile-calendar-popup {
    background: #fff;
    border: 1px solid #f0f0f0;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
    border-radius: 16px;
    padding: 16px;
    margin: 8px 0;
    font-family: 'Noto Sans KR', sans-serif;
    animation: calendarFadeIn 0.2s ease;
  }
  
  @keyframes calendarFadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .mobile-calendar-popup .calendar-container {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
  
  .mobile-calendar-popup .calendar-month {
    width: 100%;
  }
  
  .mobile-calendar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  
  .mobile-calendar-header .calendar-month-title {
    font-size: 16px;
    font-weight: 700;
    color: #1a1f36;
  }
  
  .mobile-calendar-popup .calendar-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
    margin-bottom: 8px;
  }
  
  .mobile-calendar-popup .weekday {
    text-align: center;
    font-size: 12px;
    font-weight: 600;
    color: #9ca3af;
    padding: 8px 0;
  }
  
  .mobile-calendar-popup .weekday.sunday {
    color: #ef4444;
  }
  
  .mobile-calendar-popup .weekday.saturday {
    color: #2563eb;
  }
  
  .mobile-calendar-popup .calendar-days {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
  }
  
  .mobile-calendar-popup .calendar-day {
    text-align: center;
    font-size: 14px;
    font-weight: 500;
    color: #374151;
    padding: 12px 0;
    border-radius: 8px;
    cursor: pointer;
    position: relative;
    transition: all 0.2s ease;
  }
  
  .mobile-calendar-popup .calendar-day.sunday:not(.range-start):not(.range-end):not(.disabled) {
    color: #ef4444;
  }
  
  .mobile-calendar-popup .calendar-day.saturday:not(.range-start):not(.range-end):not(.disabled) {
    color: #2563eb;
  }
  
  .mobile-calendar-popup .calendar-day.holiday:not(.range-start):not(.range-end):not(.disabled) {
    color: #ef4444;
    font-weight: 600;
  }
  
  .mobile-calendar-popup .calendar-day.today:not(.range-start):not(.range-end):not(.in-range) {
    color: #6DC3BB;
    font-weight: 700;
  }
  
  .mobile-calendar-popup .calendar-day:not(.empty):not(.disabled):hover {
    background-color: #f0f7f6;
    color: #6DC3BB;
  }
  
  .mobile-calendar-popup .calendar-day.in-range {
    background-color: #BFE7DF;
    color: #2d7a73;
    border-radius: 0;
  }
  
  .mobile-calendar-popup .calendar-day.range-start,
  .mobile-calendar-popup .calendar-day.range-end {
    background-color: #5CC5B3;
    color: #fff;
    font-weight: 700;
  }
  
  .mobile-calendar-popup .calendar-day.range-start:hover,
  .mobile-calendar-popup .calendar-day.range-end:hover {
    background-color: #49B5A3;
    color: #fff;
  }
  
  .mobile-calendar-popup .nav-prev,
  .mobile-calendar-popup .nav-next {
    display: flex;
    flex-shrink: 0;
  }
  
  .mobile-calendar-popup .calendar-month-title {
    text-align: center;
  }
  
  /* Mobile Guest Popup */
  .mobile-guest-popup {
    background: #f9fafb;
    border-radius: 12px;
    padding: 16px;
    margin: 8px 0;
  }
  
  .mobile-guest-popup .guest-header {
    font-size: 16px;
    margin-bottom: 12px;
  }
  
  .mobile-guest-popup .guest-row {
    padding: 12px 0;
    border-bottom: none;
  }
}

/* Date Picker Styles */
.date-picker-wrapper {
  position: relative;
  flex: 1;
  min-width: 0;
}

.calendar-popup {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  width: 680px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 24px;
  z-index: 90;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
  animation: calendarFadeIn 0.2s ease;
}

@keyframes calendarFadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.calendar-container {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.calendar-month {
  flex: 1;
  min-width: 280px;
}

.calendar-month-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1f36;
  text-align: center;
  margin-bottom: 16px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.calendar-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1f36;
}

.calendar-nav-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  color: #6b7280;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-nav-btn:hover {
  background-color: #f0f7f6;
  color: #6DC3BB;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  padding: 8px 0;
}

.weekday.sunday {
  color: #ef4444;
}

.weekday.saturday {
  color: #2563eb;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  padding: 12px 0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.calendar-day.disabled {
  color: #b4b8bf;
  cursor: not-allowed;
  pointer-events: none;
}

.calendar-day.sunday:not(.range-start):not(.range-end):not(.disabled) {
  color: #ef4444;
}

.calendar-day.holiday:not(.range-start):not(.range-end):not(.disabled) {
  color: #ef4444;
  font-weight: 600;
}

.calendar-day.saturday:not(.range-start):not(.range-end):not(.disabled) {
  color: #2563eb;
}

.calendar-day:not(.empty):not(.disabled):hover {
  background-color: #f0f7f6;
  color: #6DC3BB;
}

.calendar-day.empty {
  cursor: default;
}

.calendar-day.today:not(.range-start):not(.range-end):not(.in-range):not(.disabled) {
  color: #6DC3BB;
  font-weight: 700;
}

/* Range selection styles - using theme color #6DC3BB */
.calendar-day.range-start,
.calendar-day.range-end {
  background-color: #5CC5B3;
  color: white;
  font-weight: 700;
  position: relative;
}

/* Add connecting background line for start date (to the right) */
.calendar-day.range-start.has-end::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 50%;
  height: 100%;
  background-color: #BFE7DF;
  z-index: -1;
}

/* Add connecting background line for end date (to the left) */
.calendar-day.range-end::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 100%;
  background-color: #BFE7DF;
  z-index: -1;
}

.calendar-day.range-start:hover,
.calendar-day.range-end:hover {
  background-color: #49B5A3;
  color: white;
}

.calendar-day.in-range {
  background-color: #BFE7DF;
  color: #2d7a73;
  border-radius: 0;
}

/* Mobile calendar adjustments */
@media (max-width: 768px) {
  .calendar-popup {
    width: 320px;
    left: 0;
    transform: translateX(0);
    padding: 16px;
  }
  
  .calendar-container {
    flex-direction: column;
    gap: 24px;
  }
  
  .calendar-month {
    min-width: 100%;
  }
  
  .nav-prev, .nav-next {
    position: absolute;
    top: 16px;
  }
  
  .nav-prev {
    left: 8px;
  }
  
  .nav-next {
    right: 8px;
  }
  
  @keyframes calendarFadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .calendar-day {
    padding: 10px 0;
    font-size: 13px;
  }

  .calendar-day.range-start.has-end::after,
  .calendar-day.range-end::before {
    display: none;
  }
}

/* Navigation button positioning for desktop */
@media (min-width: 769px) {
  .nav-prev, .nav-next {
    flex-shrink: 0;
  }
}

/* Guest Picker Styles */
.guest-picker-wrapper {
  position: relative;
  flex: 1;
  min-width: 0;
}

.guest-popup {
  position: absolute;
  top: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 24px;
  z-index: 90;
  font-family: 'Noto Sans KR', sans-serif;
  border: 1px solid #f0f0f0;
  animation: guestFadeIn 0.2s ease;
}

@keyframes guestFadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

.guest-header {
  font-size: 20px;
  font-weight: 700;
  color: #1a1f36;
  margin-bottom: 20px;
}

.guest-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.guest-row:last-child {
  border-bottom: none;
}

.guest-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.guest-type {
  font-size: 16px;
  font-weight: 600;
  color: #1a1f36;
}

.guest-desc {
  font-size: 13px;
  color: #6b7280;
}

.guest-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.guest-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #e0e6eb;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #374151;
  transition: all 0.2s ease;
}

.guest-btn:hover:not(:disabled) {
  border-color: #6DC3BB;
  color: #6DC3BB;
}

.guest-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.guest-count {
  font-size: 16px;
  font-weight: 600;
  color: #1a1f36;
  min-width: 24px;
  text-align: center;
}

/* Mobile guest picker */
@media (max-width: 768px) {
  .guest-popup {
    width: 280px;
    left: 0;
    transform: translateX(0);
    padding: 20px;
  }
  
  @keyframes guestFadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}
</style>
