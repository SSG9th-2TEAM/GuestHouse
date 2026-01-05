import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useSearchStore = defineStore('search', () => {
    // Date state
    const startDate = ref(null)
    const endDate = ref(null)

    // Guest state
    const guestCount = ref(0)

    // Price/theme filter state
    const minPrice = ref(null)
    const maxPrice = ref(null)
    const themeIds = ref([])

    // Keyword state (location or guesthouse name)
    const keyword = ref('')

    // Computed display texts
    const dateDisplayText = computed(() => {
        if (!startDate.value) return '날짜 선택'
        if (!endDate.value) {
            return `${startDate.value.getMonth() + 1}월 ${startDate.value.getDate()}일`
        }
        const startMonth = startDate.value.getMonth() + 1
        const startDay = startDate.value.getDate()
        const endMonth = endDate.value.getMonth() + 1
        const endDay = endDate.value.getDate()
        return `${startMonth}/${startDay} ~ ${endMonth}/${endDay}`
    })

    const guestDisplayText = computed(() => {
        if (guestCount.value === 0) return '게스트 추가'
        return `게스트 ${guestCount.value}명`
    })

    // Format for detail view
    const checkInOutText = computed(() => {
        if (!startDate.value || !endDate.value) return '날짜를 선택하세요'
        const formatDate = (date) => {
            const year = date.getFullYear()
            const month = String(date.getMonth() + 1).padStart(2, '0')
            const day = String(date.getDate()).padStart(2, '0')
            return `${year}-${month}-${day}`
        }
        return `${formatDate(startDate.value)} - ${formatDate(endDate.value)}`
    })

    // Actions
    const setStartDate = (date) => {
        startDate.value = date
    }

    const setEndDate = (date) => {
        endDate.value = date
    }

    const setGuestCount = (count) => {
        guestCount.value = count
    }

    const increaseGuest = () => {
        guestCount.value++
    }

    const decreaseGuest = () => {
        if (guestCount.value > 0) {
            guestCount.value--
        }
    }

    const resetDates = () => {
        startDate.value = null
        endDate.value = null
    }

    const setKeyword = (value) => {
        if (value === undefined || value === null) {
            keyword.value = ''
            return
        }
        keyword.value = String(value)
    }

    const setPriceRange = (min, max) => {
        minPrice.value = Number.isFinite(min) ? min : null
        maxPrice.value = Number.isFinite(max) ? max : null
    }

    const setThemeIds = (ids) => {
        themeIds.value = Array.isArray(ids) ? [...ids] : []
    }

    const resetFilters = () => {
        minPrice.value = null
        maxPrice.value = null
        themeIds.value = []
    }

    return {
        startDate,
        endDate,
        guestCount,
        minPrice,
        maxPrice,
        themeIds,
        keyword,
        dateDisplayText,
        guestDisplayText,
        checkInOutText,
        setStartDate,
        setEndDate,
        setGuestCount,
        increaseGuest,
        decreaseGuest,
        resetDates,
        setKeyword,
        setPriceRange,
        setThemeIds,
        resetFilters
    }
})
