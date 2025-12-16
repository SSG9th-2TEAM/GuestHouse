<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="profile-page container">
    <div class="sub-header">
      <button class="back-btn" @click="goBack">‹</button>
      <h3>프로필 정보</h3>
      <button class="edit-btn">✎</button>
    </div>
    
    <div class="profile-content">
      <div class="profile-avatar-large">
        👤
      </div>
      
      <div class="info-group">
        <label>이름</label>
        <div class="info-value">
           홍길동 <!-- {{ user.name }} -->
        </div>
      </div>

      <div class="info-group">
        <label>이메일</label>
        <div class="info-value">
           user@example.com <!-- {{ user.name }} -->
        </div>
        <p class="info-help">이메일은 변경할 수 없습니다</p>
      </div>

      <div class="info-group">
        <label>전화번호</label>
        <div class="info-value">
           010-1234-5678 <!-- {{ user.name }} -->
        </div>
      </div>

        <div class="info-group">
        <label>역할</label>
        <div class="info-value">
            일반 사용자 <!-- {{ user.name }} -->
        </div>
      </div>

      <div class="delete-account-row">
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
  font-size: 1.25rem;
  font-weight: 700;
}

.back-btn {
  background: none;
  font-size: 2rem;
  padding: 0 0.5rem;
  line-height: 1;
  color: #333;
  cursor: pointer;
}

.edit-btn {
  background-color: var(--primary);
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: #004d40;
  cursor: pointer;
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

.info-value .icon {
  width: 24px;
  text-align: center;
  font-size: 1.2rem;
}

.info-help {
  font-size: 0.8rem;
  color: #aaa;
  margin-top: 0.25rem;
  margin-left: 35px;
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
</style>
