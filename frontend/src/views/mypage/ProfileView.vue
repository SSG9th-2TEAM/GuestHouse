<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, updateUserProfile } from '@/api/userClient'

const router = useRouter()
const user = ref({
  name: '',
  email: '',
  phone: '',
  nickname: '',
  role: '',
  gender: ''
})
const editableUser = ref({})
const isLoading = ref(true)
const isEditMode = ref(false)
const error = ref('')
const validationErrors = ref({ nickname: '', phone: '' })
const updateMessage = ref('')
const updateMessageType = ref('') // 'success' or 'error'

watch(() => editableUser.value.phone, (newVal, oldVal) => {
  if (typeof newVal !== 'string') return;
  // This watcher handles auto-hyphenation
  const digits = newVal.replace(/\D/g, '').slice(0, 11);
  let formatted = digits;
  if (digits.length > 3 && digits.length <= 7) {
    formatted = `${digits.slice(0, 3)}-${digits.slice(3)}`;
  } else if (digits.length > 7) {
    formatted = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  }
  // To avoid infinite loops, only update if the formatted value is different
  if (formatted !== newVal) {
    editableUser.value.phone = formatted;
  }
});


const loadUserData = async () => {
  try {
    isLoading.value = true
    const response = await getCurrentUser()

    if (response.ok && response.data) {
      user.value = {
        name: response.data.name || '',
        email: response.data.email || '',
        phone: response.data.phone || '정보 없음',
        nickname: response.data.nickname || '',
        role: response.data.role || 'USER',
        gender: response.data.gender || ''
      }
    } else {
      error.value = '사용자 정보를 불러오지 못했습니다.'
    }
  } catch (err) {
    console.error('Failed to load user data:', err)
    error.value = '사용자 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}

const startEdit = () => {
  editableUser.value = { ...user.value }
  validationErrors.value = { nickname: '', phone: '' } // Clear previous errors
  updateMessage.value = '' // Clear update message
  updateMessageType.value = ''
  isEditMode.value = true
}

const cancelEdit = () => {
  updateMessage.value = '' // Clear update message
  updateMessageType.value = ''
  isEditMode.value = false
}

const validateProfile = () => {
  let isValid = true;
  validationErrors.value = { nickname: '', phone: '' }; // Reset errors

  // Nickname validation
  const nickname = editableUser.value.nickname.trim();
  if (nickname.length < 2 || nickname.length > 10) {
    validationErrors.value.nickname = '닉네임은 2자 이상 10자 이하로 입력해주세요.';
    isValid = false;
  }

  // Phone number validation
  const phoneRegex = /^010-\d{4}-\d{4}$/;
  if (!phoneRegex.test(editableUser.value.phone)) {
    validationErrors.value.phone = '올바른 전화번호 형식이 아닙니다. (010-1234-5678)';
    isValid = false;
  }

  return isValid;
}

const handleProfileUpdate = async () => {
  updateMessage.value = '' // Clear previous message
  updateMessageType.value = ''

  if (!validateProfile()) {
    // If validation fails, do not proceed
    return;
  }

  try {
    const response = await updateUserProfile({
      nickname: editableUser.value.nickname,
      phone: editableUser.value.phone,
    })

    if (response.ok) {
      user.value.nickname = editableUser.value.nickname
      user.value.phone = editableUser.value.phone
      updateMessage.value = '변경에 성공했습니다.'
      updateMessageType.value = 'success'
      // 2초 후 편집 모드 종료
      setTimeout(() => {
        isEditMode.value = false
        updateMessage.value = ''
        updateMessageType.value = ''
      }, 2000)
    } else {
      // 백엔드에서 보낸 에러 메시지 표시
      const errorMessage = response.data?.message || '프로필 업데이트에 실패했습니다.'
      updateMessage.value = errorMessage
      updateMessageType.value = 'error'
    }
  } catch (err) {
    console.error('Failed to update profile:', err)
    updateMessage.value = '프로필 업데이트 중 오류가 발생했습니다.'
    updateMessageType.value = 'error'
  }
}

const getRoleText = (role) => {
  if (role === 'HOST') return '호스트'
  if (role === 'USER') return '일반 사용자'
  return '일반 사용자'
}

const getGenderText = (gender) => {
  if (gender === 'MALE') return '남성'
  if (gender === 'FEMALE') return '여성'
  return ''
}

const goBack = () => {
  if (isEditMode.value) {
    cancelEdit()
  } else {
    router.back()
  }
}

onMounted(() => {
  loadUserData()
})
</script>

<template>
  <div class="profile-page container">
    <div class="sub-header">
      <button class="back-btn" @click="goBack">‹</button>
      <h3>프로필 정보</h3>
      <div class="header-buttons">
        <template v-if="!isEditMode">
          <button class="edit-btn" @click="startEdit">✎</button>
        </template>
        <template v-else>
          <button class="cancel-btn" @click="cancelEdit">취소</button>
          <button class="save-btn" @click="handleProfileUpdate">저장</button>
        </template>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">로딩 중...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else class="profile-content">
      <div class="profile-avatar-large">
        👤
      </div>

      <div class="info-group">
        <label>이름</label>
        <div class="info-value">
          {{ user.name || '정보 없음' }}
        </div>
      </div>

      <div class="info-group">
        <label>이메일</label>
        <div class="info-value">
          {{ user.email || '정보 없음' }}
        </div>
        <p class="info-help">이메일은 변경할 수 없습니다</p>
      </div>

      <div class="info-group">
        <label for="nickname-input">닉네임</label>
        <div v-if="!isEditMode" class="info-value">
          {{ user.nickname || '정보 없음' }}
        </div>
        <template v-else>
          <input id="nickname-input" v-model="editableUser.nickname" type="text" class="info-input" />
          <p v-if="validationErrors.nickname" class="error-text">{{ validationErrors.nickname }}</p>
          <!-- Update Message -->
          <div v-if="updateMessage && isEditMode" class="update-message" :class="updateMessageType">
            {{ updateMessage }}
          </div>
        </template>
      </div>

      <div class="info-group">
        <label for="phone-input">전화번호</label>
        <div v-if="!isEditMode" class="info-value">
          {{ user.phone || '정보 없음' }}
        </div>
        <template v-else>
          <input id="phone-input" v-model="editableUser.phone" type="tel" class="info-input" placeholder="010-1234-5678" maxlength="13"/>
          <p v-if="validationErrors.phone" class="error-text">{{ validationErrors.phone }}</p>
        </template>
      </div>

      <div class="info-group">
        <label>성별</label>
        <div class="info-value">
          {{ getGenderText(user.gender) || '정보 없음' }}
        </div>
      </div>

      <div class="info-group">
        <label>역할</label>
        <div class="info-value">
          {{ getRoleText(user.role) }}
        </div>
      </div>

      <div v-if="!isEditMode" class="delete-account-row">
        <span class="delete-account" @click="router.push('/delete-account')">회원 탈퇴</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 2rem auto;
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

.sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.sub-header h3 {
  flex-grow: 1;
  text-align: center;
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0;
}

.back-btn {
  background: none;
  font-size: 2rem;
  padding: 0 0.5rem;
  line-height: 1;
  color: #333;
  cursor: pointer;
}

.header-buttons {
  display: flex;
  gap: 0.5rem;
}

.edit-btn, .save-btn, .cancel-btn {
  background-color: var(--primary);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  color: #004d40;
  cursor: pointer;
  font-weight: 600;
  padding: 0.5rem 1rem;
}

.edit-btn {
  width: 36px;
  height: 36px;
  font-size: 1.2rem;
  padding: 0;
}

.cancel-btn {
  background-color: #f0f0f0;
  color: #555;
}


.loading-state,
.error-state {
  text-align: center;
  padding: 2rem;
  color: #6b7280;
}

.error-state {
  color: #ef4444;
}

.profile-avatar-large {
  width: 100px;
  height: 100px;
  background: #f3f4f6;
  border-radius: 50%;
  margin: 0 auto 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
}

.info-group {
  margin-bottom: 2rem;
}

.info-group label {
  display: block;
  font-size: 0.9rem;
  color: #888;
  margin-bottom: 0.5rem;
}

.info-value {
  font-size: 1.1rem;
  color: #333;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.info-input {
  width: 100%;
  padding: 0.75rem;
  font-size: 1.1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-sizing: border-box;
}

.info-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(0, 77, 64, 0.2);
}

.error-text {
  color: #ef4444;
  font-size: 0.8rem;
  margin-top: 0.5rem;
}

.info-help {
  font-size: 0.8rem;
  color: #aaa;
  margin-top: 0.25rem;
  margin-left: 0;
}

.delete-account-row {
  margin-top: 3rem;
  border-top: 1px solid #eee;
  padding-top: 1.5rem;
  text-align: center;
}

.delete-account {
  color: #004d40;
  background: var(--primary);
  padding: 0.5rem 1.5rem;
  border-radius: 8px;
  font-size: 0.95rem;
  cursor: pointer;
  font-weight: 600;
}

.update-message {
  padding: 1rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  text-align: center;
  margin-top: 1rem;
  margin-bottom: 1rem;
}

.update-message.success {
  background-color: #d1fae5;
  color: #065f46;
}

.update-message.error {
  background-color: #fee2e2;
  color: #dc2626;
}
</style>
