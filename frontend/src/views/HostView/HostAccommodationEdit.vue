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
    type: '더블',
    weekdayPrice: 50000,
    weekendPrice: 70000,
    maxGuests: 2,
    size: 20,
    isActive: true
  },
  {
    id: 2,
    name: '디럭스 트윈룸',
    type: '트윈',
    weekdayPrice: 70000,
    weekendPrice: 90000,
    maxGuests: 2,
    size: 25,
    isActive: true
  },
  {
    id: 3,
    name: '패밀리룸',
    type: '패밀리',
    weekdayPrice: 100000,
    weekendPrice: 130000,
    maxGuests: 4,
    size: 35,
    isActive: false
  }
])

// 객실 폼
const showRoomForm = ref(false)
const roomForm = ref({
  name: '',
  type: '',
  weekdayPrice: '',
  weekendPrice: '',
  maxGuests: '',
  size: '',
  description: '',
  isActive: true
})

const roomBedTypes = ['스탠다드', '디럭스', '스위트', '더블', '트윈', '패밀리']

const toggleRoomActive = (id) => {
  if (!isEditing.value) return
  const room = rooms.value.find(r => r.id === id)
  if (room) {
    room.isActive = !room.isActive
  }
}

const addRoom = () => {
  if (!roomForm.value.name || !roomForm.value.type || !roomForm.value.weekdayPrice || !roomForm.value.weekendPrice) {
    openModal('객실 이름, 타입, 주중/주말 요금은 필수입니다.')
    return
  }
  
  rooms.value.push({
    id: Date.now(),
    ...roomForm.value,
    weekdayPrice: Number(roomForm.value.weekdayPrice),
    weekendPrice: Number(roomForm.value.weekendPrice),
    maxGuests: Number(roomForm.value.maxGuests),
    size: Number(roomForm.value.size)
  })
  
  roomForm.value = {
    name: '',
    type: '',
    weekdayPrice: '',
    weekendPrice: '',
    maxGuests: '',
    size: '',
    description: '',
    isActive: true
  }
  showRoomForm.value = false
  openModal('객실이 추가되었습니다.')
}

const deleteRoom = (id) => {
  rooms.value = rooms.value.filter(r => r.id !== id)
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
          <div v-for="room in rooms" :key="room.id" class="room-card">
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
                   <span class="detail-label">인원/크기</span>
                   <span class="detail-value">{{ room.maxGuests }}명 | {{ room.size }}m²</span>
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
            <!-- Delete Button (Edit Mode Only) -->
            <div v-if="isEditing" class="room-actions">
              <button class="room-btn" @click="deleteRoom(room.id)">삭제</button>
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
        
        <!-- Room Add Form -->
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
          
          <div class="room-form-actions">
            <button class="btn-outline" @click="showRoomForm = false">취소</button>
            <button class="btn-primary" @click="addRoom">추가</button>
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
  gap: 1rem;
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
  margin-top: 0.5rem;
}

.room-btn {
  padding: 0.4rem 0.8rem;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 6px;
  font-size: 0.8rem;
  color: #ff5252;
  cursor: pointer;
}

.room-btn:hover {
  background: #fff5f5;
  border-color: #ff5252;
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
</style>
