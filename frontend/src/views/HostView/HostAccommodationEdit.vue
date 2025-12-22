<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const accommodationId = route.params.id

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 로딩 상태
const isLoading = ref(true)
const loadError = ref('')

// 모달 상태
const showModal = ref(false)
const modalMessage = ref('')
const updateSuccess = ref(false)

const openModal = (message) => {
  modalMessage.value = message
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (updateSuccess.value) {
    router.push('/host/accommodation')
  }
}

// 카카오맵 관련
const mapContainer = ref(null)
const map = ref(null)
const marker = ref(null)
const geocoder = ref(null)

// 숙소 유형 매핑
const accommodationCategoryMap = {
  'GUESTHOUSE': '게스트하우스',
  'PENSION': '펜션',
  'HOTEL': '호텔',
  'MOTEL': '모텔',
  'RESORT': '리조트',
  'HANOK': '한옥',
  'CAMPING': '캠핑/글램핑'
}

// 역매핑 (한글 -> 영문)
const accommodationTypeReverseMap = {
  '게스트하우스': 'GUESTHOUSE',
  '펜션': 'PENSION',
  '호텔': 'HOTEL',
  '모텔': 'MOTEL',
  '리조트': 'RESORT',
  '한옥': 'HANOK',
  '캠핑/글램핑': 'CAMPING'
}

// 편의시설 옵션 (전체 목록)
const amenityOptions = [
  { id: 1, label: '무선 인터넷' },
  { id: 2, label: '에어컨' },
  { id: 3, label: '난방' },
  { id: 4, label: 'TV' }
]

// 테마 옵션
const themeOptions = {
  activity: {
    label: '액티비티',
    items: ['불멍', '포틀럭', '러닝', '서핑']
  },
  location: {
    label: '위치 특성',
    items: ['바닷가', '공항 주변', '노을 맛집(노을 명소)']
  },
  experience: {
    label: '특별한 경험',
    items: ['여성 전용', '1인실', '독서', '스냅 촬영']
  },
  meal: {
    label: '식사',
    items: ['조식', '오마카세']
  }
}

// 테마 ID 매핑
const themeIdMap = {
  '불멍': 1, '포틀럭': 2, '러닝': 3, '서핑': 4,
  '바닷가': 5, '공항 주변': 6, '노을 맛집(노을 명소)': 7,
  '여성 전용': 8, '1인실': 9, '독서': 10, '스냅 촬영': 11,
  '조식': 12, '오마카세': 13
}

// Form data
const form = ref({
  // 기본정보 (Readonly)
  name: '',
  type: '',
  phone: '',
  email: '',
  businessRegistrationNumber: '',
  // 위치정보 (Readonly)
  city: '',
  district: '',
  township: '',
  address: '',
  latitude: null,
  longitude: null,
  // 수정 가능 필드
  description: '',
  shortDescription: '',
  transportInfo: '',
  checkInTime: '',
  checkOutTime: '',
  parkingInfo: '',
  sns: '',
  isActive: true,
  houseRules: '', // DB 스키마에 없으면 생략 가능하지만 UI엔 있었음
  // Readonly Lists
  amenities: [], // IDs
  themes: [], // Strings (Names) or IDs depending on logic. Register uses Strings for themes.
  // Images
  bannerImage: null,
  detailImages: [],
  // Bank (Readonly for now as per "accommodation info readonly" request, though usually bank is crucial)
  bankName: '',
  accountHolder: '',
  accountNumber: ''
})

// 객실 데이터
const rooms = ref([])

// 편의시설 토글
const toggleAmenity = (id) => {
  const index = form.value.amenities.indexOf(id)
  if (index === -1) {
    form.value.amenities.push(id)
  } else {
    form.value.amenities.splice(index, 1)
  }
}

// 테마 토글
const toggleTheme = (themeName) => {
  const index = form.value.themes.indexOf(themeName)
  if (index === -1) {
    if (form.value.themes.length >= 6) {
       openModal('테마는 최대 6개까지 선택 가능합니다.')
       return
    }
    form.value.themes.push(themeName)
  } else {
    form.value.themes.splice(index, 1)
  }
}

// 편의시설 체크 여부 확인
const isAmenityChecked = (id) => {
  return form.value.amenities.includes(id)
}

// 테마 체크 여부 확인
const isThemeChecked = (themeName) => {
  return form.value.themes.includes(themeName)
}

// 데이터 로드
const loadAccommodation = async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    const response = await fetch(`${API_BASE_URL}/accommodations/${accommodationId}`)
    if (!response.ok) throw new Error('숙소 정보를 불러올 수 없습니다.')

    const data = await response.json()

    // 데이터 매핑
    form.value = {
      name: data.accommodationsName,
      type: accommodationCategoryMap[data.accommodationsCategory] || data.accommodationsCategory,
      phone: data.phone,
      email: data.email || '', // API response might not have email on root? Checked DTO, it does not have email. Using placeholder if missing.
      businessRegistrationNumber: data.businessRegistrationNumber,
      
      city: data.city,
      district: data.district,
      township: data.township,
      address: data.addressDetail,
      latitude: data.latitude,
      longitude: data.longitude,
      
      description: data.accommodationsDescription,
      shortDescription: data.shortDescription,
      transportInfo: data.transportInfo,
      parkingInfo: data.parkingInfo,
      sns: data.sns,
      checkInTime: data.checkInTime,
      checkOutTime: data.checkOutTime,
      isActive: data.accommodationStatus === 1,
      
      amenities: data.amenityIds || [], // IDs
      themes: data.themeIds ? data.themeIds.map(id => {
         // Map ID to Name for UI matching Register page string logic
         return Object.keys(themeIdMap).find(key => themeIdMap[key] === id)
      }).filter(Boolean) : [],
      
      bankName: data.bankName,
      accountNumber: data.accountNumber,
      accountHolder: data.accountHolder,
      
      // Images (Just URLs for display)
      bannerImage: null, // Logic to extract banner from images list
      detailImages: []
    }

    // Images Mapping
    if (data.images) {
        const banner = data.images.find(img => img.imageType === 'banner')
        if (banner) form.value.bannerImage = banner.imageUrl
        
        form.value.detailImages = data.images
            .filter(img => img.imageType === 'detail')
            .map(img => img.imageUrl)
    }

    // 객실 매핑
    if (data.rooms) {
      rooms.value = data.rooms.map(room => ({
        id: room.roomId, // Keep ID for updates
        name: room.roomName,
        weekdayPrice: room.price,
        weekendPrice: room.weekendPrice,
        minGuests: room.minGuests,
        maxGuests: room.maxGuests,
        bedCount: room.bedCount,
        bathroomCount: room.bathroomCount,
        description: room.roomDescription,
        mainImageUrl: room.mainImageUrl,
        amenities: room.amenities || [], // String list
        isActive: room.roomStatus === 1
      }))
    }

    await nextTick()
    initMap()

  } catch (error) {
    console.error('Load Error:', error)
    loadError.value = error.message
  } finally {
    isLoading.value = false
  }
}

// 카카오맵
const initMap = () => {
  if (!window.kakao || !window.kakao.maps || !mapContainer.value) return 

  const coords = new window.kakao.maps.LatLng(form.value.latitude, form.value.longitude)
  const options = { center: coords, level: 3 }
  
  map.value = new window.kakao.maps.Map(mapContainer.value, options)
  marker.value = new window.kakao.maps.Marker({
    position: coords,
    map: map.value
  })
}

// ========== 유효성 검사 (수정 가능 필드만) ==========
const errors = ref({})

const validateForm = () => {
    errors.value = {}
    let isValid = true
    const errorMessages = []

    if (!form.value.description?.trim()) {
        errors.value.description = '숙소 소개를 입력해주세요.'
        errorMessages.push('숙소 소개')
        isValid = false
    }
    
    if (!form.value.checkInTime) {
        errors.value.checkInTime = '체크인 시간을 입력해주세요.'
        errorMessages.push('체크인 시간')
        isValid = false
    }

    if (!form.value.checkOutTime) {
        errors.value.checkOutTime = '체크아웃 시간을 입력해주세요.'
        errorMessages.push('체크아웃 시간')
        isValid = false
    }

    // Room Validation at Submit
    if (rooms.value.length === 0) {
        errors.value.rooms = '등록된 객실이 없습니다.'
        errorMessages.push('객실')
        isValid = false
    }

    return { isValid, errorMessages }
}

const handleUpdate = async () => {
    // 객실 추가/수정 중인지 확인
    if (showRoomForm.value) {
        openModal('작성 중인 객실 정보를 먼저 저장(등록/수정)해주세요.')
        return
    }

    const { isValid, errorMessages } = validateForm()
    if (!isValid) {
        openModal(`다음 항목을 확인해주세요:\n${errorMessages.join(', ')}`)
        return
    }

    try {
        // 객실 데이터 준비 (이미지는 Base64로 변환되거나 기존 URL 유지)
        // Note: For existing rooms, if image is URL, it means no change. If File object, need upload/base64.
        // Simplified approach: Re-uploading everything or handling separation is complex. 
        // Assuming Backend handles mixed URL/Base64 or we convert new files only.
        
        const roomsData = await Promise.all(rooms.value.map(async (room) => {
            let imagePayload = room.mainImageUrl
            if (room.representativeImage instanceof File) {
                imagePayload = await fileToBase64(room.representativeImage)
            }
            
            return {
                roomId: room.id, // ID exists for update
                roomName: room.name,
                price: parseInt(room.weekdayPrice),
                weekendPrice: parseInt(room.weekendPrice),
                minGuests: parseInt(room.minGuests),
                maxGuests: parseInt(room.maxGuests),
                roomDescription: room.description || '',
                mainImageUrl: imagePayload,
                bathroomCount: parseInt(room.bathroomCount),
                roomType: 'STANDARD',
                bedCount: parseInt(room.bedCount),
                roomStatus: room.isActive ? 1 : 0
            }
        }))

        const requestData = {
            accommodationsDescription: form.value.description,
            shortDescription: form.value.shortDescription || '',
            transportInfo: form.value.transportInfo || '',
            accommodationStatus: form.value.isActive ? 1 : 0,
            parkingInfo: form.value.parkingInfo || '',
            sns: form.value.sns || '',
            phone: form.value.phone, // Readonly but sent back
            checkInTime: form.value.checkInTime,
            checkOutTime: form.value.checkOutTime,
            checkInTime: form.value.checkInTime,
            checkOutTime: form.value.checkOutTime,
            rooms: roomsData,
            // Amenities: form.amenities stores IDs already
            amenityIds: form.value.amenities,
            // Themes: form.themes stores Names, convert to IDs
            themeIds: form.value.themes.map(name => themeIdMap[name]).filter(id => id !== undefined)
        }

        const response = await fetch(`${API_BASE_URL}/accommodations/${accommodationId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        })

        if (response.ok) {
            updateSuccess.value = true
            openModal('숙소 정보가 수정되었습니다.')
        } else {
            openModal('수정에 실패했습니다.')
        }
    } catch (e) {
        console.error(e)
        openModal('오류가 발생했습니다.')
    }
}

// ========== 객실 관리 ==========
const showRoomForm = ref(false)
const roomForm = ref({
  name: '',
  weekdayPrice: '',
  weekendPrice: '',
  minGuests: '',
  maxGuests: '',
  bedCount: '',
  bathroomCount: '',
  description: '',
  amenities: [],
  representativeImage: null,
  representativeImagePreview: '',
  isActive: true
})
const roomErrors = ref({})
const editingRoomId = ref(null)

// 숫자 필터링 (한글 등 방지)
const filterNumberInput = (event) => {
  const value = event.target.value
  // 숫자가 아닌 문자가 포함되어 있다면 제거
  if (/[^0-9]/.test(value)) {
     event.target.value = value.replace(/[^0-9]/g, '')
     // v-model 업데이트 트리거
     event.target.dispatchEvent(new Event('input'))
  }
}

const validateRoomForm = () => {
    roomErrors.value = {}
    let isValid = true

    if (!roomForm.value.name?.trim()) {
        roomErrors.value.name = '객실명을 입력해주세요.'
        isValid = false
    }

    // 숫자 유효성 검사 (Regex for non-digits)
    const isNumeric = (val) => /^\d+$/.test(val)

    if (!roomForm.value.weekdayPrice || !isNumeric(roomForm.value.weekdayPrice) || parseInt(roomForm.value.weekdayPrice) <= 0) {
        roomErrors.value.weekdayPrice = '올바른 숫자를 입력해주세요.'
        isValid = false
    }
    if (!roomForm.value.weekendPrice || !isNumeric(roomForm.value.weekendPrice) || parseInt(roomForm.value.weekendPrice) <= 0) {
        roomErrors.value.weekendPrice = '올바른 숫자를 입력해주세요.'
        isValid = false
    }
    if (!roomForm.value.minGuests || !isNumeric(roomForm.value.minGuests) || parseInt(roomForm.value.minGuests) < 1) {
        roomErrors.value.minGuests = '1명 이상 입력해주세요.'
        isValid = false
    }
    if (!roomForm.value.maxGuests || !isNumeric(roomForm.value.maxGuests) || parseInt(roomForm.value.maxGuests) < 1) {
        roomErrors.value.maxGuests = '1명 이상 입력해주세요.'
        isValid = false
    }
    if (parseInt(roomForm.value.minGuests) > parseInt(roomForm.value.maxGuests)) {
        roomErrors.value.maxGuests = '최대 인원은 최소 인원보다 커야 합니다.'
        isValid = false
    }
    if (roomForm.value.bedCount && !isNumeric(roomForm.value.bedCount)) {
        roomErrors.value.bedCount = '숫자만 입력해주세요.'
        isValid = false
    }
     if (roomForm.value.bathroomCount && !isNumeric(roomForm.value.bathroomCount)) {
        roomErrors.value.bathroomCount = '숫자만 입력해주세요.'
        isValid = false
    }

    if (!roomForm.value.representativeImage && !roomForm.value.representativeImagePreview) {
        roomErrors.value.representativeImage = '이미지를 등록해주세요.'
        isValid = false
    }

    return isValid
}

const handleRoomImageUpload = (event) => {
    const file = event.target.files[0]
    if (file) {
        roomForm.value.representativeImage = file
        roomForm.value.representativeImagePreview = URL.createObjectURL(file)
    }
}
const removeRoomImage = () => {
    roomForm.value.representativeImage = null
    roomForm.value.representativeImagePreview = ''
}

// 객실 추가/수정
const saveRoom = () => {
    if (!validateRoomForm()) {
        const errors = Object.values(roomErrors.value).join('\n')
        openModal(`객실 정보를 확인해주세요.\n${errors}`)
        return
    }

    const roomData = {
        id: editingRoomId.value || Date.now(),
        ...roomForm.value,
        weekdayPrice: parseInt(roomForm.value.weekdayPrice),
        weekendPrice: parseInt(roomForm.value.weekendPrice),
        minGuests: parseInt(roomForm.value.minGuests),
        maxGuests: parseInt(roomForm.value.maxGuests),
        bedCount: parseInt(roomForm.value.bedCount) || 0,
        bathroomCount: parseInt(roomForm.value.bathroomCount) || 0,
        amenities: [...roomForm.value.amenities],
        // Image handled implicitly
    }

    if (editingRoomId.value) {
        const index = rooms.value.findIndex(r => r.id === editingRoomId.value)
        if (index !== -1) rooms.value[index] = roomData
    } else {
        rooms.value.push(roomData)
    }

    showRoomForm.value = false
    resetRoomForm()
    openModal(editingRoomId.value ? '객실이 수정되었습니다.' : '객실이 추가되었습니다.')
    editingRoomId.value = null
}

const editRoom = (room) => {
    editingRoomId.value = room.id
    roomForm.value = { ...room }
    // Ensure image preview is set if it's a URL
    if (typeof room.mainImageUrl === 'string' && room.mainImageUrl.startsWith('http')) {
         roomForm.value.representativeImagePreview = room.mainImageUrl
    } else if (room.representativeImagePreview) {
         roomForm.value.representativeImagePreview = room.representativeImagePreview
    }
    showRoomForm.value = true
}

const deleteRoom = (id) => {
    if(confirm('정말 삭제하시겠습니까?')) {
        rooms.value = rooms.value.filter(r => r.id !== id)
    }
}

const resetRoomForm = () => {
    roomForm.value = {
        name: '', weekdayPrice: '', weekendPrice: '', minGuests: '', maxGuests: '',
        bedCount: '', bathroomCount: '', description: '', amenities: [],
        representativeImage: null, representativeImagePreview: '', isActive: true
    }
    roomErrors.value = {}
}

// Base64 Util
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

// Room Amenity Options
const roomAmenityOptions = {
    bathroom: { label: '욕실', items: ['비누', '샤워', '개인 욕실'] },
    bedroom: { label: '침실', items: ['간이/추가 침대 제공', '에어컨', '난방'] },
    dining: { label: '식사 및 음료', items: ['공용 주방 이용', '전용 주방'] },
    etc: { label: '기타', items: ['무료 WiFi', '금고', '다리미'] }
}
const toggleRoomAmenity = (item) => {
    const idx = roomForm.value.amenities.indexOf(item)
    if (idx > -1) roomForm.value.amenities.splice(idx, 1)
    else roomForm.value.amenities.push(item)
}

onMounted(() => {
  loadAccommodation()
})
</script>

<template>
  <div class="register-page" :class="{ loading: isLoading }">
    <div v-if="isLoading" class="loading-spinner">Loading...</div>

    <!-- Page Header -->
    <div v-else class="page-header">
      <div class="header-top">
        <div class="title-area">
          <h1>숙소 정보 수정</h1>
        </div>
      </div>
      
      <!-- Toggle & Actions -->
      <div class="header-controls">
        <div class="toggle-wrapper">
          <span class="toggle-label">숙소 운영</span>
          <div class="toggle-switch" :class="{ active: form.isActive }" @click="form.isActive = !form.isActive">
            <div class="toggle-slider"></div>
          </div>
        </div>
        
        <div class="action-buttons">
          <button class="btn-outline" @click="$router.push('/host/accommodation')">취소</button>
          <button class="btn-primary" @click="handleUpdate">수정 완료</button>
        </div>
      </div>
    </div>

    <!-- ========== Form Content ========== -->
    <div v-if="!isLoading" class="form-content">
      <!-- Section: 기본정보 (READONLY) -->
      <section class="form-section">
        <h3 class="subsection-title">기본정보 (수정 불가)</h3>
        
        <div class="form-group">
          <label>숙소명</label>
          <input type="text" v-model="form.name" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>숙소유형</label>
          <input type="text" v-model="form.type" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>대표 연락처</label>
          <input type="text" v-model="form.phone" readonly class="readonly-input" />
        </div>

        <div class="form-group">
          <label>사업자등록번호</label>
          <input type="text" v-model="form.businessRegistrationNumber" readonly class="readonly-input" />
        </div>
      </section>

      <!-- Section: 수정 가능 정보 -->
      <section class="form-section">
        <h3 class="subsection-title update-title">숙소 상세 정보 (수정 가능)</h3>
        
        <div class="form-group">
          <label>숙소 소개(상세설명) <span class="required">*</span></label>
          <textarea
            v-model="form.description"
            rows="5"
            :class="{ 'input-error': errors.description }"
          ></textarea>
        </div>

        <div class="form-group">
           <label>한 줄 설명</label>
           <input type="text" v-model="form.shortDescription" />
        </div>

        <div class="form-group">
           <label>SNS</label>
           <input type="text" v-model="form.sns" placeholder="@instagram_id" />
        </div>
      </section>

      <!-- Section: 위치 정보 (READONLY) -->
      <section class="form-section">
        <h3 class="subsection-title">위치 정보 (수정 불가)</h3>

        <div class="form-group">
          <label>주소</label>
          <input type="text" :value="`${form.city} ${form.district} ${form.township} ${form.address}`" readonly class="readonly-input" />
        </div>

        <div class="form-group">
            <div ref="mapContainer" class="kakao-map"></div>
        </div>
      </section>

      <!-- Section: 교통 및 주차 (EDITABLE) -->
      <section class="form-section">
        <h3 class="subsection-title">교통 및 주차 정보 (수정 가능)</h3>
        
        <div class="form-group">
          <label>주변 교통정보</label>
          <textarea v-model="form.transportInfo" rows="3" placeholder="예: 강남역 1번 출구 도보 5분"></textarea>
        </div>

        <div class="form-group">
          <label>주차정보</label>
          <textarea v-model="form.parkingInfo" rows="3" placeholder="예: 건물 내 무료 주차 가능"></textarea>
        </div>
      </section>

      <!-- Section: 운영 정책 (EDITABLE) -->
      <section class="form-section">
        <h3 class="subsection-title">체크인/아웃 정보 (수정 가능)</h3>
        
        <div class="form-group">
          <label>체크인 시간 <span class="required">*</span></label>
          <div class="time-input">
            <input v-model="form.checkInTime" type="time" />
          </div>
        </div>
        
        <div class="form-group">
          <label>체크아웃 시간 <span class="required">*</span></label>
          <div class="time-input">
            <input v-model="form.checkOutTime" type="time" />
          </div>
        </div>
      </section>

      <!-- Section: 편의 시설 & 테마 (수정 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">편의 시설 & 테마 (수정 가능)</h3>
        
        <div class="amenities-grid">
          <label 
            v-for="amenity in amenityOptions" 
            :key="amenity.id" 
            class="amenity-checkbox"
            :class="{ checked: isAmenityChecked(amenity.id) }"
          >
            <input 
              type="checkbox" 
              :checked="isAmenityChecked(amenity.id)" 
              @change="toggleAmenity(amenity.id)"
            />
            <span class="checkmark"></span>
            <span class="amenity-label">{{ amenity.label }}</span>
          </label>
        </div>
        
        <div class="theme-selection mt-4">
             <h4 class="subsection-title mt-4">테마 (최대 6개)</h4>
             <div v-for="(category, key) in themeOptions" :key="key" class="theme-category">
               <div class="theme-category-title">{{ category.label }}</div>
               <div class="theme-tags">
                 <label 
                   v-for="item in category.items" 
                   :key="item" 
                   class="theme-tag"
                   :class="{ selected: isThemeChecked(item) }"
                 >
                   <input 
                     type="checkbox" 
                     :checked="isThemeChecked(item)"
                     @change="toggleTheme(item)"
                   />
                   {{ item }}
                 </label>
               </div>
             </div>
        </div>
      </section>
      
      <!-- Section: 이미지 (READONLY - DISPLAY ONLY) -->
      <section class="form-section">
         <h3 class="subsection-title">이미지 (수정 불가)</h3>
         <div v-if="form.bannerImage" class="existing-image mb-4">
            <label>배너 이미지</label>
            <img :src="form.bannerImage" class="banner-preview" />
         </div>
         <div v-if="form.detailImages.length > 0">
            <label>상세 이미지</label>
            <div class="detail-images-preview">
                <div v-for="(img, idx) in form.detailImages" :key="idx" class="detail-image-item">
                    <img :src="img" />
                </div>
            </div>
         </div>
      </section>

      <!-- Section: 정산 계좌 (READONLY) -->
      <section class="form-section">
         <h3 class="subsection-title">정산 계좌 (수정 불가)</h3>
         <div class="form-group">
            <label>은행</label>
            <input type="text" v-model="form.bankName" readonly class="readonly-input" />
         </div>
         <div class="form-group">
            <label>계좌번호</label>
            <input type="text" v-model="form.accountNumber" readonly class="readonly-input" />
         </div>
         <div class="form-group">
            <label>예금주</label>
            <input type="text" v-model="form.accountHolder" readonly class="readonly-input" />
         </div>
      </section>

      <!-- Section: 객실 관리 (FULLY EDITABLE) -->
      <section class="form-section">
        <h3 class="subsection-title" style="color: #BFE7DF;">객실 관리 (수정 가능)</h3>
        
        <!-- Room List -->
        <div v-if="rooms.length > 0" class="room-list">
          <div v-for="room in rooms" :key="room.id" class="room-card">
            <div class="room-info">
              <h4 class="room-name">{{ room.name }}</h4>
              <div class="room-details">
                <span class="detail-label">평일: {{ Number(room.weekdayPrice).toLocaleString() }}원</span> |
                <span class="detail-label">주말: {{ Number(room.weekendPrice).toLocaleString() }}원</span> |
                <span class="detail-label">{{ room.minGuests }}~{{ room.maxGuests }}명</span>
              </div>
              <div class="room-toggle">
                 <span>{{ room.isActive ? '운영중' : '운영중지' }}</span>
              </div>
            </div>
            <div class="room-actions">
              <button class="room-btn" @click="editRoom(room)">수정</button>
              <button class="room-btn" @click="deleteRoom(room.id)">삭제</button>
            </div>
          </div>
        </div>
        
        <p v-else class="no-rooms">등록된 객실이 없습니다.</p>
        
        <button class="add-room-btn" @click="showRoomForm = true; resetRoomForm()" v-if="!showRoomForm">
          + 객실 추가하기
        </button>
        
        <!-- Room Form (Edit/Add) -->
        <div v-if="showRoomForm" class="room-form">
          <h4 class="room-form-title">{{ editingRoomId ? '객실 수정' : '새 객실 등록' }}</h4>

          <div class="form-group">
            <label>객실명 <span class="required">*</span></label>
            <input v-model="roomForm.name" type="text" :class="{ 'input-error': roomErrors.name }" />
            <span v-if="roomErrors.name" class="error-message">{{ roomErrors.name }}</span>
          </div>

          <div class="form-group">
             <label>객실 대표 이미지 <span class="required">*</span></label>
             <div class="room-image-upload-area" :class="{ 'upload-error': roomErrors.representativeImage }">
               <div v-if="roomForm.representativeImagePreview" class="room-image-preview">
                 <img :src="roomForm.representativeImagePreview" />
                 <button class="room-remove-image-btn" @click="removeRoomImage">✕</button>
               </div>
               <label v-else class="room-upload-box">
                 <input type="file" @change="handleRoomImageUpload" accept="image/*" class="hidden-file-input" />
                 <span>이미지 업로드</span>
               </label>
             </div>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>주중 요금 <span class="required">*</span></label>
              <input v-model="roomForm.weekdayPrice" type="number" @input="filterNumberInput" :class="{ 'input-error': roomErrors.weekdayPrice }" />
              <span v-if="roomErrors.weekdayPrice" class="error-message">{{ roomErrors.weekdayPrice }}</span>
            </div>
            <div class="form-group">
              <label>주말 요금 <span class="required">*</span></label>
              <input v-model="roomForm.weekendPrice" type="number" @input="filterNumberInput" :class="{ 'input-error': roomErrors.weekendPrice }" />
               <span v-if="roomErrors.weekendPrice" class="error-message">{{ roomErrors.weekendPrice }}</span>
            </div>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>최소 인원</label>
              <input v-model="roomForm.minGuests" type="number" @input="filterNumberInput" />
               <span v-if="roomErrors.minGuests" class="error-message">{{ roomErrors.minGuests }}</span>
            </div>
            <div class="form-group">
              <label>최대 인원</label>
              <input v-model="roomForm.maxGuests" type="number" @input="filterNumberInput" />
               <span v-if="roomErrors.maxGuests" class="error-message">{{ roomErrors.maxGuests }}</span>
            </div>
          </div>
          
           <div class="form-row two-col">
            <div class="form-group">
              <label>침대 개수</label>
              <input v-model="roomForm.bedCount" type="number" @input="filterNumberInput" />
            </div>
            <div class="form-group">
              <label>욕실 개수</label>
              <input v-model="roomForm.bathroomCount" type="number" @input="filterNumberInput" />
            </div>
          </div>

          <div class="form-group">
            <label>객실 설명</label>
            <textarea v-model="roomForm.description" rows="3"></textarea>
          </div>

          <!-- Room Amenities -->
          <div class="room-amenities-section">
             <h4 class="room-amenities-title">객실 편의시설</h4>
             <div v-for="(cat, key) in roomAmenityOptions" :key="key" class="room-amenity-category">
                <div class="room-amenity-label">{{ cat.label }}</div>
                <div class="room-amenity-tags">
                   <label v-for="item in cat.items" :key="item" class="room-amenity-tag" :class="{ selected: roomForm.amenities.includes(item) }">
                      <input type="checkbox" :checked="roomForm.amenities.includes(item)" @change="toggleRoomAmenity(item)" />
                      {{ item }}
                   </label>
                </div>
             </div>
          </div>

          <div class="room-form-actions">
            <button class="btn-outline" @click="showRoomForm = false">취소</button>
            <button class="btn-primary" @click="saveRoom">{{ editingRoomId ? '수정' : '등록' }}</button>
          </div>
        </div>
      </section>

      <!-- Bottom Actions -->
      <div class="bottom-actions">
        <button class="btn-cancel" @click="$router.push('/host/accommodation')">취소</button>
        <button class="btn-submit" @click="handleUpdate">수정 완료</button>
      </div>
    </div>
    
    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Copying styles from Register page with necessary tweaks */
.register-page {
  background: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 2rem;
}

.loading-spinner {
    text-align: center;
    padding: 2rem;
    font-size: 1.2rem;
    color: #004d40;
}

.page-header {
  background: white;
  padding: 1.5rem;
  margin: 1rem;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: sticky;
  top: 1rem;
  z-index: 10;
}

@media (min-width: 768px) {
  .page-header { max-width: 600px; margin: 1rem auto; }
}

.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
}

.toggle-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 25px;
}

.toggle-switch {
  width: 44px;
  height: 24px;
  background: #D1D5DB;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: background 0.3s;
}
.toggle-switch.active { background: #BFE7DF; }
.toggle-slider {
  width: 20px; height: 20px; background: white;
  border-radius: 50%; position: absolute;
  top: 2px; left: 2px; transition: left 0.3s;
}
.toggle-switch.active .toggle-slider { left: 22px; }

.btn-primary {
  padding: 0.6rem 1rem; border: none; background: #BFE7DF;
  border-radius: 8px; color: #004d40; font-weight: 600; cursor: pointer;
}
.btn-outline {
  padding: 0.6rem 1rem; border: 1px solid #ddd; background: white;
  border-radius: 8px; color: #333; font-weight: 600; cursor: pointer;
}

.form-content {
  padding: 0 1rem 1rem;
  max-width: 600px;
  margin: 0 auto;
}

.form-section {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.subsection-title {
  font-size: 1rem; font-weight: 700; color: #222; margin-bottom: 1rem;
}
.update-title { color: #004d40; }

.form-group { margin-bottom: 1.25rem; }
.form-group label { display: block; font-weight: 600; margin-bottom: 0.5rem; font-size: 0.9rem; }

input, select, textarea {
  width: 100%; padding: 0.875rem 1rem; border: 1px solid #e0e0e0;
  border-radius: 8px; font-size: 0.95rem; box-sizing: border-box;
}
input:focus, textarea:focus { outline: none; border-color: #BFE7DF; }
.readonly-input { background: #f5f5f5; color: #666; cursor: not-allowed; }

/* Amenities Grid */
.amenities-grid { display: flex; flex-wrap: wrap; gap: 0.75rem; }

.amenity-checkbox {
  display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1rem;
  border: 1px solid #e0e0e0; border-radius: 8px; background: white;
  cursor: pointer; transition: all 0.2s;
}
.amenity-checkbox:hover { border-color: #BFE7DF; }
.amenity-checkbox.checked { background: #f0fcfa; border-color: #BFE7DF; }

/* Hide default checkbox */
.amenity-checkbox input[type="checkbox"] {
  accent-color: #BFE7DF; width: 18px; height: 18px; cursor: pointer;
}

/* Theme Selection UI */
.theme-selection { margin-top: 1rem; }
.theme-category { margin-bottom: 1.5rem; }
.theme-category-title { font-weight: 600; color: #555; margin-bottom: 0.5rem; font-size: 0.9rem; }
.theme-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }

.theme-tag {
  display: inline-flex; align-items: center; padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0; border-radius: 20px; font-size: 0.9rem;
  color: #333; background: white; cursor: pointer; transition: all 0.2s;
}
.theme-tag:hover { border-color: #BFE7DF; }
.theme-tag.selected {
  background: #f0fcfa; border-color: #BFE7DF; color: #004d40;
}
.theme-tag input { display: none; }

.existing-image img, .banner-preview { width: 100%; height: 150px; object-fit: cover; border-radius: 8px; }
.detail-images-preview { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.5rem; }
.detail-image-item { aspect-ratio: 1; border-radius: 8px; overflow: hidden; }
.detail-image-item img { width: 100%; height: 100%; object-fit: cover; }

/* Room List */
.room-card { border: 1px solid #e0e0e0; border-radius: 12px; padding: 1rem; background: white; margin-bottom: 1rem; }
.room-info h4 { margin: 0 0 0.5rem; }
.room-details { font-size: 0.85rem; color: #666; margin-bottom: 0.5rem; }
.room-actions { display: flex; gap: 0.5rem; justify-content: flex-end; }
.room-btn { padding: 0.4rem 0.8rem; border: 1px solid #e0e0e0; border-radius: 4px; background: white; cursor: pointer; }

.add-room-btn {
  width: 100%; padding: 1rem; border: 2px dashed #BFE7DF; background: transparent;
  color: #BFE7DF; font-weight: 600; border-radius: 12px; cursor: pointer;
}
.add-room-btn:hover { background: #f5fcfa; }

/* Room Form */
.room-form { background: #fafafa; padding: 1rem; border-radius: 10px; border: 1px solid #eee; margin-top: 1rem; }
.form-row { display: flex; gap: 1rem; }
.form-row.two-col > * { flex: 1; }

.room-image-preview { position: relative; width: 100%; height: 200px; }
.room-image-preview img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.room-remove-image-btn { position: absolute; top: 10px; right: 10px; background: rgba(0,0,0,0.5); color: white; border: none; border-radius: 50%; width: 30px; height: 30px; cursor: pointer; }

.room-upload-box {
    display: flex; align-items: center; justify-content: center; height: 100px;
    border: 2px dashed #ddd; border-radius: 8px; cursor: pointer;
}
.hidden-file-input { display: none; }

.room-amenity-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.room-amenity-tag {
    padding: 0.4rem 0.8rem; border: 1px solid #e0e0e0; border-radius: 20px; font-size: 0.85rem; cursor: pointer;
}
.room-amenity-tag.selected { background: #e0f2f1; border-color: #004d40; color: #004d40; }
.room-amenity-tag input { display: none; }

.room-form-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1rem; }

.bottom-actions { display: flex; gap: 1rem; margin-top: 2rem; }
.btn-cancel { flex: 1; padding: 1rem; border: none; background: #e0e0e0; border-radius: 8px; cursor: pointer; color: #555; font-weight: 700; }
.btn-submit { flex: 2; padding: 1rem; border: none; background: #BFE7DF; border-radius: 8px; cursor: pointer; color: #004d40; font-weight: 700; }

.error-message { color: #d32f2f; font-size: 0.8rem; margin-top: 0.25rem; display: block; }
.input-error { border-color: #d32f2f; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 100; display: flex; justify-content: center; align-items: center; }
.modal-content { background: white; padding: 2rem; border-radius: 12px; width: 90%; max-width: 320px; text-align: center; }
.modal-btn { width: 100%; padding: 0.8rem; background: #BFE7DF; border: none; border-radius: 8px; margin-top: 1rem; cursor: pointer; font-weight: 700; color: #004d40; }
</style>
