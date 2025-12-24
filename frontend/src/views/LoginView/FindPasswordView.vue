<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { findPassword } from '@/api/authClient';

const router = useRouter();
const email = ref('');
const phone = ref('');
const verificationCode = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const isLoading = ref(false);
const codeSent = ref(false);

// 전화번호 자동 포맷팅
watch(phone, (newValue) => {
  let cleaned = newValue.replace(/\D/g, '');
  let formatted = '';

  if (cleaned.startsWith('010') && cleaned.length > 3) {
    if (cleaned.length < 8) {
      formatted = cleaned.substring(0, 3) + '-' + cleaned.substring(3);
    } else {
      formatted = cleaned.substring(0, 3) + '-' + cleaned.substring(3, 7) + '-' + cleaned.substring(7, 11);
    }
  } else if (cleaned.length > 0) {
    formatted = cleaned;
  }

  if (formatted !== newValue) {
    phone.value = formatted;
  }
});

// 비밀번호 찾기 - 인증 코드 전송
const handleSendCode = async () => {
    errorMessage.value = '';
    successMessage.value = '';

    if (!email.value || !phone.value) {
        errorMessage.value = '이메일과 전화번호를 모두 입력해주세요.';
        return;
    }

    isLoading.value = true;
    try {
        const response = await findPassword({ email: email.value, phone: phone.value });
        if (response.ok) {
            codeSent.value = true;
            successMessage.value = '인증 코드가 이메일로 전송되었습니다.';
        } else {
            errorMessage.value = response.data?.message || '일치하는 사용자 정보가 없습니다.';
        }
    } catch (error) {
        console.error('비밀번호 찾기 오류:', error);
        errorMessage.value = '비밀번호 찾기 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    } finally {
        isLoading.value = false;
    }
};

// 인증 코드 확인 (검증 없이 다음 페이지로 이동)
const handleVerifyCode = () => {
    errorMessage.value = '';
    successMessage.value = '';

    if (!verificationCode.value) {
        errorMessage.value = '인증 코드를 입력해주세요.';
        return;
    }

    if (verificationCode.value.length !== 6) {
        errorMessage.value = '인증 코드는 6자리입니다.';
        return;
    }

    // 비밀번호 재설정 페이지로 이동 (실제 검증은 비밀번호 재설정 시 수행)
    router.push({
        name: 'reset-password',
        query: { email: email.value, code: verificationCode.value }
    });
};

const goToLogin = () => {
    router.push('/login');
};
</script>

<template>
    <div class="find-password-page">
        <div class="find-password-container">
            <h1 class="page-title">비밀번호 찾기</h1>

            <div class="form-section">
                <p class="description">
                    {{ codeSent ? '이메일로 전송된 인증 코드를 입력해주세요.' : '회원가입 시 입력했던 이메일과 전화번호를 입력해주세요.' }}
                </p>

                <div v-if="!codeSent">
                    <div class="input-group">
                        <label for="email">이메일</label>
                        <input type="email" id="email" v-model="email" placeholder="example@email.com" />
                    </div>
                    <div class="input-group">
                        <label for="phone">전화번호</label>
                        <input type="tel" id="phone" v-model="phone" placeholder="010-1234-5678" />
                    </div>

                    <button class="submit-btn" @click="handleSendCode" :disabled="isLoading">
                        {{ isLoading ? '전송 중...' : '인증 코드 전송' }}
                    </button>
                </div>

                <div v-else>
                    <div class="input-group">
                        <label for="code">인증 코드</label>
                        <input
                            type="text"
                            id="code"
                            v-model="verificationCode"
                            placeholder="6자리 인증 코드"
                            maxlength="6"
                        />
                    </div>

                    <button class="submit-btn" @click="handleVerifyCode">
                        다음 단계로
                    </button>

                    <button class="resend-btn" @click="handleSendCode" :disabled="isLoading">
                        인증 코드 재전송
                    </button>
                </div>

                <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
                <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

                <button class="back-btn" @click="goToLogin">로그인 페이지로 돌아가기</button>
            </div>
        </div>
    </div>
</template>

<style scoped>
.find-password-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #f9fafb;
    padding: 1rem;
}

.find-password-container {
    background: white;
    max-width: 400px;
    width: 100%;
    border-radius: 16px;
    padding: 2rem;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    text-align: center;
}

.page-title {
    font-size: 1.8rem;
    font-weight: 700;
    color: #333;
    margin-bottom: 1.5rem;
}

.description {
    font-size: 0.95rem;
    color: #666;
    margin-bottom: 1.5rem;
    line-height: 1.5;
}

.form-section {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.input-group {
    text-align: left;
}

.input-group label {
    display: block;
    font-size: 0.9rem;
    font-weight: 600;
    color: #333;
    margin-bottom: 0.5rem;
}

.input-group input {
    width: 100%;
    padding: 0.8rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 1rem;
    outline: none;
}

.input-group input:focus {
    border-color: var(--primary);
}

.submit-btn {
    width: 100%;
    padding: 1rem;
    background: var(--primary);
    color: #004d40;
    border: none;
    border-radius: 8px;
    font-size: 1.1rem;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.3s ease;
}

.submit-btn:hover {
    background: #004d40;
    color: white;
}

.submit-btn:disabled {
    background: #e5e7eb;
    color: #9ca3af;
    cursor: not-allowed;
}

.resend-btn {
    width: 100%;
    padding: 0.8rem;
    background: #f3f4f6;
    color: #333;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    margin-top: -0.5rem;
}

.resend-btn:hover {
    background: #e5e7eb;
}

.resend-btn:disabled {
    background: #f9fafb;
    color: #9ca3af;
    cursor: not-allowed;
}

.back-btn {
    width: 100%;
    padding: 0.8rem;
    background: transparent;
    color: #666;
    border: none;
    border-radius: 8px;
    font-size: 0.95rem;
    cursor: pointer;
    transition: color 0.3s ease;
}

.back-btn:hover {
    color: #333;
    text-decoration: underline;
}

.success-message {
    color: var(--primary);
    font-size: 0.9rem;
    font-weight: 600;
}

.error-message {
    color: #dc2626;
    font-size: 0.9rem;
}
</style>
