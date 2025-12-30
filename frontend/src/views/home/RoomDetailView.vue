<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSearchStore } from '@/stores/search'
import { fetchAccommodationDetail } from '@/api/accommodation'
import ImageGallery from './room-detail/features/ImageGallery.vue'
import ReviewSection from './room-detail/features/ReviewSection.vue'
import MapSection from './room-detail/features/MapSection.vue'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()

const DEFAULT_IMAGE = 'https://via.placeholder.com/800x600'
const DEFAULT_HOST_IMAGE = 'https://picsum.photos/seed/host/100/100'

const toNumber = (value, fallback = 0) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

const getAccommodationId = () => {
  const parsed = Number(route.params.id)
  return Number.isFinite(parsed) ? parsed : null
}

const buildMapQuery = () => {
  const query = {}
  const minValue = route.query.min ?? route.query.minPrice
  const maxValue = route.query.max ?? route.query.maxPrice
  if (minValue !== undefined) query.min = String(minValue)
  if (maxValue !== undefined) query.max = String(maxValue)
  if (route.query.themeIds) query.themeIds = String(route.query.themeIds)
  return query
}

const goBack = () => {
  if (route.query.from === 'map') {
    router.push({ path: '/map', query: buildMapQuery() })
    return
  }
  router.back()
}

const createEmptyGuesthouse = (id = null) => ({
  id,
  name: '',
  rating: '-',
  reviewCount: 0,
  address: '',
  description: '',
  minPrice: null,
  checkInTime: '',
  checkOutTime: '',
  transportInfo: '',
  parkingInfo: '',
  sns: '',
  phone: '',
  latitude: null,
  longitude: null,
  amenities: [],
  themes: [],
  host: {
    name: '호스트',
    joined: '',
    image: DEFAULT_HOST_IMAGE
  },
  images: [DEFAULT_IMAGE],
  rooms: [],
  reviews: []
})

const buildAddress = (data) => {
  return [data?.city, data?.district, data?.township, data?.addressDetail]
    .filter(Boolean)
    .join(' ')
}

const normalizeRooms = (rooms, fallbackPrice) => {
  if (!Array.isArray(rooms)) return []
  return rooms
    .filter((room) => room?.roomStatus === 1)
    .map((room) => {
      const price = toNumber(room?.price ?? room?.weekendPrice ?? fallbackPrice, fallbackPrice)
      const roomStatus = room?.roomStatus
      const available = roomStatus == null ? true : roomStatus === 1
      const imageUrl = room?.mainImageUrl || DEFAULT_IMAGE
      
      // 썸네일 URL 생성
      let thumbnailUrl = imageUrl
      if (imageUrl.includes('ncloudstorage.com')) {
        thumbnailUrl = imageUrl
          .replace('/room/', '/room_thumb/')
          .replace(/\.(png|gif|webp)$/i, '.jpg')
      }

      return {
        id: room?.roomId ?? room?.id,
        name: room?.roomName ?? room?.name ?? '',
        desc: room?.roomDescription ?? room?.description ?? '',
        capacity: toNumber(room?.maxGuests ?? room?.capacity ?? 0, 0),
        price,
        available,
        imageUrl,
        thumbnailUrl
      }
    })
}

const normalizeAmenities = (data) => {
  if (Array.isArray(data?.amenityDetails)) {
    return data.amenityDetails
      .map((amenity) => ({
        name: amenity?.amenityName ?? amenity?.name ?? '',
        icon: amenity?.amenityIcon ?? amenity?.icon ?? ''
      }))
      .filter((amenity) => amenity.name)
  }
  if (Array.isArray(data?.amenities)) {
    return data.amenities
      .map((name) => ({ name, icon: '' }))
      .filter((amenity) => amenity.name)
  }
  return []
}

const formatReviewDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
}

const normalizeReviews = (reviews) => {
  if (!Array.isArray(reviews)) return []
  return reviews.map((review) => {
    const ratingValue = Number(review?.rating)
    const rating = Number.isFinite(ratingValue) ? Math.round(ratingValue) : 0
    const images = Array.isArray(review?.images) ? review.images : []
    const imageUrls = images
      .map((image) => image?.imageUrl ?? image)
      .filter((url) => typeof url === 'string' && url.trim())
    const tags = Array.isArray(review?.tags) ? review.tags : []
    return {
      id: review?.reviewId ?? review?.id,
      author: review?.authorName ?? review?.reviewerEmail ?? review?.author ?? '',
      date: formatReviewDate(review?.createdAt ?? review?.date),
      rating,
      content: review?.content ?? '',
      replyContent: review?.replyContent ?? '',
      replyUpdatedAt: formatReviewDate(review?.replyUpdatedAt ?? null),
      image: imageUrls[0] ?? null,
      images: imageUrls,
      tags: tags
        .map((tag) => tag?.reviewTagName ?? tag?.name ?? tag)
        .filter((tag) => typeof tag === 'string' && tag.trim())
    }
  })
}

const normalizeDetail = (data) => {
  const imageUrls = Array.isArray(data?.images)
    ? data.images.map((image) => image?.imageUrl).filter(Boolean)
    : []
  const fallbackPrice = toNumber(data?.minPrice ?? 0, 0)
  const ratingValue = Number(data?.rating)
  const reviews = normalizeReviews(data?.reviews)
  return {
    id: data?.accommodationsId ?? null,
    name: data?.accommodationsName ?? '',
    rating: Number.isFinite(ratingValue) ? ratingValue.toFixed(2) : '-',
    reviewCount: toNumber(data?.reviewCount ?? reviews.length, 0),
    address: buildAddress(data),
    description: data?.accommodationsDescription ?? data?.shortDescription ?? '',
    minPrice: data?.minPrice ?? null,
    checkInTime: data?.checkInTime ?? '',
    checkOutTime: data?.checkOutTime ?? '',
    transportInfo: data?.transportInfo ?? '',
    parkingInfo: data?.parkingInfo ?? '',
    sns: data?.sns ?? '',
    phone: data?.phone ?? '',
    latitude: data?.latitude ?? null,
    longitude: data?.longitude ?? null,
    amenities: normalizeAmenities(data),
    themes: Array.isArray(data?.themes) ? data.themes : [],
    host: {
      name: '호스트',
      joined: '',
      image: DEFAULT_HOST_IMAGE
    },
    images: imageUrls.length ? imageUrls : [DEFAULT_IMAGE],
    rooms: normalizeRooms(data?.rooms, fallbackPrice),
    reviews
  }
}

const guesthouse = ref(createEmptyGuesthouse(getAccommodationId()))
const selectedRoom = ref(null)
const isCalendarOpen = ref(false)
const showFullDescription = ref(false)
const showAllRooms = ref(false)
const currentDate = ref(new Date())
const datePickerRef = ref(null)

const canBook = computed(() => {
  return Boolean(selectedRoom.value && searchStore.startDate && searchStore.endDate)
})

const visibleRooms = computed(() => {
  const rooms = guesthouse.value.rooms || []
  return showAllRooms.value ? rooms : rooms.slice(0, 4)
})

const hasMoreRooms = computed(() => {
  const rooms = guesthouse.value.rooms || []
  return rooms.length > 4
})

const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']
const weekDays = ['일', '월', '화', '수', '목', '금', '토']

const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth())
const nextMonthDate = computed(() => new Date(currentYear.value, currentMonth.value + 1, 1))
const nextMonthYear = computed(() => nextMonthDate.value.getFullYear())
const nextMonthMonth = computed(() => nextMonthDate.value.getMonth())

const isDescriptionLong = computed(() => {
  return (guesthouse.value.description || '').length > 120
})

const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear()
    && date1.getMonth() === date2.getMonth()
    && date1.getDate() === date2.getDate()
}

const isDateInRange = (date) => {
  if (!searchStore.startDate || !searchStore.endDate) return false
  const time = date.getTime()
  return time > searchStore.startDate.getTime() && time < searchStore.endDate.getTime()
}

const getCalendarDays = (year, month) => {
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()
  const startingDay = firstDay.getDay()

  const days = []
  for (let i = 0; i < startingDay; i += 1) {
    days.push({ day: '', isEmpty: true })
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = new Date(year, month, day)
    const isStartDate = searchStore.startDate && isSameDay(date, searchStore.startDate)
    const isEndDate = searchStore.endDate && isSameDay(date, searchStore.endDate)
    const isInRange = isDateInRange(date)
    const hasEndDate = searchStore.endDate !== null

    days.push({
      day,
      isEmpty: false,
      date,
      isToday: isSameDay(date, new Date()),
      isStartDate,
      isEndDate,
      isInRange,
      hasEndDate
    })
  }
  return days
}

const calendarDays = computed(() => getCalendarDays(currentYear.value, currentMonth.value))
const nextMonthDays = computed(() => getCalendarDays(nextMonthYear.value, nextMonthMonth.value))

const toggleCalendar = () => {
  isCalendarOpen.value = !isCalendarOpen.value
}

const prevMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value - 1, 1)
}

const nextMonth = () => {
  currentDate.value = new Date(currentYear.value, currentMonth.value + 1, 1)
}

const selectDate = (dayObj) => {
  if (dayObj.isEmpty) return

  const clickedDate = dayObj.date
  if (!searchStore.startDate || (searchStore.startDate && searchStore.endDate)) {
    searchStore.setStartDate(clickedDate)
    searchStore.setEndDate(null)
    return
  }

  if (clickedDate.getTime() < searchStore.startDate.getTime()) {
    searchStore.setEndDate(searchStore.startDate)
    searchStore.setStartDate(clickedDate)
    isCalendarOpen.value = false
    return
  }

  searchStore.setEndDate(clickedDate)
  isCalendarOpen.value = false
}

const loadAccommodation = async () => {
  const accommodationsId = getAccommodationId()
  if (!accommodationsId) {
    guesthouse.value = createEmptyGuesthouse()
    return
  }

  selectedRoom.value = null
  showFullDescription.value = false
  showAllRooms.value = false
  guesthouse.value = createEmptyGuesthouse(accommodationsId)

  try {
    const response = await fetchAccommodationDetail(accommodationsId)
    if (!response.ok || !response.data) {
      guesthouse.value = createEmptyGuesthouse(accommodationsId)
      return
    }
    guesthouse.value = normalizeDetail(response.data)
  } catch (error) {
    console.error('Failed to load accommodation detail', error)
    guesthouse.value = createEmptyGuesthouse(accommodationsId)
  }
}

const selectRoom = (room) => {
  if (selectedRoom.value?.id === room?.id) {
    selectedRoom.value = null
    return
  }
  selectedRoom.value = room
}

const formatPrice = (price) => {
  if (price == null) return '-'
  const parsed = Number(price)
  if (!Number.isFinite(parsed)) return '-'
  return parsed.toLocaleString()
}

const formatDateParam = (date) => {
  if (!date) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getSnsType = (line, url) => {
  const lower = line.toLowerCase()
  const target = `${lower} ${String(url).toLowerCase()}`
  if (target.includes('instagram') || target.includes('insta') || line.includes('인스타')) return 'instagram'
  if (target.includes('blog') || line.includes('블로그') || target.includes('naver')) return 'blog'
  if (target.includes('youtube') || line.includes('유튜브')) return 'youtube'
  if (target.includes('facebook') || line.includes('페이스북')) return 'facebook'
  return 'link'
}

const getSnsLabel = (type, url) => {
  if (type === 'instagram') return 'Instagram'
  if (type === 'blog') return 'Blog'
  if (type === 'youtube') return 'YouTube'
  if (type === 'facebook') return 'Facebook'
  try {
    const host = new URL(url).hostname.replace(/^www\./, '')
    return host || '링크'
  } catch (error) {
    return '링크'
  }
}

const buildSnsLinks = (sns) => {
  if (!sns) return []
  const lines = String(sns)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  const results = []
  const seen = new Set()

  lines.forEach((line) => {
    const match = line.match(/https?:\/\/[^\s]+/i)
    if (!match) return
    const url = match[0]
    if (seen.has(url)) return
    seen.add(url)
    const type = getSnsType(line, url)
    results.push({ url, type, label: getSnsLabel(type, url) })
  })

  return results
}

const snsLinks = computed(() => buildSnsLinks(guesthouse.value.sns))

const increaseGuest = () => {
  searchStore.increaseGuest()
}

const decreaseGuest = () => {
  searchStore.decreaseGuest()
}

// Navigate to booking with all data
const goToBooking = () => {
  if (!canBook.value) return

  const accommodationsId = guesthouse.value.id ?? getAccommodationId()
  if (!accommodationsId) return

  // Format dates for display
  let dateDisplay = '날짜를 선택하세요'
  if (searchStore.startDate && searchStore.endDate) {
    const formatDate = (date) => {
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      const day = date.getDate()
      return `${year}년 ${month}월 ${day}일`
    }
    dateDisplay = `${formatDate(searchStore.startDate)} ~ ${formatDate(searchStore.endDate)}`
  }

  const guestCount = searchStore.guestCount || 1

  router.push({
    path: `/booking/${accommodationsId}`,
    query: {
      roomId: selectedRoom.value.id,
      guestCount,
      checkin: formatDateParam(searchStore.startDate),
      checkout: formatDateParam(searchStore.endDate)
    }
  })
}

const openCalendarFromHint = (event) => {
  event.stopPropagation()
  if (datePickerRef.value?.scrollIntoView) {
    datePickerRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
  isCalendarOpen.value = true
}

const handleClickOutside = (event) => {
  if (event.target.closest('.date-picker-wrapper')) return
  if (event.target.closest('.booking-hint')) return
  isCalendarOpen.value = false
}

onMounted(loadAccommodation)
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
watch(() => route.params.id, loadAccommodation)
</script>

<template>
  <div class="room-detail container">
    <!-- Header with Back Button -->
    <div class="detail-header">
      <button class="back-btn" @click="goBack">← 뒤로가기</button>
    </div>

    <!-- Image Grid -->
    <ImageGallery :images="guesthouse.images" :name="guesthouse.name" />

    <!-- Title & Info -->
    <section class="section info-section">
      <h1>{{ guesthouse.name }}</h1>
      <div class="meta">
        <span class="rating">★ {{ guesthouse.rating }} (리뷰 {{ guesthouse.reviewCount }}개)</span>
        <span class="location">{{ guesthouse.address }}</span>
      </div>
      <div class="description-row">
        <p class="description" :class="{ 'description-clamped': !showFullDescription }">
          {{ guesthouse.description }}
        </p>
        <button
          v-if="isDescriptionLong"
          type="button"
          class="more-btn"
          @click="showFullDescription = !showFullDescription"
        >
          {{ showFullDescription ? '접기' : '더보기' }}
        </button>
      </div>
      <div class="transport-info" v-if="guesthouse.transportInfo">
        <h3 class="info-title">
          <span class="info-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M4 11.5v6.3c0 .9.7 1.7 1.6 1.7h.7v1.1c0 .6.5 1.1 1.1 1.1s1.1-.5 1.1-1.1v-1.1h7.1v1.1c0 .6.5 1.1 1.1 1.1s1.1-.5 1.1-1.1v-1.1h.7c.9 0 1.6-.7 1.6-1.7v-6.3c0-2.5-2-4.5-4.5-4.5h-6.1c-2.5 0-4.5 2-4.5 4.5Zm12.1-2.6c1.5 0 2.7 1.2 2.7 2.6v.3H5.2v-.3c0-1.5 1.2-2.6 2.7-2.6h8.2Zm-9.6 7.4c-.5 0-1-.4-1-1s.4-1 1-1 1 .4 1 1-.4 1-1 1Zm11.1 0c-.5 0-1-.4-1-1s.4-1 1-1 1 .4 1 1-.4 1-1 1Z" />
            </svg>
          </span>
          교통 정보
        </h3>
        <p>{{ guesthouse.transportInfo }}</p>
      </div>
      <div class="contact-info" v-if="guesthouse.phone">
        <h3 class="info-title">
          <span class="info-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M6.6 3.5c.5-.4 1.2-.4 1.7 0l2 1.7c.6.5.7 1.4.3 2.1l-1 1.8c-.2.4-.2.9.1 1.3 1 1.6 2.4 3 4 4 .4.2.9.3 1.3.1l1.8-1c.7-.4 1.6-.3 2.1.3l1.7 2c.4.5.4 1.2 0 1.7l-1.3 1.5c-.5.6-1.2.9-2 .8-2.7-.4-5.3-1.8-7.7-4.2s-3.8-5-4.2-7.7c-.1-.8.2-1.5.8-2l1.5-1.3Z" />
            </svg>
          </span>
          연락처
        </h3>
        <p>{{ guesthouse.phone }}</p>
      </div>
    </section>

    <section class="section amenity-section">
      <h2>편의시설</h2>
      <div class="tag-list amenity-list">
        <span
          v-for="(amenity, idx) in guesthouse.amenities"
          :key="amenity.name || idx"
          class="tag amenity-tag"
        >
          <span v-if="amenity.icon" class="amenity-icon" v-html="amenity.icon"></span>
          <span class="amenity-text">{{ amenity.name }}</span>
        </span>
        <span v-if="!guesthouse.amenities.length" class="tag empty">등록된 정보 없음</span>
      </div>
      <h2>테마</h2>
      <div class="tag-list">
        <span v-for="theme in guesthouse.themes" :key="theme" class="tag">{{ theme }}</span>
        <span v-if="!guesthouse.themes.length" class="tag empty">등록된 정보 없음</span>
      </div>
    </section>

    <hr />

    <section class="section extra-info-section">
      <h2>추가 정보</h2>
      <dl class="info-list">
        <div class="info-row">
          <dt>체크인</dt>
          <dd>{{ guesthouse.checkInTime || '정보 없음' }}</dd>
        </div>
        <div class="info-row">
          <dt>체크아웃</dt>
          <dd>{{ guesthouse.checkOutTime || '정보 없음' }}</dd>
        </div>
        <div class="info-row">
          <dt>주차</dt>
          <dd>{{ guesthouse.parkingInfo || '정보 없음' }}</dd>
        </div>
        <div class="info-row">
          <dt>SNS</dt>
          <dd>
            <template v-if="snsLinks.length">
              <a
                v-for="link in snsLinks"
                :key="link.url"
                :href="link.url"
                class="sns-link"
                target="_blank"
                rel="noopener noreferrer"
                :aria-label="link.label"
              >
                <svg v-if="link.type === 'instagram'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                  <rect x="3" y="3" width="18" height="18" rx="5" ry="5" fill="none" stroke="currentColor" stroke-width="2" />
                  <circle cx="12" cy="12" r="3.5" fill="none" stroke="currentColor" stroke-width="2" />
                  <circle cx="17.5" cy="6.5" r="1" fill="currentColor" />
                </svg>
                <svg v-else-if="link.type === 'youtube'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                  <rect x="3" y="6" width="18" height="12" rx="3" ry="3" fill="none" stroke="currentColor" stroke-width="2" />
                  <path d="M10 9l5 3-5 3z" fill="currentColor" />
                </svg>
                <svg v-else-if="link.type === 'blog'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                  <path d="M6 3h9l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z" fill="none" stroke="currentColor" stroke-width="2" />
                  <path d="M14 3v5h5" fill="none" stroke="currentColor" stroke-width="2" />
                  <path d="M8 13h8M8 17h6" fill="none" stroke="currentColor" stroke-width="2" />
                </svg>
                <svg v-else-if="link.type === 'facebook'" viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                  <path d="M15 8h3V5h-3c-2 0-4 2-4 4v3H8v3h3v6h3v-6h3l1-3h-4V9c0-.6.4-1 1-1z" fill="currentColor" />
                </svg>
                <svg v-else viewBox="0 0 24 24" class="sns-icon" aria-hidden="true">
                  <path d="M10 13a5 5 0 0 0 7.07 0l2.83-2.83a5 5 0 0 0-7.07-7.07L11 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M14 11a5 5 0 0 0-7.07 0L4.1 13.83a5 5 0 0 0 7.07 7.07L13 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <span class="sns-text">
                  {{ link.type === 'blog' && link.label.toLowerCase().includes('naver') ? '네이버 블로그 바로가기' : `${link.label} 바로가기` }}
                </span>
              </a>
            </template>
            <span v-else>{{ guesthouse.sns || '정보 없음' }}</span>
          </dd>
        </div>
      </dl>
    </section>

    <hr />

    <!-- Room Selection -->
    <section class="section room-selection">
      <h2>객실 선택</h2>
      
      <!-- Date & Guest Picker Mock -->
      <div class="picker-box">
        <div class="picker-row">
          <div ref="datePickerRef" class="picker-field date-picker-wrapper" @click.stop>
            <label>체크인 / 체크아웃</label>
            <button
              type="button"
              class="date-display"
              :aria-expanded="isCalendarOpen"
              @click="toggleCalendar"
            >
              {{ searchStore.checkInOutText }}
            </button>

            <div class="calendar-popup" v-if="isCalendarOpen">
              <div class="calendar-container">
                <button class="calendar-nav-btn nav-prev" type="button" @click="prevMonth">
                  ‹
                </button>

                <div class="calendar-month">
                  <div class="calendar-month-title">{{ currentYear }}년 {{ monthNames[currentMonth] }}</div>
                  <div class="calendar-weekdays">
                    <span v-for="day in weekDays" :key="'current-' + day" class="weekday">{{ day }}</span>
                  </div>
                  <div class="calendar-days">
                    <span
                      v-for="(dayObj, index) in calendarDays"
                      :key="'current-day-' + index"
                      class="calendar-day"
                      :class="{
                        empty: dayObj.isEmpty,
                        today: dayObj.isToday,
                        'range-start': dayObj.isStartDate,
                        'range-end': dayObj.isEndDate,
                        'in-range': dayObj.isInRange,
                        'has-end': dayObj.isStartDate && dayObj.hasEndDate
                      }"
                      @click="selectDate(dayObj)"
                    >
                      {{ dayObj.day }}
                    </span>
                  </div>
                </div>

                <div class="calendar-month">
                  <div class="calendar-month-title">{{ nextMonthYear }}년 {{ monthNames[nextMonthMonth] }}</div>
                  <div class="calendar-weekdays">
                    <span v-for="day in weekDays" :key="'next-' + day" class="weekday">{{ day }}</span>
                  </div>
                  <div class="calendar-days">
                    <span
                      v-for="(dayObj, index) in nextMonthDays"
                      :key="'next-day-' + index"
                      class="calendar-day"
                      :class="{
                        empty: dayObj.isEmpty,
                        today: dayObj.isToday,
                        'range-start': dayObj.isStartDate,
                        'range-end': dayObj.isEndDate,
                        'in-range': dayObj.isInRange,
                        'has-end': dayObj.isStartDate && dayObj.hasEndDate
                      }"
                      @click="selectDate(dayObj)"
                    >
                      {{ dayObj.day }}
                    </span>
                  </div>
                </div>

                <button class="calendar-nav-btn nav-next" type="button" @click="nextMonth">
                  ›
                </button>
              </div>
            </div>
          </div>
          <div class="picker-field">
            <label>투숙 인원</label>
            <div class="guest-control">
              <button @click="decreaseGuest" :disabled="searchStore.guestCount <= 1">-</button>
              <span>게스트 {{ searchStore.guestCount || 1 }}명</span>
              <button @click="increaseGuest">+</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Room List -->
      <div class="room-list">
        <div v-for="room in visibleRooms" :key="room.id"
             class="room-card"
             :class="{ selected: selectedRoom?.id === room.id, unavailable: !room.available }"
             @click="room.available && selectRoom(room)">
          <div class="room-media">
            <img :src="room.thumbnailUrl" :alt="room.name" loading="lazy" />
            <span v-if="!room.available" class="room-unavailable-badge">사용 중지</span>
          </div>
          <div class="room-content">
            <div class="room-info">
              <h3>{{ room.name }}</h3>
              <p>{{ room.desc }}</p>
              <span class="capacity">최대 {{ room.capacity }}명</span>
            </div>
            <div class="room-action">
              <div class="price">₩{{ formatPrice(room.price) }}</div>
              <button
                class="select-btn"
                :class="{ active: selectedRoom?.id === room.id }"
                :disabled="!room.available"
              >
                {{ !room.available ? '사용 중지' : (selectedRoom?.id === room.id ? '선택됨' : '객실') }}
              </button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="hasMoreRooms" class="room-toggle-row">
        <button type="button" class="room-toggle-btn" @click="showAllRooms = !showAllRooms">
          {{ showAllRooms ? '접기' : '더보기' }}
        </button>
      </div>
    </section>

    <hr />

    <!-- Reviews -->
    <ReviewSection :reviews="guesthouse.reviews" :name="guesthouse.name" />

    <!-- Map -->
    <MapSection
      :latitude="guesthouse.latitude"
      :longitude="guesthouse.longitude"
      :address="guesthouse.address"
      :transport-info="guesthouse.transportInfo"
    />

    <!-- Rules -->
    <section class="section rules-section">
      <div class="rule-box">
        <h3>환불 규정</h3>
        <ul>
          <li>체크인 7일 전 취소: 결제 금액 100% 환불</li>
          <li>체크인 5~6일 전 취소: 결제 금액의 90% 환불</li>
          <li>체크인 3~4일 전 취소: 결제 금액의 70% 환불</li>
          <li>체크인 1~2일 전 취소: 결제 금액의 50% 환불</li>
          <li>체크인 당일 취소 또는 노쇼(No-show): 환불 불가</li>
        </ul>
      </div>
    </section>

    <!-- Floating Bottom Bar -->
    <div class="bottom-bar">
      <div class="selection-summary">
        <span v-if="selectedRoom">선택한 객실: {{ selectedRoom.name }}</span>
        <span v-else>객실을 선택해주세요</span>
        <div class="total-price" v-if="selectedRoom">₩{{ formatPrice(selectedRoom.price) }}</div>
      </div>
      <button class="book-btn" :disabled="!canBook" @click="goToBooking">예약하기</button>
    </div>
    <div v-if="selectedRoom && !canBook" class="booking-hint">
      날짜를 선택해주세요.
      <button type="button" class="booking-hint-btn" @click="openCalendarFromHint">
        날짜 선택하기
      </button>
    </div>

  </div>
</template>

<style scoped>
.room-detail {
  padding-bottom: 2rem;
  max-width: 1200px;
}

.detail-header {
  padding: 1rem 0;
  margin-bottom: 0.5rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1rem;
  color: #333;
  cursor: pointer;
  padding: 0.5rem 0;
}

.back-btn:hover {
  color: var(--primary);
}

/* Sections */
.section {
  padding: 1.5rem 0;
}
.info-section {
  padding-top: 0;
}
hr {
  border: 0;
  border-top: 1px solid #eee;
  margin: 0;
}
h1 { font-size: 1.8rem; margin-bottom: 0.5rem; }
h2 { font-size: 1.4rem; margin-bottom: 1rem; }
h3 { font-size: 1.1rem; margin-bottom: 0.5rem; }

/* Info */
.meta {
  color: var(--text-sub);
  margin-bottom: 1rem;
  font-size: 0.95rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.description-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
}
.description {
  line-height: 1.6;
  flex: 1;
  margin: 0;
}
.description-clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.more-btn {
  margin-top: 0.15rem;
  align-self: flex-end;
  background: #BFE7DF;
  border: 1px solid #8FCFC1;
  color: #0f4c44;
  font-weight: 700;
  cursor: pointer;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  white-space: nowrap;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0;
  color: var(--text-sub);
  font-size: 0.9rem;
}
.meta-item {
  background: #f3f4f6;
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
}
.transport-info {
  margin-top: 1rem;
}
.transport-info h3 {
  margin-bottom: 0.4rem;
}
.transport-info p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.5;
}
.info-title {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.info-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  color: #0f4c44;
}
.info-icon svg {
  width: 18px;
  height: 18px;
  display: block;
  fill: currentColor;
}
.contact-info {
  margin-top: 1rem;
}
.contact-info h3 {
  margin-bottom: 0.4rem;
}
.contact-info p {
  margin: 0;
  color: var(--text-sub);
  line-height: 1.5;
}

/* Host */
.host-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.host-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

/* Amenities */
.amenity-section h2 {
  margin-bottom: 0.6rem;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.tag {
  background: #f3f4f6;
  color: var(--text-main);
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.85rem;
}
.tag.empty {
  color: var(--text-sub);
}
.amenity-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
.amenity-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  color: var(--text-main);
  flex-shrink: 0;
}
.amenity-text {
  line-height: 1;
}
:deep(.amenity-icon svg) {
  width: 18px;
  height: 18px;
  display: block;
}

/* Extra info */
.extra-info-section h2 {
  margin-bottom: 0.8rem;
}
.info-list {
  display: grid;
  gap: 0.75rem;
  margin: 0;
}
.info-row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 0.75rem;
  align-items: start;
}
.info-row dt {
  font-weight: 600;
  color: var(--text-main);
}
.info-row dd {
  margin: 0;
  color: var(--text-sub);
  word-break: break-word;
}
.sns-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.75rem;
  border-radius: 999px;
  background: #f3f4f6;
  color: var(--text-main);
  text-decoration: none;
  margin-right: 0.6rem;
  transition: background 0.2s ease;
}
.sns-link:last-child {
  margin-right: 0;
}
.sns-link:hover {
  background: #e5e7eb;
}
.sns-icon {
  width: 18px;
  height: 18px;
}
.sns-text {
  font-size: 0.85rem;
  color: var(--text-main);
  white-space: nowrap;
}

/* Room Selection */
.picker-box {
  border: 1px solid #ddd;
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}
.picker-row {
  display: flex;
  gap: 1rem;
}
.picker-field { flex: 1; }
.picker-field label { display: block; font-size: 0.8rem; font-weight: bold; margin-bottom: 0.5rem; }
.date-display, .guest-control {
  border: 1px solid #ddd;
  padding: 0 0.8rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  background: #fff;
}
.date-picker-wrapper {
  position: relative;
}
.date-display {
  width: 100%;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.guest-control button {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
}

.calendar-popup {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 50;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
  padding: 1rem;
  width: 100%;
}
.calendar-container {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}
.calendar-month {
  width: 220px;
}
.calendar-month-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
}
.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  font-size: 0.75rem;
  color: var(--text-sub);
  margin-bottom: 0.25rem;
}
.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.calendar-day {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  cursor: pointer;
  font-size: 0.8rem;
}
.calendar-day.empty {
  visibility: hidden;
  cursor: default;
}
.calendar-day.today {
  border: 1px solid var(--primary);
}
.calendar-day.in-range {
  background: #e6f4f1;
}
.calendar-day.range-start,
.calendar-day.range-end {
  background: var(--primary);
  color: #000;
  font-weight: 600;
}
.calendar-nav-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.4rem;
  padding: 0.25rem 0.5rem;
  color: var(--text-main);
}

/* Room Card */
.room-card {
  border: 2px solid #ddd;
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 1rem;
  display: flex;
  gap: 1.5rem;
  align-items: stretch;
  cursor: pointer;
  transition: border-color 0.2s;
}
.room-card:hover { border-color: var(--primary); }
.room-card.selected { border-color: var(--primary); background-color: #f9fdfc; }
.room-media {
  flex: 1 1 0;
  max-width: calc(50% - 0.75rem);
  border-radius: 10px;
  overflow: hidden;
  background: #e5e7eb;
}
.room-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  /* 이미지 축소 시 품질 개선 */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: smooth;
  transform: translateZ(0);
}
.room-content {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  flex: 1 1 0;
  max-width: calc(50% - 0.75rem);
}
.room-info h3 { margin-bottom: 0.3rem; }
.room-info p { color: var(--text-sub); font-size: 0.9rem; margin-bottom: 0.5rem; }
.capacity { font-size: 0.8rem; background: #eee; padding: 2px 6px; border-radius: 4px; }
.room-action { text-align: right; display: flex; flex-direction: column; justify-content: space-between; }
.price { font-weight: bold; font-size: 1.1rem; }
.select-btn {
  padding: 0.5rem 1rem;
  background: #eee;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  min-width: 72px;
  text-align: center;
}
.select-btn.active {
  background: var(--primary);
  color: #000;
}
.select-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

/* Room unavailable state */
.room-card.unavailable {
  opacity: 0.6;
  cursor: not-allowed;
}
.room-card.unavailable:hover {
  border-color: #ddd;
}
.room-media {
  position: relative;
}
.room-unavailable-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #ef4444;
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}
.room-toggle-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}
.room-toggle-btn {
  background: #BFE7DF;
  border: 1px solid #8FCFC1;
  color: #0f4c44;
  font-weight: 700;
  cursor: pointer;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  white-space: nowrap;
}

/* Rules */
.rules-section {
  padding-top: 0;
  padding-bottom: 0;
}
.rule-box h3 { margin-bottom: 0.8rem; }
.rule-box ul { list-style: inside disc; color: var(--text-sub); font-size: 0.9rem; line-height: 1.6; padding-left: 0.75rem; }

/* Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1px solid #ddd;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  z-index: 100;
}
.selection-summary { display: flex; flex-direction: column; }
.total-price { font-weight: bold; font-size: 1.2rem; }
.book-btn {
  background: var(--primary);
  color: #004d40;
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-weight: bold;
  font-size: 1rem;
  border: none;
  cursor: pointer;
  min-width: 140px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.book-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 1;
}
.booking-hint {
  position: fixed;
  bottom: 84px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  padding: 0.6rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  z-index: 101;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.booking-hint-btn {
  background: var(--primary);
  color: #004d40;
  border: none;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 768px) {
  .room-detail {
    max-width: 100%;
    padding: 0 16px;
  }
  
  .picker-row {
    flex-direction: column;
  }

  .calendar-container {
    flex-direction: column;
  }

  .calendar-month {
    width: 100%;
  }
  .room-card {
    flex-direction: column;
    padding: 1rem;
    gap: 0.75rem;
  }

  .room-media {
    width: 100%;
    height: 180px;
    max-width: 100%;
    flex: none;
  }

  .room-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
    max-width: 100%;
  }

  .room-info h3 {
    margin: 0;
  }

  .room-info p {
    margin: 0;
    line-height: 1.25;
  }

  .capacity {
    padding: 1px 5px;
    margin-top: 0.15rem;
  }

  .room-action {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    margin-top: 0.15rem;
  }
  .booking-hint {
    width: calc(100% - 32px);
    left: 16px;
    transform: none;
    justify-content: space-between;
  }
}
</style>
