<script setup>
import AppHeader from './components/common/AppHeader.vue'
import AppFooter from './components/common/AppFooter.vue'
</script>

<template>
  <AppHeader />
  <main>
    <div class="content-wrapper">
      <h1>여행을 떠나세요</h1>
      <p>전 세계의 숨겨진 보석들을 발견하고 잊지 못할 경험을 만들어보세요.</p>
    </div>
  </main>
  <AppFooter />
</template>

<style scoped>
h1 {
  color: #1a1f36;
  font-size: 32px;
  margin-bottom: 16px;
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
}

p {
  color: #6a7684;
  font-size: 16px;
  line-height: 1.6;
  font-family: 'Noto Sans KR', sans-serif;
}
</style>
