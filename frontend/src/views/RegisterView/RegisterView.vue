<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentStep = ref(1)

// Step 1: Terms Agreement
const allAgreed = ref(false)
const terms = ref([
  { id: 1, label: '(필수) 서비스 이용약관', required: true, checked: false },
  { id: 2, label: '(필수) 개인정보 처리방침', required: true, checked: false },
  { id: 3, label: '(선택) 마케팅 정보 수신 동의', required: false, checked: false }
])

const toggleAll = () => {
  terms.value.forEach(t => t.checked = allAgreed.value)
}

const updateAllAgreed = () => {
  allAgreed.value = terms.value.every(t => t.checked)
}

const requiredTermsAgreed = computed(() => {
  return terms.value.filter(t => t.required).every(t => t.checked)
})

// Step 2: User Info
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const phone = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

const isStep2Valid = computed(() => {
  return email.value && password.value && passwordConfirm.value && name.value && phone.value && password.value === passwordConfirm.value
})

// Step 3: Theme Selection
const themes = ref([
  { id: 1, label: '액티비티', selected: false },
  { id: 2, label: '맛집', selected: false },
  { id: 3, label: '카페', selected: false },
  { id: 4, label: '문화/예술', selected: false },
  { id: 5, label: '자연/힐링', selected: false },
  { id: 6, label: '쇼핑', selected: false },
  { id: 7, label: '역사/관광', selected: false },
  { id: 8, label: '엔터테인먼트', selected: false },
  { id: 9, label: '스포츠', selected: false },
  { id: 10, label: '음악', selected: false },
  { id: 11, label: '반려동물', selected: false },
  { id: 12, label: '기타', selected: false }
])

const toggleTheme = (theme) => {
  theme.selected = !theme.selected
}

// Modal
const showModal = ref(false)
const modalMessage = ref('')
const modalType = ref('info')
const modalCallback = ref(null)

const openModal = (message, type = 'info', callback = null) => {
  modalMessage.value = message
  modalType.value = type
  modalCallback.value = callback
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (modalCallback.value) {
    modalCallback.value()
    modalCallback.value = null
  }
}

// Navigation
const goBack = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  } else {
    router.back()
  }
}

const goNext = () => {
  if (currentStep.value === 1) {
    if (requiredTermsAgreed.value) {
      currentStep.value = 2
    } else {
      openModal('필수 약관에 동의해주세요.', 'error')
    }
  } else if (currentStep.value === 2) {
    if (!email.value || !password.value || !passwordConfirm.value || !name.value || !phone.value) {
      openModal('모든 필수 항목을 입력해주세요.', 'error')
    } else if (password.value !== passwordConfirm.value) {
      openModal('비밀번호가 일치하지 않습니다.', 'error')
    } else {
      currentStep.value = 3
    }
  }
}

const handleComplete = () => {
  openModal('회원가입이 완료되었습니다!', 'success', () => router.push('/login'))
}

const handleSkip = () => {
  openModal('회원가입이 완료되었습니다!', 'success', () => router.push('/login'))
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <!-- Header -->
      <div class="page-header">
        <button class="back-btn" @click="goBack">←</button>
        <h1>{{ currentStep === 1 ? '약관 동의' : currentStep === 2 ? '회원 정보 입력' : '관심 테마 선택' }}</h1>
      </div>

      <!-- Progress Steps -->
      <div class="progress-steps">
        <div class="step" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
          <span v-if="currentStep > 1">✓</span>
          <span v-else>1</span>
        </div>
        <div class="step-line" :class="{ active: currentStep >= 2 }"></div>
        <div class="step" :class="{ active: currentStep >= 2, done: currentStep > 2 }">
          <span v-if="currentStep > 2">✓</span>
          <span v-else>2</span>
        </div>
        <div class="step-line" :class="{ active: currentStep >= 3 }"></div>
        <div class="step" :class="{ active: currentStep >= 3 }">
          <span>3</span>
        </div>
      </div>
      <div class="step-labels">
        <span :class="{ active: currentStep === 1 }">약관동의</span>
        <span :class="{ active: currentStep === 2 }">정보입력</span>
        <span :class="{ active: currentStep === 3 }">테마선택</span>
      </div>

      <!-- Step 1: Terms -->
      <template v-if="currentStep === 1">
        <div class="terms-section">
          <label class="all-agree">
            <input type="checkbox" v-model="allAgreed" @change="toggleAll" />
            <span>전체 동의</span>
          </label>
          <hr class="divider" />
          
          <div class="term-list">
            <label v-for="term in terms" :key="term.id" class="term-row">
              <input type="checkbox" v-model="term.checked" @change="updateAllAgreed" />
              <span>{{ term.label }}</span>
              <span class="arrow">›</span>
            </label>
          </div>
        </div>

        <button class="next-btn" :disabled="!requiredTermsAgreed" @click="goNext">다음</button>
      </template>

      <!-- Step 2: Info -->
      <template v-if="currentStep === 2">
        <div class="form-section">
          <div class="input-group">
            <label>이메일 *</label>
            <div class="input-row">
              <input type="email" v-model="email" placeholder="example@email.com" />
              <button class="check-btn">중복확인</button>
            </div>
          </div>

          <div class="input-group">
            <label>비밀번호 *</label>
            <div class="input-wrapper">
              <input :type="showPassword ? 'text' : 'password'" v-model="password" placeholder="8자 이상, 영문/숫자/특수문자 포함" />
              <button class="toggle-btn" @click="showPassword = !showPassword">{{ showPassword ? '숨김' : '보기' }}</button>
            </div>
          </div>

          <div class="input-group">
            <label>비밀번호 확인 *</label>
            <div class="input-wrapper">
              <input :type="showPasswordConfirm ? 'text' : 'password'" v-model="passwordConfirm" placeholder="비밀번호를 다시 입력하세요" />
              <button class="toggle-btn" @click="showPasswordConfirm = !showPasswordConfirm">{{ showPasswordConfirm ? '숨김' : '보기' }}</button>
            </div>
          </div>

          <div class="input-group">
            <label>이름 *</label>
            <input type="text" v-model="name" placeholder="이름을 입력하세요" />
          </div>

          <div class="input-group">
            <label>전화번호 *</label>
            <input type="tel" v-model="phone" placeholder="010-1234-5678" />
          </div>
        </div>

        <button class="next-btn" :disabled="!isStep2Valid" @click="goNext">다음</button>
      </template>

      <!-- Step 3: Theme Selection -->
      <template v-if="currentStep === 3">
        <div class="theme-section">
          <p class="theme-desc">관심있는 테마를 선택하시면 맞춤형 추천을 받으실 수 있습니다.<br/>(복수 선택 가능)</p>
          
          <div class="theme-grid">
            <button 
              v-for="theme in themes" 
              :key="theme.id" 
              class="theme-btn"
              :class="{ selected: theme.selected }"
              @click="toggleTheme(theme)"
            >
              <span class="theme-checkbox">{{ theme.selected ? '☑' : '☐' }}</span>
              <span>{{ theme.label }}</span>
            </button>
          </div>
        </div>

        <div class="final-actions">
          <button class="complete-btn" @click="handleComplete">완료</button>
          <button class="skip-btn" @click="handleSkip">건너뛰기</button>
        </div>
      </template>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-icon" :class="modalType">
          <span v-if="modalType === 'success'">✓</span>
          <span v-else-if="modalType === 'error'">!</span>
          <span v-else>i</span>
        </div>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  background: #f9fafb;
  padding: 1rem;
}

.register-container {
  background: white;
  max-width: 500px;
  margin: 0 auto;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-header h1 {
  font-size: 1.2rem;
  font-weight: 700;
}

/* Progress Steps */
.progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.step {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.9rem;
  color: #888;
}

.step.active {
  background: #333;
  color: white;
}

.step.done {
  background: var(--primary);
  color: #004d40;
}

.step-line {
  width: 50px;
  height: 2px;
  background: #eee;
}

.step-line.active {
  background: #333;
}

.step-labels {
  display: flex;
  justify-content: space-between;
  max-width: 280px;
  margin: 0 auto 2rem;
  font-size: 0.8rem;
  color: #888;
}

.step-labels span.active {
  color: #2563eb;
  font-weight: 600;
}

/* Terms */
.terms-section {
  margin-bottom: 2rem;
}

.all-agree {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.5rem 0;
}

.all-agree input {
  width: 20px;
  height: 20px;
  accent-color: var(--primary);
}

.divider {
  border: 0;
  border-top: 2px solid #333;
  margin: 1rem 0;
}

.term-list {
  display: flex;
  flex-direction: column;
}

.term-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 0.5rem;
  cursor: pointer;
}

.term-row input {
  width: 18px;
  height: 18px;
  accent-color: var(--primary);
}

.term-row span {
  flex: 1;
  font-size: 0.95rem;
}

.arrow {
  color: #888;
  font-size: 1.2rem;
}

/* Form */
.form-section {
  margin-bottom: 2rem;
}

.input-group {
  margin-bottom: 1.5rem;
}

.input-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.input-group input {
  width: 100%;
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  outline: none;
}

.input-group input:focus {
  border-color: var(--primary);
}

.input-row {
  display: flex;
  gap: 0.5rem;
}

.input-row input {
  flex: 1;
}

.check-btn {
  padding: 0 1rem;
  background: #333;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.input-wrapper {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
}

.input-wrapper input {
  border: none;
  border-radius: 0;
}

.toggle-btn {
  background: none;
  border: none;
  padding: 0 1rem;
  cursor: pointer;
  font-size: 1rem;
}

/* Theme Selection */
.theme-section {
  margin-bottom: 2rem;
}

.theme-desc {
  font-size: 0.9rem;
  color: #2563eb;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.theme-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.theme-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.theme-btn.selected {
  border-color: var(--primary);
}

.theme-checkbox {
  font-size: 1.5rem;
  color: #ccc;
}

.theme-btn.selected .theme-checkbox {
  color: #004d40;
}

/* Buttons */
.next-btn {
  width: 100%;
  padding: 1rem;
  background: var(--primary);
  color: #004d40;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
}

.next-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.final-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.complete-btn {
  width: 100%;
  padding: 1rem;
  background: #333;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
}

.skip-btn {
  width: 100%;
  padding: 1rem;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
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

.modal-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 1.5rem;
  font-weight: bold;
}

.modal-icon.success {
  background: var(--primary);
  color: #004d40;
}

.modal-icon.error {
  background: #fee2e2;
  color: #dc2626;
}

.modal-icon.info {
  background: #e0f2fe;
  color: #0284c7;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 0.8rem;
  background: var(--primary);
  color: #004d40;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
</style>
