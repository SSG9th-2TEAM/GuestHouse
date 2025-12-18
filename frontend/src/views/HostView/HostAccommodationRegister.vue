<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const emit = defineEmits(['cancel', 'submit'])

// ========== Business License Verification ==========
const isVerified = ref(false)
const businessLicenseImage = ref(null)
const businessLicensePreview = ref(null)
const extractedText = ref('')
const isExtracting = ref(false)
const isVerifying = ref(false)

const handleLicenseUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    businessLicenseImage.value = file
    businessLicensePreview.value = URL.createObjectURL(file)
  }
}

const extractText = () => {
  if (!businessLicenseImage.value) {
    openModal('사업자등록증 이미지를 먼저 선택해주세요.')
    return
  }
  isExtracting.value = true
  // Mock OCR process
  setTimeout(() => {
    extractedText.value = '사업자번호: 123-45-67890\n상호: 테스트 게스트하우스\n대표자: 홍길동'
    isExtracting.value = false
    openModal('텍스트 추출이 완료되었습니다.')
  }, 1000)
}

const verifyBusinessNumber = () => {
  if (!businessLicenseImage.value) {
    openModal('사업자등록증 이미지를 먼저 선택해주세요.')
    return
  }
  isVerifying.value = true
  // Mock verification
  setTimeout(() => {
    isVerifying.value = false
    isVerified.value = true
    openModal('사업자번호가 확인되었습니다. 이제 숙소를 등록할 수 있습니다.')
  }, 1000)
}

// 카카오맵 관련
const mapContainer = ref(null)
const map = ref(null)
const marker = ref(null)
const geocoder = ref(null)

// Form data
const form = ref({
  // 기본정보
  name: '',
  type: '',
  description: '',
  phone: '',
  email: '',
  // 위치정보
  city: '',
  district: '',
  address: '',
  latitude: null,
  longitude: null,
  // 교통정보
  transportInfo: '',
  // 운영 정책
  checkInTime: '15:00',
  checkOutTime: '11:00',
  cancelPolicy: '',
  // 정책 & 규정
  houseRules: '',
  // 주차정보
  parkingInfo: '',
  // 편의시설
  amenities: [],
  // 이미지
  bannerImage: null,
  detailImages: [],
  // 검색 최적화
  shortDescription: '',
  // 테마
  themes: [],
  // 정산 계좌
  bankName: '',
  accountHolder: '',
  accountNumber: '',
  // 상태
  isActive: true
})

// 은행 목록
const bankList = ['국민은행', '신한은행', '우리은행', '하나은행', '농협', '카카오뱅크', '토스뱅크', '기업은행']

// Accommodation types
const accommodationTypes = [
  '게스트하우스',
  '펜션',
  '호텔',
  '모텔',
  '리조트',
  '한옥',
  '캠핑/글램핑'
]


// 편의시설 옵션 (4개만)
const amenityOptions = [
  { id: 'wifi', label: '무선 인터넷' },
  { id: 'aircon', label: '에어컨' },
  { id: 'heating', label: '난방' },
  { id: 'tv', label: 'TV' }
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

const toggleAmenity = (id) => {
  const index = form.value.amenities.indexOf(id)
  if (index > -1) {
    form.value.amenities.splice(index, 1)
  } else {
    form.value.amenities.push(id)
  }
}

const toggleTheme = (theme) => {
  const index = form.value.themes.indexOf(theme)
  if (index > -1) {
    form.value.themes.splice(index, 1)
  } else {
    form.value.themes.push(theme)
  }
}

const handleBannerUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    form.value.bannerImage = URL.createObjectURL(file)
  }
}

const handleDetailImagesUpload = (event) => {
  const files = Array.from(event.target.files)
  const newImages = files.map(file => URL.createObjectURL(file))
  form.value.detailImages = [...form.value.detailImages, ...newImages].slice(0, 5)
}

const removeDetailImage = (index) => {
  form.value.detailImages.splice(index, 1)
}

// 모달 상태
const showModal = ref(false)
const modalMessage = ref('')
const registrationSuccess = ref(false)

const openModal = (message) => {
  modalMessage.value = message
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (registrationSuccess.value) {
    router.push('/host/accommodation')
  }
}

// 객실 관련
const rooms = ref([])
const showRoomForm = ref(false)
const roomForm = ref({
  name: '',
  type: '',
  weekdayPrice: '',
  weekendPrice: '',
  maxGuests: '',
  size: '',
  description: '',
  amenities: [],
  representativeImage: null,
  representativeImagePreview: '',
  isActive: true
})

const roomBedTypes = ['스탠다드', '디럭스', '스위트', '더블', '트윈', '패밀리']

// 객실 이미지 업로드 처리
const handleRoomImageUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    if (!file.type.startsWith('image/')) {
      openModal('이미지 파일만 업로드 가능합니다.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      openModal('파일 크기는 5MB 이하여야 합니다.')
      return
    }
    roomForm.value.representativeImage = file
    roomForm.value.representativeImagePreview = URL.createObjectURL(file)
  }
}

const removeRoomImage = () => {
  roomForm.value.representativeImage = null
  roomForm.value.representativeImagePreview = ''
}

// 객실 편의시설 옵션 (일부만)
const roomAmenityOptions = {
  bathroom: {
    label: '욕실',
    items: ['비누', '샤워', '개인 욕실']
  },
  bedroom: {
    label: '침실',
    items: ['간이/추가 침대 제공', '에어컨', '난방']
  },
  dining: {
    label: '식사 및 음료',
    items: ['공용 주방 이용', '전용 주방']
  },
  etc: {
    label: '기타',
    items: ['무료 WiFi', '금고', '다리미']
  }
}

const toggleRoomAmenity = (item) => {
  const index = roomForm.value.amenities.indexOf(item)
  if (index > -1) {
    roomForm.value.amenities.splice(index, 1)
  } else {
    roomForm.value.amenities.push(item)
  }
}

const addRoom = () => {
  if (!roomForm.value.name || !roomForm.value.type || !roomForm.value.weekdayPrice || !roomForm.value.weekendPrice) {
    openModal('객실 이름, 타입, 주중/주말 요금은 필수입니다.')
    return
  }
  if (!roomForm.value.representativeImage) {
    openModal('객실 대표 이미지를 등록해주세요.')
    return
  }

  rooms.value.push({
    id: Date.now(),
    ...roomForm.value
  })

  // 폼 초기화
  roomForm.value = {
    name: '',
    type: '',
    weekdayPrice: '',
    weekendPrice: '',
    maxGuests: '',
    size: '',
    description: '',
    amenities: [],
    representativeImage: null,
    representativeImagePreview: '',
    isActive: true
  }
  showRoomForm.value = false
  openModal('객실이 추가되었습니다.')
}

const deleteRoom = (id) => {
  rooms.value = rooms.value.filter(r => r.id !== id)
}

const toggleRoomActive = (id) => {
  const room = rooms.value.find(r => r.id === id)
  if (room) {
    room.isActive = !room.isActive
  }
}

// 카카오맵 SDK 로딩 대기
const waitForKakao = () => {
  return new Promise((resolve) => {
    if (window.kakao && window.kakao.maps) {
      resolve()
    } else {
      const checkKakao = setInterval(() => {
        if (window.kakao && window.kakao.maps) {
          clearInterval(checkKakao)
          resolve()
        }
      }, 100)
    }
  })
}

// 카카오맵 초기화
const initMap = async () => {
  await waitForKakao()

  window.kakao.maps.load(() => {
    const container = mapContainer.value
    if (!container) return

    const options = {
      center: new window.kakao.maps.LatLng(37.5665, 126.9780), // 서울시청 기본 좌표
      level: 3
    }

    map.value = new window.kakao.maps.Map(container, options)
    geocoder.value = new window.kakao.maps.services.Geocoder()

    // 마커 생성
    marker.value = new window.kakao.maps.Marker({
      position: options.center,
      map: map.value
    })
    marker.value.setVisible(false)
  })
}

// 주소로 좌표 검색
const handleLocationCheck = () => {
  const fullAddress = `${form.value.city} ${form.value.district} ${form.value.address}`.trim()

  if (!fullAddress || fullAddress.length < 5) {
    openModal('주소를 입력해주세요.')
    return
  }

  if (!geocoder.value) {
    openModal('지도가 초기화되지 않았습니다. 잠시 후 다시 시도해주세요.')
    return
  }

  geocoder.value.addressSearch(fullAddress, (result, status) => {
    if (status === window.kakao.maps.services.Status.OK) {
      const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x)

      // 좌표 저장
      form.value.latitude = parseFloat(result[0].y)
      form.value.longitude = parseFloat(result[0].x)

      // 지도 중심 이동
      map.value.setCenter(coords)

      // 마커 위치 변경 및 표시
      marker.value.setPosition(coords)
      marker.value.setVisible(true)

      openModal('위치가 확인되었습니다!')
    } else {
      openModal('주소를 찾을 수 없습니다. 정확한 주소를 입력해주세요.')
    }
  })
}

// isVerified가 true가 되면 지도 초기화
watch(() => isVerified.value, async (newVal) => {
  if (newVal) {
    await nextTick()
    setTimeout(() => {
      initMap()
    }, 100)
  }
})





const handleTempSave = () => {
  openModal('임시 저장되었습니다.')
}

const handleSubmit = () => {
  if (!form.value.name || !form.value.type || !form.value.description) {
    openModal('숙소 필수 정보를 모두 입력해주세요.')
    return
  }

  if (rooms.value.length === 0) {
    openModal('최소 1개의 객실을 등록해야 합니다.')
    return
  }
  
  // 실제 API 연동 시 여기에 저장 로직 추가
  console.log('Submitting form:', { ...form.value, rooms: rooms.value })
  
  registrationSuccess.value = true
  openModal('숙소가 성공적으로 등록되었습니다.')
}
</script>

<template>
  <div class="register-page">
    <!-- Page Header (Only after verification) -->
    <div v-if="isVerified" class="page-header">
      <div class="header-top">
        <div class="title-area">
          <h1>숙소 등록</h1>
        </div>
      </div>
      
      <!-- Progress Bar -->
      <div class="progress-wrapper">
        <div class="progress-bar">
          <div class="progress-fill" style="width: 30%"></div>
        </div>
        <span class="progress-text">진행 중</span>
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
          <button class="btn-outline" @click="handleTempSave">임시 저장</button>
          <button class="btn-primary" @click="handleSubmit">저장하기</button>
        </div>
      </div>
    </div>

    <!-- ========== Verification Step ========== -->
    <div v-if="!isVerified" class="verification-step">
      <div class="verification-card">
        <h2 class="verification-title">사업자등록증 확인</h2>
        <p class="verification-desc">사업자등록증 이미지를 업로드하여 사업자번호를 확인하세요.</p>
        
        <div class="license-upload-area">
          <div v-if="businessLicensePreview" class="license-preview">
            <img :src="businessLicensePreview" alt="사업자등록증" />
            <button class="remove-btn" @click="businessLicensePreview = null; businessLicenseImage = null">&times;</button>
          </div>
          <label v-else class="upload-box">
            <input type="file" accept="image/*" @change="handleLicenseUpload" hidden />
            <span class="upload-text">이미지 파일 선택</span>
          </label>
        </div>
        
        <div class="verification-actions">
          <button 
            class="btn-verify" 
            @click="verifyBusinessNumber"
            :disabled="isVerifying || !businessLicenseImage"
          >
            {{ isVerifying ? '확인 중...' : '사업자번호 확인' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== Registration Form (Only after verification) ========== -->
    <div v-else class="form-content">
      <!-- Section: 숙소 등록 -->
      <section class="form-section">
        <h2 class="section-title">숙소 등록</h2>
        <p class="section-desc">숙소의 기본 정보를 입력해주세요.</p>
        
        <h3 class="subsection-title">기본정보</h3>
        
        <div class="form-group">
          <label>숙소명 <span class="required">*</span></label>
          <input 
            v-model="form.name" 
            type="text" 
            placeholder="숙소 이름을 입력하세요"
          />
        </div>
        
        <div class="form-group">
          <label>숙소유형 <span class="required">*</span></label>
          <select v-model="form.type">
            <option value="" disabled>선택해주세요</option>
            <option v-for="type in accommodationTypes" :key="type" :value="type">
              {{ type }}
            </option>
          </select>
        </div>
        
        <div class="form-group">
          <label>숙소 소개(상세설명) <span class="required">*</span></label>
          <textarea 
            v-model="form.description" 
            rows="5"
            placeholder="숙소의 매력 포인트, 주변 환경, 호스팅 스타일 등을 상세히 적어주세요."
          ></textarea>
        </div>
        
        <div class="form-group">
          <label>대표 연락처 <span class="required">*</span></label>
          <input 
            v-model="form.phone" 
            type="tel" 
            placeholder="010-1234-5678"
          />
        </div>
        
        <div class="form-group">
          <label>이메일 주소 <span class="required">*</span></label>
          <input 
            v-model="form.email" 
            type="email" 
            placeholder="example@email.com"
          />
        </div>
      </section>

      <!-- Section: 위치 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">위치 정보</h3>
        
        <div class="form-group">
          <label>시/도 <span class="required">*</span></label>
          <input 
            v-model="form.city" 
            type="text" 
            placeholder="예: 서울시"
          />
        </div>
        
        <div class="form-group">
          <label>구/군 <span class="required">*</span></label>
          <input 
            v-model="form.district" 
            type="text" 
            placeholder="예: 강남구"
          />
        </div>
        
        <div class="form-group">
          <label>상세주소(동, 호수) <span class="required">*</span></label>
          <input 
            v-model="form.address" 
            type="text" 
            placeholder="예: 테헤란로 123, 456호"
          />
        </div>
        
        <button class="btn-location" @click="handleLocationCheck">위치 확인</button>
      </section>

      <!-- Section: 지도 -->
      <section class="form-section">
        <h3 class="subsection-title">지도 <span class="required">*</span></h3>

        <div ref="mapContainer" class="kakao-map"></div>
        <p class="help-text">위치 확인 버튼을 클릭하면 지도에 마커가 표시됩니다</p>
      </section>

      <!-- Section: 교통 및 주변 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">교통 및 주변 정보</h3>
        
        <div class="form-group">
          <textarea 
            v-model="form.transportInfo" 
            rows="4"
            placeholder="지하철역, 버스정류장, 주요 교통편, 길 안내, 랜드마크, 주변 관광지 정보를 입력해주세요."
          ></textarea>
        </div>
      </section>

      <!-- Section: 운영 정책 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">체크인 정보</h3>
        
        <div class="form-group">
          <label>체크인 시간 <span class="required">*</span></label>
          <div class="time-input">
            <input 
              v-model="form.checkInTime" 
              type="time"
            />
          </div>
        </div>
        
        <div class="form-group">
          <label>체크아웃 시간 <span class="required">*</span></label>
          <div class="time-input">
            <input 
              v-model="form.checkOutTime" 
              type="time"
            />
          </div>
        </div>
      </section>

      <!-- Section: 정책 & 규정 (하우스 룰) -->
      <section class="form-section">
        <h3 class="subsection-title">정책 & 규정 (하우스 룰)</h3>
        
        <div class="form-group">
          <textarea 
            v-model="form.houseRules" 
            rows="4"
            placeholder="소음 제한, 파티 금지, 흡연 금지 등 숙소 이용 규정을 입력해주세요."
          ></textarea>
        </div>
      </section>

      <!-- Section: 주차정보 -->
      <section class="form-section">
        <h3 class="subsection-title">주차정보</h3>
        
        <div class="form-group">
          <label>주차정보</label>
          <textarea 
            v-model="form.parkingInfo" 
            rows="4"
            placeholder="주차 가능 여부, 주차 요금, 주차장 위치, 예약 방법 등을 상세히 입력해주세요."
          ></textarea>
        </div>
      </section>

      <!-- Section: 편의 시설 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">편의 시설 정보</h3>
        
        <div class="amenities-grid">
          <label 
            v-for="amenity in amenityOptions" 
            :key="amenity.id" 
            class="amenity-checkbox"
            :class="{ checked: form.amenities.includes(amenity.id) }"
          >
            <input 
              type="checkbox" 
              :checked="form.amenities.includes(amenity.id)"
              @change="toggleAmenity(amenity.id)"
            />
            <span class="checkmark"></span>
            <span class="amenity-label">{{ amenity.label }}</span>
          </label>
        </div>
      </section>

      <!-- Section: 이미지 업로드 -->
      <section class="form-section">
        <h3 class="subsection-title">이미지</h3>

        <div class="form-group">
          <label>배너 이미지 <span class="required">*</span></label>
          <div class="upload-box">
            <div class="upload-placeholder" v-if="!form.bannerImage">
              <span class="upload-text">드래그하거나 클릭해 배너 이미지 추가</span>
              <span class="upload-info">JPG, PNG, HEIC / 최대 20MB</span>
              <span class="upload-hint">권장 크기: 1920x600px</span>
            </div>
            <img v-else :src="form.bannerImage" class="banner-preview" />
            <input type="file" accept="image/*" @change="handleBannerUpload" />
          </div>
        </div>
        
        <div class="form-group">
          <label>숙소 상세 이미지 <span class="required">*</span></label>
          <div class="upload-box">
            <div class="upload-placeholder" v-if="form.detailImages.length === 0">
              <span class="upload-text">드래그하거나 클릭해 상세 이미지 추가</span>
              <span class="upload-info">JPG, PNG, HEIC / 최대 20MB (최소 5장 권장)</span>
              <span class="upload-hint">최소 1200px 권장</span>
            </div>
            <input type="file" accept="image/*" multiple @change="handleDetailImagesUpload" />
          </div>
          
          <div v-if="form.detailImages.length > 0" class="detail-images-preview">
            <div v-for="(img, idx) in form.detailImages" :key="idx" class="detail-image-item">
              <img :src="img" />
              <button class="remove-image-btn" @click="removeDetailImage(idx)">×</button>
            </div>
          </div>
        </div>
      </section>

      <!-- Section: 검색 최적화 정보 -->
      <section class="form-section">
        <h3 class="subsection-title">검색 최적화 정보</h3>
        
        <div class="form-group">
          <label>한 줄 설명 (짧은 숙소 소개 문구)</label>
          <input 
            v-model="form.shortDescription" 
            type="text" 
            placeholder="예: 바다 전망이 있는 포근한 스튜디오"
          />
        </div>
      </section>

      <!-- Section: 테마 (중복 선택 가능) -->
      <section class="form-section">
        <h3 class="subsection-title">테마 (최대 6개선택 가능)</h3>
        
        <div v-for="(category, key) in themeOptions" :key="key" class="theme-category">
          <div class="theme-category-title">
            {{ category.label }}
          </div>
          <div class="theme-tags">
            <label 
              v-for="item in category.items" 
              :key="item" 
              class="theme-tag"
              :class="{ selected: form.themes.includes(item) }"
            >
              <input 
                type="checkbox" 
                :checked="form.themes.includes(item)"
                @change="toggleTheme(item)"
              />
              {{ item }}
            </label>
          </div>
        </div>
        
        <p class="help-text">여러 테마를 선택할 수 있습니다</p>
      </section>

      <!-- Section: 정산 계좌 -->
      <section class="form-section">
        <h3 class="subsection-title">정산 계좌</h3>
        
        <div class="form-group">
          <label>은행명 <span class="required">*</span></label>
          <select v-model="form.bankName">
            <option value="" disabled>선택해주세요</option>
            <option v-for="bank in bankList" :key="bank" :value="bank">
              {{ bank }}
            </option>
          </select>
        </div>
        
        <div class="form-group">
          <label>예금주 <span class="required">*</span></label>
          <input 
            v-model="form.accountHolder" 
            type="text" 
            placeholder="예금주명을 입력해주세요"
          />
        </div>
        
        <div class="form-group">
          <label>계좌번호 <span class="required">*</span></label>
          <input 
            v-model="form.accountNumber" 
            type="text" 
            placeholder="'-' 없이 숫자만 입력"
          />
        </div>
      </section>

      <!-- Section: 등록된 객실 -->
      <section class="form-section">
        <h3 class="subsection-title" style="color: #BFE7DF;">등록된 객실</h3>
        
        <!-- 등록된 객실 목록 -->
        <div v-if="rooms.length > 0" class="room-list">
          <div v-for="room in rooms" :key="room.id" class="room-card">
            <div class="room-info">
              <h4 class="room-name">{{ room.name }}</h4>
              <div class="room-details">
              <div class="detail-row">
                <span class="detail-label">주중 요금</span>
                <span class="detail-value">₩{{ Number(room.weekdayPrice).toLocaleString() }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">주말 요금 (금~토)</span>
                <span class="detail-value">₩{{ Number(room.weekendPrice).toLocaleString() }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">최대 인원</span>
                <span class="detail-value">{{ room.maxGuests }}명</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">크기</span>
                <span class="detail-value">{{ room.size }}평</span>
              </div>
            </div>
              <div class="room-toggle">
                <span>{{ room.isActive ? 'ON' : 'OFF' }}</span>
                <div 
                  class="toggle-switch small" 
                  :class="{ active: room.isActive }"
                  @click="toggleRoomActive(room.id)"
                >
                  <div class="toggle-slider"></div>
                </div>
              </div>
            </div>
            <div class="room-actions">
              <button class="room-btn" @click="deleteRoom(room.id)">삭제</button>
            </div>
          </div>
        </div>
        
        <p v-else class="no-rooms">등록된 객실이 없습니다.</p>
        
        <!-- 객실 추가 버튼 -->
        <button class="add-room-btn" @click="showRoomForm = true" v-if="!showRoomForm">
          + 객실 추가하기
        </button>
        
        <!-- 객실 추가 폼 -->
        <div v-if="showRoomForm" class="room-form">
          <h4 class="room-form-title">새 객실 정보</h4>

          <div class="form-group">
            <label>객실명 <span class="required">*</span></label>
            <input
              v-model="roomForm.name"
              type="text"
              placeholder="예: 스탠다드 더블룸"
            />
          </div>

          <div class="form-group">
            <label>객실 대표 이미지 <span class="required">*</span></label>
            <div class="room-image-upload-area">
              <div v-if="roomForm.representativeImagePreview" class="room-image-preview">
                <img :src="roomForm.representativeImagePreview" alt="객실 대표 이미지" />
                <button type="button" class="room-remove-image-btn" @click="removeRoomImage">
                  ✕
                </button>
              </div>
              <label v-else class="room-upload-box">
                <input
                  type="file"
                  accept="image/*"
                  @change="handleRoomImageUpload"
                  class="hidden-file-input"
                />
                <div class="room-upload-content">
                  <span class="room-upload-icon">📷</span>
                  <span class="room-upload-text">이미지 업로드</span>
                  <span class="room-upload-hint">JPG, PNG (최대 5MB)</span>
                </div>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label>객실 침대 유형 <span class="required">*</span></label>
            <select v-model="roomForm.type">
              <option value="" disabled>선택해주세요</option>
              <option v-for="type in roomBedTypes" :key="type" :value="type">
                {{ type }}
              </option>
            </select>
          </div>
          
          <div class="form-row two-col">
            <div class="form-group">
              <label>주중 요금 (일~목) <span class="required">*</span></label>
              <div class="input-with-unit">
                <input 
                  v-model="roomForm.weekdayPrice" 
                  type="number" 
                  placeholder="0"
                />
                <span class="unit">원</span>
              </div>
            </div>
            <div class="form-group">
              <label>주말 요금 (금~토) <span class="required">*</span></label>
              <div class="input-with-unit">
                <input 
                  v-model="roomForm.weekendPrice" 
                  type="number" 
                  placeholder="0"
                />
                <span class="unit">원</span>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label>최대 인원 <span class="required">*</span></label>
            <input 
              v-model="roomForm.maxGuests" 
              type="number" 
              placeholder="명"
            />
          </div>
          
          <div class="form-group">
            <label>객실크기 (m²) <span class="required">*</span></label>
            <input 
              v-model="roomForm.size" 
              type="number" 
              placeholder="예: 30.0"
            />
          </div>
          
          <div class="form-group">
            <label>객실 설명 <span class="required">*</span></label>
            <textarea 
              v-model="roomForm.description" 
              rows="3"
              placeholder="객실의 특징, 편의시설, 전망 등을 상세히 입력해 주세요."
            ></textarea>
          </div>
          
          <!-- 객실 편의시설 -->
          <div class="room-amenities-section">
            <h4 class="room-amenities-title">객실 편의시설</h4>
            
            <div v-for="(category, key) in roomAmenityOptions" :key="key" class="room-amenity-category">
              <div class="room-amenity-label">{{ category.label }}</div>
              <div class="room-amenity-tags">
                <label 
                  v-for="item in category.items" 
                  :key="item" 
                  class="room-amenity-tag"
                  :class="{ selected: roomForm.amenities.includes(item) }"
                >
                  <input 
                    type="checkbox" 
                    :checked="roomForm.amenities.includes(item)"
                    @change="toggleRoomAmenity(item)"
                  />
                  {{ item }}
                </label>
              </div>
            </div>
          </div>
          
          <div class="room-form-actions">
            <button class="btn-outline" @click="showRoomForm = false">취소</button>
            <button class="btn-primary" @click="addRoom">등록</button>
          </div>
        </div>
      </section>

      <!-- Bottom Actions -->
      <div class="bottom-actions">
        <button class="btn-cancel" @click="$router.push('/host')">취소</button>
        <button class="btn-submit" @click="handleSubmit">등록 완료</button>
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
.register-page {
  background: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 2rem;
}

/* Page Header */
.page-header {
  background: white;
  padding: 1.5rem;
  margin: 1rem;
  max-width: 570px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: sticky;
  top: 1rem;
  z-index: 10;
  overflow: hidden;
}

@media (min-width: 768px) {
  .page-header {
    margin: 1rem auto;
  }
}

.header-top {
  margin-bottom: 1rem;
}

.title-area {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-badge {
  width: 36px;
  height: 36px;
  background: #BFE7DF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #004d40;
  font-size: 1.1rem;
}

.page-header h1 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #BFE7DF;
  margin: 0;
}

/* Progress Bar */
.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.progress-bar {
  position: relative;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  flex: 1;
}

.progress-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: #BFE7DF;
  border-radius: 3px;
}

.progress-text {
  position: static;
  font-size: 0.75rem;
  color: #888;
  white-space: nowrap;
  margin-left: 0.5rem;
  flex-shrink: 0;
}

/* Header Controls */
.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
}

.toggle-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 25px;
}

.toggle-label {
  font-size: 0.9rem;
  color: #333;
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

.toggle-switch.active {
  background: #BFE7DF;
}

.toggle-slider {
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: left 0.3s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.toggle-switch.active .toggle-slider {
  left: 22px;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.btn-outline {
  padding: 0.6rem 1rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  cursor: pointer;
}

.btn-primary {
  padding: 0.6rem 1rem;
  border: none;
  background: #BFE7DF;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
}

.btn-primary:hover {
  background: #a8ddd2;
}

/* Form Content */
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

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #BFE7DF;
  margin: 0 0 0.5rem;
}

.section-desc {
  font-size: 0.9rem;
  color: #888;
  margin: 0 0 1.5rem;
}

.subsection-title {
  font-size: 1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 1rem;
}

/* Form Groups */
.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.required {
  color: #BFE7DF;
}

input[type="text"],
input[type="tel"],
input[type="email"],
select,
textarea {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #BFE7DF;
}

input::placeholder,
textarea::placeholder {
  color: #aaa;
}

select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  cursor: pointer;
}

/* Location Button */
.btn-location {
  width: 100%;
  padding: 0.875rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
  margin-top: 0.5rem;
}

.btn-location:hover {
  background: #a8ddd2;
}

/* Kakao Map */
.kakao-map {
  width: 100%;
  height: 280px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
}

.coords-info {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #666;
  background: #f0f0f0;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
}

.help-text {
  font-size: 0.8rem;
  color: #888;
  margin-top: 0.75rem;
}

/* Bottom Actions */
.bottom-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.btn-cancel {
  flex: 1;
  padding: 1rem;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  cursor: pointer;
}

.btn-submit {
  flex: 2;
  padding: 1rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
}

.btn-submit:hover {
  background: #a8ddd2;
}

/* Mobile Responsive */
@media (max-width: 480px) {
  .header-controls {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }
  
  .action-buttons {
    width: 100%;
  }
  
  .btn-outline,
  .btn-primary {
    flex: 1;
  }
}

/* Time Input */
.time-input {
  position: relative;
}

.time-input input[type="time"] {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
}

.time-input input[type="time"]:focus {
  outline: none;
  border-color: #BFE7DF;
}

/* Amenities Grid */
.amenities-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.amenity-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.amenity-checkbox:hover {
  border-color: #BFE7DF;
}

.amenity-checkbox.checked {
  border-color: #BFE7DF;
  background: #f0fcfa;
}

.amenity-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #BFE7DF;
  cursor: pointer;
}

.amenity-label {
  font-size: 0.9rem;
  color: #333;
}

/* Upload Box */
.upload-box {
  position: relative;
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
}

.upload-box:hover {
  border-color: #BFE7DF;
  background: #f9fefe;
}

.upload-box input[type="file"] {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.upload-text {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}

.upload-info {
  font-size: 0.85rem;
  color: #888;
}

.upload-hint {
  font-size: 0.8rem;
  color: #aaa;
  padding: 0.25rem 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  margin-top: 0.5rem;
}

.banner-preview {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 8px;
}

/* Detail Images Preview */
.detail-images-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 0.75rem;
  margin-top: 1rem;
}

.detail-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.detail-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

/* Theme Categories */
.theme-category {
  margin-bottom: 1.5rem;
}

.theme-category-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
}

.theme-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.theme-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 0.9rem;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.theme-tag input[type="checkbox"] {
  display: none;
}

.theme-tag:hover {
  border-color: #BFE7DF;
}

.theme-tag.selected {
  border-color: #BFE7DF;
  background: #f0fcfa;
  color: #004d40;
}

/* Room List */
.room-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.room-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 1rem;
}

.room-info {
  margin-bottom: 1rem;
}

.room-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 0.5rem;
}

.room-details {
  font-size: 0.85rem;
  color: #666;
  margin: 0 0 0.75rem;
}

.room-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.toggle-switch.small {
  width: 36px;
  height: 20px;
}

.toggle-switch.small .toggle-slider {
  width: 16px;
  height: 16px;
}

.toggle-switch.small.active .toggle-slider {
  left: 18px;
}

.room-actions {
  display: flex;
  gap: 0.5rem;
}

.room-btn {
  flex: 1;
  padding: 0.6rem;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
}

.room-btn:hover {
  background: #f5f5f5;
}

.no-rooms {
  text-align: center;
  color: #888;
  padding: 2rem;
}

/* Add Room Button */
.add-room-btn {
  width: 100%;
  padding: 1rem;
  border: 2px dashed #BFE7DF;
  background: transparent;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  color: #BFE7DF;
  cursor: pointer;
  transition: all 0.2s;
}

.add-room-btn:hover {
  background: #f5fcfa;
}

/* Form Helper Classes */
.form-row {
  display: flex;
  gap: 1rem;
}

.form-row.two-col > * {
  flex: 1;
}

.input-with-unit {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-unit input {
  padding-right: 2.5rem;
}

.unit {
  position: absolute;
  right: 1rem;
  color: #666;
  font-size: 0.9rem;
}

/* Room Form */
.room-content {
  background: white;
  border-radius: 8px;
  padding: 1rem;
}
/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 320px;
  width: 90%;
  text-align: center;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  margin: 0 0 1.5rem;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 0.875rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: #004d40;
  cursor: pointer;
}

.modal-btn:hover {
  background: #a8ddd2;
}

/* Number Input Fix */
input[type="number"] {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
  box-sizing: border-box;
}

input[type="number"]:focus {
  outline: none;
  border-color: #BFE7DF;
}

/* Room Amenities Section */
.room-amenities-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.room-amenities-title {
  font-size: 1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 1rem;
}

.room-amenity-category {
  margin-bottom: 1.25rem;
}

.room-amenity-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #555;
  margin-bottom: 0.5rem;
}

.room-amenity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.room-amenity-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.85rem;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.room-amenity-tag input[type="checkbox"] {
  width: 16px;
  height: 16px;
  margin-right: 0.4rem;
  accent-color: #BFE7DF;
}

.room-amenity-tag:hover {
  border-color: #BFE7DF;
}

.room-amenity-tag.selected {
  border-color: #BFE7DF;
  background: #f0fcfa;
}

/* ========== Verification Step ========== */
.verification-step {
  padding: 2rem 1rem;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.verification-card {
  background: white;
  border-radius: 20px;
  padding: 2.5rem;
  max-width: 500px;
  width: 100%;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

.verification-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
  margin: 0 0 0.5rem;
}

.verification-desc {
  font-size: 0.95rem;
  color: #666;
  margin: 0 0 2rem;
}

.license-upload-area {
  margin-bottom: 1.5rem;
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 2rem;
  border: 2px dashed #ccc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

/* Room Image Upload Styles */
.room-image-upload-area {
  width: 100%;
}

.room-upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 150px;
  border: 2px dashed #BFE7DF;
  border-radius: 12px;
  background: #f8fffe;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-box:hover {
  border-color: #BFE7DF;
  background: #f9fcfb;
}

.upload-text {
  font-size: 1rem;
  color: #666;
}

.license-preview {
  position: relative;
  display: inline-block;
}

.license-preview img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.license-preview .remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ff5252;
  color: white;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.room-upload-box:hover {
  background: #f0fbf9;
  border-color: #8fd4c7;
}

.hidden-file-input {
  display: none;
}

.room-upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.room-upload-icon {
  font-size: 2rem;
}

.room-upload-text {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
}

.room-upload-hint {
  font-size: 0.8rem;
  color: #888;
}

.room-image-preview {
  position: relative;
  width: 100%;
  max-width: 200px;
}

.room-image-preview img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.room-remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.extracted-text-box {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
  text-align: left;
}

.extracted-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #555;
  margin: 0 0 0.5rem;
}

.extracted-content {
  font-size: 0.9rem;
  color: #333;
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.verification-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.btn-extract,
.btn-verify {
  padding: 0.875rem 1.5rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-extract {
  background: #00875A;
  color: white;
}

.btn-extract:hover:not(:disabled) {
  background: #006644;
}

.btn-verify {
  background: #BFE7DF;
  color: #004d40;
}

.btn-verify:hover:not(:disabled) {
  background: #a8ddd2;
}

.btn-extract:disabled,
.btn-verify:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.room-remove-image-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}
</style>

