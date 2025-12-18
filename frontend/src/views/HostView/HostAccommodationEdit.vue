<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const accommodationId = route.params.id

// 모달 상태
const showModal = ref(false)
const modalMessage = ref('')

const openModal = (message) => {
  modalMessage.value = message
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

// 수정 모드 상태
const isEditing = ref(false)

// Mock 데이터 - 실제로는 API에서 가져옴
const form = ref({
  name: '게스트하우스 서울',
  type: '게스트하우스',
  description: '서울 중심부에 위치한 아늑한 게스트하우스입니다.',
  phone: '010-1234-5678',
  email: 'host@example.com',
  city: '서울시',
  district: '마포구',
  address: '상수동 123-45',
  transportInfo: '지하철 6호선 상수역 2번 출구 도보 5분',
  checkInTime: '15:00',
  checkOutTime: '11:00',
  houseRules: '22시 이후 정숙, 흡연 금지',
  parkingInfo: '건물 내 주차 가능 (무료)',
  shortDescription: '서울 마포구의 아늑한 게스트하우스',
  bankName: '카카오뱅크',
  accountHolder: '홍길동',
  accountNumber: '3333123456789',
  isActive: true
})

const accommodationTypes = [
  '게스트하우스', '펜션', '호텔', '모텔', '리조트', '한옥', '캠핑/글램핑'
]

const bankList = ['국민은행', '신한은행', '우리은행', '하나은행', '농협', '카카오뱅크', '토스뱅크', '기업은행']

const handleSave = () => {
  openModal('숙소 정보가 저장되었습니다.')
  isEditing.value = false
}

const handleCancel = () => {
  if (isEditing.value) {
    isEditing.value = false
  } else {
    router.push('/host/accommodation')
  }
}

// Mock 객실 데이터 - 실제로는 API에서 가져옴
const rooms = ref([
  {
    id: 1,
    name: '스탠다드 더블룸',
    weekdayPrice: 50000,
    weekendPrice: 70000,
    minGuests: 1,
    maxGuests: 2,
    bedCount: 1,
    bathroomCount: 1,
    amenities: ['비누', '샤워', '에어컨', '무료 WiFi'],
    isActive: true
  },
  {
    id: 2,
    name: '디럭스 트윈룸',
    weekdayPrice: 70000,
    weekendPrice: 90000,
    minGuests: 1,
    maxGuests: 2,
    bedCount: 2,
    bathroomCount: 1,
    amenities: ['비누', '샤워', '개인 욕실', '에어컨', '난방', '무료 WiFi'],
    isActive: true
  },
  {
    id: 3,
    name: '패밀리룸',
    weekdayPrice: 100000,
    weekendPrice: 130000,
    minGuests: 2,
    maxGuests: 4,
    bedCount: 2,
    bathroomCount: 2,
    amenities: ['비누', '샤워', '개인 욕실', '에어컨', '난방', '전용 주방', '무료 WiFi', '금고'],
    isActive: false
  }
])

// 객실 폼
const showRoomForm = ref(false)
const editingRoomId = ref(null) // 수정 중인 객실 ID (null이면 추가 모드)
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

// 객실 편의시설 옵션
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

const toggleRoomActive = (id) => {
  if (!isEditing.value) return
  const room = rooms.value.find(r => r.id === id)
  if (room) {
    room.isActive = !room.isActive
  }
}

const addRoom = () => {
  if (!roomForm.value.name || !roomForm.value.weekdayPrice || !roomForm.value.weekendPrice) {
    openModal('객실 이름, 주중/주말 요금은 필수입니다.')
    return
  }
  if (!roomForm.value.representativeImage) {
    openModal('객실 대표 이미지를 등록해주세요.')
    return
  }

  rooms.value.push({
    id: Date.now(),
    ...roomForm.value,
    weekdayPrice: Number(roomForm.value.weekdayPrice),
    weekendPrice: Number(roomForm.value.weekendPrice),
    minGuests: Number(roomForm.value.minGuests),
    maxGuests: Number(roomForm.value.maxGuests),
    bedCount: Number(roomForm.value.bedCount),
    bathroomCount: Number(roomForm.value.bathroomCount),
    amenities: [...roomForm.value.amenities]
  })

  roomForm.value = {
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
  }
  showRoomForm.value = false
  openModal('객실이 추가되었습니다.')
}

const deleteRoom = (id) => {
  rooms.value = rooms.value.filter(r => r.id !== id)
}

// 객실 수정 버튼 클릭 - 기존 정보 로드
const editRoom = (room) => {
  editingRoomId.value = room.id
  roomForm.value = {
    name: room.name,
    weekdayPrice: room.weekdayPrice,
    weekendPrice: room.weekendPrice,
    minGuests: room.minGuests,
    maxGuests: room.maxGuests,
    bedCount: room.bedCount,
    bathroomCount: room.bathroomCount,
    description: room.description || '',
    amenities: room.amenities ? [...room.amenities] : [],
    representativeImage: room.representativeImage || null,
    representativeImagePreview: room.representativeImagePreview || '',
    isActive: room.isActive
  }
  showRoomForm.value = true
}

// 객실 수정 완료
const updateRoom = () => {
  if (!roomForm.value.name || !roomForm.value.weekdayPrice || !roomForm.value.weekendPrice) {
    openModal('객실 이름, 주중/주말 요금은 필수입니다.')
    return
  }

  const roomIndex = rooms.value.findIndex(r => r.id === editingRoomId.value)
  if (roomIndex !== -1) {
    rooms.value[roomIndex] = {
      ...rooms.value[roomIndex],
      name: roomForm.value.name,
      weekdayPrice: Number(roomForm.value.weekdayPrice),
      weekendPrice: Number(roomForm.value.weekendPrice),
      minGuests: Number(roomForm.value.minGuests),
      maxGuests: Number(roomForm.value.maxGuests),
      bedCount: Number(roomForm.value.bedCount),
      bathroomCount: Number(roomForm.value.bathroomCount),
      description: roomForm.value.description,
      amenities: [...roomForm.value.amenities],
      representativeImage: roomForm.value.representativeImage,
      representativeImagePreview: roomForm.value.representativeImagePreview,
      isActive: roomForm.value.isActive
    }
  }

  resetRoomForm()
  openModal('객실 정보가 수정되었습니다.')
}

// 객실 폼 초기화
const resetRoomForm = () => {
  roomForm.value = {
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
  }
  editingRoomId.value = null
  showRoomForm.value = false
}

const formatPrice = (price) => {
  return price.toLocaleString()
}

onMounted(() => {
  // 실제로는 API 호출하여 숙소 정보 가져오기
  console.log('Loading accommodation:', accommodationId)
})
</script>

<template>
  <div class="edit-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-top">
        <div class="title-area">
          <h1>숙소 정보 {{ isEditing ? '수정' : '상세' }}</h1>
        </div>
      </div>
      
      <div class="header-controls">
        <div class="toggle-container">
          <span class="toggle-label">숙소 운영</span>
          <div 
            class="toggle-switch" 
            :class="{ active: form.isActive }"
            @click="isEditing && (form.isActive = !form.isActive)"
          >
            <div class="toggle-slider"></div>
          </div>
        </div>
        
        <div class="action-buttons">
          <button v-if="!isEditing" class="btn-outline" @click="isEditing = true">수정하기</button>
          <button v-if="isEditing" class="btn-outline" @click="handleCancel">취소</button>
          <button v-if="isEditing" class="btn-primary" @click="handleSave">저장하기</button>
          <button v-if="!isEditing" class="btn-outline" @click="router.push('/host')">돌아가기</button>
        </div>
      </div>
    </div>

    <!-- Form Content -->
    <div class="form-content">
      <section class="form-section">
        <h3 class="section-title">기본정보</h3>
        
        <div class="form-group">
          <label>숙소명</label>
          <input v-model="form.name" type="text" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>숙소유형</label>
          <select v-model="form.type" :disabled="!isEditing">
            <option v-for="t in accommodationTypes" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        
        <div class="form-group">
          <label>숙소 소개</label>
          <textarea v-model="form.description" rows="4" :readonly="!isEditing"></textarea>
        </div>
        
        <div class="form-group">
          <label>대표 연락처</label>
          <input v-model="form.phone" type="tel" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>이메일</label>
          <input v-model="form.email" type="email" :readonly="!isEditing" />
        </div>
      </section>

      <section class="form-section">
        <h3 class="section-title">위치정보</h3>
        
        <div class="form-group">
          <label>시/도</label>
          <input v-model="form.city" type="text" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>구/군</label>
          <input v-model="form.district" type="text" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>상세주소</label>
          <input v-model="form.address" type="text" :readonly="!isEditing" />
        </div>
      </section>

      <section class="form-section">
        <h3 class="section-title">운영 정책</h3>
        
        <div class="form-group">
          <label>체크인 시간</label>
          <input v-model="form.checkInTime" type="time" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>체크아웃 시간</label>
          <input v-model="form.checkOutTime" type="time" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>하우스 룰</label>
          <textarea v-model="form.houseRules" rows="3" :readonly="!isEditing"></textarea>
        </div>
      </section>

      <section class="form-section">
        <h3 class="section-title">정산 계좌</h3>
        
        <div class="form-group">
          <label>은행명</label>
          <select v-model="form.bankName" :disabled="!isEditing">
            <option v-for="b in bankList" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>
        
        <div class="form-group">
          <label>예금주</label>
          <input v-model="form.accountHolder" type="text" :readonly="!isEditing" />
        </div>
        
        <div class="form-group">
          <label>계좌번호</label>
          <input v-model="form.accountNumber" type="text" :readonly="!isEditing" />
        </div>
      </section>

      <!-- 객실 정보 -->
      <section class="form-section">
        <h3 class="section-title">등록된 객실</h3>
        
        <div v-if="rooms.length > 0" class="room-list">
          <div v-for="room in rooms" :key="room.id" class="room-item">
            <div class="room-card" :class="{ 'editing': editingRoomId === room.id }">
              <div class="room-info">
                <h4 class="room-name">{{ room.name }}</h4>
                <div class="room-details">
                  <div class="detail-row">
                    <span class="detail-label">주중 요금</span>
                    <span class="detail-value">₩{{ formatPrice(room.weekdayPrice) }}</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">주말 요금 (금~토)</span>
                    <span class="detail-value">₩{{ formatPrice(room.weekendPrice) }}</span>
                  </div>
                  <div class="detail-row">
                     <span class="detail-label">인원</span>
                     <span class="detail-value">{{ room.minGuests }}~{{ room.maxGuests }}명</span>
                  </div>
                  <div class="detail-row">
                     <span class="detail-label">침대/욕실</span>
                     <span class="detail-value">침대 {{ room.bedCount }}개 | 욕실 {{ room.bathroomCount }}개</span>
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
              <!-- Edit/Delete Buttons (Edit Mode Only) -->
              <div v-if="isEditing && editingRoomId !== room.id" class="room-actions">
                <button class="room-btn edit-btn" @click="editRoom(room)">수정</button>
                <button class="room-btn delete-btn" @click="deleteRoom(room.id)">삭제</button>
              </div>
            </div>

            <!-- 해당 객실 수정 폼 (카드 바로 밑에 펼쳐짐) -->
            <div v-if="editingRoomId === room.id" class="room-edit-form">
              <div class="form-group">
                <label>객실명 <span class="required">*</span></label>
                <input v-model="roomForm.name" type="text" placeholder="예: 스탠다드 더블룸" />
              </div>

              <div class="form-group">
                <label>객실 대표 이미지</label>
                <div class="image-upload-area">
                  <div v-if="roomForm.representativeImagePreview" class="image-preview">
                    <img :src="roomForm.representativeImagePreview" alt="객실 대표 이미지" />
                    <button type="button" class="remove-image-btn" @click="removeRoomImage">✕</button>
                  </div>
                  <label v-else class="upload-box">
                    <input type="file" accept="image/*" @change="handleRoomImageUpload" class="hidden-input" />
                    <div class="upload-content">
                      <span class="upload-icon">📷</span>
                      <span class="upload-text">이미지 업로드</span>
                      <span class="upload-hint">JPG, PNG (최대 5MB)</span>
                    </div>
                  </label>
                </div>
              </div>

              <div class="form-row two-col">
                <div class="form-group">
                  <label>주중 요금 (일~목) <span class="required">*</span></label>
                  <div class="input-with-unit">
                    <input v-model="roomForm.weekdayPrice" type="number" placeholder="50000" />
                    <span class="unit">원</span>
                  </div>
                </div>
                <div class="form-group">
                  <label>주말 요금 (금~토) <span class="required">*</span></label>
                  <div class="input-with-unit">
                    <input v-model="roomForm.weekendPrice" type="number" placeholder="70000" />
                    <span class="unit">원</span>
                  </div>
                </div>
              </div>

              <div class="form-row two-col">
                <div class="form-group">
                  <label>최소 인원</label>
                  <input v-model="roomForm.minGuests" type="number" placeholder="명" />
                </div>
                <div class="form-group">
                  <label>최대 인원</label>
                  <input v-model="roomForm.maxGuests" type="number" placeholder="명" />
                </div>
              </div>

              <div class="form-row two-col">
                <div class="form-group">
                  <label>침대 개수</label>
                  <input v-model="roomForm.bedCount" type="number" placeholder="개" />
                </div>
                <div class="form-group">
                  <label>욕실 개수</label>
                  <input v-model="roomForm.bathroomCount" type="number" placeholder="개" />
                </div>
              </div>

              <div class="form-group">
                <label>객실 설명</label>
                <textarea v-model="roomForm.description" rows="3" placeholder="객실의 특징, 편의시설, 전망 등을 상세히 입력해 주세요."></textarea>
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
                <button class="btn-outline" @click="resetRoomForm">취소</button>
                <button class="btn-primary" @click="updateRoom">수정 완료</button>
              </div>
            </div>
          </div>
        </div>
        
        <p v-else class="no-rooms-text">등록된 객실이 없습니다.</p>

        <!-- Add Room Button (Edit Mode Only) -->
        <button 
          v-if="isEditing && !showRoomForm" 
          class="add-room-btn" 
          @click="showRoomForm = true"
        >
          + 객실 추가하기
        </button>
        
        <!-- Room Add/Edit Form -->
        <div v-if="showRoomForm" class="room-form">
          <h4 class="room-form-title">{{ editingRoomId ? '객실 정보 수정' : '새 객실 정보' }}</h4>
          
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
            <div class="image-upload-area">
              <div v-if="roomForm.representativeImagePreview" class="image-preview">
                <img :src="roomForm.representativeImagePreview" alt="객실 대표 이미지" />
                <button type="button" class="remove-image-btn" @click="removeRoomImage">
                  ✕
                </button>
              </div>
              <label v-else class="upload-box">
                <input
                  type="file"
                  accept="image/*"
                  @change="handleRoomImageUpload"
                  class="hidden-input"
                />
                <div class="upload-content">
                  <span class="upload-icon">📷</span>
                  <span class="upload-text">이미지 업로드</span>
                  <span class="upload-hint">JPG, PNG (최대 5MB)</span>
                </div>
              </label>
            </div>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>주중 요금 (일~목) <span class="required">*</span></label>
              <div class="input-with-unit">
                <input
                  v-model="roomForm.weekdayPrice"
                  type="number"
                  placeholder="50000"
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
                  placeholder="70000"
                />
                <span class="unit">원</span>
              </div>
            </div>
          </div>
          
          <div class="form-row two-col">
            <div class="form-group">
              <label>최소 인원</label>
              <input
                v-model="roomForm.minGuests"
                type="number"
                placeholder="명"
              />
            </div>
            <div class="form-group">
              <label>최대 인원</label>
              <input
                v-model="roomForm.maxGuests"
                type="number"
                placeholder="명"
              />
            </div>
          </div>

          <div class="form-row two-col">
            <div class="form-group">
              <label>침대 개수</label>
              <input
                v-model="roomForm.bedCount"
                type="number"
                placeholder="개"
              />
            </div>
            <div class="form-group">
              <label>욕실 개수</label>
              <input
                v-model="roomForm.bathroomCount"
                type="number"
                placeholder="개"
              />
            </div>
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
            <button class="btn-outline" @click="resetRoomForm">취소</button>
            <button class="btn-primary" @click="editingRoomId ? updateRoom() : addRoom()">
              {{ editingRoomId ? '수정 완료' : '추가' }}
            </button>
          </div>
        </div>
      </section>
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
.edit-page {
  background: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 2rem;
}

.page-header {
  background: white;
  padding: 1.5rem;
  margin: 1rem;
  max-width: 570px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

@media (min-width: 768px) {
  .page-header {
    margin: 1rem auto;
  }
}

.title-area h1 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #BFE7DF;
  margin: 0;
}

.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
}

.toggle-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.toggle-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
}

.toggle-switch {
  width: 48px;
  height: 24px;
  background: #ddd;
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
  left: 26px;
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
  font-size: 1.1rem;
  font-weight: 700;
  color: #BFE7DF;
  margin: 0 0 1rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  box-sizing: border-box;
}

.form-group input:read-only,
.form-group textarea:read-only {
  background: #f5f5f5;
  color: #666;
}

.form-group select:disabled {
  background: #f5f5f5;
  color: #666;
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

/* Room List */
.room-list {
  display: flex;
  flex-direction: column;
}

.room-card {
  background: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 1rem;
}

.room-name {
  font-size: 1rem;
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

.no-rooms-text {
  text-align: center;
  color: #888;
  padding: 2rem;
}

.room-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.room-btn {
  padding: 0.4rem 0.8rem;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 6px;
  font-size: 0.8rem;
  cursor: pointer;
}

.room-btn.edit-btn {
  color: #004d40;
  border-color: #BFE7DF;
}

.room-btn.edit-btn:hover {
  background: #f5fcfa;
  border-color: #8fd4c7;
}

.room-btn.delete-btn {
  color: #ff5252;
}

.room-btn.delete-btn:hover {
  background: #fff5f5;
  border-color: #ff5252;
}

/* Room Item (카드 + 수정폼 감싸는 컨테이너) */
.room-item {
  margin-bottom: 1rem;
}

.room-card.editing {
  border-color: #BFE7DF;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  border-bottom: none;
}

/* Room Edit Form (카드 바로 밑에 펼쳐지는 수정 폼) */
.room-edit-form {
  background: #f8fffe;
  border: 1px solid #BFE7DF;
  border-top: none;
  border-radius: 0 0 12px 12px;
  padding: 1.5rem;
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
  margin-top: 1rem;
}

.add-room-btn:hover {
  background: #f5fcfa;
}

/* Room Form */
.room-form {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 1.5rem;
  margin-top: 1rem;
}

.room-form-title {
  font-size: 1rem;
  font-weight: 700;
  color: #222;
  margin: 0 0 1.5rem;
}

.room-form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.room-form-actions .btn-outline,
.room-form-actions .btn-primary {
  flex: 1;
  padding: 0.75rem;
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

/* Room Details */
.detail-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.25rem;
  font-size: 0.9rem;
}

.detail-label {
  color: #666;
}

.detail-value {
  font-weight: 600;
  color: #333;
}

/* Image Upload Styles */
.image-upload-area {
  width: 100%;
}

.upload-box {
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
  background: #f0fbf9;
  border-color: #8fd4c7;
}

.hidden-input {
  display: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.upload-icon {
  font-size: 2rem;
}

.upload-text {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
}

.upload-hint {
  font-size: 0.8rem;
  color: #888;
}

.image-preview {
  position: relative;
  width: 100%;
  max-width: 200px;
}

.image-preview img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
}

.remove-image-btn {
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
  transition: background 0.2s;
}

.remove-image-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.required {
  color: #ff5252;
}

/* Hide number input spin buttons */
input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
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
</style>
