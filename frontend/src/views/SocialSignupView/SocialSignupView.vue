<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { completeSocialSignup, saveTokens } from '@/api/authClient'
import { fetchThemes } from '@/api/theme'

const router = useRouter()
const route = useRoute()
const currentStep = ref(1)
const isLoading = ref(false)

// 페이지 로드 시 토큰 저장 및 테마 로딩
onMounted(async () => {
  const accessToken = route.query.accessToken
  const refreshToken = route.query.refreshToken

  if (accessToken && refreshToken) {
    // 토큰 저장
    saveTokens(accessToken, refreshToken)
    console.log('소셜 로그인 토큰 저장 완료')

    // 테마 목록 불러오기
    await loadThemes()
  } else {
    console.error('토큰이 없습니다. 로그인 페이지로 이동합니다.')
    router.push('/login')
  }
})

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

// Step 2: Theme Selection
const themes = ref([])
const themesLoading = ref(false)
const themesError = ref('')

const loadThemes = async () => {
  themesLoading.value = true
  themesError.value = ''
  try {
    const response = await fetchThemes()
    if (response.ok && Array.isArray(response.data)) {
      // 백엔드에서 받은 테마를 프론트엔드 형식으로 변환
      themes.value = response.data.map(theme => ({
        id: theme.id,
        label: theme.themeName,
        imageUrl: theme.themeImageUrl,
        selected: false
      }))
    } else {
      themesError.value = '테마 목록을 불러오지 못했습니다.'
      console.error('테마 목록 로드 실패:', response)
    }
  } catch (error) {
    themesError.value = '테마 목록을 불러오는 중 오류가 발생했습니다.'
    console.error('테마 목록 로드 에러:', error)
  } finally {
    themesLoading.value = false
  }
}

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
    router.push('/login')
  }
}

const goNext = () => {
  if (currentStep.value === 1) {
    if (requiredTermsAgreed.value) {
      currentStep.value = 2
    } else {
      openModal('필수 약관에 동의해주세요.', 'error')
    }
  }
}

const handleComplete = async () => {
  isLoading.value = true

  try {
    // 선택된 테마 ID 추출
    const selectedThemeIds = themes.value
      .filter(theme => theme.selected)
      .map(theme => theme.id)

    // 마케팅 동의 확인
    const marketingTerm = terms.value.find(t => t.id === 3)
    const marketingAgreed = marketingTerm ? marketingTerm.checked : false

    // 소셜 회원가입 데이터 생성
    const signupData = {
      termsAgreed: true,
      themeIds: selectedThemeIds.length > 0 ? selectedThemeIds : null,
      marketingAgreed: marketingAgreed
    }

    console.log('소셜 회원가입 데이터:', signupData)

    // API 호출
    const response = await completeSocialSignup(signupData)

    console.log('소셜 회원가입 응답:', response)

    if (response.ok && response.data) {
      // 회원가입 성공
      console.log('소셜 회원가입 완료!')
      openModal('회원가입이 완료되었습니다!', 'success', () => router.push('/'))
    } else {
      // 회원가입 실패
      console.error('소셜 회원가입 실패:', response)
      openModal('회원가입에 실패했습니다.\n잠시 후 다시 시도해주세요.', 'error')
    }
  } catch (error) {
    console.error('소셜 회원가입 에러:', error)
    openModal('회원가입 중 오류가 발생했습니다.', 'error')
  } finally {
    isLoading.value = false
  }
}

const handleSkip = async () => {
  isLoading.value = true

  try {
    // 테마 선택 없이 회원가입
    const marketingTerm = terms.value.find(t => t.id === 3)
    const marketingAgreed = marketingTerm ? marketingTerm.checked : false

    const signupData = {
      termsAgreed: true,
      themeIds: null,
      marketingAgreed: marketingAgreed
    }

    console.log('소셜 회원가입 데이터 (건너뛰기):', signupData)

    const response = await completeSocialSignup(signupData)

    console.log('소셜 회원가입 응답 (건너뛰기):', response)

    if (response.ok && response.data) {
      console.log('소셜 회원가입 완료 (건너뛰기)!')
      openModal('회원가입이 완료되었습니다!', 'success', () => router.push('/'))
    } else {
      console.error('소셜 회원가입 실패 (건너뛰기):', response)
      openModal('회원가입에 실패했습니다.\n잠시 후 다시 시도해주세요.', 'error')
    }
  } catch (error) {
    console.error('소셜 회원가입 에러:', error)
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
        <button class="back-btn" @click="goBack">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5m7 7l-7-7 7-7"/></svg>
        </button>
        <h1>{{ currentStep === 1 ? '약관 동의' : '관심 테마 선택' }}</h1>
      </div>

      <!-- Progress Steps -->
      <div class="progress-steps">
        <div class="step" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
          <svg v-if="currentStep > 1" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
          <span v-if="currentStep > 1">1</span>
          <span v-else>1</span>
        </div>
        <div class="step-line" :class="{ active: currentStep >= 2 }"></div>
        <div class="step" :class="{ active: currentStep >= 2 }">
          <span>2</span>
        </div>
      </div>
      <div class="step-labels">
        <span :class="{ active: currentStep === 1 }">약관동의</span>
        <span :class="{ active: currentStep === 2 }">테마선택</span>
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

      <!-- Step 2: Theme Selection -->
      <template v-if="currentStep === 2">
        <div class="theme-section">
          <h2 class="theme-title">관심 테마를 선택해주세요</h2>
          <p class="theme-desc">마음에 드는 여행 스타일을 선택하시면<br/>꼭 맞는 숙소를 추천해 드립니다. (여러 개 선택 가능)</p>

          <div v-if="themesLoading" class="theme-loading">
            <div class="spinner"></div>
            <p>테마 목록을 불러오는 중...</p>
          </div>
          <div v-else-if="themesError" class="theme-error">{{ themesError }}</div>
          <div v-else class="theme-grid">
            <div
              v-for="theme in themes"
              :key="theme.id"
              class="theme-card"
              :class="{ selected: theme.selected }"
              @click="toggleTheme(theme)"
            >
              <img :src="theme.imageUrl" :alt="theme.label" class="theme-image" />
              <div class="theme-overlay"></div>
              <div class="theme-label-container">
                <span class="theme-label">{{ theme.label }}</span>
              </div>
              <div class="theme-checkbox-wrapper">
                <div class="theme-checkbox">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="final-actions">
          <button class="complete-btn" @click="handleComplete" :disabled="isLoading || themesLoading">
            {{ isLoading ? '가입 완료 중...' : '선택 완료' }}
          </button>
          <button class="skip-btn" @click="handleSkip" :disabled="isLoading || themesLoading">
            건너뛰기
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
:root {
  --primary-color: #3b82f6;
  --primary-dark: #2563eb;
  --primary-hover: #1d4ed8;
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --background-light: #f3f4f6;
  --background-white: #ffffff;
  --border-color: #e5e7eb;
  --accent-color: #60a5fa;
  --success-color: #10b981;
}

.register-page {
  min-height: 100vh;
  background: #ffffff;
  padding: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-container {
  background: var(--background-white);
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 2rem;
  position: relative;
  justify-content: center;
}

.back-btn {
  position: absolute;
  left: 0;
  background: var(--background-light);
  border: 2px solid var(--border-color);
  cursor: pointer;
  color: var(--text-secondary);
  padding: 0.5rem;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.back-btn:hover {
  background-color: white;
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: translateX(-2px);
}

.page-header h1 {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.03em;
}

/* Progress Steps */
.progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}
.step {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
  color: #1f2937;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.step.active {
  background: var(--primary-color);
  color: #004d40;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}
.step.done {
  background: var(--success-color);
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.step.done span {
  color: white;
}
.step.done svg {
  color: white;
}
.step-line {
  width: 60px;
  height: 3px;
  background: #e5e7eb;
  transition: all 0.3s ease;
  border-radius: 2px;
}
.step-line.active {
  background: var(--primary-color);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.step-labels {
  display: flex;
  justify-content: space-around;
  max-width: 240px;
  margin: 0 auto 2rem;
  font-size: 0.9rem;
  color: var(--text-secondary);
  font-weight: 500;
}
.step-labels span.active {
  color: var(--primary-color);
  font-weight: 700;
}

/* Terms */
.terms-section { margin-bottom: 2rem; }
.all-agree {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
  padding: 1rem;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  border: 2px solid #bae6fd;
  transition: all 0.3s ease;
}
.all-agree:hover {
  border-color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}
.all-agree input {
  width: 22px;
  height: 22px;
  accent-color: var(--primary-color);
  cursor: pointer;
}
.divider {
  border: 0;
  border-top: 2px solid var(--border-color);
  margin: 1.25rem 0;
}
.term-list { display: flex; flex-direction: column; gap: 0.5rem; }
.term-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}
.term-row:hover {
  background-color: #fafbfc;
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
  transform: translateX(4px);
}
.term-row input {
  width: 20px;
  height: 20px;
  accent-color: var(--primary-color);
  cursor: pointer;
}
.term-row span {
  flex: 1;
  font-size: 0.95rem;
  color: var(--text-primary);
  font-weight: 500;
}
.view-term-btn {
  background: var(--background-light);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  padding: 0.4rem 0.8rem;
  border-radius: 6px;
  transition: all 0.2s;
}
.view-term-btn:hover {
  color: var(--primary-color);
  background: white;
  border-color: var(--primary-color);
}

/* Theme Selection */
.theme-section {
  margin-bottom: 2rem;
  text-align: center;
}
.theme-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}
.theme-desc {
  font-size: 0.95rem;
  color: var(--text-secondary);
  margin-bottom: 2rem;
  line-height: 1.6;
}
.theme-loading {
  text-align: center;
  padding: 3rem 0;
  color: var(--text-secondary);
}
.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.theme-error {
  text-align: center;
  padding: 2rem;
  font-size: 0.95rem;
  color: #dc2626;
  background-color: #fee2e2;
  border-radius: 8px;
}
.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 1rem;
}
.theme-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  aspect-ratio: 1 / 1;
  border: 3px solid #e5e7eb;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.theme-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
  border-color: var(--accent-color);
}
.theme-card.selected {
  border-color: #BFE7DF;
  border-width: 4px;
  transform: translateY(-6px) scale(1.03);
  box-shadow: 0 12px 28px rgba(191, 231, 223, 0.5);
}
.theme-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.theme-card:hover .theme-image {
  transform: scale(1.1);
}
.theme-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.1);
  transition: background-color 0.3s ease;
}
.theme-card:hover .theme-overlay {
  background: rgba(0,0,0,0.25);
}
.theme-card.selected .theme-overlay {
  background: linear-gradient(135deg, rgba(191, 231, 223, 0.4) 0%, rgba(109, 195, 187, 0.5) 100%);
}
.theme-label-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.3), transparent);
  padding: 10px 12px;
  transition: all 0.3s ease;
}
.theme-card.selected .theme-label-container {
  background: linear-gradient(to top, rgba(191, 231, 223, 0.95), rgba(191, 231, 223, 0.85));
}
.theme-label {
  color: white;
  font-weight: 700;
  font-size: 0.95rem;
  text-align: center;
  display: block;
  width: 100%;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
}
.theme-card.selected .theme-label {
  color: #004d40;
  text-shadow: none;
}
.theme-checkbox-wrapper {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(8px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  transform: scale(0.5);
  opacity: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.theme-checkbox {
  width: 22px;
  height: 22px;
  background: var(--background-white);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-dark);
  border: 2px solid var(--border-color);
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.theme-card.selected .theme-checkbox-wrapper {
  transform: scale(1);
  opacity: 1;
  background: rgba(255, 255, 255, 0.5);
}
.theme-card.selected .theme-checkbox {
  background: #6DC3BB;
  color: white;
  border-color: white;
  box-shadow: 0 2px 8px rgba(109, 195, 187, 0.4);
}

/* Buttons */
.next-btn, .complete-btn, .skip-btn {
  width: 100%;
  padding: 1rem 1.5rem;
  border-radius: 12px;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  letter-spacing: -0.02em;
}
.next-btn, .complete-btn {
  background: #BFE7DF;
  color: #004d40;
  box-shadow: 0 4px 14px rgba(191, 231, 223, 0.4);
  border: 2px solid transparent;
}
.next-btn:hover:not(:disabled), .complete-btn:hover:not(:disabled) {
  background: #a8ddd2;
  box-shadow: 0 6px 20px rgba(191, 231, 223, 0.5);
  transform: translateY(-2px);
}
.next-btn:active:not(:disabled), .complete-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(191, 231, 223, 0.3);
}
.next-btn:disabled, .complete-btn:disabled {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
  box-shadow: none;
  opacity: 0.6;
}
.final-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.skip-btn {
  background: white;
  border: 2px solid var(--border-color);
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.skip-btn:hover:not(:disabled) {
  background-color: var(--background-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}
.skip-btn:disabled {
  background: #f9fafb;
  color: #9ca3af;
  cursor: not-allowed;
  border-color: #e5e7eb;
  box-shadow: none;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.modal-content {
  background: white;
  border-radius: 20px;
  padding: 2rem;
  max-width: 360px;
  width: 90%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}
@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.modal-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  font-size: 1.8rem;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.modal-icon.success {
  background: linear-gradient(135deg, var(--success-color) 0%, #059669 100%);
  color: white;
}
.modal-icon.error {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}
.modal-icon.info {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
}
.modal-message {
  font-size: 1.05rem;
  color: var(--text-primary);
  margin-bottom: 1.5rem;
  line-height: 1.6;
  white-space: pre-wrap;
  font-weight: 500;
}
.modal-btn {
  width: 100%;
  padding: 0.9rem;
  background: #BFE7DF;
  color: #004d40;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(191, 231, 223, 0.3);
}
.modal-btn:hover {
  background: #a8ddd2;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(191, 231, 223, 0.4);
}
.modal-btn:active {
  transform: translateY(0);
}

.modal-content.large {
  max-width: 600px;
  width: 95%;
  padding: 2rem;
  text-align: left;
}
.modal-content.large h2 {
  font-size: 1.4rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  color: var(--text-primary);
  text-align: center;
}
.terms-content-scroll {
  max-height: 50vh;
  overflow-y: auto;
  background-color: var(--background-light);
  border: 2px solid var(--border-color);
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 1.5rem;
  line-height: 1.7;
  font-size: 0.9rem;
  color: #000000;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}
.terms-content-scroll::-webkit-scrollbar {
  width: 8px;
}
.terms-content-scroll::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}
.terms-content-scroll::-webkit-scrollbar-thumb {
  background: var(--primary-color);
  border-radius: 4px;
}
.terms-content-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--primary-dark);
}

@media (max-width: 400px) {
  .theme-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.75rem;
  }
}
</style>
