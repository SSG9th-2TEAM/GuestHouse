<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { signup, checkEmailDuplicate } from '@/api/authClient'

const router = useRouter()
const currentStep = ref(1)
const isLoading = ref(false)
const isEmailChecked = ref(false)
const emailCheckMessage = ref('')

// Step 1: Terms Agreement
const allAgreed = ref(false)
const terms = ref([
  { id: 1, label: '(필수) 서비스 이용약관', required: true, checked: false, content: `
    제1조 (목적)<br/>
    본 약관은 [회사명]이 제공하는 게스트하우스 예약 플랫폼 서비스(이하 "서비스")의 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.<br/><br/>

    제2조 (서비스의 내용)<br/>
    서비스는 게스트하우스 정보 검색, 예약 및 결제, 후기 작성, 마이페이지를 통한 예약 관리 등의 기능을 제공합니다. 회사는 통신판매중개자로서, 실제 숙박 서비스는 호스트(숙소 운영자)가 제공합니다.<br/><br/>

    제3조 (회원의 의무)<br/>
    1. 회원은 본 약관 및 관계 법령을 준수해야 합니다.<br/>
    2. 회원은 정확한 정보를 제공해야 하며, 허위 정보로 인해 발생하는 불이익에 대한 책임은 회원에게 있습니다.<br/>
    3. 회원은 서비스 이용과 관련하여 다음 행위를 하여서는 안 됩니다:<br/>
       - 허위 정보 등록<br/>
       - 타인의 정보 도용<br/>
       - 서비스의 정상적인 운영 방해<br/>
       - 회사의 지식재산권 침해<br/>
       - 기타 불법적이거나 부당한 행위<br/><br/>

    제4조 (예약 및 결제)<br/>
    1. 회원은 서비스를 통해 게스트하우스 예약을 요청할 수 있으며, 회사는 호스트를 대신하여 예약 접수 및 결제를 중개합니다.<br/>
    2. 예약 확정 및 결제 완료 시, 회원과 호스트 간에 숙박 계약이 체결됩니다.<br/>
    3. 예약 취소 및 환불 정책은 각 숙소의 정책 또는 회사의 표준 정책에 따르며, 회원은 이를 확인 후 예약해야 합니다.<br/><br/>

    제5조 (후기 작성 및 관리)<br/>
    1. 회원은 숙박 서비스 이용 후 숙소에 대한 후기(리뷰)를 작성할 수 있습니다.<br/>
    2. 후기 내용은 객관적이고 사실에 기반해야 하며, 욕설, 비방, 허위 사실 유포 등 타인의 권리를 침해하거나 서비스의 건전한 운영을 저해하는 내용은 삭제될 수 있습니다.<br/><br/>

    제6조 (서비스 이용 제한)<br/>
    회사는 회원이 본 약관의 의무를 위반하거나 서비스의 정상적인 운영을 방해하는 경우, 서비스 이용을 제한하거나 회원 자격을 상실시킬 수 있습니다.<br/><br/>

    제7조 (면책 조항)<br/>
    회사는 천재지변, 불가항력 또는 회원의 귀책사유로 인한 서비스 이용의 장애에 대하여는 책임을 지지 않습니다. 또한, 회사는 통신판매중개자로서 호스트와 회원 간의 직접적인 숙박 계약에 대한 일차적인 책임을 지지 않습니다.<br/><br/>

    (본 약관의 상세 내용은 [회사명] 웹사이트에 게시된 전문을 참고하여 주십시오.)
    ` },
  { id: 2, label: '(필수) 개인정보 처리방침', required: true, checked: false, content: `
    제1조 (개인정보의 수집 및 이용 목적)<br/>
    [회사명] (이하 "회사")는 게스트하우스 예약 서비스 제공을 위해 다음 목적에 따라 최소한의 개인정보를 수집 및 이용합니다.<br/>
    1. 회원 가입 및 서비스 이용(예약, 결제, 후기 작성 등)<br/>
    2. 불만 처리 등 고객 상담<br/>
    3. 마케팅 및 광고 (선택 동의 시)<br/><br/>

    제2조 (수집하는 개인정보 항목)<br/>
    회사는 원활한 서비스 제공을 위해 다음과 같은 개인정보를 수집할 수 있습니다.<br/>
    1. **회원 가입 시**: 이메일 주소(ID), 비밀번호, 휴대전화 번호, 관심 테마 정보(선택)<br/>
    2. **예약 및 결제 시**: 예약자 이름, 숙박 인원, 연락처, 결제 정보(카드사, 카드번호 일부 등)<br/>
    3. **서비스 이용 시**: IP 주소, 서비스 이용 기록, 접속 로그, 쿠키 등<br/><br/>

    제3조 (개인정보의 보유 및 이용 기간)<br/>
    회원의 개인정보는 원칙적으로 개인정보 수집 및 이용 목적이 달성되면 지체 없이 파기합니다. 단, 관계 법령의 규정에 의하여 보존할 필요가 있는 경우, 회사는 관계 법령에서 정한 일정한 기간 동안 회원정보를 보관합니다.<br/><br/>

    제4조 (개인정보의 제3자 제공)<br/>
    회사는 회원의 개인정보를 "제1조 (개인정보의 수집 및 이용 목적)"에서 고지한 범위 내에서만 사용하며, 회원의 사전 동의 없이 동 범위를 초과하여 이용하거나 원칙적으로 외부에 제공하지 않습니다. 다만, 예약 서비스 제공을 위해 필요한 최소한의 정보(예약자 이름, 연락처, 숙박 인원 등)는 해당 호스트에게 제공될 수 있습니다.<br/><br/>

    제5조 (개인정보의 파기 절차 및 방법)<br/>
    회사는 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 지체 없이 해당 개인정보를 파기합니다. 전자적 파일 형태의 정보는 기록을 재생할 수 없는 기술적 방법을 사용하며, 종이 문서 형태의 정보는 분쇄기로 분쇄하거나 소각하여 파기합니다.<br/><br/>

    (본 개인정보 처리방침의 상세 내용은 [회사명] 웹사이트에 게시된 전문을 참고하여 주십시오.)
    ` },
  { id: 4, label: '(필수) 만 19세 이상 확인', required: true, checked: false, content: `
    연령 확인 안내 (필수)<br/><br/>
    본 서비스는 청소년보호법 등 관련 법령에 따라 만 19세 미만 청소년의 숙박 예약 및 이용을 제한하고 있습니다.<br/><br/>
    이에 회원으로 가입하고 서비스를 이용하기 위해서는 본인이 만 19세 이상임을 확인하고 동의해야 합니다.<br/><br/>
    만 19세 미만의 자가 허위의 정보로 가입 및 예약을 진행하여 발생하는 모든 문제에 대한 책임은 회원 본인 및 법정대리인에게 있습니다.
    ` },
  { id: 3, label: '(선택) 마케팅 정보 수신 동의', required: false, checked: false, content: `
    마케팅 정보 수신 동의 (선택)<br/><br/>

    [회사명]은(는) 회원님께 더 유용하고 맞춤화된 서비스 및 혜택을 제공하기 위해 마케팅 정보를 발송하고자 합니다.<br/><br/>

    **동의하시는 경우, 아래와 같은 정보를 받아보실 수 있습니다.**<br/>
    1. 신규 숙소 및 추천 상품 정보: 회원님의 관심사에 맞는 새로운 게스트하우스 또는 특별 할인 상품 소식<br/>
    2. 이벤트 및 프로모션: 쿠폰, 할인 행사, 시즌별 특별 이벤트 등 회원님께 유리한 혜택 정보<br/>
    3. 서비스 관련 소식: 서비스 업데이트, 새로운 기능 소개 등 서비스 이용에 도움이 되는 정보<br/><br/>

    수신 채널: 이메일, 문자메시지(SMS/MMS), 앱 푸시 알림<br/><br/>

    본 동의는 선택 사항이며, 동의하지 않으셔도 서비스의 기본 기능 이용에는 어떠한 제한도 없습니다. 마케팅 정보 수신 동의 여부는 "마이페이지 > 개인정보 관리"에서 언제든지 변경하거나 철회할 수 있습니다. 동의를 철회하시는 경우에도 법령에 따른 의무 이행을 위한 정보 및 비마케팅성 정보는 계속 발송될 수 있습니다.<br/><br/>

    (본 마케팅 정보 수신 동의에 대한 상세 내용은 [회사명] 웹사이트에 게시된 전문을 참고하여 주십시오.)
    ` }
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

// Terms Modal State
const showTermsModal = ref(false)
const termsModalTitle = ref('')
const termsModalContent = ref('')

const openTermsModal = (term) => {
  termsModalTitle.value = term.label
  termsModalContent.value = term.content
  showTermsModal.value = true
}

const closeTermsModal = () => {
  showTermsModal.value = false
}

// Step 2: User Info
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const phone = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

// Password validation
const passwordCriteria = ref([
  { label: '영문', valid: false, regex: /[a-zA-Z]/ },
  { label: '숫자', valid: false, regex: /[0-9]/ },
  { label: '특수문자', valid: false, regex: /[!@#$%^&*(),.?":{}|<>]/ },
  { label: '8자 이상 20자 이하', valid: false, regex: /^.{8,20}$/ }
])

const allPasswordCriteriaMet = computed(() => passwordCriteria.value.every(c => c.valid))

const passwordMatchState = computed(() => {
  if (!passwordConfirm.value) {
    return null // Don't show any message if the confirm field is empty
  }
  if (password.value === passwordConfirm.value) {
    return { valid: true, message: '비밀번호가 일치합니다.' }
  } else {
    return { valid: false, message: '비밀번호가 일치하지 않습니다.' }
  }
})

watch(password, (newValue) => {
  passwordCriteria.value.forEach(criterion => {
    criterion.valid = criterion.regex.test(newValue)
  })
})

// 핸드폰 번호
const isPhoneValid = ref(false)
const phoneErrorMessage = ref('')

watch(phone, (newValue) => {
  let cleaned = newValue.replace(/\D/g, '') // Remove all non-digits
  let formatted = ''

  if (cleaned.startsWith('010') && cleaned.length > 3) {
    if (cleaned.length < 8) {
      formatted = cleaned.substring(0, 3) + '-' + cleaned.substring(3)
    } else {
      formatted = cleaned.substring(0, 3) + '-' + cleaned.substring(3, 7) + '-' + cleaned.substring(7, 11)
    }
  } else if (cleaned.length > 0) {
    formatted = cleaned;
  }
  
  if (formatted !== newValue) {
    phone.value = formatted
  }
  validatePhone()
})

const validatePhone = () => {
  const phoneRegex = /^010-\d{4}-\d{4}$/;
  if (!phone.value) {
    isPhoneValid.value = false;
    phoneErrorMessage.value = '전화번호를 입력해주세요.';
  } else if (!phoneRegex.test(phone.value)) {
    isPhoneValid.value = false;
    phoneErrorMessage.value = '올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)';
  } else {
    isPhoneValid.value = true;
    phoneErrorMessage.value = '';
  }
}

// Email Verification State
const verificationCode = ref('')
const isCodeSent = ref(false)
const isCodeVerified = ref(false)
const timer = ref(0)
let timerId = null

const isStep2Valid = computed(() => {
  return isEmailChecked.value && 
         isCodeVerified.value &&
         password.value &&
         password.value === passwordConfirm.value &&
         allPasswordCriteriaMet.value &&
         isPhoneValid.value
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

const handleConfirmCode = () => {
  // TODO: Replace with actual API call to verify code
  if (verificationCode.value === '123456') { // Simulate successful verification
    isCodeVerified.value = true
    stopTimer()
    emailCheckMessage.value = '이메일 인증이 완료되었습니다.'
    openModal('인증에 성공했습니다.', 'success')
  } else {
    isCodeVerified.value = false
    openModal('인증번호가 올바르지 않습니다.', 'error')
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
    if (!isCodeVerified.value) {
      openModal('이메일 인증을 완료해주세요.', 'error')
    } else if (!password.value || !passwordConfirm.value || !phone.value) {
      openModal('비밀번호와 전화번호를 모두 입력해주세요.', 'error')
    } else if (password.value !== passwordConfirm.value) {
      openModal('비밀번호가 일치하지 않습니다.', 'error')
    } else {
      currentStep.value = 3
    }
  }
}

const startTimer = () => {
  stopTimer() // Ensure no multiple timers are running
  timer.value = 180 // 3 minutes
  timerId = setInterval(() => {
    if (timer.value > 0) {
      timer.value--
    } else {
      stopTimer()
    }
  }, 1000)
}

const stopTimer = () => {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
}

const formattedTimer = computed(() => {
  const minutes = Math.floor(timer.value / 60)
  const seconds = timer.value % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
})

// 이메일 인증코드 전송
const handleSendCode = async () => {
  if (!email.value) {
    openModal('이메일을 입력해주세요.', 'error')
    return
  }
  // Basic email format validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email.value)) {
    openModal('올바른 이메일 형식이 아닙니다.', 'error');
    return;
  }

  isEmailChecked.value = false;
  isCodeSent.value = false;
  isCodeVerified.value = false;

  try {
    const response = await checkEmailDuplicate(email.value)
    if (response.data === true) {
      emailCheckMessage.value = '이미 사용 중인 이메일입니다.'
      openModal('이미 사용 중인 이메일입니다.', 'error')
    } else if (response.data === false) {
      isEmailChecked.value = true;
      emailCheckMessage.value = '인증번호가 발송되었습니다.'
      // Simulate sending code
      // TODO: Replace with actual API call to send verification code
      isCodeSent.value = true
      startTimer()
      openModal('인증번호가 발송되었습니다. 이메일을 확인해주세요.', 'success')
    } else {
      openModal('이메일 중복 확인 중 오류가 발생했습니다.', 'error')
    }
  } catch (error) {
    console.error('이메일 중복 확인 에러:', error)
    openModal('이메일 중복 확인 중 오류가 발생했습니다.', 'error')
  }
}

const handleComplete = async () => {
  if (!isEmailChecked.value) {
    openModal('이메일 중복 확인을 해주세요.', 'error')
    return
  }

  isLoading.value = true

  try {
    // 선택된 테마 ID 추출
    const selectedThemeIds = themes.value
      .filter(theme => theme.selected)
      .map(theme => theme.id)

    // 마케팅 동의 확인
    const marketingTerm = terms.value.find(t => t.id === 3)
    const marketingAgreed = marketingTerm ? marketingTerm.checked : false

    // 회원가입 데이터 생성
    const signupData = {
      email: email.value,
      password: password.value,
      phone: phone.value,
      themeIds: selectedThemeIds.length > 0 ? selectedThemeIds : null,
      marketingAgreed: marketingAgreed
    }

    console.log('회원가입 데이터:', signupData)

    // API 호출
    const response = await signup(signupData)

    console.log('회원가입 응답:', response)
    console.log('response.ok:', response.ok)
    console.log('response.status:', response.status)
    console.log('response.data:', response.data)

    if (response.ok && response.data) {
      // 회원가입 성공
      console.log('회원가입 성공!')
      openModal('회원가입이 완료되었습니다!', 'success', () => router.push('/login'))
    } else {
      // 회원가입 실패
      console.error('회원가입 실패:', response)
      openModal('회원가입에 실패했습니다.\n잠시 후 다시 시도해주세요.', 'error')
    }
  } catch (error) {
    console.error('회원가입 에러:', error)
    openModal('회원가입 중 오류가 발생했습니다.', 'error')
  } finally {
    isLoading.value = false
  }
}

const handleSkip = async () => {
  if (!isEmailChecked.value) {
    openModal('이메일 중복 확인을 해주세요.', 'error')
    return
  }

  isLoading.value = true

  try {
    // 테마 선택 없이 회원가입
    const marketingTerm = terms.value.find(t => t.id === 3)
    const marketingAgreed = marketingTerm ? marketingTerm.checked : false

    const signupData = {
      email: email.value,
      password: password.value,
      phone: phone.value,
      themeIds: null,
      marketingAgreed: marketingAgreed
    }

    console.log('회원가입 데이터 (건너뛰기):', signupData)

    const response = await signup(signupData)

    console.log('회원가입 응답 (건너뛰기):', response)

    if (response.ok && response.data) {
      console.log('회원가입 성공 (건너뛰기)!')
      openModal('회원가입이 완료되었습니다!', 'success', () => router.push('/login'))
    } else {
      console.error('회원가입 실패 (건너뛰기):', response)
      openModal('회원가입에 실패했습니다.\n잠시 후 다시 시도해주세요.', 'error')
    }
  } catch (error) {
    console.error('회원가입 에러:', error)
    openModal('회원가입 중 오류가 발생했습니다.', 'error')
  } finally {
    isLoading.value = false
  }
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
              <button type="button" class="view-term-btn" @click.stop="openTermsModal(term)">보기</button>
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
              <input type="email" v-model="email" placeholder="example@email.com" :disabled="isCodeSent" @input="isCodeSent = false; isCodeVerified = false; isEmailChecked = false;" />
              <button type="button" class="check-btn" @click="handleSendCode" :disabled="isCodeSent">
                인증번호 전송
              </button>
            </div>
            <span v-if="emailCheckMessage" class="email-check-message" :class="{ success: isCodeVerified, error: !isCodeVerified && emailCheckMessage }">
              {{ emailCheckMessage }}
            </span>
          </div>

          <div v-if="isCodeSent" class="input-group">
            <label>인증번호 *</label>
            <div class="input-row">
              <input type="text" v-model="verificationCode" placeholder="인증번호 6자리를 입력하세요" :disabled="isCodeVerified" />
              <span v-if="timer > 0" class="timer">{{ formattedTimer }}</span>
              <button type="button" class="check-btn" @click="handleConfirmCode" :disabled="isCodeVerified || timer === 0">
                {{ isCodeVerified ? '인증완료' : '인증번호 확인' }}
              </button>
            </div>
          </div>

          <div class="input-group">
            <label>비밀번호 *</label>
            <div class="input-wrapper">
              <input :type="showPassword ? 'text' : 'password'" v-model="password" placeholder="비밀번호를 입력하세요" />
              <button type="button" class="toggle-btn" @click="showPassword = !showPassword">
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/><line x1="2" x2="22" y1="2" y2="22"/></svg>
              </button>
            </div>
            <div class="password-criteria">
              <span v-for="criterion in passwordCriteria" :key="criterion.label" :class="{ 'is-valid': criterion.valid }">
                <svg v-if="criterion.valid" class="check-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                <span class="criteria-label">{{ criterion.label }}</span>
              </span>
            </div>
          </div>

          <div class="input-group">
            <label>비밀번호 확인 *</label>
            <div class="input-wrapper">
              <input :type="showPasswordConfirm ? 'text' : 'password'" v-model="passwordConfirm" placeholder="비밀번호를 다시 입력하세요" />
              <button type="button" class="toggle-btn" @click="showPasswordConfirm = !showPasswordConfirm">
                <svg v-if="showPasswordConfirm" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/><line x1="2" x2="22" y1="2" y2="22"/></svg>
              </button>
            </div>
            <span v-if="passwordMatchState" class="email-check-message" :class="{ 'success-match': passwordMatchState.valid, 'error': !passwordMatchState.valid }">
              {{ passwordMatchState.message }}
            </span>
          </div>

          <div class="input-group">
            <label>전화번호 *</label>
            <input type="tel" v-model="phone" placeholder="전화번호를 입력하세요" @blur="validatePhone" />
            <span v-if="phoneErrorMessage && !isPhoneValid" class="email-check-message error">
              {{ phoneErrorMessage }}
            </span>
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
          <button class="complete-btn" @click="handleComplete" :disabled="isLoading">
            {{ isLoading ? '처리 중...' : '완료' }}
          </button>
          <button class="skip-btn" @click="handleSkip" :disabled="isLoading">
            {{ isLoading ? '처리 중...' : '건너뛰기' }}
          </button>
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

    <!-- Terms Modal -->
    <div v-if="showTermsModal" class="modal-overlay" @click.self="closeTermsModal">
      <div class="modal-content large">
        <h2>{{ termsModalTitle }}</h2>
        <div class="terms-content-scroll">
          <div v-html="termsModalContent"></div>
        </div>
        <button class="modal-btn" @click="closeTermsModal">닫기</button>
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

.view-term-btn {
  background: none;
  border: none;
  color: #00796b;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.view-term-btn:hover {
  background: #f0f0f0;
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

.input-row .timer {
  display: flex;
  align-items: center;
  padding: 0 1rem;
  font-size: 0.9rem;
  color: #ef4444;
  white-space: nowrap;
}

.check-btn {
  padding: 0 1rem;
  background: #BFE7DF;
  color: #333;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.check-btn.checked {
  background: #BFE7DF;
  color: #333;
  cursor: default;
}

.email-check-message {
  font-size: 0.85rem;
  margin-top: 0.5rem;
  display: block;
}

.email-check-message.success {
  color: #2e7d32;
}

.email-check-message.success-match {
  color: #00796b;
}

.email-check-message.error {
  color: #dc2626;
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
  font-size: 0; /* Hide text fallback */
  color: #9ca3af;
}

.toggle-btn:hover {
  color: #374151;
}

.toggle-btn svg {
  transition: color 0.2s;
}

.password-criteria {
  display: flex;
  flex-wrap: wrap; /* Allow items to wrap */
  gap: 0.75rem;
  font-size: 0.9rem; /* Increased font size */
  font-weight: 600; /* Made bolder */
  color: #a1a1aa;
  margin-top: 0.5rem;
}

.password-criteria > span {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  white-space: nowrap; /* Prevent criteria labels from breaking */
}

.password-criteria .is-valid {
  color: #00796b;
}

.password-criteria .is-valid .check-icon {
  color: #00796b;
}

.check-icon {
  width: 16px;
  height: 16px;
  stroke-width: 2.5; /* Make checkmark bolder */
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

.complete-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.7;
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

.skip-btn:disabled {
  background: #f5f5f5;
  color: #9ca3af;
  cursor: not-allowed;
  opacity: 0.7;
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

.modal-content.large {
  max-width: 600px;
  width: 95%;
  padding: 2.5rem;
}

.terms-content-scroll {
  max-height: 400px;
  overflow-y: auto;
  background-color: #f9f9f9;
  border: 1px solid #eee;
  padding: 1rem;
  margin-bottom: 1.5rem;
  text-align: left;
  line-height: 1.6;
  font-size: 0.9rem;
  color: #555;
}

.modal-content h2 {
  font-size: 1.3rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  color: #333;
}

.modal-content p {
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
